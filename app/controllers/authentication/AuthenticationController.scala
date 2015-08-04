package controllers.authentication

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AuthenticatorResult
import com.mohiva.play.silhouette.api.util.{Clock, Credentials, PasswordHasher}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers._
import controllers.SilhouetteTestController
import forms.{SignInForm, SignUpForm}
import models.User
import models.services.{UserExists, UserService}
import net.ceedubs.ficus.Ficus._
import play.api.Configuration
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import views.html._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

class AuthenticationController @Inject()(
  userService: UserService,
  passwordHasher: PasswordHasher,
  authInfoRepository: AuthInfoRepository,
  credentialsProvider: CredentialsProvider,
  socialProviderRegistry: SocialProviderRegistry,
  configuration: Configuration,
  clock: Clock)(implicit val env: Environment[User, CookieAuthenticator], val messagesApi: MessagesApi) extends SilhouetteTestController {

  // TODO move this to module
  private val c = configuration.underlying
  private val authenticatorExpiry: FiniteDuration = c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry")
  private val authenticatorIdleTimeout: Option[FiniteDuration] = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout")
  private val cookieMaxAge: Option[FiniteDuration] = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.cookieMaxAge")

  def signInPage = UserAwareAction { implicit request =>
    request.identity match {
      case Some(user) => Redirect(controllers.routes.Application.index())
      case None => Ok(pages.authentication.signIn(SignInForm.form, socialProviderRegistry))
    }
  }

  def signUpPage = UserAwareAction { implicit request =>
    request.identity match {
      case Some(user) => Redirect(controllers.routes.Application.index())
      case None => Ok(pages.authentication.signUp(SignUpForm.form))
    }
  }

  /**
   * Handles the Sign Out action.
   *
   * @return The result to display.
   */
  def signOut = SecuredAction.async { implicit request =>
    doSignOut(Redirect(controllers.routes.Application.index()))
  }

  private def doSignOut(result: => Result)(implicit request: SecuredRequest[AnyContent]): Future[Result] = {
    env.authenticatorService.discard(request.authenticator, result) andThen {
      case Success(_) => env.eventBus.publish(LogoutEvent(request.identity, request, request2Messages))
    }
  }

  /**
   * Registers a new user.
   *
   * @return The result to display.
   */
  def signUp = Action.async { implicit request =>
    SignUpForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(pages.authentication.signUp(form))),
      data => doSignUp(data, Redirect(controllers.routes.Application.index())) recover {
        case userExists: UserExists =>
          BadRequest(pages.authentication.signUp(SignUpForm.form.fill(data).withError("email", "user.exists")))
      }
    )
  }

  private def doSignUp(data: SignUpForm.Data, expecterdResult: => Result)(implicit request: Request[AnyContent]): Future[Result] = {
    val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
    val userData = User(0, Some(data.displayName), Some(data.email), Some(data.realName), data.avatarURL)

    for {
      user: User <- createUser(loginInfo, userData, data.password)
      result <- initAuthenticator(loginInfo, user, rememberMe = false, expecterdResult)
    } yield result
  }

  private def createUser(loginInfo: LoginInfo, userData: User, password: String)(implicit request: Request[AnyContent]) = {
    for {
      user <- userService.create(loginInfo, userData)
      authInfo <- authInfoRepository.add(loginInfo, passwordHasher.hash(password))
    } yield user
  } andThen {
    case Success(user) => env.eventBus.publish(SignUpEvent(user, request, request2Messages))
  }

  /**
   * Authenticates a user against the credentials provider.
   *
   * @return The result to display.
   */
  def signInWithCredentials = Action.async { implicit request =>
    SignInForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(pages.authentication.signIn(form, socialProviderRegistry))),
      data => doSignIn(data, Redirect(controllers.routes.Application.index())) recover {
        case providerException: ProviderException =>
          BadRequest(pages.authentication.signIn(SignInForm.form.fill(data).withGlobalError(Messages("invalid.credentials")), socialProviderRegistry))
      }
    )
  }

  private def doSignIn(data: SignInForm.Data, expectedResult: => Result)(implicit request: Request[AnyContent]): Future[Result] = for {
    loginInfo: LoginInfo <- credentialsProvider.authenticate(Credentials(data.email, data.password))
    user: User <- userService(loginInfo)
    result <- initAuthenticator(loginInfo, user, data.rememberMe, expectedResult)
  } yield result

  /**
   * Authenticates a user against a social provider.
   *
   * @param provider The ID of the provider to authenticate against.
   * @return The result to display.
   */
  def signInWithSocialProvider(provider: String) = Action.async { implicit request =>
    socialProviderRegistry.get[SocialProvider](provider) match {
      case Some(provider: SocialProvider with CommonSocialProfileBuilder) =>
        provider.authenticate().flatMap {
          case Left(result) => Future.successful(result)
          case Right(authInfo) => doSignIn(provider)(authInfo, Redirect(controllers.routes.Application.index())) recover {
            case e: ProviderException =>
              logger.error("Unexpected provider error", e)
              Redirect(routes.AuthenticationController.signInPage()).flashing("error" -> Messages("could.not.authenticate"))
          }
        }
      case _ =>
        logger.error("Unable to sign in", new ProviderException(s"Unknown provider $provider"))
        Future.successful(Redirect(routes.AuthenticationController.signInPage()).flashing("error" -> Messages("could.not.authenticate")))
    }
  }

  private def doSignIn(provider: SocialProvider with CommonSocialProfileBuilder)(authInfo: provider.A, expectedResult: Result)(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      profile: CommonSocialProfile <- provider.retrieveProfile(authInfo)
      user: User <- createOrUpdateUser(profile, authInfo)
      result <- initAuthenticator(profile.loginInfo, user, rememberMe = false, expectedResult)
    } yield result
  }

  private def createOrUpdateUser(profile: CommonSocialProfile, authInfo: AuthInfo) =
    userService.retrieve(profile.loginInfo) flatMap {
      case Some(user) => authInfoRepository.save(profile.loginInfo, authInfo) map { _ => user }
      case None =>
        val userData = User(0, None, profile.email, profile.fullName, profile.avatarURL)
        for {
          authInfo <- authInfoRepository.save(profile.loginInfo, authInfo)
          user: User <- userService.create(profile.loginInfo, userData)
        } yield user
    }

  private def initAuthenticator(loginInfo: LoginInfo, user: User, rememberMe: Boolean, expectedResult: Result)(implicit request: Request[AnyContent]): Future[AuthenticatorResult] = for {
    authenticator <- env.authenticatorService.create(loginInfo) map applyRememberMe(rememberMe)
    cookie <- env.authenticatorService.init(authenticator)
    result: AuthenticatorResult <- env.authenticatorService.embed(cookie, expectedResult)
  } yield {
      env.eventBus.publish(LoginEvent(user, request, request2Messages))
      result
    }

  private def applyRememberMe(rememberMe: Boolean)(authenticator: CookieAuthenticator): CookieAuthenticator = {
    if (rememberMe) authenticator.copy(
      expirationDateTime = clock.now + authenticatorExpiry,
      idleTimeout = authenticatorIdleTimeout,
      cookieMaxAge = cookieMaxAge
    )
    else authenticator
  }
}


package controllers.authentication

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{Clock, Credentials, PasswordHasher}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers._
import controllers.SilhouetteTestController
import forms.{SignInForm, SignUpForm}
import models.TestUser
import models.services.TestUserService
import net.ceedubs.ficus.Ficus._
import play.api.Configuration
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import views.html._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class AuthenticationController @Inject()(
  userService: TestUserService,
  passwordHasher: PasswordHasher,
  authInfoRepository: AuthInfoRepository,
  credentialsProvider: CredentialsProvider,
  socialProviderRegistry: SocialProviderRegistry,
  configuration: Configuration,
  clock: Clock)(implicit val env: Environment[TestUser, CookieAuthenticator], val messagesApi: MessagesApi) extends SilhouetteTestController {

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
      case Some(user) => Redirect(controllers.routes.Application.index)
      case None => Ok(pages.authentication.signUp(SignUpForm.form))
    }
  }

  /**
   * Handles the Sign Out action.
   *
   * @return The result to display.
   */
  def signOut = SecuredAction.async { implicit request =>
    val result = Redirect(controllers.routes.Application.index())
    env.eventBus.publish(LogoutEvent(request.identity, request, request2Messages))
    env.authenticatorService.discard(request.authenticator, result)
  }

  /**
   * Registers a new user.
   *
   * @return The result to display.
   */
  def signUp = Action.async { implicit request =>
    SignUpForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(pages.authentication.signUp(form))),
      data => {
        doSignUp(data) flatMap { cookie =>
          val result = Redirect(controllers.routes.Application.index)
          env.authenticatorService.embed(cookie, result)
        } recover {
          case userExists: UserExists =>
            BadRequest(pages.authentication.signUp(SignUpForm.form.fill(data).withError("email", "user.exists")))
        }
      }
    )
  }

  private def doSignUp(data: SignUpForm.Data)(implicit request: Request[AnyContent]): Future[Cookie] = for {
    user <- saveUser(data)
    cookie <- login(user, rememberMe = false)
  } yield cookie

  private def saveUser(signUpData: SignUpForm.Data)(implicit request: Request[AnyContent]) = {
    val loginInfo = LoginInfo(CredentialsProvider.ID, signUpData.email)
    userService.retrieve(loginInfo) flatMap {
      case Some(user) => throw new UserExists(user)
      case None => for {
        user <- userService.save(TestUser(id = UUID.randomUUID(), loginInfo = loginInfo))
        authInfo <- authInfoRepository.add(loginInfo, passwordHasher.hash(signUpData.password))
      } yield {
          env.eventBus.publish(SignUpEvent(user, request, request2Messages))
          user
        }
    }
  }

  /**
   * Authenticates a user against the credentials provider.
   *
   * @return The result to display.
   */
  def signInWithCredentials = Action.async { implicit request =>
    SignInForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(pages.authentication.signIn(form, socialProviderRegistry))),
      data => doSignIn(data) flatMap { cookie =>
        val result = Redirect(controllers.routes.Application.index())
        env.authenticatorService.embed(cookie, result)
      } recover {
        case providerException: ProviderException =>
          BadRequest(pages.authentication.signIn(SignInForm.form.fill(data).withGlobalError(Messages("invalid.credentials")), socialProviderRegistry))
      }
    )
  }

  private def doSignIn(data: SignInForm.Data)(implicit request: Request[AnyContent]): Future[Cookie] = for {
    user <- retrieveUser(data)
    cookie <- login(user, data.rememberMe)
  } yield cookie

  private def retrieveUser(data: SignInForm.Data) = for {
    loginInfo <- credentialsProvider.authenticate(Credentials(data.email, data.password))
    maybeUser <- userService.retrieve(loginInfo)
  } yield {
      maybeUser match {
        case Some(user) => user
        case None => throw new IdentityNotFoundException("Couldn't find user")
      }
    }

  private def login(user: TestUser, rememberMe: Boolean)(implicit request: Request[AnyContent]): Future[Cookie] = for {
    authenticator <- env.authenticatorService.create(user.loginInfo) map remember(rememberMe)
    cookie <- env.authenticatorService.init(authenticator)
  } yield {
      env.eventBus.publish(LoginEvent(user, request, request2Messages))
      cookie
    }

  private def remember(rememberMe: Boolean)(authenticator: CookieAuthenticator): CookieAuthenticator = {
    if (rememberMe) authenticator.copy(
      expirationDateTime = clock.now + authenticatorExpiry,
      idleTimeout = authenticatorIdleTimeout,
      cookieMaxAge = cookieMaxAge
    )
    else authenticator
  }

  /**
   * Authenticates a user against a social provider.
   *
   * @param provider The ID of the provider to authenticate against.
   * @return The result to display.
   */
  def signInWithSocialProvider(provider: String) = Action.async { implicit request =>
    (socialProviderRegistry.get[SocialProvider](provider) match {
      case Some(provider: SocialProvider with CommonSocialProfileBuilder) =>
        provider.authenticate().flatMap {
          case Left(result) => Future.successful(result)
          case Right(authInfo) => doSignIn(provider)(authInfo) flatMap { cookie =>
            val result = Redirect(controllers.routes.Application.index())
            env.authenticatorService.embed(cookie, result)
          }
        }
      case _ => Future.failed(new ProviderException(s"Cannot authenticate with unexpected social provider $provider"))
    }).recover {
      case e: ProviderException =>
        logger.error("Unexpected provider error", e)
        Redirect(routes.AuthenticationController.signInPage()).flashing("error" -> Messages("could.not.authenticate"))
    }
  }

  private def doSignIn(provider: SocialProvider with CommonSocialProfileBuilder)(authInfo: provider.A)(implicit request: Request[AnyContent]): Future[Cookie] = for {
    user <- saveUser(provider)(authInfo)
    cookie <- login(user, rememberMe = false)
  } yield cookie

  private def saveUser(provider: SocialProvider with CommonSocialProfileBuilder)(authInfo: provider.A) = for {
    profile: CommonSocialProfile <- provider.retrieveProfile(authInfo)
    user: TestUser <- userService.save(profile)
    authInfo <- authInfoRepository.save(profile.loginInfo, authInfo)
  } yield user

}

case class UserExists(user: TestUser) extends scala.Exception(s"User exists: ${user.loginInfo.providerID}/${user.loginInfo.providerKey}")

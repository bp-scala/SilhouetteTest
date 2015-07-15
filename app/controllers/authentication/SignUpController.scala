package controllers.authentication

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AuthenticatorResult
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import controllers.SilhouetteTestController
import forms.SignUpForm
import forms.SignUpForm.Data
import models.TestUser
import models.services.TestUserService
import play.api.i18n.MessagesApi
import play.api.mvc._
import views.html._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by rp on 15. 07. 15..
 */
class SignUpController @Inject()(passwordHasher: PasswordHasher, userService: TestUserService, authInfoRepository: AuthInfoRepository)(implicit val env: Environment[TestUser, CookieAuthenticator], val messagesApi: MessagesApi) extends SilhouetteTestController {
  def signUpPage = UserAwareAction { implicit request =>
    request.identity match {
      case Some(user) => Redirect(controllers.routes.Application.index)
      case None => Ok(pages.authentication.signUp(SignUpForm.form))
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
      data => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
        userService.retrieve(loginInfo).flatMap {
          case Some(user) => Future.successful(BadRequest(pages.authentication.signUp(SignUpForm.form.fill(data).withError("email", "user.exists"))))
          case None => doSignUp(data, loginInfo)
        }
      }
    )
  }

  private def doSignUp(data: Data, loginInfo: LoginInfo)(implicit request: Request[AnyContent]): Future[AuthenticatorResult] = for {
    savedUser <- createUser(data, loginInfo)
    cookie <- createCookie(loginInfo)
    result <- env.authenticatorService.embed(cookie, Redirect(controllers.routes.Application.index))
  } yield {
      env.eventBus.publish(SignUpEvent(savedUser, request, request2Messages))
      env.eventBus.publish(LoginEvent(savedUser, request, request2Messages))
      result
    }


  private def createUser(signUpData: Data, loginInfo: LoginInfo)(implicit request: Request[AnyContent]): Future[TestUser] = {
    val authInfo = passwordHasher.hash(signUpData.password)
    val user = TestUser(id = UUID.randomUUID(), loginInfo = loginInfo)
    for {
      savedUser <- userService.save(user)
      savedPasswordInfo <- authInfoRepository.add(loginInfo, authInfo)
    } yield savedUser
  }

  private def createCookie(loginInfo: LoginInfo)(implicit request: Request[AnyContent]): Future[Cookie] = for {
    authenticator <- env.authenticatorService.create(loginInfo)
    cookie <- env.authenticatorService.init(authenticator)
  } yield cookie
}

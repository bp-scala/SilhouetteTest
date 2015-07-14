package controllers

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import forms.SignUpForm
import models.TestUser
import models.services.TestUserService
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Application @Inject()(val messagesApi: MessagesApi, passwordHasher: PasswordHasher, userService: TestUserService, authInfoRepository: AuthInfoRepository)(implicit val env: Environment[TestUser, CookieAuthenticator]) extends Silhouette[TestUser, CookieAuthenticator] with I18nSupport {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def secured = SecuredAction { implicit request =>
    Ok(views.html.secured("Your secured new application is ready."))
  }

  def signUp = UserAwareAction { implicit request =>
    request.identity match {
      case Some(user) => Redirect(routes.Application.index)
      case None => Ok(views.html.signUp(SignUpForm.form))
    }
  }

  /**
   * Registers a new user.
   *
   * @return The result to display.
   */
  def postSignUp = Action.async { implicit request =>
    SignUpForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.signUp(form))),
      data => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
        userService.retrieve(loginInfo).flatMap {
          case Some(user) =>
            Future.successful(Redirect(routes.Application.signUp()).flashing("error" -> Messages("user.exists")))
          case None =>
            val authInfo = passwordHasher.hash(data.password)
            val user = TestUser(id = UUID.randomUUID(), loginInfo = loginInfo)
            for {
              user <- userService.save(user)
              authInfo <- authInfoRepository.add(loginInfo, authInfo)
              authenticator <- env.authenticatorService.create(loginInfo)
              value <- env.authenticatorService.init(authenticator)
              result <- env.authenticatorService.embed(value, Redirect(routes.Application.index()))
            } yield {
              env.eventBus.publish(SignUpEvent(user, request, request2Messages))
              env.eventBus.publish(LoginEvent(user, request, request2Messages))
              result
            }
        }
      }
    )
  }
}
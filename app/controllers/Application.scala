package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.User
import models.services.UserService
import play.api.i18n.MessagesApi
import views.html._

class Application @Inject()(passwordHasher: PasswordHasher, userService: UserService, authInfoRepository: AuthInfoRepository)(implicit val env: Environment[User, CookieAuthenticator], val messagesApi: MessagesApi) extends SilhouetteTestController {
  def index = UserAwareAction { implicit request =>
    Ok(pages.index("This is an unsecured page."))
  }

  def secured = SecuredAction { implicit request =>
    Ok(pages.secured("This is a secured page."))
  }
}
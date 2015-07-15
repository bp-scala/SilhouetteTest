package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.TestUser
import models.services.TestUserService
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import views.html._

class Application @Inject()(val messagesApi: MessagesApi, passwordHasher: PasswordHasher, userService: TestUserService, authInfoRepository: AuthInfoRepository)(implicit val env: Environment[TestUser, CookieAuthenticator]) extends Silhouette[TestUser, CookieAuthenticator] with I18nSupport {
  def index = UserAwareAction { implicit request =>
    Ok(pages.index("Your new application is ready."))
  }

  def secured = SecuredAction { implicit request =>
    Ok(pages.secured("Your secured new application is ready."))
  }

  protected implicit def currentUser(implicit request: Request[AnyContent]): Option[TestUser] = request match {
    case userAware: UserAwareRequest[TestUser] => userAware.identity
    case secured: SecuredRequest[TestUser] => Some(secured.identity)
    case _ => None
  }

}
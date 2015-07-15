package controllers.authentication

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, LogoutEvent, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.TestUser
import play.api.i18n.{I18nSupport, MessagesApi}

/**
 * Created by rp on 15. 07. 15..
 */
class SignOutController @Inject()(val messagesApi: MessagesApi)(implicit val env: Environment[TestUser, CookieAuthenticator]) extends Silhouette[TestUser, CookieAuthenticator] with I18nSupport {
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
}

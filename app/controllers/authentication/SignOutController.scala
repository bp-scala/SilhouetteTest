package controllers.authentication

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, LogoutEvent}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import controllers.SilhouetteTestController
import models.TestUser
import play.api.i18n.MessagesApi

/**
 * Created by rp on 15. 07. 15..
 */
class SignOutController @Inject()()(implicit val env: Environment[TestUser, CookieAuthenticator], val messagesApi: MessagesApi) extends SilhouetteTestController {
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

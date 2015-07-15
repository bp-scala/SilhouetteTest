package controllers.authentication

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import controllers.SilhouetteTestController
import models.TestUser
import play.api.i18n.MessagesApi

/**
 * Created by rp on 15. 07. 15..
 */
class SignInController @Inject()()(implicit val env: Environment[TestUser, CookieAuthenticator], val messagesApi: MessagesApi) extends SilhouetteTestController {

}

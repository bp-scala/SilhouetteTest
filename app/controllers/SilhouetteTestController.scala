package controllers

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.User
import play.api.i18n.I18nSupport
import play.api.mvc.{AnyContent, Request}

/**
 * Created by rp on 15. 07. 16..
 */
trait SilhouetteTestController extends Silhouette[User, CookieAuthenticator] with I18nSupport {
  protected implicit def currentUser(implicit request: Request[AnyContent]): Option[User] = request match {
    case userAware: UserAwareRequest[User] => userAware.identity
    case secured: SecuredRequest[User] => Some(secured.identity)
    case _ => None
  }
}

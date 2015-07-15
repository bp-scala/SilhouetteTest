package controllers

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.TestUser
import play.api.i18n.I18nSupport
import play.api.mvc.{AnyContent, Request}

/**
 * Created by rp on 15. 07. 16..
 */
trait SilhouetteTestController extends Silhouette[TestUser, CookieAuthenticator] with I18nSupport {
  protected implicit def currentUser(implicit request: Request[AnyContent]): Option[TestUser] = request match {
    case userAware: UserAwareRequest[TestUser] => userAware.identity
    case secured: SecuredRequest[TestUser] => Some(secured.identity)
    case _ => None
  }
}

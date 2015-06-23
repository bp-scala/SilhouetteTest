package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import com.mohiva.play.silhouette.api.util.Clock
import com.mohiva.play.silhouette.impl.authenticators.{SessionAuthenticator, SessionAuthenticatorService, SessionAuthenticatorSettings}
import com.mohiva.play.silhouette.impl.util.DefaultFingerprintGenerator
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class Application @Inject()(val messagesApi: MessagesApi) extends Silhouette[TestUser, SessionAuthenticator] with I18nSupport {

  implicit val env = new TestEnvironment

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def secured = SecuredAction { implicit request =>
    Ok(views.html.secured("Your secured new application is ready."))
  }
}

case class TestUser(loginInfo: LoginInfo) extends Identity

class TestUserService extends IdentityService[TestUser] {
  var userMap = Map.empty[LoginInfo, TestUser]

  def retrieve(loginInfo: LoginInfo): Future[Option[TestUser]] = Future {
    userMap.get(loginInfo)
  }

  def save(user: TestUser) = Future {
    userMap += (user.loginInfo -> user)
    user
  }
}


class TestEnvironment extends Environment[TestUser, SessionAuthenticator] {
  override def identityService: IdentityService[TestUser] = new TestUserService
  override def authenticatorService: AuthenticatorService[SessionAuthenticator] = new SessionAuthenticatorService(SessionAuthenticatorSettings(
    sessionKey = "authenticator",
    encryptAuthenticator = true,
    useFingerprinting = true,
    authenticatorIdleTimeout = Some(1800),
    authenticatorExpiry = 43200
  ), new DefaultFingerprintGenerator(), Clock())
  override def eventBus: EventBus = EventBus()
  override def requestProviders: Seq[RequestProvider] = Nil
  override implicit val executionContext: ExecutionContext = global
}



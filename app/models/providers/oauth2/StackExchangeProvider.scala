package models.providers.oauth2

import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.api.{Logger, LoginInfo}
import com.mohiva.play.silhouette.impl.exceptions.ProfileRetrievalException
import com.mohiva.play.silhouette.impl.providers._
import models.providers.oauth2.StackExchangeProvider._
import play.api.libs.json.{JsNumber, JsObject, JsValue}
import play.api.libs.ws.WSResponse

import scala.concurrent.Future
import scala.util.Try

/**
 * Base Stack Exchange OAuth2 Provider.
 *
 */
trait BaseStackExchangeProvider extends OAuth2Provider with Logger {

  /**
   * The content type to parse a profile from.
   */
  override type Content = JsValue

  /**
   * The provider ID.
   */
  override val id = ID

  /**
   * Defines the URLs that are needed to retrieve the profile data.
   */
  override protected val urls = Map("api" -> API)

  /**
   * Builds the social profile.
   *
   * @param authInfo The auth info received from the provider.
   * @return On success the build social profile, otherwise a failure.
   */
  override protected def buildProfile(authInfo: OAuth2Info): Future[Profile] = {
    val url: String = urls("api").format(site, authInfo.accessToken, key)
    logger.debug(s"getting profile from $url")
    httpLayer.url(url).get().flatMap { response =>
      val json = response.json
      (json \ "error_id").asOpt[JsNumber] match {
        case Some(errorId) =>
          val errorMsg = (json \ "error_message").as[String]
          val errorType = (json \ "error_name").as[String]
          val errorCode = errorId.as[Int]

          throw new ProfileRetrievalException(SpecifiedProfileError.format(id, errorMsg, errorType, errorCode))
        case _ => profileParser.parse((json \ "items").as[List[JsObject]].head)
      }
    }
  }

  override protected def buildInfo(response: WSResponse) = Try {
    val params = parseURLEncodedBody(response.body)
    OAuth2Info(accessToken = params("access_token"), expiresIn = params.get("expires") map {_.toInt})
  }

  private def parseURLEncodedBody(body: String): Map[String, String] =
    (body.split("&").toSeq map { p =>
      val Array(name, value) = p.split("=")
      name -> value
    }).toMap


  def site: String

  def key: String
}


/**
 * The profile parser for the common social profile.
 */
class StackExchangeProfileParser extends SocialProfileParser[JsValue, CommonSocialProfile] {

  /**
   * Parses the social profile.
   *
   * @param json The content returned from the provider.
   * @return The social profile from given result.
   */
  override def parse(json: JsValue) = Future.successful {
    val userID = (json \ "account_id").as[Int].toString
    val firstName = None
    val lastName = None
    val fullName = None
    val avatarURL = (json \ "profile_image").asOpt[String]
    val email = None

    CommonSocialProfile(
      loginInfo = LoginInfo(ID, userID),
      firstName = firstName,
      lastName = lastName,
      fullName = fullName,
      avatarURL = avatarURL,
      email = email)
  }
}

/**
 * The Stack Exchange OAuth2 Provider.
 *
 * @param httpLayer The HTTP layer implementation.
 * @param stateProvider The state provider implementation.
 * @param settings The provider settings.
 */
class StackExchangeProvider(
  protected val httpLayer: HTTPLayer,
  protected val stateProvider: OAuth2StateProvider,
  val settings: OAuth2Settings)
  extends BaseStackExchangeProvider with CommonSocialProfileBuilder {

  /**
   * The type of this class.
   */
  override type Self = StackExchangeProvider

  /**
   * The profile parser implementation.
   */
  override val profileParser = new StackExchangeProfileParser

  val key = settings.customProperties("key")
  val site = settings.customProperties("site")

  /**
   * Gets a provider initialized with a new settings object.
   *
   * @param f A function which gets the settings passed and returns different settings.
   * @return An instance of the provider initialized with new settings.
   */
  override def withSettings(f: (Settings) => Settings) = new StackExchangeProvider(httpLayer, stateProvider, f(settings))
}

/**
 * The companion object.
 */
object StackExchangeProvider {

  /**
   * The error messages.
   */
  val SpecifiedProfileError = "[Silhouette][%s] Error retrieving profile information. Error message: %s, type: %s, code: %s"

  /**
   * The Stack Exchange constants.
   */
  val ID = "stackexchange"

  val API = "https://api.stackexchange.com/me?site=%s&access_token=%s&key=%s"
}
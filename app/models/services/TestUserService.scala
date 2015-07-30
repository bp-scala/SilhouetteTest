package models.services
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import models.TestUser

import scala.concurrent.Future

/**
 * Created by rp on 15. 07. 14..
 */
trait TestUserService extends IdentityService[TestUser] {
  def retrieve(loginInfo: LoginInfo): Future[Option[TestUser]]

  def save(user: TestUser): Future[TestUser]

  def save(profile: CommonSocialProfile): Future[TestUser]
}

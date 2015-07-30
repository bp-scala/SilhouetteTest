package models.services.impl.map

import java.util.UUID
import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import models.TestUser
import models.services.TestUserService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by rp on 15. 07. 14..
 */
@Singleton
class TestUserMap @Inject() extends TestUserService {
  private var userMap = Map.empty[LoginInfo, TestUser]

  override def retrieve(loginInfo: LoginInfo): Future[Option[TestUser]] = Future {
    userMap.get(loginInfo)
  }

  override def save(user: TestUser) = Future {
    userMap += (user.loginInfo -> user)
    user
  }

  def save(profile: CommonSocialProfile) = Future {
    userMap.get(profile.loginInfo) match {
      case Some(user) =>
        userMap += (profile.loginInfo -> user)
        user
      case None =>
        val user = TestUser(UUID.randomUUID(), profile.loginInfo)
        userMap += (profile.loginInfo -> user)
        user
    }
  }
}

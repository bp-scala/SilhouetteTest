package models.services.impl.map

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import models.TestUser
import models.services.TestUserService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by rp on 15. 07. 14..
 */
class TestUserMap @Inject() extends TestUserService {
  override def retrieve(loginInfo: LoginInfo): Future[Option[TestUser]] = Future {
    userMap.get(loginInfo)
  }

  override def save(user: TestUser) = Future {
    userMap += (user.loginInfo -> user)
    user
  }

  private var userMap = Map.empty[LoginInfo, TestUser]
}

package models.services

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import models.User

import scala.concurrent.{ExecutionContext, Future}


trait UserService extends IdentityService[User] {
  protected implicit def ec: ExecutionContext

  def retrieve(loginInfo: LoginInfo): Future[Option[User]]
  def create(loginInfo: LoginInfo, user: User): Future[User]

  def apply(loginInfo: LoginInfo) = retrieve(loginInfo) map {
    case Some(user) => user
    case None => throw new IdentityNotFoundException("Couldn't find user")
  }

  def update(updatedUser: User): Future[User]
}


case class UserData(
  loginInfo: LoginInfo,
  displayName: String,
  email: String,
  avatarURL: Option[String],
  fullName: Option[String])

case class UserExists(user: User, loginInfo: LoginInfo) extends scala.Exception(s"User exists: ${loginInfo.providerID }/${loginInfo.providerKey }")

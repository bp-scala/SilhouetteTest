package models.services.impl.map

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers.{OAuth1Info, OAuth2Info, OpenIDInfo}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


@Singleton
class OAuth2InfoMap @Inject() extends AuthInfoMap[OAuth2Info]

class OAuth1InfoMap @Inject() extends AuthInfoMap[OAuth1Info]

class PasswordInfoMap @Inject() extends AuthInfoMap[PasswordInfo]

class OpenIDInfoMap @Inject() extends AuthInfoMap[OpenIDInfo]

/**
 * Created by rp on 15. 07. 14..
 */
trait AuthInfoMap [T <: com.mohiva.play.silhouette.api.AuthInfo] extends DelegableAuthInfoDAO[T]  {

  private var infoMap = Map.empty[LoginInfo, T]

  /**
   * Saves the auth info for the given login info.
   *
   * This method either adds the auth info if it doesn't exists or it updates the auth info
   * if it already exists.
   *
   * @param loginInfo The login info for which the auth info should be saved.
   * @param authInfo The auth info to save.
   * @return The saved auth info.
   */
  def save(loginInfo: LoginInfo, authInfo: T): Future[T] = {
    find(loginInfo).flatMap {
      case Some(_) => update(loginInfo, authInfo)
      case None => add(loginInfo, authInfo)
    }
  }

  /**
   * Finds the auth info which is linked with the specified login info.
   *
   * @param loginInfo The linked login info.
   * @return The retrieved auth info or None if no auth info could be retrieved for the given login info.
   */
  def find(loginInfo: LoginInfo): Future[Option[T]] = {
    Future.successful(infoMap.get(loginInfo))
  }

  /**
   * Adds new auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be added.
   * @param authInfo The auth info to add.
   * @return The added auth info.
   */
  def add(loginInfo: LoginInfo, authInfo: T): Future[T] = {
    infoMap += (loginInfo -> authInfo)
    Future.successful(authInfo)
  }

  /**
   * Updates the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be updated.
   * @param authInfo The auth info to update.
   * @return The updated auth info.
   */
  def update(loginInfo: LoginInfo, authInfo: T): Future[T] = {
    infoMap += (loginInfo -> authInfo)
    Future.successful(authInfo)
  }

  /**
   * Removes the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be removed.
   * @return A future to wait for the process to be completed.
   */
 def remove(loginInfo: LoginInfo): Future[Unit] = {
    infoMap -= loginInfo
    Future.successful(())
  }
}

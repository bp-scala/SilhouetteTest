package models.services.impl.mysql

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import slick.driver.MySQLDriver.api._
import slick.lifted
import slick.lifted.Tag

import scala.concurrent.{ExecutionContext, Future}

trait MySqlAuthInfoDao[AI <: com.mohiva.play.silhouette.api.AuthInfo, Row <: AuthInfoRow, T <: AuthInfoTable[Row]] extends DelegableAuthInfoDAO[AI] with MySqlDao {
  protected def tableQuery: lifted.TableQuery[T]

  protected def row(loginInfo: LoginInfo, authInfo: AI): Row

  protected def authInfo(row: Row): AI

  protected implicit def ec: ExecutionContext

  def find(loginInfo: LoginInfo): Future[Option[AI]] = {
    val action = findByLoginInfo(loginInfo.providerID, loginInfo.providerKey).result
    database.run(action) map { _.headOption map authInfo }
  }
  def update(loginInfo: LoginInfo, authInfo: AI): Future[AI] = {
    val action = findByLoginInfo(loginInfo.providerID, loginInfo.providerKey).update(row(loginInfo, authInfo))
    database.run(action) map { _ => authInfo }
  }
  def remove(loginInfo: LoginInfo): Future[Unit] = {
    val action = findByLoginInfo(loginInfo.providerID, loginInfo.providerKey).delete
    database.run(action) map { _ => }
  }
  def save(loginInfo: LoginInfo, authInfo: AI): Future[AI] = {
    val action = tableQuery.insertOrUpdate(row(loginInfo, authInfo))
    database.run(action) map { _ => authInfo }
  }
  def add(loginInfo: LoginInfo, authInfo: AI): Future[AI] = {
    val action = tableQuery += row(loginInfo, authInfo)
    database.run(action) map { _ => authInfo }
  }

  private val findByLoginInfo = Compiled { (providerID: Rep[String], providerKey: Rep[String]) =>
    filterByLoginInfo(providerID, providerKey)
  }

  private def filterByLoginInfo(providerID: Rep[String], providerKey: Rep[String]) = tableQuery filter { table =>
    table.providerID === providerID && table.providerKey === providerKey
  }
}

trait AuthInfoRow {
  def providerID: String
  def providerKey: String
}

abstract class AuthInfoTable[Row <: AuthInfoRow](tag: Tag, name: String) extends Table[Row](tag, name) {
  def providerID = column[String]("provider_id", O.SqlType("varchar(32)"))

  def providerKey = column[String]("provider_key", O.SqlType("varchar(64)"))

  def pk = primaryKey(s"pk_$name", (providerID, providerKey))
}
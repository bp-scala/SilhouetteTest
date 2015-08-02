package models.services.impl.mysql

import javax.inject.{Inject, Singleton}
import javax.sql.DataSource

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import slick.driver.MySQLDriver.api._
import slick.lifted.{TableQuery, Tag}

import scala.concurrent.ExecutionContext

@Singleton
class MySqlPasswordInfoDao @Inject()(protected val dataSource: DataSource)(protected implicit val ec: ExecutionContext) extends MySqlAuthInfoDao[PasswordInfo, PasswordInfoRow, PasswordInfoTable] {
  override protected val tableQuery = TableQuery[PasswordInfoTable]

  override protected def authInfo(row: PasswordInfoRow): PasswordInfo = PasswordInfo(row.hasher, row.password, row.salt)

  override protected def row(loginInfo: LoginInfo, authInfo: PasswordInfo): PasswordInfoRow = PasswordInfoRow(
    loginInfo.providerID, loginInfo.providerKey,
    authInfo.hasher, authInfo.password, authInfo.salt
  )
}

case class PasswordInfoRow(providerID: String, providerKey: String, hasher: String, password: String, salt: Option[String]) extends AuthInfoRow

class PasswordInfoTable(tag: Tag) extends AuthInfoTable[PasswordInfoRow](tag, "password_infos") {
  def * = (providerID, providerKey, hasher, password, salt) <>(PasswordInfoRow.tupled, PasswordInfoRow.unapply)

  def hasher = column[String]("hasher", O.SqlType("varchar(16)"))

  def password = column[String]("password", O.SqlType("varchar(64)"))

  def salt = column[Option[String]]("salt", O.SqlType("varchar(64)"))
}

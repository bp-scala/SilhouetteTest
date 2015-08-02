package models.services.impl.mysql

import javax.inject.{Inject, Singleton}
import javax.sql.DataSource

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import slick.driver.MySQLDriver.api._
import slick.lifted.{TableQuery, Tag}

import scala.concurrent.ExecutionContext


@Singleton
class MySqlOAuth1InfoDao @Inject()(protected val dataSource: DataSource)(protected implicit val ec: ExecutionContext) extends MySqlAuthInfoDao[OAuth1Info, OAuth1InfoRow, OAuth1InfoTable] {
  override protected def tableQuery = TableQuery[OAuth1InfoTable]

  override protected def authInfo(row: OAuth1InfoRow): OAuth1Info = OAuth1Info(row.token, row.secret)

  override protected def row(loginInfo: LoginInfo, authInfo: OAuth1Info): OAuth1InfoRow = OAuth1InfoRow(
    loginInfo.providerID, loginInfo.providerKey,
    authInfo.token, authInfo.secret
  )
}

case class OAuth1InfoRow(providerID: String, providerKey: String, token: String, secret: String) extends AuthInfoRow

class OAuth1InfoTable(tag: Tag) extends AuthInfoTable[OAuth1InfoRow](tag, "oauth1_infos") {
  def * = (providerID, providerKey, token, secret) <>(OAuth1InfoRow.tupled, OAuth1InfoRow.unapply)

  def token = column[String]("token", O.SqlType("varchar(32)"))

  def secret = column[String]("password", O.SqlType("varchar(32)"))
}

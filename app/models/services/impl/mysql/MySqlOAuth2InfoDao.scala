package models.services.impl.mysql

import javax.inject.{Inject, Singleton}
import javax.sql.DataSource

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import slick.driver.MySQLDriver.api._
import slick.lifted.{TableQuery, Tag}

import scala.concurrent.ExecutionContext

@Singleton
class MySqlOAuth2InfoDao @Inject()(protected val dataSource: DataSource)(protected implicit val ec: ExecutionContext) extends MySqlAuthInfoDao[OAuth2Info, OAuth2InfoRow, OAuth2InfoTable] {
  override protected def tableQuery = TableQuery[OAuth2InfoTable]

  override protected def authInfo(row: OAuth2InfoRow): OAuth2Info = OAuth2Info(row.accessToken, row.tokenType, row.expiresIn, row.refreshToken)

  override protected def row(loginInfo: LoginInfo, authInfo: OAuth2Info): OAuth2InfoRow = OAuth2InfoRow(
    loginInfo.providerID, loginInfo.providerKey,
    authInfo.accessToken, authInfo.tokenType, authInfo.expiresIn, authInfo.refreshToken
  )
}

case class OAuth2InfoRow(providerID: String, providerKey: String, accessToken: String, tokenType: Option[String], expiresIn: Option[Int], refreshToken: Option[String]) extends AuthInfoRow

class OAuth2InfoTable(tag: Tag) extends AuthInfoTable[OAuth2InfoRow](tag, "oauth2_infos") {
  def * = (providerID, providerKey, accessToken, tokenType, expiresIn, refreshToken) <>(OAuth2InfoRow.tupled, OAuth2InfoRow.unapply)

  def accessToken = column[String]("access_token", O.SqlType("varchar(32)"))

  def tokenType = column[Option[String]]("token_type", O.SqlType("varchar(32)"))

  def expiresIn = column[Option[Int]]("expires_in")

  def refreshToken = column[Option[String]]("refresh_token", O.SqlType("varchar(32)"))
}

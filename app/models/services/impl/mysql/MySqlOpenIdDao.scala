package models.services.impl.mysql

import javax.inject.{Inject, Singleton}
import javax.sql.DataSource

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OpenIDInfo
import slick.driver.MySQLDriver.api._
import slick.lifted.{TableQuery, Tag}

import scala.concurrent.ExecutionContext

@Singleton
class MySqlOpenIDInfoDao @Inject()(protected val dataSource: DataSource)(protected implicit val ec: ExecutionContext) extends MySqlAuthInfoDao[OpenIDInfo, OpenIDInfoRow, OpenIDInfoTable] {
  override protected def tableQuery = TableQuery[OpenIDInfoTable]

  override protected def authInfo(row: OpenIDInfoRow): OpenIDInfo = OpenIDInfo(row.id, Map.empty[String, String])

  override protected def row(loginInfo: LoginInfo, authInfo: OpenIDInfo): OpenIDInfoRow = OpenIDInfoRow(
    loginInfo.providerID, loginInfo.providerKey,
    authInfo.id
  )
}

case class OpenIDInfoRow(providerID: String, providerKey: String, id: String) extends AuthInfoRow

class OpenIDInfoTable(tag: Tag) extends AuthInfoTable[OpenIDInfoRow](tag, "openid_infos") {
  def * = (providerID, providerKey, id) <>(OpenIDInfoRow.tupled, OpenIDInfoRow.unapply)

  def id = column[String]("id", O.SqlType("varchar(512)"))
}

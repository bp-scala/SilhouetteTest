package models.services.impl.mysql

import slick.driver.MySQLDriver.api._
import slick.lifted.TableQuery

object MySqlSchema {
  val users = TableQuery[Users]
  val userLogins = TableQuery[UserLoginInfos]
  val passwordInfos = TableQuery[PasswordInfoTable]
  val openIds = TableQuery[OpenIDInfoTable]
  val oauth1s = TableQuery[OAuth1InfoTable]
  val oauth2s = TableQuery[OAuth2InfoTable]

  def statements = Seq(users, userLogins, passwordInfos, openIds, oauth1s, oauth2s) map { _.schema } flatMap { _.createStatements }
}

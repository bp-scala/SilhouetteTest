package models.services.impl.mysql

import slick.driver.MySQLDriver
import slick.driver.MySQLDriver.api._
import slick.lifted.TableQuery

object MySqlSchema {
  private val users = TableQuery[Users]
  private val userLogins = TableQuery[UserLoginInfos]
  private val passwordInfos = TableQuery[PasswordInfoTable]
  private val openIds = TableQuery[OpenIDInfoTable]
  private val oauth1s = TableQuery[OAuth1InfoTable]
  private val oauth2s = TableQuery[OAuth2InfoTable]

  private val schemas: Seq[MySQLDriver.DDL] = Seq(users, userLogins, passwordInfos, openIds, oauth1s, oauth2s) map { _.schema }

  override def toString = ((dropStatements ++ createStatements) mkString ";\n") + ";"

  def createStatements = schemas flatMap { _.createStatements }

  def dropStatements = schemas flatMap { _.dropStatements }
}

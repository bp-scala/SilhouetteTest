package models.services.impl.mysql

import javax.inject.{Inject, Singleton}
import javax.sql.DataSource

import com.mohiva.play.silhouette.api.LoginInfo
import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException
import models.User
import models.services.{UserExists, UserService}
import slick.driver.MySQLDriver.api._
import slick.lifted.Tag

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MySqlUserDao @Inject()(protected val dataSource: DataSource)(protected implicit val ec: ExecutionContext) extends UserService with MySqlDao {
  private val userQuery = TableQuery[Users]
  private val userLoginInfoQuery = TableQuery[UserLoginInfos]
  private val userInsert = userQuery returning userQuery.map(_.id)

  override def retrieve(loginInfo: LoginInfo) = {
    def query(providerID: Rep[String], providerKey: Rep[String]) = userLoginInfoQuery join userQuery filter { case (loginInfos, users) =>
      loginInfos.providerID === providerID && loginInfos.providerKey === providerKey
    } map { case (loginInfos, users) =>
      users
    }

    val action = query(loginInfo.providerID, loginInfo.providerKey).result
    database.run(action) map { _.headOption }
  }

  override def create(loginInfo: LoginInfo, user: User): Future[User] = {
    val action = for {
      userId <- userInsert += user
      _ <- userLoginInfoQuery += UserLoginInfo(userId, loginInfo.providerID, loginInfo.providerKey)
    } yield user.copy(id = userId)
    database.run(action.transactionally) recover {
      case x: MySQLIntegrityConstraintViolationException => throw UserExists(user, loginInfo)
    }
  }

  override def update(user: User): Future[User] = {
    val query = userQuery filter { _.id === user.id }
    val action = query.update(user)
    database.run(action) map { _ => user }
  }
}

class Users(tag: Tag) extends Table[User](tag, "users") {
  def * = (id, displayName, email, avatarURL, fullName).shaped <>(User.tupled, User.unapply)

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def displayName = column[String]("display_name", O.SqlType("varchar(16)"))

  def email = column[String]("email", O.SqlType("varchar(64)"))

  def avatarURL = column[Option[String]]("avatar_url", O.SqlType("varchar(256)"))

  def fullName = column[Option[String]]("full_name", O.SqlType("varchar(128)"))
}

case class UserLoginInfo(userId: Long, providerID: String, providerKey: String)

class UserLoginInfos(tag: Tag) extends Table[UserLoginInfo](tag, "user_login_infos") {
  val users = TableQuery[Users]

  def pk = primaryKey(s"pk_user_login_infos", (providerID, providerKey))

  def fkUserId = foreignKey("fk_user", userId, users)(_.id)

  def userId = column[Long]("user_id")

  def * = (userId, providerID, providerKey).shaped <>(UserLoginInfo.tupled, UserLoginInfo.unapply)

  def providerID = column[String]("provider_id", O.SqlType("varchar(32)"))

  def providerKey = column[String]("provider_key", O.SqlType("varchar(64)"))
}

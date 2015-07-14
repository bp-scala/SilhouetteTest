package models

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

case class TestUser(id: UUID, loginInfo: LoginInfo) extends Identity



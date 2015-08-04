package models

import com.mohiva.play.silhouette.api.Identity

case class User(
  id: Long,
  displayName: Option[String],
  email: Option[String],
  realName: Option[String],
  avatarURL: Option[String]) extends Identity
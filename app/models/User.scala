package models

import com.mohiva.play.silhouette.api.Identity

case class User(
  id: Long,
  displayName: String,
  email: String,
  avatarURL: Option[String],
  fullName: Option[String]) extends Identity
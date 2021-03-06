package forms

import play.api.data.Form
import play.api.data.Forms._

/**
 * The form which handles the sign up process.
 */
object SignUpForm {

  /**
   * A play framework form.
   */
  val form = Form(
    mapping(
      "displayName" -> text,
      "email" -> email,
      "password" -> nonEmptyText,
      "realName" -> text,
      "avatarURL" -> optional(text)
    )(Data.apply)(Data.unapply)
  )

  /**
   * The form data.
   *
   */
  case class Data(
    displayName: String,
    email: String,
    password: String,
    realName: String,
    avatarURL: Option[String])
}

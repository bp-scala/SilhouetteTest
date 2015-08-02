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
      "fullName" -> optional(text),
      "avatarURL" -> optional(text)
    )(Data.apply)(Data.unapply)
  )

  /**
   * The form data.
   *
   * @param fullName The full name of a user.
   * @param email The email of the user.
   * @param password The password of the user.
   */
  case class Data(
    displayName: String,
    email: String,
    password: String,
    fullName: Option[String],
    avatarURL: Option[String])
}

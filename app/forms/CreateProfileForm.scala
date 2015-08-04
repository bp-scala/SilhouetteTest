package forms

import play.api.data.Form
import play.api.data.Forms._

/**
 * The form which handles the sign up process.
 */
object CreateProfileForm {

  /**
   * A play framework form.
   */
  val form = Form(
    mapping(
      "displayName" -> text,
      "email" -> email,
      "fullName" -> text,
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
    fullName: String,
    avatarURL: Option[String])

}

package models.util

import java.text.Normalizer
import java.util.regex.Pattern

object SlugGenerator {

  private val WHITESPACE = Pattern.compile("[\\s]")
  private val NONLATIN = Pattern.compile("[^\\w-]")
  private val specialChars = Map(
    "\\+" -> "plus",
    // Latin-1 Supplement
    "ß" -> "b",
    // Latin Extended-A
    "đ" -> "d",
    "ħ" -> "h",
    "ı" -> "i",
    "ĳ" -> "ij",
    "ĸ" -> "k",
    "ŀ" -> "l",
    "ł" -> "l",
    "ŉ" -> "n",
    "ŋ" -> "n",
    "œ" -> "oe",
    "ŧ" -> "t",
    "ſ" -> "s",
    "€" -> "e",
    "£" -> "l",
    "æ" -> "ae",
    "ø" -> "o",

    // Extras
    "\\." -> "-",
    "_" -> "-"
  )

  private def generateAvailable(name: String, available: (String => Boolean)): String = {
    val baseSlug = generate(name)

    val matching = Stream.from(0) map {
      case 0 => baseSlug
      case n => s"$baseSlug-$n"
    } find available

    matching.head
  }

  private def generate(input: String): String = {
    var slug = input

    // Whitespace
    slug = slug.trim.toLowerCase
    slug = WHITESPACE.matcher(slug).replaceAll("-")

    // Special chars
    slug = Normalizer.normalize(slug, Normalizer.Form.NFD)
    specialChars.foreach {
      case (key, value) => slug = slug.replaceAll(key, value)
    }

    // All other chars...
    slug = NONLATIN.matcher(slug).replaceAll("")

    // Remove extra dashes
    val isDash: (Char => Boolean) = _ == '-'
    slug.replaceAll("(-){2,}", "-").dropWhile(isDash).dropWhileInverse(isDash)
  }

  implicit class SlugifiableString(s: String) {
    def slugify = generate(s)

    def slugify(p: (String => Boolean)) = generateAvailable(s, p)

    def dropWhileInverse(p: Char => Boolean): String = s.dropRight(suffixLength(p, expectTrue = true))

    private def suffixLength(p: Char => Boolean, expectTrue: Boolean): Int = {
      var i = 0
      while (i < s.length && p(s.apply(s.length - i - 1)) == expectTrue) i += 1
      i
    }
  }


}


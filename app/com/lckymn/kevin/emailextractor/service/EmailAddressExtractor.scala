/**
 *
 */
package com.lckymn.kevin.emailextractor.service;

import com.lckymn.kevin.emailextractor.AbstractExtractor
import models.EmailAddress

import scala.util.matching.Regex

/**
 *
 * @author Lee, SeongHyun (Kevin)
 * @version 0.0.1 (2013-03-20)
 */
case class SimpleEmailAddressExtractor(val pattern: Regex) extends AbstractExtractor[EmailAddress] {
  override def extract(value: String): Set[EmailAddress] = {
    extract(value, (email) => EmailAddress(email))
  }
}

object DefaultEmailAddressExtractor extends SimpleEmailAddressExtractor("""([a-zA-Z0-9]+([-_\.\+]+[a-zA-Z0-9]+)*@[a-zA-Z0-9]+([-_]+[a-zA-Z0-9]+)*(?:[.][a-zA-Z0-9]+([-_]+[a-zA-Z0-9]+)*)+)""".r) {

  /* An email address has at most 254 characters (RFC 5321). */
  private val MaxLength = 254

  /* All the characters that the pattern can match. */
  private val candidatePattern = """[-_.+@a-zA-Z0-9]+""".r

  /**
   * The pattern must not run on a long text of these characters. Its groups use recursion, so one such text causes
   * a StackOverflowError, and Pekko then shuts down the JVM. It also takes quadratic time.
   * A match is always inside one candidate, so a limit on the length of the candidates does not lose any valid address.
   */
  override def extract(value: String): Set[EmailAddress] =
    candidatePattern.findAllIn(value)
      .filter(candidate => candidate.length <= MaxLength && candidate.contains('@'))
      .flatMap(super.extract(_))
      .toSet
}

object EmailAddressExtractor {
  def apply() = DefaultEmailAddressExtractor

  def apply(pattern: Regex) = SimpleEmailAddressExtractor(pattern)
}

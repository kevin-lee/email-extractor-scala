package com.lckymn.kevin.emailextractor.service

import hedgehog._
import hedgehog.runner._
import models.EmailAddress

object EmailAddressExtractorSpec extends Properties {

  val value1           = "test@test.com, tere@some.com, sdjsdjk@blah.com"
  val expectedEmailSet = Set(EmailAddress("test@test.com"), EmailAddress("tere@some.com"), EmailAddress("sdjsdjk@blah.com"))

  val value2            =
    "test@test.com dskljsdkjsd;dskjd another@email.com tere@some.com, sdkjdasiuodas sdjsdjk@blah.com. sdkljsdlkjasd"
  val expectedEmailSet2 = Set(
    EmailAddress("test@test.com"),
    EmailAddress("another@email.com"),
    EmailAddress("tere@some.com"),
    EmailAddress("sdjsdjk@blah.com"),
  )

  val value3            =
    "test@test.com dskljsdkjsd;dskjd TEST@TEST.COM another_address-2@email.com tere+blah@some.com, sdkjdasiuodas sdj.sdjk@blah.com. sdkljsdlkjasd"
  val expectedEmailSet3 = Set(
    EmailAddress("test@test.com"),
    EmailAddress("another_address-2@email.com"),
    EmailAddress("tere+blah@some.com"),
    EmailAddress("sdj.sdjk@blah.com"),
  )

  override def tests: List[Test] = List(
    example("""EmailAddressExtractor().extract("") should return Set()""", testEmpty),
    example(s"""EmailAddressExtractor().extract("$value1") should return $expectedEmailSet""", testValue1),
    example(s"""EmailAddressExtractor().extract("$value2") should return $expectedEmailSet2""", testValue2),
    example(
      s"""EmailAddressExtractor("@".r).extract("$value2") should return Set(EmailAddress("@"))""",
      testCustomPattern
    ),
    example(s"""EmailAddressExtractor().extract("$value3") should return $expectedEmailSet3""", testValue3),
    example("EmailAddressExtractor().extract(a very long text of separated words) should not overflow the stack", testLongSeparatedText),
    example("EmailAddressExtractor().extract(a very long word) should find the email address after it", testLongWord),
    example("EmailAddressExtractor().extract(an email address of 254 characters) should return it", testMaxLength),
    example("EmailAddressExtractor().extract(an email address of 255 characters) should return Set()", testOverMaxLength),
    property("EmailAddressExtractor().extract(text) should return all the email addresses in the text", testExtractAll),
    property("EmailAddressExtractor().extract(text without @) should return Set()", testNoAtSign),
  )

  def testLongSeparatedText: Result =
    EmailAddressExtractor().extract("a." * 1000000 + "a") ==== Set.empty[EmailAddress]

  def testLongWord: Result =
    EmailAddressExtractor().extract("a" * 1000000 + " test@test.com") ==== Set(EmailAddress("test@test.com"))

  private def emailAddressOfLength(length: Int): String = {
    val domain = "@test.com"
    "a" * (length - domain.length) + domain
  }

  def testMaxLength: Result = {
    val emailAddress = emailAddressOfLength(254)
    EmailAddressExtractor().extract(emailAddress) ==== Set(EmailAddress(emailAddress))
  }

  def testOverMaxLength: Result =
    EmailAddressExtractor().extract(emailAddressOfLength(255)) ==== Set.empty[EmailAddress]

  def testEmpty: Result =
    EmailAddressExtractor().extract("") ==== Set.empty[EmailAddress]

  def testValue1: Result =
    EmailAddressExtractor().extract(value1) ==== expectedEmailSet

  def testValue2: Result =
    EmailAddressExtractor().extract(value2) ==== expectedEmailSet2

  def testCustomPattern: Result =
    EmailAddressExtractor("@".r).extract(value2) ==== Set(EmailAddress("@"))

  def testValue3: Result =
    EmailAddressExtractor().extract(value3) ==== expectedEmailSet3

  private val genWord: Gen[String] = Gen.string(Gen.alphaNum, Range.linear(1, 10))

  private def genSegments(separators: List[Char]): Gen[String] =
    for {
      head <- genWord
      tail <- (for {
                separator <- Gen.element1(separators.head, separators.tail: _*)
                word      <- genWord
              } yield s"$separator$word").list(Range.linear(0, 3))
    } yield (head :: tail).mkString

  private val genEmailAddress: Gen[String] =
    for {
      localPart  <- genSegments(List('-', '_', '.', '+'))
      domainPart <- genSegments(List('-', '_'))
      tlds       <- genSegments(List('-', '_')).list(Range.linear(1, 3))
    } yield s"$localPart@$domainPart.${tlds.mkString(".")}"

  def testExtractAll: Property =
    for {
      emails <- genEmailAddress.list(Range.linear(1, 10)).log("emails")
      filler <- genWord.list(Range.linear(1, 5)).log("filler")
    } yield {
      val text = emails.zipAll(filler, "", "").map { case (email, word) => s"$email $word" }.mkString(", ")
      EmailAddressExtractor().extract(text) ==== emails.map(EmailAddress(_)).toSet
    }

  def testNoAtSign: Property =
    for {
      text <- Gen.string(Gen.unicode, Range.linear(0, 100)).map(_.filterNot(_ == '@')).log("text")
    } yield EmailAddressExtractor().extract(text) ==== Set.empty[EmailAddress]
}

package controllers

import com.lckymn.kevin.emailextractor.Extractor
import com.lckymn.kevin.emailextractor.service._
import com.lckymn.kevin.emailextractor.util.CommonUtil._
import models.EmailAddress
import play.api.libs.json._
import play.api.mvc._

import javax.inject.{Inject, Singleton}

/**
  *
  * @author Lee, SeongHyun (Kevin)
  * @version 0.0.1 (2013-03-20)
  */
@Singleton
class EmailAddressExtractorController @Inject() (
  val controllerComponents: ControllerComponents
) extends BaseController {

  val emailAddressExtractor: Extractor[EmailAddress] = EmailAddressExtractor()

  private val inputValueReads: Reads[String] = (__ \ "inputValue").read[String]

  def extract: Action[JsValue] = Action(parse.json) { implicit request =>
    request.body.validate(inputValueReads).map(Option(_)).map(_.filter(_.nonEmpty)).map {
      case None =>
        Ok(toResultJson("You did not submit any text."))

      case Some(inputValue) =>
        val resultSet = emailAddressExtractor.extract(inputValue)
                                             .map(_.emailAddress)
        val size = resultSet.size
        Ok(toResultJson(isSuccess = true, makeMessage(size), size, resultSet.mkString(", ")))
    }.recoverTotal {
      e => BadRequest("Detected error:" + JsError.toJson(e))
    }
  }
}

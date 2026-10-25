package controllers

import com.lckymn.kevin.emailextractor.Extractor
import com.lckymn.kevin.emailextractor.service._
import com.lckymn.kevin.web.JavascriptRoutable
import models.EmailAddress
import org.webjars.play.WebJarsUtil
import play.api.mvc._
import play.api.routing.JavaScriptReverseRoute

import javax.inject.{Inject, Singleton}

/**
 *
 * @author Lee, SeongHyun (Kevin)
 * @version 0.0.1 (2013-03-20)
 */
@Singleton
class Application @Inject() (
  val controllerComponents: ControllerComponents
)(implicit webJarsUtil: WebJarsUtil)
    extends BaseController
    with JavascriptRoutable {

  val javascriptReverseRoute: List[JavaScriptReverseRoute] =
    List(controllers.routes.javascript.EmailAddressExtractorController.extract)

  val emailAddressExtractor: Extractor[EmailAddress] = EmailAddressExtractor()

  def index: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.index("0.0.9"))
  }
}

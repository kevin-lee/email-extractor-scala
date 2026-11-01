package test

import com.lckymn.kevin.emailextractor.util.CommonUtil._
import hedgehog._
import hedgehog.runner._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.http.Writeable
import play.api.mvc.{CookieHeaderEncoding, Request, SessionCookieBaker}
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.Future

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
object ApplicationSpec extends Properties {

  override def tests: List[Test] = List(
    example("send 404 on a bad request", testBadRequest),
    example("render the index page", testIndexPage),
    example("post no input JSON at all", testNoInputJson),
    example("post with empty input JSON value", testEmptyInputJsonValue),
    example("post with JSON containing no email address", testJsonWithNoEmailAddress),
    example("post with JSON containing some email addresses", testJsonWithSomeEmailAddresses),
    example("post with a session cookie but no CSRF token", testPostWithoutCsrfToken),
    example("post with the session cookie and the CSRF token from the index page", testPostWithCsrfToken),
    example("the session cookie has the Secure flag", testSessionCookieSecure),
    example("redirect the context path with a trailing slash", testContextWithSlash),
    example("keep the query in the redirect of the context path", testContextWithSlashAndQuery),
    example("serve the context path with no trailing slash", testContextWithoutSlash),
    example("do not redirect a POST to the context path with a trailing slash", testPostContextWithSlash),
    example("do not redirect other paths with a trailing slash", testOtherPathsWithSlash),
    example("the JavaScript route for extracting has the context path", testJavascriptRouteWithContext),
  )

  private def withApp(f: Application => Result): Result = {
    val app = new GuiceApplicationBuilder().build()
    running(app)(f(app))
  }

  private val context = "/email-extractor-scala"

  private def withContextApp(f: Application => Result): Result = {
    val app = new GuiceApplicationBuilder().configure("play.http.context" -> context).build()
    running(app)(f(app))
  }

  private def withRoute[A: Writeable](app: Application, request: Request[A])(
    f: Future[play.api.mvc.Result] => Result
  ): Result =
    route(app, request).fold(Result.failure.log(s"No route for ${request.method} ${request.uri}"))(f)

  def testSessionCookieSecure: Result = withApp { app =>
    withRoute(app, FakeRequest(GET, "/")) { home =>
      app.injector.instanceOf[SessionCookieBaker].encodeAsCookie(session(home)).secure ==== true
    }
  }

  def testContextWithSlash: Result = withContextApp { app =>
    withRoute(app, FakeRequest(GET, s"$context/")) { result =>
      Result.all(List(status(result) ==== MOVED_PERMANENTLY, redirectLocation(result) ==== Some(context)))
    }
  }

  def testContextWithSlashAndQuery: Result = withContextApp { app =>
    withRoute(app, FakeRequest(GET, s"$context/?a=1")) { result =>
      redirectLocation(result) ==== Some(s"$context?a=1")
    }
  }

  def testContextWithoutSlash: Result = withContextApp { app =>
    withRoute(app, FakeRequest(GET, context)) { result =>
      status(result) ==== OK
    }
  }

  def testPostContextWithSlash: Result = withContextApp { app =>
    withRoute(
      app,
      FakeRequest(POST, s"$context/").withJsonBody(Json.obj("inputValue" -> "kevin@test.com"))
    ) { result =>
      status(result) ==== NOT_FOUND
    }
  }

  def testOtherPathsWithSlash: Result = withContextApp { app =>
    Result.all(
      List(s"$context/boum/", "//example.com/", "/boum/").map { path =>
        withRoute(app, FakeRequest(GET, path)) { result =>
          redirectLocation(result) ==== None
        }
      }
    )
  }

  private val javascriptRouteUrlPattern = """method:"POST", url:"([^"]+)"""".r

  /** The routes prefix is global. The app with no context runs first, so that the context app starts with the default prefix. */
  def testJavascriptRouteWithContext: Result = {
    val defaultPrefixSet = withApp { app =>
      withRoute(app, FakeRequest(GET, "/"))(home => status(home) ==== OK)
    }
    val routeInContext = withContextApp { app =>
      withRoute(app, FakeRequest(GET, s"$context/assets/javascripts/routes")) { routes =>
        javascriptRouteUrlPattern.findFirstMatchIn(contentAsString(routes)).map(_.group(1)) match {
          case Some(url) =>
            withRoute(
              app,
              FakeRequest(POST, url).withJsonBody(Json.obj("inputValue" -> "kevin@test.com"))
            ) { result =>
              Result.all(List(url ==== context, status(result) ==== OK))
            }

          case None =>
            Result.failure.log("The JavaScript routes have no POST route.")
        }
      }
    }
    Result.all(List(defaultPrefixSet, routeInContext))
  }

  def testBadRequest: Result = withApp { app =>
    val result = route(app, FakeRequest(GET, "/boum")).map(status(_))
    result ==== Some(NOT_FOUND)
  }

  def testIndexPage: Result = withApp { app =>
    val home = route(app, FakeRequest(GET, "/")).get

    Result.all(
      List(
        status(home) ==== OK,
        contentType(home) ==== Some("text/html"),
        Result.assert(contentAsString(home).contains("<title>Extract Email Addresses</title>")),
      )
    )
  }

  def testNoInputJson: Result = withApp { app =>
    val result = route(app, FakeRequest(POST, "/")).get

    Result.all(
      List(
        status(result) ==== UNSUPPORTED_MEDIA_TYPE,
        contentType(result) ==== Some("text/html"),
      )
    )
  }

  def testEmptyInputJsonValue: Result = withApp { app =>
    val result = route(
      app,
      FakeRequest(POST, "/")
        .withJsonBody(Json.obj("inputValue" -> ""))
    ).get

    Result.all(
      List(
        status(result) ==== OK,
        contentType(result) ==== Some("application/json"),
        Json.parse(contentAsString(result)) ==== Json.parse(
          s"""
              {
                "success": false,
                "message": "You did not submit any text.",
                "howMany": 0,
                "result": ""
              }
              """
        ),
      )
    )
  }

  def testJsonWithNoEmailAddress: Result = withApp { app =>
    val result = route(
      app,
      FakeRequest(POST, "/")
        .withJsonBody(Json.obj("inputValue" -> "This doesn't contain any email addresses."))
    ).get

    val resultMessage = "There is no email address found."
    val size          = 0

    Result.all(
      List(
        status(result) ==== OK,
        contentType(result) ==== Some("application/json"),
        Json.parse(contentAsString(result)) ==== Json.parse(
          s"""
              {
                "success": true,
                "message": "${resultMessage}",
                "howMany": ${size},
                "result": ""
              }
              """
        ),
      )
    )
  }

  def testJsonWithSomeEmailAddresses: Result = withApp { app =>
    val first  = "kevin.lee@some.fake.testemailaddress.com"
    val second = "another@email.address.cc"
    val third  = "the.third.one@test.com"
    val result = route(
      app,
      FakeRequest(POST, "/")
        .withJsonBody(
          Json.obj("inputValue" -> s"This contains some email addresses. ${first}, ${second} blah blah ${third}")
        )
    ).get

    val resultSet = Set(first, second, third)
    val size      = resultSet.size

    Result.all(
      List(
        status(result) ==== OK,
        contentType(result) ==== Some("application/json"),
        Json.parse(contentAsString(result)) ==== Json.parse(
          s"""
                {
                "success": true,
                "message": "${makeMessage(size)}",
                "howMany": ${size},
                "result": "${resultSet.mkString(", ")}"
                }
                """
        ),
      )
    )
  }

  private val csrfTokenPattern = """<meta name="csrf-token" content="([^"]+)">""".r

  /** Makes a POST request that carries the session of the given result, as a browser does.
    * FakeRequest does not read the session from the Cookie header, so the session is set with withSession.
    * The Cookie header is still necessary because the CSRF filter checks only requests that have it.
    */
  private def postWithSessionOf(app: Application, home: scala.concurrent.Future[play.api.mvc.Result]) = {
    val sessionCookie = app.injector.instanceOf[SessionCookieBaker].encodeAsCookie(session(home))
    val cookieHeader  = app.injector.instanceOf[CookieHeaderEncoding].encodeCookieHeader(Seq(sessionCookie))
    FakeRequest(POST, "/")
      .withSession(session(home).data.toSeq: _*)
      .withHeaders(COOKIE -> cookieHeader)
      .withJsonBody(Json.obj("inputValue" -> "kevin@test.com"))
  }

  def testPostWithoutCsrfToken: Result = withApp { app =>
    val home   = route(app, FakeRequest(GET, "/")).get
    val result = route(app, postWithSessionOf(app, home)).get

    status(result) ==== FORBIDDEN
  }

  def testPostWithCsrfToken: Result = withApp { app =>
    val home      = route(app, FakeRequest(GET, "/")).get
    val csrfToken = csrfTokenPattern.findFirstMatchIn(contentAsString(home)).map(_.group(1))

    csrfToken match {
      case Some(token) =>
        val result = route(app, postWithSessionOf(app, home).withHeaders("Csrf-Token" -> token)).get

        Result.all(
          List(
            status(result) ==== OK,
            (Json.parse(contentAsString(result)) \ "result").asOpt[String] ==== Some("kevin@test.com"),
          )
        )

      case None =>
        Result.failure.log("The index page has no CSRF token.")
    }
  }
}

package scenarios

import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import utils.Environment

object S2S_Token {

  val RpeAPIURL = Environment.rpeAPIURL
  val IdamAPIUrl = Environment.idamAPIURL

  val adminUserFeeder = csv("AdminUsers.csv").circular

  val clientSecret = ConfigFactory.load.getString("auth.clientSecret")

  val GetServiceToken =

    exec(http("XUIDataPrep_000_ServiceToken")
      .post(RpeAPIURL + "/testing-support/lease")
      .body(StringBody("""{"microservice":"rd_professional_api"}""")).asJson
      .check(regex("(.+)").saveAs("serviceToken")))

    .pause(1)

  val GetBearerToken =

    feed(adminUserFeeder)
      .exec(http("XUIDataPrep_000_AuthToken")
        .post(IdamAPIUrl + "/o/token")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .formParam("grant_type", "password")
        .formParam("username", "${adminUsername}")
        .formParam("password", "${adminPassword}")
        .formParam("client_id", "rd-professional-api")
        .formParam("client_secret", clientSecret)
        .formParam("scope", "openid profile roles openid roles profile create-user manage-user")
        .check(jsonPath("$.access_token").saveAs("bearerToken")))

    .pause(1)

}
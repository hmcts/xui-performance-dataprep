package scenarios

import java.io.{BufferedWriter, FileWriter}
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import utils.Environment

import scala.util.Random


object XUI_DataPrep {

  val ManageOrgURL = Environment.manageOrgURL
  val IdamAPIUrl = Environment.idamAPIURL
  val PrdAPIUrl = Environment.prdAPIURL

  val rnd = new Random()

  def randomString(length: Int) = {
    rnd.alphanumeric.filter(_.isLetter).take(length).mkString
  }

  val paymentAccount1 = "PBA0066906"
  val paymentAccount2 = "PBA0077597"
  //TODO: Update the orgName below to set the name of the org, based on the service (e.g. prl/div/nfd/pro/fpl, etc)
  val orgName = "perf-prl-" + randomString(5)

  val orgDetailsFeeder = Iterator.continually(Map(
    "orgManagerEmail" -> (orgName + "-su@mailinator.com"),
    "orgName" -> orgName))

  /*======================================================================================
  * Create Org Manager with empty roles
  ======================================================================================*/

  val createOrgManager =

    exec(_.setAll(
      "SRAId" -> ("TRA" + randomString(5)),
      "CompanyNumber" -> randomString(8),
      "CompanyURL" -> ("www.perf-" + randomString(5) + ".com"),
      "PaymentAccount1" -> paymentAccount1,
      "PaymentAccount2" -> paymentAccount2))

    .feed(orgDetailsFeeder)
      .exec(http("XUIDataPrep_010_CreateOrgManager")
        .post(IdamAPIUrl + "/testing-support/accounts")
        .header("Content-Type", "application/json")
        .body(ElFileBody("bodies/CreateOrgManager.json"))
        .check(status is 201))

  /*======================================================================================
  * Create a new Organisation with the Org Manager
  ======================================================================================*/

  val createOrg =

    exec(S2S_Token.GetServiceToken)

    .exec(http("XUIDataPrep_020_CreateOrg")
      .post(PrdAPIUrl + "/refdata/external/v1/organisations")
      .header("Content-Type", "application/json")
      .header("ServiceAuthorization", "${serviceToken}")
      .body(ElFileBody("bodies/CreateOrg.json"))
      .check(jsonPath("$.organisationIdentifier").saveAs("orgId"))
      .check(status in (200,201)))

  /*======================================================================================
  * Approve the new organisation (as an admin user)
  ======================================================================================*/

  val approveOrg =

    exec(S2S_Token.GetBearerToken)

    .exec(http("XUIDataPrep_030_ApproveOrg")
      .put(PrdAPIUrl + "/refdata/internal/v1/organisations/${orgId}")
      .header("Content-Type", "application/json")
      .header("ServiceAuthorization", "${serviceToken}")
      .header("Authorization", "Bearer ${bearerToken}")
      .body(ElFileBody("bodies/ApproveOrg.json"))
      .check(jsonPath("$.organisationIdentifier"))
      .check(status in (200,201)))

  /*======================================================================================
  * Add solicitor users
  ======================================================================================*/

  val AddUser =

    exec(http("XUIDataPrep_040_CreateUser")
      .post(IdamAPIUrl + "/testing-support/accounts")
      .header("Content-Type", "application/json")
      .body(ElFileBody("bodies/CreateUser.json"))
      .check(status is 201))

    .exec(http("XUIDataPrep_050_AddUser${count}")
      .put(PrdAPIUrl + "/refdata/internal/v1/organisations/${orgId}/users/")
      .header("Content-Type", "application/json")
      .header("ServiceAuthorization", "${serviceToken}")
      .header("Authorization", "Bearer ${bearerToken}")
      .body(ElFileBody("bodies/AddUser.json"))
      .check(jsonPath("$.userIdentifier"))
      .check(status in (200,201)))

    .exec { session =>
      val fw = new BufferedWriter(new FileWriter("gatling/output/UserDetails.csv", true))
      try {
        fw.write(session("orgName").as[String] + "," + session("orgId").as[String] + "," + session("orgManagerEmail").as[String] + "," +
          session("orgName").as[String] + "-user" + session("count").as[String] + "@mailinator.com,Pass19word\r\n")
      } finally fw.close()
      session
    }

}
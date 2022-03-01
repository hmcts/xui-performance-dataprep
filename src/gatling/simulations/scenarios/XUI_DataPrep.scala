package scenarios

import java.io.{BufferedWriter, FileWriter}

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import utils.Environment


object XUI_DataPrep {

  val ManageOrgURL = Environment.manageOrgURL

  val feeder = csv("userid-increment.csv").circular
  val adminFeeder = csv("AdminUsers.csv").circular

  private val rng: Random = new Random()
  private def sRAId(): String = rng.alphanumeric.take(15).mkString
  private def companyNumber(): String = rng.alphanumeric.take(8).mkString
  private def companyURL(): String = rng.alphanumeric.take(15).mkString
  private def paymentAccount1(): String = rng.alphanumeric.take(7).mkString
  private def paymentAccount2(): String = rng.alphanumeric.take(7).mkString

  
  /*======================================================================================
*Business process : Below scrit  is to create Org, Approve Org and manage Org like invote users
======================================================================================*/
  
  /*======================================================================================
*Business process : Below method is to create user with empty roles and bu using this user organisation will be created in subsequent requents
======================================================================================*/

val createSuperUser=

  feed(Feeders.createDynamicDataFeeder)
  
  .exec(http("XUI_CreateSuperUser")
    .post(idamAPI+"/testing-support/accounts")
    .header("Content-Type", "application/json")
    .body(StringBody("{\"email\": \"${generatedEmail}\", \"forename\": \"Vijay\", \"password\": \"Pass19word\", \"surname\": \"Vykuntam\"}"))
    .check(status is 201))
  
/*======================================================================================
*Business process : Below method is to create Organisation with the user created from above request
======================================================================================*/

val createOrg=

  exec(_.setAll(
    ("SRAId", sRAId()),
    ("CompanyNumber", companyNumber()),
    ("CompanyURL", companyURL()),
    ("PaymentAccount1",paymentAccount1()),
    ("PaymentAccount2",paymentAccount2()),
  ))

  .exec(http("RD15_External_CreateOrganization")
    .post(prdUrl+"/refdata/external/v1/organisations")
    .header("ServiceAuthorization", "Bearer ${s2sToken}")
    .body(StringBody("{\n   \"name\": \"${organisationName}\",\n   \"sraId\": \"TRA${SRAId}\",\n   \"sraRegulated\": true,\n   \"companyNumber\": \"${CompanyNumber}\",\n" +
      "\"companyUrl\": \"www.tr${CompanyURL}.com\",\n   \"superUser\": {\n       \"firstName\": \"Vijay\",\n       \"lastName\": \"Vykuntam\",\n" +
      "\"email\": \"${generatedEmail}\"\n,\n        \"jurisdictions\": [\n    {\n      \"id\": \"DIVORCE\"\n    },\n    {\n      \"id\": \"SSCS\"\n    },\n    {\n      \"id\": \"PROBATE\"\n    },\n    {\n      \"id\": \"PUBLICLAW\"\n    },\n    {\n      \"id\": \"BULK SCANNING\"\n    },\n    {\n      \"id\": \"IA\"\n    },\n    {\n      \"id\": \"CMC\"\n    },\n    {\n      \"id\": \"EMPLOYMENT\"\n    },\n    {\n      \"id\": \"Family public law and adoption\"\n    },\n    {\n      \"id\": \"Civil enforcement and possession\"\n    }\n  ]   },\n   \"paymentAccount\": [\n\n          \"PBA${PaymentAccount1}\",\"PBA${PaymentAccount2}\"\n\n   ],\n" +
      "\"contactInformation\": [\n       {\n           \"addressLine1\": \"4\",\n           \"addressLine2\": \"Hibernia Gardens\",\n           \"addressLine3\": \"Maharaj road\",\n" +
      "\"townCity\": \"Hounslow\",\n           \"county\": \"middlesex\",\n           \"country\": \"UK\",\n           \"postCode\": \"TW3 3SD\",\n           \"dxAddress\": [\n" +
      "{\n                   \"dxNumber\": \"DX 1121111990\",\n                   \"dxExchange\": \"112111192099908492\"\n               }\n           ]\n       }\n   ]\n}"))
    .header("Content-Type", "application/json")
    .check(jsonPath("$.organisationIdentifier").saveAs("organisationRefCode"))
    .check(status in (200,201)))
  
  /*======================================================================================
  *Business process : Below method is to create approve the organisation Home page
  ======================================================================================*/

  val approveOrgHomePage=
    exec(http("EXUI_AO_005_Homepage")
      .get(url_approve + "/")
      .check(status.is(200)))

    .exec(http("request_6")
      .get(url_approve+"/api/environment/config")
      .check(status.is(200)))

    .exec(http("request_12")
      .get(url_approve+"/auth/isAuthenticated")
      .check(status.is(200)))

    .exec(http("request_8")
      .get(url_approve+"/api/user/details")
      .check(status.is(200))
      .check(regex("oauth2/callback&state=(.*)&nonce").saveAs("state"))
      .check(regex("&nonce=(.*)&response_type").saveAs("nonce"))
      .check(css("input[name='_csrf']", "value").saveAs("csrfToken")))
  
  /*======================================================================================
  *Business process : Below method is for approve organisation login requests
  ======================================================================================*/

  val approveOrganisationlogin =

    feed(adminFeeder)

    .exec(http("EXUI_AO_005_Login")
      .post(IdamUrl + "/login?client_id=xuiaowebapp&redirect_uri="+url_approve+"/oauth2/callback&state=${state}&nonce=${nonce}&response_type=code&scope=profile%20openid%20roles%20manage-user%20create-user&prompt=")
      .headers(headers_login)
      .formParam("username", "${adminEmail}")
      .formParam("password", "${adminPassword}")
      .formParam("save", "Sign in")
      .formParam("selfRegistrationEnabled", "false")
      .formParam("_csrf", "${csrfToken}")
      .check(status.is(200)))

    .exec(http("request_5")
      .get( "/api/environment/config")
      .check(status.is(200)))

    .exec(http("request_6")
      .get( "/api/user/details")
      .check(status.is(200)))

    .exec(http("request_7")
      .get("/auth/isAuthenticated")
      .check(status.is(200)))

    .exec(http("request_9")
      .get( "/api/organisations?status=ACTIVE")
      .check(status.is(200)))

    .exec(http("EXUI_AO_010_Login")
      .get(url_approve + "/api/organisations?status=PENDING")
      .check(status.is(200)))

    .exec(getCookieValue(CookieKey("XSRF-TOKEN").withDomain("administer-orgs.perftest.platform.hmcts.net").saveAs("XSRFToken")))
  
  /*======================================================================================
    *Business process : Below method is to create approve the organisation approave
    * This will approve the organisation created which means we can login to manage org to invite users
    ======================================================================================*/
  val approveOrganisationApprove =

    exec(http("request_3")
      .get("/auth/isAuthenticated")
      .check(status.in(200,304)))

    .exec(http("request_4")
      .get("/auth/isAuthenticated")
      .check(status.in(200,304)))

    .exec(http("EXUI_AO_Approve")
      .put(url_approve+"/api/organisations/${organisationRefCode}")
      .headers(headers_approve)
      .header("X-XSRF-TOKEN", "${XSRFToken}")
      .body(ElFileBody("AO.json")).asJson
      .check(status.is(200))
      .check(status.saveAs("aostatusvalue")))
    
    .doIf(session=>session("aostatusvalue").as[String].contains("200")) {
      exec { session =>
        val fw = new BufferedWriter(new FileWriter("OrgDetails.csv", true))
        try {
          fw.write(session("organisationName").as[String] + "," + session("organisationRefCode").as[String] + "," + session("generatedEmail").as[String] + "\r\n")
        } finally fw.close()
        session
      }
    }

  /*======================================================================================
  *Business process : Below method is to create approve the organisation approave
  * This will approve the organisation logout
  ======================================================================================*/

  val approveOrganisationLogout =

    exec(http("EXUI_AO_005_Logout")
      .get(url_approve + "/api/logout")
      .check(status.is(200)))
  
  /*======================================================================================
  *Business process : Below method is to create manage organisation
  * below request is for manage organisation home page
  ======================================================================================*/

  val manageOrgHomePage =

    exec(http("EXUI_MO_005_Homepage")
      .get(url_mo + "/")
      .check(status.is(200)))
      
    .exec(http("EXUI_MO_010_Homepage")
      .get("/auth/login")
      .check(status.is(200))
      .check(regex("&state=(.*)&client_id").saveAs("state"))
      .check(css("input[name='_csrf']", "value").saveAs("csrfToken"))
    )
  
  /*======================================================================================
  *Business process : Below method is to create manage organisation
  * below request is for manage organisation login page
  ======================================================================================*/

  val manageOrganisationLogin =

    exec(http("EXUI_MO_005_Login")
      .post(IdamUrl + "/login?response_type=code&redirect_uri=https%3a%2f%2f"+baseDomainOrg+"%2foauth2%2fcallback&scope=profile%20openid%20roles%20manage-user%20create-user%20manage-roles&state=${state}&client_id=xuimowebapp")
      .formParam("username", "${generatedEmail}")
      .formParam("password", "Pass19word")
      .formParam("save", "Sign in")
      .formParam("selfRegistrationEnabled", "false")
      .formParam("_csrf", "${csrfToken}")
      .check(status.in(200, 302)))

    .exec(http("EXUI_MO_020_Login")
      .get(url_mo + "/api/organisation/")
      .check(status.in(200, 302,304)))


    .exec(http("EXUI_MO_065_050_Login")
      .get(url_mo + "/api/user/details")
      .check(status.in(200, 302,304)))

  val usersPage =

    exec(http("EXUI_MO_005_Userspage")
      .get(url_mo + "/api/userList")
      .check(status.is(200)))

  /*======================================================================================
        *Business process : Below method is to invite users
        * below request is for manage organisation click on invite user page in
        ======================================================================================*/

  val inviteUserPage =

    exec(http("EXUI_MO_005_InviteUserpage")
      .get(url_mo + "/api/jurisdictions")
      .check(status.in(200,304)))
  
  /*======================================================================================
        *Business process : Below method is to send invitations
        * below request is for manage organisation home page
        ======================================================================================*/

  val sendInvitation =

    exec(http("XUI_CreateSuperUser")
      .post(idamAPI+"/testing-support/accounts")
      .header("Content-Type", "application/json")
      .body(StringBody("{\"email\": \"${organisationName}-user${n}@mailinator.com\", \"forename\": \"VUser\", \"password\": \"Pass19word\", \"surname\": \"VykUser\"}"))
      .check(status is 201))

    .exec(http("EXUI_MO_005_SendInvitation")
      .post(url_mo + "/api/inviteUser")
      .body(ElFileBody("MO.json")).asJson
      .check(status.is(200)))
      .exitHereIfFailed





}
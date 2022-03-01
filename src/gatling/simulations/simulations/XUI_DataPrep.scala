package simulations

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import scenarios._
import utils.Environment
import io.gatling.core.pause.PauseType

import scala.concurrent.duration._

class XUI_DataPrep extends Simulation {

	/* ADDITIONAL COMMAND LINE ARGUMENT OPTIONS */
	val debugMode = System.getProperty("debug", "off") //runs a single user e.g. ./gradle gatlingRun -Ddebug=on (default: off)
	/* ******************************** */

	val rampUpDurationMins = 5
	val numberOfOrgsToCreate = 1

	//Determine the pause pattern to use:
	//Performance test = use the pauses defined in the script
	//Pipeline = override pauses in the script with a fixed value (pipelinePauseMillis)
	//Debug mode = disable all pauses
	val pauseOption:PauseType = debugMode match{
		case "off" => constantPauses
		case _ => disabledPauses
	}

	////Manage-org protocol
  val httpProtocol = Environment.HttpProtocol
		.inferHtmlResources()
		.silentResources

	before{
		println(s"Debug Mode: ${debugMode}")
	}

	/*===============================================================================================
	* below scenario is for create org, approve org and manage org related business process
	 ==================================================================================================*/

	val EXUIScn = scenario("EXUI").repeat(1)
	 {
		 exitBlockOnFail {
			 exec(_.set("env", s"${env}"))
			 .exec(
				 S2SHelper.S2SAuthToken,
				 XUI_DataPrep.createSuperUser,
				 XUI_DataPrep.createOrg,
				 XUI_DataPrep.approveOrgHomePage,
				 XUI_DataPrep.approveOrganisationlogin,
				 XUI_DataPrep.approveOrganisationApprove,
				 XUI_DataPrep.approveOrganisationLogout,
				 XUI_DataPrep.manageOrgHomePage,
				 XUI_DataPrep.manageOrganisationLogin,
				 XUI_DataPrep.usersPage,
				 XUI_DataPrep.inviteUserPage
			 )
			 .repeat(4, "n") {
				 exec(XUI_DataPrep.sendInvitation)
			 }
			 .exec(
				 XUI_DataPrep.manageOrganisationLogout
			 )
		 }
	 }



	setUp(
		EXUIScn.inject(rampUsers(numberOfOrgsToCreate) during (rampUpDurationMins minutes)).pauses(pauseOption)
	).protocols(httpProtocol)
		.assertions(forAll.successfulRequests.percent.gte(80))

 



}

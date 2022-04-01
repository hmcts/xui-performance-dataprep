package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef.http
import io.gatling.core.scenario.Simulation
import scenarios._
import scala.concurrent.duration._

class XUI_DataPrep extends Simulation {

	val rampUpDurationMins = 3
	val numberOfOrgsToCreate = 10
	val numberOfSolicitorsPerOrg = 4

	/*======================================================================================
	* Creates Org with an Org Manager (SuperUser/AdminUser) and Adds Solicitors
	======================================================================================*/

	val XUIDataPrepScenario = scenario("XUI Data Prep").repeat(1)
	 {
		 exitBlockOnFail {
			 exec(
				 XUI_DataPrep.CreateOrgManager,
				 XUI_DataPrep.CreateOrg,
				 XUI_DataPrep.ApproveOrg
			 )
			 .repeat(numberOfSolicitorsPerOrg, "count") {
				 exec(XUI_DataPrep.AddUser)
			 }
		 }
	 }


	setUp(
		XUIDataPrepScenario.inject(rampUsers(numberOfOrgsToCreate) during (rampUpDurationMins minutes))
	).protocols(http)
		.assertions(global.successfulRequests.percent.is(100))

}

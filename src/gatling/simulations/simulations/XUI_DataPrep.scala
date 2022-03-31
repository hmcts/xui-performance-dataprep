package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef.http
import io.gatling.core.scenario.Simulation
import scenarios._


import scala.concurrent.duration._

class XUI_DataPrep extends Simulation {

	val rampUpDurationMins = 5
	val numberOfOrgsToCreate = 1
	val numberOfSolicitorsPerOrg = 4

	/*======================================================================================
	* Creates Org with an Org Manager (SuperUser/AdminUser) and Adds Solicitors
	======================================================================================*/

	val XUIDataPrepScenario = scenario("XUI Data Prep").repeat(1)
	 {
		 exitBlockOnFail {
			 exec(
				 XUI_DataPrep.createOrgManager,
				 XUI_DataPrep.createOrg,
				 XUI_DataPrep.approveOrg
			 )
			 .repeat(numberOfSolicitorsPerOrg, "count") {
				 exec(XUI_DataPrep.AddUser)
			 }
		 }
	 }


	setUp(
		XUIDataPrepScenario.inject(rampUsers(numberOfOrgsToCreate) during (rampUpDurationMins minutes)).pauses(pauseOption)
	).protocols(http)
		.assertions(forAll.successfulRequests.percent.gte(80))

 



}

package exchangeapi

import java.time._

import com.horizon.exchangeapi._
import com.horizon.exchangeapi.tables._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.native.Serialization.write
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import scala.collection.immutable._
import scalaj.http._

/**
 * Tests for the /workloads routes. To run
 * the test suite, you can either:
 *  - run the "test" command in the SBT console
 *  - right-click the file in eclipse and chose "Run As" - "JUnit Test"
 *
 * clear and detailed tutorial of FunSuite: http://doc.scalatest.org/1.9.1/index.html#org.scalatest.FunSuite
 */
@RunWith(classOf[JUnitRunner])
class WorkloadsSuite extends FunSuite {

  val localUrlRoot = "http://localhost:8080"
  val urlRoot = sys.env.getOrElse("EXCHANGE_URL_ROOT", localUrlRoot)
  val runningLocally = (urlRoot == localUrlRoot)
  val ACCEPT = ("Accept","application/json")
  val ACCEPTTEXT = ("Accept","text/plain")
  val CONTENT = ("Content-Type","application/json")
  val CONTENTTEXT = ("Content-Type","text/plain")
  val orgid = "WorkloadsSuiteTests"
  val authpref=orgid+"/"
  val URL = urlRoot+"/v1/orgs/"+orgid
  val user = "9999"
  val orguser = authpref+user
  val pw = user+"pw"
  val USERAUTH = ("Authorization","Basic "+orguser+":"+pw)
  val user2 = "10000"
  val orguser2 = authpref+user2
  val pw2 = user2+"pw"
  val USER2AUTH = ("Authorization","Basic "+orguser2+":"+pw2)
  val rootuser = Role.superUser
  val rootpw = sys.env.getOrElse("EXCHANGE_ROOTPW", "")      // need to put this root pw in config.json
  val ROOTAUTH = ("Authorization","Basic "+rootuser+":"+rootpw)
  val nodeId = "9912"     // the 1st node created, that i will use to run some rest methods
  val nodeToken = nodeId+"tok"
  val NODEAUTH = ("Authorization","Basic "+authpref+nodeId+":"+nodeToken)
  val agbotId = "9947"
  val agbotToken = agbotId+"tok"
  val AGBOTAUTH = ("Authorization","Basic "+authpref+agbotId+":"+agbotToken)
  val wkBase = "wk9920"
  val wkUrl = "http://" + wkBase
  val workload = wkBase + "_1.0.0_arm"
  val orgworkload = authpref+workload
  val wkBase2 = "wk9921"
  val wkUrl2 = "http://" + wkBase2
  val workload2 = wkBase2 + "_1.0.0_arm"
  val orgworkload2 = authpref+workload2
  val wkBase3 = "wk9922"
  val wkUrl3 = "http://" + wkBase3
  val workload3 = wkBase3 + "_1.0.0_arm"
  val microurl = "https://bluehorizon.network/microservices/network"
  val microarch = "amd64"
  val microversion = "1.0.0"
  val microurl2 = "https://bluehorizon.network/microservices/rtlsdr"
  val microarch2 = "amd64"
  val microversion2 = "2.0.0"
  val keyId = "mykey.pem"
  val key = "abcdefghijk"
  val keyId2 = "mykey2.pem"
  val key2 = "lnmopqrstuvwxyz"

  implicit val formats = DefaultFormats // Brings in default date formats etc.

  /** Delete all the test users */
  def deleteAllUsers() = {
    for (i <- List(user,user2)) {
      val response = Http(URL+"/users/"+i).method("delete").headers(ACCEPT).headers(ROOTAUTH).asString
      info("DELETE "+i+", code: "+response.code+", response.body: "+response.body)
      assert(response.code === HttpCode.DELETED || response.code === HttpCode.NOT_FOUND)
    }
  }

  /** Create an org to use for this test */
  test("POST /orgs/"+orgid+" - create org") {
    // Try deleting it 1st, in case it is left over from previous test
    var response = Http(URL).method("delete").headers(ACCEPT).headers(ROOTAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.DELETED || response.code === HttpCode.NOT_FOUND)

    val input = PostPutOrgRequest("My Org", "desc")
    response = Http(URL).postData(write(input)).method("post").headers(CONTENT).headers(ACCEPT).headers(ROOTAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.POST_OK)
  }

  /** Delete all the test users, in case they exist from a previous run. Do not need to delete the workloads, because they are deleted when the user is deleted. */
  test("Begin - DELETE all test users") {
    if (rootpw == "") fail("The exchange root password must be set in EXCHANGE_ROOTPW and must also be put in config.json.")
    deleteAllUsers()
  }

  /** Add users, node, workload for future tests */
  test("Add users, node, workload for future tests") {
    var userInput = PostPutUsersRequest(pw, admin = false, user+"@hotmail.com")
    var userResponse = Http(URL+"/users/"+user).postData(write(userInput)).method("post").headers(CONTENT).headers(ACCEPT).headers(ROOTAUTH).asString
    info("code: "+userResponse.code+", userResponse.body: "+userResponse.body)
    assert(userResponse.code === HttpCode.POST_OK)

    userInput = PostPutUsersRequest(pw2, admin = false, user2+"@hotmail.com")
    userResponse = Http(URL+"/users/"+user2).postData(write(userInput)).method("post").headers(CONTENT).headers(ACCEPT).headers(ROOTAUTH).asString
    info("code: "+userResponse.code+", userResponse.body: "+userResponse.body)
    assert(userResponse.code === HttpCode.POST_OK)

    val devInput = PutNodesRequest(nodeToken, "bc dev test", "", None, Some(List(RegService("foo",1,"{}",List(
      Prop("arch","arm","string","in"),
      Prop("version","2.0.0","version","in"),
      Prop("blockchainProtocols","agProto","list","in"))))), "whisper-id", Map(), "NODEABC")
    val devResponse = Http(URL+"/nodes/"+nodeId).postData(write(devInput)).method("put").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+devResponse.code)
    assert(devResponse.code === HttpCode.PUT_OK)

    val agbotInput = PutAgbotsRequest(agbotToken, "agbot"+agbotId+"-norm", /*List[APattern](),*/ "whisper-id", "ABC")
    val agbotResponse = Http(URL+"/agbots/"+agbotId).postData(write(agbotInput)).method("put").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+agbotResponse.code+", agbotResponse.body: "+agbotResponse.body)
    assert(agbotResponse.code === HttpCode.PUT_OK)
  }

  test("POST /orgs/"+orgid+"/workloads - add "+workload+" before the referenced microservice exists - should fail") {
    val input = PostPutWorkloadRequest(wkBase+" arm", "desc", public = false, wkUrl, "1.0.0", "arm", None, List(WMicroservices(microurl,orgid,microversion,microarch)), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/workloads").postData(write(input)).method("post").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.BAD_INPUT)
  }

  test("POST /orgs/"+orgid+"/microservices - add microservice so workloads can reference it") {
    val input = PostPutMicroserviceRequest("testMicro", "desc", public = false, microurl, microversion, microarch, "single", None, None, List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/microservices").postData(write(input)).method("post").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.POST_OK)
  }

  test("PUT /orgs/"+orgid+"/workloads/"+workload+" - update WK that is not there yet - should fail") {
    // PostPutWorkloadRequest(label: String, description: String, workloadUrl: String, version: String, arch: String, downloadUrl: String, apiSpec: List[Map[String,String]], userInput: List[Map[String,String]], workloads: List[Map[String,String]]) {
    val input = PostPutWorkloadRequest(wkBase+" arm", "desc", public = false, wkUrl, "1.0.0", "arm", Some("updated"), List(WMicroservices(microurl,orgid,microversion,microarch)), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/workloads/"+workload).postData(write(input)).method("put").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.NOT_FOUND)
  }

  test("POST /orgs/"+orgid+"/workloads - add "+workload+" that is not signed - should fail") {
    val input = PostPutWorkloadRequest(wkBase+" arm", "desc", public = false, wkUrl, "1.0.0", "arm", None, List(WMicroservices(microurl,orgid,microversion,microarch)), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","","")))
    val response = Http(URL+"/workloads").postData(write(input)).method("post").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.BAD_INPUT)
  }

  test("POST /orgs/"+orgid+"/workloads - add "+workload+" that needs 2 MSes - should fail") {
    val input = PostPutWorkloadRequest(wkBase+" arm", "desc", public = false, wkUrl, "1.0.0", "arm", None, List(WMicroservices(microurl,orgid,microversion,microarch),WMicroservices(microurl2,orgid,microversion2,microarch2)), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","")))
    val response = Http(URL+"/workloads").postData(write(input)).method("post").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.BAD_INPUT)
  }

  test("POST /orgs/"+orgid+"/workloads - add "+workload+" as user") {
    val input = PostPutWorkloadRequest(wkBase+" arm", "desc", public = false, wkUrl, "1.0.0", "arm", None, List(WMicroservices(microurl,orgid,microversion,microarch)), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/workloads").postData(write(input)).method("post").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.POST_OK)
    val respObj = parse(response.body).extract[ApiResponse]
    assert(respObj.msg.contains("workload '"+orgworkload+"' created"))
  }

  test("POST /orgs/"+orgid+"/workloads - add "+workload+" again - should fail") {
    val input = PostPutWorkloadRequest(wkBase+" arm", "desc", public = false, wkUrl, "1.0.0", "arm", None, List(WMicroservices(microurl,orgid,microversion,microarch)), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/workloads").postData(write(input)).method("post").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.ALREADY_EXISTS)
  }

  test("PUT /orgs/"+orgid+"/workloads/"+workload+" - update to need 2 MSes - should fail") {
    val input = PostPutWorkloadRequest(wkBase+" arm", "desc", public = false, wkUrl, "1.0.0", "arm", Some("updated"), List(WMicroservices(microurl,orgid,microversion,microarch),WMicroservices(microurl2,orgid,microversion2,microarch2)), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/workloads/"+workload).postData(write(input)).method("put").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.BAD_INPUT)
  }

  test("PUT /orgs/"+orgid+"/workloads/"+workload+" - update needing only the existing MS") {
    val input = PostPutWorkloadRequest(wkBase+" arm", "desc", public = false, wkUrl, "1.0.0", "arm", Some("updated"), List(WMicroservices(microurl,orgid,microversion,microarch)), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/workloads/"+workload).postData(write(input)).method("put").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.PUT_OK)
  }

  test("POST /orgs/"+orgid+"/microservices - add 2nd microservice so workloads can reference both") {
    val input = PostPutMicroserviceRequest("testMicro", "desc", public = false, microurl2, microversion2, microarch2, "single", None, None, List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/microservices").postData(write(input)).method("post").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.POST_OK)
  }

  test("PUT /orgs/"+orgid+"/workloads/"+workload+" - update to need 2 MSes - this time should succeed") {
    val input = PostPutWorkloadRequest(wkBase+" arm", "desc", public = false, wkUrl, "1.0.0", "arm", Some("updated"), List(WMicroservices(microurl,orgid,microversion,microarch),WMicroservices(microurl2,orgid,microversion2,microarch2)), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/workloads/"+workload).postData(write(input)).method("put").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.PUT_OK)
  }

  test("PUT /orgs/"+orgid+"/workloads/"+workload+" - update to need 2 MSes, but 1 version is not in range - should fail") {
    val input = PostPutWorkloadRequest(wkBase+" arm", "desc", public = false, wkUrl, "1.0.0", "arm", Some("updated"), List(WMicroservices(microurl,orgid,"2.0.0",microarch),WMicroservices(microurl2,orgid,microversion2,microarch2)), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/workloads/"+workload).postData(write(input)).method("put").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.BAD_INPUT)
  }

  test("PUT /orgs/"+orgid+"/workloads/"+workload+" - update as 2nd user - should fail") {
    val input = PostPutWorkloadRequest(wkBase+" arm", "desc", public = false, wkUrl, "1.0.0", "arm", Some("should not work"), List(WMicroservices(microurl,orgid,microversion,microarch)), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/workloads/"+workload).postData(write(input)).method("put").headers(CONTENT).headers(ACCEPT).headers(USER2AUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.ACCESS_DENIED)
  }

  test("PUT /orgs/"+orgid+"/workloads/"+workload+" - update as agbot - should fail") {
    val input = PostPutWorkloadRequest(wkBase+" arm", "desc", public = false, wkUrl, "1.0.0", "arm", None, List(WMicroservices(microurl,orgid,microversion,microarch)), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/workloads/"+workload).postData(write(input)).method("put").headers(CONTENT).headers(ACCEPT).headers(AGBOTAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.ACCESS_DENIED)
  }

  test("PUT /orgs/"+orgid+"/workloads/"+workload2+" - invalid workload body") {
    val badJsonInput = """{
      "labelxx": "GPS x86_64"
    }"""
    val response = Http(URL+"/workloads/"+workload2).postData(badJsonInput).method("put").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    assert(response.code === HttpCode.BAD_INPUT)
  }

  test("POST /orgs/"+orgid+"/workloads - add "+workload2+" as node - should fail") {
    val input = PostPutWorkloadRequest(wkBase2+" arm", "desc", public = false, wkUrl2, "1.0.0", "arm", None, List(), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/workloads").postData(write(input)).method("post").headers(CONTENT).headers(ACCEPT).headers(NODEAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.ACCESS_DENIED)
  }

  test("POST /orgs/"+orgid+"/workloads - add "+workload2+" as 2nd user, with no referenced MSes") {
    val input = PostPutWorkloadRequest(wkBase2+" arm", "desc", public = true, wkUrl2, "1.0.0", "arm", None, List(), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/workloads").postData(write(input)).method("post").headers(CONTENT).headers(ACCEPT).headers(USER2AUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.POST_OK)
  }

  test("PUT /orgs/"+orgid+"/workloads/"+workload2+" - add "+workload2+" as 2nd user, with a referenced MS so future GETs work") {
    val input = PostPutWorkloadRequest(wkBase2+" arm", "desc", public = true, wkUrl2, "1.0.0", "arm", None, List(WMicroservices(microurl,orgid,microversion,microarch)), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/workloads/"+workload2).postData(write(input)).method("put").headers(CONTENT).headers(ACCEPT).headers(USER2AUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.PUT_OK)
  }

  /*todo: when all test suites are run at the same time, there are sometimes timing problems them all setting config values...
  test("POST /orgs/"+orgid+"/workloads - with low maxWorkloads - should fail") {
    if (runningLocally) {     // changing limits via POST /admin/config does not work in multi-node mode
      // Get the current config value so we can restore it afterward
      ExchConfig.load()
      val origMaxWorkloads = ExchConfig.getInt("api.limits.maxWorkloads")

      // Change the maxWorkloads config value in the svr
      var configInput = AdminConfigRequest("api.limits.maxWorkloads", "0")    // user only owns 1 currently
      var response = Http(URL+"/admin/config").postData(write(configInput)).method("put").headers(CONTENT).headers(ACCEPT).headers(ROOTAUTH).asString
      info("code: "+response.code+", response.body: "+response.body)
      assert(response.code === HttpCode.PUT_OK)

      // Now try adding another workload - expect it to be rejected
      val input = PostPutWorkloadRequest(wkBase3+" arm", "desc", wkUrl3, "1.0.0", "arm", "", List(WMicroservices(microurl,orgid,microversion,microarch)), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
      response = Http(URL+"/workloads").postData(write(input)).method("post").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
      info("code: "+response.code+", response.body: "+response.body)
      assert(response.code === HttpCode.ACCESS_DENIED)
      val respObj = parse(response.body).extract[ApiResponse]
      assert(respObj.msg.contains("Access Denied"))

      // Restore the maxWorkloads config value in the svr
      configInput = AdminConfigRequest("api.limits.maxWorkloads", origMaxWorkloads.toString)
      response = Http(URL+"/admin/config").postData(write(configInput)).method("put").headers(CONTENT).headers(ACCEPT).headers(ROOTAUTH).asString
      info("code: "+response.code+", response.body: "+response.body)
      assert(response.code === HttpCode.PUT_OK)
    }
  }
  */

  test("GET /orgs/"+orgid+"/workloads") {
    val response: HttpResponse[String] = Http(URL+"/workloads").headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    //info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.OK)
    val respObj = parse(response.body).extract[GetWorkloadsResponse]
    assert(respObj.workloads.size === 2)

    assert(respObj.workloads.contains(orgworkload))
    var wk = respObj.workloads(orgworkload)     // the 2nd get turns the Some(val) into val
    assert(wk.label === wkBase+" arm")
    assert(wk.owner === orguser)

    assert(respObj.workloads.contains(orgworkload2))
    wk = respObj.workloads(orgworkload2)     // the 2nd get turns the Some(val) into val
    assert(wk.label === wkBase2+" arm")
    assert(wk.owner === orguser2)
  }

  test("GET /orgs/"+orgid+"/workloads - filter owner and workloadUrl") {
    val response: HttpResponse[String] = Http(URL+"/workloads").headers(ACCEPT).headers(USERAUTH).param("owner",orguser2).param("specRef",microurl).asString
    info("code: "+response.code)
    // info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.OK)
    val respObj = parse(response.body).extract[GetWorkloadsResponse]
    assert(respObj.workloads.size === 1)
    assert(respObj.workloads.contains(orgworkload2))
  }

  test("GET /orgs/"+orgid+"/workloads - filter by public setting") {
    // Find the public==true workloads
    var response: HttpResponse[String] = Http(URL+"/workloads").headers(ACCEPT).headers(USERAUTH).param("public","true").asString
    info("code: "+response.code)
    assert(response.code === HttpCode.OK)
    var respObj = parse(response.body).extract[GetWorkloadsResponse]
    assert(respObj.workloads.size === 1)
    assert(respObj.workloads.contains(orgworkload2))

    // Find the public==false workloads
    response = Http(URL+"/workloads").headers(ACCEPT).headers(USERAUTH).param("public","false").asString
    info("code: "+response.code)
    assert(response.code === HttpCode.OK)
    respObj = parse(response.body).extract[GetWorkloadsResponse]
    assert(respObj.workloads.size === 1)
    assert(respObj.workloads.contains(orgworkload))
  }

  test("GET /orgs/"+orgid+"/workloads - as node") {
    val response: HttpResponse[String] = Http(URL+"/workloads").headers(ACCEPT).headers(NODEAUTH).asString
    info("code: "+response.code)
    // info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.OK)
    val respObj = parse(response.body).extract[GetWorkloadsResponse]
    assert(respObj.workloads.size === 2)
  }

  test("GET /orgs/"+orgid+"/workloads - as agbot") {
    val response: HttpResponse[String] = Http(URL+"/workloads").headers(ACCEPT).headers(AGBOTAUTH).asString
    info("code: "+response.code)
    // info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.OK)
    val respObj = parse(response.body).extract[GetWorkloadsResponse]
    assert(respObj.workloads.size === 2)
  }

  test("GET /orgs/"+orgid+"/workloads/"+workload+" - as user") {
    val response: HttpResponse[String] = Http(URL+"/workloads/"+workload).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    // info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.OK)
    val respObj = parse(response.body).extract[GetWorkloadsResponse]
    assert(respObj.workloads.size === 1)

    assert(respObj.workloads.contains(orgworkload))
    val wk = respObj.workloads(orgworkload)     // the 2nd get turns the Some(val) into val
    assert(wk.label === wkBase+" arm")

    // Verify the lastUpdated from the PUT above is within a few seconds of now. Format is: 2016-09-29T13:04:56.850Z[UTC]
    val now: Long = System.currentTimeMillis / 1000     // seconds since 1/1/1970
    val lastUp = ZonedDateTime.parse(wk.lastUpdated).toEpochSecond
    assert(now - lastUp <= 5)    // should not be more than 3 seconds from the time the put was done above
  }

  test("PATCH /orgs/"+orgid+"/workloads/"+workload+" - as user") {
    val jsonInput = """{
      "downloadUrl": "this is now patched"
    }"""
    val response = Http(URL+"/workloads/"+workload).postData(jsonInput).method("patch").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.PUT_OK)
  }

  test("PATCH /orgs/"+orgid+"/workloads/"+workload+" - as user2 - should fail") {
    val jsonInput = """{
      "downloadUrl": "this is now patched"
    }"""
    val response = Http(URL+"/workloads/"+workload).postData(jsonInput).method("patch").headers(CONTENT).headers(ACCEPT).headers(USER2AUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.ACCESS_DENIED)
  }

  test("GET /orgs/"+orgid+"/workloads/"+workload+" - as agbot, check patch by getting that 1 attr") {
    val response: HttpResponse[String] = Http(URL+"/workloads/"+workload).headers(ACCEPT).headers(AGBOTAUTH).param("attribute","downloadUrl").asString
    info("code: "+response.code)
    // info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.OK)
    val respObj = parse(response.body).extract[GetWorkloadAttributeResponse]
    assert(respObj.attribute === "downloadUrl")
    assert(respObj.value === "this is now patched")
  }

  test("GET /orgs/"+orgid+"/workloads/"+workload+"notthere - as user - should fail") {
    val response: HttpResponse[String] = Http(URL+"/workloads/"+workload+"notthere").headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    // info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.NOT_FOUND)
    val getWorkloadResp = parse(response.body).extract[GetWorkloadsResponse]
    assert(getWorkloadResp.workloads.size === 0)
  }


  // Key tests ==============================================
  test("GET /orgs/"+orgid+"/workloads/"+workload+"/keys - no keys have been created yet - should fail") {
    val response: HttpResponse[String] = Http(URL+"/workloads/"+workload+"/keys").headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    assert(response.code === HttpCode.NOT_FOUND)
    val resp = parse(response.body).extract[List[String]]
    assert(resp.size === 0)
  }

  test("PUT /orgs/"+orgid+"/workloads/"+workload+"/keys/"+keyId+" - add "+keyId+" as user") {
    //val input = PutWorkloadKeyRequest(key)
    val response = Http(URL+"/workloads/"+workload+"/keys/"+keyId).postData(key).method("put").headers(CONTENTTEXT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.POST_OK)
  }

  test("PUT /orgs/"+orgid+"/workloads/"+workload+"/keys/"+keyId2+" - add "+keyId2+" as user") {
    //val input = PutWorkloadKeyRequest(key2)
    val response = Http(URL+"/workloads/"+workload+"/keys/"+keyId2).postData(key2).method("put").headers(CONTENTTEXT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.POST_OK)
  }

  test("GET /orgs/"+orgid+"/workloads/"+workload+"/keys - should be 2 now") {
    val response: HttpResponse[String] = Http(URL+"/workloads/"+workload+"/keys").headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    assert(response.code === HttpCode.OK)
    val resp = parse(response.body).extract[List[String]]
    assert(resp.size === 2)
    assert(resp.contains(keyId) && resp.contains(keyId2))
  }

  test("GET /orgs/"+orgid+"/workloads/"+workload+"/keys/"+keyId+" - get 1 of the keys and check content") {
    val response: HttpResponse[String] = Http(URL+"/workloads/"+workload+"/keys/"+keyId).headers(ACCEPTTEXT).headers(USERAUTH).asString
    //val response: HttpResponse[Array[Byte]] = Http(URL+"/workloads/"+workload+"/keys/"+keyId).headers(ACCEPTTEXT).headers(USERAUTH).asBytes
    //val bodyStr = (response.body.map(_.toChar)).mkString
    //info("code: "+response.code+", response.body: "+bodyStr)
    info("code: "+response.code)
    assert(response.code === HttpCode.OK)
    assert(response.body === key)
  }

  test("DELETE /orgs/"+orgid+"/workloads/"+workload+"/keys/"+keyId) {
    val response: HttpResponse[String] = Http(URL+"/workloads/"+workload+"/keys/"+keyId).method("delete").headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    assert(response.code === HttpCode.DELETED)
  }

  test("DELETE /orgs/"+orgid+"/workloads/"+workload+"/keys/"+keyId+" try deleting it again - should fail") {
    val response: HttpResponse[String] = Http(URL+"/workloads/"+workload+"/keys/"+keyId).method("delete").headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    assert(response.code === HttpCode.NOT_FOUND)
  }

  test("GET /orgs/"+orgid+"/workloads/"+workload+"/keys/"+keyId+" - verify it is gone") {
    val response: HttpResponse[String] = Http(URL+"/workloads/"+workload+"/keys/"+keyId).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    assert(response.code === HttpCode.NOT_FOUND)
  }

  test("DELETE /orgs/"+orgid+"/workloads/"+workload+"/keys - delete all keys") {
    val response: HttpResponse[String] = Http(URL+"/workloads/"+workload+"/keys").method("delete").headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    assert(response.code === HttpCode.DELETED)
  }

  test("GET /orgs/"+orgid+"/workloads/"+workload+"/keys - all keys should be gone now") {
    val response: HttpResponse[String] = Http(URL+"/workloads/"+workload+"/keys").headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    assert(response.code === HttpCode.NOT_FOUND)
    val resp = parse(response.body).extract[List[String]]
    assert(resp.size === 0)
  }


  test("DELETE /orgs/"+orgid+"/workloads/"+workload) {
    val response = Http(URL+"/workloads/"+workload).method("delete").headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.DELETED)
  }

  test("GET /orgs/"+orgid+"/workloads/"+workload+" - as user - verify gone") {
    val response: HttpResponse[String] = Http(URL+"/workloads/"+workload).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    // info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.NOT_FOUND)
    val getWorkloadResp = parse(response.body).extract[GetWorkloadsResponse]
    assert(getWorkloadResp.workloads.size === 0)
  }

  test("DELETE /orgs/"+orgid+"/users/"+user2+" - which should also delete workload2") {
    val response = Http(URL+"/users/"+user2).method("delete").headers(ACCEPT).headers(ROOTAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.DELETED)
  }

  test("GET /orgs/"+orgid+"/workloads/"+workload2+" - as user - verify gone") {
    val response: HttpResponse[String] = Http(URL+"/workloads/"+workload2).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    // info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.NOT_FOUND)
    val getWorkloadResp = parse(response.body).extract[GetWorkloadsResponse]
    assert(getWorkloadResp.workloads.size === 0)
  }

  /** Clean up, delete all the test workloads */
  test("Cleanup - DELETE all test workloads") {
    deleteAllUsers()
  }

  /** Delete the org we used for this test */
  test("POST /orgs/"+orgid+" - delete org") {
    // Try deleting it 1st, in case it is left over from previous test
    val response = Http(URL).method("delete").headers(ACCEPT).headers(ROOTAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.DELETED)
  }

}
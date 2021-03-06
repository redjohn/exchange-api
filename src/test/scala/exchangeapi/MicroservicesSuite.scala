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
 * Tests for the /microservices routes. To run
 * the test suite, you can either:
 *  - run the "test" command in the SBT console
 *  - right-click the file in eclipse and chose "Run As" - "JUnit Test"
 *
 * clear and detailed tutorial of FunSuite: http://doc.scalatest.org/1.9.1/index.html#org.scalatest.FunSuite
 */
@RunWith(classOf[JUnitRunner])
class MicroservicesSuite extends FunSuite {

  val localUrlRoot = "http://localhost:8080"
  val urlRoot = sys.env.getOrElse("EXCHANGE_URL_ROOT", localUrlRoot)
  val runningLocally = (urlRoot == localUrlRoot)
  val ACCEPT = ("Accept","application/json")
  val ACCEPTTEXT = ("Accept","text/plain")
  val CONTENT = ("Content-Type","application/json")
  val CONTENTTEXT = ("Content-Type","text/plain")
  val orgid = "MicroservicesSuiteTests"
  val authpref=orgid+"/"
  val URL = urlRoot+"/v1/orgs/"+orgid
  //val NOORGURL = urlRoot+"/v1"
  val user = "9997"
  val orguser = authpref+user
  val pw = user+"pw"
  val USERAUTH = ("Authorization","Basic "+orguser+":"+pw)
  val user2 = "9998"
  val orguser2 = authpref+user2
  val pw2 = user2+"pw"
  val USER2AUTH = ("Authorization","Basic "+orguser2+":"+pw2)
  val rootuser = Role.superUser
  val rootpw = sys.env.getOrElse("EXCHANGE_ROOTPW", "")      // need to put this root pw in config.json
  val ROOTAUTH = ("Authorization","Basic "+rootuser+":"+rootpw)
  val nodeId = "9911"     // the 1st node created, that i will use to run some rest methods
  val nodeToken = nodeId+"tok"
  val NODEAUTH = ("Authorization","Basic "+authpref+nodeId+":"+nodeToken)
  val agbotId = "9946"
  val agbotToken = agbotId+"tok"
  val AGBOTAUTH = ("Authorization","Basic "+authpref+agbotId+":"+agbotToken)
  val msBase = "ms9920"
  val msUrl = "http://" + msBase
  val microservice = msBase + "_1.0.0_arm"
  val orgmicroservice = authpref+microservice
  val msBase2 = "ms9921"
  val msUrl2 = "http://" + msBase2
  val microservice2 = msBase2 + "_1.0.0_arm"
  val orgmicroservice2 = authpref+microservice2
  val msBase3 = "ms9922"
  val msUrl3 = "http://" + msBase3
  val microservice3 = msBase3 + "_1.0.0_arm"
  val keyId = "mykey.pem"
  val key = "abcdefghijk"
  val keyId2 = "mykey2.pem"
  val key2 = "lnmopqrstuvwxyz"
  //val orgmicroservice3 = authpref+microservice3
  //var numExistingMicroservices = 0    // this will be set later

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

  /** Delete all the test users, in case they exist from a previous run. Do not need to delete the microservices, because they are deleted when the user is deleted. */
  test("Begin - DELETE all test users") {
    if (rootpw == "") fail("The exchange root password must be set in EXCHANGE_ROOTPW and must also be put in config.json.")
    deleteAllUsers()
  }

  /** Add users, node, microservice for future tests */
  test("Add users, node, agbot for future tests") {
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

  test("PUT /orgs/"+orgid+"/microservices/"+microservice+" - update MS that is not there yet - should fail") {
    val input = PostPutMicroserviceRequest(msBase+" arm", "desc", public = false, msUrl, "1.0.0", "arm", "single", Some("updated"), Some(Map("usbNodeIds" -> "1546:01a7")), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","")))
    val response = Http(URL+"/microservices/"+microservice).postData(write(input)).method("put").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.NOT_FOUND)
  }

  test("POST /orgs/"+orgid+"/microservices - add "+microservice+" that is not signed - should fail") {
    val input = PostPutMicroserviceRequest(msBase+" arm", "desc", public = false, msUrl, "1.0.0", "arm", "single", None, Some(Map("usbNodeIds" -> "1546:01a7")), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","","a")))
    val response = Http(URL+"/microservices").postData(write(input)).method("post").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.BAD_INPUT)
  }

  test("POST /orgs/"+orgid+"/microservices - add "+microservice+" as user, and omit matchHardware") {
    val input = PostPutMicroserviceRequest(msBase+" arm", "desc", public = false, msUrl, "1.0.0", "arm", "single", None, None, List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/microservices").postData(write(input)).method("post").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.POST_OK)
    val respObj = parse(response.body).extract[ApiResponse]
    assert(respObj.msg.contains("microservice '"+orgmicroservice+"' created"))
  }

  test("POST /orgs/"+orgid+"/microservices - add "+microservice+" again - should fail") {
    val input = PostPutMicroserviceRequest(msBase+" arm", "desc", public = false, msUrl, "1.0.0", "arm", "single", None, Some(Map("usbNodeIds" -> "1546:01a7")), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/microservices").postData(write(input)).method("post").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.ALREADY_EXISTS)
  }

  test("PUT /orgs/"+orgid+"/microservices/"+microservice+" - update as same user") {
    val input = PostPutMicroserviceRequest(msBase+" arm", "desc", public = false, msUrl, "1.0.0", "arm", "single", Some("updated"), Some(Map("usbNodeIds" -> "1546:01a7")), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/microservices/"+microservice).postData(write(input)).method("put").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.PUT_OK)
  }

  test("PUT /orgs/"+orgid+"/microservices/"+microservice+" - update as 2nd user - should fail") {
    val input = PostPutMicroserviceRequest(msBase+" arm", "desc", public = false, msUrl, "1.0.0", "arm", "single", Some("should not work"), Some(Map("usbNodeIds" -> "1546:01a7")), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/microservices/"+microservice).postData(write(input)).method("put").headers(CONTENT).headers(ACCEPT).headers(USER2AUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.ACCESS_DENIED)
  }

  test("PUT /orgs/"+orgid+"/microservices/"+microservice+" - update as agbot - should fail") {
    val input = PostPutMicroserviceRequest(msBase+" arm", "desc", public = false, msUrl, "1.0.0", "arm", "single", None, Some(Map("usbNodeIds" -> "1546:01a7")), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/microservices/"+microservice).postData(write(input)).method("put").headers(CONTENT).headers(ACCEPT).headers(AGBOTAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.ACCESS_DENIED)
  }

  test("PUT /orgs/"+orgid+"/microservices/"+microservice2+" - invalid microservice body") {
    val badJsonInput = """{
      "labelxx": "GPS x86_64"
    }"""
    val response = Http(URL+"/microservices/"+microservice2).postData(badJsonInput).method("put").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    assert(response.code === HttpCode.BAD_INPUT)
  }

  test("POST /orgs/"+orgid+"/microservices - add "+microservice2+" as node - should fail") {
    val input = PostPutMicroserviceRequest(msBase2+" arm", "desc", public = false, msUrl2, "1.0.0", "arm", "single", None, Some(Map("usbNodeIds" -> "1546:01a7")), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/microservices").postData(write(input)).method("post").headers(CONTENT).headers(ACCEPT).headers(NODEAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.ACCESS_DENIED)
  }

  test("POST /orgs/"+orgid+"/microservices - add "+microservice2+" as 2nd user") {
    val input = PostPutMicroserviceRequest(msBase2+" arm", "desc", public = true, msUrl2, "1.0.0", "arm", "single", None, Some(Map("usbNodeIds" -> "1546:01a7")), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    val response = Http(URL+"/microservices").postData(write(input)).method("post").headers(CONTENT).headers(ACCEPT).headers(USER2AUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.POST_OK)
  }

/*todo: when all test suites are run at the same time, there are sometimes timing problems with them all setting config values...
test("POST /orgs/"+orgid+"/microservices - with low maxMicroservices - should fail") {
  if (runningLocally) {     // changing limits via POST /admin/config does not work in multi-node mode
    // Get the current config value so we can restore it afterward
    ExchConfig.load()
    val origMaxMicroservices = ExchConfig.getInt("api.limits.maxMicroservices")

    // Change the maxMicroservices config value in the svr
    var configInput = AdminConfigRequest("api.limits.maxMicroservices", "0")    // user only owns 1 currently
    var response = Http(URL+"/admin/config").postData(write(configInput)).method("put").headers(CONTENT).headers(ACCEPT).headers(ROOTAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.PUT_OK)

    // Now try adding another microservice - expect it to be rejected
    val input = PostPutMicroserviceRequest(msBase3+" arm", "desc", msUrl3, "1.0.0", "arm", "single", None, Some(Map("usbNodeIds" -> "1546:01a7")), List(Map("name" -> "foo")), List(MDockerImages("{\"services\":{}}","a","a")))
    response = Http(URL+"/microservices").postData(write(input)).method("post").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.ACCESS_DENIED)
    val respObj = parse(response.body).extract[ApiResponse]
    assert(respObj.msg.contains("Access Denied"))

    // Restore the maxMicroservices config value in the svr
    configInput = AdminConfigRequest("api.limits.maxMicroservices", origMaxMicroservices.toString)
    response = Http(URL+"/admin/config").postData(write(configInput)).method("put").headers(CONTENT).headers(ACCEPT).headers(ROOTAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.PUT_OK)
  }
}
*/

test("GET /orgs/"+orgid+"/microservices") {
  val response: HttpResponse[String] = Http(URL+"/microservices").headers(ACCEPT).headers(USERAUTH).asString
  info("code: "+response.code)
  // info("code: "+response.code+", response.body: "+response.body)
  assert(response.code === HttpCode.OK)
  val respObj = parse(response.body).extract[GetMicroservicesResponse]
  assert(respObj.microservices.size === 2)

  assert(respObj.microservices.contains(orgmicroservice))
  var ms = respObj.microservices(orgmicroservice)     // the 2nd get turns the Some(val) into val
  assert(ms.label === msBase+" arm")
  assert(ms.owner === orguser)

  assert(respObj.microservices.contains(orgmicroservice2))
  ms = respObj.microservices(orgmicroservice2)     // the 2nd get turns the Some(val) into val
  assert(ms.label === msBase2+" arm")
  assert(ms.owner === orguser2)
}

  test("GET /orgs/"+orgid+"/microservices - filter owner and specRef") {
    val response: HttpResponse[String] = Http(URL+"/microservices").headers(ACCEPT).headers(USERAUTH).param("owner",orguser2).param("specRef",msUrl2+"%").asString
    info("code: "+response.code)
    // info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.OK)
    val respObj = parse(response.body).extract[GetMicroservicesResponse]
    assert(respObj.microservices.size === 1)
    assert(respObj.microservices.contains(orgmicroservice2))
  }

  test("GET /orgs/"+orgid+"/microservices - filter by public setting") {
    // Find the public==true microservices
    var response: HttpResponse[String] = Http(URL+"/microservices").headers(ACCEPT).headers(USERAUTH).param("public","true").asString
    info("code: "+response.code)
    assert(response.code === HttpCode.OK)
    var respObj = parse(response.body).extract[GetMicroservicesResponse]
    assert(respObj.microservices.size === 1)
    assert(respObj.microservices.contains(orgmicroservice2))

    // Find the public==false microservices
    response = Http(URL+"/microservices").headers(ACCEPT).headers(USERAUTH).param("public","false").asString
    info("code: "+response.code)
    assert(response.code === HttpCode.OK)
    respObj = parse(response.body).extract[GetMicroservicesResponse]
    assert(respObj.microservices.size === 1)
    assert(respObj.microservices.contains(orgmicroservice))
  }

test("GET /orgs/"+orgid+"/microservices - as node") {
  val response: HttpResponse[String] = Http(URL+"/microservices").headers(ACCEPT).headers(NODEAUTH).asString
  info("code: "+response.code)
  // info("code: "+response.code+", response.body: "+response.body)
  assert(response.code === HttpCode.OK)
  val respObj = parse(response.body).extract[GetMicroservicesResponse]
  assert(respObj.microservices.size === 2)
}

test("GET /orgs/"+orgid+"/microservices - as agbot") {
  val response: HttpResponse[String] = Http(URL+"/microservices").headers(ACCEPT).headers(AGBOTAUTH).asString
  info("code: "+response.code)
  // info("code: "+response.code+", response.body: "+response.body)
  assert(response.code === HttpCode.OK)
  val respObj = parse(response.body).extract[GetMicroservicesResponse]
  assert(respObj.microservices.size === 2)
}

test("GET /orgs/"+orgid+"/microservices/"+microservice+" - as user") {
  val response: HttpResponse[String] = Http(URL+"/microservices/"+microservice).headers(ACCEPT).headers(USERAUTH).asString
  info("code: "+response.code)
  // info("code: "+response.code+", response.body: "+response.body)
  assert(response.code === HttpCode.OK)
  val respObj = parse(response.body).extract[GetMicroservicesResponse]
  assert(respObj.microservices.size === 1)

  assert(respObj.microservices.contains(orgmicroservice))
  val ms = respObj.microservices(orgmicroservice)     // the 2nd get turns the Some(val) into val
  assert(ms.label === msBase+" arm")

  // Verify the lastUpdated from the PUT above is within a few seconds of now. Format is: 2016-09-29T13:04:56.850Z[UTC]
  val now: Long = System.currentTimeMillis / 1000     // seconds since 1/1/1970
  val lastUp = ZonedDateTime.parse(ms.lastUpdated).toEpochSecond
  assert(now - lastUp <= 5)    // should not be more than 3 seconds from the time the put was done above
}

test("PATCH /orgs/"+orgid+"/microservices/"+microservice+" - as user") {
  val jsonInput = """{
    "downloadUrl": "this is now patched"
  }"""
  val response = Http(URL+"/microservices/"+microservice).postData(jsonInput).method("patch").headers(CONTENT).headers(ACCEPT).headers(USERAUTH).asString
  info("code: "+response.code+", response.body: "+response.body)
  assert(response.code === HttpCode.PUT_OK)
}

test("PATCH /orgs/"+orgid+"/microservices/"+microservice+" - as user2 - should fail") {
  val jsonInput = """{
    "downloadUrl": "this is now patched"
  }"""
  val response = Http(URL+"/microservices/"+microservice).postData(jsonInput).method("patch").headers(CONTENT).headers(ACCEPT).headers(USER2AUTH).asString
  info("code: "+response.code+", response.body: "+response.body)
  assert(response.code === HttpCode.ACCESS_DENIED)
}

test("GET /orgs/"+orgid+"/microservices/"+microservice+" - as agbot, check patch by getting that 1 attr") {
  val response: HttpResponse[String] = Http(URL+"/microservices/"+microservice).headers(ACCEPT).headers(AGBOTAUTH).param("attribute","downloadUrl").asString
  info("code: "+response.code)
  // info("code: "+response.code+", response.body: "+response.body)
  assert(response.code === HttpCode.OK)
  val respObj = parse(response.body).extract[GetMicroserviceAttributeResponse]
  assert(respObj.attribute === "downloadUrl")
  assert(respObj.value === "this is now patched")
}

test("GET /orgs/"+orgid+"/microservices/"+microservice+"notthere - as user - should fail") {
  val response: HttpResponse[String] = Http(URL+"/microservices/"+microservice+"notthere").headers(ACCEPT).headers(USERAUTH).asString
  info("code: "+response.code)
  // info("code: "+response.code+", response.body: "+response.body)
  assert(response.code === HttpCode.NOT_FOUND)
  val getMicroserviceResp = parse(response.body).extract[GetMicroservicesResponse]
  assert(getMicroserviceResp.microservices.size === 0)
}


  // Key tests ==============================================
  test("GET /orgs/"+orgid+"/microservices/"+microservice+"/keys - no keys have been created yet - should fail") {
    val response: HttpResponse[String] = Http(URL+"/microservices/"+microservice+"/keys").headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    assert(response.code === HttpCode.NOT_FOUND)
    val resp = parse(response.body).extract[List[String]]
    assert(resp.size === 0)
  }

  test("PUT /orgs/"+orgid+"/microservices/"+microservice+"/keys/"+keyId+" - add "+keyId+" as user") {
    //val input = PutMicroserviceKeyRequest(key)
    val response = Http(URL+"/microservices/"+microservice+"/keys/"+keyId).postData(key).method("put").headers(CONTENTTEXT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.POST_OK)
  }

  test("PUT /orgs/"+orgid+"/microservices/"+microservice+"/keys/"+keyId2+" - add "+keyId2+" as user") {
    //val input = PutMicroserviceKeyRequest(key2)
    val response = Http(URL+"/microservices/"+microservice+"/keys/"+keyId2).postData(key2).method("put").headers(CONTENTTEXT).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code+", response.body: "+response.body)
    assert(response.code === HttpCode.POST_OK)
  }

  test("GET /orgs/"+orgid+"/microservices/"+microservice+"/keys - should be 2 now") {
    val response: HttpResponse[String] = Http(URL+"/microservices/"+microservice+"/keys").headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    assert(response.code === HttpCode.OK)
    val resp = parse(response.body).extract[List[String]]
    assert(resp.size === 2)
    assert(resp.contains(keyId) && resp.contains(keyId2))
  }

  test("GET /orgs/"+orgid+"/microservices/"+microservice+"/keys/"+keyId+" - get 1 of the keys and check content") {
    val response: HttpResponse[String] = Http(URL+"/microservices/"+microservice+"/keys/"+keyId).headers(ACCEPTTEXT).headers(USERAUTH).asString
    //val response: HttpResponse[Array[Byte]] = Http(URL+"/microservices/"+microservice+"/keys/"+keyId).headers(ACCEPTTEXT).headers(USERAUTH).asBytes
    //val bodyStr = (response.body.map(_.toChar)).mkString
    //info("code: "+response.code+", response.body: "+bodyStr)
    info("code: "+response.code)
    assert(response.code === HttpCode.OK)
    assert(response.body === key)
  }

  test("DELETE /orgs/"+orgid+"/microservices/"+microservice+"/keys/"+keyId) {
    val response: HttpResponse[String] = Http(URL+"/microservices/"+microservice+"/keys/"+keyId).method("delete").headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    assert(response.code === HttpCode.DELETED)
  }

  test("DELETE /orgs/"+orgid+"/microservices/"+microservice+"/keys/"+keyId+" try deleting it again - should fail") {
    val response: HttpResponse[String] = Http(URL+"/microservices/"+microservice+"/keys/"+keyId).method("delete").headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    assert(response.code === HttpCode.NOT_FOUND)
  }

  test("GET /orgs/"+orgid+"/microservices/"+microservice+"/keys/"+keyId+" - verify it is gone") {
    val response: HttpResponse[String] = Http(URL+"/microservices/"+microservice+"/keys/"+keyId).headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    assert(response.code === HttpCode.NOT_FOUND)
  }

  test("DELETE /orgs/"+orgid+"/microservices/"+microservice+"/keys - delete all keys") {
    val response: HttpResponse[String] = Http(URL+"/microservices/"+microservice+"/keys").method("delete").headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    assert(response.code === HttpCode.DELETED)
  }

  test("GET /orgs/"+orgid+"/microservices/"+microservice+"/keys - all keys should be gone now") {
    val response: HttpResponse[String] = Http(URL+"/microservices/"+microservice+"/keys").headers(ACCEPT).headers(USERAUTH).asString
    info("code: "+response.code)
    assert(response.code === HttpCode.NOT_FOUND)
    val resp = parse(response.body).extract[List[String]]
    assert(resp.size === 0)
  }


  test("DELETE /orgs/"+orgid+"/microservices/"+microservice) {
  val response = Http(URL+"/microservices/"+microservice).method("delete").headers(ACCEPT).headers(USERAUTH).asString
  info("code: "+response.code+", response.body: "+response.body)
  assert(response.code === HttpCode.DELETED)
}

test("GET /orgs/"+orgid+"/microservices/"+microservice+" - as user - verify gone") {
  val response: HttpResponse[String] = Http(URL+"/microservices/"+microservice).headers(ACCEPT).headers(USERAUTH).asString
  info("code: "+response.code)
  // info("code: "+response.code+", response.body: "+response.body)
  assert(response.code === HttpCode.NOT_FOUND)
  val getMicroserviceResp = parse(response.body).extract[GetMicroservicesResponse]
  assert(getMicroserviceResp.microservices.size === 0)
}

test("DELETE /orgs/"+orgid+"/users/"+user2+" - which should also delete microservice2") {
  val response = Http(URL+"/users/"+user2).method("delete").headers(ACCEPT).headers(ROOTAUTH).asString
  info("code: "+response.code+", response.body: "+response.body)
  assert(response.code === HttpCode.DELETED)
}

test("GET /orgs/"+orgid+"/microservices/"+microservice2+" - as user - verify gone") {
  val response: HttpResponse[String] = Http(URL+"/microservices/"+microservice2).headers(ACCEPT).headers(USERAUTH).asString
  info("code: "+response.code)
  // info("code: "+response.code+", response.body: "+response.body)
  assert(response.code === HttpCode.NOT_FOUND)
  val getMicroserviceResp = parse(response.body).extract[GetMicroservicesResponse]
  assert(getMicroserviceResp.microservices.size === 0)
}

/** Clean up, delete all the test microservices */
test("Cleanup - DELETE all test microservices") {
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
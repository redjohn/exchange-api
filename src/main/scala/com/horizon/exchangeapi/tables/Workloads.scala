package com.horizon.exchangeapi.tables

//import java.sql.Blob

import com.horizon.exchangeapi.OrgAndId
import org.json4s._
import org.json4s.jackson.Serialization.read
import slick.jdbc.PostgresProfile.api._


/** Contains the object representations of the DB tables related to workloads. */
case class WMicroservices(specRef: String, org: String, version: String, arch: String)

case class WorkloadRow(workload: String, orgid: String, owner: String, label: String, description: String, public: Boolean, workloadUrl: String, version: String, arch: String, downloadUrl: String, apiSpec: String, userInput: String, workloads: String, lastUpdated: String) {
   protected implicit val jsonFormats: Formats = DefaultFormats

  def toWorkload: Workload = {
    val spec = if (apiSpec != "") read[List[WMicroservices]](apiSpec) else List[WMicroservices]()
    val input = if (userInput != "") read[List[Map[String,String]]](userInput) else List[Map[String,String]]()
    val wrk = if (workloads != "") read[List[MDockerImages]](workloads) else List[MDockerImages]()
    new Workload(owner, label, description, public, workloadUrl, version, arch, downloadUrl, spec, input, wrk, lastUpdated)
  }

  // update returns a DB action to update this row
  def update: DBIO[_] = (for { m <- WorkloadsTQ.rows if m.workload === workload } yield m).update(this)

  // insert returns a DB action to insert this row
  def insert: DBIO[_] = WorkloadsTQ.rows += this
}

/** Mapping of the workloads db table to a scala class */
class Workloads(tag: Tag) extends Table[WorkloadRow](tag, "workloads") {
  def workload = column[String]("workload", O.PrimaryKey)    // the content of this is orgid/workload
  def orgid = column[String]("orgid")
  def owner = column[String]("owner")
  def label = column[String]("label")
  def description = column[String]("description")
  def public = column[Boolean]("public")
  def workloadUrl = column[String]("workloadurl")
  def version = column[String]("version")
  def arch = column[String]("arch")
  def downloadUrl = column[String]("downloadurl")
  def apiSpec = column[String]("apispec")
  def userInput = column[String]("userinput")
  def workloads = column[String]("workloads")
  def lastUpdated = column[String]("lastupdated")
  // this describes what you get back when you return rows from a query
  def * = (workload, orgid, owner, label, description, public, workloadUrl, version, arch, downloadUrl, apiSpec, userInput, workloads, lastUpdated) <> (WorkloadRow.tupled, WorkloadRow.unapply)
  def user = foreignKey("user_fk", owner, UsersTQ.rows)(_.username, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  def orgidKey = foreignKey("orgid_fk", orgid, OrgsTQ.rows)(_.orgid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
}

// Instance to access the workloads table
object WorkloadsTQ {
  val rows = TableQuery[Workloads]

  def formId(orgid: String, url: String, version: String, arch: String): String = {
    // Remove the https:// from the beginning of workloadUrl and replace troublesome chars with a dash. It has already been checked as a valid URL in validate().
    val workloadUrl2 = """^[A-Za-z0-9+.-]*?://""".r replaceFirstIn (url, "")
    val workloadUrl3 = """[$!*,;/?@&~=%]""".r replaceAllIn (workloadUrl2, "-")     // I think possible chars in valid urls are: $_.+!*,;/?:@&~=%-
    return OrgAndId(orgid, workloadUrl3 + "_" + version + "_" + arch).toString
  }

  def getAllWorkloads(orgid: String) = rows.filter(_.orgid === orgid)
  def getWorkload(workload: String) = rows.filter(_.workload === workload)
  def getOwner(workload: String) = rows.filter(_.workload === workload).map(_.owner)
  def getNumOwned(owner: String) = rows.filter(_.owner === owner).length
  def getLabel(workload: String) = rows.filter(_.workload === workload).map(_.label)
  def getDescription(workload: String) = rows.filter(_.workload === workload).map(_.description)
  def getPublic(workload: String) = rows.filter(_.workload === workload).map(_.public)
  def getWorkloadUrl(workload: String) = rows.filter(_.workload === workload).map(_.workloadUrl)
  def getVersion(workload: String) = rows.filter(_.workload === workload).map(_.version)
  def getArch(workload: String) = rows.filter(_.workload === workload).map(_.arch)
  def getDownloadUrl(workload: String) = rows.filter(_.workload === workload).map(_.downloadUrl)
  def getApiSpec(workload: String) = rows.filter(_.workload === workload).map(_.apiSpec)
  def getUserInput(workload: String) = rows.filter(_.workload === workload).map(_.userInput)
  def getWorkloads(workload: String) = rows.filter(_.workload === workload).map(_.workloads)
  def getLastUpdated(workload: String) = rows.filter(_.workload === workload).map(_.lastUpdated)

  /** Returns a query for the specified workload attribute value. Returns null if an invalid attribute name is given. */
  def getAttribute(workload: String, attrName: String): Query[_,_,Seq] = {
    val filter = rows.filter(_.workload === workload)
    // According to 1 post by a slick developer, there is not yet a way to do this properly dynamically
    return attrName match {
      case "owner" => filter.map(_.owner)
      case "label" => filter.map(_.label)
      case "description" => filter.map(_.description)
      case "public" => filter.map(_.public)
      case "workloadUrl" => filter.map(_.workloadUrl)
      case "version" => filter.map(_.version)
      case "arch" => filter.map(_.arch)
      case "downloadUrl" => filter.map(_.downloadUrl)
      case "apiSpec" => filter.map(_.apiSpec)
      case "userInput" => filter.map(_.userInput)
      case "workloads" => filter.map(_.workloads)
      case "lastUpdated" => filter.map(_.lastUpdated)
      case _ => null
    }
  }

  /** Returns the actions to delete the workload and the blockchains that reference it */
  def getDeleteActions(workload: String): DBIO[_] = getWorkload(workload).delete   // with the foreign keys set up correctly and onDelete=cascade, the db will automatically delete these associated blockchain rows
}

// This is the workload table minus the key - used as the data structure to return to the REST clients
class Workload(var owner: String, var label: String, var description: String, var public: Boolean, var workloadUrl: String, var version: String, var arch: String, var downloadUrl: String, var apiSpec: List[WMicroservices], var userInput: List[Map[String,String]], var workloads: List[MDockerImages], var lastUpdated: String) {
  def copy = new Workload(owner, label, description, public, workloadUrl, version, arch, downloadUrl, apiSpec, userInput, workloads, lastUpdated)
}


// Key is a sub-resource of workload
case class WorkloadKeyRow(keyId: String, workloadId: String, key: String, lastUpdated: String) {
  def toWorkloadKey = WorkloadKey(key, lastUpdated)

  def upsert: DBIO[_] = WorkloadKeysTQ.rows.insertOrUpdate(this)
}

class WorkloadKeys(tag: Tag) extends Table[WorkloadKeyRow](tag, "workloadkeys") {
  def keyId = column[String]("keyid")     // key - the key name
  def workloadId = column[String]("workloadid")               // additional key - the composite orgid/workloadid
  def key = column[String]("key")                   // the actual key content
  def lastUpdated = column[String]("lastupdated")
  def * = (keyId, workloadId, key, lastUpdated) <> (WorkloadKeyRow.tupled, WorkloadKeyRow.unapply)
  def primKey = primaryKey("pk_wkk", (keyId, workloadId))
  def workload = foreignKey("workload_fk", workloadId, WorkloadsTQ.rows)(_.workload, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
}

object WorkloadKeysTQ {
  val rows = TableQuery[WorkloadKeys]

  def getKeys(workloadId: String) = rows.filter(_.workloadId === workloadId)
  def getKey(workloadId: String, keyId: String) = rows.filter( r => {r.workloadId === workloadId && r.keyId === keyId} )
}

case class WorkloadKey(key: String, lastUpdated: String)

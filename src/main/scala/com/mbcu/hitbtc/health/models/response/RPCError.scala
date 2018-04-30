package com.mbcu.hitbtc.health.models.response

import play.api.libs.json.{Json, OFormat}

object RPCError {
  implicit val jsonFormat: OFormat[RPCError] = Json.format[RPCError]


}
case class RPCError (code : Int, message : Option[String], description : Option[String])

// deprecate this object because params is not consistent, sometimes object sometimes array
//object RPC {
//  implicit val jsonFormat = Json.format[RPC]
//}
//case class RPC (jsonrpc : String, id : Option[String], method : Option[String], result : Option[Boolean], error : Option[RPCError], params : Option[Seq[Order]])


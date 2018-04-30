package com.mbcu.hitbtc.health.models.internal

import play.api.libs.functional.syntax._
import play.api.libs.json._


case class Credentials(pKey: String, nonce: String, signature: String)

object Credentials {
  implicit val jsonFormat: OFormat[Credentials] = Json.format[Credentials]
}

case class Env(emails: Option[Seq[String]], sesKey: Option[String], sesSecret: Option[String], symbols : Seq[String])

object Env {
  implicit val jsonFormat: OFormat[Env] = Json.format[Env]

  object Implicits {
    implicit val envWrites: Writes[Env] {
      def writes(env: Env): JsValue
    } = new Writes[Env] {
      def writes(env: Env): JsValue = Json.obj(
        "emails" -> env.emails,
        "sesKey" -> env.sesKey,
        "sesSecret" -> env.sesSecret,
        "symbols" -> env.symbols
      )
    }

    implicit val envReads: Reads[Env] = (
      (JsPath \ "emails").readNullable[Seq[String]] and
        (JsPath \ "sesKey").readNullable[String] and
        (JsPath \ "sesSecret").readNullable[String] and
        (JsPath \ "symbols").read[Seq[String]]
      ) (Env.apply _)
  }

}

case class Config(credentials: Credentials, env: Env)

object Config {
  implicit val jsonFormat: OFormat[Config] = Json.format[Config]
}
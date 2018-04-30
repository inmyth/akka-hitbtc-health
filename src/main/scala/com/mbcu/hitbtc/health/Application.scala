package com.mbcu.hitbtc.health

import java.util.logging.Logger

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.mbcu.hitbtc.health.actors.MainActor
import com.mbcu.hitbtc.health.models.response.RPCError
import com.mbcu.hitbtc.health.utils.{MyLogging, MyLoggingSingle, MyUtils}

import scala.collection.immutable.ListMap


object Application extends App with MyLogging {

  import akka.actor.Props
  import com.mbcu.hitbtc.health.actors.FileActor

  override def main(args: Array[String]) {
    implicit val system: ActorSystem = akka.actor.ActorSystem("mmm")
    implicit val materializer: ActorMaterializer = akka.stream.ActorMaterializer()

    if (args.length != 2){
      println("Requires two arguments : <config file path>  <log directory path>")
      System.exit(-1)
    }
    MyLoggingSingle.init(args(1))
    info(s"START UP ${MyUtils.date()}")

    val mainActor = system.actorOf(Props(new MainActor(args(0))), name = "main")
    mainActor ! "start"
  }

}

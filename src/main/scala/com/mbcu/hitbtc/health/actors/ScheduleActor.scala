package com.mbcu.hitbtc.health.actors

import akka.actor.Actor

class ScheduleActor extends Actor {

  override def receive: Receive = {

    case "log orderbooks" =>
      sender() ! "log orderbooks"
  }

}

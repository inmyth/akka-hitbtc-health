package com.mbcu.hitbtc.health.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.dispatch.ExecutionContexts._
import com.mbcu.hitbtc.health.actors.ParserActor._
import com.mbcu.hitbtc.health.actors.SesActor.{CacheMessages, MailSent, MailTimer}
import com.mbcu.hitbtc.health.actors.WsActor._
import com.mbcu.hitbtc.health.models.internal.Config
import com.mbcu.hitbtc.health.models.request.SubscribeMarket.Market
import com.mbcu.hitbtc.health.models.request.{Login, SubscribeMarket, SubscribeReports}
import com.mbcu.hitbtc.health.models.response.RPCError
import com.mbcu.hitbtc.health.utils.MyLogging
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object MainActor {

  def props(configPath : String): Props = Props(new MainActor(configPath))

  case class ConfigReady(config : Try[Config])

  case class Shutdown(code :Int)

  case class HandleRPCError(er : RPCError, id : Option[String], code : Option[Int] = None)

  case class HandleError(msg :String, code : Option[Int] = None)

  case class ReqTick(symbol : String)

  case class UnreqTick(symbol : String)

}

class MainActor(configPath : String) extends Actor with MyLogging {
  import com.mbcu.hitbtc.health.actors.MainActor._
  val ENDPOINT = "wss://api.hitbtc.com/api/2/ws"
  private var config: Option[Config] = None
  private var ws: Option[ActorRef] = None
  private var parser: Option[ActorRef] = None
  private var ses : Option[ActorRef] = None
  implicit val ec: ExecutionContextExecutor = global

  override def receive: Receive = {

    case "start" =>
      val fileActor = context.actorOf(Props(new FileActor(configPath)))
      fileActor ! "start"

    case ConfigReady(tcfg) =>
      tcfg match {
        case Failure(f) => println(
          s"""Config error
            |$f
          """.stripMargin)
          System.exit(-1)
        case Success(cfg) =>
          config = Some(cfg)
          ses = Some(context.actorOf(Props(new SesActor(cfg.env.sesKey, cfg.env.sesSecret, cfg.env.emails)), name = "ses"))
          ses foreach (_ ! "start")
          ws = Some(context.actorOf(Props(new WsActor(ENDPOINT)), name = "ws"))
          ws.foreach(_ ! "start")
      }

    case WsConnected =>
      info(s"Connected to $ENDPOINT")
      parser = Some(context.actorOf(Props(new ParserActor(config))))
      self ! "login"

    case WsDisconnected =>
      info(s"Disconnected")

    case "login" => config.foreach(c => ws.foreach(_ ! SendJs(Json.toJson(Login.from(c)))))

    case LoginSuccess =>
      ws.foreach(_ ! SendJs(SubscribeReports.toJsValue))
      config match {
        case Some(c) => c.env.symbols.foreach(self ! ReqTick(_))
        case _ =>
      }

    case SubsribeReportsSuccess => info("Subscribe Reports success")

    case activeOrders : ActiveOrders =>

    case ReqTick(symbol) => ws foreach(_ ! SendJs(SubscribeMarket(Market.subscribeTicker, symbol)))

    case UnreqTick(symbol) => ws foreach(_ ! SendJs(SubscribeMarket(Market.unsubscribeTicker, symbol)))

    case GotTicker(ticker) => println(ticker)

    case ErrorSymbol(er, id) => self ! HandleRPCError(er, id, Some(-1))

    case ErrorAuthFailed(er, id) => self ! HandleRPCError(er, id, Some(-1))

    case ErrorServer(er, id) => self ! HandleRPCError(er, id, Some(1))

    case WSError(er, code) => self ! HandleError(er , code)

    case wsGotText : WsGotText =>
      parser match {
        case Some(parserActor) => parserActor ! wsGotText
        case _ => warn("MainActor#wsGotText : _")
      }

    case HandleRPCError(er, id, code) =>
      var s = s"""${er.toString}
         |id $id""".stripMargin
      handleError(s, code)

    case HandleError(msg, code) => handleError(msg, code)

    case MailTimer =>
      ses match {
        case Some(s) => context.system.scheduler.scheduleOnce(5 second, s, "execute send")
        case _ => warn("MainActor#MailTimer no ses actor")
      }

    case MailSent(t, shutdownCode) =>
      t match {
        case Success(_) => info("Email Sent")
        case Failure(c) => info(
          s"""Failed sending email
            |${c.getMessage}
          """.stripMargin)
      }
      shutdownCode match {
        case Some(code) => self ! Shutdown(code)
        case _ =>
      }

    case Shutdown(code) =>
      info(s"Stopping application, code $code")
      implicit val executionContext: ExecutionContext = context.system.dispatcher
      context.system.scheduler.scheduleOnce(Duration.Zero)(System.exit(code))
  }

  def handleError(s: String, code : Option[Int]) : Unit = {
    error(s)
    ses foreach(_ ! CacheMessages(s, code))
  }


}

package com.lolboxen.akka.tcp

import akka.actor._
import akka.util.ByteString

/**
  * Created by trent ahrens on 2/15/16.
  */
object TcpExtention extends ExtensionId[TcpImpl] {
  override def createExtension(system: ExtendedActorSystem): TcpImpl = new TcpImpl(system)
}

class TcpImpl(system: ExtendedActorSystem) extends Extension {
  val manager = system.actorOf(Props[TcpManager], "com_lolboxen_akka_tcp_TcpManager")
}

object Tcp {
  def apply(system: ActorSystem): ActorRef = TcpExtention(system).manager

  case class Connect(host: String, port: Int, proxy: Option[Socks5])

  case class ConnectFailed(cause: Throwable)

  case object Connected

  case object Close

  trait ConnectionClosed {
    def isErrorClosed: Boolean = false

    def getErrorCause: Throwable = null
  }

  case class ErrorClosed(cause: Throwable) extends ConnectionClosed {
    override def isErrorClosed = true
    override def getErrorCause = cause
  }

  case class Received(data: ByteString)

  case class Write(data: ByteString)

  case class Socks5(host: String, port: Int, username: Option[String], password: Option[String])
}

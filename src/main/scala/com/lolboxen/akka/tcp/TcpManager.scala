package com.lolboxen.akka.tcp

import akka.actor.{Props, OneForOneStrategy, SupervisorStrategy, Actor}
import io.netty.channel.nio.NioEventLoopGroup

/**
  * Created by trent ahrens on 2/15/16.
  */
class TcpManager extends Actor {

  val eventLoop = new NioEventLoopGroup()


  @throws[Exception](classOf[Exception])
  override def postStop(): Unit =
    eventLoop.shutdownGracefully()

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy()({
      case _: Exception => SupervisorStrategy.Stop
    })

  override def receive: Receive = {
    case msg: Tcp.Connect =>
      context.actorOf(Props(classOf[TcpConnection], eventLoop, sender())) ! msg
  }
}

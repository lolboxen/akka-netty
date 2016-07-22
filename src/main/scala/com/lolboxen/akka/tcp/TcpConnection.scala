package com.lolboxen.akka.tcp

import java.net.InetSocketAddress
import java.util

import akka.actor.{Terminated, Actor, ActorRef}
import akka.util.ByteString
import com.lolboxen.akka.tcp.Tcp._
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel._
import io.netty.handler.codec.{MessageToMessageEncoder, MessageToMessageDecoder}
import io.netty.handler.proxy.Socks5ProxyHandler
import io.netty.handler.timeout.ReadTimeoutHandler

/**
  * Created by trent ahrens on 2/15/16.
  */


private case class ChannelConnected(channel: Channel)

private case class ChannelException(cause: Throwable)

class TcpConnection(eventLoopGroup: EventLoopGroup, commander: ActorRef) extends Actor {
  import context._

  watch(commander)

  var channel: Channel = null

  override def postStop(): Unit =
    if (channel != null) channel.close()

  override def receive: Receive = unconnected

  def unconnected: Receive = onCommanderTermination orElse {
    case Tcp.Connect(host, port, proxy) =>
      new Bootstrap()
        .group(eventLoopGroup)
        .channel(classOf[NioSocketChannel])
        .handler(new TcpChannelInitializer(self, proxy))
        .connect(host, port).addListener(new ChannelFutureListener {
        override def operationComplete(future: ChannelFuture): Unit = {
          if (!future.isSuccess)
            self ! ConnectFailed(future.cause())
          else if (future.isCancelled)
            self ! ConnectFailed(new Exception("connect was canceled"))
          else
            self ! ChannelConnected(future.channel())
        }
      })
      become(connecting)
  }

  def connecting: Receive = onCommanderTermination orElse {
    case msg: ConnectFailed =>
      commander ! msg
      stop(self)

    case ChannelException(cause) =>
      commander ! ConnectFailed(cause)
      stop(self)

    case ChannelConnected(c) =>
      commander ! Connected
      channel = c
      become(connected)
  }

  def connected: Receive = onCommanderTermination orElse {
    case ChannelException(cause) =>
      channel.close()
      commander ! ErrorClosed(cause)
      stop(self)

    case msg: Received =>
      commander ! msg

    case Write(data) =>
      channel.writeAndFlush(data)

    case Close =>
      stop(self)
  }

  def onCommanderTermination: Receive = {
    case _: Terminated =>
      stop(self)
  }
}

private class TcpChannelInitializer(ref: ActorRef, proxy: Option[Socks5]) extends ChannelInitializer[NioSocketChannel] {
  override def initChannel(ch: NioSocketChannel): Unit = {
    val pipeline = ch.pipeline()

    proxy.foreach {
      case Socks5(proxyHost, proxyPort, proxyUser, proxyPass) =>
        pipeline.addLast(new Socks5ProxyHandler(new InetSocketAddress(proxyHost, proxyPort), proxyUser.orNull, proxyPass.orNull))
    }

    pipeline
      .addLast(new ReadTimeoutHandler(300))
      .addLast(new ByteStringDecoder)
      .addLast(new ByteStringEncoder)
      .addLast(new TcpInboundHandler(ref))
      .addLast(new TcpExceptionHandler(ref))
  }
}

private class TcpExceptionHandler(ref: ActorRef) extends ChannelHandlerAdapter {
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit =
    ref ! ChannelException(cause)
}

private class TcpInboundHandler(ref: ActorRef) extends SimpleChannelInboundHandler[ByteString] {
  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteString): Unit = {
    ref ! Received(msg)
  }
}

private class ByteStringDecoder extends MessageToMessageDecoder[ByteBuf] {
  override def decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: util.List[AnyRef]): Unit = {
    val len = msg.readableBytes()
    val bytes = new Array[Byte](len)
    msg.readBytes(bytes)
    out.add(ByteString(bytes))
  }
}

private class ByteStringEncoder extends MessageToMessageEncoder[ByteString] {
  override def encode(ctx: ChannelHandlerContext, msg: ByteString, out: util.List[AnyRef]): Unit = {
    out.add(ctx.alloc().buffer(msg.length).writeBytes(msg.toByteBuffer))
  }
}
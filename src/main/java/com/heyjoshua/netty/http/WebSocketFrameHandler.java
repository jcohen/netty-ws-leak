package com.heyjoshua.netty.http;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Sharable
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
  private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketFrameHandler.class);
  private static final AtomicInteger ACTIVE_CONNECTIONS = new AtomicInteger();

  private final Counter messageReceived;
  private final Counter connects;
  private final Counter disconnects;

  @Inject
  WebSocketFrameHandler(MeterRegistry meterRegistry) {
    this.messageReceived = meterRegistry.counter("websocket.message_received");
    this.connects = meterRegistry.counter("websocket.connect");
    this.disconnects = meterRegistry.counter("websocket.disconnect");

    meterRegistry.gauge(
        "active_connections",
        Collections.singletonList(Tag.of("map", "false")),
        ACTIVE_CONNECTIONS,
        AtomicInteger::get);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    super.userEventTriggered(ctx, evt);
    if (evt instanceof HandshakeComplete) {
      websocketConnected(ctx);
    }
  }

  private void websocketConnected(ChannelHandlerContext ctx) {
    connects.increment();
    LOGGER.trace("Web socket connected: {}.", ctx.channel());

    int size = ACTIVE_CONNECTIONS.incrementAndGet();
    if ((size % 100) == 0) {
      LOGGER.info("Total connections: {}", size);
    }

    ctx.channel().writeAndFlush(new TextWebSocketFrame("{}"));
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    LOGGER.trace("Web socket disconnected: {}.", ctx.channel());
    // TODO(jcohen): Figure out how to detect connection errors and increment a separate counter.
    disconnects.increment();

    int size = ACTIVE_CONNECTIONS.decrementAndGet();
    if ((size % 100) == 0) {
      LOGGER.info("Total connections: {}", size);
    }
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
    // Ping, pong and close frames have already been handled by `WebSocketServerProtocolHandler`.

    if (frame instanceof TextWebSocketFrame) {
      messageReceived.increment();
      String message = ((TextWebSocketFrame) frame).text();
      LOGGER.trace("Received text web socket frame: {}", message);
      //      ctx.channel().writeAndFlush(new TextWebSocketFrame(message.toUpperCase(Locale.US)));
    } else {
      throw new UnsupportedOperationException(
          "Unsupported frame type: " + frame.getClass().getName());
    }
  }
}

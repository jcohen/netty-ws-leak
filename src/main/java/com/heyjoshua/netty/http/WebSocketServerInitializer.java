package com.heyjoshua.netty.http;

import static java.util.Objects.requireNonNull;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {
  private final WebSocketFrameHandler webSocketFrameHandler;

  @Inject
  WebSocketServerInitializer(WebSocketFrameHandler webSocketFrameHandler) {

    this.webSocketFrameHandler = requireNonNull(webSocketFrameHandler);
  }

  @Override
  protected void initChannel(SocketChannel ch) {
    ChannelPipeline pipeline = ch.pipeline();

    pipeline.addLast(new HttpServerCodec());
    pipeline.addLast(new HttpObjectAggregator(65536));
    pipeline.addLast(new WebSocketServerCompressionHandler());
    pipeline.addLast(new WebSocketServerProtocolHandler("/", null, true));
    pipeline.addLast(webSocketFrameHandler);
  }
}

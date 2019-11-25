package com.heyjoshua.netty.http;

import static java.util.Objects.requireNonNull;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocatorMetric;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import java.util.Arrays;
import javax.inject.Inject;

public class ExampleServer {
  private final WebSocketServerInitializer channelInitializer;
  private final MeterRegistry meterRegistry;

  @Inject
  public ExampleServer(WebSocketServerInitializer channelInitializer, MeterRegistry meterRegistry) {

    this.channelInitializer = requireNonNull(channelInitializer);

    this.meterRegistry = requireNonNull(meterRegistry);
  }

  public void start() throws InterruptedException {
    ResourceLeakDetector.setLevel(Level.PARANOID);

    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      ServerBootstrap b = new ServerBootstrap();
      b.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          // Disabled because it's too verbose to see other log output.
          // .handler(new LoggingHandler(LogLevel.DEBUG))
          .childHandler(channelInitializer)
          .option(ChannelOption.SO_BACKLOG, 128)
          .childOption(ChannelOption.SO_KEEPALIVE, true)
          .childOption(ChannelOption.SO_REUSEADDR, true);

      ChannelFuture f = b.bind(8080).sync();

      ByteBufAllocatorMetric pooledMetric = PooledByteBufAllocator.DEFAULT.metric();

      meterRegistry.gauge(
          "netty.memory.used",
          Arrays.asList(Tag.of("id", "direct"), Tag.of("type", "pooled")),
          pooledMetric,
          ByteBufAllocatorMetric::usedDirectMemory);

      meterRegistry.gauge(
          "netty.memory.used",
          Arrays.asList(Tag.of("id", "heap"), Tag.of("type", "pooled")),
          pooledMetric,
          ByteBufAllocatorMetric::usedHeapMemory);

      ByteBufAllocatorMetric unpooledMetric = UnpooledByteBufAllocator.DEFAULT.metric();
      meterRegistry.gauge(
          "netty.memory.used",
          Arrays.asList(Tag.of("id", "direct"), Tag.of("type", "unpooled")),
          unpooledMetric,
          ByteBufAllocatorMetric::usedDirectMemory);

      meterRegistry.gauge(
          "netty.memory.used",
          Arrays.asList(Tag.of("id", "heap"), Tag.of("type", "unpooled")),
          unpooledMetric,
          ByteBufAllocatorMetric::usedHeapMemory);

      // Wait until the server socket is closed.
      f.channel().closeFuture().sync();
    } finally {
      workerGroup.shutdownGracefully().sync();
      bossGroup.shutdownGracefully();
    }
  }
}

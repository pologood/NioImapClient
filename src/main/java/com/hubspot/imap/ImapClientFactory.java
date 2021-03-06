package com.hubspot.imap;

import java.io.Closeable;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hubspot.imap.client.ImapClient;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;


public class ImapClientFactory implements Closeable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImapClientFactory.class);

  private final ImapConfiguration configuration;
  private final Bootstrap bootstrap;
  private final EventLoopGroup eventLoopGroup;
  private final EventExecutorGroup promiseExecutorGroup;
  private final EventExecutorGroup idleExecutorGroup;

  public ImapClientFactory(ImapConfiguration configuration) {
    this.configuration = configuration;
    this.bootstrap = new Bootstrap();

    final Class<? extends Channel> channelClass;
    if (configuration.useEpoll() && SystemUtils.IS_OS_LINUX) {
      LOGGER.info("Using epoll eventloop");
      this.eventLoopGroup = new EpollEventLoopGroup(configuration.numEventLoopThreads());
      channelClass = EpollSocketChannel.class;
    } else {
      this.eventLoopGroup = new NioEventLoopGroup(configuration.numEventLoopThreads());
      channelClass = NioSocketChannel.class;
    }

    ThreadFactoryBuilder baseThreadFactoryBuilder = new ThreadFactoryBuilder()
        .setDaemon(true)
        .setUncaughtExceptionHandler((t, e) -> LOGGER.error("Uncaught exception on thread {}", t.getName(), e));

    this.promiseExecutorGroup = new DefaultEventExecutorGroup(configuration.numExecutorThreads(), baseThreadFactoryBuilder.setNameFormat("imap-promise-executor-%d").build());
    this.idleExecutorGroup = new DefaultEventExecutorGroup(configuration.numExecutorThreads(), baseThreadFactoryBuilder.setNameFormat("imap-idle-executor-%d").build());

    SslContext context = null;
    if (configuration.useSsl()) {
      try {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(((KeyStore) null));

        context = SslContextBuilder.forClient()
            .trustManager(trustManagerFactory)
            .build();
      } catch (NoSuchAlgorithmException | SSLException | KeyStoreException e) {
        throw Throwables.propagate(e);
      }
    }

    bootstrap.group(eventLoopGroup)
        .option(ChannelOption.SO_LINGER, configuration.soLinger())
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, configuration.connectTimeoutMillis())
        .option(ChannelOption.SO_KEEPALIVE, false)
        .option(ChannelOption.AUTO_CLOSE, true)
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
        .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
        .handler(configuration.useSsl() ? new ImapChannelInitializer(context, configuration) : new ImapChannelInitializer(configuration));

    bootstrap.channel(channelClass);
  }

  private ImapClient create(String clientName, String userName, String authToken, Optional<ImapConfiguration> newConfig) {
    ImapConfiguration finalConfig = newConfig.orElse(configuration);
    return new ImapClient(finalConfig, bootstrap, promiseExecutorGroup, idleExecutorGroup, clientName, userName, authToken);
  }

  public Future<ImapClient> connect(String userName, String authToken) throws InterruptedException {
    return connect(UUID.randomUUID().toString(), userName, authToken, Optional.empty());
  }

  public Future<ImapClient> connect(String clientName, String userName, String authToken) throws InterruptedException {
    return connect(clientName, userName, authToken, Optional.empty());
  }

  public Future<ImapClient> connect(String clientName, String userName, String authToken, Optional<ImapConfiguration> configuration) throws InterruptedException {
    ImapClient client = create(clientName, userName, authToken, configuration);

    return client.connect();
  }

  @Override
  public void close() {
    promiseExecutorGroup.shutdownGracefully();
    idleExecutorGroup.shutdownGracefully();
    eventLoopGroup.shutdownGracefully();
  }
}

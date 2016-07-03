package com.hubspot.imap;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import com.google.common.net.HostAndPort;

import io.netty.channel.epoll.EpollMode;

@Immutable
@Style(
    typeAbstract = {"*IF"},
    typeImmutable = "*"
)
public interface ImapConfigurationIF {
  HostAndPort hostAndPort();

  AuthType authType();

  @Default
  default EpollMode epollMode() {
    return EpollMode.EDGE_TRIGGERED;
  }

  @Default
  default boolean useEpoll() {
    return false;
  }

  @Default
  default boolean useSsl() {
    return true;
  }

  int noopKeepAliveIntervalSec();

  @Default
  default int socketTimeoutMs() {
    return 1000;
  }

  @Default
  default int writeBackOffMs() {
    return 10;
  }

  @Default
  default int numEventLoopThreads() {
    return 0;
  }

  @Default
  default int numExecutorThreads() {
    return Runtime.getRuntime().availableProcessors() * 2;
  }

  @Default
  default int maxLineLength() {
    return 100000;
  }

  @Default
  default int defaultResponseBufferSize() {
    return 1000;
  }

  @Default
  default int connectTimeoutMillis() {
    return 30000;
  }

  @Default
  default int closeTimeoutSec() {
    return 30;
  }

  enum AuthType {
    PASSWORD,
    XOAUTH2;
  }
}
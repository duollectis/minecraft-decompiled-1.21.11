package net.minecraft.network;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalIoHandler;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.util.concurrent.ThreadFactory;
import org.jspecify.annotations.Nullable;

public abstract class NetworkingBackend {
   private static final NetworkingBackend NIO = new NetworkingBackend("NIO", NioSocketChannel.class, NioServerSocketChannel.class) {
      @Override
      protected IoHandlerFactory newFactory() {
         return NioIoHandler.newFactory();
      }
   };
   private static final NetworkingBackend EPOLL = new NetworkingBackend("Epoll", EpollSocketChannel.class, EpollServerSocketChannel.class) {
      @Override
      protected IoHandlerFactory newFactory() {
         return EpollIoHandler.newFactory();
      }
   };
   private static final NetworkingBackend KQUEUE = new NetworkingBackend("Kqueue", KQueueSocketChannel.class, KQueueServerSocketChannel.class) {
      @Override
      protected IoHandlerFactory newFactory() {
         return KQueueIoHandler.newFactory();
      }
   };
   private static final NetworkingBackend LOCAL = new NetworkingBackend("Local", LocalChannel.class, LocalServerChannel.class) {
      @Override
      protected IoHandlerFactory newFactory() {
         return LocalIoHandler.newFactory();
      }
   };
   private final String name;
   private final Class<? extends Channel> channelClass;
   private final Class<? extends ServerChannel> serverChannelClass;
   private volatile @Nullable EventLoopGroup eventLoopGroup;

   public static NetworkingBackend remote(boolean useEpoll) {
      if (useEpoll) {
         if (KQueue.isAvailable()) {
            return KQUEUE;
         }

         if (Epoll.isAvailable()) {
            return EPOLL;
         }
      }

      return NIO;
   }

   public static NetworkingBackend local() {
      return LOCAL;
   }

   NetworkingBackend(String name, Class<? extends Channel> channelClass, Class<? extends ServerChannel> serverChannelClass) {
      this.name = name;
      this.channelClass = channelClass;
      this.serverChannelClass = serverChannelClass;
   }

   private ThreadFactory createThreadFactory() {
      return new ThreadFactoryBuilder().setNameFormat("Netty " + this.name + " IO #%d").setDaemon(true).build();
   }

   protected abstract IoHandlerFactory newFactory();

   private EventLoopGroup createEventLoopGroup() {
      return new MultiThreadIoEventLoopGroup(this.createThreadFactory(), this.newFactory());
   }

   public EventLoopGroup getEventLoopGroup() {
      EventLoopGroup eventLoopGroup = this.eventLoopGroup;
      if (eventLoopGroup == null) {
         synchronized (this) {
            eventLoopGroup = this.eventLoopGroup;
            if (eventLoopGroup == null) {
               eventLoopGroup = this.createEventLoopGroup();
               this.eventLoopGroup = eventLoopGroup;
            }
         }
      }

      return eventLoopGroup;
   }

   public Class<? extends Channel> getChannelClass() {
      return this.channelClass;
   }

   public Class<? extends ServerChannel> getServerChannelClass() {
      return this.serverChannelClass;
   }
}

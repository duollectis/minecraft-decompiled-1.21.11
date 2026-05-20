package com.mojang.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nullable;

public class LogQueues {
   private static final Map<String, BlockingQueue<String>> QUEUES = new HashMap<>();
   private static final ReentrantReadWriteLock QUEUE_LOCK = new ReentrantReadWriteLock();

   public static BlockingQueue<String> getOrCreateQueue(String target) {
      try {
         QUEUE_LOCK.readLock().lock();
         BlockingQueue<String> queue = QUEUES.get(target);
         if (queue != null) {
            return queue;
         }
      } finally {
         QUEUE_LOCK.readLock().unlock();
      }

      BlockingQueue var11;
      try {
         QUEUE_LOCK.writeLock().lock();
         var11 = QUEUES.computeIfAbsent(target, k -> new LinkedBlockingQueue<>());
      } finally {
         QUEUE_LOCK.writeLock().unlock();
      }

      return var11;
   }

   @Nullable
   public static String getNextLogEvent(String queueName) {
      QUEUE_LOCK.readLock().lock();
      BlockingQueue<String> queue = QUEUES.get(queueName);
      QUEUE_LOCK.readLock().unlock();
      if (queue != null) {
         try {
            return queue.take();
         } catch (InterruptedException var3) {
         }
      }

      return null;
   }
}

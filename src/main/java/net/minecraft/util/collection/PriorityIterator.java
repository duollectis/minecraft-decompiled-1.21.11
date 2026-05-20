package net.minecraft.util.collection;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Deque;
import org.jspecify.annotations.Nullable;

public final class PriorityIterator<T> extends AbstractIterator<T> {
   private static final int LOWEST_PRIORITY = Integer.MIN_VALUE;
   private @Nullable Deque<T> maxPriorityQueue = null;
   private int maxPriority = Integer.MIN_VALUE;
   private final Int2ObjectMap<Deque<T>> queuesByPriority = new Int2ObjectOpenHashMap();

   public void enqueue(T value, int priority) {
      if (priority == this.maxPriority && this.maxPriorityQueue != null) {
         this.maxPriorityQueue.addLast(value);
      } else {
         Deque<T> deque = (Deque<T>)this.queuesByPriority.computeIfAbsent(priority, p -> Queues.newArrayDeque());
         deque.addLast(value);
         if (priority >= this.maxPriority) {
            this.maxPriorityQueue = deque;
            this.maxPriority = priority;
         }
      }
   }

   protected @Nullable T computeNext() {
      if (this.maxPriorityQueue == null) {
         return (T)this.endOfData();
      } else {
         T object = this.maxPriorityQueue.removeFirst();
         if (object == null) {
            return (T)this.endOfData();
         } else {
            if (this.maxPriorityQueue.isEmpty()) {
               this.refreshMaxPriority();
            }

            return object;
         }
      }
   }

   private void refreshMaxPriority() {
      int i = Integer.MIN_VALUE;
      Deque<T> deque = null;
      ObjectIterator var3 = Int2ObjectMaps.fastIterable(this.queuesByPriority).iterator();

      while (var3.hasNext()) {
         Entry<Deque<T>> entry = (Entry<Deque<T>>)var3.next();
         Deque<T> deque2 = (Deque<T>)entry.getValue();
         int j = entry.getIntKey();
         if (j > i && !deque2.isEmpty()) {
            i = j;
            deque = deque2;
            if (j == this.maxPriority - 1) {
               break;
            }
         }
      }

      this.maxPriority = i;
      this.maxPriorityQueue = deque;
   }
}

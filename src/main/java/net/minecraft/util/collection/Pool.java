package net.minecraft.util.collection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

public final class Pool<E> {
   private static final int FLATTENED_CONTENT_THRESHOLD = 64;
   private final int totalWeight;
   private final List<Weighted<E>> entries;
   private final Pool.@Nullable Content<E> content;

   Pool(List<? extends Weighted<E>> entries) {
      this.entries = List.copyOf(entries);
      this.totalWeight = Weighting.getWeightSum(entries, Weighted::weight);
      if (this.totalWeight == 0) {
         this.content = null;
      } else if (this.totalWeight < 64) {
         this.content = new Pool.FlattenedContent<>(this.entries, this.totalWeight);
      } else {
         this.content = new Pool.WrappedContent<>(this.entries);
      }
   }

   public static <E> Pool<E> empty() {
      return new Pool<>(List.of());
   }

   public static <E> Pool<E> of(E entry) {
      return new Pool<>(List.of(new Weighted<>(entry, 1)));
   }

   @SafeVarargs
   public static <E> Pool<E> of(Weighted<E>... entries) {
      return new Pool<>(List.of(entries));
   }

   public static <E> Pool<E> of(List<Weighted<E>> entries) {
      return new Pool<>(entries);
   }

   public static <E> Pool.Builder<E> builder() {
      return new Pool.Builder<>();
   }

   public boolean isEmpty() {
      return this.entries.isEmpty();
   }

   public <T> Pool<T> transform(Function<E, T> function) {
      return new Pool(Lists.transform(this.entries, entry -> entry.transform(function)));
   }

   public Optional<E> getOrEmpty(Random random) {
      if (this.content == null) {
         return Optional.empty();
      } else {
         int i = random.nextInt(this.totalWeight);
         return Optional.of(this.content.get(i));
      }
   }

   public E get(Random random) {
      if (this.content == null) {
         throw new IllegalStateException("Weighted list has no elements");
      } else {
         int i = random.nextInt(this.totalWeight);
         return this.content.get(i);
      }
   }

   public List<Weighted<E>> getEntries() {
      return this.entries;
   }

   public static <E> Codec<Pool<E>> createCodec(Codec<E> entryCodec) {
      return Weighted.createCodec(entryCodec).listOf().xmap(Pool::of, Pool::getEntries);
   }

   public static <E> Codec<Pool<E>> createCodec(MapCodec<E> entryCodec) {
      return Weighted.createCodec(entryCodec).listOf().xmap(Pool::of, Pool::getEntries);
   }

   public static <E> Codec<Pool<E>> createNonEmptyCodec(Codec<E> entryCodec) {
      return Codecs.nonEmptyList(Weighted.createCodec(entryCodec).listOf()).xmap(Pool::of, Pool::getEntries);
   }

   public static <E> Codec<Pool<E>> createNonEmptyCodec(MapCodec<E> entryCodec) {
      return Codecs.nonEmptyList(Weighted.createCodec(entryCodec).listOf()).xmap(Pool::of, Pool::getEntries);
   }

   public static <E, B extends ByteBuf> PacketCodec<B, Pool<E>> createPacketCodec(PacketCodec<B, E> entryCodec) {
      return Weighted.createPacketCodec(entryCodec).collect(PacketCodecs.toList()).xmap(Pool::of, Pool::getEntries);
   }

   public boolean contains(E value) {
      for (Weighted<E> weighted : this.entries) {
         if (weighted.value().equals(value)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean equals(@Nullable Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof Pool<?> pool) ? false : this.totalWeight == pool.totalWeight && Objects.equals(this.entries, pool.entries);
      }
   }

   @Override
   public int hashCode() {
      int i = this.totalWeight;
      return 31 * i + this.entries.hashCode();
   }

   public static class Builder<E> {
      private final com.google.common.collect.ImmutableList.Builder<Weighted<E>> entries = ImmutableList.builder();

      public Pool.Builder<E> add(E object) {
         return this.add(object, 1);
      }

      public Pool.Builder<E> add(E object, int weight) {
         this.entries.add(new Weighted<>(object, weight));
         return this;
      }

      public Pool<E> build() {
         return new Pool<>(this.entries.build());
      }
   }

   interface Content<E> {
      E get(int i);
   }

   static class FlattenedContent<E> implements Pool.Content<E> {
      private final Object[] entries;

      FlattenedContent(List<Weighted<E>> entries, int totalWeight) {
         this.entries = new Object[totalWeight];
         int i = 0;

         for (Weighted<E> weighted : entries) {
            int j = weighted.weight();
            Arrays.fill(this.entries, i, i + j, weighted.value());
            i += j;
         }
      }

      @Override
      public E get(int i) {
         return (E)this.entries[i];
      }
   }

   static class WrappedContent<E> implements Pool.Content<E> {
      private final Weighted<?>[] entries;

      WrappedContent(List<Weighted<E>> entries) {
         this.entries = entries.toArray(Weighted[]::new);
      }

      @Override
      public E get(int i) {
         for (Weighted<?> weighted : this.entries) {
            i -= weighted.weight();
            if (i < 0) {
               return (E)weighted.value();
            }
         }

         throw new IllegalStateException(i + " exceeded total weight");
      }
   }
}

package net.minecraft.world.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.encoding.VarInts;
import net.minecraft.util.collection.IndexedIterable;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

public class SingularPalette<T> implements Palette<T> {
   private @Nullable T entry;

   public SingularPalette(List<T> idList) {
      if (!idList.isEmpty()) {
         Validate.isTrue(idList.size() <= 1, "Can't initialize SingleValuePalette with %d values.", idList.size());
         this.entry = idList.getFirst();
      }
   }

   public static <A> Palette<A> create(int bitSize, List<A> idList) {
      return new SingularPalette<>(idList);
   }

   @Override
   public int index(T object, PaletteResizeListener<T> listener) {
      if (this.entry != null && this.entry != object) {
         return listener.onResize(1, object);
      } else {
         this.entry = object;
         return 0;
      }
   }

   @Override
   public boolean hasAny(Predicate<T> predicate) {
      if (this.entry == null) {
         throw new IllegalStateException("Use of an uninitialized palette");
      } else {
         return predicate.test(this.entry);
      }
   }

   @Override
   public T get(int id) {
      if (this.entry != null && id == 0) {
         return this.entry;
      } else {
         throw new IllegalStateException("Missing Palette entry for id " + id + ".");
      }
   }

   @Override
   public void readPacket(PacketByteBuf buf, IndexedIterable<T> idList) {
      this.entry = idList.getOrThrow(buf.readVarInt());
   }

   @Override
   public void writePacket(PacketByteBuf buf, IndexedIterable<T> idList) {
      if (this.entry == null) {
         throw new IllegalStateException("Use of an uninitialized palette");
      } else {
         buf.writeVarInt(idList.getRawId(this.entry));
      }
   }

   @Override
   public int getPacketSize(IndexedIterable<T> idList) {
      if (this.entry == null) {
         throw new IllegalStateException("Use of an uninitialized palette");
      } else {
         return VarInts.getSizeInBytes(idList.getRawId(this.entry));
      }
   }

   @Override
   public int getSize() {
      return 1;
   }

   @Override
   public Palette<T> copy() {
      if (this.entry == null) {
         throw new IllegalStateException("Use of an uninitialized palette");
      } else {
         return this;
      }
   }
}

package net.minecraft.world.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.collection.IndexedIterable;

public interface Palette<T> {
   int index(T object, PaletteResizeListener<T> listener);

   boolean hasAny(Predicate<T> predicate);

   T get(int id);

   void readPacket(PacketByteBuf buf, IndexedIterable<T> idList);

   void writePacket(PacketByteBuf buf, IndexedIterable<T> idList);

   int getPacketSize(IndexedIterable<T> idList);

   int getSize();

   Palette<T> copy();

   public interface Factory {
      <A> Palette<A> create(int bits, List<A> values);
   }
}

package net.minecraft.nbt;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractStringBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class NbtOps implements DynamicOps<NbtElement> {
   public static final NbtOps INSTANCE = new NbtOps();

   private NbtOps() {
   }

   public NbtElement empty() {
      return NbtEnd.INSTANCE;
   }

   public NbtElement emptyList() {
      return new NbtList();
   }

   public NbtElement emptyMap() {
      return new NbtCompound();
   }

   public <U> U convertTo(DynamicOps<U> dynamicOps, NbtElement nbtElement) {
      return (U)(switch (nbtElement) {
         case NbtEnd nbtEnd -> (Object)dynamicOps.empty();
         case NbtByte(byte var34) -> (Object)dynamicOps.createByte(var34);
         case NbtShort(short var35) -> (Object)dynamicOps.createShort(var35);
         case NbtInt(int var36) -> (Object)dynamicOps.createInt(var36);
         case NbtLong(long var37) -> (Object)dynamicOps.createLong(var37);
         case NbtFloat(float var38) -> (Object)dynamicOps.createFloat(var38);
         case NbtDouble(double var39) -> (Object)dynamicOps.createDouble(var39);
         case NbtByteArray nbtByteArray -> (Object)dynamicOps.createByteList(ByteBuffer.wrap(nbtByteArray.getByteArray()));
         case NbtString(String var40) -> (Object)dynamicOps.createString(var40);
         case NbtList nbtList -> (Object)this.convertList(dynamicOps, nbtList);
         case NbtCompound nbtCompound -> (Object)this.convertMap(dynamicOps, nbtCompound);
         case NbtIntArray nbtIntArray -> (Object)dynamicOps.createIntList(Arrays.stream(nbtIntArray.getIntArray()));
         case NbtLongArray nbtLongArray -> (Object)dynamicOps.createLongList(Arrays.stream(nbtLongArray.getLongArray()));
         default -> throw new MatchException(null, null);
      });
   }

   public DataResult<Number> getNumberValue(NbtElement nbtElement) {
      return nbtElement.asNumber().<DataResult<Number>>map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Not a number"));
   }

   public NbtElement createNumeric(Number number) {
      return NbtDouble.of(number.doubleValue());
   }

   public NbtElement createByte(byte b) {
      return NbtByte.of(b);
   }

   public NbtElement createShort(short s) {
      return NbtShort.of(s);
   }

   public NbtElement createInt(int i) {
      return NbtInt.of(i);
   }

   public NbtElement createLong(long l) {
      return NbtLong.of(l);
   }

   public NbtElement createFloat(float f) {
      return NbtFloat.of(f);
   }

   public NbtElement createDouble(double d) {
      return NbtDouble.of(d);
   }

   public NbtElement createBoolean(boolean bl) {
      return NbtByte.of(bl);
   }

   public DataResult<String> getStringValue(NbtElement nbtElement) {
      return nbtElement instanceof NbtString(String var4) ? DataResult.success(var4) : DataResult.error(() -> "Not a string");
   }

   public NbtElement createString(String string) {
      return NbtString.of(string);
   }

   public DataResult<NbtElement> mergeToList(NbtElement nbtElement, NbtElement nbtElement2) {
      return createMerger(nbtElement)
         .map(merger -> DataResult.success(merger.merge(nbtElement2).getResult()))
         .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + nbtElement, nbtElement));
   }

   public DataResult<NbtElement> mergeToList(NbtElement nbtElement, List<NbtElement> list) {
      return createMerger(nbtElement)
         .map(merger -> DataResult.success(merger.merge(list).getResult()))
         .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + nbtElement, nbtElement));
   }

   public DataResult<NbtElement> mergeToMap(NbtElement nbtElement, NbtElement nbtElement2, NbtElement nbtElement3) {
      if (!(nbtElement instanceof NbtCompound) && !(nbtElement instanceof NbtEnd)) {
         return DataResult.error(() -> "mergeToMap called with not a map: " + nbtElement, nbtElement);
      } else if (nbtElement2 instanceof NbtString(String var10)) {
         String nbtCompound = var10;
         NbtCompound nbtCompound2 = nbtElement instanceof NbtCompound nbtCompoundx ? nbtCompoundx.shallowCopy() : new NbtCompound();
         nbtCompound2.put(nbtCompound, nbtElement3);
         return DataResult.success(nbtCompound2);
      } else {
         return DataResult.error(() -> "key is not a string: " + nbtElement2, nbtElement);
      }
   }

   public DataResult<NbtElement> mergeToMap(NbtElement nbtElement, MapLike<NbtElement> mapLike) {
      if (!(nbtElement instanceof NbtCompound) && !(nbtElement instanceof NbtEnd)) {
         return DataResult.error(() -> "mergeToMap called with not a map: " + nbtElement, nbtElement);
      } else {
         Iterator<Pair<NbtElement, NbtElement>> iterator = mapLike.entries().iterator();
         if (!iterator.hasNext()) {
            return nbtElement == this.empty() ? DataResult.success(this.emptyMap()) : DataResult.success(nbtElement);
         } else {
            NbtCompound nbtCompound2 = nbtElement instanceof NbtCompound nbtCompound ? nbtCompound.shallowCopy() : new NbtCompound();
            List<NbtElement> list = new ArrayList<>();
            iterator.forEachRemaining(pair -> {
               NbtElement nbtElementx = (NbtElement)pair.getFirst();
               if (nbtElementx instanceof NbtString(String string)) {
                  nbtCompound2.put(string, (NbtElement)pair.getSecond());
               } else {
                  list.add(nbtElementx);
               }
            });
            return !list.isEmpty() ? DataResult.error(() -> "some keys are not strings: " + list, nbtCompound2) : DataResult.success(nbtCompound2);
         }
      }
   }

   public DataResult<NbtElement> mergeToMap(NbtElement nbtElement, Map<NbtElement, NbtElement> map) {
      if (!(nbtElement instanceof NbtCompound) && !(nbtElement instanceof NbtEnd)) {
         return DataResult.error(() -> "mergeToMap called with not a map: " + nbtElement, nbtElement);
      } else if (map.isEmpty()) {
         return nbtElement == this.empty() ? DataResult.success(this.emptyMap()) : DataResult.success(nbtElement);
      } else {
         NbtCompound nbtCompound2 = nbtElement instanceof NbtCompound nbtCompound ? nbtCompound.shallowCopy() : new NbtCompound();
         List<NbtElement> list = new ArrayList<>();

         for (Entry<NbtElement, NbtElement> entry : map.entrySet()) {
            NbtElement nbtElement2 = entry.getKey();
            if (nbtElement2 instanceof NbtString(String var10)) {
               nbtCompound2.put(var10, entry.getValue());
            } else {
               list.add(nbtElement2);
            }
         }

         return !list.isEmpty() ? DataResult.error(() -> "some keys are not strings: " + list, nbtCompound2) : DataResult.success(nbtCompound2);
      }
   }

   public DataResult<Stream<Pair<NbtElement, NbtElement>>> getMapValues(NbtElement nbtElement) {
      return nbtElement instanceof NbtCompound nbtCompound
         ? DataResult.success(nbtCompound.entrySet().stream().map(entry -> Pair.of(this.createString(entry.getKey()), entry.getValue())))
         : DataResult.error(() -> "Not a map: " + nbtElement);
   }

   public DataResult<Consumer<BiConsumer<NbtElement, NbtElement>>> getMapEntries(NbtElement nbtElement) {
      return nbtElement instanceof NbtCompound nbtCompound ? DataResult.success((Consumer<BiConsumer<NbtElement, NbtElement>>)biConsumer -> {
         for (Entry<String, NbtElement> entry : nbtCompound.entrySet()) {
            biConsumer.accept(this.createString(entry.getKey()), entry.getValue());
         }
      }) : DataResult.error(() -> "Not a map: " + nbtElement);
   }

   public DataResult<MapLike<NbtElement>> getMap(NbtElement nbtElement) {
      return nbtElement instanceof NbtCompound nbtCompound ? DataResult.success(new MapLike<NbtElement>() {
         public @Nullable NbtElement get(NbtElement nbtElement) {
            if (nbtElement instanceof NbtString(String var4)) {
               return nbtCompound.get(var4);
            } else {
               throw new UnsupportedOperationException("Cannot get map entry with non-string key: " + nbtElement);
            }
         }

         public @Nullable NbtElement get(String string) {
            return nbtCompound.get(string);
         }

         public Stream<Pair<NbtElement, NbtElement>> entries() {
            return nbtCompound.entrySet().stream().map(entry -> Pair.of(NbtOps.this.createString(entry.getKey()), entry.getValue()));
         }

         @Override
         public String toString() {
            return "MapLike[" + nbtCompound + "]";
         }
      }) : DataResult.error(() -> "Not a map: " + nbtElement);
   }

   public NbtElement createMap(Stream<Pair<NbtElement, NbtElement>> stream) {
      NbtCompound nbtCompound = new NbtCompound();
      stream.forEach(entry -> {
         NbtElement nbtElement = (NbtElement)entry.getFirst();
         NbtElement nbtElement2 = (NbtElement)entry.getSecond();
         if (nbtElement instanceof NbtString(String string)) {
            nbtCompound.put(string, nbtElement2);
         } else {
            throw new UnsupportedOperationException("Cannot create map with non-string key: " + nbtElement);
         }
      });
      return nbtCompound;
   }

   public DataResult<Stream<NbtElement>> getStream(NbtElement nbtElement) {
      return nbtElement instanceof AbstractNbtList abstractNbtList ? DataResult.success(abstractNbtList.stream()) : DataResult.error(() -> "Not a list");
   }

   public DataResult<Consumer<Consumer<NbtElement>>> getList(NbtElement nbtElement) {
      return nbtElement instanceof AbstractNbtList abstractNbtList
         ? DataResult.success(abstractNbtList::forEach)
         : DataResult.error(() -> "Not a list: " + nbtElement);
   }

   public DataResult<ByteBuffer> getByteBuffer(NbtElement nbtElement) {
      return nbtElement instanceof NbtByteArray nbtByteArray
         ? DataResult.success(ByteBuffer.wrap(nbtByteArray.getByteArray()))
         : DynamicOps.super.getByteBuffer(nbtElement);
   }

   public NbtElement createByteList(ByteBuffer byteBuffer) {
      ByteBuffer byteBuffer2 = byteBuffer.duplicate().clear();
      byte[] bs = new byte[byteBuffer.capacity()];
      byteBuffer2.get(0, bs, 0, bs.length);
      return new NbtByteArray(bs);
   }

   public DataResult<IntStream> getIntStream(NbtElement nbtElement) {
      return nbtElement instanceof NbtIntArray nbtIntArray
         ? DataResult.success(Arrays.stream(nbtIntArray.getIntArray()))
         : DynamicOps.super.getIntStream(nbtElement);
   }

   public NbtElement createIntList(IntStream intStream) {
      return new NbtIntArray(intStream.toArray());
   }

   public DataResult<LongStream> getLongStream(NbtElement nbtElement) {
      return nbtElement instanceof NbtLongArray nbtLongArray
         ? DataResult.success(Arrays.stream(nbtLongArray.getLongArray()))
         : DynamicOps.super.getLongStream(nbtElement);
   }

   public NbtElement createLongList(LongStream longStream) {
      return new NbtLongArray(longStream.toArray());
   }

   public NbtElement createList(Stream<NbtElement> stream) {
      return new NbtList(stream.collect(Util.toArrayList()));
   }

   public NbtElement remove(NbtElement nbtElement, String string) {
      if (nbtElement instanceof NbtCompound nbtCompound) {
         NbtCompound nbtCompound2 = nbtCompound.shallowCopy();
         nbtCompound2.remove(string);
         return nbtCompound2;
      } else {
         return nbtElement;
      }
   }

   @Override
   public String toString() {
      return "NBT";
   }

   public RecordBuilder<NbtElement> mapBuilder() {
      return new NbtOps.MapBuilder();
   }

   private static Optional<NbtOps.Merger> createMerger(NbtElement nbt) {
      if (nbt instanceof NbtEnd) {
         return Optional.of(new NbtOps.CompoundListMerger());
      } else if (nbt instanceof AbstractNbtList abstractNbtList) {
         if (abstractNbtList.isEmpty()) {
            return Optional.of(new NbtOps.CompoundListMerger());
         } else {
            return switch (abstractNbtList) {
               case NbtList nbtList -> Optional.of(new NbtOps.CompoundListMerger(nbtList));
               case NbtByteArray nbtByteArray -> Optional.of(new NbtOps.ByteArrayMerger(nbtByteArray.getByteArray()));
               case NbtIntArray nbtIntArray -> Optional.of(new NbtOps.IntArrayMerger(nbtIntArray.getIntArray()));
               case NbtLongArray nbtLongArray -> Optional.of(new NbtOps.LongArrayMerger(nbtLongArray.getLongArray()));
               default -> throw new MatchException(null, null);
            };
         }
      } else {
         return Optional.empty();
      }
   }

   static class ByteArrayMerger implements NbtOps.Merger {
      private final ByteArrayList list = new ByteArrayList();

      public ByteArrayMerger(byte[] values) {
         this.list.addElements(0, values);
      }

      @Override
      public NbtOps.Merger merge(NbtElement nbt) {
         if (nbt instanceof NbtByte nbtByte) {
            this.list.add(nbtByte.byteValue());
            return this;
         } else {
            return new NbtOps.CompoundListMerger(this.list).merge(nbt);
         }
      }

      @Override
      public NbtElement getResult() {
         return new NbtByteArray(this.list.toByteArray());
      }
   }

   static class CompoundListMerger implements NbtOps.Merger {
      private final NbtList list = new NbtList();

      CompoundListMerger() {
      }

      CompoundListMerger(NbtList nbtList) {
         this.list.addAll(nbtList);
      }

      public CompoundListMerger(IntArrayList list) {
         list.forEach(value -> this.list.add(NbtInt.of(value)));
      }

      public CompoundListMerger(ByteArrayList list) {
         list.forEach(value -> this.list.add(NbtByte.of(value)));
      }

      public CompoundListMerger(LongArrayList list) {
         list.forEach(value -> this.list.add(NbtLong.of(value)));
      }

      @Override
      public NbtOps.Merger merge(NbtElement nbt) {
         this.list.add(nbt);
         return this;
      }

      @Override
      public NbtElement getResult() {
         return this.list;
      }
   }

   static class IntArrayMerger implements NbtOps.Merger {
      private final IntArrayList list = new IntArrayList();

      public IntArrayMerger(int[] values) {
         this.list.addElements(0, values);
      }

      @Override
      public NbtOps.Merger merge(NbtElement nbt) {
         if (nbt instanceof NbtInt nbtInt) {
            this.list.add(nbtInt.intValue());
            return this;
         } else {
            return new NbtOps.CompoundListMerger(this.list).merge(nbt);
         }
      }

      @Override
      public NbtElement getResult() {
         return new NbtIntArray(this.list.toIntArray());
      }
   }

   static class LongArrayMerger implements NbtOps.Merger {
      private final LongArrayList list = new LongArrayList();

      public LongArrayMerger(long[] values) {
         this.list.addElements(0, values);
      }

      @Override
      public NbtOps.Merger merge(NbtElement nbt) {
         if (nbt instanceof NbtLong nbtLong) {
            this.list.add(nbtLong.longValue());
            return this;
         } else {
            return new NbtOps.CompoundListMerger(this.list).merge(nbt);
         }
      }

      @Override
      public NbtElement getResult() {
         return new NbtLongArray(this.list.toLongArray());
      }
   }

   class MapBuilder extends AbstractStringBuilder<NbtElement, NbtCompound> {
      protected MapBuilder() {
         super(NbtOps.this);
      }

      protected NbtCompound initBuilder() {
         return new NbtCompound();
      }

      protected NbtCompound append(String string, NbtElement nbtElement, NbtCompound nbtCompound) {
         nbtCompound.put(string, nbtElement);
         return nbtCompound;
      }

      protected DataResult<NbtElement> build(NbtCompound nbtCompound, NbtElement nbtElement) {
         if (nbtElement == null || nbtElement == NbtEnd.INSTANCE) {
            return DataResult.success(nbtCompound);
         } else if (!(nbtElement instanceof NbtCompound nbtCompound2)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + nbtElement, nbtElement);
         } else {
            NbtCompound nbtCompound3 = nbtCompound2.shallowCopy();

            for (Entry<String, NbtElement> entry : nbtCompound.entrySet()) {
               nbtCompound3.put(entry.getKey(), entry.getValue());
            }

            return DataResult.success(nbtCompound3);
         }
      }
   }

   interface Merger {
      NbtOps.Merger merge(NbtElement nbt);

      default NbtOps.Merger merge(Iterable<NbtElement> nbts) {
         NbtOps.Merger merger = this;

         for (NbtElement nbtElement : nbts) {
            merger = merger.merge(nbtElement);
         }

         return merger;
      }

      default NbtOps.Merger merge(Stream<NbtElement> nbts) {
         return this.merge(nbts::iterator);
      }

      NbtElement getResult();
   }
}

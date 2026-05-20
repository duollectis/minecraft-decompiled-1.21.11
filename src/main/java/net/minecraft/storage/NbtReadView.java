package net.minecraft.storage;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Streams;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.DataResult.Success;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.NbtType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.ErrorReporter;
import org.jspecify.annotations.Nullable;

public class NbtReadView implements ReadView {
   private final ErrorReporter reporter;
   private final ReadContext context;
   private final NbtCompound nbt;

   private NbtReadView(ErrorReporter reporter, ReadContext context, NbtCompound nbt) {
      this.reporter = reporter;
      this.context = context;
      this.nbt = nbt;
   }

   public static ReadView create(ErrorReporter reporter, RegistryWrapper.WrapperLookup registries, NbtCompound nbt) {
      return new NbtReadView(reporter, new ReadContext(registries, NbtOps.INSTANCE), nbt);
   }

   public static ReadView.ListReadView createList(ErrorReporter reporter, RegistryWrapper.WrapperLookup registries, List<NbtCompound> elements) {
      return new NbtReadView.NbtListReadView(reporter, new ReadContext(registries, NbtOps.INSTANCE), elements);
   }

   @Override
   public <T> Optional<T> read(String key, Codec<T> codec) {
      NbtElement nbtElement = this.nbt.get(key);
      if (nbtElement == null) {
         return Optional.empty();
      } else {
         return switch (codec.parse(this.context.getOps(), nbtElement)) {
            case Success<T> success -> Optional.of(success.value());
            case Error<T> error -> {
               this.reporter.report(new NbtReadView.DecodeError(key, nbtElement, error));
               yield error.partialValue();
            }
            default -> throw new MatchException(null, null);
         };
      }
   }

   @Override
   public <T> Optional<T> read(MapCodec<T> mapCodec) {
      DynamicOps<NbtElement> dynamicOps = this.context.getOps();

      return switch (dynamicOps.getMap(this.nbt).flatMap(map -> mapCodec.decode(dynamicOps, map))) {
         case Success<T> success -> Optional.of(success.value());
         case Error<T> error -> {
            this.reporter.report(new NbtReadView.DecodeMapError(error));
            yield error.partialValue();
         }
         default -> throw new MatchException(null, null);
      };
   }

   private <T extends NbtElement> @Nullable T get(String key, NbtType<T> type) {
      NbtElement nbtElement = this.nbt.get(key);
      if (nbtElement == null) {
         return null;
      } else {
         NbtType<?> nbtType = nbtElement.getNbtType();
         if (nbtType != type) {
            this.reporter.report(new NbtReadView.ExpectedTypeError(key, type, nbtType));
            return null;
         } else {
            return (T)nbtElement;
         }
      }
   }

   private @Nullable AbstractNbtNumber get(String key) {
      NbtElement nbtElement = this.nbt.get(key);
      if (nbtElement == null) {
         return null;
      } else if (nbtElement instanceof AbstractNbtNumber abstractNbtNumber) {
         return abstractNbtNumber;
      } else {
         this.reporter.report(new NbtReadView.ExpectedNumberError(key, nbtElement.getNbtType()));
         return null;
      }
   }

   @Override
   public Optional<ReadView> getOptionalReadView(String key) {
      NbtCompound nbtCompound = this.get(key, NbtCompound.TYPE);
      return nbtCompound != null ? Optional.of(this.createChildReadView(key, nbtCompound)) : Optional.empty();
   }

   @Override
   public ReadView getReadView(String key) {
      NbtCompound nbtCompound = this.get(key, NbtCompound.TYPE);
      return nbtCompound != null ? this.createChildReadView(key, nbtCompound) : this.context.getEmptyReadView();
   }

   @Override
   public Optional<ReadView.ListReadView> getOptionalListReadView(String key) {
      NbtList nbtList = this.get(key, NbtList.TYPE);
      return nbtList != null ? Optional.of(this.createChildListReadView(key, this.context, nbtList)) : Optional.empty();
   }

   @Override
   public ReadView.ListReadView getListReadView(String key) {
      NbtList nbtList = this.get(key, NbtList.TYPE);
      return nbtList != null ? this.createChildListReadView(key, this.context, nbtList) : this.context.getEmptyListReadView();
   }

   @Override
   public <T> Optional<ReadView.TypedListReadView<T>> getOptionalTypedListView(String key, Codec<T> typeCodec) {
      NbtList nbtList = this.get(key, NbtList.TYPE);
      return nbtList != null ? Optional.of(this.createTypedListReadView(key, nbtList, typeCodec)) : Optional.empty();
   }

   @Override
   public <T> ReadView.TypedListReadView<T> getTypedListView(String key, Codec<T> typeCodec) {
      NbtList nbtList = this.get(key, NbtList.TYPE);
      return nbtList != null ? this.createTypedListReadView(key, nbtList, typeCodec) : this.context.getEmptyTypedListReadView();
   }

   @Override
   public boolean getBoolean(String key, boolean fallback) {
      AbstractNbtNumber abstractNbtNumber = this.get(key);
      return abstractNbtNumber != null ? abstractNbtNumber.byteValue() != 0 : fallback;
   }

   @Override
   public byte getByte(String key, byte fallback) {
      AbstractNbtNumber abstractNbtNumber = this.get(key);
      return abstractNbtNumber != null ? abstractNbtNumber.byteValue() : fallback;
   }

   @Override
   public int getShort(String key, short fallback) {
      AbstractNbtNumber abstractNbtNumber = this.get(key);
      return abstractNbtNumber != null ? abstractNbtNumber.shortValue() : fallback;
   }

   @Override
   public Optional<Integer> getOptionalInt(String key) {
      AbstractNbtNumber abstractNbtNumber = this.get(key);
      return abstractNbtNumber != null ? Optional.of(abstractNbtNumber.intValue()) : Optional.empty();
   }

   @Override
   public int getInt(String key, int fallback) {
      AbstractNbtNumber abstractNbtNumber = this.get(key);
      return abstractNbtNumber != null ? abstractNbtNumber.intValue() : fallback;
   }

   @Override
   public long getLong(String key, long fallback) {
      AbstractNbtNumber abstractNbtNumber = this.get(key);
      return abstractNbtNumber != null ? abstractNbtNumber.longValue() : fallback;
   }

   @Override
   public Optional<Long> getOptionalLong(String key) {
      AbstractNbtNumber abstractNbtNumber = this.get(key);
      return abstractNbtNumber != null ? Optional.of(abstractNbtNumber.longValue()) : Optional.empty();
   }

   @Override
   public float getFloat(String key, float fallback) {
      AbstractNbtNumber abstractNbtNumber = this.get(key);
      return abstractNbtNumber != null ? abstractNbtNumber.floatValue() : fallback;
   }

   @Override
   public double getDouble(String key, double fallback) {
      AbstractNbtNumber abstractNbtNumber = this.get(key);
      return abstractNbtNumber != null ? abstractNbtNumber.doubleValue() : fallback;
   }

   @Override
   public Optional<String> getOptionalString(String key) {
      NbtString nbtString = this.get(key, NbtString.TYPE);
      return nbtString != null ? Optional.of(nbtString.value()) : Optional.empty();
   }

   @Override
   public String getString(String key, String fallback) {
      NbtString nbtString = this.get(key, NbtString.TYPE);
      return nbtString != null ? nbtString.value() : fallback;
   }

   @Override
   public Optional<int[]> getOptionalIntArray(String key) {
      NbtIntArray nbtIntArray = this.get(key, NbtIntArray.TYPE);
      return nbtIntArray != null ? Optional.of(nbtIntArray.getIntArray()) : Optional.empty();
   }

   @Override
   public RegistryWrapper.WrapperLookup getRegistries() {
      return this.context.getRegistries();
   }

   private ReadView createChildReadView(String key, NbtCompound nbt) {
      return (ReadView)(nbt.isEmpty()
         ? this.context.getEmptyReadView()
         : new NbtReadView(this.reporter.makeChild(new ErrorReporter.MapElementContext(key)), this.context, nbt));
   }

   static ReadView createReadView(ErrorReporter reporter, ReadContext context, NbtCompound nbt) {
      return (ReadView)(nbt.isEmpty() ? context.getEmptyReadView() : new NbtReadView(reporter, context, nbt));
   }

   private ReadView.ListReadView createChildListReadView(String key, ReadContext context, NbtList list) {
      return (ReadView.ListReadView)(list.isEmpty() ? context.getEmptyListReadView() : new NbtReadView.ChildListReadView(this.reporter, key, context, list));
   }

   private <T> ReadView.TypedListReadView<T> createTypedListReadView(String key, NbtList list, Codec<T> typeCodec) {
      return (ReadView.TypedListReadView<T>)(list.isEmpty()
         ? this.context.getEmptyTypedListReadView()
         : new NbtReadView.NbtTypedListReadView<>(this.reporter, key, this.context, typeCodec, list));
   }

   static class ChildListReadView implements ReadView.ListReadView {
      private final ErrorReporter reporter;
      private final String name;
      final ReadContext context;
      private final NbtList list;

      ChildListReadView(ErrorReporter reporter, String name, ReadContext context, NbtList list) {
         this.reporter = reporter;
         this.name = name;
         this.context = context;
         this.list = list;
      }

      @Override
      public boolean isEmpty() {
         return this.list.isEmpty();
      }

      ErrorReporter createErrorReporter(int index) {
         return this.reporter.makeChild(new ErrorReporter.NamedListElementContext(this.name, index));
      }

      void reportExpectedTypeAtIndexError(int index, NbtElement element) {
         this.reporter.report(new NbtReadView.ExpectedTypeAtIndexError(this.name, index, NbtCompound.TYPE, element.getNbtType()));
      }

      @Override
      public Stream<ReadView> stream() {
         return Streams.mapWithIndex(this.list.stream(), (element, index) -> {
            if (element instanceof NbtCompound nbtCompound) {
               return NbtReadView.createReadView(this.createErrorReporter((int)index), this.context, nbtCompound);
            } else {
               this.reportExpectedTypeAtIndexError((int)index, element);
               return null;
            }
         }).filter(Objects::nonNull);
      }

      @Override
      public Iterator<ReadView> iterator() {
         final Iterator<NbtElement> iterator = this.list.iterator();
         return new AbstractIterator<ReadView>() {
            private int index;

            protected @Nullable ReadView computeNext() {
               while (iterator.hasNext()) {
                  NbtElement nbtElement = iterator.next();
                  int i = this.index++;
                  if (nbtElement instanceof NbtCompound nbtCompound) {
                     return NbtReadView.createReadView(ChildListReadView.this.createErrorReporter(i), ChildListReadView.this.context, nbtCompound);
                  }

                  ChildListReadView.this.reportExpectedTypeAtIndexError(i, nbtElement);
               }

               return (ReadView)this.endOfData();
            }
         };
      }
   }

   public record DecodeAtIndexError(String name, int index, NbtElement element, Error<?> error) implements ErrorReporter.Error {
      @Override
      public String getMessage() {
         return "Failed to decode value '" + this.element + "' from field '" + this.name + "' at index " + this.index + "': " + this.error.message();
      }
   }

   public record DecodeError(String name, NbtElement element, Error<?> error) implements ErrorReporter.Error {
      @Override
      public String getMessage() {
         return "Failed to decode value '" + this.element + "' from field '" + this.name + "': " + this.error.message();
      }
   }

   public record DecodeMapError(Error<?> error) implements ErrorReporter.Error {
      @Override
      public String getMessage() {
         return "Failed to decode from map: " + this.error.message();
      }
   }

   public record ExpectedNumberError(String name, NbtType<?> actual) implements ErrorReporter.Error {
      @Override
      public String getMessage() {
         return "Expected field '" + this.name + "' to contain number, but got " + this.actual.getCrashReportName();
      }
   }

   public record ExpectedTypeAtIndexError(String name, int index, NbtType<?> expected, NbtType<?> actual) implements ErrorReporter.Error {
      @Override
      public String getMessage() {
         return "Expected list '"
            + this.name
            + "' to contain at index "
            + this.index
            + " value of type "
            + this.expected.getCrashReportName()
            + ", but got "
            + this.actual.getCrashReportName();
      }
   }

   public record ExpectedTypeError(String name, NbtType<?> expected, NbtType<?> actual) implements ErrorReporter.Error {
      @Override
      public String getMessage() {
         return "Expected field '"
            + this.name
            + "' to contain value of type "
            + this.expected.getCrashReportName()
            + ", but got "
            + this.actual.getCrashReportName();
      }
   }

   static class NbtListReadView implements ReadView.ListReadView {
      private final ErrorReporter reporter;
      private final ReadContext context;
      private final List<NbtCompound> nbts;

      public NbtListReadView(ErrorReporter reporter, ReadContext context, List<NbtCompound> nbts) {
         this.reporter = reporter;
         this.context = context;
         this.nbts = nbts;
      }

      ReadView createReadView(int index, NbtCompound nbt) {
         return NbtReadView.createReadView(this.reporter.makeChild(new ErrorReporter.ListElementContext(index)), this.context, nbt);
      }

      @Override
      public boolean isEmpty() {
         return this.nbts.isEmpty();
      }

      @Override
      public Stream<ReadView> stream() {
         return Streams.mapWithIndex(this.nbts.stream(), (nbt, index) -> this.createReadView((int)index, nbt));
      }

      @Override
      public Iterator<ReadView> iterator() {
         final ListIterator<NbtCompound> listIterator = this.nbts.listIterator();
         return new AbstractIterator<ReadView>() {
            protected @Nullable ReadView computeNext() {
               if (listIterator.hasNext()) {
                  int i = listIterator.nextIndex();
                  NbtCompound nbtCompound = listIterator.next();
                  return NbtListReadView.this.createReadView(i, nbtCompound);
               } else {
                  return (ReadView)this.endOfData();
               }
            }
         };
      }
   }

   static class NbtTypedListReadView<T> implements ReadView.TypedListReadView<T> {
      private final ErrorReporter reporter;
      private final String name;
      final ReadContext context;
      final Codec<T> typeCodec;
      private final NbtList list;

      NbtTypedListReadView(ErrorReporter reporter, String name, ReadContext context, Codec<T> typeCodec, NbtList list) {
         this.reporter = reporter;
         this.name = name;
         this.context = context;
         this.typeCodec = typeCodec;
         this.list = list;
      }

      @Override
      public boolean isEmpty() {
         return this.list.isEmpty();
      }

      void reportDecodeAtIndexError(int index, NbtElement element, Error<?> error) {
         this.reporter.report(new NbtReadView.DecodeAtIndexError(this.name, index, element, error));
      }

      @Override
      @SuppressWarnings("unchecked")
      public Stream<T> stream() {
         return (Stream<T>) Streams.mapWithIndex(this.list.stream(), (element, index) -> {
            return switch (this.typeCodec.parse(this.context.getOps(), element)) {
               case Success<T> success -> (Object)success.value();
               case Error<T> error -> {
                  this.reportDecodeAtIndexError((int)index, element, error);
                  yield error.partialValue().orElse(null);
               }
               default -> throw new MatchException(null, null);
            };
         }).filter(Objects::nonNull);
      }

      @Override
      public Iterator<T> iterator() {
         final ListIterator<NbtElement> listIterator = this.list.listIterator();
         return new AbstractIterator<T>() {
            protected @Nullable T computeNext() {
               while (listIterator.hasNext()) {
                  int i = listIterator.nextIndex();
                  NbtElement nbtElement = listIterator.next();
                  switch (NbtTypedListReadView.this.typeCodec.parse(NbtTypedListReadView.this.context.getOps(), nbtElement)) {
                     case Success<T> success:
                        return (T)success.value();
                     case Error<T> error:
                        NbtTypedListReadView.this.reportDecodeAtIndexError(i, nbtElement, error);
                        if (!error.partialValue().isPresent()) {
                           break;
                        }

                        return (T)error.partialValue().get();
                     default:
                        throw new MatchException(null, null);
                  }
               }

               return (T)this.endOfData();
            }
         };
      }
   }
}

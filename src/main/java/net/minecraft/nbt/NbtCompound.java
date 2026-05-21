package net.minecraft.nbt;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.*;
import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

/**
 * {@code NbtCompound}.
 */
public final class NbtCompound implements NbtElement {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final Codec<NbtCompound> CODEC = Codec.PASSTHROUGH
			.comapFlatMap(
					dynamic -> {
						NbtElement nbtElement = (NbtElement) dynamic.convert(NbtOps.INSTANCE).getValue();
						return nbtElement instanceof NbtCompound nbtCompound
						       ? DataResult.success(
								nbtCompound == dynamic.getValue() ? nbtCompound.copy() : nbtCompound)
						       : DataResult.error(() -> "Not a compound tag: " + nbtElement);
					},
					nbt -> new Dynamic(NbtOps.INSTANCE, nbt.copy())
			);
	private static final int SIZE = 48;
	private static final int MAX_DEPTH = 32;
	public static final NbtType<NbtCompound> TYPE = new NbtType.OfVariableSize<NbtCompound>() {
		public NbtCompound read(DataInput dataInput, NbtSizeTracker nbtSizeTracker) throws IOException {
			nbtSizeTracker.pushStack();

			NbtCompound var3;
			try {
				var3 = readCompound(dataInput, nbtSizeTracker);
			}
			finally {
				nbtSizeTracker.popStack();
			}

			return var3;
		}

		private static NbtCompound readCompound(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.add(48L);
			Map<String, NbtElement> map = Maps.newHashMap();

			byte b;
			while ((b = input.readByte()) != 0) {
				String string = readString(input, tracker);
				NbtElement nbtElement = NbtCompound.read(NbtTypes.byId(b), string, input, tracker);
				if (map.put(string, nbtElement) == null) {
					tracker.add(36L);
				}
			}

			return new NbtCompound(map);
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			tracker.pushStack();

			NbtScanner.Result var4;
			try {
				var4 = scanCompound(input, visitor, tracker);
			}
			finally {
				tracker.popStack();
			}

			return var4;
		}

		private static NbtScanner.Result scanCompound(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			tracker.add(48L);

			byte b;
			label35:
			while ((b = input.readByte()) != 0) {
				NbtType<?> nbtType = NbtTypes.byId(b);
				switch (visitor.visitSubNbtType(nbtType)) {
					case HALT:
						return NbtScanner.Result.HALT;
					case BREAK:
						NbtString.skip(input);
						nbtType.skip(input, tracker);
						break label35;
					case SKIP:
						NbtString.skip(input);
						nbtType.skip(input, tracker);
						break;
					default:
						String string = readString(input, tracker);
						switch (visitor.startSubNbt(nbtType, string)) {
							case HALT:
								return NbtScanner.Result.HALT;
							case BREAK:
								nbtType.skip(input, tracker);
								break label35;
							case SKIP:
								nbtType.skip(input, tracker);
								break;
							default:
								tracker.add(36L);
								switch (nbtType.doAccept(input, visitor, tracker)) {
									case HALT:
										return NbtScanner.Result.HALT;
									case BREAK:
								}
						}
				}
			}

			if (b != 0) {
				while ((b = input.readByte()) != 0) {
					NbtString.skip(input);
					NbtTypes.byId(b).skip(input, tracker);
				}
			}

			return visitor.endNested();
		}

		private static String readString(DataInput input, NbtSizeTracker tracker) throws IOException {
			String string = input.readUTF();
			tracker.add(28L);
			tracker.add(2L, string.length());
			return string;
		}

		@Override
		public void skip(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.pushStack();

			byte b;
			try {
				while ((b = input.readByte()) != 0) {
					NbtString.skip(input);
					NbtTypes.byId(b).skip(input, tracker);
				}
			}
			finally {
				tracker.popStack();
			}
		}

		@Override
		public String getCrashReportName() {
			return "COMPOUND";
		}

		@Override
		public String getCommandFeedbackName() {
			return "TAG_Compound";
		}
	};
	private final Map<String, NbtElement> entries;

	NbtCompound(Map<String, NbtElement> entries) {
		this.entries = entries;
	}

	public NbtCompound() {
		this(new HashMap<>());
	}

	@Override
	public void write(DataOutput output) throws IOException {
		for (String string : this.entries.keySet()) {
			NbtElement nbtElement = this.entries.get(string);
			write(string, nbtElement, output);
		}

		output.writeByte(0);
	}

	@Override
	public int getSizeInBytes() {
		int i = 48;

		for (Entry<String, NbtElement> entry : this.entries.entrySet()) {
			i += 28 + 2 * entry.getKey().length();
			i += 36;
			i += entry.getValue().getSizeInBytes();
		}

		return i;
	}

	public Set<String> getKeys() {
		return this.entries.keySet();
	}

	public Set<Entry<String, NbtElement>> entrySet() {
		return this.entries.entrySet();
	}

	public Collection<NbtElement> values() {
		return this.entries.values();
	}

	public void forEach(BiConsumer<String, NbtElement> entryConsumer) {
		this.entries.forEach(entryConsumer);
	}

	@Override
	public byte getType() {
		return 10;
	}

	@Override
	public NbtType<NbtCompound> getNbtType() {
		return TYPE;
	}

	public int getSize() {
		return this.entries.size();
	}

	public @Nullable NbtElement put(String key, NbtElement element) {
		return this.entries.put(key, element);
	}

	public void putByte(String key, byte value) {
		this.entries.put(key, NbtByte.of(value));
	}

	public void putShort(String key, short value) {
		this.entries.put(key, NbtShort.of(value));
	}

	public void putInt(String key, int value) {
		this.entries.put(key, NbtInt.of(value));
	}

	public void putLong(String key, long value) {
		this.entries.put(key, NbtLong.of(value));
	}

	public void putFloat(String key, float value) {
		this.entries.put(key, NbtFloat.of(value));
	}

	public void putDouble(String key, double value) {
		this.entries.put(key, NbtDouble.of(value));
	}

	public void putString(String key, String value) {
		this.entries.put(key, NbtString.of(value));
	}

	public void putByteArray(String key, byte[] value) {
		this.entries.put(key, new NbtByteArray(value));
	}

	public void putIntArray(String key, int[] value) {
		this.entries.put(key, new NbtIntArray(value));
	}

	public void putLongArray(String key, long[] value) {
		this.entries.put(key, new NbtLongArray(value));
	}

	public void putBoolean(String key, boolean value) {
		this.entries.put(key, NbtByte.of(value));
	}

	public @Nullable NbtElement get(String key) {
		return this.entries.get(key);
	}

	public boolean contains(String key) {
		return this.entries.containsKey(key);
	}

	private Optional<NbtElement> getOptional(String key) {
		return Optional.ofNullable(this.entries.get(key));
	}

	public Optional<Byte> getByte(String key) {
		return this.getOptional(key).flatMap(NbtElement::asByte);
	}

	public byte getByte(String key, byte fallback) {
		return this.entries.get(key) instanceof AbstractNbtNumber abstractNbtNumber ? abstractNbtNumber.byteValue()
		                                                                            : fallback;
	}

	public Optional<Short> getShort(String key) {
		return this.getOptional(key).flatMap(NbtElement::asShort);
	}

	public short getShort(String key, short fallback) {
		return this.entries.get(key) instanceof AbstractNbtNumber abstractNbtNumber ? abstractNbtNumber.shortValue()
		                                                                            : fallback;
	}

	public Optional<Integer> getInt(String key) {
		return this.getOptional(key).flatMap(NbtElement::asInt);
	}

	public int getInt(String key, int fallback) {
		return this.entries.get(key) instanceof AbstractNbtNumber abstractNbtNumber ? abstractNbtNumber.intValue()
		                                                                            : fallback;
	}

	public Optional<Long> getLong(String key) {
		return this.getOptional(key).flatMap(NbtElement::asLong);
	}

	public long getLong(String key, long fallback) {
		return this.entries.get(key) instanceof AbstractNbtNumber abstractNbtNumber ? abstractNbtNumber.longValue()
		                                                                            : fallback;
	}

	public Optional<Float> getFloat(String key) {
		return this.getOptional(key).flatMap(NbtElement::asFloat);
	}

	public float getFloat(String key, float fallback) {
		return this.entries.get(key) instanceof AbstractNbtNumber abstractNbtNumber ? abstractNbtNumber.floatValue()
		                                                                            : fallback;
	}

	public Optional<Double> getDouble(String key) {
		return this.getOptional(key).flatMap(NbtElement::asDouble);
	}

	public double getDouble(String key, double fallback) {
		return this.entries.get(key) instanceof AbstractNbtNumber abstractNbtNumber ? abstractNbtNumber.doubleValue()
		                                                                            : fallback;
	}

	public Optional<String> getString(String key) {
		return this.getOptional(key).flatMap(NbtElement::asString);
	}

	public String getString(String key, String fallback) {
		return this.entries.get(key) instanceof NbtString(String var8) ? var8 : fallback;
	}

	public Optional<byte[]> getByteArray(String key) {
		return this.entries.get(key) instanceof NbtByteArray nbtByteArray ? Optional.of(nbtByteArray.getByteArray())
		                                                                  : Optional.empty();
	}

	public Optional<int[]> getIntArray(String key) {
		return this.entries.get(key) instanceof NbtIntArray nbtIntArray ? Optional.of(nbtIntArray.getIntArray())
		                                                                : Optional.empty();
	}

	public Optional<long[]> getLongArray(String key) {
		return this.entries.get(key) instanceof NbtLongArray nbtLongArray ? Optional.of(nbtLongArray.getLongArray())
		                                                                  : Optional.empty();
	}

	public Optional<NbtCompound> getCompound(String key) {
		return this.entries.get(key) instanceof NbtCompound nbtCompound ? Optional.of(nbtCompound) : Optional.empty();
	}

	public NbtCompound getCompoundOrEmpty(String key) {
		return this.getCompound(key).orElseGet(NbtCompound::new);
	}

	public Optional<NbtList> getList(String key) {
		return this.entries.get(key) instanceof NbtList nbtList ? Optional.of(nbtList) : Optional.empty();
	}

	public NbtList getListOrEmpty(String key) {
		return this.getList(key).orElseGet(NbtList::new);
	}

	public Optional<Boolean> getBoolean(String key) {
		return this.getOptional(key).flatMap(NbtElement::asBoolean);
	}

	public boolean getBoolean(String key, boolean fallback) {
		return this.getByte(key, (byte) (fallback ? 1 : 0)) != 0;
	}

	public @Nullable NbtElement remove(String key) {
		return this.entries.remove(key);
	}

	@Override
	public String toString() {
		StringNbtWriter stringNbtWriter = new StringNbtWriter();
		stringNbtWriter.visitCompound(this);
		return stringNbtWriter.getString();
	}

	public boolean isEmpty() {
		return this.entries.isEmpty();
	}

	protected NbtCompound shallowCopy() {
		return new NbtCompound(new HashMap<>(this.entries));
	}

	public NbtCompound copy() {
		HashMap<String, NbtElement> hashMap = new HashMap<>();
		this.entries.forEach((key, value) -> hashMap.put(key, value.copy()));
		return new NbtCompound(hashMap);
	}

	@Override
	public Optional<NbtCompound> asCompound() {
		return Optional.of(this);
	}

	@Override
	public boolean equals(Object o) {
		return this == o ? true : o instanceof NbtCompound && Objects.equals(this.entries, ((NbtCompound) o).entries);
	}

	@Override
	public int hashCode() {
		return this.entries.hashCode();
	}

	private static void write(String key, NbtElement element, DataOutput output) throws IOException {
		output.writeByte(element.getType());
		if (element.getType() != 0) {
			output.writeUTF(key);
			element.write(output);
		}
	}

	static NbtElement read(NbtType<?> reader, String key, DataInput input, NbtSizeTracker tracker) {
		try {
			return reader.read(input, tracker);
		}
		catch (IOException var7) {
			CrashReport crashReport = CrashReport.create(var7, "Loading NBT data");
			CrashReportSection crashReportSection = crashReport.addElement("NBT Tag");
			crashReportSection.add("Tag name", key);
			crashReportSection.add("Tag type", reader.getCrashReportName());
			throw new NbtCrashException(crashReport);
		}
	}

	public NbtCompound copyFrom(NbtCompound source) {
		for (String string : source.entries.keySet()) {
			NbtElement nbtElement = source.entries.get(string);
			if (nbtElement instanceof NbtCompound nbtCompound
					&& this.entries.get(string) instanceof NbtCompound nbtCompound2) {
				nbtCompound2.copyFrom(nbtCompound);
			}
			else {
				this.put(string, nbtElement.copy());
			}
		}

		return this;
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitCompound(this);
	}

	@Override
	public NbtScanner.Result doAccept(NbtScanner visitor) {
		for (Entry<String, NbtElement> entry : this.entries.entrySet()) {
			NbtElement nbtElement = entry.getValue();
			NbtType<?> nbtType = nbtElement.getNbtType();
			NbtScanner.NestedResult nestedResult = visitor.visitSubNbtType(nbtType);
			switch (nestedResult) {
				case HALT:
					return NbtScanner.Result.HALT;
				case BREAK:
					return visitor.endNested();
				case SKIP:
					break;
				default:
					nestedResult = visitor.startSubNbt(nbtType, entry.getKey());
					switch (nestedResult) {
						case HALT:
							return NbtScanner.Result.HALT;
						case BREAK:
							return visitor.endNested();
						case SKIP:
							break;
						default:
							NbtScanner.Result result = nbtElement.doAccept(visitor);
							switch (result) {
								case HALT:
									return NbtScanner.Result.HALT;
								case BREAK:
									return visitor.endNested();
							}
					}
			}
		}

		return visitor.endNested();
	}

	public <T> void put(String key, Codec<T> codec, T value) {
		this.put(key, codec, NbtOps.INSTANCE, value);
	}

	public <T> void putNullable(String key, Codec<T> codec, @Nullable T value) {
		if (value != null) {
			this.put(key, codec, value);
		}
	}

	public <T> void put(String key, Codec<T> codec, DynamicOps<NbtElement> ops, T value) {
		this.put(key, (NbtElement) codec.encodeStart(ops, value).getOrThrow());
	}

	public <T> void putNullable(String key, Codec<T> codec, DynamicOps<NbtElement> ops, @Nullable T value) {
		if (value != null) {
			this.put(key, codec, ops, value);
		}
	}

	public <T> void copyFromCodec(MapCodec<T> codec, T value) {
		this.copyFromCodec(codec, NbtOps.INSTANCE, value);
	}

	public <T> void copyFromCodec(MapCodec<T> codec, DynamicOps<NbtElement> ops, T value) {
		this.copyFrom((NbtCompound) codec.encoder().encodeStart(ops, value).getOrThrow());
	}

	public <T> Optional<T> get(String key, Codec<T> codec) {
		return this.get(key, codec, NbtOps.INSTANCE);
	}

	public <T> Optional<T> get(String key, Codec<T> codec, DynamicOps<NbtElement> ops) {
		NbtElement nbtElement = this.get(key);
		return nbtElement == null
		       ? Optional.empty()
		       : codec
		         .parse(ops, nbtElement)
		         .resultOrPartial(error -> LOGGER.error(
				         "Failed to read field ({}={}): {}",
				         new Object[]{key, nbtElement, error}
		         ));
	}

	public <T> Optional<T> decode(MapCodec<T> codec) {
		return this.decode(codec, NbtOps.INSTANCE);
	}

	public <T> Optional<T> decode(MapCodec<T> codec, DynamicOps<NbtElement> ops) {
		return codec
				.decode(ops, (MapLike) ops.getMap(this).getOrThrow())
				.resultOrPartial(error -> LOGGER.error("Failed to read value ({}): {}", this, error));
	}
}

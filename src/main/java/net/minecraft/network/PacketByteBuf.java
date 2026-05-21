package net.minecraft.network;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.nbt.*;
import net.minecraft.network.codec.PacketDecoder;
import net.minecraft.network.codec.PacketEncoder;
import net.minecraft.network.encoding.StringEncoding;
import net.minecraft.network.encoding.VarInts;
import net.minecraft.network.encoding.VarLongs;
import net.minecraft.network.encoding.VelocityEncoding;
import net.minecraft.network.encryption.NetworkEncryptionException;
import net.minecraft.network.encryption.NetworkEncryptionUtils;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.security.PublicKey;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/**
 * Расширение {@link ByteBuf} с удобными методами для сериализации игровых типов данных.
 * Используется во всех сетевых пакетах Minecraft для чтения и записи данных.
 * Поддерживает VarInt/VarLong, NBT, UUID, BlockPos, строки, коллекции и другие типы.
 */
public class PacketByteBuf extends ByteBuf {

    private static final int MAX_PUBLIC_KEY_LENGTH = 256;
    private static final Gson GSON = new Gson();

    public static final short DEFAULT_MAX_STRING_LENGTH = 32767;
    public static final int MAX_TEXT_LENGTH = 262144;

    private final ByteBuf parent;

    public PacketByteBuf(ByteBuf parent) {
        this.parent = parent;
    }

	/** @deprecated Используй {@link net.minecraft.network.codec.PacketCodec} вместо этого. */
	@Deprecated
	/**
	 * Decode.
	 *
	 * @param ops ops
	 * @param codec codec
	 *
	 * @return T — результат операции
	 */
	public <T> T decode(DynamicOps<NbtElement> ops, Codec<T> codec) {
		return decode(ops, codec, NbtSizeTracker.ofUnlimitedBytes());
	}

	/** @deprecated Используй {@link net.minecraft.network.codec.PacketCodec} вместо этого. */
	@Deprecated
	/**
	 * Decode.
	 *
	 * @param ops ops
	 * @param codec codec
	 * @param sizeTracker size tracker
	 *
	 * @return T — результат операции
	 */
	public <T> T decode(DynamicOps<NbtElement> ops, Codec<T> codec, NbtSizeTracker sizeTracker) {
		NbtElement nbtElement = readNbt(sizeTracker);
		return codec
				.parse(ops, nbtElement)
				.getOrThrow(error -> new DecoderException("Failed to decode: " + error + " " + nbtElement));
	}

	/** @deprecated Используй {@link net.minecraft.network.codec.PacketCodec} вместо этого. */
	@Deprecated
	/**
	 * Encode.
	 *
	 * @param ops ops
	 * @param codec codec
	 * @param value value
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public <T> PacketByteBuf encode(DynamicOps<NbtElement> ops, Codec<T> codec, T value) {
		NbtElement
				nbtElement =
				(NbtElement) codec
						.encodeStart(ops, value)
						.getOrThrow(error -> new EncoderException("Failed to encode: " + error + " " + value));
		writeNbt(nbtElement);
		return this;
	}

	/** Декодирует объект из JSON-строки в буфере с помощью указанного кодека. */
	/**
	 * Декодирует as json.
	 *
	 * @param codec codec
	 *
	 * @return T — результат операции
	 */
	public <T> T decodeAsJson(Codec<T> codec) {
		JsonElement jsonElement = LenientJsonParser.parse(readString());
		DataResult<T> dataResult = codec.parse(JsonOps.INSTANCE, jsonElement);
		return (T) dataResult.getOrThrow(error -> new DecoderException("Failed to decode JSON: " + error));
	}

	/** Кодирует объект в JSON-строку и записывает её в буфер. */
	/**
	 * Кодирует as json.
	 *
	 * @param codec codec
	 * @param value value
	 *
	 * @return void — результат операции
	 */
	public <T> void encodeAsJson(Codec<T> codec, T value) {
		DataResult<JsonElement> dataResult = codec.encodeStart(JsonOps.INSTANCE, value);
		writeString(GSON.toJson((JsonElement) dataResult.getOrThrow(error -> new EncoderException(
				"Failed to encode: " + error + " " + value))));
	}

	/** Создаёт валидатор, выбрасывающий исключение если значение превышает максимум. */
	public static <T> IntFunction<T> getMaxValidator(IntFunction<T> applier, int max) {
		return value -> {
			if (value > max) {
				throw new DecoderException("Value " + value + " is larger than limit " + max);
			}

			return applier.apply(value);
		};
	}

	/** Читает коллекцию: сначала размер (VarInt), затем элементы. */
	public <T, C extends Collection<T>> C readCollection(
			IntFunction<C> collectionFactory,
			PacketDecoder<? super PacketByteBuf, T> reader
	) {
		int i = readVarInt();
		C collection = (C) collectionFactory.apply(i);

		for (int j = 0; j < i; j++) {
			collection.add(reader.decode(this));
		}

		return collection;
	}

	/** Записывает коллекцию: сначала размер (VarInt), затем элементы. */
	/**
	 * Записывает collection.
	 *
	 * @param collection collection
	 * @param PacketByteBuf packet byte buf
	 * @param writer writer
	 *
	 * @return void — результат операции
	 */
	public <T> void writeCollection(Collection<T> collection, PacketEncoder<? super PacketByteBuf, T> writer) {
		writeVarInt(collection.size());

		for (T object : collection) {
			writer.encode(this, object);
		}
	}

	/** Читает список элементов из буфера. */
	/**
	 * Читает list.
	 *
	 * @param PacketByteBuf packet byte buf
	 * @param reader reader
	 *
	 * @return List — результат операции
	 */
	public <T> List<T> readList(PacketDecoder<? super PacketByteBuf, T> reader) {
		return readCollection(Lists::newArrayListWithCapacity, reader);
	}

	/** Читает список целых чисел (VarInt) из буфера. */
	/**
	 * Читает int list.
	 *
	 * @return IntList — результат операции
	 */
	public IntList readIntList() {
		int i = readVarInt();
		IntList intList = new IntArrayList();

		for (int j = 0; j < i; j++) {
			intList.add(readVarInt());
		}

		return intList;
	}

	/** Записывает список целых чисел (VarInt) в буфер. */
	/**
	 * Записывает int list.
	 *
	 * @param list list
	 */
	public void writeIntList(IntList list) {
		writeVarInt(list.size());
		list.forEach(this::writeVarInt);
	}

	/** Читает карту ключ-значение из буфера. */
	public <K, V, M extends Map<K, V>> M readMap(
			IntFunction<M> mapFactory,
			PacketDecoder<? super PacketByteBuf, K> keyReader,
			PacketDecoder<? super PacketByteBuf, V> valueReader
	) {
		int i = readVarInt();
		M map = (M) mapFactory.apply(i);

		for (int j = 0; j < i; j++) {
			K object = keyReader.decode(this);
			V object2 = valueReader.decode(this);
			map.put(object, object2);
		}

		return map;
	}

	/** Читает карту ключ-значение из буфера. */
	public <K, V> Map<K, V> readMap(
			PacketDecoder<? super PacketByteBuf, K> keyReader,
			PacketDecoder<? super PacketByteBuf, V> valueReader
	) {
		return readMap(Maps::newHashMapWithExpectedSize, keyReader, valueReader);
	}

	/** Записывает карту ключ-значение в буфер. */
	public <K, V> void writeMap(
			Map<K, V> map,
			PacketEncoder<? super PacketByteBuf, K> keyWriter,
			PacketEncoder<? super PacketByteBuf, V> valueWriter
	) {
		writeVarInt(map.size());
		map.forEach((key, value) -> {
			keyWriter.encode(this, (K) key);
			valueWriter.encode(this, (V) value);
		});
	}

	/** Вызывает consumer для каждого элемента коллекции в буфере. */
	/**
	 * For each in collection.
	 *
	 * @param consumer consumer
	 */
	public void forEachInCollection(Consumer<PacketByteBuf> consumer) {
		int i = readVarInt();

		for (int j = 0; j < i; j++) {
			consumer.accept(this);
		}
	}

	/** Записывает набор значений перечисления как битовую маску. */
	/**
	 * Записывает enum set.
	 *
	 * @param enumSet enum set
	 * @param type type
	 *
	 * @return > void — результат операции
	 */
	public <E extends Enum<E>> void writeEnumSet(EnumSet<E> enumSet, Class<E> type) {
		E[] enums = (E[]) type.getEnumConstants();
		BitSet bitSet = new BitSet(enums.length);

		for (int i = 0; i < enums.length; i++) {
			bitSet.set(i, enumSet.contains(enums[i]));
		}

		writeBitSet(bitSet, enums.length);
	}

	/** Читает набор значений перечисления из битовой маски. */
	/**
	 * Читает enum set.
	 *
	 * @param type type
	 *
	 * @return > EnumSet — результат операции
	 */
	public <E extends Enum<E>> EnumSet<E> readEnumSet(Class<E> type) {
		E[] enums = (E[]) type.getEnumConstants();
		BitSet bitSet = readBitSet(enums.length);
		EnumSet<E> enumSet = EnumSet.noneOf(type);

		for (int i = 0; i < enums.length; i++) {
			if (bitSet.get(i)) {
				enumSet.add(enums[i]);
			}
		}

		return enumSet;
	}

	/** Записывает Optional: флаг присутствия и, если есть, значение. */
	/**
	 * Записывает optional.
	 *
	 * @param value value
	 * @param PacketByteBuf packet byte buf
	 * @param writer writer
	 *
	 * @return void — результат операции
	 */
	public <T> void writeOptional(Optional<T> value, PacketEncoder<? super PacketByteBuf, T> writer) {
		if (value.isEmpty()) {
			writeBoolean(false);
			return;
		}

		writeBoolean(true);
		writer.encode(this, value.get());
	}

	/** Читает Optional: флаг присутствия и, если есть, значение. */
	/**
	 * Читает optional.
	 *
	 * @param PacketByteBuf packet byte buf
	 * @param reader reader
	 *
	 * @return Optional — результат операции
	 */
	public <T> Optional<T> readOptional(PacketDecoder<? super PacketByteBuf, T> reader) {
		return readBoolean() ? Optional.of(reader.decode(this)) : Optional.empty();
	}

	/** Записывает Either: флаг стороны и соответствующее значение. */
	public <L, R> void writeEither(
			Either<L, R> either,
			PacketEncoder<? super PacketByteBuf, L> leftEncoder,
			PacketEncoder<? super PacketByteBuf, R> rightEncoder
	) {
		either.ifLeft(object -> {
			writeBoolean(true);
			leftEncoder.encode(this, (L) object);
		}).ifRight(object -> {
			writeBoolean(false);
			rightEncoder.encode(this, (R) object);
		});
	}

	/** Читает Either: флаг стороны и соответствующее значение. */
	public <L, R> Either<L, R> readEither(
			PacketDecoder<? super PacketByteBuf, L> leftDecoder,
			PacketDecoder<? super PacketByteBuf, R> rightDecoder
	) {
		return readBoolean() ? Either.left(leftDecoder.decode(this)) : Either.right(rightDecoder.decode(this));
	}

	/** Читает nullable-значение: флаг присутствия и, если есть, значение. */
	/**
	 * Читает nullable.
	 *
	 * @param PacketByteBuf packet byte buf
	 * @param reader reader
	 *
	 * @return @Nullable T — результат операции
	 */
	public <T> @Nullable T readNullable(PacketDecoder<? super PacketByteBuf, T> reader) {
		return readNullable(this, reader);
	}

	/** Читает nullable-значение: флаг присутствия и, если есть, значение. */
	/**
	 * Читает nullable.
	 *
	 * @param buf buf
	 * @param B b
	 * @param reader reader
	 *
	 * @return @Nullable T — результат операции
	 */
	public static <T, B extends ByteBuf> @Nullable T readNullable(B buf, PacketDecoder<? super B, T> reader) {
		return buf.readBoolean() ? reader.decode(buf) : null;
	}

	/** Записывает nullable-значение: флаг присутствия и, если есть, значение. */
	/**
	 * Записывает nullable.
	 *
	 * @param value value
	 * @param PacketByteBuf packet byte buf
	 * @param writer writer
	 *
	 * @return void — результат операции
	 */
	public <T> void writeNullable(@Nullable T value, PacketEncoder<? super PacketByteBuf, T> writer) {
		writeNullable(this, value, writer);
	}

	/** Записывает nullable-значение: флаг присутствия и, если есть, значение. */
	public static <T, B extends ByteBuf> void writeNullable(
			B buf,
			@Nullable T value,
			PacketEncoder<? super B, T> writer
	) {
		if (value == null) {
			buf.writeBoolean(false);
			return;
		}

		buf.writeBoolean(true);
		writer.encode(buf, value);
	}

	/** Читает массив байт (с префиксом длины VarInt). */
	/**
	 * Читает byte array.
	 *
	 * @return byte[] — результат операции
	 */
	public byte[] readByteArray() {
		return readByteArray(this);
	}

	/** Читает массив байт (с префиксом длины VarInt). */
	/**
	 * Читает byte array.
	 *
	 * @param buf buf
	 *
	 * @return byte[] — результат операции
	 */
	public static byte[] readByteArray(ByteBuf buf) {
		return readByteArray(buf, buf.readableBytes());
	}

	/** Записывает массив байт (с префиксом длины VarInt). */
	/**
	 * Записывает byte array.
	 *
	 * @param array array
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeByteArray(byte[] array) {
		writeByteArray(this, array);
		return this;
	}

	/** Записывает массив байт (с префиксом длины VarInt). */
	/**
	 * Записывает byte array.
	 *
	 * @param buf buf
	 * @param array array
	 */
	public static void writeByteArray(ByteBuf buf, byte[] array) {
		VarInts.write(buf, array.length);
		buf.writeBytes(array);
	}

	/** Читает массив байт (с префиксом длины VarInt). */
	/**
	 * Читает byte array.
	 *
	 * @param maxSize max size
	 *
	 * @return byte[] — результат операции
	 */
	public byte[] readByteArray(int maxSize) {
		return readByteArray(this, maxSize);
	}

	/** Читает массив байт (с префиксом длины VarInt). */
	/**
	 * Читает byte array.
	 *
	 * @param buf buf
	 * @param maxSize max size
	 *
	 * @return byte[] — результат операции
	 */
	public static byte[] readByteArray(ByteBuf buf, int maxSize) {
		int i = VarInts.read(buf);
		if (i > maxSize) {
			throw new DecoderException("ByteArray with size " + i + " is bigger than allowed " + maxSize);
		}

		byte[] bs = new byte[i];
		buf.readBytes(bs);
		return bs;
	}

	/** Записывает массив int как VarInt-значения с префиксом длины. */
	/**
	 * Записывает int array.
	 *
	 * @param array array
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeIntArray(int[] array) {
		writeVarInt(array.length);

		for (int i : array) {
			writeVarInt(i);
		}

		return this;
	}

	/** Читает массив int (VarInt) с проверкой максимального размера. */
	/**
	 * Читает int array.
	 *
	 * @return int[] — результат операции
	 */
	public int[] readIntArray() {
		return readIntArray(readableBytes());
	}

	/** Читает массив int (VarInt) с проверкой максимального размера. */
	/**
	 * Читает int array.
	 *
	 * @param maxSize max size
	 *
	 * @return int[] — результат операции
	 */
	public int[] readIntArray(int maxSize) {
		int i = readVarInt();
		if (i > maxSize) {
			throw new DecoderException("VarIntArray with size " + i + " is bigger than allowed " + maxSize);
		}

		int[] is = new int[i];

		for (int j = 0; j < is.length; j++) {
			is[j] = readVarInt();
		}

		return is;
	}

	/** Записывает массив long с префиксом длины VarInt. */
	/**
	 * Записывает long array.
	 *
	 * @param values values
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeLongArray(long[] values) {
		writeLongArray(this, values);
		return this;
	}

	/** Записывает массив long с префиксом длины VarInt. */
	/**
	 * Записывает long array.
	 *
	 * @param buf buf
	 * @param values values
	 */
	public static void writeLongArray(ByteBuf buf, long[] values) {
		VarInts.write(buf, values.length);
		writeFixedLengthLongArray(buf, values);
	}

	/** Записывает массив long фиксированной длины без префикса. */
	/**
	 * Записывает fixed length long array.
	 *
	 * @param values values
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeFixedLengthLongArray(long[] values) {
		writeFixedLengthLongArray(this, values);
		return this;
	}

	/** Записывает массив long фиксированной длины без префикса. */
	/**
	 * Записывает fixed length long array.
	 *
	 * @param buf buf
	 * @param values values
	 */
	public static void writeFixedLengthLongArray(ByteBuf buf, long[] values) {
		for (long l : values) {
			buf.writeLong(l);
		}
	}

	/** Читает массив long с префиксом длины VarInt. */
	/**
	 * Читает long array.
	 *
	 * @return long[] — результат операции
	 */
	public long[] readLongArray() {
		return readLongArray(this);
	}

	/** Читает массив long фиксированной длины без префикса. */
	/**
	 * Читает fixed length long array.
	 *
	 * @param values values
	 *
	 * @return long[] — результат операции
	 */
	public long[] readFixedLengthLongArray(long[] values) {
		return readFixedLengthLongArray(this, values);
	}

	/** Читает массив long с префиксом длины VarInt. */
	/**
	 * Читает long array.
	 *
	 * @param buf buf
	 *
	 * @return long[] — результат операции
	 */
	public static long[] readLongArray(ByteBuf buf) {
		int i = VarInts.read(buf);
		int j = buf.readableBytes() / 8;
		if (i > j) {
			throw new DecoderException("LongArray with size " + i + " is bigger than allowed " + j);
		}

		return readFixedLengthLongArray(buf, new long[i]);
	}

	/** Читает массив long фиксированной длины без префикса. */
	/**
	 * Читает fixed length long array.
	 *
	 * @param buf buf
	 * @param values values
	 *
	 * @return long[] — результат операции
	 */
	public static long[] readFixedLengthLongArray(ByteBuf buf, long[] values) {
		for (int i = 0; i < values.length; i++) {
			values[i] = buf.readLong();
		}

		return values;
	}

	/** Читает позицию блока (упакованную в long). */
	/**
	 * Читает block pos.
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos readBlockPos() {
		return readBlockPos(this);
	}

	/** Читает позицию блока (упакованную в long). */
	/**
	 * Читает block pos.
	 *
	 * @param buf buf
	 *
	 * @return BlockPos — результат операции
	 */
	public static BlockPos readBlockPos(ByteBuf buf) {
		return BlockPos.fromLong(buf.readLong());
	}

	/** Записывает позицию блока (упакованную в long). */
	/**
	 * Записывает block pos.
	 *
	 * @param pos pos
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeBlockPos(BlockPos pos) {
		writeBlockPos(this, pos);
		return this;
	}

	/** Записывает позицию блока (упакованную в long). */
	/**
	 * Записывает block pos.
	 *
	 * @param buf buf
	 * @param pos pos
	 */
	public static void writeBlockPos(ByteBuf buf, BlockPos pos) {
		buf.writeLong(pos.asLong());
	}

	/** Читает позицию чанка (упакованную в long). */
	/**
	 * Читает chunk pos.
	 *
	 * @return ChunkPos — результат операции
	 */
	public ChunkPos readChunkPos() {
		return new ChunkPos(readLong());
	}

	/** Записывает позицию чанка (упакованную в long). */
	/**
	 * Записывает chunk pos.
	 *
	 * @param pos pos
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeChunkPos(ChunkPos pos) {
		writeLong(pos.toLong());
		return this;
	}

	/** Читает позицию чанка (упакованную в long). */
	/**
	 * Читает chunk pos.
	 *
	 * @param buf buf
	 *
	 * @return ChunkPos — результат операции
	 */
	public static ChunkPos readChunkPos(ByteBuf buf) {
		return new ChunkPos(buf.readLong());
	}

	/** Записывает позицию чанка (упакованную в long). */
	/**
	 * Записывает chunk pos.
	 *
	 * @param buf buf
	 * @param pos pos
	 */
	public static void writeChunkPos(ByteBuf buf, ChunkPos pos) {
		buf.writeLong(pos.toLong());
	}

	/** Читает глобальную позицию (измерение + координаты блока). */
	/**
	 * Читает global pos.
	 *
	 * @return GlobalPos — результат операции
	 */
	public GlobalPos readGlobalPos() {
		RegistryKey<World> registryKey = readRegistryKey(RegistryKeys.WORLD);
		BlockPos blockPos = readBlockPos();
		return GlobalPos.create(registryKey, blockPos);
	}

	/** Записывает глобальную позицию (измерение + координаты блока). */
	/**
	 * Записывает global pos.
	 *
	 * @param pos pos
	 */
	public void writeGlobalPos(GlobalPos pos) {
		writeRegistryKey(pos.dimension());
		writeBlockPos(pos.pos());
	}

	/** Читает трёхмерный вектор (3 float). */
	/**
	 * Читает vector3f.
	 *
	 * @return Vector3f — результат операции
	 */
	public Vector3f readVector3f() {
		return readVector3f(this);
	}

	/** Читает трёхмерный вектор (3 float). */
	/**
	 * Читает vector3f.
	 *
	 * @param buf buf
	 *
	 * @return Vector3f — результат операции
	 */
	public static Vector3f readVector3f(ByteBuf buf) {
		return new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
	}

	/** Записывает трёхмерный вектор (3 float). */
	/**
	 * Записывает vector3f.
	 *
	 * @param vector3f vector3f
	 */
	public void writeVector3f(Vector3f vector3f) {
		writeVector3f(this, vector3f);
	}

	/** Записывает трёхмерный вектор (3 float). */
	/**
	 * Записывает vector3f.
	 *
	 * @param buf buf
	 * @param vec vec
	 */
	public static void writeVector3f(ByteBuf buf, Vector3fc vec) {
		buf.writeFloat(vec.x());
		buf.writeFloat(vec.y());
		buf.writeFloat(vec.z());
	}

	/** Читает кватернион вращения (4 float). */
	/**
	 * Читает quaternionf.
	 *
	 * @return Quaternionf — результат операции
	 */
	public Quaternionf readQuaternionf() {
		return readQuaternionf(this);
	}

	/** Читает кватернион вращения (4 float). */
	/**
	 * Читает quaternionf.
	 *
	 * @param buf buf
	 *
	 * @return Quaternionf — результат операции
	 */
	public static Quaternionf readQuaternionf(ByteBuf buf) {
		return new Quaternionf(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
	}

	/** Записывает кватернион вращения (4 float). */
	/**
	 * Записывает quaternionf.
	 *
	 * @param quaternionf quaternionf
	 */
	public void writeQuaternionf(Quaternionf quaternionf) {
		writeQuaternionf(this, quaternionf);
	}

	/** Записывает кватернион вращения (4 float). */
	/**
	 * Записывает quaternionf.
	 *
	 * @param buf buf
	 * @param quaternion quaternion
	 */
	public static void writeQuaternionf(ByteBuf buf, Quaternionfc quaternion) {
		buf.writeFloat(quaternion.x());
		buf.writeFloat(quaternion.y());
		buf.writeFloat(quaternion.z());
		buf.writeFloat(quaternion.w());
	}

	/** Читает трёхмерный вектор с двойной точностью (3 double). */
	/**
	 * Читает vec3d.
	 *
	 * @param buf buf
	 *
	 * @return Vec3d — результат операции
	 */
	public static Vec3d readVec3d(ByteBuf buf) {
		return new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
	}

	/** Читает трёхмерный вектор с двойной точностью (3 double). */
	/**
	 * Читает vec3d.
	 *
	 * @return Vec3d — результат операции
	 */
	public Vec3d readVec3d() {
		return readVec3d(this);
	}

	/** Записывает трёхмерный вектор с двойной точностью (3 double). */
	/**
	 * Записывает vec3d.
	 *
	 * @param buf buf
	 * @param vec vec
	 */
	public static void writeVec3d(ByteBuf buf, Vec3d vec) {
		buf.writeDouble(vec.getX());
		buf.writeDouble(vec.getY());
		buf.writeDouble(vec.getZ());
	}

	/** Записывает трёхмерный вектор с двойной точностью (3 double). */
	/**
	 * Записывает vec3d.
	 *
	 * @param vec vec
	 */
	public void writeVec3d(Vec3d vec) {
		writeVec3d(this, vec);
	}

	/** Читает скорость сущности (закодированную как short-тройка). */
	/**
	 * Читает velocity.
	 *
	 * @return Vec3d — результат операции
	 */
	public Vec3d readVelocity() {
		return VelocityEncoding.readVelocity(this);
	}

	/** Записывает скорость сущности (закодированную как short-тройка). */
	/**
	 * Записывает velocity.
	 *
	 * @param velocity velocity
	 */
	public void writeVelocity(Vec3d velocity) {
		VelocityEncoding.writeVelocity(this, velocity);
	}

	/** Читает константу перечисления по её порядковому номеру (VarInt). */
	/**
	 * Читает enum constant.
	 *
	 * @param enumClass enum class
	 *
	 * @return > T — результат операции
	 */
	public <T extends Enum<T>> T readEnumConstant(Class<T> enumClass) {
		return enumClass.getEnumConstants()[readVarInt()];
	}

	/** Записывает порядковый номер константы перечисления (VarInt). */
	/**
	 * Записывает enum constant.
	 *
	 * @param instance instance
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeEnumConstant(Enum<?> instance) {
		return writeVarInt(instance.ordinal());
	}

	/** Декодирует значение по целочисленному идентификатору. */
	/**
	 * Decode.
	 *
	 * @param idToValue id to value
	 *
	 * @return T — результат операции
	 */
	public <T> T decode(IntFunction<T> idToValue) {
		int i = readVarInt();
		return idToValue.apply(i);
	}

	/** Кодирует значение в целочисленный идентификатор. */
	/**
	 * Encode.
	 *
	 * @param valueToId value to id
	 * @param value value
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public <T> PacketByteBuf encode(ToIntFunction<T> valueToId, T value) {
		int i = valueToId.applyAsInt(value);
		return writeVarInt(i);
	}

	/** Читает целое число в формате VarInt (переменная длина, 1-5 байт). */
	/**
	 * Читает var int.
	 *
	 * @return int — результат операции
	 */
	public int readVarInt() {
		return VarInts.read(parent);
	}

	/** Читает длинное целое в формате VarLong (переменная длина, 1-10 байт). */
	/**
	 * Читает var long.
	 *
	 * @return long — результат операции
	 */
	public long readVarLong() {
		return VarLongs.read(parent);
	}

	/** Записывает UUID как два long (most/least significant bits). */
	/**
	 * Записывает uuid.
	 *
	 * @param uuid uuid
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeUuid(UUID uuid) {
		writeUuid(this, uuid);
		return this;
	}

	/** Записывает UUID как два long (most/least significant bits). */
	/**
	 * Записывает uuid.
	 *
	 * @param buf buf
	 * @param uuid uuid
	 */
	public static void writeUuid(ByteBuf buf, UUID uuid) {
		buf.writeLong(uuid.getMostSignificantBits());
		buf.writeLong(uuid.getLeastSignificantBits());
	}

	/** Читает UUID из двух long (most/least significant bits). */
	/**
	 * Читает uuid.
	 *
	 * @return UUID — результат операции
	 */
	public UUID readUuid() {
		return readUuid(this);
	}

	/** Читает UUID из двух long (most/least significant bits). */
	/**
	 * Читает uuid.
	 *
	 * @param buf buf
	 *
	 * @return UUID — результат операции
	 */
	public static UUID readUuid(ByteBuf buf) {
		return new UUID(buf.readLong(), buf.readLong());
	}

	/** Записывает целое число в формате VarInt (переменная длина, 1-5 байт). */
	/**
	 * Записывает var int.
	 *
	 * @param value value
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeVarInt(int value) {
		VarInts.write(parent, value);
		return this;
	}

	/** Записывает длинное целое в формате VarLong (переменная длина, 1-10 байт). */
	/**
	 * Записывает var long.
	 *
	 * @param value value
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeVarLong(long value) {
		VarLongs.write(parent, value);
		return this;
	}

	/** Записывает NBT-элемент в буфер (null записывается как NbtEnd). */
	/**
	 * Записывает nbt.
	 *
	 * @param nbt nbt
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeNbt(@Nullable NbtElement nbt) {
		writeNbt(this, nbt);
		return this;
	}

	/** Записывает NBT-элемент в буфер (null записывается как NbtEnd). */
	/**
	 * Записывает nbt.
	 *
	 * @param buf buf
	 * @param nbt nbt
	 */
	public static void writeNbt(ByteBuf buf, @Nullable NbtElement nbt) {
		if (nbt == null) {
			nbt = NbtEnd.INSTANCE;
		}

		try {
			NbtIo.writeForPacket(nbt, new ByteBufOutputStream(buf));
		}
		catch (IOException var3) {
			throw new EncoderException(var3);
		}
	}

	/** Читает NBT-тег из буфера. */
	/**
	 * Читает nbt.
	 *
	 * @return @Nullable NbtCompound — результат операции
	 */
	public @Nullable NbtCompound readNbt() {
		return readNbt(this);
	}

	/** Читает NBT-тег из буфера. */
	/**
	 * Читает nbt.
	 *
	 * @param buf buf
	 *
	 * @return @Nullable NbtCompound — результат операции
	 */
	public static @Nullable NbtCompound readNbt(ByteBuf buf) {
		NbtElement nbtElement = readNbt(buf, NbtSizeTracker.forPacket());
		if (nbtElement != null && !(nbtElement instanceof NbtCompound)) {
			throw new DecoderException("Not a compound tag: " + nbtElement);
		}

		return (NbtCompound) nbtElement;
	}

	/** Читает NBT-тег из буфера. */
	/**
	 * Читает nbt.
	 *
	 * @param buf buf
	 * @param sizeTracker size tracker
	 *
	 * @return @Nullable NbtElement — результат операции
	 */
	public static @Nullable NbtElement readNbt(ByteBuf buf, NbtSizeTracker sizeTracker) {
		try {
			NbtElement nbtElement = NbtIo.read(new ByteBufInputStream(buf), sizeTracker);
			return nbtElement.getType() == 0 ? null : nbtElement;
		}
		catch (IOException var3) {
			throw new EncoderException(var3);
		}
	}

	/** Читает NBT-тег из буфера. */
	/**
	 * Читает nbt.
	 *
	 * @param sizeTracker size tracker
	 *
	 * @return @Nullable NbtElement — результат операции
	 */
	public @Nullable NbtElement readNbt(NbtSizeTracker sizeTracker) {
		return readNbt(this, sizeTracker);
	}

	/** Читает строку UTF-8 с проверкой максимальной длины. */
	/**
	 * Читает string.
	 *
	 * @return String — результат операции
	 */
	public String readString() {
		return readString(DEFAULT_MAX_STRING_LENGTH);
	}

	/** Читает строку UTF-8 с проверкой максимальной длины. */
	/**
	 * Читает string.
	 *
	 * @param maxLength max length
	 *
	 * @return String — результат операции
	 */
	public String readString(int maxLength) {
		return StringEncoding.decode(parent, maxLength);
	}

	/** Записывает строку UTF-8 с проверкой максимальной длины. */
	/**
	 * Записывает string.
	 *
	 * @param string string
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeString(String string) {
		return writeString(string, DEFAULT_MAX_STRING_LENGTH);
	}

	/** Записывает строку UTF-8 с проверкой максимальной длины. */
	/**
	 * Записывает string.
	 *
	 * @param string string
	 * @param maxLength max length
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeString(String string, int maxLength) {
		StringEncoding.encode(parent, string, maxLength);
		return this;
	}

	/** Читает идентификатор ресурса (namespace:path). */
	/**
	 * Читает identifier.
	 *
	 * @return Identifier — результат операции
	 */
	public Identifier readIdentifier() {
		return Identifier.of(readString(DEFAULT_MAX_STRING_LENGTH));
	}

	/** Записывает идентификатор ресурса (namespace:path). */
	/**
	 * Записывает identifier.
	 *
	 * @param id id
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeIdentifier(Identifier id) {
		writeString(id.toString());
		return this;
	}

	/** Читает ключ реестра для указанного реестра. */
	/**
	 * Читает registry key.
	 *
	 * @param registryRef registry ref
	 *
	 * @return RegistryKey — результат операции
	 */
	public <T> RegistryKey<T> readRegistryKey(RegistryKey<? extends Registry<T>> registryRef) {
		Identifier identifier = readIdentifier();
		return RegistryKey.of(registryRef, identifier);
	}

	/** Записывает ключ реестра. */
	/**
	 * Записывает registry key.
	 *
	 * @param key key
	 */
	public void writeRegistryKey(RegistryKey<?> key) {
		writeIdentifier(key.getValue());
	}

	/** Читает ключ-ссылку на реестр. */
	/**
	 * Читает registry ref key.
	 *
	 * @return RegistryKey> — результат операции
	 */
	public <T> RegistryKey<? extends Registry<T>> readRegistryRefKey() {
		Identifier identifier = readIdentifier();
		return RegistryKey.ofRegistry(identifier);
	}

	/** Читает момент времени (epoch millis как long). */
	/**
	 * Читает instant.
	 *
	 * @return Instant — результат операции
	 */
	public Instant readInstant() {
		return Instant.ofEpochMilli(readLong());
	}

	/** Записывает момент времени (epoch millis как long). */
	/**
	 * Записывает instant.
	 *
	 * @param instant instant
	 */
	public void writeInstant(Instant instant) {
		writeLong(instant.toEpochMilli());
	}

	/** Читает RSA публичный ключ из байтового массива. */
	/**
	 * Читает public key.
	 *
	 * @return PublicKey — результат операции
	 */
	public PublicKey readPublicKey() {
		try {
			return NetworkEncryptionUtils.decodeEncodedRsaPublicKey(readByteArray(MAX_PUBLIC_KEY_LENGTH));
		}
		catch (NetworkEncryptionException var2) {
			throw new DecoderException("Malformed public key bytes", var2);
		}
	}

	/** Записывает RSA публичный ключ как байтовый массив. */
	/**
	 * Записывает public key.
	 *
	 * @param publicKey public key
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writePublicKey(PublicKey publicKey) {
		writeByteArray(publicKey.getEncoded());
		return this;
	}

	/** Читает результат попадания по блоку. */
	/**
	 * Читает block hit result.
	 *
	 * @return BlockHitResult — результат операции
	 */
	public BlockHitResult readBlockHitResult() {
		BlockPos blockPos = readBlockPos();
		Direction direction = readEnumConstant(Direction.class);
		float f = readFloat();
		float g = readFloat();
		float h = readFloat();
		boolean bl = readBoolean();
		boolean bl2 = readBoolean();
		return new BlockHitResult(
				new Vec3d(
						(double) blockPos.getX() + f,
						(double) blockPos.getY() + g,
						(double) blockPos.getZ() + h
				), direction, blockPos, bl, bl2
		);
	}

	/** Записывает результат попадания по блоку. */
	/**
	 * Записывает block hit result.
	 *
	 * @param hitResult hit result
	 */
	public void writeBlockHitResult(BlockHitResult hitResult) {
		BlockPos blockPos = hitResult.getBlockPos();
		writeBlockPos(blockPos);
		writeEnumConstant(hitResult.getSide());
		Vec3d vec3d = hitResult.getPos();
		writeFloat((float) (vec3d.x - blockPos.getX()));
		writeFloat((float) (vec3d.y - blockPos.getY()));
		writeFloat((float) (vec3d.z - blockPos.getZ()));
		writeBoolean(hitResult.isInsideBlock());
		writeBoolean(hitResult.isAgainstWorldBorder());
	}

	/** Читает BitSet из буфера. */
	/**
	 * Читает bit set.
	 *
	 * @return BitSet — результат операции
	 */
	public BitSet readBitSet() {
		return BitSet.valueOf(readLongArray());
	}

	/** Записывает BitSet в буфер. */
	/**
	 * Записывает bit set.
	 *
	 * @param bitSet bit set
	 */
	public void writeBitSet(BitSet bitSet) {
		writeLongArray(bitSet.toLongArray());
	}

	/** Читает BitSet из буфера. */
	/**
	 * Читает bit set.
	 *
	 * @param size size
	 *
	 * @return BitSet — результат операции
	 */
	public BitSet readBitSet(int size) {
		byte[] bs = new byte[MathHelper.ceilDiv(size, 8)];
		readBytes(bs);
		return BitSet.valueOf(bs);
	}

	/** Записывает BitSet в буфер. */
	/**
	 * Записывает bit set.
	 *
	 * @param bitSet bit set
	 * @param size size
	 */
	public void writeBitSet(BitSet bitSet, int size) {
		if (bitSet.length() > size) {
			throw new EncoderException("BitSet is larger than expected size (" + bitSet.length() + ">" + size + ")");
		}

		byte[] bs = bitSet.toByteArray();
		writeBytes(Arrays.copyOf(bs, MathHelper.ceilDiv(size, 8)));
	}

	/** Читает идентификатор синхронизации контейнера (VarInt). */
	/**
	 * Читает sync id.
	 *
	 * @param buf buf
	 *
	 * @return int — результат операции
	 */
	public static int readSyncId(ByteBuf buf) {
		return VarInts.read(buf);
	}

	/** Читает идентификатор синхронизации контейнера (VarInt). */
	/**
	 * Читает sync id.
	 *
	 * @return int — результат операции
	 */
	public int readSyncId() {
		return readSyncId(parent);
	}

	/** Записывает идентификатор синхронизации контейнера (VarInt). */
	/**
	 * Записывает sync id.
	 *
	 * @param buf buf
	 * @param syncId sync id
	 */
	public static void writeSyncId(ByteBuf buf, int syncId) {
		VarInts.write(buf, syncId);
	}

	/** Записывает идентификатор синхронизации контейнера (VarInt). */
	/**
	 * Записывает sync id.
	 *
	 * @param syncId sync id
	 */
	public void writeSyncId(int syncId) {
		writeSyncId(parent, syncId);
	}

	public boolean isContiguous() {
		return parent.isContiguous();
	}

	/**
	 * Max fast writable bytes.
	 *
	 * @return int — результат операции
	 */
	public int maxFastWritableBytes() {
		return parent.maxFastWritableBytes();
	}

	/**
	 * Capacity.
	 *
	 * @return int — результат операции
	 */
	public int capacity() {
		return parent.capacity();
	}

	/**
	 * Capacity.
	 *
	 * @param i i
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf capacity(int i) {
		parent.capacity(i);
		return this;
	}

	/**
	 * Max capacity.
	 *
	 * @return int — результат операции
	 */
	public int maxCapacity() {
		return parent.maxCapacity();
	}

	/**
	 * Alloc.
	 *
	 * @return ByteBufAllocator — результат операции
	 */
	public ByteBufAllocator alloc() {
		return parent.alloc();
	}

	/**
	 * Order.
	 *
	 * @return ByteOrder — результат операции
	 */
	public ByteOrder order() {
		return parent.order();
	}

	/**
	 * Order.
	 *
	 * @param byteOrder byte order
	 *
	 * @return ByteBuf — результат операции
	 */
	public ByteBuf order(ByteOrder byteOrder) {
		return parent.order(byteOrder);
	}

	/**
	 * Unwrap.
	 *
	 * @return ByteBuf — результат операции
	 */
	public ByteBuf unwrap() {
		return parent;
	}

	public boolean isDirect() {
		return parent.isDirect();
	}

	public boolean isReadOnly() {
		return parent.isReadOnly();
	}

	/**
	 * As read only.
	 *
	 * @return ByteBuf — результат операции
	 */
	public ByteBuf asReadOnly() {
		return parent.asReadOnly();
	}

	/**
	 * Читает er index.
	 *
	 * @return int — результат операции
	 */
	public int readerIndex() {
		return parent.readerIndex();
	}

	/**
	 * Читает er index.
	 *
	 * @param i i
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf readerIndex(int i) {
		parent.readerIndex(i);
		return this;
	}

	/**
	 * Записывает r index.
	 *
	 * @return int — результат операции
	 */
	public int writerIndex() {
		return parent.writerIndex();
	}

	/**
	 * Записывает r index.
	 *
	 * @param i i
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writerIndex(int i) {
		parent.writerIndex(i);
		return this;
	}

	public PacketByteBuf setIndex(int i, int j) {
		parent.setIndex(i, j);
		return this;
	}

	/**
	 * Читает able bytes.
	 *
	 * @return int — результат операции
	 */
	public int readableBytes() {
		return parent.readableBytes();
	}

	/**
	 * Writable bytes.
	 *
	 * @return int — результат операции
	 */
	public int writableBytes() {
		return parent.writableBytes();
	}

	/**
	 * Max writable bytes.
	 *
	 * @return int — результат операции
	 */
	public int maxWritableBytes() {
		return parent.maxWritableBytes();
	}

	public boolean isReadable() {
		return parent.isReadable();
	}

	public boolean isReadable(int size) {
		return parent.isReadable(size);
	}

	public boolean isWritable() {
		return parent.isWritable();
	}

	public boolean isWritable(int size) {
		return parent.isWritable(size);
	}

	/**
	 * Clear.
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf clear() {
		parent.clear();
		return this;
	}

	/**
	 * Mark reader index.
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf markReaderIndex() {
		parent.markReaderIndex();
		return this;
	}

	/**
	 * Сбрасывает reader index.
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf resetReaderIndex() {
		parent.resetReaderIndex();
		return this;
	}

	/**
	 * Mark writer index.
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf markWriterIndex() {
		parent.markWriterIndex();
		return this;
	}

	/**
	 * Сбрасывает writer index.
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf resetWriterIndex() {
		parent.resetWriterIndex();
		return this;
	}

	/**
	 * Discard read bytes.
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf discardReadBytes() {
		parent.discardReadBytes();
		return this;
	}

	/**
	 * Discard some read bytes.
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf discardSomeReadBytes() {
		parent.discardSomeReadBytes();
		return this;
	}

	/**
	 * Ensure writable.
	 *
	 * @param i i
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf ensureWritable(int i) {
		parent.ensureWritable(i);
		return this;
	}

	/**
	 * Ensure writable.
	 *
	 * @param minBytes min bytes
	 * @param force force
	 *
	 * @return int — результат операции
	 */
	public int ensureWritable(int minBytes, boolean force) {
		return parent.ensureWritable(minBytes, force);
	}

	public boolean getBoolean(int index) {
		return parent.getBoolean(index);
	}

	public byte getByte(int index) {
		return parent.getByte(index);
	}

	public short getUnsignedByte(int index) {
		return parent.getUnsignedByte(index);
	}

	public short getShort(int index) {
		return parent.getShort(index);
	}

	public short getShortLE(int index) {
		return parent.getShortLE(index);
	}

	public int getUnsignedShort(int index) {
		return parent.getUnsignedShort(index);
	}

	public int getUnsignedShortLE(int index) {
		return parent.getUnsignedShortLE(index);
	}

	public int getMedium(int index) {
		return parent.getMedium(index);
	}

	public int getMediumLE(int index) {
		return parent.getMediumLE(index);
	}

	public int getUnsignedMedium(int index) {
		return parent.getUnsignedMedium(index);
	}

	public int getUnsignedMediumLE(int index) {
		return parent.getUnsignedMediumLE(index);
	}

	public int getInt(int index) {
		return parent.getInt(index);
	}

	public int getIntLE(int index) {
		return parent.getIntLE(index);
	}

	public long getUnsignedInt(int index) {
		return parent.getUnsignedInt(index);
	}

	public long getUnsignedIntLE(int index) {
		return parent.getUnsignedIntLE(index);
	}

	public long getLong(int index) {
		return parent.getLong(index);
	}

	public long getLongLE(int index) {
		return parent.getLongLE(index);
	}

	public char getChar(int index) {
		return parent.getChar(index);
	}

	public float getFloat(int index) {
		return parent.getFloat(index);
	}

	public double getDouble(int index) {
		return parent.getDouble(index);
	}

	public PacketByteBuf getBytes(int i, ByteBuf byteBuf) {
		parent.getBytes(i, byteBuf);
		return this;
	}

	public PacketByteBuf getBytes(int i, ByteBuf byteBuf, int j) {
		parent.getBytes(i, byteBuf, j);
		return this;
	}

	public PacketByteBuf getBytes(int i, ByteBuf byteBuf, int j, int k) {
		parent.getBytes(i, byteBuf, j, k);
		return this;
	}

	public PacketByteBuf getBytes(int i, byte[] bs) {
		parent.getBytes(i, bs);
		return this;
	}

	public PacketByteBuf getBytes(int i, byte[] bs, int j, int k) {
		parent.getBytes(i, bs, j, k);
		return this;
	}

	public PacketByteBuf getBytes(int i, ByteBuffer byteBuffer) {
		parent.getBytes(i, byteBuffer);
		return this;
	}

	public PacketByteBuf getBytes(int i, OutputStream outputStream, int j) throws IOException {
		parent.getBytes(i, outputStream, j);
		return this;
	}

	public int getBytes(int index, GatheringByteChannel channel, int length) throws IOException {
		return parent.getBytes(index, channel, length);
	}

	public int getBytes(int index, FileChannel channel, long pos, int length) throws IOException {
		return parent.getBytes(index, channel, pos, length);
	}

	public CharSequence getCharSequence(int index, int length, Charset charset) {
		return parent.getCharSequence(index, length, charset);
	}

	public PacketByteBuf setBoolean(int i, boolean bl) {
		parent.setBoolean(i, bl);
		return this;
	}

	public PacketByteBuf setByte(int i, int j) {
		parent.setByte(i, j);
		return this;
	}

	public PacketByteBuf setShort(int i, int j) {
		parent.setShort(i, j);
		return this;
	}

	public PacketByteBuf setShortLE(int i, int j) {
		parent.setShortLE(i, j);
		return this;
	}

	public PacketByteBuf setMedium(int i, int j) {
		parent.setMedium(i, j);
		return this;
	}

	public PacketByteBuf setMediumLE(int i, int j) {
		parent.setMediumLE(i, j);
		return this;
	}

	public PacketByteBuf setInt(int i, int j) {
		parent.setInt(i, j);
		return this;
	}

	public PacketByteBuf setIntLE(int i, int j) {
		parent.setIntLE(i, j);
		return this;
	}

	public PacketByteBuf setLong(int i, long l) {
		parent.setLong(i, l);
		return this;
	}

	public PacketByteBuf setLongLE(int i, long l) {
		parent.setLongLE(i, l);
		return this;
	}

	public PacketByteBuf setChar(int i, int j) {
		parent.setChar(i, j);
		return this;
	}

	public PacketByteBuf setFloat(int i, float f) {
		parent.setFloat(i, f);
		return this;
	}

	public PacketByteBuf setDouble(int i, double d) {
		parent.setDouble(i, d);
		return this;
	}

	public PacketByteBuf setBytes(int i, ByteBuf byteBuf) {
		parent.setBytes(i, byteBuf);
		return this;
	}

	public PacketByteBuf setBytes(int i, ByteBuf byteBuf, int j) {
		parent.setBytes(i, byteBuf, j);
		return this;
	}

	public PacketByteBuf setBytes(int i, ByteBuf byteBuf, int j, int k) {
		parent.setBytes(i, byteBuf, j, k);
		return this;
	}

	public PacketByteBuf setBytes(int i, byte[] bs) {
		parent.setBytes(i, bs);
		return this;
	}

	public PacketByteBuf setBytes(int i, byte[] bs, int j, int k) {
		parent.setBytes(i, bs, j, k);
		return this;
	}

	public PacketByteBuf setBytes(int i, ByteBuffer byteBuffer) {
		parent.setBytes(i, byteBuffer);
		return this;
	}

	public int setBytes(int index, InputStream stream, int length) throws IOException {
		return parent.setBytes(index, stream, length);
	}

	public int setBytes(int index, ScatteringByteChannel channel, int length) throws IOException {
		return parent.setBytes(index, channel, length);
	}

	public int setBytes(int index, FileChannel channel, long pos, int length) throws IOException {
		return parent.setBytes(index, channel, pos, length);
	}

	public PacketByteBuf setZero(int i, int j) {
		parent.setZero(i, j);
		return this;
	}

	public int setCharSequence(int index, CharSequence sequence, Charset charset) {
		return parent.setCharSequence(index, sequence, charset);
	}

	/**
	 * Читает boolean.
	 *
	 * @return boolean — результат операции
	 */
	public boolean readBoolean() {
		return parent.readBoolean();
	}

	/**
	 * Читает byte.
	 *
	 * @return byte — результат операции
	 */
	public byte readByte() {
		return parent.readByte();
	}

	/**
	 * Читает unsigned byte.
	 *
	 * @return short — результат операции
	 */
	public short readUnsignedByte() {
		return parent.readUnsignedByte();
	}

	/**
	 * Читает short.
	 *
	 * @return short — результат операции
	 */
	public short readShort() {
		return parent.readShort();
	}

	/**
	 * Читает short l e.
	 *
	 * @return short — результат операции
	 */
	public short readShortLE() {
		return parent.readShortLE();
	}

	/**
	 * Читает unsigned short.
	 *
	 * @return int — результат операции
	 */
	public int readUnsignedShort() {
		return parent.readUnsignedShort();
	}

	/**
	 * Читает unsigned short l e.
	 *
	 * @return int — результат операции
	 */
	public int readUnsignedShortLE() {
		return parent.readUnsignedShortLE();
	}

	/**
	 * Читает medium.
	 *
	 * @return int — результат операции
	 */
	public int readMedium() {
		return parent.readMedium();
	}

	/**
	 * Читает medium l e.
	 *
	 * @return int — результат операции
	 */
	public int readMediumLE() {
		return parent.readMediumLE();
	}

	/**
	 * Читает unsigned medium.
	 *
	 * @return int — результат операции
	 */
	public int readUnsignedMedium() {
		return parent.readUnsignedMedium();
	}

	/**
	 * Читает unsigned medium l e.
	 *
	 * @return int — результат операции
	 */
	public int readUnsignedMediumLE() {
		return parent.readUnsignedMediumLE();
	}

	/**
	 * Читает int.
	 *
	 * @return int — результат операции
	 */
	public int readInt() {
		return parent.readInt();
	}

	/**
	 * Читает int l e.
	 *
	 * @return int — результат операции
	 */
	public int readIntLE() {
		return parent.readIntLE();
	}

	/**
	 * Читает unsigned int.
	 *
	 * @return long — результат операции
	 */
	public long readUnsignedInt() {
		return parent.readUnsignedInt();
	}

	/**
	 * Читает unsigned int l e.
	 *
	 * @return long — результат операции
	 */
	public long readUnsignedIntLE() {
		return parent.readUnsignedIntLE();
	}

	/**
	 * Читает long.
	 *
	 * @return long — результат операции
	 */
	public long readLong() {
		return parent.readLong();
	}

	/**
	 * Читает long l e.
	 *
	 * @return long — результат операции
	 */
	public long readLongLE() {
		return parent.readLongLE();
	}

	/**
	 * Читает char.
	 *
	 * @return char — результат операции
	 */
	public char readChar() {
		return parent.readChar();
	}

	/**
	 * Читает float.
	 *
	 * @return float — результат операции
	 */
	public float readFloat() {
		return parent.readFloat();
	}

	/**
	 * Читает double.
	 *
	 * @return double — результат операции
	 */
	public double readDouble() {
		return parent.readDouble();
	}

	/**
	 * Читает bytes.
	 *
	 * @param length length
	 *
	 * @return ByteBuf — результат операции
	 */
	public ByteBuf readBytes(int length) {
		return parent.readBytes(length);
	}

	/**
	 * Читает slice.
	 *
	 * @param length length
	 *
	 * @return ByteBuf — результат операции
	 */
	public ByteBuf readSlice(int length) {
		return parent.readSlice(length);
	}

	/**
	 * Читает retained slice.
	 *
	 * @param length length
	 *
	 * @return ByteBuf — результат операции
	 */
	public ByteBuf readRetainedSlice(int length) {
		return parent.readRetainedSlice(length);
	}

	/**
	 * Читает bytes.
	 *
	 * @param byteBuf byte buf
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf readBytes(ByteBuf byteBuf) {
		parent.readBytes(byteBuf);
		return this;
	}

	/**
	 * Читает bytes.
	 *
	 * @param byteBuf byte buf
	 * @param i i
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf readBytes(ByteBuf byteBuf, int i) {
		parent.readBytes(byteBuf, i);
		return this;
	}

	/**
	 * Читает bytes.
	 *
	 * @param byteBuf byte buf
	 * @param i i
	 * @param j j
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf readBytes(ByteBuf byteBuf, int i, int j) {
		parent.readBytes(byteBuf, i, j);
		return this;
	}

	/**
	 * Читает bytes.
	 *
	 * @param bs bs
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf readBytes(byte[] bs) {
		parent.readBytes(bs);
		return this;
	}

	/**
	 * Читает bytes.
	 *
	 * @param bs bs
	 * @param i i
	 * @param j j
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf readBytes(byte[] bs, int i, int j) {
		parent.readBytes(bs, i, j);
		return this;
	}

	/**
	 * Читает bytes.
	 *
	 * @param byteBuffer byte buffer
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf readBytes(ByteBuffer byteBuffer) {
		parent.readBytes(byteBuffer);
		return this;
	}

	/**
	 * Читает bytes.
	 *
	 * @param outputStream output stream
	 * @param i i
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf readBytes(OutputStream outputStream, int i) throws IOException {
		parent.readBytes(outputStream, i);
		return this;
	}

	/**
	 * Читает bytes.
	 *
	 * @param channel channel
	 * @param length length
	 *
	 * @return int — результат операции
	 */
	public int readBytes(GatheringByteChannel channel, int length) throws IOException {
		return parent.readBytes(channel, length);
	}

	/**
	 * Читает char sequence.
	 *
	 * @param length length
	 * @param charset charset
	 *
	 * @return CharSequence — результат операции
	 */
	public CharSequence readCharSequence(int length, Charset charset) {
		return parent.readCharSequence(length, charset);
	}

	/** Читает строку UTF-8 с проверкой максимальной длины. */
	/**
	 * Читает string.
	 *
	 * @param i i
	 * @param charset charset
	 *
	 * @return String — результат операции
	 */
	public String readString(int i, Charset charset) {
		return parent.readString(i, charset);
	}

	/**
	 * Читает bytes.
	 *
	 * @param channel channel
	 * @param pos pos
	 * @param length length
	 *
	 * @return int — результат операции
	 */
	public int readBytes(FileChannel channel, long pos, int length) throws IOException {
		return parent.readBytes(channel, pos, length);
	}

	/**
	 * Skip bytes.
	 *
	 * @param i i
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf skipBytes(int i) {
		parent.skipBytes(i);
		return this;
	}

	/**
	 * Записывает boolean.
	 *
	 * @param bl bl
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeBoolean(boolean bl) {
		parent.writeBoolean(bl);
		return this;
	}

	/**
	 * Записывает byte.
	 *
	 * @param i i
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeByte(int i) {
		parent.writeByte(i);
		return this;
	}

	/**
	 * Записывает short.
	 *
	 * @param i i
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeShort(int i) {
		parent.writeShort(i);
		return this;
	}

	/**
	 * Записывает short l e.
	 *
	 * @param i i
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeShortLE(int i) {
		parent.writeShortLE(i);
		return this;
	}

	/**
	 * Записывает medium.
	 *
	 * @param i i
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeMedium(int i) {
		parent.writeMedium(i);
		return this;
	}

	/**
	 * Записывает medium l e.
	 *
	 * @param i i
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeMediumLE(int i) {
		parent.writeMediumLE(i);
		return this;
	}

	/**
	 * Записывает int.
	 *
	 * @param i i
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeInt(int i) {
		parent.writeInt(i);
		return this;
	}

	/**
	 * Записывает int l e.
	 *
	 * @param i i
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeIntLE(int i) {
		parent.writeIntLE(i);
		return this;
	}

	/**
	 * Записывает long.
	 *
	 * @param l l
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeLong(long l) {
		parent.writeLong(l);
		return this;
	}

	/**
	 * Записывает long l e.
	 *
	 * @param l l
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeLongLE(long l) {
		parent.writeLongLE(l);
		return this;
	}

	/**
	 * Записывает char.
	 *
	 * @param i i
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeChar(int i) {
		parent.writeChar(i);
		return this;
	}

	/**
	 * Записывает float.
	 *
	 * @param f f
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeFloat(float f) {
		parent.writeFloat(f);
		return this;
	}

	/**
	 * Записывает double.
	 *
	 * @param d d
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeDouble(double d) {
		parent.writeDouble(d);
		return this;
	}

	/**
	 * Записывает bytes.
	 *
	 * @param byteBuf byte buf
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeBytes(ByteBuf byteBuf) {
		parent.writeBytes(byteBuf);
		return this;
	}

	/**
	 * Записывает bytes.
	 *
	 * @param byteBuf byte buf
	 * @param i i
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeBytes(ByteBuf byteBuf, int i) {
		parent.writeBytes(byteBuf, i);
		return this;
	}

	/**
	 * Записывает bytes.
	 *
	 * @param byteBuf byte buf
	 * @param i i
	 * @param j j
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeBytes(ByteBuf byteBuf, int i, int j) {
		parent.writeBytes(byteBuf, i, j);
		return this;
	}

	/**
	 * Записывает bytes.
	 *
	 * @param bs bs
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeBytes(byte[] bs) {
		parent.writeBytes(bs);
		return this;
	}

	/**
	 * Записывает bytes.
	 *
	 * @param bs bs
	 * @param i i
	 * @param j j
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeBytes(byte[] bs, int i, int j) {
		parent.writeBytes(bs, i, j);
		return this;
	}

	/**
	 * Записывает bytes.
	 *
	 * @param byteBuffer byte buffer
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeBytes(ByteBuffer byteBuffer) {
		parent.writeBytes(byteBuffer);
		return this;
	}

	/**
	 * Записывает bytes.
	 *
	 * @param stream stream
	 * @param length length
	 *
	 * @return int — результат операции
	 */
	public int writeBytes(InputStream stream, int length) throws IOException {
		return parent.writeBytes(stream, length);
	}

	/**
	 * Записывает bytes.
	 *
	 * @param channel channel
	 * @param length length
	 *
	 * @return int — результат операции
	 */
	public int writeBytes(ScatteringByteChannel channel, int length) throws IOException {
		return parent.writeBytes(channel, length);
	}

	/**
	 * Записывает bytes.
	 *
	 * @param channel channel
	 * @param pos pos
	 * @param length length
	 *
	 * @return int — результат операции
	 */
	public int writeBytes(FileChannel channel, long pos, int length) throws IOException {
		return parent.writeBytes(channel, pos, length);
	}

	/**
	 * Записывает zero.
	 *
	 * @param i i
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf writeZero(int i) {
		parent.writeZero(i);
		return this;
	}

	/**
	 * Записывает char sequence.
	 *
	 * @param sequence sequence
	 * @param charset charset
	 *
	 * @return int — результат операции
	 */
	public int writeCharSequence(CharSequence sequence, Charset charset) {
		return parent.writeCharSequence(sequence, charset);
	}

	/**
	 * Index of.
	 *
	 * @param from from
	 * @param to to
	 * @param value value
	 *
	 * @return int — результат операции
	 */
	public int indexOf(int from, int to, byte value) {
		return parent.indexOf(from, to, value);
	}

	/**
	 * Bytes before.
	 *
	 * @param value value
	 *
	 * @return int — результат операции
	 */
	public int bytesBefore(byte value) {
		return parent.bytesBefore(value);
	}

	/**
	 * Bytes before.
	 *
	 * @param length length
	 * @param value value
	 *
	 * @return int — результат операции
	 */
	public int bytesBefore(int length, byte value) {
		return parent.bytesBefore(length, value);
	}

	/**
	 * Bytes before.
	 *
	 * @param index index
	 * @param length length
	 * @param value value
	 *
	 * @return int — результат операции
	 */
	public int bytesBefore(int index, int length, byte value) {
		return parent.bytesBefore(index, length, value);
	}

	/**
	 * For each byte.
	 *
	 * @param byteProcessor byte processor
	 *
	 * @return int — результат операции
	 */
	public int forEachByte(ByteProcessor byteProcessor) {
		return parent.forEachByte(byteProcessor);
	}

	/**
	 * For each byte.
	 *
	 * @param index index
	 * @param length length
	 * @param byteProcessor byte processor
	 *
	 * @return int — результат операции
	 */
	public int forEachByte(int index, int length, ByteProcessor byteProcessor) {
		return parent.forEachByte(index, length, byteProcessor);
	}

	/**
	 * For each byte desc.
	 *
	 * @param byteProcessor byte processor
	 *
	 * @return int — результат операции
	 */
	public int forEachByteDesc(ByteProcessor byteProcessor) {
		return parent.forEachByteDesc(byteProcessor);
	}

	/**
	 * For each byte desc.
	 *
	 * @param index index
	 * @param length length
	 * @param byteProcessor byte processor
	 *
	 * @return int — результат операции
	 */
	public int forEachByteDesc(int index, int length, ByteProcessor byteProcessor) {
		return parent.forEachByteDesc(index, length, byteProcessor);
	}

	/**
	 * Copy.
	 *
	 * @return ByteBuf — результат операции
	 */
	public ByteBuf copy() {
		return parent.copy();
	}

	/**
	 * Copy.
	 *
	 * @param index index
	 * @param length length
	 *
	 * @return ByteBuf — результат операции
	 */
	public ByteBuf copy(int index, int length) {
		return parent.copy(index, length);
	}

	/**
	 * Slice.
	 *
	 * @return ByteBuf — результат операции
	 */
	public ByteBuf slice() {
		return parent.slice();
	}

	/**
	 * Retained slice.
	 *
	 * @return ByteBuf — результат операции
	 */
	public ByteBuf retainedSlice() {
		return parent.retainedSlice();
	}

	/**
	 * Slice.
	 *
	 * @param index index
	 * @param length length
	 *
	 * @return ByteBuf — результат операции
	 */
	public ByteBuf slice(int index, int length) {
		return parent.slice(index, length);
	}

	/**
	 * Retained slice.
	 *
	 * @param index index
	 * @param length length
	 *
	 * @return ByteBuf — результат операции
	 */
	public ByteBuf retainedSlice(int index, int length) {
		return parent.retainedSlice(index, length);
	}

	/**
	 * Duplicate.
	 *
	 * @return ByteBuf — результат операции
	 */
	public ByteBuf duplicate() {
		return parent.duplicate();
	}

	/**
	 * Retained duplicate.
	 *
	 * @return ByteBuf — результат операции
	 */
	public ByteBuf retainedDuplicate() {
		return parent.retainedDuplicate();
	}

	/**
	 * Nio buffer count.
	 *
	 * @return int — результат операции
	 */
	public int nioBufferCount() {
		return parent.nioBufferCount();
	}

	/**
	 * Nio buffer.
	 *
	 * @return ByteBuffer — результат операции
	 */
	public ByteBuffer nioBuffer() {
		return parent.nioBuffer();
	}

	/**
	 * Nio buffer.
	 *
	 * @param index index
	 * @param length length
	 *
	 * @return ByteBuffer — результат операции
	 */
	public ByteBuffer nioBuffer(int index, int length) {
		return parent.nioBuffer(index, length);
	}

	/**
	 * Internal nio buffer.
	 *
	 * @param index index
	 * @param length length
	 *
	 * @return ByteBuffer — результат операции
	 */
	public ByteBuffer internalNioBuffer(int index, int length) {
		return parent.internalNioBuffer(index, length);
	}

	/**
	 * Nio buffers.
	 *
	 * @return ByteBuffer[] — результат операции
	 */
	public ByteBuffer[] nioBuffers() {
		return parent.nioBuffers();
	}

	/**
	 * Nio buffers.
	 *
	 * @param index index
	 * @param length length
	 *
	 * @return ByteBuffer[] — результат операции
	 */
	public ByteBuffer[] nioBuffers(int index, int length) {
		return parent.nioBuffers(index, length);
	}

	public boolean hasArray() {
		return parent.hasArray();
	}

	/**
	 * Array.
	 *
	 * @return byte[] — результат операции
	 */
	public byte[] array() {
		return parent.array();
	}

	/**
	 * Array offset.
	 *
	 * @return int — результат операции
	 */
	public int arrayOffset() {
		return parent.arrayOffset();
	}

	public boolean hasMemoryAddress() {
		return parent.hasMemoryAddress();
	}

	/**
	 * Memory address.
	 *
	 * @return long — результат операции
	 */
	public long memoryAddress() {
		return parent.memoryAddress();
	}

	/**
	 * To string.
	 *
	 * @param charset charset
	 *
	 * @return String — результат операции
	 */
	public String toString(Charset charset) {
		return parent.toString(charset);
	}

	/**
	 * To string.
	 *
	 * @param index index
	 * @param length length
	 * @param charset charset
	 *
	 * @return String — результат операции
	 */
	public String toString(int index, int length, Charset charset) {
		return parent.toString(index, length, charset);
	}

	/**
	 * Проверяет наличие h code.
	 *
	 * @return int — {@code true} если условие выполнено
	 */
	public int hashCode() {
		return parent.hashCode();
	}

	/**
	 * Equals.
	 *
	 * @param o o
	 *
	 * @return boolean — результат операции
	 */
	public boolean equals(Object o) {
		return parent.equals(o);
	}

	/**
	 * Compare to.
	 *
	 * @param byteBuf byte buf
	 *
	 * @return int — результат операции
	 */
	public int compareTo(ByteBuf byteBuf) {
		return parent.compareTo(byteBuf);
	}

	/**
	 * To string.
	 *
	 * @return String — результат операции
	 */
	public String toString() {
		return parent.toString();
	}

	/**
	 * Retain.
	 *
	 * @param i i
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf retain(int i) {
		parent.retain(i);
		return this;
	}

	/**
	 * Retain.
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf retain() {
		parent.retain();
		return this;
	}

	/**
	 * Touch.
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf touch() {
		parent.touch();
		return this;
	}

	/**
	 * Touch.
	 *
	 * @param object object
	 *
	 * @return PacketByteBuf — результат операции
	 */
	public PacketByteBuf touch(Object object) {
		parent.touch(object);
		return this;
	}

	/**
	 * Ref cnt.
	 *
	 * @return int — результат операции
	 */
	public int refCnt() {
		return parent.refCnt();
	}

	/**
	 * Release.
	 *
	 * @return boolean — результат операции
	 */
	public boolean release() {
		return parent.release();
	}

	/**
	 * Release.
	 *
	 * @param decrement decrement
	 *
	 * @return boolean — результат операции
	 */
	public boolean release(int decrement) {
		return parent.release(decrement);
	}
}

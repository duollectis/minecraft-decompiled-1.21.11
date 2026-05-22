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

	/**
	 * Декодирует объект из NBT-данных в буфере с помощью указанного кодека.
	 *
	 * @param ops   операции над NBT-деревом
	 * @param codec кодек для десериализации
	 * @return десериализованный объект
	 * @deprecated Используй {@link net.minecraft.network.codec.PacketCodec} вместо этого.
	 */
	@Deprecated
	public <T> T decode(DynamicOps<NbtElement> ops, Codec<T> codec) {
		return decode(ops, codec, NbtSizeTracker.ofUnlimitedBytes());
	}

	/**
	 * Декодирует объект из NBT-данных в буфере с ограничением размера.
	 *
	 * @param ops         операции над NBT-деревом
	 * @param codec       кодек для десериализации
	 * @param sizeTracker ограничитель размера NBT
	 * @return десериализованный объект
	 * @deprecated Используй {@link net.minecraft.network.codec.PacketCodec} вместо этого.
	 */
	@Deprecated
	public <T> T decode(DynamicOps<NbtElement> ops, Codec<T> codec, NbtSizeTracker sizeTracker) {
		NbtElement nbtElement = readNbt(sizeTracker);
		return codec
				.parse(ops, nbtElement)
				.getOrThrow(error -> new DecoderException("Failed to decode: " + error + " " + nbtElement));
	}

	/**
	 * Кодирует объект в NBT и записывает в буфер.
	 *
	 * @param ops   операции над NBT-деревом
	 * @param codec кодек для сериализации
	 * @param value объект для кодирования
	 * @return этот буфер (для цепочки вызовов)
	 * @deprecated Используй {@link net.minecraft.network.codec.PacketCodec} вместо этого.
	 */
	@Deprecated
	public <T> PacketByteBuf encode(DynamicOps<NbtElement> ops, Codec<T> codec, T value) {
		NbtElement nbtElement = codec
				.encodeStart(ops, value)
				.getOrThrow(error -> new EncoderException("Failed to encode: " + error + " " + value));
		writeNbt(nbtElement);
		return this;
	}

	/** Декодирует объект из JSON-строки в буфере с помощью указанного кодека. */
	public <T> T decodeAsJson(Codec<T> codec) {
		JsonElement jsonElement = LenientJsonParser.parse(readString());
		DataResult<T> dataResult = codec.parse(JsonOps.INSTANCE, jsonElement);
		return dataResult.getOrThrow(error -> new DecoderException("Failed to decode JSON: " + error));
	}

	/** Кодирует объект в JSON-строку и записывает её в буфер. */
	public <T> void encodeAsJson(Codec<T> codec, T value) {
		DataResult<JsonElement> dataResult = codec.encodeStart(JsonOps.INSTANCE, value);
		writeString(GSON.toJson(dataResult.getOrThrow(error -> new EncoderException(
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
		int size = readVarInt();
		C collection = collectionFactory.apply(size);

		for (int index = 0; index < size; index++) {
			collection.add(reader.decode(this));
		}

		return collection;
	}

	/** Записывает коллекцию: сначала размер (VarInt), затем элементы. */
	public <T> void writeCollection(Collection<T> collection, PacketEncoder<? super PacketByteBuf, T> writer) {
		writeVarInt(collection.size());

		for (T element : collection) {
			writer.encode(this, element);
		}
	}

	/** Читает список элементов из буфера. */
	public <T> List<T> readList(PacketDecoder<? super PacketByteBuf, T> reader) {
		return readCollection(Lists::newArrayListWithCapacity, reader);
	}

	/** Читает список целых чисел (VarInt) из буфера. */
	public IntList readIntList() {
		int size = readVarInt();
		IntList intList = new IntArrayList();

		for (int index = 0; index < size; index++) {
			intList.add(readVarInt());
		}

		return intList;
	}

	/** Записывает список целых чисел (VarInt) в буфер. */
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
		int size = readVarInt();
		M map = mapFactory.apply(size);

		for (int index = 0; index < size; index++) {
			K key = keyReader.decode(this);
			V value = valueReader.decode(this);
			map.put(key, value);
		}

		return map;
	}

	/** Читает карту ключ-значение из буфера (с HashMap по умолчанию). */
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
			keyWriter.encode(this, key);
			valueWriter.encode(this, value);
		});
	}

	/** Вызывает consumer для каждого элемента коллекции в буфере. */
	public void forEachInCollection(Consumer<PacketByteBuf> consumer) {
		int size = readVarInt();

		for (int index = 0; index < size; index++) {
			consumer.accept(this);
		}
	}

	/** Записывает набор значений перечисления как битовую маску. */
	public <E extends Enum<E>> void writeEnumSet(EnumSet<E> enumSet, Class<E> type) {
		E[] constants = type.getEnumConstants();
		BitSet bitSet = new BitSet(constants.length);

		for (int index = 0; index < constants.length; index++) {
			bitSet.set(index, enumSet.contains(constants[index]));
		}

		writeBitSet(bitSet, constants.length);
	}

	/** Читает набор значений перечисления из битовой маски. */
	public <E extends Enum<E>> EnumSet<E> readEnumSet(Class<E> type) {
		E[] constants = type.getEnumConstants();
		BitSet bitSet = readBitSet(constants.length);
		EnumSet<E> enumSet = EnumSet.noneOf(type);

		for (int index = 0; index < constants.length; index++) {
			if (bitSet.get(index)) {
				enumSet.add(constants[index]);
			}
		}

		return enumSet;
	}

	/** Записывает Optional: флаг присутствия и, если есть, значение. */
	public <T> void writeOptional(Optional<T> value, PacketEncoder<? super PacketByteBuf, T> writer) {
		if (value.isEmpty()) {
			writeBoolean(false);
			return;
		}

		writeBoolean(true);
		writer.encode(this, value.get());
	}

	/** Читает Optional: флаг присутствия и, если есть, значение. */
	public <T> Optional<T> readOptional(PacketDecoder<? super PacketByteBuf, T> reader) {
		return readBoolean() ? Optional.of(reader.decode(this)) : Optional.empty();
	}

	/** Записывает Either: флаг стороны и соответствующее значение. */
	public <L, R> void writeEither(
			Either<L, R> either,
			PacketEncoder<? super PacketByteBuf, L> leftEncoder,
			PacketEncoder<? super PacketByteBuf, R> rightEncoder
	) {
		either.ifLeft(left -> {
			writeBoolean(true);
			leftEncoder.encode(this, left);
		}).ifRight(right -> {
			writeBoolean(false);
			rightEncoder.encode(this, right);
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
	public <T> @Nullable T readNullable(PacketDecoder<? super PacketByteBuf, T> reader) {
		return readNullable(this, reader);
	}

	/** Читает nullable-значение из произвольного буфера. */
	public static <T, B extends ByteBuf> @Nullable T readNullable(B buf, PacketDecoder<? super B, T> reader) {
		return buf.readBoolean() ? reader.decode(buf) : null;
	}

	/** Записывает nullable-значение: флаг присутствия и, если есть, значение. */
	public <T> void writeNullable(@Nullable T value, PacketEncoder<? super PacketByteBuf, T> writer) {
		writeNullable(this, value, writer);
	}

	/** Записывает nullable-значение в произвольный буфер. */
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
	public byte[] readByteArray() {
		return readByteArray(this);
	}

	/** Читает массив байт из произвольного буфера (с префиксом длины VarInt). */
	public static byte[] readByteArray(ByteBuf buf) {
		return readByteArray(buf, buf.readableBytes());
	}

	/** Записывает массив байт (с префиксом длины VarInt). */
	public PacketByteBuf writeByteArray(byte[] array) {
		writeByteArray(this, array);
		return this;
	}

	/** Записывает массив байт в произвольный буфер (с префиксом длины VarInt). */
	public static void writeByteArray(ByteBuf buf, byte[] array) {
		VarInts.write(buf, array.length);
		buf.writeBytes(array);
	}

	/** Читает массив байт с проверкой максимального размера. */
	public byte[] readByteArray(int maxSize) {
		return readByteArray(this, maxSize);
	}

	/** Читает массив байт из произвольного буфера с проверкой максимального размера. */
	public static byte[] readByteArray(ByteBuf buf, int maxSize) {
		int length = VarInts.read(buf);

		if (length > maxSize) {
			throw new DecoderException("ByteArray with size " + length + " is bigger than allowed " + maxSize);
		}

		byte[] bytes = new byte[length];
		buf.readBytes(bytes);
		return bytes;
	}

	/** Записывает массив int как VarInt-значения с префиксом длины. */
	public PacketByteBuf writeIntArray(int[] array) {
		writeVarInt(array.length);

		for (int value : array) {
			writeVarInt(value);
		}

		return this;
	}

	/** Читает массив int (VarInt) с проверкой максимального размера. */
	public int[] readIntArray() {
		return readIntArray(readableBytes());
	}

	/** Читает массив int (VarInt) с проверкой максимального размера. */
	public int[] readIntArray(int maxSize) {
		int length = readVarInt();

		if (length > maxSize) {
			throw new DecoderException("VarIntArray with size " + length + " is bigger than allowed " + maxSize);
		}

		int[] array = new int[length];

		for (int index = 0; index < array.length; index++) {
			array[index] = readVarInt();
		}

		return array;
	}

	/** Записывает массив long с префиксом длины VarInt. */
	public PacketByteBuf writeLongArray(long[] values) {
		writeLongArray(this, values);
		return this;
	}

	/** Записывает массив long в произвольный буфер с префиксом длины VarInt. */
	public static void writeLongArray(ByteBuf buf, long[] values) {
		VarInts.write(buf, values.length);
		writeFixedLengthLongArray(buf, values);
	}

	/** Записывает массив long фиксированной длины без префикса. */
	public PacketByteBuf writeFixedLengthLongArray(long[] values) {
		writeFixedLengthLongArray(this, values);
		return this;
	}

	/** Записывает массив long фиксированной длины в произвольный буфер без префикса. */
	public static void writeFixedLengthLongArray(ByteBuf buf, long[] values) {
		for (long value : values) {
			buf.writeLong(value);
		}
	}

	/** Читает массив long с префиксом длины VarInt. */
	public long[] readLongArray() {
		return readLongArray(this);
	}

	/** Читает массив long фиксированной длины без префикса. */
	public long[] readFixedLengthLongArray(long[] values) {
		return readFixedLengthLongArray(this, values);
	}

	/**
	 * Читает массив long из произвольного буфера с проверкой размера.
	 * Максимальный размер ограничен количеством доступных байт (каждый long = 8 байт).
	 */
	public static long[] readLongArray(ByteBuf buf) {
		int length = VarInts.read(buf);
		int maxAllowed = buf.readableBytes() / 8;

		if (length > maxAllowed) {
			throw new DecoderException("LongArray with size " + length + " is bigger than allowed " + maxAllowed);
		}

		return readFixedLengthLongArray(buf, new long[length]);
	}

	/** Читает массив long фиксированной длины из произвольного буфера без префикса. */
	public static long[] readFixedLengthLongArray(ByteBuf buf, long[] values) {
		for (int index = 0; index < values.length; index++) {
			values[index] = buf.readLong();
		}

		return values;
	}

	/** Читает позицию блока (упакованную в long). */
	public BlockPos readBlockPos() {
		return readBlockPos(this);
	}

	/** Читает позицию блока из произвольного буфера (упакованную в long). */
	public static BlockPos readBlockPos(ByteBuf buf) {
		return BlockPos.fromLong(buf.readLong());
	}

	/** Записывает позицию блока (упакованную в long). */
	public PacketByteBuf writeBlockPos(BlockPos pos) {
		writeBlockPos(this, pos);
		return this;
	}

	/** Записывает позицию блока в произвольный буфер (упакованную в long). */
	public static void writeBlockPos(ByteBuf buf, BlockPos pos) {
		buf.writeLong(pos.asLong());
	}

	/** Читает позицию чанка (упакованную в long). */
	public ChunkPos readChunkPos() {
		return new ChunkPos(readLong());
	}

	/** Записывает позицию чанка (упакованную в long). */
	public PacketByteBuf writeChunkPos(ChunkPos pos) {
		writeLong(pos.toLong());
		return this;
	}

	/** Читает позицию чанка из произвольного буфера (упакованную в long). */
	public static ChunkPos readChunkPos(ByteBuf buf) {
		return new ChunkPos(buf.readLong());
	}

	/** Записывает позицию чанка в произвольный буфер (упакованную в long). */
	public static void writeChunkPos(ByteBuf buf, ChunkPos pos) {
		buf.writeLong(pos.toLong());
	}

	/** Читает глобальную позицию (измерение + координаты блока). */
	public GlobalPos readGlobalPos() {
		RegistryKey<World> registryKey = readRegistryKey(RegistryKeys.WORLD);
		BlockPos blockPos = readBlockPos();
		return GlobalPos.create(registryKey, blockPos);
	}

	/** Записывает глобальную позицию (измерение + координаты блока). */
	public void writeGlobalPos(GlobalPos pos) {
		writeRegistryKey(pos.dimension());
		writeBlockPos(pos.pos());
	}

	/** Читает трёхмерный вектор (3 float). */
	public Vector3f readVector3f() {
		return readVector3f(this);
	}

	/** Читает трёхмерный вектор из произвольного буфера (3 float). */
	public static Vector3f readVector3f(ByteBuf buf) {
		return new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
	}

	/** Записывает трёхмерный вектор (3 float). */
	public void writeVector3f(Vector3f vector) {
		writeVector3f(this, vector);
	}

	/** Записывает трёхмерный вектор в произвольный буфер (3 float). */
	public static void writeVector3f(ByteBuf buf, Vector3fc vec) {
		buf.writeFloat(vec.x());
		buf.writeFloat(vec.y());
		buf.writeFloat(vec.z());
	}

	/** Читает кватернион вращения (4 float). */
	public Quaternionf readQuaternionf() {
		return readQuaternionf(this);
	}

	/** Читает кватернион вращения из произвольного буфера (4 float). */
	public static Quaternionf readQuaternionf(ByteBuf buf) {
		return new Quaternionf(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
	}

	/** Записывает кватернион вращения (4 float). */
	public void writeQuaternionf(Quaternionf quaternion) {
		writeQuaternionf(this, quaternion);
	}

	/** Записывает кватернион вращения в произвольный буфер (4 float). */
	public static void writeQuaternionf(ByteBuf buf, Quaternionfc quaternion) {
		buf.writeFloat(quaternion.x());
		buf.writeFloat(quaternion.y());
		buf.writeFloat(quaternion.z());
		buf.writeFloat(quaternion.w());
	}

	/** Читает трёхмерный вектор с двойной точностью из произвольного буфера (3 double). */
	public static Vec3d readVec3d(ByteBuf buf) {
		return new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
	}

	/** Читает трёхмерный вектор с двойной точностью (3 double). */
	public Vec3d readVec3d() {
		return readVec3d(this);
	}

	/** Записывает трёхмерный вектор с двойной точностью в произвольный буфер (3 double). */
	public static void writeVec3d(ByteBuf buf, Vec3d vec) {
		buf.writeDouble(vec.getX());
		buf.writeDouble(vec.getY());
		buf.writeDouble(vec.getZ());
	}

	/** Записывает трёхмерный вектор с двойной точностью (3 double). */
	public void writeVec3d(Vec3d vec) {
		writeVec3d(this, vec);
	}

	/** Читает скорость сущности (закодированную как short-тройка). */
	public Vec3d readVelocity() {
		return VelocityEncoding.readVelocity(this);
	}

	/** Записывает скорость сущности (закодированную как short-тройка). */
	public void writeVelocity(Vec3d velocity) {
		VelocityEncoding.writeVelocity(this, velocity);
	}

	/** Читает константу перечисления по её порядковому номеру (VarInt). */
	public <T extends Enum<T>> T readEnumConstant(Class<T> enumClass) {
		return enumClass.getEnumConstants()[readVarInt()];
	}

	/** Записывает порядковый номер константы перечисления (VarInt). */
	public PacketByteBuf writeEnumConstant(Enum<?> instance) {
		return writeVarInt(instance.ordinal());
	}

	/** Декодирует значение по целочисленному идентификатору (VarInt). */
	public <T> T decode(IntFunction<T> idToValue) {
		return idToValue.apply(readVarInt());
	}

	/** Кодирует значение в целочисленный идентификатор (VarInt). */
	public <T> PacketByteBuf encode(ToIntFunction<T> valueToId, T value) {
		return writeVarInt(valueToId.applyAsInt(value));
	}

	/** Читает целое число в формате VarInt (переменная длина, 1-5 байт). */
	public int readVarInt() {
		return VarInts.read(parent);
	}

	/** Читает длинное целое в формате VarLong (переменная длина, 1-10 байт). */
	public long readVarLong() {
		return VarLongs.read(parent);
	}

	/** Записывает UUID как два long (most/least significant bits). */
	public PacketByteBuf writeUuid(UUID uuid) {
		writeUuid(this, uuid);
		return this;
	}

	/** Записывает UUID в произвольный буфер как два long (most/least significant bits). */
	public static void writeUuid(ByteBuf buf, UUID uuid) {
		buf.writeLong(uuid.getMostSignificantBits());
		buf.writeLong(uuid.getLeastSignificantBits());
	}

	/** Читает UUID из двух long (most/least significant bits). */
	public UUID readUuid() {
		return readUuid(this);
	}

	/** Читает UUID из произвольного буфера из двух long (most/least significant bits). */
	public static UUID readUuid(ByteBuf buf) {
		return new UUID(buf.readLong(), buf.readLong());
	}

	/** Записывает целое число в формате VarInt (переменная длина, 1-5 байт). */
	public PacketByteBuf writeVarInt(int value) {
		VarInts.write(parent, value);
		return this;
	}

	/** Записывает длинное целое в формате VarLong (переменная длина, 1-10 байт). */
	public PacketByteBuf writeVarLong(long value) {
		VarLongs.write(parent, value);
		return this;
	}

	/** Записывает NBT-элемент в буфер (null записывается как NbtEnd). */
	public PacketByteBuf writeNbt(@Nullable NbtElement nbt) {
		writeNbt(this, nbt);
		return this;
	}

	/** Записывает NBT-элемент в произвольный буфер (null записывается как NbtEnd). */
	public static void writeNbt(ByteBuf buf, @Nullable NbtElement nbt) {
		if (nbt == null) {
			nbt = NbtEnd.INSTANCE;
		}

		try {
			NbtIo.writeForPacket(nbt, new ByteBufOutputStream(buf));
		}
		catch (IOException e) {
			throw new EncoderException(e);
		}
	}

	/** Читает NBT-тег из буфера. */
	public @Nullable NbtCompound readNbt() {
		return readNbt(this);
	}

	/** Читает NBT-тег из произвольного буфера. */
	public static @Nullable NbtCompound readNbt(ByteBuf buf) {
		NbtElement nbtElement = readNbt(buf, NbtSizeTracker.forPacket());

		if (nbtElement != null && !(nbtElement instanceof NbtCompound)) {
			throw new DecoderException("Not a compound tag: " + nbtElement);
		}

		return (NbtCompound) nbtElement;
	}

	/** Читает произвольный NBT-элемент из буфера с ограничением размера. */
	public static @Nullable NbtElement readNbt(ByteBuf buf, NbtSizeTracker sizeTracker) {
		try {
			NbtElement nbtElement = NbtIo.read(new ByteBufInputStream(buf), sizeTracker);
			return nbtElement.getType() == 0 ? null : nbtElement;
		}
		catch (IOException e) {
			throw new EncoderException(e);
		}
	}

	/** Читает произвольный NBT-элемент из буфера с ограничением размера. */
	public @Nullable NbtElement readNbt(NbtSizeTracker sizeTracker) {
		return readNbt(this, sizeTracker);
	}

	/** Читает строку UTF-8 с максимальной длиной по умолчанию. */
	public String readString() {
		return readString(DEFAULT_MAX_STRING_LENGTH);
	}

	/** Читает строку UTF-8 с проверкой максимальной длины. */
	public String readString(int maxLength) {
		return StringEncoding.decode(parent, maxLength);
	}

	/** Записывает строку UTF-8 с максимальной длиной по умолчанию. */
	public PacketByteBuf writeString(String string) {
		return writeString(string, DEFAULT_MAX_STRING_LENGTH);
	}

	/** Записывает строку UTF-8 с проверкой максимальной длины. */
	public PacketByteBuf writeString(String string, int maxLength) {
		StringEncoding.encode(parent, string, maxLength);
		return this;
	}

	/** Читает идентификатор ресурса (namespace:path). */
	public Identifier readIdentifier() {
		return Identifier.of(readString(DEFAULT_MAX_STRING_LENGTH));
	}

	/** Записывает идентификатор ресурса (namespace:path). */
	public PacketByteBuf writeIdentifier(Identifier id) {
		writeString(id.toString());
		return this;
	}

	/** Читает ключ реестра для указанного реестра. */
	public <T> RegistryKey<T> readRegistryKey(RegistryKey<? extends Registry<T>> registryRef) {
		Identifier identifier = readIdentifier();
		return RegistryKey.of(registryRef, identifier);
	}

	/** Записывает ключ реестра. */
	public void writeRegistryKey(RegistryKey<?> key) {
		writeIdentifier(key.getValue());
	}

	/** Читает ключ-ссылку на реестр. */
	public <T> RegistryKey<? extends Registry<T>> readRegistryRefKey() {
		Identifier identifier = readIdentifier();
		return RegistryKey.ofRegistry(identifier);
	}

	/** Читает момент времени (epoch millis как long). */
	public Instant readInstant() {
		return Instant.ofEpochMilli(readLong());
	}

	/** Записывает момент времени (epoch millis как long). */
	public void writeInstant(Instant instant) {
		writeLong(instant.toEpochMilli());
	}

	/** Читает RSA публичный ключ из байтового массива. */
	public PublicKey readPublicKey() {
		try {
			return NetworkEncryptionUtils.decodeEncodedRsaPublicKey(readByteArray(MAX_PUBLIC_KEY_LENGTH));
		}
		catch (NetworkEncryptionException e) {
			throw new DecoderException("Malformed public key bytes", e);
		}
	}

	/** Записывает RSA публичный ключ как байтовый массив. */
	public PacketByteBuf writePublicKey(PublicKey publicKey) {
		writeByteArray(publicKey.getEncoded());
		return this;
	}

	/**
	 * Читает результат попадания по блоку.
	 * Смещение внутри блока кодируется как три float относительно координат блока.
	 */
	public BlockHitResult readBlockHitResult() {
		BlockPos blockPos = readBlockPos();
		Direction direction = readEnumConstant(Direction.class);
		float offsetX = readFloat();
		float offsetY = readFloat();
		float offsetZ = readFloat();
		boolean insideBlock = readBoolean();
		boolean againstWorldBorder = readBoolean();
		return new BlockHitResult(
				new Vec3d(
						blockPos.getX() + offsetX,
						blockPos.getY() + offsetY,
						blockPos.getZ() + offsetZ
				),
				direction,
				blockPos,
				insideBlock,
				againstWorldBorder
		);
	}

	/** Записывает результат попадания по блоку. */
	public void writeBlockHitResult(BlockHitResult hitResult) {
		BlockPos blockPos = hitResult.getBlockPos();
		writeBlockPos(blockPos);
		writeEnumConstant(hitResult.getSide());
		Vec3d pos = hitResult.getPos();
		writeFloat((float) (pos.x - blockPos.getX()));
		writeFloat((float) (pos.y - blockPos.getY()));
		writeFloat((float) (pos.z - blockPos.getZ()));
		writeBoolean(hitResult.isInsideBlock());
		writeBoolean(hitResult.isAgainstWorldBorder());
	}

	/** Читает BitSet из буфера (с префиксом длины). */
	public BitSet readBitSet() {
		return BitSet.valueOf(readLongArray());
	}

	/** Записывает BitSet в буфер (с префиксом длины). */
	public void writeBitSet(BitSet bitSet) {
		writeLongArray(bitSet.toLongArray());
	}

	/** Читает BitSet фиксированного размера из буфера (без префикса длины). */
	public BitSet readBitSet(int size) {
		byte[] bytes = new byte[MathHelper.ceilDiv(size, 8)];
		readBytes(bytes);
		return BitSet.valueOf(bytes);
	}

	/**
	 * Записывает BitSet фиксированного размера в буфер (без префикса длины).
	 * Выбрасывает исключение, если BitSet превышает заявленный размер.
	 */
	public void writeBitSet(BitSet bitSet, int size) {
		if (bitSet.length() > size) {
			throw new EncoderException("BitSet is larger than expected size (" + bitSet.length() + ">" + size + ")");
		}

		byte[] bytes = bitSet.toByteArray();
		writeBytes(Arrays.copyOf(bytes, MathHelper.ceilDiv(size, 8)));
	}

	/** Читает идентификатор синхронизации контейнера (VarInt) из произвольного буфера. */
	public static int readSyncId(ByteBuf buf) {
		return VarInts.read(buf);
	}

	/** Читает идентификатор синхронизации контейнера (VarInt). */
	public int readSyncId() {
		return readSyncId(parent);
	}

	/** Записывает идентификатор синхронизации контейнера (VarInt) в произвольный буфер. */
	public static void writeSyncId(ByteBuf buf, int syncId) {
		VarInts.write(buf, syncId);
	}

	/** Записывает идентификатор синхронизации контейнера (VarInt). */
	public void writeSyncId(int syncId) {
		writeSyncId(parent, syncId);
	}

	public boolean isContiguous() {
		return parent.isContiguous();
	}

	public int maxFastWritableBytes() {
		return parent.maxFastWritableBytes();
	}

	public int capacity() {
		return parent.capacity();
	}

	public PacketByteBuf capacity(int capacity) {
		parent.capacity(capacity);
		return this;
	}

	public int maxCapacity() {
		return parent.maxCapacity();
	}

	public ByteBufAllocator alloc() {
		return parent.alloc();
	}

	public ByteOrder order() {
		return parent.order();
	}

	public ByteBuf order(ByteOrder byteOrder) {
		return parent.order(byteOrder);
	}

	public ByteBuf unwrap() {
		return parent;
	}

	public boolean isDirect() {
		return parent.isDirect();
	}

	public boolean isReadOnly() {
		return parent.isReadOnly();
	}

	public ByteBuf asReadOnly() {
		return parent.asReadOnly();
	}

	public int readerIndex() {
		return parent.readerIndex();
	}

	public PacketByteBuf readerIndex(int index) {
		parent.readerIndex(index);
		return this;
	}

	public int writerIndex() {
		return parent.writerIndex();
	}

	public PacketByteBuf writerIndex(int index) {
		parent.writerIndex(index);
		return this;
	}

	public PacketByteBuf setIndex(int readerIndex, int writerIndex) {
		parent.setIndex(readerIndex, writerIndex);
		return this;
	}

	public int readableBytes() {
		return parent.readableBytes();
	}

	public int writableBytes() {
		return parent.writableBytes();
	}

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

	public PacketByteBuf clear() {
		parent.clear();
		return this;
	}

	public PacketByteBuf markReaderIndex() {
		parent.markReaderIndex();
		return this;
	}

	public PacketByteBuf resetReaderIndex() {
		parent.resetReaderIndex();
		return this;
	}

	public PacketByteBuf markWriterIndex() {
		parent.markWriterIndex();
		return this;
	}

	public PacketByteBuf resetWriterIndex() {
		parent.resetWriterIndex();
		return this;
	}

	public PacketByteBuf discardReadBytes() {
		parent.discardReadBytes();
		return this;
	}

	public PacketByteBuf discardSomeReadBytes() {
		parent.discardSomeReadBytes();
		return this;
	}

	public PacketByteBuf ensureWritable(int minBytes) {
		parent.ensureWritable(minBytes);
		return this;
	}

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

	public PacketByteBuf getBytes(int index, ByteBuf dst) {
		parent.getBytes(index, dst);
		return this;
	}

	public PacketByteBuf getBytes(int index, ByteBuf dst, int length) {
		parent.getBytes(index, dst, length);
		return this;
	}

	public PacketByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
		parent.getBytes(index, dst, dstIndex, length);
		return this;
	}

	public PacketByteBuf getBytes(int index, byte[] dst) {
		parent.getBytes(index, dst);
		return this;
	}

	public PacketByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
		parent.getBytes(index, dst, dstIndex, length);
		return this;
	}

	public PacketByteBuf getBytes(int index, ByteBuffer dst) {
		parent.getBytes(index, dst);
		return this;
	}

	public PacketByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
		parent.getBytes(index, out, length);
		return this;
	}

	public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
		return parent.getBytes(index, out, length);
	}

	public int getBytes(int index, FileChannel out, long position, int length) throws IOException {
		return parent.getBytes(index, out, position, length);
	}

	public CharSequence getCharSequence(int index, int length, Charset charset) {
		return parent.getCharSequence(index, length, charset);
	}

	public PacketByteBuf setBoolean(int index, boolean value) {
		parent.setBoolean(index, value);
		return this;
	}

	public PacketByteBuf setByte(int index, int value) {
		parent.setByte(index, value);
		return this;
	}

	public PacketByteBuf setShort(int index, int value) {
		parent.setShort(index, value);
		return this;
	}

	public PacketByteBuf setShortLE(int index, int value) {
		parent.setShortLE(index, value);
		return this;
	}

	public PacketByteBuf setMedium(int index, int value) {
		parent.setMedium(index, value);
		return this;
	}

	public PacketByteBuf setMediumLE(int index, int value) {
		parent.setMediumLE(index, value);
		return this;
	}

	public PacketByteBuf setInt(int index, int value) {
		parent.setInt(index, value);
		return this;
	}

	public PacketByteBuf setIntLE(int index, int value) {
		parent.setIntLE(index, value);
		return this;
	}

	public PacketByteBuf setLong(int index, long value) {
		parent.setLong(index, value);
		return this;
	}

	public PacketByteBuf setLongLE(int index, long value) {
		parent.setLongLE(index, value);
		return this;
	}

	public PacketByteBuf setChar(int index, int value) {
		parent.setChar(index, value);
		return this;
	}

	public PacketByteBuf setFloat(int index, float value) {
		parent.setFloat(index, value);
		return this;
	}

	public PacketByteBuf setDouble(int index, double value) {
		parent.setDouble(index, value);
		return this;
	}

	public PacketByteBuf setBytes(int index, ByteBuf src) {
		parent.setBytes(index, src);
		return this;
	}

	public PacketByteBuf setBytes(int index, ByteBuf src, int length) {
		parent.setBytes(index, src, length);
		return this;
	}

	public PacketByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
		parent.setBytes(index, src, srcIndex, length);
		return this;
	}

	public PacketByteBuf setBytes(int index, byte[] src) {
		parent.setBytes(index, src);
		return this;
	}

	public PacketByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
		parent.setBytes(index, src, srcIndex, length);
		return this;
	}

	public PacketByteBuf setBytes(int index, ByteBuffer src) {
		parent.setBytes(index, src);
		return this;
	}

	public int setBytes(int index, InputStream src, int length) throws IOException {
		return parent.setBytes(index, src, length);
	}

	public int setBytes(int index, ScatteringByteChannel src, int length) throws IOException {
		return parent.setBytes(index, src, length);
	}

	public int setBytes(int index, FileChannel src, long position, int length) throws IOException {
		return parent.setBytes(index, src, position, length);
	}

	public PacketByteBuf setZero(int index, int length) {
		parent.setZero(index, length);
		return this;
	}

	public int setCharSequence(int index, CharSequence sequence, Charset charset) {
		return parent.setCharSequence(index, sequence, charset);
	}

	public boolean readBoolean() {
		return parent.readBoolean();
	}

	public byte readByte() {
		return parent.readByte();
	}

	public short readUnsignedByte() {
		return parent.readUnsignedByte();
	}

	public short readShort() {
		return parent.readShort();
	}

	public short readShortLE() {
		return parent.readShortLE();
	}

	public int readUnsignedShort() {
		return parent.readUnsignedShort();
	}

	public int readUnsignedShortLE() {
		return parent.readUnsignedShortLE();
	}

	public int readMedium() {
		return parent.readMedium();
	}

	public int readMediumLE() {
		return parent.readMediumLE();
	}

	public int readUnsignedMedium() {
		return parent.readUnsignedMedium();
	}

	public int readUnsignedMediumLE() {
		return parent.readUnsignedMediumLE();
	}

	public int readInt() {
		return parent.readInt();
	}

	public int readIntLE() {
		return parent.readIntLE();
	}

	public long readUnsignedInt() {
		return parent.readUnsignedInt();
	}

	public long readUnsignedIntLE() {
		return parent.readUnsignedIntLE();
	}

	public long readLong() {
		return parent.readLong();
	}

	public long readLongLE() {
		return parent.readLongLE();
	}

	public char readChar() {
		return parent.readChar();
	}

	public float readFloat() {
		return parent.readFloat();
	}

	public double readDouble() {
		return parent.readDouble();
	}

	public ByteBuf readBytes(int length) {
		return parent.readBytes(length);
	}

	public ByteBuf readSlice(int length) {
		return parent.readSlice(length);
	}

	public ByteBuf readRetainedSlice(int length) {
		return parent.readRetainedSlice(length);
	}

	public PacketByteBuf readBytes(ByteBuf dst) {
		parent.readBytes(dst);
		return this;
	}

	public PacketByteBuf readBytes(ByteBuf dst, int length) {
		parent.readBytes(dst, length);
		return this;
	}

	public PacketByteBuf readBytes(ByteBuf dst, int dstIndex, int length) {
		parent.readBytes(dst, dstIndex, length);
		return this;
	}

	public PacketByteBuf readBytes(byte[] dst) {
		parent.readBytes(dst);
		return this;
	}

	public PacketByteBuf readBytes(byte[] dst, int dstIndex, int length) {
		parent.readBytes(dst, dstIndex, length);
		return this;
	}

	public PacketByteBuf readBytes(ByteBuffer dst) {
		parent.readBytes(dst);
		return this;
	}

	public PacketByteBuf readBytes(OutputStream out, int length) throws IOException {
		parent.readBytes(out, length);
		return this;
	}

	public int readBytes(GatheringByteChannel out, int length) throws IOException {
		return parent.readBytes(out, length);
	}

	public CharSequence readCharSequence(int length, Charset charset) {
		return parent.readCharSequence(length, charset);
	}

	public String readString(int length, Charset charset) {
		return parent.readString(length, charset);
	}

	public int readBytes(FileChannel out, long position, int length) throws IOException {
		return parent.readBytes(out, position, length);
	}

	public PacketByteBuf skipBytes(int length) {
		parent.skipBytes(length);
		return this;
	}

	public PacketByteBuf writeBoolean(boolean value) {
		parent.writeBoolean(value);
		return this;
	}

	public PacketByteBuf writeByte(int value) {
		parent.writeByte(value);
		return this;
	}

	public PacketByteBuf writeShort(int value) {
		parent.writeShort(value);
		return this;
	}

	public PacketByteBuf writeShortLE(int value) {
		parent.writeShortLE(value);
		return this;
	}

	public PacketByteBuf writeMedium(int value) {
		parent.writeMedium(value);
		return this;
	}

	public PacketByteBuf writeMediumLE(int value) {
		parent.writeMediumLE(value);
		return this;
	}

	public PacketByteBuf writeInt(int value) {
		parent.writeInt(value);
		return this;
	}

	public PacketByteBuf writeIntLE(int value) {
		parent.writeIntLE(value);
		return this;
	}

	public PacketByteBuf writeLong(long value) {
		parent.writeLong(value);
		return this;
	}

	public PacketByteBuf writeLongLE(long value) {
		parent.writeLongLE(value);
		return this;
	}

	public PacketByteBuf writeChar(int value) {
		parent.writeChar(value);
		return this;
	}

	public PacketByteBuf writeFloat(float value) {
		parent.writeFloat(value);
		return this;
	}

	public PacketByteBuf writeDouble(double value) {
		parent.writeDouble(value);
		return this;
	}

	public PacketByteBuf writeBytes(ByteBuf src) {
		parent.writeBytes(src);
		return this;
	}

	public PacketByteBuf writeBytes(ByteBuf src, int length) {
		parent.writeBytes(src, length);
		return this;
	}

	public PacketByteBuf writeBytes(ByteBuf src, int srcIndex, int length) {
		parent.writeBytes(src, srcIndex, length);
		return this;
	}

	public PacketByteBuf writeBytes(byte[] src) {
		parent.writeBytes(src);
		return this;
	}

	public PacketByteBuf writeBytes(byte[] src, int srcIndex, int length) {
		parent.writeBytes(src, srcIndex, length);
		return this;
	}

	public PacketByteBuf writeBytes(ByteBuffer src) {
		parent.writeBytes(src);
		return this;
	}

	public int writeBytes(InputStream src, int length) throws IOException {
		return parent.writeBytes(src, length);
	}

	public int writeBytes(ScatteringByteChannel src, int length) throws IOException {
		return parent.writeBytes(src, length);
	}

	public int writeBytes(FileChannel src, long position, int length) throws IOException {
		return parent.writeBytes(src, position, length);
	}

	public PacketByteBuf writeZero(int length) {
		parent.writeZero(length);
		return this;
	}

	public int writeCharSequence(CharSequence sequence, Charset charset) {
		return parent.writeCharSequence(sequence, charset);
	}

	public int indexOf(int from, int to, byte value) {
		return parent.indexOf(from, to, value);
	}

	public int bytesBefore(byte value) {
		return parent.bytesBefore(value);
	}

	public int bytesBefore(int length, byte value) {
		return parent.bytesBefore(length, value);
	}

	public int bytesBefore(int index, int length, byte value) {
		return parent.bytesBefore(index, length, value);
	}

	public int forEachByte(ByteProcessor processor) {
		return parent.forEachByte(processor);
	}

	public int forEachByte(int index, int length, ByteProcessor processor) {
		return parent.forEachByte(index, length, processor);
	}

	public int forEachByteDesc(ByteProcessor processor) {
		return parent.forEachByteDesc(processor);
	}

	public int forEachByteDesc(int index, int length, ByteProcessor processor) {
		return parent.forEachByteDesc(index, length, processor);
	}

	public ByteBuf copy() {
		return parent.copy();
	}

	public ByteBuf copy(int index, int length) {
		return parent.copy(index, length);
	}

	public ByteBuf slice() {
		return parent.slice();
	}

	public ByteBuf retainedSlice() {
		return parent.retainedSlice();
	}

	public ByteBuf slice(int index, int length) {
		return parent.slice(index, length);
	}

	public ByteBuf retainedSlice(int index, int length) {
		return parent.retainedSlice(index, length);
	}

	public ByteBuf duplicate() {
		return parent.duplicate();
	}

	public ByteBuf retainedDuplicate() {
		return parent.retainedDuplicate();
	}

	public int nioBufferCount() {
		return parent.nioBufferCount();
	}

	public ByteBuffer nioBuffer() {
		return parent.nioBuffer();
	}

	public ByteBuffer nioBuffer(int index, int length) {
		return parent.nioBuffer(index, length);
	}

	public ByteBuffer internalNioBuffer(int index, int length) {
		return parent.internalNioBuffer(index, length);
	}

	public ByteBuffer[] nioBuffers() {
		return parent.nioBuffers();
	}

	public ByteBuffer[] nioBuffers(int index, int length) {
		return parent.nioBuffers(index, length);
	}

	public boolean hasArray() {
		return parent.hasArray();
	}

	public byte[] array() {
		return parent.array();
	}

	public int arrayOffset() {
		return parent.arrayOffset();
	}

	public boolean hasMemoryAddress() {
		return parent.hasMemoryAddress();
	}

	public long memoryAddress() {
		return parent.memoryAddress();
	}

	public String toString(Charset charset) {
		return parent.toString(charset);
	}

	public String toString(int index, int length, Charset charset) {
		return parent.toString(index, length, charset);
	}

	@Override
	public int hashCode() {
		return parent.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return parent.equals(other);
	}

	public int compareTo(ByteBuf other) {
		return parent.compareTo(other);
	}

	@Override
	public String toString() {
		return parent.toString();
	}

	public PacketByteBuf retain(int increment) {
		parent.retain(increment);
		return this;
	}

	public PacketByteBuf retain() {
		parent.retain();
		return this;
	}

	public PacketByteBuf touch() {
		parent.touch();
		return this;
	}

	public PacketByteBuf touch(Object hint) {
		parent.touch(hint);
		return this;
	}

	public int refCnt() {
		return parent.refCnt();
	}

	public boolean release() {
		return parent.release();
	}

	public boolean release(int decrement) {
		return parent.release(decrement);
	}
}

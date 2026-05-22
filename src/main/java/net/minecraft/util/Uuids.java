package net.minecraft.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import com.mojang.util.UndashedUuid;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/**
 * Утилиты для сериализации и десериализации {@link UUID} в различных форматах:
 * int-массив, строка, байтовый массив, пакетный кодек.
 */
public final class Uuids {

	/** Размер UUID в байтах (128 бит = 16 байт). */
	public static final int BYTE_ARRAY_SIZE = 16;
	/** Количество int-элементов для представления UUID (128 бит / 32 бит = 4). */
	private static final int INT_ARRAY_SIZE = 4;
	private static final int INT_MASK = 0xFFFFFFFF;
	private static final String OFFLINE_PLAYER_UUID_PREFIX = "OfflinePlayer:";

	/** Кодек UUID через поток из 4 int-значений (формат NBT). */
	public static final Codec<UUID> INT_STREAM_CODEC = Codec.INT_STREAM
			.comapFlatMap(
					stream -> Util.decodeFixedLengthArray(stream.asLongStream(), INT_ARRAY_SIZE).map(longs -> Uuids.toUuid(java.util.Arrays.stream(longs).mapToInt(l -> (int) l).toArray())),
					uuid -> Arrays.stream(toIntArray(uuid))
			);

	public static final Codec<Set<UUID>> SET_CODEC =
			Codec.list(INT_STREAM_CODEC).xmap(Sets::newHashSet, Lists::newArrayList);

	public static final Codec<Set<UUID>> LINKED_SET_CODEC =
			Codec.list(INT_STREAM_CODEC).xmap(Sets::newLinkedHashSet, Lists::newArrayList);

	/** Кодек UUID через строку в стандартном формате с дефисами. */
	public static final Codec<UUID> STRING_CODEC = Codec.STRING.comapFlatMap(
			string -> {
				try {
					return DataResult.success(UUID.fromString(string), Lifecycle.stable());
				}
				catch (IllegalArgumentException exception) {
					return DataResult.error(() -> "Invalid UUID " + string + ": " + exception.getMessage());
				}
			},
			UUID::toString
	);

	/** Кодек UUID: принимает строку без дефисов или int-массив; сериализует без дефисов. */
	public static final Codec<UUID> CODEC = Codec.withAlternative(
			Codec.STRING.comapFlatMap(
					string -> {
						try {
							return DataResult.success(UndashedUuid.fromStringLenient(string), Lifecycle.stable());
						}
						catch (IllegalArgumentException exception) {
							return DataResult.error(() -> "Invalid UUID " + string + ": " + exception.getMessage());
						}
					},
					UndashedUuid::toString
			),
			INT_STREAM_CODEC
	);

	/** Строгий кодек: предпочитает int-массив, допускает строку как запасной вариант. */
	public static final Codec<UUID> STRICT_CODEC = Codec.withAlternative(INT_STREAM_CODEC, STRING_CODEC);

	public static final PacketCodec<ByteBuf, UUID> PACKET_CODEC = new PacketCodec<>() {
		@Override
		public UUID decode(ByteBuf byteBuf) {
			return PacketByteBuf.readUuid(byteBuf);
		}

		@Override
		public void encode(ByteBuf byteBuf, UUID uuid) {
			PacketByteBuf.writeUuid(byteBuf, uuid);
		}
	};

	private Uuids() {
	}

	/**
	 * Конвертирует массив из 4 int-значений в {@link UUID}.
	 *
	 * @param array массив из 4 элементов: [mostHigh, mostLow, leastHigh, leastLow]
	 * @return UUID
	 */
	public static UUID toUuid(int[] array) {
		long most = (long) array[0] << 32 | array[1] & (long) INT_MASK;
		long least = (long) array[2] << 32 | array[3] & (long) INT_MASK;
		return new UUID(most, least);
	}

	/**
	 * Конвертирует {@link UUID} в массив из 4 int-значений.
	 *
	 * @param uuid UUID для конвертации
	 * @return массив из 4 элементов
	 */
	public static int[] toIntArray(UUID uuid) {
		long most = uuid.getMostSignificantBits();
		long least = uuid.getLeastSignificantBits();
		return new int[]{(int) (most >> 32), (int) most, (int) (least >> 32), (int) least};
	}

	/**
	 * Конвертирует {@link UUID} в байтовый массив длиной {@value #BYTE_ARRAY_SIZE} в порядке big-endian.
	 *
	 * @param uuid UUID для конвертации
	 * @return байтовый массив
	 */
	public static byte[] toByteArray(UUID uuid) {
		byte[] bytes = new byte[BYTE_ARRAY_SIZE];
		ByteBuffer
				.wrap(bytes)
				.order(ByteOrder.BIG_ENDIAN)
				.putLong(uuid.getMostSignificantBits())
				.putLong(uuid.getLeastSignificantBits());
		return bytes;
	}

	/**
	 * Читает UUID из {@link Dynamic} в формате int-массива длиной 4.
	 *
	 * @param dynamic динамическое значение
	 * @return UUID
	 * @throws IllegalArgumentException если массив имеет неверную длину
	 */
	public static UUID toUuid(Dynamic<?> dynamic) {
		int[] array = dynamic.asIntStream().toArray();

		if (array.length != INT_ARRAY_SIZE) {
			throw new IllegalArgumentException(
					"Could not read UUID. Expected int-array of length 4, got " + array.length + "."
			);
		}

		return toUuid(array);
	}

	public static UUID getOfflinePlayerUuid(String nickname) {
		return UUID.nameUUIDFromBytes((OFFLINE_PLAYER_UUID_PREFIX + nickname).getBytes(StandardCharsets.UTF_8));
	}

	public static GameProfile getOfflinePlayerProfile(String nickname) {
		return new GameProfile(getOfflinePlayerUuid(nickname), nickname);
	}
}

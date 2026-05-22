package net.minecraft.network.handler;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.encoding.VarInts;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Диспетчер пакетных кодеков: кодирует/декодирует пакеты по VarInt-идентификатору типа.
 * Строится через {@link Builder} и является иммутабельным после создания.
 */
public class PacketCodecDispatcher<B extends ByteBuf, V, T> implements PacketCodec<B, V> {

	private static final int UNKNOWN_PACKET_INDEX = -1;
	private final Function<V, ? extends T> packetIdGetter;
	private final List<PacketCodecDispatcher.PacketType<B, V, T>> packetTypes;
	private final Object2IntMap<T> typeToIndex;

	PacketCodecDispatcher(
			Function<V, ? extends T> packetIdGetter,
			List<PacketCodecDispatcher.PacketType<B, V, T>> packetTypes,
			Object2IntMap<T> typeToIndex
	) {
		this.packetIdGetter = packetIdGetter;
		this.packetTypes = packetTypes;
		this.typeToIndex = typeToIndex;
	}

	@Override
	public V decode(B buf) {
		int index = VarInts.read(buf);
		if (index < 0 || index >= packetTypes.size()) {
			throw new DecoderException("Received unknown packet id " + index);
		}

		PacketCodecDispatcher.PacketType<B, V, T> packetType = packetTypes.get(index);

		try {
			return (V) packetType.codec.decode(buf);
		} catch (Exception e) {
			if (e instanceof PacketCodecDispatcher.UndecoratedException) {
				throw e;
			}

			throw new DecoderException("Failed to decode packet '" + packetType.id + "'", e);
		}
	}

	@Override
	public void encode(B buf, V value) {
		T typeId = packetIdGetter.apply(value);
		int index = typeToIndex.getOrDefault(typeId, UNKNOWN_PACKET_INDEX);

		if (index == UNKNOWN_PACKET_INDEX) {
			throw new EncoderException("Sending unknown packet '" + typeId + "'");
		}

		VarInts.write(buf, index);
		PacketCodecDispatcher.PacketType<B, V, T> packetType = packetTypes.get(index);

		try {
			@SuppressWarnings("unchecked")
			PacketCodec<? super B, V> codec = (PacketCodec<? super B, V>) packetType.codec;
			codec.encode(buf, value);
		} catch (Exception e) {
			if (e instanceof PacketCodecDispatcher.UndecoratedException) {
				throw e;
			}

			throw new EncoderException("Failed to encode packet '" + typeId + "'", e);
		}
	}

	public static <B extends ByteBuf, V, T> PacketCodecDispatcher.Builder<B, V, T> builder(
			Function<V, ? extends T> packetIdGetter
	) {
		return new PacketCodecDispatcher.Builder<>(packetIdGetter);
	}

	public static class Builder<B extends ByteBuf, V, T> {

		private final List<PacketCodecDispatcher.PacketType<B, V, T>> packetTypes = new ArrayList<>();
		private final Function<V, ? extends T> packetIdGetter;

		Builder(Function<V, ? extends T> packetIdGetter) {
			this.packetIdGetter = packetIdGetter;
		}

		public PacketCodecDispatcher.Builder<B, V, T> add(T id, PacketCodec<? super B, ? extends V> codec) {
			packetTypes.add(new PacketCodecDispatcher.PacketType<>(codec, id));
			return this;
		}

		/**
		 * Собирает иммутабельный диспетчер. Выбрасывает {@link IllegalStateException},
		 * если один и тот же идентификатор типа зарегистрирован дважды.
		 */
		public PacketCodecDispatcher<B, V, T> build() {
			Object2IntOpenHashMap<T> indexMap = new Object2IntOpenHashMap<>();
			indexMap.defaultReturnValue(UNKNOWN_PACKET_INDEX - 1);

			for (PacketCodecDispatcher.PacketType<B, V, T> packetType : packetTypes) {
				int index = indexMap.size();
				int existing = indexMap.putIfAbsent(packetType.id, index);

				if (existing != UNKNOWN_PACKET_INDEX - 1) {
					throw new IllegalStateException("Duplicate registration for type " + packetType.id);
				}
			}

			return new PacketCodecDispatcher<>(packetIdGetter, List.copyOf(packetTypes), indexMap);
		}
	}

	record PacketType<B, V, T>(PacketCodec<? super B, ? extends V> codec, T id) {}

	/**
	 * Маркерный интерфейс для исключений, которые не нужно оборачивать дополнительным контекстом.
	 */
	interface UndecoratedException {}
}

package net.minecraft.world.debug;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Тип отладочной подписки — дескриптор канала передачи отладочных данных от сервера к клиенту.
 * <p>
 * Каждый экземпляр описывает один вид отладочной информации: кодек для сериализации,
 * а также время жизни данных (expiry). Нулевое значение expiry означает, что данные
 * хранятся бессрочно до явного обновления.
 *
 * @param <T> тип передаваемых отладочных данных
 */
public class DebugSubscriptionType<T> {

	/** Значение expiry по умолчанию — данные хранятся бессрочно. */
	public static final int DEFAULT_EXPIRY = 0;

	final @Nullable PacketCodec<? super RegistryByteBuf, T> packetCodec;
	private final int expiry;

	public DebugSubscriptionType(@Nullable PacketCodec<? super RegistryByteBuf, T> packetCodec, int expiry) {
		this.packetCodec = packetCodec;
		this.expiry = expiry;
	}

	public DebugSubscriptionType(@Nullable PacketCodec<? super RegistryByteBuf, T> packetCodec) {
		this(packetCodec, DEFAULT_EXPIRY);
	}

	/**
	 * Создаёт обёртку {@link OptionalValue} с возможно отсутствующим значением.
	 *
	 * @param value значение или {@code null}
	 * @return обёртка с {@link Optional#ofNullable(Object)}
	 */
	public OptionalValue<T> optionalValueFor(@Nullable T value) {
		return new OptionalValue<>(this, Optional.ofNullable(value));
	}

	/**
	 * Создаёт обёртку {@link OptionalValue} с отсутствующим значением.
	 *
	 * @return обёртка с {@link Optional#empty()}
	 */
	public OptionalValue<T> optionalValueFor() {
		return new OptionalValue<>(this, Optional.empty());
	}

	/**
	 * Создаёт обёртку {@link Value} с гарантированно присутствующим значением.
	 *
	 * @param value ненулевое значение
	 * @return обёртка с конкретным значением
	 */
	public Value<T> valueFor(T value) {
		return new Value<>(this, value);
	}

	public @Nullable PacketCodec<? super RegistryByteBuf, T> getPacketCodec() {
		return packetCodec;
	}

	public int getExpiry() {
		return expiry;
	}

	@Override
	public String toString() {
		return Util.registryValueToString(Registries.DEBUG_SUBSCRIPTION, this);
	}

	/**
	 * Обёртка значения подписки, допускающая отсутствие данных.
	 * <p>
	 * Используется в пакетах, где сервер может явно сигнализировать об удалении
	 * ранее отправленных данных, передавая {@link Optional#empty()}.
	 *
	 * @param <T> тип данных подписки
	 */
	public record OptionalValue<T>(DebugSubscriptionType<T> subscription, Optional<T> value) {

		public static final PacketCodec<RegistryByteBuf, OptionalValue<?>> PACKET_CODEC =
				PacketCodecs.registryValue(RegistryKeys.DEBUG_SUBSCRIPTION)
				            .dispatch(
						            OptionalValue::subscription,
						            OptionalValue::createPacketCodec
				            );

		/**
		 * Строит кодек для конкретного типа подписки через {@link PacketCodecs#optional}.
		 * Требует ненулевого кодека у типа — иначе бросает {@link NullPointerException}.
		 */
		private static <T> PacketCodec<? super RegistryByteBuf, OptionalValue<T>> createPacketCodec(
				DebugSubscriptionType<T> type
		) {
			return PacketCodecs.optional(Objects.requireNonNull(type.packetCodec))
			                   .xmap(
					                   value -> new OptionalValue<>(type, (Optional<T>) value),
					                   OptionalValue::value
			                   );
		}
	}

	/**
	 * Обёртка гарантированно присутствующего значения подписки.
	 *
	 * @param <T> тип данных подписки
	 */
	public record Value<T>(DebugSubscriptionType<T> subscription, T value) {

		public static final PacketCodec<RegistryByteBuf, Value<?>> PACKET_CODEC =
				PacketCodecs.registryValue(RegistryKeys.DEBUG_SUBSCRIPTION)
				            .dispatch(
						            Value::subscription,
						            Value::createPacketCodec
				            );

		/**
		 * Строит кодек для конкретного типа подписки через прямой xmap.
		 * Требует ненулевого кодека у типа — иначе бросает {@link NullPointerException}.
		 */
		private static <T> PacketCodec<? super RegistryByteBuf, Value<T>> createPacketCodec(
				DebugSubscriptionType<T> type
		) {
			return Objects.requireNonNull(type.packetCodec)
			              .xmap(
					              value -> new Value<>(type, (T) value),
					              Value::value
			              );
		}
	}
}

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
 * {@code DebugSubscriptionType}.
 */
public class DebugSubscriptionType<T> {

	public static final int DEFAULT_EXPIRY = 0;
	final @Nullable PacketCodec<? super RegistryByteBuf, T> packetCodec;
	private final int expiry;

	public DebugSubscriptionType(@Nullable PacketCodec<? super RegistryByteBuf, T> packetCodec, int expiry) {
		this.packetCodec = packetCodec;
		this.expiry = expiry;
	}

	public DebugSubscriptionType(@Nullable PacketCodec<? super RegistryByteBuf, T> packetCodec) {
		this(packetCodec, 0);
	}

	public DebugSubscriptionType.OptionalValue<T> optionalValueFor(@Nullable T value) {
		return new DebugSubscriptionType.OptionalValue<>(this, Optional.ofNullable(value));
	}

	public DebugSubscriptionType.OptionalValue<T> optionalValueFor() {
		return new DebugSubscriptionType.OptionalValue<>(this, Optional.empty());
	}

	public DebugSubscriptionType.Value<T> valueFor(T value) {
		return new DebugSubscriptionType.Value<>(this, value);
	}

	@Override
	public String toString() {
		return Util.registryValueToString(Registries.DEBUG_SUBSCRIPTION, this);
	}

	public @Nullable PacketCodec<? super RegistryByteBuf, T> getPacketCodec() {
		return this.packetCodec;
	}

	public int getExpiry() {
		return this.expiry;
	}

	/**
	 * {@code OptionalValue}.
	 */
	public record OptionalValue<T>(DebugSubscriptionType<T> subscription, Optional<T> value) {

		public static final PacketCodec<RegistryByteBuf, DebugSubscriptionType.OptionalValue<?>>
				PACKET_CODEC =
				PacketCodecs.registryValue(
						            RegistryKeys.DEBUG_SUBSCRIPTION
				            )
				            .dispatch(
						            DebugSubscriptionType.OptionalValue::subscription,
						            DebugSubscriptionType.OptionalValue::createPacketCodec
				            );

		private static <T> PacketCodec<? super RegistryByteBuf, DebugSubscriptionType.OptionalValue<T>> createPacketCodec(
				DebugSubscriptionType<T> type
		) {
			return PacketCodecs.optional(Objects.requireNonNull(type.packetCodec))
			                   .xmap(
					                   value -> new DebugSubscriptionType.OptionalValue<>(type, (Optional<T>) value),
					                   DebugSubscriptionType.OptionalValue::value
			                   );
		}
	}

	/**
	 * {@code Value}.
	 */
	public record Value<T>(DebugSubscriptionType<T> subscription, T value) {

		public static final PacketCodec<RegistryByteBuf, DebugSubscriptionType.Value<?>>
				PACKET_CODEC =
				PacketCodecs.registryValue(
						            RegistryKeys.DEBUG_SUBSCRIPTION
				            )
				            .dispatch(
						            DebugSubscriptionType.Value::subscription,
						            DebugSubscriptionType.Value::createPacketCodec
				            );

		private static <T> PacketCodec<? super RegistryByteBuf, DebugSubscriptionType.Value<T>> createPacketCodec(
				DebugSubscriptionType<T> type
		) {
			return Objects
					.requireNonNull(type.packetCodec)
					.xmap(
							value -> new DebugSubscriptionType.Value<>(type, (T) value),
							DebugSubscriptionType.Value::value
					);
		}
	}
}

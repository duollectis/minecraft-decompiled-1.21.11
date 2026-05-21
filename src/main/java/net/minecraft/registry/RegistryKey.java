package net.minecraft.registry;

import com.google.common.collect.MapMaker;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

/**
 * {@code RegistryKey}.
 */
public class RegistryKey<T> {

	private static final ConcurrentMap<RegistryKey.RegistryIdPair, RegistryKey<?>>
			INSTANCES =
			new MapMaker().weakValues().makeMap();
	private final Identifier registry;
	private final Identifier value;

	public static <T> Codec<RegistryKey<T>> createCodec(RegistryKey<? extends Registry<T>> registry) {
		return Identifier.CODEC.xmap(id -> of(registry, id), RegistryKey::getValue);
	}

	public static <T> PacketCodec<ByteBuf, RegistryKey<T>> createPacketCodec(RegistryKey<? extends Registry<T>> registry) {
		return Identifier.PACKET_CODEC.xmap(id -> of(registry, id), RegistryKey::getValue);
	}

	public static <T> RegistryKey<T> of(RegistryKey<? extends Registry<T>> registry, Identifier value) {
		return of(registry.value, value);
	}

	public static <T> RegistryKey<Registry<T>> ofRegistry(Identifier registry) {
		return of(RegistryKeys.ROOT, registry);
	}

	private static <T> RegistryKey<T> of(Identifier registry, Identifier value) {
		return (RegistryKey<T>) INSTANCES.computeIfAbsent(
				new RegistryKey.RegistryIdPair(registry, value),
				pair -> new RegistryKey(pair.registry, pair.id)
		);
	}

	private RegistryKey(Identifier registry, Identifier value) {
		this.registry = registry;
		this.value = value;
	}

	@Override
	public String toString() {
		return "ResourceKey[" + this.registry + " / " + this.value + "]";
	}

	public boolean isOf(RegistryKey<? extends Registry<?>> registry) {
		return this.registry.equals(registry.getValue());
	}

	public <E> Optional<RegistryKey<E>> tryCast(RegistryKey<? extends Registry<E>> registryRef) {
		return this.isOf(registryRef) ? Optional.of((RegistryKey<E>) this) : Optional.empty();
	}

	public Identifier getValue() {
		return this.value;
	}

	public Identifier getRegistry() {
		return this.registry;
	}

	public RegistryKey<Registry<T>> getRegistryRef() {
		return ofRegistry(this.registry);
	}

	/**
	 * {@code RegistryIdPair}.
	 */
	record RegistryIdPair(Identifier registry, Identifier id) {
	}
}

package net.minecraft.registry;

import com.google.common.collect.MapMaker;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

/**
 * Типизированный ключ реестра, однозначно идентифицирующий элемент внутри конкретного реестра.
 * Состоит из двух {@link Identifier}: идентификатора самого реестра и идентификатора значения.
 *
 * <p>Экземпляры интернируются через слабые ссылки — один и тот же ключ всегда возвращает
 * один и тот же объект, что позволяет использовать сравнение по ссылке ({@code ==}).
 *
 * @param <T> тип элемента реестра
 */
public class RegistryKey<T> {

	private static final ConcurrentMap<RegistryKey.RegistryIdPair, RegistryKey<?>> INSTANCES =
			new MapMaker().weakValues().makeMap();

	private final Identifier registry;
	private final Identifier value;

	/**
	 * Создаёт codec для сериализации ключей конкретного реестра.
	 * Кодирует только {@link #value}, так как реестр фиксирован.
	 *
	 * @param registry ключ реестра, к которому принадлежат сериализуемые ключи
	 */
	public static <T> Codec<RegistryKey<T>> createCodec(RegistryKey<? extends Registry<T>> registry) {
		return Identifier.CODEC.xmap(id -> of(registry, id), RegistryKey::getValue);
	}

	/**
	 * Создаёт packet codec для сетевой передачи ключей конкретного реестра.
	 *
	 * @param registry ключ реестра, к которому принадлежат передаваемые ключи
	 */
	public static <T> PacketCodec<ByteBuf, RegistryKey<T>> createPacketCodec(
			RegistryKey<? extends Registry<T>> registry
	) {
		return Identifier.PACKET_CODEC.xmap(id -> of(registry, id), RegistryKey::getValue);
	}

	/**
	 * Возвращает интернированный ключ для элемента {@code value} в реестре {@code registry}.
	 *
	 * @param registry ключ реестра-владельца
	 * @param value    идентификатор элемента
	 */
	public static <T> RegistryKey<T> of(RegistryKey<? extends Registry<T>> registry, Identifier value) {
		return of(registry.value, value);
	}

	/**
	 * Возвращает интернированный ключ для самого реестра с идентификатором {@code registry}.
	 * Все ключи реестров принадлежат корневому реестру {@link RegistryKeys#ROOT}.
	 *
	 * @param registry идентификатор реестра
	 */
	public static <T> RegistryKey<Registry<T>> ofRegistry(Identifier registry) {
		return of(RegistryKeys.ROOT, registry);
	}

	@SuppressWarnings("unchecked")
	private static <T> RegistryKey<T> of(Identifier registry, Identifier value) {
		return (RegistryKey<T>) INSTANCES.computeIfAbsent(
				new RegistryKey.RegistryIdPair(registry, value),
				pair -> new RegistryKey<>(pair.registry(), pair.id())
		);
	}

	private RegistryKey(Identifier registry, Identifier value) {
		this.registry = registry;
		this.value = value;
	}

	@Override
	public String toString() {
		return "ResourceKey[" + registry + " / " + value + "]";
	}

	/**
	 * Проверяет, принадлежит ли этот ключ указанному реестру.
	 *
	 * @param registryRef ключ реестра для проверки
	 * @return {@code true}, если идентификатор реестра совпадает
	 */
	public boolean isOf(RegistryKey<? extends Registry<?>> registryRef) {
		return registry.equals(registryRef.getValue());
	}

	/**
	 * Пытается привести этот ключ к ключу другого реестра.
	 * Возвращает {@link Optional#empty()}, если ключ принадлежит другому реестру.
	 *
	 * @param registryRef целевой реестр
	 */
	public <E> Optional<RegistryKey<E>> tryCast(RegistryKey<? extends Registry<E>> registryRef) {
		return isOf(registryRef) ? Optional.of((RegistryKey<E>) this) : Optional.empty();
	}

	public Identifier getValue() {
		return value;
	}

	public Identifier getRegistry() {
		return registry;
	}

	public RegistryKey<Registry<T>> getRegistryRef() {
		return ofRegistry(registry);
	}

	record RegistryIdPair(Identifier registry, Identifier id) {
	}
}

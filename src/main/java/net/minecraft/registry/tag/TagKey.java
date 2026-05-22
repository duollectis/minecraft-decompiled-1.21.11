package net.minecraft.registry.tag;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.tag.FabricTagKey;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Типизированный ключ тега реестра. Интернируется через {@link Interner} для экономии памяти —
 * два {@link TagKey} с одинаковыми {@code registryRef} и {@code id} гарантированно
 * являются одним и тем же объектом после вызова {@link #of}.
 *
 * @param <T>         тип объектов реестра, которому принадлежит тег
 * @param registryRef ключ реестра, к которому относится тег
 * @param id          идентификатор тега
 */
public record TagKey<T>(RegistryKey<? extends Registry<T>> registryRef, Identifier id) implements FabricTagKey {

	private static final Interner<TagKey<?>> INTERNER = Interners.newWeakInterner();

	@Deprecated
	public TagKey(RegistryKey<? extends Registry<T>> registryRef, Identifier id) {
		this.registryRef = registryRef;
		this.id = id;
	}

	/**
	 * Создаёт кодек для {@link TagKey} без префикса {@code #}.
	 * Используется там, где тег уже известен как тег и префикс избыточен.
	 *
	 * @param registryRef ключ реестра для привязки тега
	 * @return кодек, сериализующий тег как {@link Identifier}
	 */
	public static <T> Codec<TagKey<T>> unprefixedCodec(RegistryKey<? extends Registry<T>> registryRef) {
		return Identifier.CODEC.xmap(id -> of(registryRef, id), TagKey::id);
	}

	/**
	 * Создаёт кодек для {@link TagKey} с обязательным префиксом {@code #}.
	 * Используется в JSON-файлах, где теги отличаются от прямых ссылок префиксом.
	 *
	 * @param registryRef ключ реестра для привязки тега
	 * @return кодек, сериализующий тег как строку вида {@code "#namespace:id"}
	 */
	public static <T> Codec<TagKey<T>> codec(RegistryKey<? extends Registry<T>> registryRef) {
		return Codec.STRING
				.comapFlatMap(
						string -> string.startsWith("#")
								? Identifier.validate(string.substring(1)).map(id -> of(registryRef, id))
								: DataResult.error(() -> "Not a tag id"),
						string -> "#" + string.id
				);
	}

	public static <T> PacketCodec<ByteBuf, TagKey<T>> packetCodec(RegistryKey<? extends Registry<T>> registryRef) {
		return Identifier.PACKET_CODEC.xmap(id -> of(registryRef, id), TagKey::id);
	}

	/**
	 * Создаёт или возвращает интернированный {@link TagKey} для заданного реестра и идентификатора.
	 *
	 * @param registryRef ключ реестра
	 * @param id          идентификатор тега
	 * @return интернированный экземпляр {@link TagKey}
	 */
	public static <T> TagKey<T> of(RegistryKey<? extends Registry<T>> registryRef, Identifier id) {
		return (TagKey<T>) INTERNER.intern(new TagKey<>(registryRef, id));
	}

	public boolean isOf(RegistryKey<? extends Registry<?>> registryRef) {
		return this.registryRef == registryRef;
	}

	public <E> Optional<TagKey<E>> tryCast(RegistryKey<? extends Registry<E>> registryRef) {
		return isOf(registryRef) ? Optional.of((TagKey<E>) this) : Optional.empty();
	}

	@Override
	public String toString() {
		return "TagKey[" + registryRef.getValue() + " / " + id + "]";
	}
}

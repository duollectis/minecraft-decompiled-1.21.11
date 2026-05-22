package net.minecraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
	 * Тип компонента данных предмета. Определяет кодек сериализации и сетевой кодек
	 * для конкретного типа данных, прикрепляемых к стеку предметов.
	 *
	 * @param <T> тип значения компонента
	 */
public interface ComponentType<T> {

	/**
		 * Кодек для сериализации ссылки на тип компонента из реестра.
		 */
	Codec<ComponentType<?>> CODEC = Codec.lazyInitialized(() -> Registries.DATA_COMPONENT_TYPE.getCodec());

	/**
		 * Сетевой кодек для передачи типа компонента по сети.
		 */
	PacketCodec<RegistryByteBuf, ComponentType<?>> PACKET_CODEC = PacketCodec.recursive(
			packetCodec -> PacketCodecs.registryValue(RegistryKeys.DATA_COMPONENT_TYPE)
	);

	/**
		 * Кодек, отклоняющий транзитные (не сериализуемые) типы компонентов.
		 */
	Codec<ComponentType<?>> PERSISTENT_CODEC = CODEC.validate(
			componentType -> componentType.shouldSkipSerialization()
								? DataResult.error(() -> "Encountered transient component "
														+ Registries.DATA_COMPONENT_TYPE.getId(componentType))
								: DataResult.success(componentType)
	);

	/**
		 * Кодек для карты «тип компонента → значение», используемой в NBT.
		 */
	Codec<Map<ComponentType<?>, Object>>
			TYPE_TO_VALUE_MAP_CODEC =
			Codec.dispatchedMap(PERSISTENT_CODEC, ComponentType::getCodecOrThrow);

	/**
		 * Создаёт новый строитель типа компонента.
		 *
		 * @param <T> тип значения компонента
		 * @return новый экземпляр {@link Builder}
		 */
	static <T> ComponentType.Builder<T> builder() {
		return new ComponentType.Builder<>();
	}

	/**
		 * @return кодек сериализации значения компонента, или {@code null} для транзитных компонентов
		 */
	@Nullable Codec<T> getCodec();

	/**
		 * Возвращает кодек сериализации или выбрасывает исключение, если компонент транзитный.
		 *
		 * @return кодек сериализации
		 * @throws IllegalStateException если компонент не является персистентным
		 */
	default Codec<T> getCodecOrThrow() {
		Codec<T> resolved = this.getCodec();

		if (resolved == null) {
			throw new IllegalStateException(this + " is not a persistent component");
		}

		return resolved;
	}

	/**
		 * @return {@code true}, если компонент транзитный и не должен сериализоваться
		 */
	default boolean shouldSkipSerialization() {
		return this.getCodec() == null;
	}

	/**
		 * @return {@code true}, если компонент пропускает анимацию руки при использовании предмета
		 */
	boolean skipsHandAnimation();

	/**
		 * @return сетевой кодек для передачи значения компонента по сети
		 */
	PacketCodec<? super RegistryByteBuf, T> getPacketCodec();

	/**
		 * Строитель для создания экземпляров {@link ComponentType}.
		 *
		 * @param <T> тип значения компонента
		 */
	static class Builder<T> {

		private @Nullable Codec<T> codec;
		private @Nullable PacketCodec<? super RegistryByteBuf, T> packetCodec;
		private boolean cache;
		private boolean skipsHandAnimation;

		/**
			 * Устанавливает кодек сериализации для компонента.
			 *
			 * @param codec кодек сериализации
			 * @return этот строитель
			 */
		public ComponentType.Builder<T> codec(Codec<T> codec) {
			this.codec = codec;
			return this;
		}

		/**
			 * Устанавливает сетевой кодек для компонента.
			 *
			 * @param packetCodec сетевой кодек
			 * @return этот строитель
			 */
		public ComponentType.Builder<T> packetCodec(PacketCodec<? super RegistryByteBuf, T> packetCodec) {
			this.packetCodec = packetCodec;
			return this;
		}

		/**
			 * Включает кэширование хэша для значений компонента.
			 *
			 * @return этот строитель
			 */
		public ComponentType.Builder<T> cache() {
			this.cache = true;
			return this;
		}

		/**
			 * Собирает и возвращает готовый {@link ComponentType}.
			 *
			 * @return новый тип компонента
			 */
		public ComponentType<T> build() {
			PacketCodec<? super RegistryByteBuf, T> resolvedPacketCodec = Objects.requireNonNullElseGet(
					this.packetCodec,
					() -> PacketCodecs.registryCodec(Objects.requireNonNull(this.codec, "Missing Codec for component"))
			);
			Codec<T> resolvedCodec = this.cache && this.codec != null
										? DataComponentTypes.CACHE.wrap(this.codec)
										: this.codec;

			return new ComponentType.Builder.SimpleDataComponentType<>(
					resolvedCodec,
					resolvedPacketCodec,
					this.skipsHandAnimation
			);
		}

		/**
			 * Помечает компонент как пропускающий анимацию руки.
			 *
			 * @return этот строитель
			 */
		public ComponentType.Builder<T> skipsHandAnimation() {
			this.skipsHandAnimation = true;
			return this;
		}

		/**
			 * Стандартная реализация {@link ComponentType}, хранящая кодеки и флаги.
			 *
			 * @param <T> тип значения компонента
			 */
		static class SimpleDataComponentType<T> implements ComponentType<T> {

			private final @Nullable Codec<T> codec;
			private final PacketCodec<? super RegistryByteBuf, T> packetCodec;
			private final boolean skipsHandAnimation;

			SimpleDataComponentType(
					@Nullable Codec<T> codec,
					PacketCodec<? super RegistryByteBuf, T> packetCodec,
					boolean skipsHandAnimation
			) {
				this.codec = codec;
				this.packetCodec = packetCodec;
				this.skipsHandAnimation = skipsHandAnimation;
			}

			@Override
			public boolean skipsHandAnimation() {
				return this.skipsHandAnimation;
			}

			@Override
			public @Nullable Codec<T> getCodec() {
				return this.codec;
			}

			@Override
			public PacketCodec<? super RegistryByteBuf, T> getPacketCodec() {
				return this.packetCodec;
			}

			@Override
			public String toString() {
				return Util.registryValueToString(Registries.DATA_COMPONENT_TYPE, this);
			}
		}
	}
}

package net.minecraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.Map.Entry;

/**
	 * Пара «тип компонента → значение», представляющая один компонент данных предмета.
	 * Используется при сериализации и передаче компонентов по сети.
	 *
	 * @param <T> тип значения компонента
	 */
public record Component<T>(ComponentType<T> type, T value) {

	/**
		 * Сетевой кодек для передачи компонента произвольного типа.
		 * Использует wildcard-вспомогательные методы для обхода ограничений generics.
		 */
	public static final PacketCodec<RegistryByteBuf, Component<?>> PACKET_CODEC =
			new PacketCodec<>() {
				@Override
				public Component<?> decode(RegistryByteBuf buf) {
					ComponentType<?> componentType = ComponentType.PACKET_CODEC.decode(buf);
					return readWildcard(buf, componentType);
				}

				@SuppressWarnings("unchecked")
				private <X> Component<X> readWildcard(RegistryByteBuf buf, ComponentType<?> type) {
					return read(buf, (ComponentType<X>) type);
				}

				private static <T> Component<T> read(RegistryByteBuf buf, ComponentType<T> type) {
					return new Component<>(type, type.getPacketCodec().decode(buf));
				}

				@Override
				public void encode(RegistryByteBuf buf, Component<?> component) {
					writeWildcard(buf, component);
				}

				@SuppressWarnings("unchecked")
				private <X> void writeWildcard(RegistryByteBuf buf, Component<?> component) {
					write(buf, (Component<X>) component);
				}

				private static <T> void write(RegistryByteBuf buf, Component<T> component) {
					ComponentType.PACKET_CODEC.encode(buf, component.type());
					component.type().getPacketCodec().encode(buf, component.value());
				}
			};

	static Component<?> of(Entry<ComponentType<?>, Object> entry) {
		return of(entry.getKey(), entry.getValue());
	}

	@SuppressWarnings("unchecked")
	public static <T> Component<T> of(ComponentType<T> type, Object value) {
		return new Component<>(type, (T) value);
	}

	public void apply(MergedComponentMap components) {
		components.set(type, value);
	}

	/**
		 * Кодирует значение компонента в формат {@code D} с помощью кодека типа.
		 *
		 * @param ops операции над целевым форматом
		 * @return результат кодирования, или ошибка если компонент транзитный
		 */
	public <D> DataResult<D> encode(DynamicOps<D> ops) {
		Codec<T> codec = type.getCodec();
		return codec == null
				? DataResult.error(() -> "Component of type " + type + " is not encodable")
				: codec.encodeStart(ops, value);
	}

	@Override
	public String toString() {
		return type + "=>" + value;
	}
}

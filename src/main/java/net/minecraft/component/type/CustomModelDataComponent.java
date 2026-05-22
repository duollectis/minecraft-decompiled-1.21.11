package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
	 * Компонент произвольных данных модели предмета. Позволяет ресурспакам и моддерам
	 * задавать кастомные числа, флаги, строки и цвета для выбора вариантов модели
	 * через предикаты {@code custom_model_data} в JSON-файлах моделей.
	 */
public record CustomModelDataComponent(
		List<Float> floats,
		List<Boolean> flags,
		List<String> strings,
		List<Integer> colors
) {

	public static final CustomModelDataComponent
			DEFAULT =
			new CustomModelDataComponent(List.of(), List.of(), List.of(), List.of());
	public static final Codec<CustomModelDataComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
										Codec.FLOAT
												.listOf()
												.optionalFieldOf("floats", List.of())
												.forGetter(CustomModelDataComponent::floats),
										Codec.BOOL.listOf().optionalFieldOf("flags", List.of()).forGetter(CustomModelDataComponent::flags),
										Codec.STRING
												.listOf()
												.optionalFieldOf("strings", List.of())
												.forGetter(CustomModelDataComponent::strings),
										Codecs.RGB.listOf().optionalFieldOf("colors", List.of()).forGetter(CustomModelDataComponent::colors)
								)
								.apply(instance, CustomModelDataComponent::new)
	);
	public static final PacketCodec<ByteBuf, CustomModelDataComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.FLOAT.collect(PacketCodecs.toList()),
			CustomModelDataComponent::floats,
			PacketCodecs.BOOLEAN.collect(PacketCodecs.toList()),
			CustomModelDataComponent::flags,
			PacketCodecs.STRING.collect(PacketCodecs.toList()),
			CustomModelDataComponent::strings,
			PacketCodecs.INTEGER.collect(PacketCodecs.toList()),
			CustomModelDataComponent::colors,
			CustomModelDataComponent::new
	);

	private static <T> @Nullable T getValue(List<T> values, int index) {
		return index >= 0 && index < values.size() ? values.get(index) : null;
	}

	/**
		 * Возвращает числовое значение с плавающей точкой по индексу,
		 * или {@code null}, если индекс выходит за пределы списка.
		 */
	public @Nullable Float getFloat(int index) {
		return getValue(floats, index);
	}

	/**
		 * Возвращает булев флаг по индексу,
		 * или {@code null}, если индекс выходит за пределы списка.
		 */
	public @Nullable Boolean getFlag(int index) {
		return getValue(flags, index);
	}

	/**
		 * Возвращает строковое значение по индексу,
		 * или {@code null}, если индекс выходит за пределы списка.
		 */
	public @Nullable String getString(int index) {
		return getValue(strings, index);
	}

	/**
		 * Возвращает цвет в формате RGB по индексу,
		 * или {@code null}, если индекс выходит за пределы списка.
		 */
	public @Nullable Integer getColor(int index) {
		return getValue(colors, index);
	}
}

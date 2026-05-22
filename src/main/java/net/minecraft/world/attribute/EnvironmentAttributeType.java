package net.minecraft.world.attribute;

import com.google.common.collect.ImmutableBiMap;
import com.mojang.serialization.Codec;
import net.minecraft.registry.Registries;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.Interpolator;

import java.util.Map;

/**
 * Описывает тип атрибута окружения: codec значений, набор допустимых модификаторов
 * и интерполяторы для различных контекстов (ключевые кадры, смена состояния,
 * пространственная интерполяция, частичный тик).
 *
 * @param <Value> тип значения атрибута
 */
public record EnvironmentAttributeType<Value>(
	Codec<Value> valueCodec,
	Map<EnvironmentAttributeModifier.Type, EnvironmentAttributeModifier<Value, ?>> modifierLibrary,
	Codec<EnvironmentAttributeModifier<Value, ?>> modifierCodec,
	Interpolator<Value> keyframeLerp,
	Interpolator<Value> stateChangeLerp,
	Interpolator<Value> spatialLerp,
	Interpolator<Value> partialTickLerp
) {

	/**
	 * Создаёт интерполируемый тип с одним интерполятором для всех контекстов.
	 *
	 * @param valueCodec codec значений
	 * @param modifierLibrary библиотека модификаторов
	 * @param lerp интерполятор для всех контекстов
	 */
	public static <Value> EnvironmentAttributeType<Value> interpolated(
		Codec<Value> valueCodec,
		Map<EnvironmentAttributeModifier.Type, EnvironmentAttributeModifier<Value, ?>> modifierLibrary,
		Interpolator<Value> lerp
	) {
		return interpolated(valueCodec, modifierLibrary, lerp, lerp);
	}

	/**
	 * Создаёт интерполируемый тип с раздельными интерполяторами для пространства и частичного тика.
	 *
	 * @param valueCodec codec значений
	 * @param modifierLibrary библиотека модификаторов
	 * @param spatialLerp интерполятор для пространственного смешивания биомов
	 * @param partialTickLerp интерполятор для сглаживания между тиками
	 */
	public static <Value> EnvironmentAttributeType<Value> interpolated(
		Codec<Value> valueCodec,
		Map<EnvironmentAttributeModifier.Type, EnvironmentAttributeModifier<Value, ?>> modifierLibrary,
		Interpolator<Value> spatialLerp,
		Interpolator<Value> partialTickLerp
	) {
		return new EnvironmentAttributeType<>(
			valueCodec,
			modifierLibrary,
			createModifierCodec(modifierLibrary),
			spatialLerp,
			spatialLerp,
			spatialLerp,
			partialTickLerp
		);
	}

	/**
	 * Создаёт дискретный тип с библиотекой модификаторов.
	 * Использует пороговые интерполяторы вместо плавных.
	 *
	 * @param valueCodec codec значений
	 * @param modifierLibrary библиотека модификаторов
	 */
	public static <Value> EnvironmentAttributeType<Value> discrete(
		Codec<Value> valueCodec,
		Map<EnvironmentAttributeModifier.Type, EnvironmentAttributeModifier<Value, ?>> modifierLibrary
	) {
		return new EnvironmentAttributeType<>(
			valueCodec,
			modifierLibrary,
			createModifierCodec(modifierLibrary),
			Interpolator.threshold(1.0F),
			Interpolator.threshold(0.0F),
			Interpolator.threshold(0.5F),
			Interpolator.threshold(0.0F)
		);
	}

	/**
	 * Создаёт дискретный тип без модификаторов (только override).
	 *
	 * @param valueCodec codec значений
	 */
	public static <Value> EnvironmentAttributeType<Value> discrete(Codec<Value> valueCodec) {
		return discrete(valueCodec, Map.of());
	}

	private static <Value> Codec<EnvironmentAttributeModifier<Value, ?>> createModifierCodec(
		Map<EnvironmentAttributeModifier.Type, EnvironmentAttributeModifier<Value, ?>> modifierLibrary
	) {
		ImmutableBiMap<EnvironmentAttributeModifier.Type, EnvironmentAttributeModifier<Value, ?>> biMap =
			ImmutableBiMap.<EnvironmentAttributeModifier.Type, EnvironmentAttributeModifier<Value, ?>>builder()
				.put(EnvironmentAttributeModifier.Type.OVERRIDE, EnvironmentAttributeModifier.override())
				.putAll(modifierLibrary)
				.buildOrThrow();

		return Codecs.idChecked(
			EnvironmentAttributeModifier.Type.CODEC,
			biMap::get,
			biMap.inverse()::get
		);
	}

	/**
	 * Проверяет, что модификатор допустим для данного типа атрибута.
	 * Override-модификатор всегда допустим.
	 *
	 * @param modifier проверяемый модификатор
	 * @throws IllegalArgumentException если модификатор не входит в библиотеку типа
	 */
	public void validate(EnvironmentAttributeModifier<Value, ?> modifier) {
		if (modifier != EnvironmentAttributeModifier.override() && !modifierLibrary.containsValue(modifier)) {
			throw new IllegalArgumentException("Modifier " + modifier + " is not valid for " + this);
		}
	}

	@Override
	public String toString() {
		return Util.registryValueToString(Registries.ATTRIBUTE_TYPE, this);
	}
}

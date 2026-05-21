package net.minecraft.world.attribute;

import com.google.common.collect.ImmutableBiMap;
import com.mojang.serialization.Codec;
import net.minecraft.registry.Registries;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.Interpolator;

import java.util.Map;

/**
 * {@code EnvironmentAttributeType}.
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

	public static <Value> EnvironmentAttributeType<Value> interpolated(
			Codec<Value> valueCodec,
			Map<EnvironmentAttributeModifier.Type, EnvironmentAttributeModifier<Value, ?>> modifierLibrary,
			Interpolator<Value> lerp
	) {
		return interpolated(valueCodec, modifierLibrary, lerp, lerp);
	}

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
	 * Discrete.
	 *
	 * @param valueCodec value codec
	 *
	 * @return EnvironmentAttributeType — результат операции
	 */
	public static <Value> EnvironmentAttributeType<Value> discrete(Codec<Value> valueCodec) {
		return discrete(valueCodec, Map.of());
	}

	private static <Value> Codec<EnvironmentAttributeModifier<Value, ?>> createModifierCodec(
			Map<EnvironmentAttributeModifier.Type, EnvironmentAttributeModifier<Value, ?>> modifierLibrary
	) {
		ImmutableBiMap<EnvironmentAttributeModifier.Type, EnvironmentAttributeModifier<Value, ?>>
				immutableBiMap =
				ImmutableBiMap
						.<EnvironmentAttributeModifier.Type, EnvironmentAttributeModifier<Value, ?>>builder()
						.put(EnvironmentAttributeModifier.Type.OVERRIDE, EnvironmentAttributeModifier.override())
						.putAll(modifierLibrary)
						.buildOrThrow();
		return Codecs.idChecked(
				EnvironmentAttributeModifier.Type.CODEC,
				immutableBiMap::get,
				immutableBiMap.inverse()::get
		);
	}

	/**
	 * Validate.
	 *
	 * @param modifier modifier
	 */
	public void validate(EnvironmentAttributeModifier<Value, ?> modifier) {
		if (modifier != EnvironmentAttributeModifier.override() && !this.modifierLibrary.containsValue(modifier)) {
			throw new IllegalArgumentException("Modifier " + modifier + " is not valid for " + this);
		}
	}

	@Override
	public String toString() {
		return Util.registryValueToString(Registries.ATTRIBUTE_TYPE, this);
	}
}

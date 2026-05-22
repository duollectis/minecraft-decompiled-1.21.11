package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.Interpolator;
import net.minecraft.world.MoonPhase;

import java.util.List;

/**
 * Реестр всех типов атрибутов окружения.
 * Каждый тип определяет codec значений, набор допустимых модификаторов
 * и стратегию интерполяции.
 */
public interface EnvironmentAttributeTypes {

	EnvironmentAttributeType<Boolean> BOOLEAN = register(
		"boolean",
		EnvironmentAttributeType.discrete(Codec.BOOL, EnvironmentAttributeModifier.BOOLEAN_MODIFIERS)
	);

	EnvironmentAttributeType<TriState> TRI_STATE = register(
		"tri_state",
		EnvironmentAttributeType.discrete(TriState.CODEC)
	);

	EnvironmentAttributeType<Float> FLOAT = register(
		"float",
		EnvironmentAttributeType.interpolated(
			Codec.FLOAT,
			EnvironmentAttributeModifier.FLOAT_MODIFIERS,
			Interpolator.ofFloat()
		)
	);

	/** Угол в градусах с угловой интерполяцией (90° порог для частичного тика). */
	EnvironmentAttributeType<Float> ANGLE_DEGREES = register(
		"angle_degrees",
		EnvironmentAttributeType.interpolated(
			Codec.FLOAT,
			EnvironmentAttributeModifier.FLOAT_MODIFIERS,
			Interpolator.ofFloat(),
			Interpolator.angle(90.0F)
		)
	);

	EnvironmentAttributeType<Integer> RGB_COLOR = register(
		"rgb_color",
		EnvironmentAttributeType.interpolated(
			Codecs.HEX_RGB,
			EnvironmentAttributeModifier.RGB,
			Interpolator.ofColor()
		)
	);

	EnvironmentAttributeType<Integer> ARGB_COLOR = register(
		"argb_color",
		EnvironmentAttributeType.interpolated(
			Codecs.HEX_ARGB,
			EnvironmentAttributeModifier.ARGB,
			Interpolator.ofColor()
		)
	);

	EnvironmentAttributeType<MoonPhase> MOON_PHASE = register(
		"moon_phase",
		EnvironmentAttributeType.discrete(MoonPhase.CODEC)
	);

	EnvironmentAttributeType<Activity> ACTIVITY = register(
		"activity",
		EnvironmentAttributeType.discrete(Registries.ACTIVITY.getCodec())
	);

	EnvironmentAttributeType<BedRule> BED_RULE = register(
		"bed_rule",
		EnvironmentAttributeType.discrete(BedRule.CODEC)
	);

	EnvironmentAttributeType<ParticleEffect> PARTICLE = register(
		"particle",
		EnvironmentAttributeType.discrete(ParticleTypes.TYPE_CODEC)
	);

	EnvironmentAttributeType<List<AmbientParticle>> AMBIENT_PARTICLES = register(
		"ambient_particles",
		EnvironmentAttributeType.discrete(AmbientParticle.CODEC.listOf())
	);

	EnvironmentAttributeType<BackgroundMusic> BACKGROUND_MUSIC = register(
		"background_music",
		EnvironmentAttributeType.discrete(BackgroundMusic.CODEC)
	);

	EnvironmentAttributeType<AmbientSounds> AMBIENT_SOUNDS = register(
		"ambient_sounds",
		EnvironmentAttributeType.discrete(AmbientSounds.CODEC)
	);

	Codec<EnvironmentAttributeType<?>> CODEC = Registries.ATTRIBUTE_TYPE.getCodec();

	/** Вызывается при инициализации реестра для гарантии загрузки всех типов. */
	static EnvironmentAttributeType<?> registerAndGetDefault(Registry<EnvironmentAttributeType<?>> registry) {
		return BOOLEAN;
	}

	private static <Value> EnvironmentAttributeType<Value> register(String path, EnvironmentAttributeType<Value> type) {
		Registry.register(Registries.ATTRIBUTE_TYPE, Identifier.ofVanilla(path), type);
		return type;
	}
}

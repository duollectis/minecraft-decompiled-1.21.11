package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.world.MoonPhase;

import java.util.List;

/**
 * Реестр всех атрибутов окружения игры.
 * Атрибуты разбиты на три категории:
 * <ul>
 *   <li>{@code visual/*} — визуальные параметры (цвета, туман, небо)</li>
 *   <li>{@code audio/*} — звуковые параметры (музыка, фоновые звуки)</li>
 *   <li>{@code gameplay/*} — игровые параметры (правила мира, поведение мобов)</li>
 * </ul>
 */
public interface EnvironmentAttributes {

	// ─── Визуальные атрибуты ────────────────────────────────────────────────

	EnvironmentAttribute<Integer> FOG_COLOR_VISUAL = register(
		"visual/fog_color",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.RGB_COLOR).defaultValue(0).interpolated().synced()
	);

	EnvironmentAttribute<Float> FOG_START_DISTANCE_VISUAL = register(
		"visual/fog_start_distance",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.FLOAT).defaultValue(0.0F).interpolated().synced()
	);

	EnvironmentAttribute<Float> FOG_END_DISTANCE_VISUAL = register(
		"visual/fog_end_distance",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.FLOAT)
			.defaultValue(1024.0F)
			.validator(AttributeValidator.NON_NEGATIVE_FLOAT)
			.interpolated()
			.synced()
	);

	EnvironmentAttribute<Float> SKY_FOG_END_DISTANCE_VISUAL = register(
		"visual/sky_fog_end_distance",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.FLOAT)
			.defaultValue(512.0F)
			.validator(AttributeValidator.NON_NEGATIVE_FLOAT)
			.interpolated()
			.synced()
	);

	EnvironmentAttribute<Float> CLOUD_FOG_END_DISTANCE_VISUAL = register(
		"visual/cloud_fog_end_distance",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.FLOAT)
			.defaultValue(2048.0F)
			.validator(AttributeValidator.NON_NEGATIVE_FLOAT)
			.interpolated()
			.synced()
	);

	EnvironmentAttribute<Integer> WATER_FOG_COLOR_VISUAL = register(
		"visual/water_fog_color",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.RGB_COLOR)
			.defaultValue(-16448205)
			.interpolated()
			.synced()
	);

	EnvironmentAttribute<Float> WATER_FOG_START_DISTANCE_VISUAL = register(
		"visual/water_fog_start_distance",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.FLOAT).defaultValue(-8.0F).interpolated().synced()
	);

	EnvironmentAttribute<Float> WATER_FOG_END_DISTANCE_VISUAL = register(
		"visual/water_fog_end_distance",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.FLOAT)
			.defaultValue(96.0F)
			.validator(AttributeValidator.NON_NEGATIVE_FLOAT)
			.interpolated()
			.synced()
	);

	EnvironmentAttribute<Integer> SKY_COLOR_VISUAL = register(
		"visual/sky_color",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.RGB_COLOR).defaultValue(0).interpolated().synced()
	);

	EnvironmentAttribute<Integer> SUNRISE_SUNSET_COLOR_VISUAL = register(
		"visual/sunrise_sunset_color",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.ARGB_COLOR).defaultValue(0).interpolated().synced()
	);

	EnvironmentAttribute<Integer> CLOUD_COLOR_VISUAL = register(
		"visual/cloud_color",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.ARGB_COLOR).defaultValue(0).interpolated().synced()
	);

	EnvironmentAttribute<Float> CLOUD_HEIGHT_VISUAL = register(
		"visual/cloud_height",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.FLOAT).defaultValue(192.33F).interpolated().synced()
	);

	EnvironmentAttribute<Float> SUN_ANGLE_VISUAL = register(
		"visual/sun_angle",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.ANGLE_DEGREES)
			.defaultValue(0.0F)
			.interpolated()
			.synced()
	);

	EnvironmentAttribute<Float> MOON_ANGLE_VISUAL = register(
		"visual/moon_angle",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.ANGLE_DEGREES)
			.defaultValue(0.0F)
			.interpolated()
			.synced()
	);

	EnvironmentAttribute<Float> STAR_ANGLE_VISUAL = register(
		"visual/star_angle",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.ANGLE_DEGREES)
			.defaultValue(0.0F)
			.interpolated()
			.synced()
	);

	EnvironmentAttribute<MoonPhase> MOON_PHASE_VISUAL = register(
		"visual/moon_phase",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.MOON_PHASE)
			.defaultValue(MoonPhase.FULL_MOON)
			.synced()
	);

	EnvironmentAttribute<Float> STAR_BRIGHTNESS_VISUAL = register(
		"visual/star_brightness",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.FLOAT)
			.defaultValue(0.0F)
			.validator(AttributeValidator.PROBABILITY)
			.interpolated()
			.synced()
	);

	EnvironmentAttribute<Integer> SKY_LIGHT_COLOR_VISUAL = register(
		"visual/sky_light_color",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.RGB_COLOR).defaultValue(-1).interpolated().synced()
	);

	EnvironmentAttribute<Float> SKY_LIGHT_FACTOR_VISUAL = register(
		"visual/sky_light_factor",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.FLOAT)
			.defaultValue(1.0F)
			.validator(AttributeValidator.PROBABILITY)
			.interpolated()
			.synced()
	);

	EnvironmentAttribute<ParticleEffect> DEFAULT_DRIPSTONE_PARTICLE_VISUAL = register(
		"visual/default_dripstone_particle",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.PARTICLE)
			.defaultValue(ParticleTypes.DRIPPING_DRIPSTONE_WATER)
			.synced()
	);

	EnvironmentAttribute<List<AmbientParticle>> AMBIENT_PARTICLES_VISUAL = register(
		"visual/ambient_particles",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.AMBIENT_PARTICLES).defaultValue(List.of()).synced()
	);

	// ─── Аудио-атрибуты ─────────────────────────────────────────────────────

	EnvironmentAttribute<BackgroundMusic> BACKGROUND_MUSIC_AUDIO = register(
		"audio/background_music",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.BACKGROUND_MUSIC)
			.defaultValue(BackgroundMusic.EMPTY)
			.synced()
	);

	EnvironmentAttribute<Float> MUSIC_VOLUME_AUDIO = register(
		"audio/music_volume",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.FLOAT)
			.defaultValue(1.0F)
			.validator(AttributeValidator.PROBABILITY)
			.synced()
	);

	EnvironmentAttribute<AmbientSounds> AMBIENT_SOUNDS_AUDIO = register(
		"audio/ambient_sounds",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.AMBIENT_SOUNDS)
			.defaultValue(AmbientSounds.DEFAULT)
			.synced()
	);

	EnvironmentAttribute<Boolean> FIREFLY_BUSH_SOUNDS_AUDIO = register(
		"audio/firefly_bush_sounds",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.BOOLEAN).defaultValue(false).synced()
	);

	// ─── Игровые атрибуты ───────────────────────────────────────────────────

	EnvironmentAttribute<Float> SKY_LIGHT_LEVEL_GAMEPLAY = register(
		"gameplay/sky_light_level",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.FLOAT)
			.defaultValue(15.0F)
			.validator(AttributeValidator.ranged(0.0F, 15.0F))
			.global()
			.synced()
	);

	EnvironmentAttribute<Boolean> CAN_START_RAID_GAMEPLAY = register(
		"gameplay/can_start_raid",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.BOOLEAN).defaultValue(true)
	);

	EnvironmentAttribute<Boolean> WATER_EVAPORATES_GAMEPLAY = register(
		"gameplay/water_evaporates",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.BOOLEAN).defaultValue(false).synced()
	);

	EnvironmentAttribute<BedRule> BED_RULE_GAMEPLAY = register(
		"gameplay/bed_rule",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.BED_RULE).defaultValue(BedRule.OVERWORLD)
	);

	EnvironmentAttribute<Boolean> RESPAWN_ANCHOR_WORKS_GAMEPLAY = register(
		"gameplay/respawn_anchor_works",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.BOOLEAN).defaultValue(false)
	);

	EnvironmentAttribute<Boolean> NETHER_PORTAL_SPAWNS_PIGLIN_GAMEPLAY = register(
		"gameplay/nether_portal_spawns_piglin",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.BOOLEAN).defaultValue(false)
	);

	EnvironmentAttribute<Boolean> FAST_LAVA_GAMEPLAY = register(
		"gameplay/fast_lava",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.BOOLEAN).defaultValue(false).global().synced()
	);

	EnvironmentAttribute<Boolean> INCREASED_FIRE_BURNOUT_GAMEPLAY = register(
		"gameplay/increased_fire_burnout",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.BOOLEAN).defaultValue(false)
	);

	EnvironmentAttribute<TriState> EYEBLOSSOM_OPEN_GAMEPLAY = register(
		"gameplay/eyeblossom_open",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.TRI_STATE).defaultValue(TriState.DEFAULT)
	);

	EnvironmentAttribute<Float> TURTLE_EGG_HATCH_CHANCE_GAMEPLAY = register(
		"gameplay/turtle_egg_hatch_chance",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.FLOAT)
			.defaultValue(0.0F)
			.validator(AttributeValidator.PROBABILITY)
	);

	EnvironmentAttribute<Boolean> PIGLINS_ZOMBIFY_GAMEPLAY = register(
		"gameplay/piglins_zombify",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.BOOLEAN).defaultValue(true).synced()
	);

	EnvironmentAttribute<Boolean> SNOW_GOLEM_MELTS_GAMEPLAY = register(
		"gameplay/snow_golem_melts",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.BOOLEAN).defaultValue(false)
	);

	EnvironmentAttribute<Boolean> CREAKING_ACTIVE_GAMEPLAY = register(
		"gameplay/creaking_active",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.BOOLEAN).defaultValue(false).synced()
	);

	EnvironmentAttribute<Float> SURFACE_SLIME_SPAWN_CHANCE_GAMEPLAY = register(
		"gameplay/surface_slime_spawn_chance",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.FLOAT)
			.defaultValue(0.0F)
			.validator(AttributeValidator.PROBABILITY)
	);

	EnvironmentAttribute<Float> CAT_WAKING_UP_GIFT_CHANCE_GAMEPLAY = register(
		"gameplay/cat_waking_up_gift_chance",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.FLOAT)
			.defaultValue(0.0F)
			.validator(AttributeValidator.PROBABILITY)
	);

	EnvironmentAttribute<Boolean> BEES_STAY_IN_HIVE_GAMEPLAY = register(
		"gameplay/bees_stay_in_hive",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.BOOLEAN).defaultValue(false)
	);

	EnvironmentAttribute<Boolean> MONSTERS_BURN_GAMEPLAY = register(
		"gameplay/monsters_burn",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.BOOLEAN).defaultValue(false)
	);

	EnvironmentAttribute<Boolean> CAN_PILLAGER_PATROL_SPAWN_GAMEPLAY = register(
		"gameplay/can_pillager_patrol_spawn",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.BOOLEAN).defaultValue(true)
	);

	EnvironmentAttribute<Activity> VILLAGER_ACTIVITY_GAMEPLAY = register(
		"gameplay/villager_activity",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.ACTIVITY).defaultValue(Activity.IDLE)
	);

	EnvironmentAttribute<Activity> BABY_VILLAGER_ACTIVITY_GAMEPLAY = register(
		"gameplay/baby_villager_activity",
		EnvironmentAttribute.builder(EnvironmentAttributeTypes.ACTIVITY).defaultValue(Activity.IDLE)
	);

	// ─── Codec и инициализация ───────────────────────────────────────────────

	Codec<EnvironmentAttribute<?>> CODEC = Registries.ENVIRONMENTAL_ATTRIBUTE.getCodec();

	/** Вызывается при инициализации реестра для гарантии загрузки всех атрибутов. */
	static EnvironmentAttribute<?> registerAndGetDefault(Registry<EnvironmentAttribute<?>> registry) {
		return RESPAWN_ANCHOR_WORKS_GAMEPLAY;
	}

	private static <Value> EnvironmentAttribute<Value> register(
		String path,
		EnvironmentAttribute.Builder<Value> builder
	) {
		EnvironmentAttribute<Value> attribute = builder.build();
		Registry.register(Registries.ENVIRONMENTAL_ATTRIBUTE, Identifier.ofVanilla(path), attribute);
		return attribute;
	}
}

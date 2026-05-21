package net.minecraft.world.attribute.timeline;

import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.MoonPhase;
import net.minecraft.world.attribute.BooleanModifier;
import net.minecraft.world.attribute.ColorModifier;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.FloatModifier;
import net.minecraft.world.dimension.DimensionType;

/**
 * {@code Timelines}.
 */
public interface Timelines {

	RegistryKey<Timeline> DAY = key("day");

	RegistryKey<Timeline> MOON = key("moon");

	RegistryKey<Timeline> VILLAGER_SCHEDULE = key("villager_schedule");

	RegistryKey<Timeline> EARLY_GAME = key("early_game");

	float DAY_SKY_LIGHT_INTENSITY = 15.0F;

	float NIGHT_SKY_LIGHT_INTENSITY = 4.0F;

	int NIGHT_SKY_LIGHT_COLOR = ColorHelper.fromFloats(1.0F, 0.48F, 0.48F, 1.0F);

	float NIGHT_FOG_DENSITY = 0.24F;

	int TRANSPARENT_COLOR = -16777216;

	int NIGHT_FOG_COLOR = ColorHelper.fromFloats(1.0F, 0.06F, 0.06F, 0.09F);

	int NIGHT_CLOUD_COLOR = ColorHelper.fromFloats(1.0F, 0.1F, 0.1F, 0.15F);

	static void bootstrap(Registerable<Timeline> registry) {
		EasingType easingType = EasingType.cubicBezierSymmetric(0.362F, 0.241F);
		int i = 12600;
		int j = 23401;
		int k = 6000;
		registry.register(
				DAY,
				Timeline.builder()
				        .period(24000)
				        .entry(
						        EnvironmentAttributes.SUN_ANGLE_VISUAL,
						        track -> track.easingType(easingType).keyframe(6000, 360.0F).keyframe(6000, 0.0F)
				        )
				        .entry(
						        EnvironmentAttributes.MOON_ANGLE_VISUAL,
						        track -> track.easingType(easingType).keyframe(6000, 540.0F).keyframe(6000, 180.0F)
				        )
				        .entry(
						        EnvironmentAttributes.STAR_ANGLE_VISUAL,
						        track -> track.easingType(easingType).keyframe(6000, 360.0F).keyframe(6000, 0.0F)
				        )
				        .entry(
						        EnvironmentAttributes.FIREFLY_BUSH_SOUNDS_AUDIO,
						        BooleanModifier.OR,
						        track -> track.keyframe(12600, true).keyframe(23401, false)
				        )
				        .entry(
						        EnvironmentAttributes.FOG_COLOR_VISUAL,
						        ColorModifier.MULTIPLY_RGB,
						        track -> track
								        .keyframe(133, -1)
								        .keyframe(11867, -1)
								        .keyframe(13670, NIGHT_FOG_COLOR)
								        .keyframe(22330, NIGHT_FOG_COLOR)
				        )
				        .entry(
						        EnvironmentAttributes.SKY_COLOR_VISUAL,
						        ColorModifier.MULTIPLY_RGB,
						        track -> track
								        .keyframe(133, -1)
								        .keyframe(11867, -1)
								        .keyframe(13670, -16777216)
								        .keyframe(22330, -16777216)
				        )
				        .entry(
						        EnvironmentAttributes.SKY_LIGHT_COLOR_VISUAL,
						        ColorModifier.MULTIPLY_RGB,
						        track -> track
								        .keyframe(730, -1)
								        .keyframe(11270, -1)
								        .keyframe(13140, NIGHT_SKY_LIGHT_COLOR)
								        .keyframe(22860, NIGHT_SKY_LIGHT_COLOR)
				        )
				        .entry(
						        EnvironmentAttributes.SKY_LIGHT_FACTOR_VISUAL,
						        FloatModifier.MULTIPLY,
						        track -> track
								        .keyframe(730, 1.0F)
								        .keyframe(11270, 1.0F)
								        .keyframe(13140, 0.24F)
								        .keyframe(22860, 0.24F)
				        )
				        .entry(
						        EnvironmentAttributes.SKY_LIGHT_LEVEL_GAMEPLAY,
						        FloatModifier.MULTIPLY,
						        track -> track
								        .keyframe(133, 1.0F)
								        .keyframe(11867, 1.0F)
								        .keyframe(13670, 0.26666668F)
								        .keyframe(22330, 0.26666668F)
				        )
				        .entry(
						        EnvironmentAttributes.SUNRISE_SUNSET_COLOR_VISUAL,
						        track -> track.keyframe(71, 1609540403)
						                      .keyframe(310, 703969843)
						                      .keyframe(565, 117167155)
						                      .keyframe(730, 16770355)
						                      .keyframe(11270, 16770355)
						                      .keyframe(11397, 83679283)
						                      .keyframe(11522, 268028723)
						                      .keyframe(11690, 703969843)
						                      .keyframe(11929, 1609540403)
						                      .keyframe(12243, -1310226637)
						                      .keyframe(12358, -857440717)
						                      .keyframe(12512, -371166669)
						                      .keyframe(12613, -153261261)
						                      .keyframe(12732, -19242189)
						                      .keyframe(12841, -19440589)
						                      .keyframe(13035, -321760973)
						                      .keyframe(13252, -1043577037)
						                      .keyframe(13775, 918435635)
						                      .keyframe(13888, 532362547)
						                      .keyframe(14039, 163001139)
						                      .keyframe(14192, 11744051)
						                      .keyframe(21807, 11678515)
						                      .keyframe(21961, 163001139)
						                      .keyframe(22112, 532362547)
						                      .keyframe(22225, 918435635)
						                      .keyframe(22748, -1043577037)
						                      .keyframe(22965, -321760973)
						                      .keyframe(23159, -19440589)
						                      .keyframe(23272, -19242189)
						                      .keyframe(23488, -371166669)
						                      .keyframe(23642, -857440717)
						                      .keyframe(23757, -1310226637)
				        )
				        .entry(
						        EnvironmentAttributes.STAR_BRIGHTNESS_VISUAL,
						        FloatModifier.MAXIMUM,
						        track -> track.keyframe(92, 0.037F)
						                      .keyframe(627, 0.0F)
						                      .keyframe(11373, 0.0F)
						                      .keyframe(11732, 0.016F)
						                      .keyframe(11959, 0.044F)
						                      .keyframe(12399, 0.143F)
						                      .keyframe(12729, 0.258F)
						                      .keyframe(13228, 0.5F)
						                      .keyframe(22772, 0.5F)
						                      .keyframe(23032, 0.364F)
						                      .keyframe(23356, 0.225F)
						                      .keyframe(23758, 0.101F)
				        )
				        .entry(
						        EnvironmentAttributes.CLOUD_COLOR_VISUAL,
						        ColorModifier.MULTIPLY_ARGB,
						        track -> track
								        .keyframe(133, -1)
								        .keyframe(11867, -1)
								        .keyframe(13670, NIGHT_CLOUD_COLOR)
								        .keyframe(22330, NIGHT_CLOUD_COLOR)
				        )
				        .entry(
						        EnvironmentAttributes.EYEBLOSSOM_OPEN_GAMEPLAY,
						        track -> track.keyframe(12600, TriState.TRUE).keyframe(23401, TriState.FALSE)
				        )
				        .entry(
						        EnvironmentAttributes.CREAKING_ACTIVE_GAMEPLAY,
						        BooleanModifier.OR,
						        track -> track.keyframe(12600, true).keyframe(23401, false)
				        )
				        .entry(
						        EnvironmentAttributes.TURTLE_EGG_HATCH_CHANCE_GAMEPLAY,
						        FloatModifier.MAXIMUM,
						        track -> track
								        .easingType(EasingType.CONSTANT)
								        .keyframe(21062, 1.0F)
								        .keyframe(21905, 0.002F)
				        )
				        .entry(
						        EnvironmentAttributes.CAT_WAKING_UP_GIFT_CHANCE_GAMEPLAY,
						        FloatModifier.MAXIMUM,
						        track -> track.easingType(EasingType.CONSTANT).keyframe(362, 0.0F).keyframe(23667, 0.7F)
				        )
				        .entry(
						        EnvironmentAttributes.BEES_STAY_IN_HIVE_GAMEPLAY,
						        BooleanModifier.OR,
						        track -> track.keyframe(12542, true).keyframe(23460, false)
				        )
				        .entry(
						        EnvironmentAttributes.MONSTERS_BURN_GAMEPLAY,
						        BooleanModifier.OR,
						        track -> track.keyframe(12542, false).keyframe(23460, true)
				        )
				        .build()
		);
		Timeline.Builder builder = Timeline.builder().period(24000 * MoonPhase.COUNT).entry(
				EnvironmentAttributes.MOON_PHASE_VISUAL, track -> {
					for (MoonPhase moonPhase : MoonPhase.values()) {
						track.keyframe(moonPhase.phaseTicks(), moonPhase);
					}
				}
		).entry(
				EnvironmentAttributes.SURFACE_SLIME_SPAWN_CHANCE_GAMEPLAY, FloatModifier.MAXIMUM, track -> {
					track.easingType(EasingType.CONSTANT);

					for (MoonPhase moonPhase : MoonPhase.values()) {
						track.keyframe(moonPhase.phaseTicks(), DimensionType.MOON_SIZES[moonPhase.getIndex()] * 0.5F);
					}
				}
		);
		registry.register(MOON, builder.build());
		int l = 2000;
		int m = 7000;
		registry.register(
				VILLAGER_SCHEDULE,
				Timeline.builder()
				        .period(24000)
				        .entry(
						        EnvironmentAttributes.VILLAGER_ACTIVITY_GAMEPLAY,
						        track -> track.keyframe(10, Activity.IDLE)
						                      .keyframe(2000, Activity.WORK)
						                      .keyframe(9000, Activity.MEET)
						                      .keyframe(11000, Activity.IDLE)
						                      .keyframe(12000, Activity.REST)
				        )
				        .entry(
						        EnvironmentAttributes.BABY_VILLAGER_ACTIVITY_GAMEPLAY,
						        track -> track.keyframe(10, Activity.IDLE)
						                      .keyframe(3000, Activity.PLAY)
						                      .keyframe(6000, Activity.IDLE)
						                      .keyframe(10000, Activity.PLAY)
						                      .keyframe(12000, Activity.REST)
				        )
				        .build()
		);
		registry.register(
				EARLY_GAME,
				Timeline.builder()
				        .entry(
						        EnvironmentAttributes.CAN_PILLAGER_PATROL_SPAWN_GAMEPLAY,
						        BooleanModifier.AND,
						        track -> track.keyframe(0, false).keyframe(120000, true)
				        )
				        .build()
		);
	}

	private static RegistryKey<Timeline> key(String path) {
		return RegistryKey.of(RegistryKeys.TIMELINE, Identifier.ofVanilla(path));
	}
}

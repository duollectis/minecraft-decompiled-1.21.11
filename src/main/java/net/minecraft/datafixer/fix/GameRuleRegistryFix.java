package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.math.MathHelper;

/**
 * {@code GameRuleRegistryFix}.
 */
public class GameRuleRegistryFix extends DataFix {

	public GameRuleRegistryFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	protected TypeRewriteRule makeRule() {
		return this.fixTypeEverywhereTyped(
				"GameRuleRegistryFix",
				this.getInputSchema().getType(TypeReferences.LEVEL),
				typed -> typed.update(
						DSL.remainderFinder(),
						dynamic -> dynamic.renameAndFixField(
								"GameRules",
								"game_rules",
								dynamicx -> {
									boolean bl = Boolean.parseBoolean(dynamicx.get("doFireTick").asString("true"));
									boolean
											bl2 =
											Boolean.parseBoolean(dynamicx
													.get("allowFireTicksAwayFromPlayer")
													.asString("false"));
									int i;
									if (!bl) {
										i = 0;
									}
									else if (!bl2) {
										i = 128;
									}
									else {
										i = -1;
									}

									if (i != 128) {
										dynamicx =
												dynamicx.set(
														"minecraft:fire_spread_radius_around_player",
														dynamicx.createInt(i)
												);
									}

									return dynamicx.remove("spawnChunkRadius")
									               .remove("entitiesWithPassengersCanUsePortals")
									               .remove("doFireTick")
									               .remove("allowFireTicksAwayFromPlayer")
									               .renameAndFixField(
											               "allowEnteringNetherUsingPortals",
											               "minecraft:allow_entering_nether_using_portals",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "announceAdvancements",
											               "minecraft:show_advancement_messages",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "blockExplosionDropDecay",
											               "minecraft:block_explosion_drop_decay",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "commandBlockOutput",
											               "minecraft:command_block_output",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "enableCommandBlocks",
											               "minecraft:command_blocks_work",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "commandBlocksEnabled",
											               "minecraft:command_blocks_work",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "commandModificationBlockLimit",
											               "minecraft:max_block_modifications",
											               dynamicxx -> clamp(dynamicxx, 1)
									               )
									               .renameAndFixField(
											               "disableElytraMovementCheck",
											               "minecraft:elytra_movement_check",
											               GameRuleRegistryFix::isFalse
									               )
									               .renameAndFixField(
											               "disablePlayerMovementCheck",
											               "minecraft:player_movement_check",
											               GameRuleRegistryFix::isFalse
									               )
									               .renameAndFixField(
											               "disableRaids",
											               "minecraft:raids",
											               GameRuleRegistryFix::isFalse
									               )
									               .renameAndFixField(
											               "doDaylightCycle",
											               "minecraft:advance_time",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "doEntityDrops",
											               "minecraft:entity_drops",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "doImmediateRespawn",
											               "minecraft:immediate_respawn",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "doInsomnia",
											               "minecraft:spawn_phantoms",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "doLimitedCrafting",
											               "minecraft:limited_crafting",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "doMobLoot",
											               "minecraft:mob_drops",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "doMobSpawning",
											               "minecraft:spawn_mobs",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "doPatrolSpawning",
											               "minecraft:spawn_patrols",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "doTileDrops",
											               "minecraft:block_drops",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "doTraderSpawning",
											               "minecraft:spawn_wandering_traders",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "doVinesSpread",
											               "minecraft:spread_vines",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "doWardenSpawning",
											               "minecraft:spawn_wardens",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "doWeatherCycle",
											               "minecraft:advance_weather",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "drowningDamage",
											               "minecraft:drowning_damage",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "enderPearlsVanishOnDeath",
											               "minecraft:ender_pearls_vanish_on_death",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "fallDamage",
											               "minecraft:fall_damage",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "fireDamage",
											               "minecraft:fire_damage",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "forgiveDeadPlayers",
											               "minecraft:forgive_dead_players",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "freezeDamage",
											               "minecraft:freeze_damage",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "globalSoundEvents",
											               "minecraft:global_sound_events",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "keepInventory",
											               "minecraft:keep_inventory",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "lavaSourceConversion",
											               "minecraft:lava_source_conversion",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "locatorBar",
											               "minecraft:locator_bar",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "logAdminCommands",
											               "minecraft:log_admin_commands",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "maxCommandChainLength",
											               "minecraft:max_command_sequence_length",
											               dynamicxx -> clamp(dynamicxx, 0)
									               )
									               .renameAndFixField(
											               "maxCommandForkCount",
											               "minecraft:max_command_forks",
											               dynamicxx -> clamp(dynamicxx, 0)
									               )
									               .renameAndFixField(
											               "maxEntityCramming",
											               "minecraft:max_entity_cramming",
											               dynamicxx -> clamp(dynamicxx, 0)
									               )
									               .renameAndFixField(
											               "minecartMaxSpeed",
											               "minecraft:max_minecart_speed",
											               GameRuleRegistryFix::clamp
									               )
									               .renameAndFixField(
											               "mobExplosionDropDecay",
											               "minecraft:mob_explosion_drop_decay",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "mobGriefing",
											               "minecraft:mob_griefing",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "naturalRegeneration",
											               "minecraft:natural_health_regeneration",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "playersNetherPortalCreativeDelay",
											               "minecraft:players_nether_portal_creative_delay",
											               dynamicxx -> clamp(dynamicxx, 0)
									               )
									               .renameAndFixField(
											               "playersNetherPortalDefaultDelay",
											               "minecraft:players_nether_portal_default_delay",
											               dynamicxx -> clamp(dynamicxx, 0)
									               )
									               .renameAndFixField(
											               "playersSleepingPercentage",
											               "minecraft:players_sleeping_percentage",
											               dynamicxx -> clamp(dynamicxx, 0)
									               )
									               .renameAndFixField(
											               "projectilesCanBreakBlocks",
											               "minecraft:projectiles_can_break_blocks",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "pvp",
											               "minecraft:pvp",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "randomTickSpeed",
											               "minecraft:random_tick_speed",
											               dynamicxx -> clamp(dynamicxx, 0)
									               )
									               .renameAndFixField(
											               "reducedDebugInfo",
											               "minecraft:reduced_debug_info",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "sendCommandFeedback",
											               "minecraft:send_command_feedback",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "showDeathMessages",
											               "minecraft:show_death_messages",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "snowAccumulationHeight",
											               "minecraft:max_snow_accumulation_height",
											               dynamicxx -> clamp(dynamicxx, 0, 8)
									               )
									               .renameAndFixField(
											               "spawnMonsters",
											               "minecraft:spawn_monsters",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "spawnRadius",
											               "minecraft:respawn_radius",
											               GameRuleRegistryFix::clamp
									               )
									               .renameAndFixField(
											               "spawnerBlocksEnabled",
											               "minecraft:spawner_blocks_work",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "spectatorsGenerateChunks",
											               "minecraft:spectators_generate_chunks",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "tntExplodes",
											               "minecraft:tnt_explodes",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "tntExplosionDropDecay",
											               "minecraft:tnt_explosion_drop_decay",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "universalAnger",
											               "minecraft:universal_anger",
											               GameRuleRegistryFix::isTrue
									               )
									               .renameAndFixField(
											               "waterSourceConversion",
											               "minecraft:water_source_conversion",
											               GameRuleRegistryFix::isTrue
									               );
								}
						)
				)
		);
	}

	private static Dynamic<?> clamp(Dynamic<?> dynamic) {
		return clamp(dynamic, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	private static Dynamic<?> clamp(Dynamic<?> dynamic, int min) {
		return clamp(dynamic, min, Integer.MAX_VALUE);
	}

	private static Dynamic<?> clamp(Dynamic<?> dynamic, int min, int max) {
		String string = dynamic.asString("");

		try {
			int i = Integer.parseInt(string);
			return dynamic.createInt(MathHelper.clamp(i, min, max));
		}
		catch (NumberFormatException var5) {
			return dynamic;
		}
	}

	private static Dynamic<?> isTrue(Dynamic<?> dynamic) {
		return dynamic.createBoolean(Boolean.parseBoolean(dynamic.asString("")));
	}

	private static Dynamic<?> isFalse(Dynamic<?> dynamic) {
		return dynamic.createBoolean(!Boolean.parseBoolean(dynamic.asString("")));
	}
}

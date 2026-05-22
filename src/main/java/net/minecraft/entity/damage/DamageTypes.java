package net.minecraft.entity.damage;

import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * Реестр всех ключей типов урона, используемых в ванильной игре.
 * Каждый ключ соответствует записи в датапаке {@code data/minecraft/damage_type/}.
 * Метод {@link #bootstrap} регистрирует все стандартные типы при генерации мира.
 */
public interface DamageTypes {

	RegistryKey<DamageType> IN_FIRE = of("in_fire");
	RegistryKey<DamageType> CAMPFIRE = of("campfire");
	RegistryKey<DamageType> LIGHTNING_BOLT = of("lightning_bolt");
	RegistryKey<DamageType> ON_FIRE = of("on_fire");
	RegistryKey<DamageType> LAVA = of("lava");
	RegistryKey<DamageType> HOT_FLOOR = of("hot_floor");
	RegistryKey<DamageType> IN_WALL = of("in_wall");
	RegistryKey<DamageType> CRAMMING = of("cramming");
	RegistryKey<DamageType> DROWN = of("drown");
	RegistryKey<DamageType> STARVE = of("starve");
	RegistryKey<DamageType> CACTUS = of("cactus");
	RegistryKey<DamageType> FALL = of("fall");
	RegistryKey<DamageType> ENDER_PEARL = of("ender_pearl");
	RegistryKey<DamageType> FLY_INTO_WALL = of("fly_into_wall");
	RegistryKey<DamageType> OUT_OF_WORLD = of("out_of_world");
	RegistryKey<DamageType> GENERIC = of("generic");
	RegistryKey<DamageType> MAGIC = of("magic");
	RegistryKey<DamageType> WITHER = of("wither");
	RegistryKey<DamageType> DRAGON_BREATH = of("dragon_breath");
	RegistryKey<DamageType> DRY_OUT = of("dry_out");
	RegistryKey<DamageType> SWEET_BERRY_BUSH = of("sweet_berry_bush");
	RegistryKey<DamageType> FREEZE = of("freeze");
	RegistryKey<DamageType> STALAGMITE = of("stalagmite");
	RegistryKey<DamageType> FALLING_BLOCK = of("falling_block");
	RegistryKey<DamageType> FALLING_ANVIL = of("falling_anvil");
	RegistryKey<DamageType> FALLING_STALACTITE = of("falling_stalactite");
	RegistryKey<DamageType> STING = of("sting");
	RegistryKey<DamageType> MOB_ATTACK = of("mob_attack");
	RegistryKey<DamageType> MOB_ATTACK_NO_AGGRO = of("mob_attack_no_aggro");
	RegistryKey<DamageType> PLAYER_ATTACK = of("player_attack");
	RegistryKey<DamageType> SPEAR = of("spear");
	RegistryKey<DamageType> ARROW = of("arrow");
	RegistryKey<DamageType> TRIDENT = of("trident");
	RegistryKey<DamageType> MOB_PROJECTILE = of("mob_projectile");
	RegistryKey<DamageType> SPIT = of("spit");
	RegistryKey<DamageType> WIND_CHARGE = of("wind_charge");
	RegistryKey<DamageType> FIREWORKS = of("fireworks");
	RegistryKey<DamageType> FIREBALL = of("fireball");
	RegistryKey<DamageType> UNATTRIBUTED_FIREBALL = of("unattributed_fireball");
	RegistryKey<DamageType> WITHER_SKULL = of("wither_skull");
	RegistryKey<DamageType> THROWN = of("thrown");
	RegistryKey<DamageType> INDIRECT_MAGIC = of("indirect_magic");
	RegistryKey<DamageType> THORNS = of("thorns");
	RegistryKey<DamageType> EXPLOSION = of("explosion");
	RegistryKey<DamageType> PLAYER_EXPLOSION = of("player_explosion");
	RegistryKey<DamageType> SONIC_BOOM = of("sonic_boom");
	RegistryKey<DamageType> BAD_RESPAWN_POINT = of("bad_respawn_point");
	RegistryKey<DamageType> OUTSIDE_BORDER = of("outside_border");
	RegistryKey<DamageType> GENERIC_KILL = of("generic_kill");
	RegistryKey<DamageType> MACE_SMASH = of("mace_smash");

	static void bootstrap(Registerable<DamageType> registry) {
		registry.register(IN_FIRE, new DamageType("inFire", 0.1F, DamageEffects.BURNING));
		registry.register(CAMPFIRE, new DamageType("inFire", 0.1F, DamageEffects.BURNING));
		registry.register(LIGHTNING_BOLT, new DamageType("lightningBolt", 0.1F));
		registry.register(ON_FIRE, new DamageType("onFire", 0.0F, DamageEffects.BURNING));
		registry.register(LAVA, new DamageType("lava", 0.1F, DamageEffects.BURNING));
		registry.register(HOT_FLOOR, new DamageType("hotFloor", 0.1F, DamageEffects.BURNING));
		registry.register(IN_WALL, new DamageType("inWall", 0.0F));
		registry.register(CRAMMING, new DamageType("cramming", 0.0F));
		registry.register(DROWN, new DamageType("drown", 0.0F, DamageEffects.DROWNING));
		registry.register(STARVE, new DamageType("starve", 0.0F));
		registry.register(CACTUS, new DamageType("cactus", 0.1F));
		registry.register(
			FALL,
			new DamageType(
				"fall",
				DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
				0.0F,
				DamageEffects.HURT,
				DeathMessageType.FALL_VARIANTS
			)
		);
		registry.register(
			ENDER_PEARL,
			new DamageType(
				"fall",
				DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
				0.0F,
				DamageEffects.HURT,
				DeathMessageType.FALL_VARIANTS
			)
		);
		registry.register(FLY_INTO_WALL, new DamageType("flyIntoWall", 0.0F));
		registry.register(OUT_OF_WORLD, new DamageType("outOfWorld", 0.0F));
		registry.register(GENERIC, new DamageType("generic", 0.0F));
		registry.register(MAGIC, new DamageType("magic", 0.0F));
		registry.register(WITHER, new DamageType("wither", 0.0F));
		registry.register(DRAGON_BREATH, new DamageType("dragonBreath", 0.0F));
		registry.register(DRY_OUT, new DamageType("dryout", 0.1F));
		registry.register(SWEET_BERRY_BUSH, new DamageType("sweetBerryBush", 0.1F, DamageEffects.POKING));
		registry.register(FREEZE, new DamageType("freeze", 0.0F, DamageEffects.FREEZING));
		registry.register(STALAGMITE, new DamageType("stalagmite", 0.0F));
		registry.register(FALLING_BLOCK, new DamageType("fallingBlock", 0.1F));
		registry.register(FALLING_ANVIL, new DamageType("anvil", 0.1F));
		registry.register(FALLING_STALACTITE, new DamageType("fallingStalactite", 0.1F));
		registry.register(STING, new DamageType("sting", 0.1F));
		registry.register(MOB_ATTACK, new DamageType("mob", 0.1F));
		registry.register(MOB_ATTACK_NO_AGGRO, new DamageType("mob", 0.1F));
		registry.register(PLAYER_ATTACK, new DamageType("player", 0.1F));
		registry.register(SPEAR, new DamageType("spear", 0.1F));
		registry.register(ARROW, new DamageType("arrow", 0.1F));
		registry.register(TRIDENT, new DamageType("trident", 0.1F));
		registry.register(MOB_PROJECTILE, new DamageType("mob", 0.1F));
		registry.register(SPIT, new DamageType("mob", 0.1F));
		registry.register(FIREWORKS, new DamageType("fireworks", 0.1F));
		registry.register(UNATTRIBUTED_FIREBALL, new DamageType("onFire", 0.1F, DamageEffects.BURNING));
		registry.register(FIREBALL, new DamageType("fireball", 0.1F, DamageEffects.BURNING));
		registry.register(WITHER_SKULL, new DamageType("witherSkull", 0.1F));
		registry.register(THROWN, new DamageType("thrown", 0.1F));
		registry.register(INDIRECT_MAGIC, new DamageType("indirectMagic", 0.0F));
		registry.register(THORNS, new DamageType("thorns", 0.1F, DamageEffects.THORNS));
		registry.register(EXPLOSION, new DamageType("explosion", DamageScaling.ALWAYS, 0.1F));
		registry.register(PLAYER_EXPLOSION, new DamageType("explosion.player", DamageScaling.ALWAYS, 0.1F));
		registry.register(SONIC_BOOM, new DamageType("sonic_boom", DamageScaling.ALWAYS, 0.0F));
		registry.register(
			BAD_RESPAWN_POINT,
			new DamageType(
				"badRespawnPoint",
				DamageScaling.ALWAYS,
				0.1F,
				DamageEffects.HURT,
				DeathMessageType.INTENTIONAL_GAME_DESIGN
			)
		);
		registry.register(OUTSIDE_BORDER, new DamageType("outsideBorder", 0.0F));
		registry.register(GENERIC_KILL, new DamageType("genericKill", 0.0F));
		registry.register(WIND_CHARGE, new DamageType("mob", 0.1F));
		registry.register(MACE_SMASH, new DamageType("mace_smash", 0.1F));
	}

	private static RegistryKey<DamageType> of(String id) {
		return RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.ofVanilla(id));
	}
}

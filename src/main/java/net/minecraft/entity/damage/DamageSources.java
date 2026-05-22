package net.minecraft.entity.damage;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractFireballEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.explosion.Explosion;
import org.jspecify.annotations.Nullable;

/**
 * Фабрика источников урона для конкретного мира. Кэширует часто используемые
 * безатрибутные источники (огонь, утопление, голод и т.д.) как поля,
 * а для источников с атакующей сущностью создаёт новые объекты при каждом вызове.
 */
public class DamageSources {

	public final Registry<DamageType> registry;

	private final DamageSource inFire;
	private final DamageSource campfire;
	private final DamageSource lightningBolt;
	private final DamageSource onFire;
	private final DamageSource lava;
	private final DamageSource hotFloor;
	private final DamageSource inWall;
	private final DamageSource cramming;
	private final DamageSource drown;
	private final DamageSource starve;
	private final DamageSource cactus;
	private final DamageSource fall;
	private final DamageSource enderPearl;
	private final DamageSource flyIntoWall;
	private final DamageSource outOfWorld;
	private final DamageSource generic;
	private final DamageSource magic;
	private final DamageSource wither;
	private final DamageSource dragonBreath;
	private final DamageSource dryOut;
	private final DamageSource sweetBerryBush;
	private final DamageSource freeze;
	private final DamageSource stalagmite;
	private final DamageSource outsideBorder;
	private final DamageSource genericKill;

	public DamageSources(DynamicRegistryManager registryManager) {
		registry = registryManager.getOrThrow(RegistryKeys.DAMAGE_TYPE);
		inFire = create(DamageTypes.IN_FIRE);
		campfire = create(DamageTypes.CAMPFIRE);
		lightningBolt = create(DamageTypes.LIGHTNING_BOLT);
		onFire = create(DamageTypes.ON_FIRE);
		lava = create(DamageTypes.LAVA);
		hotFloor = create(DamageTypes.HOT_FLOOR);
		inWall = create(DamageTypes.IN_WALL);
		cramming = create(DamageTypes.CRAMMING);
		drown = create(DamageTypes.DROWN);
		starve = create(DamageTypes.STARVE);
		cactus = create(DamageTypes.CACTUS);
		fall = create(DamageTypes.FALL);
		enderPearl = create(DamageTypes.ENDER_PEARL);
		flyIntoWall = create(DamageTypes.FLY_INTO_WALL);
		outOfWorld = create(DamageTypes.OUT_OF_WORLD);
		generic = create(DamageTypes.GENERIC);
		magic = create(DamageTypes.MAGIC);
		wither = create(DamageTypes.WITHER);
		dragonBreath = create(DamageTypes.DRAGON_BREATH);
		dryOut = create(DamageTypes.DRY_OUT);
		sweetBerryBush = create(DamageTypes.SWEET_BERRY_BUSH);
		freeze = create(DamageTypes.FREEZE);
		stalagmite = create(DamageTypes.STALAGMITE);
		outsideBorder = create(DamageTypes.OUTSIDE_BORDER);
		genericKill = create(DamageTypes.GENERIC_KILL);
	}

	public final DamageSource create(RegistryKey<DamageType> key) {
		return new DamageSource(registry.getOrThrow(key));
	}

	public final DamageSource create(RegistryKey<DamageType> key, @Nullable Entity attacker) {
		return new DamageSource(registry.getOrThrow(key), attacker);
	}

	public final DamageSource create(RegistryKey<DamageType> key, @Nullable Entity source, @Nullable Entity attacker) {
		return new DamageSource(registry.getOrThrow(key), source, attacker);
	}

	public DamageSource inFire() { return inFire; }

	public DamageSource campfire() { return campfire; }

	public DamageSource lightningBolt() { return lightningBolt; }

	public DamageSource onFire() { return onFire; }

	public DamageSource lava() { return lava; }

	public DamageSource hotFloor() { return hotFloor; }

	public DamageSource inWall() { return inWall; }

	public DamageSource cramming() { return cramming; }

	public DamageSource drown() { return drown; }

	public DamageSource starve() { return starve; }

	public DamageSource cactus() { return cactus; }

	public DamageSource fall() { return fall; }

	public DamageSource enderPearl() { return enderPearl; }

	public DamageSource flyIntoWall() { return flyIntoWall; }

	public DamageSource outOfWorld() { return outOfWorld; }

	public DamageSource generic() { return generic; }

	public DamageSource magic() { return magic; }

	public DamageSource wither() { return wither; }

	public DamageSource dragonBreath() { return dragonBreath; }

	public DamageSource dryOut() { return dryOut; }

	public DamageSource sweetBerryBush() { return sweetBerryBush; }

	public DamageSource freeze() { return freeze; }

	public DamageSource stalagmite() { return stalagmite; }

	public DamageSource outsideBorder() { return outsideBorder; }

	public DamageSource genericKill() { return genericKill; }

	public DamageSource fallingBlock(Entity attacker) {
		return create(DamageTypes.FALLING_BLOCK, attacker);
	}

	public DamageSource fallingAnvil(Entity attacker) {
		return create(DamageTypes.FALLING_ANVIL, attacker);
	}

	public DamageSource fallingStalactite(Entity attacker) {
		return create(DamageTypes.FALLING_STALACTITE, attacker);
	}

	public DamageSource sting(LivingEntity attacker) {
		return create(DamageTypes.STING, attacker);
	}

	public DamageSource mobAttack(LivingEntity attacker) {
		return create(DamageTypes.MOB_ATTACK, attacker);
	}

	public DamageSource mobAttackNoAggro(LivingEntity attacker) {
		return create(DamageTypes.MOB_ATTACK_NO_AGGRO, attacker);
	}

	public DamageSource playerAttack(PlayerEntity attacker) {
		return create(DamageTypes.PLAYER_ATTACK, attacker);
	}

	public DamageSource arrow(PersistentProjectileEntity source, @Nullable Entity attacker) {
		return create(DamageTypes.ARROW, source, attacker);
	}

	public DamageSource trident(Entity source, @Nullable Entity attacker) {
		return create(DamageTypes.TRIDENT, source, attacker);
	}

	public DamageSource mobProjectile(Entity source, @Nullable LivingEntity attacker) {
		return create(DamageTypes.MOB_PROJECTILE, source, attacker);
	}

	public DamageSource spit(Entity source, @Nullable LivingEntity attacker) {
		return create(DamageTypes.SPIT, source, attacker);
	}

	public DamageSource windCharge(Entity source, @Nullable LivingEntity attacker) {
		return create(DamageTypes.WIND_CHARGE, source, attacker);
	}

	public DamageSource fireworks(FireworkRocketEntity source, @Nullable Entity attacker) {
		return create(DamageTypes.FIREWORKS, source, attacker);
	}

	/**
	 * Создаёт источник урона от огненного шара. Если атакующий не задан,
	 * используется тип {@code UNATTRIBUTED_FIREBALL} (без атрибуции к игроку).
	 *
	 * @param source снаряд-огненный шар
	 * @param attacker атакующая сущность или {@code null}
	 * @return источник урона
	 */
	public DamageSource fireball(AbstractFireballEntity source, @Nullable Entity attacker) {
		return attacker == null
			? create(DamageTypes.UNATTRIBUTED_FIREBALL, source)
			: create(DamageTypes.FIREBALL, source, attacker);
	}

	public DamageSource witherSkull(WitherSkullEntity source, Entity attacker) {
		return create(DamageTypes.WITHER_SKULL, source, attacker);
	}

	public DamageSource thrown(Entity source, @Nullable Entity attacker) {
		return create(DamageTypes.THROWN, source, attacker);
	}

	public DamageSource indirectMagic(Entity source, @Nullable Entity attacker) {
		return create(DamageTypes.INDIRECT_MAGIC, source, attacker);
	}

	public DamageSource thorns(Entity attacker) {
		return create(DamageTypes.THORNS, attacker);
	}

	/**
	 * Создаёт источник урона от взрыва. Если взрыв вызван игроком,
	 * используется тип {@code PLAYER_EXPLOSION}, иначе — {@code EXPLOSION}.
	 *
	 * @param explosion объект взрыва или {@code null}
	 * @return источник урона
	 */
	public DamageSource explosion(@Nullable Explosion explosion) {
		return explosion != null
			? explosion(explosion.getEntity(), explosion.getCausingEntity())
			: explosion(null, null);
	}

	public DamageSource explosion(@Nullable Entity source, @Nullable Entity attacker) {
		return create(
			attacker != null && source != null ? DamageTypes.PLAYER_EXPLOSION : DamageTypes.EXPLOSION,
			source,
			attacker
		);
	}

	public DamageSource sonicBoom(Entity attacker) {
		return create(DamageTypes.SONIC_BOOM, attacker);
	}

	public DamageSource badRespawnPoint(Vec3d position) {
		return new DamageSource(registry.getOrThrow(DamageTypes.BAD_RESPAWN_POINT), position);
	}

	public DamageSource maceSmash(Entity attacker) {
		return create(DamageTypes.MACE_SMASH, attacker);
	}
}

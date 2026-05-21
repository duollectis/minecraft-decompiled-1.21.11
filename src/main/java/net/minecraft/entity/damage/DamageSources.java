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
 * {@code DamageSources}.
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
		this.registry = registryManager.getOrThrow(RegistryKeys.DAMAGE_TYPE);
		this.inFire = this.create(DamageTypes.IN_FIRE);
		this.campfire = this.create(DamageTypes.CAMPFIRE);
		this.lightningBolt = this.create(DamageTypes.LIGHTNING_BOLT);
		this.onFire = this.create(DamageTypes.ON_FIRE);
		this.lava = this.create(DamageTypes.LAVA);
		this.hotFloor = this.create(DamageTypes.HOT_FLOOR);
		this.inWall = this.create(DamageTypes.IN_WALL);
		this.cramming = this.create(DamageTypes.CRAMMING);
		this.drown = this.create(DamageTypes.DROWN);
		this.starve = this.create(DamageTypes.STARVE);
		this.cactus = this.create(DamageTypes.CACTUS);
		this.fall = this.create(DamageTypes.FALL);
		this.enderPearl = this.create(DamageTypes.ENDER_PEARL);
		this.flyIntoWall = this.create(DamageTypes.FLY_INTO_WALL);
		this.outOfWorld = this.create(DamageTypes.OUT_OF_WORLD);
		this.generic = this.create(DamageTypes.GENERIC);
		this.magic = this.create(DamageTypes.MAGIC);
		this.wither = this.create(DamageTypes.WITHER);
		this.dragonBreath = this.create(DamageTypes.DRAGON_BREATH);
		this.dryOut = this.create(DamageTypes.DRY_OUT);
		this.sweetBerryBush = this.create(DamageTypes.SWEET_BERRY_BUSH);
		this.freeze = this.create(DamageTypes.FREEZE);
		this.stalagmite = this.create(DamageTypes.STALAGMITE);
		this.outsideBorder = this.create(DamageTypes.OUTSIDE_BORDER);
		this.genericKill = this.create(DamageTypes.GENERIC_KILL);
	}

	/**
	 * Create.
	 *
	 * @param key key
	 *
	 * @return DamageSource — результат операции
	 */
	public final DamageSource create(RegistryKey<DamageType> key) {
		return new DamageSource(this.registry.getOrThrow(key));
	}

	/**
	 * Create.
	 *
	 * @param key key
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public final DamageSource create(RegistryKey<DamageType> key, @Nullable Entity attacker) {
		return new DamageSource(this.registry.getOrThrow(key), attacker);
	}

	/**
	 * Create.
	 *
	 * @param key key
	 * @param source source
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public final DamageSource create(RegistryKey<DamageType> key, @Nullable Entity source, @Nullable Entity attacker) {
		return new DamageSource(this.registry.getOrThrow(key), source, attacker);
	}

	/**
	 * In fire.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource inFire() {
		return this.inFire;
	}

	/**
	 * Campfire.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource campfire() {
		return this.campfire;
	}

	/**
	 * Lightning bolt.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource lightningBolt() {
		return this.lightningBolt;
	}

	/**
	 * Обрабатывает событие fire.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource onFire() {
		return this.onFire;
	}

	/**
	 * Lava.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource lava() {
		return this.lava;
	}

	/**
	 * Hot floor.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource hotFloor() {
		return this.hotFloor;
	}

	/**
	 * In wall.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource inWall() {
		return this.inWall;
	}

	/**
	 * Cramming.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource cramming() {
		return this.cramming;
	}

	/**
	 * Drown.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource drown() {
		return this.drown;
	}

	/**
	 * Starve.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource starve() {
		return this.starve;
	}

	/**
	 * Cactus.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource cactus() {
		return this.cactus;
	}

	/**
	 * Fall.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource fall() {
		return this.fall;
	}

	/**
	 * Ender pearl.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource enderPearl() {
		return this.enderPearl;
	}

	/**
	 * Fly into wall.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource flyIntoWall() {
		return this.flyIntoWall;
	}

	/**
	 * Out of world.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource outOfWorld() {
		return this.outOfWorld;
	}

	/**
	 * Generic.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource generic() {
		return this.generic;
	}

	/**
	 * Magic.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource magic() {
		return this.magic;
	}

	/**
	 * Wither.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource wither() {
		return this.wither;
	}

	/**
	 * Dragon breath.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource dragonBreath() {
		return this.dragonBreath;
	}

	/**
	 * Dry out.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource dryOut() {
		return this.dryOut;
	}

	/**
	 * Sweet berry bush.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource sweetBerryBush() {
		return this.sweetBerryBush;
	}

	/**
	 * Freeze.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource freeze() {
		return this.freeze;
	}

	/**
	 * Stalagmite.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource stalagmite() {
		return this.stalagmite;
	}

	/**
	 * Falling block.
	 *
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource fallingBlock(Entity attacker) {
		return this.create(DamageTypes.FALLING_BLOCK, attacker);
	}

	/**
	 * Falling anvil.
	 *
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource fallingAnvil(Entity attacker) {
		return this.create(DamageTypes.FALLING_ANVIL, attacker);
	}

	/**
	 * Falling stalactite.
	 *
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource fallingStalactite(Entity attacker) {
		return this.create(DamageTypes.FALLING_STALACTITE, attacker);
	}

	/**
	 * Sting.
	 *
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource sting(LivingEntity attacker) {
		return this.create(DamageTypes.STING, attacker);
	}

	/**
	 * Mob attack.
	 *
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource mobAttack(LivingEntity attacker) {
		return this.create(DamageTypes.MOB_ATTACK, attacker);
	}

	/**
	 * Mob attack no aggro.
	 *
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource mobAttackNoAggro(LivingEntity attacker) {
		return this.create(DamageTypes.MOB_ATTACK_NO_AGGRO, attacker);
	}

	/**
	 * Player attack.
	 *
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource playerAttack(PlayerEntity attacker) {
		return this.create(DamageTypes.PLAYER_ATTACK, attacker);
	}

	/**
	 * Arrow.
	 *
	 * @param source source
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource arrow(PersistentProjectileEntity source, @Nullable Entity attacker) {
		return this.create(DamageTypes.ARROW, source, attacker);
	}

	/**
	 * Trident.
	 *
	 * @param source source
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource trident(Entity source, @Nullable Entity attacker) {
		return this.create(DamageTypes.TRIDENT, source, attacker);
	}

	/**
	 * Mob projectile.
	 *
	 * @param source source
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource mobProjectile(Entity source, @Nullable LivingEntity attacker) {
		return this.create(DamageTypes.MOB_PROJECTILE, source, attacker);
	}

	/**
	 * Spit.
	 *
	 * @param source source
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource spit(Entity source, @Nullable LivingEntity attacker) {
		return this.create(DamageTypes.SPIT, source, attacker);
	}

	/**
	 * Wind charge.
	 *
	 * @param source source
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource windCharge(Entity source, @Nullable LivingEntity attacker) {
		return this.create(DamageTypes.WIND_CHARGE, source, attacker);
	}

	/**
	 * Fireworks.
	 *
	 * @param source source
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource fireworks(FireworkRocketEntity source, @Nullable Entity attacker) {
		return this.create(DamageTypes.FIREWORKS, source, attacker);
	}

	/**
	 * Fireball.
	 *
	 * @param source source
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource fireball(AbstractFireballEntity source, @Nullable Entity attacker) {
		return attacker == null ? this.create(DamageTypes.UNATTRIBUTED_FIREBALL, source)
		                        : this.create(DamageTypes.FIREBALL, source, attacker);
	}

	/**
	 * Wither skull.
	 *
	 * @param source source
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource witherSkull(WitherSkullEntity source, Entity attacker) {
		return this.create(DamageTypes.WITHER_SKULL, source, attacker);
	}

	/**
	 * Thrown.
	 *
	 * @param source source
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource thrown(Entity source, @Nullable Entity attacker) {
		return this.create(DamageTypes.THROWN, source, attacker);
	}

	/**
	 * Indirect magic.
	 *
	 * @param source source
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource indirectMagic(Entity source, @Nullable Entity attacker) {
		return this.create(DamageTypes.INDIRECT_MAGIC, source, attacker);
	}

	/**
	 * Thorns.
	 *
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource thorns(Entity attacker) {
		return this.create(DamageTypes.THORNS, attacker);
	}

	/**
	 * Explosion.
	 *
	 * @param explosion explosion
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource explosion(@Nullable Explosion explosion) {
		return explosion != null ? this.explosion(explosion.getEntity(), explosion.getCausingEntity())
		                         : this.explosion(null, null);
	}

	/**
	 * Explosion.
	 *
	 * @param source source
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource explosion(@Nullable Entity source, @Nullable Entity attacker) {
		return this.create(
				attacker != null && source != null ? DamageTypes.PLAYER_EXPLOSION : DamageTypes.EXPLOSION,
				source,
				attacker
		);
	}

	/**
	 * Sonic boom.
	 *
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource sonicBoom(Entity attacker) {
		return this.create(DamageTypes.SONIC_BOOM, attacker);
	}

	/**
	 * Bad respawn point.
	 *
	 * @param position position
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource badRespawnPoint(Vec3d position) {
		return new DamageSource(this.registry.getOrThrow(DamageTypes.BAD_RESPAWN_POINT), position);
	}

	/**
	 * Outside border.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource outsideBorder() {
		return this.outsideBorder;
	}

	/**
	 * Generic kill.
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource genericKill() {
		return this.genericKill;
	}

	/**
	 * Mace smash.
	 *
	 * @param attacker attacker
	 *
	 * @return DamageSource — результат операции
	 */
	public DamageSource maceSmash(Entity attacker) {
		return this.create(DamageTypes.MACE_SMASH, attacker);
	}
}

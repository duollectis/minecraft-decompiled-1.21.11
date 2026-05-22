package net.minecraft.entity.mob;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.rule.GameRules;

import java.util.function.Predicate;

/**
 * Базовый класс для всех враждебных мобов. Управляет деспауном на свету.
 */
public abstract class HostileEntity extends PathAwareEntity implements Monster {

	private static final float BRIGHT_DESPAWN_THRESHOLD = 0.5F;
	private static final int BRIGHT_DESPAWN_INCREMENT = 2;

	protected HostileEntity(EntityType<? extends HostileEntity> entityType, World world) {
		super(entityType, world);
		experiencePoints = 5;
	}

	@Override
	public SoundCategory getSoundCategory() {
		return SoundCategory.HOSTILE;
	}

	@Override
	public void tickMovement() {
		tickHandSwing();
		updateDespawnCounter();
		super.tickMovement();
	}

	protected void updateDespawnCounter() {
		if (getBrightnessAtEyes() > BRIGHT_DESPAWN_THRESHOLD) {
			despawnCounter += BRIGHT_DESPAWN_INCREMENT;
		}
	}

	@Override
	protected SoundEvent getSwimSound() {
		return SoundEvents.ENTITY_HOSTILE_SWIM;
	}

	@Override
	protected SoundEvent getSplashSound() {
		return SoundEvents.ENTITY_HOSTILE_SPLASH;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_HOSTILE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_HOSTILE_DEATH;
	}

	@Override
	public LivingEntity.FallSounds getFallSounds() {
		return new LivingEntity.FallSounds(SoundEvents.ENTITY_HOSTILE_SMALL_FALL, SoundEvents.ENTITY_HOSTILE_BIG_FALL);
	}

	@Override
	public float getPathfindingFavor(BlockPos pos, WorldView world) {
		return -world.getPhototaxisFavor(pos);
	}

	public static boolean isSpawnDark(ServerWorldAccess world, BlockPos pos, Random random) {
		if (world.getLightLevel(LightType.SKY, pos) > random.nextInt(32)) {
			return false;
		}

		DimensionType dimensionType = world.getDimension();
		int blockLightLimit = dimensionType.monsterSpawnBlockLightLimit();
		if (blockLightLimit < 15 && world.getLightLevel(LightType.BLOCK, pos) > blockLightLimit) {
			return false;
		}

		int lightLevel = world.toServerWorld().isThundering()
				? world.getLightLevel(pos, 10)
				: world.getLightLevel(pos);
		return lightLevel <= dimensionType.monsterSpawnLightTest().get(random);
	}

	public static boolean canSpawnInDark(
			EntityType<? extends MobEntity> type,
			ServerWorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		return world.getDifficulty() != Difficulty.PEACEFUL
				&& (SpawnReason.isTrialSpawner(spawnReason) || isSpawnDark(world, pos, random))
				&& canMobSpawn(type, world, spawnReason, pos, random);
	}

	public static boolean canSpawnIgnoreLightLevel(
			EntityType<? extends HostileEntity> type,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		return world.getDifficulty() != Difficulty.PEACEFUL && canMobSpawn(type, world, spawnReason, pos, random);
	}

	public static boolean canSpawnInDarkUnderSky(
			EntityType<? extends MobEntity> type,
			ServerWorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		return canSpawnInDark(type, world, spawnReason, pos, random) && (SpawnReason.isAnySpawner(spawnReason)
				|| world.isSkyVisible(pos)
		);
	}

	public static DefaultAttributeContainer.Builder createHostileAttributes() {
		return MobEntity.createMobAttributes().add(EntityAttributes.ATTACK_DAMAGE);
	}

	@Override
	public boolean shouldDropExperience() {
		return true;
	}

	@Override
	protected boolean shouldDropLoot(ServerWorld world) {
		return world.getGameRules().getValue(GameRules.DO_MOB_LOOT);
	}

	public boolean isAngryAt(ServerWorld world, PlayerEntity player) {
		return true;
	}

	@Override
	public ItemStack getProjectileType(ItemStack stack) {
		if (stack.getItem() instanceof RangedWeaponItem rangedWeapon) {
			Predicate<ItemStack> heldProjectiles = rangedWeapon.getHeldProjectiles();
			ItemStack projectile = RangedWeaponItem.getHeldProjectile(this, heldProjectiles);
			return projectile.isEmpty() ? new ItemStack(Items.ARROW) : projectile;
		}

		return ItemStack.EMPTY;
	}
}

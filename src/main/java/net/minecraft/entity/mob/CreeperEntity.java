package net.minecraft.entity.mob;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTables;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * Крипер — взрывоопасный моб, поджигающий фитиль при приближении к цели.
 * Заряженный крипер (после удара молнии) создаёт взрыв двойной силы
 * и при убийстве другого моба дропает его голову.
 */
public class CreeperEntity extends HostileEntity {

	private static final TrackedData<Integer>
			FUSE_SPEED =
			DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Boolean>
			CHARGED =
			DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean>
			IGNITED =
			DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final boolean DEFAULT_CHARGED = false;
	private static final boolean DEFAULT_IGNITED = false;
	private static final short DEFAULT_FUSE = 30;
	private static final byte DEFAULT_EXPLOSION_RADIUS = 3;
	private int lastFuseTime;
	private int currentFuseTime;
	private int fuseTime = DEFAULT_FUSE;
	private int explosionRadius = DEFAULT_EXPLOSION_RADIUS;
	private boolean headsDropped;

	public CreeperEntity(EntityType<? extends CreeperEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new SwimGoal(this));
		goalSelector.add(2, new CreeperIgniteGoal(this));
		goalSelector.add(3, new FleeEntityGoal<>(this, OcelotEntity.class, 6.0F, 1.0, 1.2));
		goalSelector.add(3, new FleeEntityGoal<>(this, CatEntity.class, 6.0F, 1.0, 1.2));
		goalSelector.add(4, new MeleeAttackGoal(this, 1.0, false));
		goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		goalSelector.add(6, new LookAroundGoal(this));
		targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		targetSelector.add(2, new RevengeGoal(this));
	}

	public static DefaultAttributeContainer.Builder createCreeperAttributes() {
		return HostileEntity.createHostileAttributes().add(EntityAttributes.MOVEMENT_SPEED, 0.25);
	}

	@Override
	public int getSafeFallDistance() {
		return getTarget() == null
				? getSafeFallDistance(0.0F)
				: getSafeFallDistance(getHealth() - 1.0F);
	}

	@Override
	public boolean handleFallDamage(double fallDistance, float damagePerDistance, DamageSource damageSource) {
		boolean damaged = super.handleFallDamage(fallDistance, damagePerDistance, damageSource);
		currentFuseTime += (int) (fallDistance * 1.5);

		if (currentFuseTime > fuseTime - 5) {
			currentFuseTime = fuseTime - 5;
		}

		return damaged;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(FUSE_SPEED, -1);
		builder.add(CHARGED, false);
		builder.add(IGNITED, false);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("powered", isCharged());
		view.putShort("Fuse", (short) fuseTime);
		view.putByte("ExplosionRadius", (byte) explosionRadius);
		view.putBoolean("ignited", isIgnited());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		dataTracker.set(CHARGED, view.getBoolean("powered", false));
		fuseTime = view.getShort("Fuse", (short) DEFAULT_FUSE);
		explosionRadius = view.getByte("ExplosionRadius", (byte) 3);

		if (view.getBoolean("ignited", false)) {
			ignite();
		}
	}

	@Override
	public void tick() {
		if (isAlive()) {
			lastFuseTime = currentFuseTime;

			if (isIgnited()) {
				setFuseSpeed(1);
			}

			int fuseSpeed = getFuseSpeed();

			if (fuseSpeed > 0 && currentFuseTime == 0) {
				playSound(SoundEvents.ENTITY_CREEPER_PRIMED, 1.0F, 0.5F);
				emitGameEvent(GameEvent.PRIME_FUSE);
			}

			currentFuseTime += fuseSpeed;

			if (currentFuseTime < 0) {
				currentFuseTime = 0;
			}

			if (currentFuseTime >= fuseTime) {
				currentFuseTime = fuseTime;
				explode();
			}
		}

		super.tick();
	}

	@Override
	public void setTarget(@Nullable LivingEntity target) {
		if (target instanceof GoatEntity) {
			return;
		}

		super.setTarget(target);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_CREEPER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_CREEPER_DEATH;
	}

	@Override
	public boolean onKilledOther(ServerWorld world, LivingEntity other, DamageSource damageSource) {
		if (shouldDropLoot(world) && isCharged() && !headsDropped) {
			other.generateLoot(
					world, damageSource, false, LootTables.ROOT_CHARGED_CREEPER, stack -> {
						other.dropStack(world, stack);
						headsDropped = true;
					}
			);
		}

		return super.onKilledOther(world, other, damageSource);
	}

	@Override
	public boolean tryAttack(ServerWorld world, Entity target) {
		return true;
	}

	public boolean isCharged() {
		return dataTracker.get(CHARGED);
	}

	public float getLerpedFuseTime(float tickProgress) {
		return MathHelper.lerp(tickProgress, (float) lastFuseTime, (float) currentFuseTime) / (fuseTime - 2);
	}

	public int getFuseSpeed() {
		return dataTracker.get(FUSE_SPEED);
	}

	public void setFuseSpeed(int fuseSpeed) {
		dataTracker.set(FUSE_SPEED, fuseSpeed);
	}

	@Override
	public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
		super.onStruckByLightning(world, lightning);
		dataTracker.set(CHARGED, true);
	}

	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack itemStack = player.getStackInHand(hand);

		if (!itemStack.isIn(ItemTags.CREEPER_IGNITERS)) {
			return super.interactMob(player, hand);
		}

		SoundEvent igniteSound = itemStack.isOf(Items.FIRE_CHARGE)
				? SoundEvents.ITEM_FIRECHARGE_USE
				: SoundEvents.ITEM_FLINTANDSTEEL_USE;
		getEntityWorld().playSound(player, getX(), getY(), getZ(), igniteSound, getSoundCategory(), 1.0F, random.nextFloat() * 0.4F + 0.8F);

		if (!getEntityWorld().isClient()) {
			ignite();

			if (!itemStack.isDamageable()) {
				itemStack.decrement(1);
			}
			else {
				itemStack.damage(1, player, hand.getEquipmentSlot());
			}
		}

		return ActionResult.SUCCESS;
	}

	/**
	 * Создаёт взрыв и облако эффектов, затем удаляет крипера из мира.
	 * Радиус взрыва удваивается для заряженного крипера.
	 */
	private void explode() {
		if (!(getEntityWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		float radiusMultiplier = isCharged() ? 2.0F : 1.0F;
		dead = true;
		serverWorld.createExplosion(this, getX(), getY(), getZ(), explosionRadius * radiusMultiplier, World.ExplosionSourceType.MOB);
		spawnEffectsCloud();
		onRemoval(serverWorld, Entity.RemovalReason.KILLED);
		discard();
	}

	private void spawnEffectsCloud() {
		Collection<StatusEffectInstance> effects = getStatusEffects();

		if (effects.isEmpty()) {
			return;
		}

		AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(getEntityWorld(), getX(), getY(), getZ());
		cloud.setRadius(2.5F);
		cloud.setRadiusOnUse(-0.5F);
		cloud.setWaitTime(10);
		cloud.setDuration(300);
		cloud.setPotionDurationScale(0.25F);
		cloud.setRadiusGrowth(-cloud.getRadius() / cloud.getDuration());

		for (StatusEffectInstance effect : effects) {
			cloud.addEffect(new StatusEffectInstance(effect));
		}

		getEntityWorld().spawnEntity(cloud);
	}

	public boolean isIgnited() {
		return dataTracker.get(IGNITED);
	}

	public void ignite() {
		dataTracker.set(IGNITED, true);
	}
}

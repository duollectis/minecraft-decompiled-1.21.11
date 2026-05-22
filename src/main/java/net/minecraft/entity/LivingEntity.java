package net.minecraft.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JavaOps;
import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.*;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.effect.EnchantmentLocationBasedEffect;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTracker;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ElytraFlightController;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.*;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerWaypointHandler;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockLocating;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.rule.GameRules;
import net.minecraft.world.waypoint.ServerWaypoint;
import net.minecraft.world.waypoint.Waypoint;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Базовый класс всех живых существ. Управляет здоровьем, статус-эффектами,
 * снаряжением, анимациями конечностей, атрибутами и логикой смерти.
 * Является точкой расширения для {@link net.minecraft.entity.mob.MobEntity}
 * и {@link net.minecraft.entity.player.PlayerEntity}.
 */
public abstract class LivingEntity extends Entity implements Attackable, ServerWaypoint {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String ACTIVE_EFFECTS_KEY = "active_effects";
	public static final String ATTRIBUTES_KEY = "attributes";
	public static final String SLEEPING_POS_KEY = "sleeping_pos";
	public static final String EQUIPMENT_KEY = "equipment";
	public static final String BRAIN_KEY = "Brain";
	public static final String FALL_FLYING_KEY = "FallFlying";
	public static final String HURT_TIME_KEY = "HurtTime";
	public static final String DEATH_TIME_KEY = "DeathTime";
	public static final String HURT_BY_TIMESTAMP_KEY = "HurtByTimestamp";
	public static final String HEALTH_KEY = "Health";
	private static final Identifier POWDER_SNOW_SPEED_MODIFIER_ID = Identifier.ofVanilla("powder_snow");
	private static final Identifier SPRINTING_SPEED_MODIFIER_ID = Identifier.ofVanilla("sprinting");
	private static final EntityAttributeModifier SPRINTING_SPEED_BOOST = new EntityAttributeModifier(
			SPRINTING_SPEED_MODIFIER_ID, 0.3F, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
	);
	public static final int EQUIPMENT_SLOT_ID = 98;
	public static final int ENTITY_STATUS_HURT = 100;
	public static final int ENTITY_STATUS_DEATH_SOUND = 105;
	public static final int ENTITY_STATUS_DEATH_PARTICLES = 106;
	public static final int ENTITY_STATUS_HURT_THORN = 100;
	private static final int HURT_SOUND_COOLDOWN_TICKS = 40;
	public static final double STEP_SOUND_MIN_SPEED = 0.003;
	public static final double GRAVITY = 0.08;
	public static final int DEATH_TICKS = 20;
	protected static final float MOVEMENT_SPEED_MULTIPLIER = 0.98F;
	private static final int ELYTRA_FLIGHT_TICKS_THRESHOLD = 10;
	private static final int ELYTRA_FLIGHT_MIN_SPEED = 2;
	public static final float JUMP_VELOCITY = 0.42F;
	protected static final float SWIM_SPEED_MULTIPLIER = 0.4F;
	protected static final int SWIM_ANIMATION_TICKS = 20;
	private static final double MAX_ENTITY_VIEWING_DISTANCE = 128.0;
	protected static final int USING_ITEM_FLAG = 1;
	protected static final int OFF_HAND_ACTIVE_FLAG = 2;
	protected static final int USING_RIPTIDE_FLAG = 4;
	protected static final TrackedData<Byte>
			LIVING_FLAGS =
			DataTracker.registerData(LivingEntity.class, TrackedDataHandlerRegistry.BYTE);
	private static final TrackedData<Float>
			HEALTH =
			DataTracker.registerData(LivingEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<List<ParticleEffect>>
			POTION_SWIRLS =
			DataTracker.registerData(LivingEntity.class, TrackedDataHandlerRegistry.PARTICLE_LIST);
	private static final TrackedData<Boolean>
			POTION_SWIRLS_AMBIENT =
			DataTracker.registerData(LivingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Integer>
			STUCK_ARROW_COUNT =
			DataTracker.registerData(LivingEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer>
			STINGER_COUNT =
			DataTracker.registerData(LivingEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Optional<BlockPos>> SLEEPING_POSITION = DataTracker.registerData(
			LivingEntity.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS
	);
	private static final int FREEZE_DAMAGE_INTERVAL_TICKS = 15;
	protected static final EntityDimensions
			SLEEPING_DIMENSIONS =
			EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(0.2F);
	public static final float BABY_SCALE_FACTOR = 0.5F;
	public static final float KNOCKBACK_RESISTANCE_THRESHOLD = 0.5F;
	private static final float ELYTRA_GRAVITY = 0.04F;
	public static final Predicate<LivingEntity> NOT_WEARING_GAZE_DISGUISE_PREDICATE = entity -> {
		if (entity instanceof PlayerEntity playerEntity) {
			ItemStack itemStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
			return !itemStack.isIn(ItemTags.GAZE_DISGUISE_EQUIPMENT);
		}
		else {
			return true;
		}
	};
	private static final Dynamic<?> BRAIN = new Dynamic(JavaOps.INSTANCE, Map.of("memories", Map.of()));
	private final AttributeContainer attributes;
	private final DamageTracker damageTracker = new DamageTracker(this);
	private final Map<RegistryEntry<StatusEffect>, StatusEffectInstance> activeStatusEffects = Maps.newHashMap();
	private final Map<EquipmentSlot, ItemStack>
			lastEquipmentStacks =
			Util.mapEnum(EquipmentSlot.class, slot -> ItemStack.EMPTY);
	public boolean handSwinging;
	private boolean noDrag = false;
	public Hand preferredHand;
	public int handSwingTicks;
	public int stuckArrowTimer;
	public int stuckStingerTimer;
	public int hurtTime;
	public int maxHurtTime;
	public int deathTime;
	public float lastHandSwingProgress;
	public float handSwingProgress;
	protected int ticksSinceLastAttack;
	protected int ticksSinceHandEquipping;
	public final LimbAnimator limbAnimator = new LimbAnimator();
	public float bodyYaw;
	public float lastBodyYaw;
	public float headYaw;
	public float lastHeadYaw;
	public final ElytraFlightController elytraFlightController = new ElytraFlightController(this);
	protected @Nullable LazyEntityReference<PlayerEntity> attackingPlayer;
	public int playerHitTimer;
	protected boolean dead;
	protected int despawnCounter;
	protected float lastDamageTaken;
	protected boolean jumping;
	public float sidewaysSpeed;
	public float upwardSpeed;
	public float forwardSpeed;
	protected PositionInterpolator interpolator = new PositionInterpolator(this);
	protected double serverHeadYaw;
	protected int headTrackingIncrements;
	private boolean effectsChanged = true;
	private @Nullable LazyEntityReference<LivingEntity> attackerReference;
	private int lastAttackedTime;
	private @Nullable LivingEntity attacking;
	private int lastAttackTime;
	private float movementSpeed;
	private int jumpingCooldown;
	private float absorptionAmount;
	protected ItemStack activeItemStack = ItemStack.EMPTY;
	protected int itemUseTimeLeft;
	protected int glidingTicks;
	private long lastKineticAttackTime = -2147483648L;
	private BlockPos lastBlockPos;
	private Optional<BlockPos> climbingPos = Optional.empty();
	private @Nullable DamageSource lastDamageSource;
	private long lastDamageTime;
	protected int riptideTicks;
	protected float riptideAttackDamage;
	protected @Nullable ItemStack riptideStack;
	protected @Nullable Object2LongMap<Entity> piercingCooldowns;
	private float leaningPitch;
	private float lastLeaningPitch;
	protected Brain<?> brain;
	private boolean experienceDroppingDisabled;
	private final EnumMap<EquipmentSlot, Reference2ObjectMap<Enchantment, Set<EnchantmentLocationBasedEffect>>>
			locationBasedEnchantmentEffects =
			new EnumMap<>(
					EquipmentSlot.class
			);
	protected final EntityEquipment equipment;
	private Waypoint.Config waypointConfig = new Waypoint.Config();

	protected LivingEntity(EntityType<? extends LivingEntity> entityType, World world) {
		super(entityType, world);
		this.attributes = new AttributeContainer(DefaultAttributeRegistry.get(entityType));
		this.setHealth(this.getMaxHealth());
		this.equipment = this.createEquipment();
		this.intersectionChecked = true;
		this.refreshPosition();
		this.setYaw(this.random.nextFloat() * (float) (Math.PI * 2));
		this.headYaw = this.getYaw();
		this.brain = this.deserializeBrain(BRAIN);
	}

	@Override
	public @Nullable LivingEntity getEntity() {
		return this;
	}

	@Contract(pure = true)
	protected EntityEquipment createEquipment() {
		return new EntityEquipment();
	}

	public Brain<?> getBrain() {
		return this.brain;
	}

	protected Brain.Profile<?> createBrainProfile() {
		return Brain.createProfile(ImmutableList.of(), ImmutableList.of());
	}

	protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
		return this.createBrainProfile().deserialize(dynamic);
	}

	@Override
	public void kill(ServerWorld world) {
		this.damage(world, this.getDamageSources().genericKill(), Float.MAX_VALUE);
	}

	public boolean canTarget(EntityType<?> type) {
		return true;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(LIVING_FLAGS, (byte) 0);
		builder.add(POTION_SWIRLS, List.of());
		builder.add(POTION_SWIRLS_AMBIENT, false);
		builder.add(STUCK_ARROW_COUNT, 0);
		builder.add(STINGER_COUNT, 0);
		builder.add(HEALTH, 1.0F);
		builder.add(SLEEPING_POSITION, Optional.empty());
	}

	public static DefaultAttributeContainer.Builder createLivingAttributes() {
		return DefaultAttributeContainer.builder()
		                                .add(EntityAttributes.MAX_HEALTH)
		                                .add(EntityAttributes.KNOCKBACK_RESISTANCE)
		                                .add(EntityAttributes.MOVEMENT_SPEED)
		                                .add(EntityAttributes.ARMOR)
		                                .add(EntityAttributes.ARMOR_TOUGHNESS)
		                                .add(EntityAttributes.MAX_ABSORPTION)
		                                .add(EntityAttributes.STEP_HEIGHT)
		                                .add(EntityAttributes.SCALE)
		                                .add(EntityAttributes.GRAVITY)
		                                .add(EntityAttributes.SAFE_FALL_DISTANCE)
		                                .add(EntityAttributes.FALL_DAMAGE_MULTIPLIER)
		                                .add(EntityAttributes.JUMP_STRENGTH)
		                                .add(EntityAttributes.OXYGEN_BONUS)
		                                .add(EntityAttributes.BURNING_TIME)
		                                .add(EntityAttributes.EXPLOSION_KNOCKBACK_RESISTANCE)
		                                .add(EntityAttributes.WATER_MOVEMENT_EFFICIENCY)
		                                .add(EntityAttributes.MOVEMENT_EFFICIENCY)
		                                .add(EntityAttributes.ATTACK_KNOCKBACK)
		                                .add(EntityAttributes.CAMERA_DISTANCE)
		                                .add(EntityAttributes.WAYPOINT_TRANSMIT_RANGE);
	}

	@Override
	protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
		if (!this.isTouchingWater()) {
			this.checkWaterState();
		}

		if (this.getEntityWorld() instanceof ServerWorld serverWorld && onGround && this.fallDistance > 0.0) {
			this.applyMovementEffects(serverWorld, landedPosition);
			double unsafeFallDist = Math.max(0, MathHelper.floor(this.getUnsafeFallDistance(this.fallDistance)));

			if (unsafeFallDist > 0.0 && !state.isAir()) {
				double particleX = this.getX();
				double particleY = this.getY();
				double particleZ = this.getZ();
				BlockPos blockPos = this.getBlockPos();

				if (landedPosition.getX() != blockPos.getX() || landedPosition.getZ() != blockPos.getZ()) {
					double offsetX = particleX - landedPosition.getX() - 0.5;
					double offsetZ = particleZ - landedPosition.getZ() - 0.5;
					double maxOffset = Math.max(Math.abs(offsetX), Math.abs(offsetZ));
					particleX = landedPosition.getX() + 0.5 + offsetX / maxOffset * 0.5;
					particleZ = landedPosition.getZ() + 0.5 + offsetZ / maxOffset * 0.5;
				}

				double particleSize = Math.min(0.2F + unsafeFallDist / 15.0, 2.5);
				int particleCount = (int) (150.0 * particleSize);
				serverWorld.spawnParticles(
						new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
						particleX,
						particleY,
						particleZ,
						particleCount,
						0.0,
						0.0,
						0.0,
						0.15F
				);
			}
		}

		super.fall(heightDifference, onGround, state, landedPosition);

		if (onGround) {
			this.climbingPos = Optional.empty();
		}
	}

	public boolean canBreatheInWater() {
		return this.getType().isIn(EntityTypeTags.CAN_BREATHE_UNDER_WATER);
	}

	public float getLeaningPitch(float tickProgress) {
		return MathHelper.lerp(tickProgress, this.lastLeaningPitch, this.leaningPitch);
	}

	public boolean hasLandedInFluid() {
		return this.getVelocity().getY() < 1.0E-5F && this.isInFluid();
	}

	@Override
	public void baseTick() {
		lastHandSwingProgress = handSwingProgress;

		if (firstUpdate) {
			getSleepingPosition().ifPresent(this::setPositionInBed);
		}

		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			EnchantmentHelper.onTick(serverWorld, this);
		}

		super.baseTick();
		Profiler profiler = Profilers.get();
		profiler.push("livingEntityBaseTick");

		if (isAlive() && getEntityWorld() instanceof ServerWorld serverWorld2) {
			boolean isPlayer = this instanceof PlayerEntity;

			if (isInsideWall()) {
				damage(serverWorld2, getDamageSources().inWall(), 1.0F);
			}
			else if (isPlayer && !serverWorld2.getWorldBorder().contains(getBoundingBox())) {
				double distanceInsideBorder = serverWorld2.getWorldBorder().getDistanceInsideBorder(this)
						+ serverWorld2.getWorldBorder().getSafeZone();

				if (distanceInsideBorder < 0.0) {
					double damagePerBlock = serverWorld2.getWorldBorder().getDamagePerBlock();
					if (damagePerBlock > 0.0) {
						damage(
								serverWorld2,
								getDamageSources().outsideBorder(),
								Math.max(1, MathHelper.floor(-distanceInsideBorder * damagePerBlock))
						);
					}
				}
			}

			if (isSubmergedIn(FluidTags.WATER)
					&& !serverWorld2
					.getBlockState(BlockPos.ofFloored(getX(), getEyeY(), getZ()))
					.isOf(Blocks.BUBBLE_COLUMN)) {
				boolean shouldConsumeAir = !canBreatheInWater()
						&& !StatusEffectUtil.hasWaterBreathing(this)
						&& (!isPlayer || !((PlayerEntity) this).getAbilities().invulnerable);

				if (shouldConsumeAir) {
					setAir(getNextAirUnderwater(getAir()));

					if (shouldDrown()) {
						setAir(0);
						serverWorld2.sendEntityStatus(this, (byte) 67);
						damage(serverWorld2, getDamageSources().drown(), 2.0F);
					}
				}
				else if (getAir() < getMaxAir() && StatusEffectUtil.canIncreaseAirOnLand(this)) {
					setAir(getNextAirOnLand(getAir()));
				}

				if (hasVehicle() && getVehicle() != null && getVehicle().shouldDismountUnderwater()) {
					stopRiding();
				}
			}
			else if (getAir() < getMaxAir()) {
				setAir(getNextAirOnLand(getAir()));
			}

			BlockPos blockPos = getBlockPos();
			if (!Objects.equal(lastBlockPos, blockPos)) {
				lastBlockPos = blockPos;
				applyMovementEffects(serverWorld2, blockPos);
			}
		}

		if (this.hurtTime > 0) {
			this.hurtTime--;
		}

		if (this.timeUntilRegen > 0 && !(this instanceof ServerPlayerEntity)) {
			this.timeUntilRegen--;
		}

		if (this.isDead() && this.getEntityWorld().shouldUpdatePostDeath(this)) {
			this.updatePostDeath();
		}

		if (this.playerHitTimer > 0) {
			this.playerHitTimer--;
		}
		else {
			this.attackingPlayer = null;
		}

		if (this.attacking != null && !this.attacking.isAlive()) {
			this.attacking = null;
		}

		LivingEntity livingEntity = this.getAttacker();
		if (livingEntity != null) {
			if (!livingEntity.isAlive()) {
				this.setAttacker(null);
			}
			else if (this.age - this.lastAttackedTime > 100) {
				this.setAttacker(null);
			}
		}

		this.tickStatusEffects();
		this.lastHeadYaw = this.headYaw;
		this.lastBodyYaw = this.bodyYaw;
		this.lastYaw = this.getYaw();
		this.lastPitch = this.getPitch();
		profiler.pop();
	}

	protected boolean shouldDrown() {
		return this.getAir() <= -20;
	}

	@Override
	protected float getVelocityMultiplier() {
		return MathHelper.lerp(
				(float) this.getAttributeValue(EntityAttributes.MOVEMENT_EFFICIENCY),
				super.getVelocityMultiplier(),
				1.0F
		);
	}

	public float getLuck() {
		return 0.0F;
	}

	protected void removePowderSnowSlow() {
		EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
		if (entityAttributeInstance != null) {
			if (entityAttributeInstance.getModifier(POWDER_SNOW_SPEED_MODIFIER_ID) != null) {
				entityAttributeInstance.removeModifier(POWDER_SNOW_SPEED_MODIFIER_ID);
			}
		}
	}

	protected void addPowderSnowSlowIfNeeded() {
		if (getLandingBlockState().isAir()) {
			return;
		}

		if (getFrozenTicks() <= 0) {
			return;
		}

		EntityAttributeInstance speedAttr = getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);

		if (speedAttr == null) {
			return;
		}

		float slowAmount = -0.05F * getFreezingScale();
		speedAttr.addTemporaryModifier(
				new EntityAttributeModifier(
						POWDER_SNOW_SPEED_MODIFIER_ID,
						slowAmount,
						EntityAttributeModifier.Operation.ADD_VALUE
				)
		);
	}

	protected void applyMovementEffects(ServerWorld world, BlockPos pos) {
		EnchantmentHelper.applyLocationBasedEffects(world, this);
	}

	public boolean isBaby() {
		return false;
	}

	public float getScaleFactor() {
		return this.isBaby() ? 0.5F : 1.0F;
	}

	public final float getScale() {
		AttributeContainer attributeContainer = this.getAttributes();
		return attributeContainer == null ? 1.0F
		                                  : this.clampScale((float) attributeContainer.getValue(EntityAttributes.SCALE));
	}

	protected float clampScale(float scale) {
		return scale;
	}

	public boolean shouldSwimInFluids() {
		return true;
	}

	protected void updatePostDeath() {
		this.deathTime++;
		if (this.deathTime >= DEATH_TICKS && !this.getEntityWorld().isClient() && !this.isRemoved()) {
			this.getEntityWorld().sendEntityStatus(this, (byte) 60);
			this.remove(Entity.RemovalReason.KILLED);
		}
	}

	public boolean shouldDropExperience() {
		return !this.isBaby();
	}

	protected boolean shouldDropLoot(ServerWorld world) {
		return !this.isBaby() && world.getGameRules().getValue(GameRules.DO_MOB_LOOT);
	}

	protected int getNextAirUnderwater(int air) {
		EntityAttributeInstance oxygenBonusAttr = getAttributeInstance(EntityAttributes.OXYGEN_BONUS);
		double oxygenBonus = oxygenBonusAttr != null ? oxygenBonusAttr.getValue() : 0.0;

		return oxygenBonus > 0.0 && random.nextDouble() >= 1.0 / (oxygenBonus + 1.0) ? air : air - 1;
	}

	protected int getNextAirOnLand(int air) {
		return Math.min(air + 4, this.getMaxAir());
	}

	public final int getExperienceToDrop(ServerWorld world, @Nullable Entity attacker) {
		return EnchantmentHelper.getMobExperience(world, attacker, this, this.getExperienceToDrop(world));
	}

	protected int getExperienceToDrop(ServerWorld world) {
		return 0;
	}

	protected boolean shouldAlwaysDropExperience() {
		return false;
	}

	public @Nullable LivingEntity getAttacker() {
		return LazyEntityReference.getLivingEntity(this.attackerReference, this.getEntityWorld());
	}

	public @Nullable PlayerEntity getAttackingPlayer() {
		return LazyEntityReference.getPlayerEntity(this.attackingPlayer, this.getEntityWorld());
	}

	@Override
	public LivingEntity getLastAttacker() {
		return this.getAttacker();
	}

	public int getLastAttackedTime() {
		return this.lastAttackedTime;
	}

	public void setAttacking(PlayerEntity attackingPlayer, int playerHitTimer) {
		this.setAttacking(LazyEntityReference.of(attackingPlayer), playerHitTimer);
	}

	public void setAttacking(UUID attackingPlayer, int playerHitTimer) {
		this.setAttacking(LazyEntityReference.ofUUID(attackingPlayer), playerHitTimer);
	}

	private void setAttacking(LazyEntityReference<PlayerEntity> attackingPlayer, int playerHitTimer) {
		this.attackingPlayer = attackingPlayer;
		this.playerHitTimer = playerHitTimer;
	}

	public void setAttacker(@Nullable LivingEntity attacker) {
		this.attackerReference = LazyEntityReference.of(attacker);
		this.lastAttackedTime = this.age;
	}

	public @Nullable LivingEntity getAttacking() {
		return this.attacking;
	}

	public int getLastAttackTime() {
		return this.lastAttackTime;
	}

	public void onAttacking(Entity target) {
		attacking = target instanceof LivingEntity livingTarget ? livingTarget : null;
		lastAttackTime = age;
	}

	public int getDespawnCounter() {
		return this.despawnCounter;
	}

	public void setDespawnCounter(int despawnCounter) {
		this.despawnCounter = despawnCounter;
	}

	public boolean hasNoDrag() {
		return this.noDrag;
	}

	public void setNoDrag(boolean noDrag) {
		this.noDrag = noDrag;
	}

	protected boolean isArmorSlot(EquipmentSlot slot) {
		return true;
	}

	public void onEquipStack(EquipmentSlot slot, ItemStack oldStack, ItemStack newStack) {
		if (!this.getEntityWorld().isClient() && !this.isSpectator()) {
			if (!ItemStack.areItemsAndComponentsEqual(oldStack, newStack) && !this.firstUpdate) {
				EquippableComponent equippableComponent = newStack.get(DataComponentTypes.EQUIPPABLE);
				if (!this.isSilent() && equippableComponent != null && slot == equippableComponent.slot()) {
					this.getEntityWorld()
					    .playSound(
							    null,
							    this.getX(),
							    this.getY(),
							    this.getZ(),
							    this.getEquipSound(slot, newStack, equippableComponent),
							    this.getSoundCategory(),
							    1.0F,
							    1.0F,
							    this.random.nextLong()
					    );
				}

				if (this.isArmorSlot(slot)) {
					this.emitGameEvent(equippableComponent != null ? GameEvent.EQUIP : GameEvent.UNEQUIP);
				}
			}
		}
	}

	protected RegistryEntry<SoundEvent> getEquipSound(
			EquipmentSlot slot,
			ItemStack stack,
			EquippableComponent equippableComponent
	) {
		return equippableComponent.equipSound();
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		if ((reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED)
				&& this.getEntityWorld() instanceof ServerWorld serverWorld) {
			this.onRemoval(serverWorld, reason);
		}

		super.remove(reason);
		this.brain.forgetAll();
	}

	@Override
	public void onRemove(Entity.RemovalReason reason) {
		super.onRemove(reason);
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			serverWorld.getWaypointHandler().onUntrack((ServerWaypoint) this);
		}
	}

	protected void onRemoval(ServerWorld world, Entity.RemovalReason reason) {
		for (StatusEffectInstance statusEffectInstance : this.getStatusEffects()) {
			statusEffectInstance.onEntityRemoval(world, this, reason);
		}

		this.activeStatusEffects.clear();
	}

	@Override
	protected void writeCustomData(WriteView view) {
		view.putFloat("Health", this.getHealth());
		view.putShort("HurtTime", (short) this.hurtTime);
		view.putInt("HurtByTimestamp", this.lastAttackedTime);
		view.putShort("DeathTime", (short) this.deathTime);
		view.putFloat("AbsorptionAmount", this.getAbsorptionAmount());
		view.put("attributes", EntityAttributeInstance.Packed.LIST_CODEC, this.getAttributes().pack());
		if (!this.activeStatusEffects.isEmpty()) {
			view.put(
					"active_effects",
					StatusEffectInstance.CODEC.listOf(),
					List.copyOf(this.activeStatusEffects.values())
			);
		}

		view.putBoolean("FallFlying", this.isGliding());
		this.getSleepingPosition().ifPresent(pos -> view.put("sleeping_pos", BlockPos.CODEC, pos));
		DataResult<Dynamic<?>>
				dataResult =
				this.brain.encode(NbtOps.INSTANCE).map(nbtElement -> new Dynamic(NbtOps.INSTANCE, nbtElement));
		dataResult.resultOrPartial(LOGGER::error).ifPresent(brain -> view.put("Brain", Codec.PASSTHROUGH, brain));
		if (this.attackingPlayer != null) {
			this.attackingPlayer.writeData(view, "last_hurt_by_player");
			view.putInt("last_hurt_by_player_memory_time", this.playerHitTimer);
		}

		if (this.attackerReference != null) {
			this.attackerReference.writeData(view, "last_hurt_by_mob");
			view.putInt("ticks_since_last_hurt_by_mob", this.age - this.lastAttackedTime);
		}

		if (!this.equipment.isEmpty()) {
			view.put("equipment", EntityEquipment.CODEC, this.equipment);
		}

		if (this.waypointConfig.hasCustomStyle()) {
			view.put("locator_bar_icon", Waypoint.Config.CODEC, this.waypointConfig);
		}
	}

	public @Nullable ItemEntity dropItem(ItemStack stack, boolean dropAtSelf, boolean retainOwnership) {
		if (stack.isEmpty()) {
			return null;
		}
		else if (this.getEntityWorld().isClient()) {
			this.swingHand(Hand.MAIN_HAND);
			return null;
		}
		else {
			ItemEntity itemEntity = this.createItemEntity(stack, dropAtSelf, retainOwnership);
			if (itemEntity != null) {
				this.getEntityWorld().spawnEntity(itemEntity);
			}

			return itemEntity;
		}
	}

	@Override
	protected void readCustomData(ReadView view) {
		this.setAbsorptionAmountUnclamped(view.getFloat("AbsorptionAmount", 0.0F));
		if (this.getEntityWorld() != null && !this.getEntityWorld().isClient()) {
			view
					.<List<EntityAttributeInstance.Packed>>read("attributes", EntityAttributeInstance.Packed.LIST_CODEC)
					.ifPresent(this.getAttributes()::unpack);
		}

		List<StatusEffectInstance>
				list =
				view
						.<List<StatusEffectInstance>>read("active_effects", StatusEffectInstance.CODEC.listOf())
						.orElse(List.of());
		this.activeStatusEffects.clear();

		for (StatusEffectInstance statusEffectInstance : list) {
			this.activeStatusEffects.put(statusEffectInstance.getEffectType(), statusEffectInstance);
			this.effectsChanged = true;
		}

		this.setHealth(view.getFloat("Health", this.getMaxHealth()));
		this.hurtTime = view.getShort("HurtTime", (short) 0);
		this.deathTime = view.getShort("DeathTime", (short) 0);
		this.lastAttackedTime = view.getInt("HurtByTimestamp", 0);
		view.getOptionalString("Team").ifPresent(team -> {
			Scoreboard scoreboard = this.getEntityWorld().getScoreboard();
			Team team2 = scoreboard.getTeam(team);
			boolean bl = team2 != null && scoreboard.addScoreHolderToTeam(this.getUuidAsString(), team2);
			if (!bl) {
				LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", team);
			}
		});
		this.setFlag(7, view.getBoolean("FallFlying", false));
		view.<BlockPos>read("sleeping_pos", BlockPos.CODEC).ifPresentOrElse(
				pos -> {
					this.setSleepingPosition(pos);
					this.dataTracker.set(POSE, EntityPose.SLEEPING);
					if (!this.firstUpdate) {
						this.setPositionInBed(pos);
					}
				}, this::clearSleepingPosition
		);
		view.<Dynamic<?>>read("Brain", Codec.PASSTHROUGH).ifPresent(brain -> this.brain = this.deserializeBrain(brain));
		this.attackingPlayer = LazyEntityReference.fromData(view, "last_hurt_by_player");
		this.playerHitTimer = view.getInt("last_hurt_by_player_memory_time", 0);
		this.attackerReference = LazyEntityReference.fromData(view, "last_hurt_by_mob");
		this.lastAttackedTime = view.getInt("ticks_since_last_hurt_by_mob", 0) + this.age;
		this.equipment.copyFrom(view
				.<EntityEquipment>read("equipment", EntityEquipment.CODEC)
				.orElseGet(EntityEquipment::new));
		this.waypointConfig =
				view.<Waypoint.Config>read("locator_bar_icon", Waypoint.Config.CODEC).orElseGet(Waypoint.Config::new);
	}

	@Override
	public void beforePacketsSent() {
		super.beforePacketsSent();
		this.handleEffectsChanged();
	}

	protected void tickStatusEffects() {
		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			Iterator<RegistryEntry<StatusEffect>> iterator = activeStatusEffects.keySet().iterator();

			try {
				while (iterator.hasNext()) {
					RegistryEntry<StatusEffect> effectType = iterator.next();
					StatusEffectInstance effectInstance = activeStatusEffects.get(effectType);

					if (!effectInstance.update(
							serverWorld,
							this,
							() -> onStatusEffectUpgraded(effectInstance, true, null)
					)) {
						iterator.remove();
						onStatusEffectsRemoved(List.of(effectInstance));
					}
					else if (effectInstance.getDuration() % 600 == 0) {
						onStatusEffectUpgraded(effectInstance, false, null);
					}
				}
			}
			catch (ConcurrentModificationException ignored) {
			}
		}
		else {
			for (StatusEffectInstance effectInstance : activeStatusEffects.values()) {
				effectInstance.tickClient();
			}

			List<ParticleEffect> potionSwirls = dataTracker.get(POTION_SWIRLS);

			if (!potionSwirls.isEmpty()) {
				boolean isAmbient = dataTracker.get(POTION_SWIRLS_AMBIENT);
				int visibilityFactor = isInvisible() ? FREEZE_DAMAGE_INTERVAL_TICKS : 4;
				int ambientFactor = isAmbient ? 5 : 1;

				if (random.nextInt(visibilityFactor * ambientFactor) == 0) {
					getEntityWorld()
						.addParticleClient(
							Util.getRandom(potionSwirls, random),
							getParticleX(0.5),
							getRandomBodyY(),
							getParticleZ(0.5),
							1.0,
							1.0,
							1.0
						);
				}
			}
		}
	}

	private void handleEffectsChanged() {
		if (this.effectsChanged) {
			this.updatePotionVisibility();
			this.updateGlowing();
			this.effectsChanged = false;
		}
	}

	protected void updatePotionVisibility() {
		if (this.activeStatusEffects.isEmpty()) {
			this.clearPotionSwirls();
			this.setInvisible(false);
		}
		else {
			this.setInvisible(this.hasStatusEffect(StatusEffects.INVISIBILITY));
			this.updatePotionSwirls();
		}
	}

	private void updatePotionSwirls() {
		List<ParticleEffect> list = this.activeStatusEffects
				.values()
				.stream()
				.filter(StatusEffectInstance::shouldShowParticles)
				.map(StatusEffectInstance::createParticle)
				.toList();
		this.dataTracker.set(POTION_SWIRLS, list);
		this.dataTracker.set(POTION_SWIRLS_AMBIENT, containsOnlyAmbientEffects(this.activeStatusEffects.values()));
	}

	private void updateGlowing() {
		boolean glowing = isGlowing();

		if (getFlag(6) != glowing) {
			setFlag(6, glowing);
		}
	}

	public double getAttackDistanceScalingFactor(@Nullable Entity entity) {
		double factor = 1.0;

		if (isSneaky()) {
			factor *= 0.8;
		}

		if (isInvisible()) {
			float armorVisibility = Math.max(getArmorVisibility(), 0.1F);
			factor *= 0.7 * armorVisibility;
		}

		if (entity != null) {
			ItemStack headStack = getEquippedStack(EquipmentSlot.HEAD);
			EntityType<?> entityType = entity.getType();

			if (entityType == EntityType.SKELETON && headStack.isOf(Items.SKELETON_SKULL)
					|| entityType == EntityType.ZOMBIE && headStack.isOf(Items.ZOMBIE_HEAD)
					|| entityType == EntityType.PIGLIN && headStack.isOf(Items.PIGLIN_HEAD)
					|| entityType == EntityType.PIGLIN_BRUTE && headStack.isOf(Items.PIGLIN_HEAD)
					|| entityType == EntityType.CREEPER && headStack.isOf(Items.CREEPER_HEAD)) {
				factor *= 0.5;
			}
		}

		return factor;
	}

	public boolean canTarget(LivingEntity target) {
		return !(target instanceof PlayerEntity && this.getEntityWorld().getDifficulty() == Difficulty.PEACEFUL)
			&& target.canTakeDamage();
	}

	public boolean canTakeDamage() {
		return !this.isInvulnerable() && this.isPartOfGame();
	}

	public boolean isPartOfGame() {
		return !this.isSpectator() && this.isAlive();
	}

	public static boolean containsOnlyAmbientEffects(Collection<StatusEffectInstance> effects) {
		for (StatusEffectInstance statusEffectInstance : effects) {
			if (statusEffectInstance.shouldShowParticles() && !statusEffectInstance.isAmbient()) {
				return false;
			}
		}

		return true;
	}

	protected void clearPotionSwirls() {
		this.dataTracker.set(POTION_SWIRLS, List.of());
	}

	public boolean clearStatusEffects() {
		if (this.getEntityWorld().isClient()) {
			return false;
		}
		else if (this.activeStatusEffects.isEmpty()) {
			return false;
		}
		else {
			Map<RegistryEntry<StatusEffect>, StatusEffectInstance> map = Maps.newHashMap(this.activeStatusEffects);
			this.activeStatusEffects.clear();
			this.onStatusEffectsRemoved(map.values());
			return true;
		}
	}

	public Collection<StatusEffectInstance> getStatusEffects() {
		return this.activeStatusEffects.values();
	}

	public Map<RegistryEntry<StatusEffect>, StatusEffectInstance> getActiveStatusEffects() {
		return this.activeStatusEffects;
	}

	public boolean hasStatusEffect(RegistryEntry<StatusEffect> effect) {
		return this.activeStatusEffects.containsKey(effect);
	}

	public @Nullable StatusEffectInstance getStatusEffect(RegistryEntry<StatusEffect> effect) {
		return this.activeStatusEffects.get(effect);
	}

	public float getEffectFadeFactor(RegistryEntry<StatusEffect> effect, float tickProgress) {
		StatusEffectInstance statusEffectInstance = this.getStatusEffect(effect);
		return statusEffectInstance != null ? statusEffectInstance.getFadeFactor(this, tickProgress) : 0.0F;
	}

	public final boolean addStatusEffect(StatusEffectInstance effect) {
		return this.addStatusEffect(effect, null);
	}

	public boolean addStatusEffect(StatusEffectInstance effect, @Nullable Entity source) {
		if (!this.canHaveStatusEffect(effect)) {
			return false;
		}

		StatusEffectInstance existing = this.activeStatusEffects.get(effect.getEffectType());
		boolean applied = false;

		if (existing == null) {
			this.activeStatusEffects.put(effect.getEffectType(), effect);
			this.onStatusEffectApplied(effect, source);
			applied = true;
			effect.playApplySound(this);
		}
		else if (existing.upgrade(effect)) {
			this.onStatusEffectUpgraded(existing, true, source);
			applied = true;
		}

		effect.onApplied(this);
		return applied;
	}

	public boolean canHaveStatusEffect(StatusEffectInstance effect) {
		if (this.getType().isIn(EntityTypeTags.IMMUNE_TO_INFESTED)) {
			return !effect.equals(StatusEffects.INFESTED);
		}

		if (this.getType().isIn(EntityTypeTags.IMMUNE_TO_OOZING)) {
			return !effect.equals(StatusEffects.OOZING);
		}

		return !this.getType().isIn(EntityTypeTags.IGNORES_POISON_AND_REGEN)
			|| (!effect.equals(StatusEffects.REGENERATION) && !effect.equals(StatusEffects.POISON));
	}

	public void setStatusEffect(StatusEffectInstance effect, @Nullable Entity source) {
		if (this.canHaveStatusEffect(effect)) {
			StatusEffectInstance statusEffectInstance = this.activeStatusEffects.put(effect.getEffectType(), effect);
			if (statusEffectInstance == null) {
				this.onStatusEffectApplied(effect, source);
			}
			else {
				effect.copyFadingFrom(statusEffectInstance);
				this.onStatusEffectUpgraded(effect, true, source);
			}
		}
	}

	public boolean hasInvertedHealingAndHarm() {
		return this.getType().isIn(EntityTypeTags.INVERTED_HEALING_AND_HARM);
	}

	public final @Nullable StatusEffectInstance removeStatusEffectInternal(RegistryEntry<StatusEffect> effect) {
		return this.activeStatusEffects.remove(effect);
	}

	public boolean removeStatusEffect(RegistryEntry<StatusEffect> effect) {
		StatusEffectInstance statusEffectInstance = this.removeStatusEffectInternal(effect);
		if (statusEffectInstance != null) {
			this.onStatusEffectsRemoved(List.of(statusEffectInstance));
			return true;
		}
		else {
			return false;
		}
	}

	protected void onStatusEffectApplied(StatusEffectInstance effect, @Nullable Entity source) {
		if (!this.getEntityWorld().isClient()) {
			this.effectsChanged = true;
			effect.getEffectType().value().onApplied(this.getAttributes(), effect.getAmplifier());
			this.sendEffectToControllingPlayer(effect);
		}
	}

	public void sendEffectToControllingPlayer(StatusEffectInstance effect) {
		for (Entity entity : this.getPassengerList()) {
			if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
				serverPlayerEntity.networkHandler.sendPacket(new EntityStatusEffectS2CPacket(
						this.getId(),
						effect,
						false
				));
			}
		}
	}

	protected void onStatusEffectUpgraded(StatusEffectInstance effect, boolean reapplyEffect, @Nullable Entity source) {
		if (!this.getEntityWorld().isClient()) {
			this.effectsChanged = true;
			if (reapplyEffect) {
				StatusEffect statusEffect = effect.getEffectType().value();
				statusEffect.onRemoved(this.getAttributes());
				statusEffect.onApplied(this.getAttributes(), effect.getAmplifier());
				this.updateAttributes();
			}

			this.sendEffectToControllingPlayer(effect);
		}
	}

	protected void onStatusEffectsRemoved(Collection<StatusEffectInstance> effects) {
		if (!this.getEntityWorld().isClient()) {
			this.effectsChanged = true;

			for (StatusEffectInstance statusEffectInstance : effects) {
				statusEffectInstance.getEffectType().value().onRemoved(this.getAttributes());

				for (Entity entity : this.getPassengerList()) {
					if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
						serverPlayerEntity.networkHandler.sendPacket(new RemoveEntityStatusEffectS2CPacket(
								this.getId(),
								statusEffectInstance.getEffectType()
						));
					}
				}
			}

			this.updateAttributes();
		}
	}

	private void updateAttributes() {
		Set<EntityAttributeInstance> set = this.getAttributes().getPendingUpdate();

		for (EntityAttributeInstance entityAttributeInstance : set) {
			this.updateAttribute(entityAttributeInstance.getAttribute());
		}

		set.clear();
	}

	protected void updateAttribute(RegistryEntry<EntityAttribute> attribute) {
		if (attribute.matches(EntityAttributes.MAX_HEALTH)) {
			float f = this.getMaxHealth();
			if (this.getHealth() > f) {
				this.setHealth(f);
			}
		}
		else if (attribute.matches(EntityAttributes.MAX_ABSORPTION)) {
			float f = this.getMaxAbsorption();
			if (this.getAbsorptionAmount() > f) {
				this.setAbsorptionAmount(f);
			}
		}
		else if (attribute.matches(EntityAttributes.SCALE)) {
			this.calculateDimensions();
		}
		else if (attribute.matches(EntityAttributes.WAYPOINT_TRANSMIT_RANGE)
				&& this.getEntityWorld() instanceof ServerWorld serverWorld) {
			ServerWaypointHandler serverWaypointHandler = serverWorld.getWaypointHandler();
			if (this.attributes.getValue(attribute) > 0.0) {
				serverWaypointHandler.onTrack((ServerWaypoint) this);
			}
			else {
				serverWaypointHandler.onUntrack((ServerWaypoint) this);
			}
		}
	}

	public void heal(float amount) {
		float f = this.getHealth();
		if (f > 0.0F) {
			this.setHealth(f + amount);
		}
	}

	public float getHealth() {
		return this.dataTracker.get(HEALTH);
	}

	public void setHealth(float health) {
		this.dataTracker.set(HEALTH, MathHelper.clamp(health, 0.0F, this.getMaxHealth()));
	}

	public boolean isDead() {
		return this.getHealth() <= 0.0F;
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (isInvulnerableTo(world, source)) {
			return false;
		}

		if (isDead()) {
			return false;
		}

		if (source.isIn(DamageTypeTags.IS_FIRE) && hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
			return false;
		}

		if (isSleeping()) {
			wakeUp();
		}

		despawnCounter = 0;

		if (amount < 0.0F) {
			amount = 0.0F;
		}

		ItemStack activeItem = getActiveItem();
		float blockedAmount = getDamageBlockedAmount(world, source, amount);
		amount -= blockedAmount;
		boolean wasBlocked = blockedAmount > 0.0F;

		if (source.isIn(DamageTypeTags.IS_FREEZING) && getType().isIn(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
			amount *= 5.0F;
		}

		if (source.isIn(DamageTypeTags.DAMAGES_HELMET) && !getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
			damageHelmet(source, amount);
			amount *= 0.75F;
		}

		if (Float.isNaN(amount) || Float.isInfinite(amount)) {
			amount = Float.MAX_VALUE;
		}

		boolean isFullDamage = true;

		if (timeUntilRegen > 10.0F && !source.isIn(DamageTypeTags.BYPASSES_COOLDOWN)) {
			if (amount <= lastDamageTaken) {
				return false;
			}

			applyDamage(world, source, amount - lastDamageTaken);
			lastDamageTaken = amount;
			isFullDamage = false;
		}
		else {
			lastDamageTaken = amount;
			timeUntilRegen = DEATH_TICKS;
			applyDamage(world, source, amount);
			maxHurtTime = ELYTRA_FLIGHT_TICKS_THRESHOLD;
			hurtTime = maxHurtTime;
		}

		becomeAngry(source);
		setAttackingPlayer(source);

		if (isFullDamage) {
			BlocksAttacksComponent blocksAttacksComponent = activeItem.get(DataComponentTypes.BLOCKS_ATTACKS);

			if (wasBlocked && blocksAttacksComponent != null) {
				blocksAttacksComponent.playBlockSound(world, this);
			}
			else {
				world.sendEntityDamage(this, source);
			}

			if (!source.isIn(DamageTypeTags.NO_IMPACT) && (!wasBlocked || amount > 0.0F)) {
				scheduleVelocityUpdate();
			}

			if (!source.isIn(DamageTypeTags.NO_KNOCKBACK)) {
				double knockbackX = 0.0;
				double knockbackZ = 0.0;

				if (source.getSource() instanceof ProjectileEntity projectileEntity) {
					DoubleDoubleImmutablePair knockback = projectileEntity.getKnockback(this, source);
					knockbackX = -knockback.leftDouble();
					knockbackZ = -knockback.rightDouble();
				}
				else if (source.getPosition() != null) {
					knockbackX = source.getPosition().getX() - getX();
					knockbackZ = source.getPosition().getZ() - getZ();
				}

				takeKnockback(SWIM_SPEED_MULTIPLIER, knockbackX, knockbackZ);

				if (!wasBlocked) {
					tiltScreen(knockbackX, knockbackZ);
				}
			}
		}

		if (isDead()) {
			if (!tryUseDeathProtector(source)) {
				if (isFullDamage) {
					playSound(getDeathSound());
					playThornsSound(source);
				}

				onDeath(source);
			}
		}
		else if (isFullDamage) {
			playHurtSound(source);
			playThornsSound(source);
		}

		boolean didDamage = !wasBlocked || amount > 0.0F;

		if (didDamage) {
			lastDamageSource = source;
			lastDamageTime = getEntityWorld().getTime();

			for (StatusEffectInstance effectInstance : getStatusEffects()) {
				effectInstance.onEntityDamage(world, this, source, amount);
			}
		}

		if (this instanceof ServerPlayerEntity serverPlayer) {
			Criteria.ENTITY_HURT_PLAYER.trigger(serverPlayer, source, amount, amount, wasBlocked);

			if (blockedAmount > 0.0F && blockedAmount < 3.4028235E37F) {
				serverPlayer.increaseStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(blockedAmount * 10.0F));
			}
		}

		if (source.getAttacker() instanceof ServerPlayerEntity attackerPlayer) {
			Criteria.PLAYER_HURT_ENTITY.trigger(attackerPlayer, this, source, amount, amount, wasBlocked);
		}

		return didDamage;
	}

	public float getDamageBlockedAmount(ServerWorld world, DamageSource source, float amount) {
		if (amount <= 0.0F) {
			return 0.0F;
		}

		ItemStack blockingItem = getBlockingItem();

		if (blockingItem == null) {
			return 0.0F;
		}

		BlocksAttacksComponent blocksAttacksComponent = blockingItem.get(DataComponentTypes.BLOCKS_ATTACKS);

		if (blocksAttacksComponent == null || blocksAttacksComponent.bypassedBy().map(source::isIn).orElse(false)) {
			return 0.0F;
		}

		if (source.getSource() instanceof PersistentProjectileEntity piercing && piercing.getPierceLevel() > 0) {
			return 0.0F;
		}

		Vec3d sourcePos = source.getPosition();
		double angleToSource;

		if (sourcePos != null) {
			Vec3d lookDir = getRotationVector(0.0F, getHeadYaw());
			Vec3d toSource = sourcePos.subtract(getEntityPos());
			Vec3d toSourceHorizontal = new Vec3d(toSource.x, 0.0, toSource.z).normalize();
			angleToSource = Math.acos(toSourceHorizontal.dotProduct(lookDir));
		}
		else {
			angleToSource = Math.PI;
		}

		float reducedAmount = blocksAttacksComponent.getDamageReductionAmount(source, amount, angleToSource);
		blocksAttacksComponent.onShieldHit(getEntityWorld(), blockingItem, this, getActiveHand(), reducedAmount);

		if (reducedAmount > 0.0F
				&& !source.isIn(DamageTypeTags.IS_PROJECTILE)
				&& source.getSource() instanceof LivingEntity attacker) {
			takeShieldHit(world, attacker);
		}

		return reducedAmount;
	}

	private void playThornsSound(DamageSource damageSource) {
		if (damageSource.isOf(DamageTypes.THORNS)) {
			SoundCategory soundCategory = this instanceof PlayerEntity ? SoundCategory.PLAYERS : SoundCategory.HOSTILE;
			this.getEntityWorld()
			    .playSound(
					    null,
					    this.getEntityPos().x,
					    this.getEntityPos().y,
					    this.getEntityPos().z,
					    SoundEvents.ENCHANT_THORNS_HIT,
					    soundCategory
			    );
		}
	}

	protected void becomeAngry(DamageSource damageSource) {
		if (damageSource.getAttacker() instanceof LivingEntity livingEntity
				&& !damageSource.isIn(DamageTypeTags.NO_ANGER)
				&& (!damageSource.isOf(DamageTypes.WIND_CHARGE) || !this
				.getType()
				.isIn(EntityTypeTags.NO_ANGER_FROM_WIND_CHARGE)
		)) {
			this.setAttacker(livingEntity);
		}
	}

	protected @Nullable PlayerEntity setAttackingPlayer(DamageSource damageSource) {
		Entity entity = damageSource.getAttacker();
		if (entity instanceof PlayerEntity playerEntity) {
			this.setAttacking(playerEntity, 100);
		}
		else if (entity instanceof WolfEntity wolfEntity && wolfEntity.isTamed()) {
			if (wolfEntity.getOwnerReference() != null) {
				this.setAttacking(wolfEntity.getOwnerReference().getUuid(), 100);
			}
			else {
				this.attackingPlayer = null;
				this.playerHitTimer = 0;
			}
		}

		return LazyEntityReference.getPlayerEntity(this.attackingPlayer, this.getEntityWorld());
	}

	protected void takeShieldHit(ServerWorld world, LivingEntity attacker) {
		attacker.knockback(this);
	}

	protected void knockback(LivingEntity target) {
		target.takeKnockback(0.5, target.getX() - this.getX(), target.getZ() - this.getZ());
	}

	private boolean tryUseDeathProtector(DamageSource source) {
		if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		else {
			ItemStack itemStack = null;
			DeathProtectionComponent deathProtectionComponent = null;

			for (Hand hand : Hand.values()) {
				ItemStack itemStack2 = this.getStackInHand(hand);
				deathProtectionComponent = itemStack2.get(DataComponentTypes.DEATH_PROTECTION);
				if (deathProtectionComponent != null) {
					itemStack = itemStack2.copy();
					itemStack2.decrement(1);
					break;
				}
			}

			if (itemStack != null) {
				if (this instanceof ServerPlayerEntity serverPlayerEntity) {
					serverPlayerEntity.incrementStat(Stats.USED.getOrCreateStat(itemStack.getItem()));
					Criteria.USED_TOTEM.trigger(serverPlayerEntity, itemStack);
					itemStack.emitUseGameEvent(this, GameEvent.ITEM_INTERACT_FINISH);
				}

				this.setHealth(1.0F);
				deathProtectionComponent.applyDeathEffects(itemStack, this);
				this.getEntityWorld().sendEntityStatus(this, (byte) 35);
			}

			return deathProtectionComponent != null;
		}
	}

	public @Nullable DamageSource getRecentDamageSource() {
		if (this.getEntityWorld().getTime() - this.lastDamageTime > HURT_SOUND_COOLDOWN_TICKS) {
			this.lastDamageSource = null;
		}

		return this.lastDamageSource;
	}

	protected void playHurtSound(DamageSource damageSource) {
		this.playSound(this.getHurtSound(damageSource));
	}

	public void playSound(@Nullable SoundEvent sound) {
		if (sound != null) {
			this.playSound(sound, this.getSoundVolume(), this.getSoundPitch());
		}
	}

	private void playEquipmentBreakEffects(ItemStack stack) {
		if (!stack.isEmpty()) {
			RegistryEntry<SoundEvent> registryEntry = stack.get(DataComponentTypes.BREAK_SOUND);
			if (registryEntry != null && !this.isSilent()) {
				this.getEntityWorld()
				    .playSoundClient(
						    this.getX(),
						    this.getY(),
						    this.getZ(),
						    registryEntry.value(),
						    this.getSoundCategory(),
						    0.8F,
						    0.8F + this.getEntityWorld().random.nextFloat() * SWIM_SPEED_MULTIPLIER,
						    false
				    );
			}

			this.spawnItemParticles(stack, 5);
		}
	}

	public void onDeath(DamageSource damageSource) {
		if (!this.isRemoved() && !this.dead) {
			Entity entity = damageSource.getAttacker();
			LivingEntity livingEntity = this.getPrimeAdversary();
			if (livingEntity != null) {
				livingEntity.updateKilledAdvancementCriterion(this, damageSource);
			}

			if (this.isSleeping()) {
				this.wakeUp();
			}

			this.clearActiveItem();
			if (!this.getEntityWorld().isClient() && this.hasCustomName()) {
				LOGGER.info("Named entity {} died: {}", this, this.getDamageTracker().getDeathMessage().getString());
			}

			this.dead = true;
			this.getDamageTracker().update();
			if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
				if (entity == null || entity.onKilledOther(serverWorld, this, damageSource)) {
					this.emitGameEvent(GameEvent.ENTITY_DIE);
					this.drop(serverWorld, damageSource);
					this.onKilledBy(livingEntity);
				}

				this.getEntityWorld().sendEntityStatus(this, (byte) 3);
			}

			this.setPose(EntityPose.DYING);
		}
	}

	protected void onKilledBy(@Nullable LivingEntity adversary) {
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			boolean witherRosePlaced = false;
			if (adversary instanceof WitherEntity) {
				if (serverWorld.getGameRules().getValue(GameRules.DO_MOB_GRIEFING)) {
					BlockPos blockPos = this.getBlockPos();
					BlockState blockState = Blocks.WITHER_ROSE.getDefaultState();
					if (this.getEntityWorld().getBlockState(blockPos).isAir()
							&& blockState.canPlaceAt(this.getEntityWorld(), blockPos)) {
						this.getEntityWorld().setBlockState(blockPos, blockState, 3);
						witherRosePlaced = true;
					}
				}

				if (!witherRosePlaced) {
					ItemEntity itemEntity = new ItemEntity(
							this.getEntityWorld(),
							this.getX(),
							this.getY(),
							this.getZ(),
							new ItemStack(Items.WITHER_ROSE)
					);
					this.getEntityWorld().spawnEntity(itemEntity);
				}
			}
		}
	}

	/**
	 * Выбрасывает лут, снаряжение, инвентарь и опыт при смерти сущности.
	 * Флаг {@code causedByPlayer} влияет на шанс выпадения редкого лута.
	 */
	protected void drop(ServerWorld world, DamageSource damageSource) {
		boolean causedByPlayer = playerHitTimer > 0;

		if (shouldDropLoot(world)) {
			dropLoot(world, damageSource, causedByPlayer);
			dropEquipment(world, damageSource, causedByPlayer);
		}

		dropInventory(world);
		dropExperience(world, damageSource.getAttacker());
	}

	protected void dropInventory(ServerWorld world) {
	}

	protected void dropExperience(ServerWorld world, @Nullable Entity attacker) {
		if (!this.isExperienceDroppingDisabled()
				&& (
				this.shouldAlwaysDropExperience() || this.playerHitTimer > 0 && this.shouldDropExperience() && world
						.getGameRules()
						.getValue(GameRules.DO_MOB_LOOT)
		)) {
			ExperienceOrbEntity.spawn(world, this.getEntityPos(), this.getExperienceToDrop(world, attacker));
		}
	}

	protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
	}

	public long getLootTableSeed() {
		return 0L;
	}

	protected float getAttackKnockbackAgainst(Entity target, DamageSource damageSource) {
		float f = (float) this.getAttributeValue(EntityAttributes.ATTACK_KNOCKBACK);
		return this.getEntityWorld() instanceof ServerWorld serverWorld
		       ? EnchantmentHelper.modifyKnockback(serverWorld, this.getWeaponStack(), target, damageSource, f) / 2.0F
		       : f / 2.0F;
	}

	protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
		Optional<RegistryKey<LootTable>> optional = this.getLootTableKey();
		if (!optional.isEmpty()) {
			this.dropLoot(world, damageSource, causedByPlayer, optional.get());
		}
	}

	public void dropLoot(
			ServerWorld world,
			DamageSource damageSource,
			boolean causedByPlayer,
			RegistryKey<LootTable> lootTableKey
	) {
		this.generateLoot(world, damageSource, causedByPlayer, lootTableKey, stack -> this.dropStack(world, stack));
	}

	public void generateLoot(
			ServerWorld world,
			DamageSource damageSource,
			boolean causedByPlayer,
			RegistryKey<LootTable> lootTableKey,
			Consumer<ItemStack> lootConsumer
	) {
		LootTable lootTable = world.getServer().getReloadableRegistries().getLootTable(lootTableKey);
		LootWorldContext.Builder builder = new LootWorldContext.Builder(world)
				.add(LootContextParameters.THIS_ENTITY, this)
				.add(LootContextParameters.ORIGIN, this.getEntityPos())
				.add(LootContextParameters.DAMAGE_SOURCE, damageSource)
				.addOptional(LootContextParameters.ATTACKING_ENTITY, damageSource.getAttacker())
				.addOptional(LootContextParameters.DIRECT_ATTACKING_ENTITY, damageSource.getSource());
		PlayerEntity playerEntity = this.getAttackingPlayer();
		if (causedByPlayer && playerEntity != null) {
			builder = builder.add(LootContextParameters.LAST_DAMAGE_PLAYER, playerEntity).luck(playerEntity.getLuck());
		}

		LootWorldContext lootWorldContext = builder.build(LootContextTypes.ENTITY);
		lootTable.generateLoot(lootWorldContext, this.getLootTableSeed(), lootConsumer);
	}

	public boolean forEachBrushedItem(
			ServerWorld world,
			RegistryKey<LootTable> lootTableKey,
			@Nullable Entity interactingEntity,
			ItemStack tool,
			BiConsumer<ServerWorld, ItemStack> lootConsumer
	) {
		return this.forEachGeneratedItem(
				world,
				lootTableKey,
				parameterSetBuilder -> parameterSetBuilder.add(LootContextParameters.TARGET_ENTITY, this)
				                                          .addOptional(
						                                          LootContextParameters.INTERACTING_ENTITY,
						                                          interactingEntity
				                                          )
				                                          .add(LootContextParameters.TOOL, tool)
				                                          .build(LootContextTypes.ENTITY_INTERACT),
				lootConsumer
		);
	}

	public boolean forEachGiftedItem(
			ServerWorld world,
			RegistryKey<LootTable> lootTableKey,
			BiConsumer<ServerWorld, ItemStack> lootConsumer
	) {
		return this.forEachGeneratedItem(
				world,
				lootTableKey,
				parameterSetBuilder -> parameterSetBuilder.add(LootContextParameters.ORIGIN, this.getEntityPos())
				                                          .add(LootContextParameters.THIS_ENTITY, this)
				                                          .build(LootContextTypes.GIFT),
				lootConsumer
		);
	}

	protected void forEachShearedItem(
			ServerWorld world,
			RegistryKey<LootTable> lootTableKey,
			ItemStack tool,
			BiConsumer<ServerWorld, ItemStack> lootConsumer
	) {
		this.forEachGeneratedItem(
				world,
				lootTableKey,
				parameterSetBuilder -> parameterSetBuilder.add(LootContextParameters.ORIGIN, this.getEntityPos())
				                                          .add(LootContextParameters.THIS_ENTITY, this)
				                                          .add(LootContextParameters.TOOL, tool)
				                                          .build(LootContextTypes.SHEARING),
				lootConsumer
		);
	}

	protected boolean forEachGeneratedItem(
			ServerWorld world,
			RegistryKey<LootTable> lootTableKey,
			Function<LootWorldContext.Builder, LootWorldContext> lootContextParametersFactory,
			BiConsumer<ServerWorld, ItemStack> lootConsumer
	) {
		LootTable lootTable = world.getServer().getReloadableRegistries().getLootTable(lootTableKey);
		LootWorldContext lootWorldContext = lootContextParametersFactory.apply(new LootWorldContext.Builder(world));
		List<ItemStack> list = lootTable.generateLoot(lootWorldContext);
		if (!list.isEmpty()) {
			list.forEach(stack -> lootConsumer.accept(world, stack));
			return true;
		}
		else {
			return false;
		}
	}

	public void takeKnockback(double strength, double x, double z) {
		strength *= 1.0 - this.getAttributeValue(EntityAttributes.KNOCKBACK_RESISTANCE);
		if (!(strength <= 0.0)) {
			this.velocityDirty = true;
			Vec3d vec3d = this.getVelocity();

			while (x * x + z * z < 1.0E-5F) {
				x = (this.random.nextDouble() - this.random.nextDouble()) * 0.01;
				z = (this.random.nextDouble() - this.random.nextDouble()) * 0.01;
			}

			Vec3d vec3d2 = new Vec3d(x, 0.0, z).normalize().multiply(strength);
			this.setVelocity(
					vec3d.x / 2.0 - vec3d2.x,
					this.isOnGround() ? Math.min(SWIM_SPEED_MULTIPLIER, vec3d.y / 2.0 + strength) : vec3d.y,
					vec3d.z / 2.0 - vec3d2.z
			);
		}
	}

	public void tiltScreen(double deltaX, double deltaZ) {
	}

	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_GENERIC_HURT;
	}

	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_GENERIC_DEATH;
	}

	private SoundEvent getFallSound(int distance) {
		return distance > 4 ? this.getFallSounds().big() : this.getFallSounds().small();
	}

	/**
	 * Отключает experience dropping.
	 */
	public void disableExperienceDropping() {
		this.experienceDroppingDisabled = true;
	}

	public boolean isExperienceDroppingDisabled() {
		return this.experienceDroppingDisabled;
	}

	public float getDamageTiltYaw() {
		return 0.0F;
	}

	public Box getHitbox() {
		Box box = this.getBoundingBox();
		Entity entity = this.getVehicle();
		if (entity != null) {
			Vec3d vec3d = entity.getPassengerRidingPos(this);
			return box.withMinY(Math.max(vec3d.y, box.minY));
		}
		else {
			return box;
		}
	}

	public Map<Enchantment, Set<EnchantmentLocationBasedEffect>> getLocationBasedEnchantmentEffects(EquipmentSlot slot) {
		return (Map<Enchantment, Set<EnchantmentLocationBasedEffect>>) this.locationBasedEnchantmentEffects
				.computeIfAbsent(slot, slotx -> new Reference2ObjectArrayMap());
	}

	/**
	 * Использует attack enchantment effects.
	 */
	public void useAttackEnchantmentEffects() {
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			EnchantmentHelper.onAttack(serverWorld, this);
		}
	}

	public LivingEntity.FallSounds getFallSounds() {
		return new LivingEntity.FallSounds(SoundEvents.ENTITY_GENERIC_SMALL_FALL, SoundEvents.ENTITY_GENERIC_BIG_FALL);
	}

	public Optional<BlockPos> getClimbingPos() {
		return this.climbingPos;
	}

	public boolean isClimbing() {
		if (this.isSpectator()) {
			return false;
		}
		else {
			BlockPos blockPos = this.getBlockPos();
			BlockState blockState = this.getBlockStateAtPos();
			if (this.isGliding() && blockState.isIn(BlockTags.CAN_GLIDE_THROUGH)) {
				return false;
			}
			else if (blockState.isIn(BlockTags.CLIMBABLE)) {
				this.climbingPos = Optional.of(blockPos);
				return true;
			}
			else if (blockState.getBlock() instanceof TrapdoorBlock && this.canEnterTrapdoor(blockPos, blockState)) {
				this.climbingPos = Optional.of(blockPos);
				return true;
			}
			else {
				return false;
			}
		}
	}

	private boolean canEnterTrapdoor(BlockPos pos, BlockState state) {
		if (!state.get(TrapdoorBlock.OPEN)) {
			return false;
		}
		else {
			BlockState blockState = this.getEntityWorld().getBlockState(pos.down());
			return blockState.isOf(Blocks.LADDER)
					&& blockState.get(LadderBlock.FACING) == state.get(TrapdoorBlock.FACING);
		}
	}

	@Override
	public boolean isAlive() {
		return !this.isRemoved() && this.getHealth() > 0.0F;
	}

	public boolean isEntityLookingAtMe(
			LivingEntity entity,
			double d,
			boolean bl,
			boolean visualShape,
			double... checkedYs
	) {
		Vec3d vec3d = entity.getRotationVec(1.0F).normalize();

		for (double e : checkedYs) {
			Vec3d vec3d2 = new Vec3d(this.getX() - entity.getX(), e - entity.getEyeY(), this.getZ() - entity.getZ());
			double f = vec3d2.length();
			vec3d2 = vec3d2.normalize();
			double g = vec3d.dotProduct(vec3d2);
			if (g > 1.0 - d / (bl ? f : 1.0)
					&& entity.canSee(
					this,
					visualShape ? RaycastContext.ShapeType.VISUAL : RaycastContext.ShapeType.COLLIDER,
					RaycastContext.FluidHandling.NONE,
					e
			)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public int getSafeFallDistance() {
		return this.getSafeFallDistance(0.0F);
	}

	protected final int getSafeFallDistance(float health) {
		return MathHelper.floor(health + 3.0F);
	}

	@Override
	public boolean handleFallDamage(double fallDistance, float damagePerDistance, DamageSource damageSource) {
		boolean parentHandled = super.handleFallDamage(fallDistance, damagePerDistance, damageSource);
		int fallDamage = computeFallDamage(fallDistance, damagePerDistance);

		if (fallDamage > 0) {
			playSound(getFallSound(fallDamage), 1.0F, 1.0F);
			playBlockFallSound();
			serverDamage(damageSource, fallDamage);
			return true;
		}

		return parentHandled;
	}

	protected int computeFallDamage(double fallDistance, float damagePerDistance) {
		if (this.getType().isIn(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
			return 0;
		}
		else {
			double d = this.getUnsafeFallDistance(fallDistance);
			return MathHelper.floor(
					d * damagePerDistance * this.getAttributeValue(EntityAttributes.FALL_DAMAGE_MULTIPLIER));
		}
	}

	private double getUnsafeFallDistance(double fallDistance) {
		return fallDistance + 1.0E-6 - this.getAttributeValue(EntityAttributes.SAFE_FALL_DISTANCE);
	}

	protected void playBlockFallSound() {
		if (!this.isSilent()) {
			int blockX = MathHelper.floor(this.getX());
			int blockY = MathHelper.floor(this.getY() - 0.2F);
			int blockZ = MathHelper.floor(this.getZ());
			BlockState blockState = this.getEntityWorld().getBlockState(new BlockPos(blockX, blockY, blockZ));
			if (!blockState.isAir()) {
				BlockSoundGroup blockSoundGroup = blockState.getSoundGroup();
				this.playSound(
						blockSoundGroup.getFallSound(),
						blockSoundGroup.getVolume() * 0.5F,
						blockSoundGroup.getPitch() * 0.75F
				);
			}
		}
	}

	@Override
	public void animateDamage(float yaw) {
		this.maxHurtTime = ELYTRA_FLIGHT_TICKS_THRESHOLD;
		this.hurtTime = this.maxHurtTime;
	}

	public int getArmor() {
		return MathHelper.floor(this.getAttributeValue(EntityAttributes.ARMOR));
	}

	public void damageArmor(DamageSource source, float amount) {
	}

	public void damageHelmet(DamageSource source, float amount) {
	}

	protected void damageEquipment(DamageSource source, float amount, EquipmentSlot... slots) {
		if (amount <= 0.0F) {
			return;
		}

		int durabilityDamage = (int) Math.max(1.0F, amount / 4.0F);

		for (EquipmentSlot slot : slots) {
			ItemStack stack = getEquippedStack(slot);
			EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);

			if (equippable != null && equippable.damageOnHurt() && stack.isDamageable() && stack.takesDamageFrom(source)) {
				stack.damage(durabilityDamage, this, slot);
			}
		}
	}

	protected float applyArmorToDamage(DamageSource source, float amount) {
		if (!source.isIn(DamageTypeTags.BYPASSES_ARMOR)) {
			this.damageArmor(source, amount);
			amount =
					DamageUtil.getDamageLeft(
							this,
							amount,
							source,
							this.getArmor(),
							(float) this.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS)
					);
		}

		return amount;
	}

	protected float modifyAppliedDamage(DamageSource source, float amount) {
		if (source.isIn(DamageTypeTags.BYPASSES_EFFECTS)) {
			return amount;
		}

		if (this.hasStatusEffect(StatusEffects.RESISTANCE) && !source.isIn(DamageTypeTags.BYPASSES_RESISTANCE)) {
			int resistanceLevel = (this.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1) * 5;
			int resistanceFactor = 25 - resistanceLevel;
			float reducedDamage = amount * resistanceFactor;
			float originalAmount = amount;
			amount = Math.max(reducedDamage / 25.0F, 0.0F);
			float absorbed = originalAmount - amount;

			if (absorbed > 0.0F && absorbed < Float.MAX_VALUE) {
				if (this instanceof ServerPlayerEntity serverPlayer) {
					serverPlayer.increaseStat(Stats.DAMAGE_RESISTED, Math.round(absorbed * 10.0F));
				}
				else if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
					attacker.increaseStat(Stats.DAMAGE_DEALT_RESISTED, Math.round(absorbed * 10.0F));
				}
			}
		}

		if (amount <= 0.0F) {
			return 0.0F;
		}

		if (source.isIn(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
			return amount;
		}

		float enchantProtection = this.getEntityWorld() instanceof ServerWorld serverWorld
			? EnchantmentHelper.getProtectionAmount(serverWorld, this, source)
			: 0.0F;

		if (enchantProtection > 0.0F) {
			amount = DamageUtil.getInflictedDamage(amount, enchantProtection);
		}

		return amount;
	}

	/**
	 * Применяет финальный урон к здоровью и поглощению после всех модификаторов.
	 * Сначала вычитает из поглощения, затем из здоровья. Статистика поглощённого
	 * урона записывается только для атак игроков-серверов.
	 */
	protected void applyDamage(ServerWorld world, DamageSource source, float amount) {
		if (this.isInvulnerableTo(world, source)) {
			return;
		}

		amount = this.applyArmorToDamage(source, amount);
		amount = this.modifyAppliedDamage(source, amount);
		float damageAfterAbsorption = Math.max(amount - this.getAbsorptionAmount(), 0.0F);
		this.setAbsorptionAmount(this.getAbsorptionAmount() - (amount - damageAfterAbsorption));
		float absorbedAmount = amount - damageAfterAbsorption;
		if (absorbedAmount > 0.0F && absorbedAmount < 3.4028235E37F
				&& source.getAttacker() instanceof ServerPlayerEntity serverPlayerEntity) {
			serverPlayerEntity.increaseStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(absorbedAmount * 10.0F));
		}

		if (damageAfterAbsorption != 0.0F) {
			this.getDamageTracker().onDamage(source, damageAfterAbsorption);
			this.setHealth(this.getHealth() - damageAfterAbsorption);
			this.setAbsorptionAmount(this.getAbsorptionAmount() - damageAfterAbsorption);
			this.emitGameEvent(GameEvent.ENTITY_DAMAGE);
		}
	}

	public DamageTracker getDamageTracker() {
		return this.damageTracker;
	}

	public @Nullable LivingEntity getPrimeAdversary() {
		if (this.attackingPlayer != null) {
			return this.attackingPlayer.getEntityByClass(this.getEntityWorld(), PlayerEntity.class);
		}
		else {
			return this.attackerReference != null ? this.attackerReference.getEntityByClass(
					this.getEntityWorld(),
					LivingEntity.class
			) : null;
		}
	}

	public final float getMaxHealth() {
		return (float) this.getAttributeValue(EntityAttributes.MAX_HEALTH);
	}

	public final float getMaxAbsorption() {
		return (float) this.getAttributeValue(EntityAttributes.MAX_ABSORPTION);
	}

	public final int getStuckArrowCount() {
		return this.dataTracker.get(STUCK_ARROW_COUNT);
	}

	public final void setStuckArrowCount(int stuckArrowCount) {
		this.dataTracker.set(STUCK_ARROW_COUNT, stuckArrowCount);
	}

	public final int getStingerCount() {
		return this.dataTracker.get(STINGER_COUNT);
	}

	public final void setStingerCount(int stingerCount) {
		this.dataTracker.set(STINGER_COUNT, stingerCount);
	}

	private int getHandSwingDuration() {
		ItemStack mainHandStack = this.getStackInHand(Hand.MAIN_HAND);
		int baseDuration = mainHandStack.getSwingAnimation().duration();

		if (StatusEffectUtil.hasHaste(this)) {
			return baseDuration - (1 + StatusEffectUtil.getHasteAmplifier(this));
		}

		return this.hasStatusEffect(StatusEffects.MINING_FATIGUE)
			? baseDuration + (1 + this.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) * 2
			: baseDuration;
	}

	public void swingHand(Hand hand) {
		this.swingHand(hand, false);
	}

	public void swingHand(Hand hand, boolean fromServerPlayer) {
		if (handSwinging && handSwingTicks < getHandSwingDuration() / 2 && handSwingTicks >= 0) {
			return;
		}

		handSwingTicks = -1;
		handSwinging = true;
		preferredHand = hand;

		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			// 0 = анимация главной руки, 3 = анимация второй руки (протокол Minecraft)
			int animationId = hand == Hand.MAIN_HAND ? 0 : 3;
			EntityAnimationS2CPacket packet = new EntityAnimationS2CPacket(this, animationId);
			ServerChunkManager chunkManager = serverWorld.getChunkManager();

			if (fromServerPlayer) {
				chunkManager.sendToNearbyPlayers(this, packet);
			}
			else {
				chunkManager.sendToOtherNearbyPlayers(this, packet);
			}
		}
	}

	@Override
	public void onDamaged(DamageSource damageSource) {
		this.limbAnimator.setSpeed(1.5F);
		this.timeUntilRegen = DEATH_TICKS;
		this.maxHurtTime = ELYTRA_FLIGHT_TICKS_THRESHOLD;
		this.hurtTime = this.maxHurtTime;
		SoundEvent soundEvent = this.getHurtSound(damageSource);
		if (soundEvent != null) {
			this.playSound(
					soundEvent,
					this.getSoundVolume(),
					(this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F
			);
		}

		this.lastDamageSource = damageSource;
		this.lastDamageTime = this.getEntityWorld().getTime();
	}

	@Override
	public void handleStatus(byte status) {
		switch (status) {
			case 2:
				playKineticHitSound();
				break;
			case 3:
				SoundEvent deathSound = getDeathSound();

				if (deathSound != null) {
					playSound(
							deathSound,
							getSoundVolume(),
							(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F
					);
				}

				if (!(this instanceof PlayerEntity)) {
					setHealth(0.0F);
					onDeath(getDamageSources().generic());
				}

				break;
			case 46:
				for (int particleIndex = 0; particleIndex < 128; particleIndex++) {
					double lerpFactor = particleIndex / 127.0;
					float velX = (random.nextFloat() - 0.5F) * 0.2F;
					float velY = (random.nextFloat() - 0.5F) * 0.2F;
					float velZ = (random.nextFloat() - 0.5F) * 0.2F;
					double particleX = MathHelper.lerp(lerpFactor, lastX, getX())
							+ (random.nextDouble() - 0.5) * getWidth() * 2.0;
					double particleY = MathHelper.lerp(lerpFactor, lastY, getY())
							+ random.nextDouble() * getHeight();
					double particleZ = MathHelper.lerp(lerpFactor, lastZ, getZ())
							+ (random.nextDouble() - 0.5) * getWidth() * 2.0;
					getEntityWorld().addParticleClient(ParticleTypes.PORTAL, particleX, particleY, particleZ, velX, velY, velZ);
				}

				break;
			case 47:
				this.playEquipmentBreakEffects(this.getEquippedStack(EquipmentSlot.MAINHAND));
				break;
			case 48:
				this.playEquipmentBreakEffects(this.getEquippedStack(EquipmentSlot.OFFHAND));
				break;
			case 49:
				this.playEquipmentBreakEffects(this.getEquippedStack(EquipmentSlot.HEAD));
				break;
			case 50:
				this.playEquipmentBreakEffects(this.getEquippedStack(EquipmentSlot.CHEST));
				break;
			case 51:
				this.playEquipmentBreakEffects(this.getEquippedStack(EquipmentSlot.LEGS));
				break;
			case 52:
				this.playEquipmentBreakEffects(this.getEquippedStack(EquipmentSlot.FEET));
				break;
			case 54:
				HoneyBlock.addRichParticles(this);
				break;
			case 55:
				this.swapHandStacks();
				break;
			case 60:
				this.addDeathParticles();
				break;
			case 65:
				this.playEquipmentBreakEffects(this.getEquippedStack(EquipmentSlot.BODY));
				break;
			case 67:
				this.addBubbleParticles();
				break;
			case 68:
				this.playEquipmentBreakEffects(this.getEquippedStack(EquipmentSlot.SADDLE));
				break;
			default:
				super.handleStatus(status);
		}
	}

	public float getTimeSinceLastKineticAttack(float tickProgress) {
		return this.lastKineticAttackTime < 0L ? 0.0F
		                                       : (float) (this.getEntityWorld().getTime() - this.lastKineticAttackTime)
		                                         + tickProgress;
	}

	/**
	 * Добавляет death particles.
	 */
	public void addDeathParticles() {
		for (int i = 0; i < DEATH_TICKS; i++) {
			double offsetX = this.random.nextGaussian() * 0.02;
			double offsetY = this.random.nextGaussian() * 0.02;
			double offsetZ = this.random.nextGaussian() * 0.02;
			this.getEntityWorld()
			    .addParticleClient(
					    ParticleTypes.POOF,
					    this.getParticleX(1.0) - offsetX * 10.0,
					    this.getRandomBodyY() - offsetY * 10.0,
					    this.getParticleZ(1.0) - offsetZ * 10.0,
					    offsetX,
					    offsetY,
					    offsetZ
			    );
		}
	}

	private void addBubbleParticles() {
		Vec3d velocity = this.getVelocity();

		for (int i = 0; i < 8; i++) {
			double offsetX = this.random.nextTriangular(0.0, 1.0);
			double offsetY = this.random.nextTriangular(0.0, 1.0);
			double offsetZ = this.random.nextTriangular(0.0, 1.0);
			this.getEntityWorld()
				.addParticleClient(
					ParticleTypes.BUBBLE,
					this.getX() + offsetX,
					this.getY() + offsetY,
					this.getZ() + offsetZ,
					velocity.x,
					velocity.y,
					velocity.z
				);
		}
	}

	private void playKineticHitSound() {
		if (this.getEntityWorld().getTime() - this.lastKineticAttackTime > 10L) {
			this.lastKineticAttackTime = this.getEntityWorld().getTime();
			KineticWeaponComponent kineticWeaponComponent = this.activeItemStack.get(DataComponentTypes.KINETIC_WEAPON);
			if (kineticWeaponComponent != null) {
				kineticWeaponComponent.playHitSound(this);
			}
		}
	}

	private void swapHandStacks() {
		ItemStack itemStack = this.getEquippedStack(EquipmentSlot.OFFHAND);
		this.equipStack(EquipmentSlot.OFFHAND, this.getEquippedStack(EquipmentSlot.MAINHAND));
		this.equipStack(EquipmentSlot.MAINHAND, itemStack);
	}

	@Override
	protected void tickInVoid() {
		this.serverDamage(this.getDamageSources().outOfWorld(), 4.0F);
	}

	protected void tickHandSwing() {
		int swingDuration = this.getHandSwingDuration();
		if (this.handSwinging) {
			this.handSwingTicks++;
			if (this.handSwingTicks >= swingDuration) {
				this.handSwingTicks = 0;
				this.handSwinging = false;
			}
		}
		else {
			this.handSwingTicks = 0;
		}

		this.handSwingProgress = (float) this.handSwingTicks / swingDuration;
	}

	public @Nullable EntityAttributeInstance getAttributeInstance(RegistryEntry<EntityAttribute> attribute) {
		return this.getAttributes().getCustomInstance(attribute);
	}

	public double getAttributeValue(RegistryEntry<EntityAttribute> attribute) {
		return this.getAttributes().getValue(attribute);
	}

	public double getAttributeBaseValue(RegistryEntry<EntityAttribute> attribute) {
		return this.getAttributes().getBaseValue(attribute);
	}

	public AttributeContainer getAttributes() {
		return this.attributes;
	}

	public ItemStack getMainHandStack() {
		return this.getEquippedStack(EquipmentSlot.MAINHAND);
	}

	public ItemStack getOffHandStack() {
		return this.getEquippedStack(EquipmentSlot.OFFHAND);
	}

	public ItemStack getStackInArm(Arm arm) {
		return this.getMainArm() == arm ? this.getMainHandStack() : this.getOffHandStack();
	}

	@Override
	public ItemStack getWeaponStack() {
		return this.getMainHandStack();
	}

	public AttackRangeComponent getAttackRange() {
		AttackRangeComponent
				attackRangeComponent =
				this.getActiveOrMainHandStack().get(DataComponentTypes.ATTACK_RANGE);
		return attackRangeComponent != null ? attackRangeComponent : AttackRangeComponent.defaultForEntity(this);
	}

	public ItemStack getActiveOrMainHandStack() {
		return this.isUsingItem() ? this.getActiveItem() : this.getMainHandStack();
	}

	public boolean isHolding(Item item) {
		return this.isHolding(stack -> stack.isOf(item));
	}

	public boolean isHolding(Predicate<ItemStack> predicate) {
		return predicate.test(this.getMainHandStack()) || predicate.test(this.getOffHandStack());
	}

	public ItemStack getStackInHand(Hand hand) {
		if (hand == Hand.MAIN_HAND) {
			return this.getEquippedStack(EquipmentSlot.MAINHAND);
		}
		else if (hand == Hand.OFF_HAND) {
			return this.getEquippedStack(EquipmentSlot.OFFHAND);
		}
		else {
			throw new IllegalArgumentException("Invalid hand " + hand);
		}
	}

	public void setStackInHand(Hand hand, ItemStack stack) {
		if (hand == Hand.MAIN_HAND) {
			this.equipStack(EquipmentSlot.MAINHAND, stack);
		}
		else {
			if (hand != Hand.OFF_HAND) {
				throw new IllegalArgumentException("Invalid hand " + hand);
			}

			this.equipStack(EquipmentSlot.OFFHAND, stack);
		}
	}

	public boolean hasStackEquipped(EquipmentSlot slot) {
		return !this.getEquippedStack(slot).isEmpty();
	}

	public boolean canUseSlot(EquipmentSlot slot) {
		return true;
	}

	public ItemStack getEquippedStack(EquipmentSlot slot) {
		return this.equipment.get(slot);
	}

	public void equipStack(EquipmentSlot slot, ItemStack stack) {
		this.onEquipStack(slot, this.equipment.put(slot, stack), stack);
	}

	public float getArmorVisibility() {
		int totalSlots = 0;
		int equippedCount = 0;

		for (EquipmentSlot slot : AttributeModifierSlot.ARMOR) {
			if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
				if (!this.getEquippedStack(slot).isEmpty()) {
					equippedCount++;
				}

				totalSlots++;
			}
		}

		return totalSlots > 0 ? (float) equippedCount / totalSlots : 0.0F;
	}

	@Override
	public void setSprinting(boolean sprinting) {
		super.setSprinting(sprinting);
		EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
		entityAttributeInstance.removeModifier(SPRINTING_SPEED_BOOST.id());
		if (sprinting) {
			entityAttributeInstance.addTemporaryModifier(SPRINTING_SPEED_BOOST);
		}
	}

	protected float getSoundVolume() {
		return 1.0F;
	}

	public float getSoundPitch() {
		return this.isBaby()
		       ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.5F
		       : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
	}

	protected boolean isImmobile() {
		return this.isDead();
	}

	@Override
	public void pushAwayFrom(Entity entity) {
		if (!this.isSleeping()) {
			super.pushAwayFrom(entity);
		}
	}

	private void onDismounted(Entity vehicle) {
		Vec3d dismountPos;

		if (isRemoved()) {
			dismountPos = getEntityPos();
		}
		else if (!vehicle.isRemoved()
				&& !getEntityWorld().getBlockState(vehicle.getBlockPos()).isIn(BlockTags.PORTALS)) {
			dismountPos = vehicle.updatePassengerForDismount(this);
		}
		else {
			double safeY = Math.max(getY(), vehicle.getY());
			dismountPos = new Vec3d(getX(), safeY, getZ());
			boolean fitsInSpace = getWidth() <= 4.0F && getHeight() <= 4.0F;

			if (fitsInSpace) {
				double halfHeight = getHeight() / 2.0;
				Vec3d centerPos = dismountPos.add(0.0, halfHeight, 0.0);
				VoxelShape shape = VoxelShapes.cuboid(Box.of(centerPos, getWidth(), getHeight(), getWidth()));
				dismountPos = getEntityWorld()
						.findClosestCollision(this, shape, centerPos, getWidth(), getHeight(), getWidth())
						.map(pos -> pos.add(0.0, -halfHeight, 0.0))
						.orElse(dismountPos);
			}
		}

		requestTeleportAndDismount(dismountPos.x, dismountPos.y, dismountPos.z);
	}

	@Override
	public boolean shouldRenderName() {
		return this.isCustomNameVisible();
	}

	protected float getJumpVelocity() {
		return this.getJumpVelocity(1.0F);
	}

	protected float getJumpVelocity(float strength) {
		return (float) this.getAttributeValue(EntityAttributes.JUMP_STRENGTH) * strength
				* this.getJumpVelocityMultiplier() + this.getJumpBoostVelocityModifier();
	}

	public float getJumpBoostVelocityModifier() {
		return this.hasStatusEffect(StatusEffects.JUMP_BOOST) ? 0.1F * (
				this.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1.0F
		) : 0.0F;
	}

	@VisibleForTesting
	public void jump() {
		float jumpVelocity = getJumpVelocity();

		if (jumpVelocity <= 1.0E-5F) {
			return;
		}

		Vec3d currentVelocity = getVelocity();
		setVelocity(currentVelocity.x, Math.max((double) jumpVelocity, currentVelocity.y), currentVelocity.z);

		if (isSprinting()) {
			float yawRad = getYaw() * (float) (Math.PI / 180.0);
			addVelocityInternal(new Vec3d(-MathHelper.sin(yawRad) * 0.2, 0.0, MathHelper.cos(yawRad) * 0.2));
		}

		velocityDirty = true;
	}

	/**
	 * Knock downwards.
	 */
	protected void knockDownwards() {
		this.setVelocity(this.getVelocity().add(0.0, -ELYTRA_GRAVITY, 0.0));
	}

	protected void swimUpward(TagKey<Fluid> fluid) {
		this.setVelocity(this.getVelocity().add(0.0, ELYTRA_GRAVITY, 0.0));
	}

	protected float getBaseWaterMovementSpeedMultiplier() {
		return 0.8F;
	}

	public boolean canWalkOnFluid(FluidState state) {
		return false;
	}

	@Override
	protected double getGravity() {
		return this.getAttributeValue(EntityAttributes.GRAVITY);
	}

	protected double getEffectiveGravity() {
		return this.getVelocity().y <= 0.0 && this.hasStatusEffect(StatusEffects.SLOW_FALLING)
			? Math.min(this.getFinalGravity(), 0.01)
			: this.getFinalGravity();
	}

	public void travel(Vec3d movementInput) {
		if (this.isTravellingInFluid(this.getEntityWorld().getFluidState(this.getBlockPos()))) {
			this.travelInFluid(movementInput);
		}
		else if (this.isGliding()) {
			this.travelGliding(movementInput);
		}
		else {
			this.travelMidAir(movementInput);
		}
	}

	protected boolean isTravellingInFluid(FluidState state) {
		return (this.isTouchingWater() || this.isInLava()) && this.shouldSwimInFluids() && !this.canWalkOnFluid(state);
	}

	protected void travelFlying(Vec3d movementInput, float speed) {
		this.travelFlying(movementInput, 0.02F, 0.02F, speed);
	}

	protected void travelFlying(Vec3d movementInput, float inWaterSpeed, float inLavaSpeed, float regularSpeed) {
		if (this.isTouchingWater()) {
			this.updateVelocity(inWaterSpeed, movementInput);
			this.move(MovementType.SELF, this.getVelocity());
			this.setVelocity(this.getVelocity().multiply(0.8F));
		}
		else if (this.isInLava()) {
			this.updateVelocity(inLavaSpeed, movementInput);
			this.move(MovementType.SELF, this.getVelocity());
			this.setVelocity(this.getVelocity().multiply(0.5));
		}
		else {
			this.updateVelocity(regularSpeed, movementInput);
			this.move(MovementType.SELF, this.getVelocity());
			this.setVelocity(this.getVelocity().multiply(0.91F));
		}
	}

	private void travelMidAir(Vec3d movementInput) {
		BlockPos blockPos = this.getVelocityAffectingPos();
		float slipperiness = this.isOnGround()
			? this.getEntityWorld().getBlockState(blockPos).getBlock().getSlipperiness()
			: 1.0F;
		float drag = slipperiness * 0.91F;
		Vec3d motion = this.applyMovementInput(movementInput, slipperiness);
		double velocityY = motion.y;

		StatusEffectInstance levitation = this.getStatusEffect(StatusEffects.LEVITATION);
		if (levitation != null) {
			velocityY += (0.05 * (levitation.getAmplifier() + 1) - motion.y) * 0.2;
		}
		else if (!this.getEntityWorld().isClient() || this.getEntityWorld().isChunkLoaded(blockPos)) {
			velocityY -= this.getEffectiveGravity();
		}
		else if (this.getY() > this.getEntityWorld().getBottomY()) {
			velocityY = -0.1;
		}
		else {
			velocityY = 0.0;
		}

		if (this.hasNoDrag()) {
			this.setVelocity(motion.x, velocityY, motion.z);
		}
		else {
			float verticalDrag = this instanceof Flutterer ? drag : MOVEMENT_SPEED_MULTIPLIER;
			this.setVelocity(motion.x * drag, velocityY * verticalDrag, motion.z * drag);
		}
	}

	private void travelInFluid(Vec3d movementInput) {
		boolean falling = this.getVelocity().y <= 0.0;
		double startY = this.getY();
		double gravity = this.getEffectiveGravity();

		if (this.isTouchingWater()) {
			this.travelInWater(movementInput, gravity, falling, startY);
			this.floatIfRidden();
		}
		else {
			this.travelInLava(movementInput, gravity, falling, startY);
		}
	}

	protected void travelInWater(Vec3d movementInput, double gravity, boolean falling, double y) {
		float waterDrag = this.isSprinting() ? 0.9F : this.getBaseWaterMovementSpeedMultiplier();
		float waterSpeed = 0.02F;
		float efficiency = (float) this.getAttributeValue(EntityAttributes.WATER_MOVEMENT_EFFICIENCY);

		if (!this.isOnGround()) {
			efficiency *= 0.5F;
		}

		if (efficiency > 0.0F) {
			waterDrag += (0.54600006F - waterDrag) * efficiency;
			waterSpeed += (this.getMovementSpeed() - waterSpeed) * efficiency;
		}

		if (this.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
			waterDrag = 0.96F;
		}

		this.updateVelocity(waterSpeed, movementInput);
		this.move(MovementType.SELF, this.getVelocity());
		Vec3d velocity = this.getVelocity();

		if (this.horizontalCollision && this.isClimbing()) {
			velocity = new Vec3d(velocity.x, 0.2, velocity.z);
		}

		velocity = velocity.multiply(waterDrag, 0.8F, waterDrag);
		this.setVelocity(this.applyFluidMovingSpeed(gravity, falling, velocity));
		this.resetVerticalVelocityInFluid(y);
	}

	private void travelInLava(Vec3d movementInput, double gravity, boolean falling, double y) {
		this.updateVelocity(0.02F, movementInput);
		this.move(MovementType.SELF, this.getVelocity());
		if (this.getFluidHeight(FluidTags.LAVA) <= this.getSwimHeight()) {
			this.setVelocity(this.getVelocity().multiply(0.5, 0.8F, 0.5));
			Vec3d vec3d = this.applyFluidMovingSpeed(gravity, falling, this.getVelocity());
			this.setVelocity(vec3d);
		}
		else {
			this.setVelocity(this.getVelocity().multiply(0.5));
		}

		if (gravity != 0.0) {
			this.setVelocity(this.getVelocity().add(0.0, -gravity / 4.0, 0.0));
		}

		this.resetVerticalVelocityInFluid(y);
	}

	private void resetVerticalVelocityInFluid(double y) {
		Vec3d vec3d = this.getVelocity();
		if (this.horizontalCollision && this.doesNotCollide(vec3d.x, vec3d.y + 0.6F - this.getY() + y, vec3d.z)) {
			this.setVelocity(vec3d.x, 0.3F, vec3d.z);
		}
	}

	private void floatIfRidden() {
		if (this.getType().isIn(EntityTypeTags.CAN_FLOAT_WHILE_RIDDEN)
			&& this.hasPassengers()
			&& this.getFluidHeight(FluidTags.WATER) > this.getSwimHeight()
		) {
			this.setVelocity(this.getVelocity().add(0.0, ELYTRA_GRAVITY, 0.0));
		}
	}

	private void travelGliding(Vec3d movementInput) {
		if (this.isClimbing()) {
			this.travelMidAir(movementInput);
			this.stopGliding();
		}
		else {
			Vec3d vec3d = this.getVelocity();
			double d = vec3d.horizontalLength();
			this.setVelocity(this.calcGlidingVelocity(vec3d));
			this.move(MovementType.SELF, this.getVelocity());
			if (!this.getEntityWorld().isClient()) {
				double e = this.getVelocity().horizontalLength();
				this.checkGlidingCollision(d, e);
			}
		}
	}

	public void stopGliding() {
		this.setFlag(7, false);
	}

	private Vec3d calcGlidingVelocity(Vec3d oldVelocity) {
		Vec3d lookDir = this.getRotationVector();
		float pitchRad = this.getPitch() * (float) (Math.PI / 180.0);
		double horizontalDir = Math.sqrt(lookDir.x * lookDir.x + lookDir.z * lookDir.z);
		double horizontalSpeed = oldVelocity.horizontalLength();
		double gravity = this.getEffectiveGravity();
		double cosPitchSq = MathHelper.square(Math.cos(pitchRad));

		oldVelocity = oldVelocity.add(0.0, gravity * (-1.0 + cosPitchSq * 0.75), 0.0);

		if (oldVelocity.y < 0.0 && horizontalDir > 0.0) {
			double liftFactor = oldVelocity.y * -0.1 * cosPitchSq;
			oldVelocity = oldVelocity.add(
				lookDir.x * liftFactor / horizontalDir,
				liftFactor,
				lookDir.z * liftFactor / horizontalDir
			);
		}

		if (pitchRad < 0.0F && horizontalDir > 0.0) {
			double pitchLift = horizontalSpeed * -MathHelper.sin(pitchRad) * 0.04;
			oldVelocity = oldVelocity.add(
				-lookDir.x * pitchLift / horizontalDir,
				pitchLift * 3.2,
				-lookDir.z * pitchLift / horizontalDir
			);
		}

		if (horizontalDir > 0.0) {
			oldVelocity = oldVelocity.add(
				(lookDir.x / horizontalDir * horizontalSpeed - oldVelocity.x) * 0.1,
				0.0,
				(lookDir.z / horizontalDir * horizontalSpeed - oldVelocity.z) * 0.1
			);
		}

		return oldVelocity.multiply(0.99F, MOVEMENT_SPEED_MULTIPLIER, 0.99F);
	}

	private void checkGlidingCollision(double oldSpeed, double newSpeed) {
		if (this.horizontalCollision) {
			double speedDelta = oldSpeed - newSpeed;
			float impactDamage = (float) (speedDelta * 10.0 - 3.0);
			if (impactDamage > 0.0F) {
				this.playSound(this.getFallSound((int) impactDamage), 1.0F, 1.0F);
				this.serverDamage(this.getDamageSources().flyIntoWall(), impactDamage);
			}
		}
	}

	private void travelControlled(PlayerEntity controllingPlayer, Vec3d movementInput) {
		Vec3d vec3d = this.getControlledMovementInput(controllingPlayer, movementInput);
		this.tickControlled(controllingPlayer, vec3d);
		if (this.canMoveVoluntarily()) {
			this.setMovementSpeed(this.getSaddledSpeed(controllingPlayer));
			this.travel(vec3d);
		}
		else {
			this.setVelocity(Vec3d.ZERO);
		}
	}

	protected void tickControlled(PlayerEntity controllingPlayer, Vec3d movementInput) {
	}

	protected Vec3d getControlledMovementInput(PlayerEntity controllingPlayer, Vec3d movementInput) {
		return movementInput;
	}

	protected float getSaddledSpeed(PlayerEntity controllingPlayer) {
		return this.getMovementSpeed();
	}

	public void updateLimbs(boolean flutter) {
		float movementDelta = (float) MathHelper.magnitude(
			this.getX() - this.lastX,
			flutter ? this.getY() - this.lastY : 0.0,
			this.getZ() - this.lastZ
		);

		if (!this.hasVehicle() && this.isAlive()) {
			this.updateLimbs(movementDelta);
		}
		else {
			this.limbAnimator.reset();
		}
	}

	protected void updateLimbs(float posDelta) {
		float normalizedDelta = Math.min(posDelta * 4.0F, 1.0F);
		this.limbAnimator.updateLimbs(normalizedDelta, SWIM_SPEED_MULTIPLIER, this.isBaby() ? 3.0F : 1.0F);
	}

	private Vec3d applyMovementInput(Vec3d movementInput, float slipperiness) {
		this.updateVelocity(this.getMovementSpeed(slipperiness), movementInput);
		this.setVelocity(this.applyClimbingSpeed(this.getVelocity()));
		this.move(MovementType.SELF, this.getVelocity());
		Vec3d vec3d = this.getVelocity();
		if ((this.horizontalCollision || this.jumping) && (this.isClimbing()
				|| this.wasInPowderSnow && PowderSnowBlock.canWalkOnPowderSnow(this)
		)) {
			vec3d = new Vec3d(vec3d.x, 0.2, vec3d.z);
		}

		return vec3d;
	}

	public Vec3d applyFluidMovingSpeed(double gravity, boolean falling, Vec3d motion) {
		if (gravity != 0.0 && !this.isSprinting()) {
			double d;
			if (falling && Math.abs(motion.y - 0.005) >= STEP_SOUND_MIN_SPEED && Math.abs(motion.y - gravity / 16.0) < STEP_SOUND_MIN_SPEED) {
				d = -STEP_SOUND_MIN_SPEED;
			}
			else {
				d = motion.y - gravity / 16.0;
			}

			return new Vec3d(motion.x, d, motion.z);
		}
		else {
			return motion;
		}
	}

	private Vec3d applyClimbingSpeed(Vec3d motion) {
		if (!this.isClimbing()) {
			return motion;
		}

		this.onLanding();
		float climbLimit = 0.15F;
		double clampedX = MathHelper.clamp(motion.x, -climbLimit, climbLimit);
		double clampedZ = MathHelper.clamp(motion.z, -climbLimit, climbLimit);
		double clampedY = Math.max(motion.y, -climbLimit);

		if (clampedY < 0.0 && !this.getBlockStateAtPos().isOf(Blocks.SCAFFOLDING) && this.isHoldingOntoLadder()
				&& this instanceof PlayerEntity) {
			clampedY = 0.0;
		}

		return new Vec3d(clampedX, clampedY, clampedZ);
	}

	private float getMovementSpeed(float slipperiness) {
		return this.isOnGround() ? this.getMovementSpeed() * (0.21600002F / (slipperiness * slipperiness * slipperiness
		)
		) : this.getOffGroundSpeed();
	}

	protected float getOffGroundSpeed() {
		return this.getControllingPassenger() instanceof PlayerEntity ? this.getMovementSpeed() * 0.1F : 0.02F;
	}

	public float getMovementSpeed() {
		return this.movementSpeed;
	}

	public void setMovementSpeed(float movementSpeed) {
		this.movementSpeed = movementSpeed;
	}

	public boolean tryAttack(ServerWorld world, Entity target) {
		this.onAttacking(target);
		return false;
	}

	public void knockbackTarget(Entity target, float strength, Vec3d playerTargetVelocity) {
		if (strength > 0.0F && target instanceof LivingEntity livingEntity) {
			livingEntity.takeKnockback(
					strength,
					MathHelper.sin(this.getYaw() * (float) (Math.PI / 180.0)),
					-MathHelper.cos(this.getYaw() * (float) (Math.PI / 180.0))
			);
			this.setVelocity(this.getVelocity().multiply(0.6, 1.0, 0.6));
		}
	}

	protected void playAttackSound() {
	}

	@Override
	public void tick() {
		super.tick();
		tickActiveItemStack();
		updateLeaningPitch();

		if (!getEntityWorld().isClient()) {
			int arrowCount = getStuckArrowCount();

			if (arrowCount > 0) {
				if (stuckArrowTimer <= 0) {
					stuckArrowTimer = DEATH_TICKS * (30 - arrowCount);
				}

				stuckArrowTimer--;

				if (stuckArrowTimer <= 0) {
					setStuckArrowCount(arrowCount - 1);
				}
			}

			int stingerCount = getStingerCount();

			if (stingerCount > 0) {
				if (stuckStingerTimer <= 0) {
					stuckStingerTimer = DEATH_TICKS * (30 - stingerCount);
				}

				stuckStingerTimer--;

				if (stuckStingerTimer <= 0) {
					setStingerCount(stingerCount - 1);
				}
			}

			sendEquipmentChanges();

			if (age % DEATH_TICKS == 0) {
				getDamageTracker().update();
			}

			if (isSleeping() && (!isInteractable() || !isSleepingInBed())) {
				wakeUp();
			}
		}

		if (!isRemoved()) {
			tickMovement();
		}

		double deltaX = getX() - lastX;
		double deltaZ = getZ() - lastZ;
		float horizontalDistSq = (float) (deltaX * deltaX + deltaZ * deltaZ);
		float targetBodyYaw = bodyYaw;

		if (horizontalDistSq > 0.0025000002F) {
			float movementYaw = (float) MathHelper.atan2(deltaZ, deltaX) * (180.0F / (float) Math.PI) - 90.0F;
			float yawDiff = MathHelper.abs(MathHelper.wrapDegrees(getYaw()) - movementYaw);
			targetBodyYaw = (95.0F < yawDiff && yawDiff < 265.0F) ? movementYaw - 180.0F : movementYaw;
		}

		if (handSwingProgress > 0.0F) {
			targetBodyYaw = getYaw();
		}

		Profiler profiler = Profilers.get();
		profiler.push("headTurn");
		turnHead(targetBodyYaw);
		profiler.pop();
		profiler.push("rangeChecks");

		while (getYaw() - lastYaw < -180.0F) {
			lastYaw -= 360.0F;
		}

		while (getYaw() - lastYaw >= 180.0F) {
			lastYaw += 360.0F;
		}

		while (bodyYaw - lastBodyYaw < -180.0F) {
			lastBodyYaw -= 360.0F;
		}

		while (bodyYaw - lastBodyYaw >= 180.0F) {
			lastBodyYaw += 360.0F;
		}

		while (getPitch() - lastPitch < -180.0F) {
			lastPitch -= 360.0F;
		}

		while (getPitch() - lastPitch >= 180.0F) {
			lastPitch += 360.0F;
		}

		while (headYaw - lastHeadYaw < -180.0F) {
			lastHeadYaw -= 360.0F;
		}

		while (headYaw - lastHeadYaw >= 180.0F) {
			lastHeadYaw += 360.0F;
		}

		profiler.pop();
		glidingTicks = isGliding() ? glidingTicks + 1 : 0;

		if (isSleeping()) {
			setPitch(0.0F);
		}

		updateAttributes();
		elytraFlightController.update();
	}

	public boolean isInPiercingCooldown(Entity target, int cooldownTicks) {
		if (this.piercingCooldowns == null) {
			return false;
		}

		return this.piercingCooldowns.containsKey(target)
			&& this.getEntityWorld().getTime() - this.piercingCooldowns.getLong(target) < cooldownTicks;
	}

	public void startPiercingCooldown(Entity target) {
		if (this.piercingCooldowns != null) {
			this.piercingCooldowns.put(target, this.getEntityWorld().getTime());
		}
	}

	public int getPiercedEntityCount(Predicate<Entity> predicate) {
		return this.piercingCooldowns == null ? 0 : (int) this.piercingCooldowns
		                                                  .keySet()
		                                                  .stream()
		                                                  .filter(predicate)
		                                                  .count();
	}

	public boolean pierce(
			EquipmentSlot slot,
			Entity target,
			float damage,
			boolean dealDamage,
			boolean knockback,
			boolean dismount
	) {
		if (!(this.getEntityWorld() instanceof ServerWorld serverWorld)) {
			return false;
		}

		ItemStack weapon = this.getEquippedStack(slot);
		DamageSource damageSource = weapon.getDamageSource(this, () -> this.getDamageSources().mobAttack(this));
		float finalDamage = EnchantmentHelper.getDamage(serverWorld, weapon, target, damageSource, damage);
		Vec3d prevVelocity = target.getVelocity();
		boolean didDamage = dealDamage && target.damage(serverWorld, damageSource, finalDamage);
		boolean anyEffect = knockback | didDamage;

		if (knockback) {
			this.knockbackTarget(target, SWIM_SPEED_MULTIPLIER + this.getAttackKnockbackAgainst(target, damageSource), prevVelocity);
		}

		if (dismount && target.hasVehicle()) {
			anyEffect = true;
			target.stopRiding();
		}

		if (target instanceof LivingEntity livingTarget) {
			weapon.postHit(livingTarget, this);
		}

		if (didDamage) {
			EnchantmentHelper.onTargetDamaged(serverWorld, target, damageSource);
		}

		if (!anyEffect) {
			return false;
		}

		this.onAttacking(target);
		this.playAttackSound();
		return true;
	}

	public void beforePlayerAttack() {
	}

	private void sendEquipmentChanges() {
		Map<EquipmentSlot, ItemStack> map = this.getEquipmentChanges();
		if (map != null) {
			this.checkHandStackSwap(map);
			if (!map.isEmpty()) {
				this.sendEquipmentChanges(map);
			}
		}
	}

	private @Nullable Map<EquipmentSlot, ItemStack> getEquipmentChanges() {
		Map<EquipmentSlot, ItemStack> changes = null;

		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			ItemStack previousStack = this.lastEquipmentStacks.get(slot);
			ItemStack currentStack = this.getEquippedStack(slot);

			if (this.areItemsDifferent(previousStack, currentStack)) {
				if (changes == null) {
					changes = Maps.newEnumMap(EquipmentSlot.class);
				}

				changes.put(slot, currentStack);

				if (!previousStack.isEmpty()) {
					this.onEquipmentRemoved(previousStack, slot, this.getAttributes());
				}
			}
		}

		if (changes != null) {
			for (Entry<EquipmentSlot, ItemStack> entry : changes.entrySet()) {
				EquipmentSlot changedSlot = entry.getKey();
				ItemStack newStack = entry.getValue();

				if (!newStack.isEmpty() && !newStack.shouldBreak()) {
					newStack.applyAttributeModifiers(
						changedSlot, (attribute, modifier) -> {
							EntityAttributeInstance instance = this.attributes.getCustomInstance(attribute);
							if (instance != null) {
								instance.removeModifier(modifier.id());
								instance.addTemporaryModifier(modifier);
							}
						}
					);

					if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
						EnchantmentHelper.applyLocationBasedEffects(serverWorld, newStack, this, changedSlot);
					}
				}
			}
		}

		return changes;
	}

	public boolean areItemsDifferent(ItemStack stack, ItemStack stack2) {
		return !ItemStack.areEqual(stack2, stack);
	}

	private void checkHandStackSwap(Map<EquipmentSlot, ItemStack> equipmentChanges) {
		ItemStack itemStack = equipmentChanges.get(EquipmentSlot.MAINHAND);
		ItemStack itemStack2 = equipmentChanges.get(EquipmentSlot.OFFHAND);
		if (itemStack != null
				&& itemStack2 != null
				&& ItemStack.areEqual(itemStack, this.lastEquipmentStacks.get(EquipmentSlot.OFFHAND))
				&& ItemStack.areEqual(itemStack2, this.lastEquipmentStacks.get(EquipmentSlot.MAINHAND))) {
			((ServerWorld) this.getEntityWorld())
					.getChunkManager()
					.sendToOtherNearbyPlayers(this, new EntityStatusS2CPacket(this, (byte) 55));
			equipmentChanges.remove(EquipmentSlot.MAINHAND);
			equipmentChanges.remove(EquipmentSlot.OFFHAND);
			this.lastEquipmentStacks.put(EquipmentSlot.MAINHAND, itemStack.copy());
			this.lastEquipmentStacks.put(EquipmentSlot.OFFHAND, itemStack2.copy());
		}
	}

	private void sendEquipmentChanges(Map<EquipmentSlot, ItemStack> equipmentChanges) {
		List<Pair<EquipmentSlot, ItemStack>> list = Lists.newArrayListWithCapacity(equipmentChanges.size());
		equipmentChanges.forEach((slot, stack) -> {
			ItemStack itemStack = stack.copy();
			list.add(Pair.of(slot, itemStack));
			this.lastEquipmentStacks.put(slot, itemStack);
		});
		((ServerWorld) this.getEntityWorld())
				.getChunkManager()
				.sendToOtherNearbyPlayers(this, new EntityEquipmentUpdateS2CPacket(this.getId(), list));
	}

	/**
	 * Turn head.
	 *
	 * @param bodyRotation body rotation
	 */
	protected void turnHead(float bodyRotation) {
		float f = MathHelper.wrapDegrees(bodyRotation - this.bodyYaw);
		this.bodyYaw += f * 0.3F;
		float g = MathHelper.wrapDegrees(this.getYaw() - this.bodyYaw);
		float h = this.getMaxRelativeHeadRotation();
		if (Math.abs(g) > h) {
			this.bodyYaw = this.bodyYaw + (g - MathHelper.sign(g) * h);
		}
	}

	protected float getMaxRelativeHeadRotation() {
		return 50.0F;
	}

	/**
	 * Выполняет тик обновления для movement.
	 */
	public void tickMovement() {
		if (jumpingCooldown > 0) {
			jumpingCooldown--;
		}

		if (isInterpolating()) {
			getInterpolator().tick();
		}
		else if (!canMoveVoluntarily()) {
			setVelocity(getVelocity().multiply(MOVEMENT_SPEED_MULTIPLIER));
		}

		if (headTrackingIncrements > 0) {
			lerpHeadYaw(headTrackingIncrements, serverHeadYaw);
			headTrackingIncrements--;
		}

		equipment.tick(this);
		Vec3d velocity = getVelocity();
		double velX = velocity.x;
		double velY = velocity.y;
		double velZ = velocity.z;

		if (getType().equals(EntityType.PLAYER)) {
			if (velocity.horizontalLengthSquared() < 9.0E-6) {
				velX = 0.0;
				velZ = 0.0;
			}
		}
		else {
			if (Math.abs(velocity.x) < STEP_SOUND_MIN_SPEED) {
				velX = 0.0;
			}

			if (Math.abs(velocity.z) < STEP_SOUND_MIN_SPEED) {
				velZ = 0.0;
			}
		}

		if (Math.abs(velocity.y) < STEP_SOUND_MIN_SPEED) {
			velY = 0.0;
		}

		setVelocity(velX, velY, velZ);
		Profiler profiler = Profilers.get();
		profiler.push("ai");
		tickMovementInput();

		if (isImmobile()) {
			jumping = false;
			sidewaysSpeed = 0.0F;
			forwardSpeed = 0.0F;
		}
		else if (canActVoluntarily() && !getEntityWorld().isClient()) {
			profiler.push("newAi");
			tickNewAi();
			profiler.pop();
		}

		profiler.pop();
		profiler.push("jump");

		if (jumping && shouldSwimInFluids()) {
			double fluidHeight = isInLava()
					? getFluidHeight(FluidTags.LAVA)
					: getFluidHeight(FluidTags.WATER);
			boolean isTouchingFluid = isTouchingWater() && fluidHeight > 0.0;
			double swimHeight = getSwimHeight();

			if (isTouchingFluid && !(isOnGround() && fluidHeight <= swimHeight)) {
				swimUpward(FluidTags.WATER);
			}
			else if (isInLava() && !(isOnGround() && fluidHeight <= swimHeight)) {
				swimUpward(FluidTags.LAVA);
			}
			else if ((isOnGround() || isTouchingFluid && fluidHeight <= swimHeight) && jumpingCooldown == 0) {
				jump();
				jumpingCooldown = ELYTRA_FLIGHT_TICKS_THRESHOLD;
			}
		}
		else {
			jumpingCooldown = 0;
		}

		profiler.pop();
		profiler.push("travel");
		if (this.isGliding()) {
			this.tickGliding();
		}

		Box box = this.getBoundingBox();
		Vec3d vec3d2 = new Vec3d(this.sidewaysSpeed, this.upwardSpeed, this.forwardSpeed);
		if (this.hasStatusEffect(StatusEffects.SLOW_FALLING) || this.hasStatusEffect(StatusEffects.LEVITATION)) {
			this.onLanding();
		}

		if (this.getControllingPassenger() instanceof PlayerEntity playerEntity && this.isAlive()) {
			this.travelControlled(playerEntity, vec3d2);
		}
		else if (this.canMoveVoluntarily() && this.canActVoluntarily()) {
			this.travel(vec3d2);
		}

		if (!this.getEntityWorld().isClient() || this.isLogicalSideForUpdatingMovement()) {
			this.tickBlockCollision();
		}

		if (this.getEntityWorld().isClient()) {
			this.updateLimbs(this instanceof Flutterer);
		}

		profiler.pop();
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			profiler.push("freezing");
			if (!this.inPowderSnow || !this.canFreeze()) {
				this.setFrozenTicks(Math.max(0, this.getFrozenTicks() - 2));
			}

			this.removePowderSnowSlow();
			this.addPowderSnowSlowIfNeeded();
			if (this.age % HURT_SOUND_COOLDOWN_TICKS == 0 && this.isFrozen() && this.canFreeze()) {
				this.damage(serverWorld, this.getDamageSources().freeze(), 1.0F);
			}

			profiler.pop();
		}

		profiler.push("push");
		if (this.riptideTicks > 0) {
			this.riptideTicks--;
			this.tickRiptide(box, this.getBoundingBox());
		}

		this.tickCramming();
		profiler.pop();
		if (this.getEntityWorld() instanceof ServerWorld serverWorld && this.hurtByWater()
				&& this.isTouchingWaterOrRain()) {
			this.damage(serverWorld, this.getDamageSources().drown(), 1.0F);
		}
	}

	/**
	 * Выполняет тик обновления для movement input.
	 */
	protected void tickMovementInput() {
		this.sidewaysSpeed *= MOVEMENT_SPEED_MULTIPLIER;
		this.forwardSpeed *= MOVEMENT_SPEED_MULTIPLIER;
	}

	public boolean hurtByWater() {
		return false;
	}

	public boolean isJumping() {
		return this.jumping;
	}

	protected void tickGliding() {
		this.limitFallDistance();
		if (!this.getEntityWorld().isClient()) {
			if (!this.canGlide()) {
				this.setFlag(7, false);
				return;
			}

			int currentGlidingTicks = this.glidingTicks + 1;
			if (currentGlidingTicks % ELYTRA_FLIGHT_TICKS_THRESHOLD == 0) {
				int tickInterval = currentGlidingTicks / ELYTRA_FLIGHT_TICKS_THRESHOLD;
				if (tickInterval % 2 == 0) {
					List<EquipmentSlot> glidingSlots = EquipmentSlot.VALUES
						.stream()
						.filter(slot -> canGlideWith(this.getEquippedStack(slot), slot))
						.toList();
					EquipmentSlot damagedSlot = Util.getRandom(glidingSlots, this.random);
					this.getEquippedStack(damagedSlot).damage(1, this, damagedSlot);
				}

				this.emitGameEvent(GameEvent.ELYTRA_GLIDE);
			}
		}
	}

	protected boolean canGlide() {
		if (this.isOnGround() || this.hasVehicle() || this.hasStatusEffect(StatusEffects.LEVITATION)) {
			return false;
		}

		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			if (canGlideWith(this.getEquippedStack(slot), slot)) {
				return true;
			}
		}

		return false;
	}

	protected void tickNewAi() {
	}

	protected void tickCramming() {
		List<Entity> crammed = this.getEntityWorld().getCrammedEntities(this, this.getBoundingBox());

		if (crammed.isEmpty()) {
			return;
		}

		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			int maxCramming = serverWorld.getGameRules().getValue(GameRules.MAX_ENTITY_CRAMMING);
			if (maxCramming > 0 && crammed.size() > maxCramming - 1 && this.random.nextInt(4) == 0) {
				int nonRidingCount = 0;

				for (Entity entity : crammed) {
					if (!entity.hasVehicle()) {
						nonRidingCount++;
					}
				}

				if (nonRidingCount > maxCramming - 1) {
					this.damage(serverWorld, this.getDamageSources().cramming(), 6.0F);
				}
			}
		}

		for (Entity entity : crammed) {
			this.pushAway(entity);
		}
	}

	protected void tickRiptide(Box prevBox, Box currentBox) {
		Box unionBox = prevBox.union(currentBox);
		List<Entity> nearby = this.getEntityWorld().getOtherEntities(this, unionBox);

		if (!nearby.isEmpty()) {
			for (Entity entity : nearby) {
				if (entity instanceof LivingEntity livingTarget) {
					this.attackLivingEntity(livingTarget);
					this.riptideTicks = 0;
					this.setVelocity(this.getVelocity().multiply(-0.2));
					break;
				}
			}
		}
		else if (this.horizontalCollision) {
			this.riptideTicks = 0;
		}

		if (!this.getEntityWorld().isClient() && this.riptideTicks <= 0) {
			this.setLivingFlag(4, false);
			this.riptideAttackDamage = 0.0F;
			this.riptideStack = null;
		}
	}

	protected void pushAway(Entity entity) {
		entity.pushAwayFrom(this);
	}

	protected void attackLivingEntity(LivingEntity target) {
	}

	public boolean isUsingRiptide() {
		return (this.dataTracker.get(LIVING_FLAGS) & 4) != 0;
	}

	@Override
	public void stopRiding() {
		Entity entity = this.getVehicle();
		super.stopRiding();
		if (entity != null && entity != this.getVehicle() && !this.getEntityWorld().isClient()) {
			this.onDismounted(entity);
		}
	}

	@Override
	public void tickRiding() {
		super.tickRiding();
		this.onLanding();
	}

	@Override
	public PositionInterpolator getInterpolator() {
		return this.interpolator;
	}

	@Override
	public void updateTrackedHeadRotation(float yaw, int interpolationSteps) {
		this.serverHeadYaw = yaw;
		this.headTrackingIncrements = interpolationSteps;
	}

	public void setJumping(boolean jumping) {
		this.jumping = jumping;
	}

	public void triggerItemPickedUpByEntityCriteria(ItemEntity item) {
		if (item.getOwner() instanceof ServerPlayerEntity thrower) {
			Criteria.THROWN_ITEM_PICKED_UP_BY_ENTITY.trigger(thrower, item.getStack(), this);
		}
	}

	public void sendPickup(Entity item, int count) {
		if (!item.isRemoved()
				&& !this.getEntityWorld().isClient()
				&& (item instanceof ItemEntity || item instanceof PersistentProjectileEntity
				|| item instanceof ExperienceOrbEntity
		)) {
			((ServerWorld) this.getEntityWorld())
					.getChunkManager()
					.sendToOtherNearbyPlayers(
							item,
							new ItemPickupAnimationS2CPacket(item.getId(), this.getId(), count)
					);
		}
	}

	public boolean canSee(Entity entity) {
		return this.canSee(
				entity,
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE,
				entity.getEyeY()
		);
	}

	public boolean canSee(
			Entity entity,
			RaycastContext.ShapeType shapeType,
			RaycastContext.FluidHandling fluidHandling,
			double entityY
	) {
		if (entity.getEntityWorld() != this.getEntityWorld()) {
			return false;
		}

		Vec3d eyePos = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
		Vec3d targetPos = new Vec3d(entity.getX(), entityY, entity.getZ());

		return targetPos.distanceTo(eyePos) <= MAX_ENTITY_VIEWING_DISTANCE
			&& this.getEntityWorld()
				.raycast(new RaycastContext(eyePos, targetPos, shapeType, fluidHandling, this))
				.getType() == HitResult.Type.MISS;
	}

	@Override
	public float getYaw(float tickProgress) {
		return tickProgress == 1.0F ? this.headYaw
		                            : MathHelper.lerpAngleDegrees(tickProgress, this.lastHeadYaw, this.headYaw);
	}

	public float getHandSwingProgress(float tickProgress) {
		float delta = this.handSwingProgress - this.lastHandSwingProgress;
		if (delta < 0.0F) {
			delta++;
		}

		return this.lastHandSwingProgress + delta * tickProgress;
	}

	@Override
	public boolean canHit() {
		return !this.isRemoved();
	}

	@Override
	public boolean isPushable() {
		return this.isAlive() && !this.isSpectator() && !this.isClimbing();
	}

	@Override
	public float getHeadYaw() {
		return this.headYaw;
	}

	@Override
	public void setHeadYaw(float headYaw) {
		this.headYaw = headYaw;
	}

	@Override
	public void setBodyYaw(float bodyYaw) {
		this.bodyYaw = bodyYaw;
	}

	@Override
	public Vec3d positionInPortal(Direction.Axis portalAxis, BlockLocating.Rectangle portalRect) {
		return positionInPortal(super.positionInPortal(portalAxis, portalRect));
	}

	public static Vec3d positionInPortal(Vec3d pos) {
		return new Vec3d(pos.x, pos.y, 0.0);
	}

	public float getAbsorptionAmount() {
		return this.absorptionAmount;
	}

	public final void setAbsorptionAmount(float absorptionAmount) {
		this.setAbsorptionAmountUnclamped(MathHelper.clamp(absorptionAmount, 0.0F, this.getMaxAbsorption()));
	}

	protected void setAbsorptionAmountUnclamped(float absorptionAmount) {
		this.absorptionAmount = absorptionAmount;
	}

	/**
	 * Enter combat.
	 */
	public void enterCombat() {
	}

	/**
	 * End combat.
	 */
	public void endCombat() {
	}

	/**
	 * Mark effects dirty.
	 */
	protected void markEffectsDirty() {
		this.effectsChanged = true;
	}

	public abstract Arm getMainArm();

	public boolean isUsingItem() {
		return (this.dataTracker.get(LIVING_FLAGS) & 1) > 0;
	}

	public Hand getActiveHand() {
		return (this.dataTracker.get(LIVING_FLAGS) & 2) > 0 ? Hand.OFF_HAND : Hand.MAIN_HAND;
	}

	private void tickActiveItemStack() {
		if (this.isUsingItem()) {
			if (ItemStack.areItemsEqual(this.getStackInHand(this.getActiveHand()), this.activeItemStack)) {
				this.activeItemStack = this.getStackInHand(this.getActiveHand());
				this.tickItemStackUsage(this.activeItemStack);
			}
			else {
				this.clearActiveItem();
			}
		}
	}

	private @Nullable ItemEntity createItemEntity(ItemStack stack, boolean atSelf, boolean retainOwnership) {
		if (stack.isEmpty()) {
			return null;
		}
		else {
			double d = this.getEyeY() - 0.3F;
			ItemEntity itemEntity = new ItemEntity(this.getEntityWorld(), this.getX(), d, this.getZ(), stack);
			itemEntity.setPickupDelay(HURT_SOUND_COOLDOWN_TICKS);
			if (retainOwnership) {
				itemEntity.setThrower(this);
			}

			if (atSelf) {
				float randomRadius = this.random.nextFloat() * 0.5F;
				float randomAngle = this.random.nextFloat() * (float) (Math.PI * 2);
				itemEntity.setVelocity(-MathHelper.sin(randomAngle) * randomRadius, 0.2F, MathHelper.cos(randomAngle) * randomRadius);
			}
			else {
				float pitchSin = MathHelper.sin(this.getPitch() * (float) (Math.PI / 180.0));
				float pitchCos = MathHelper.cos(this.getPitch() * (float) (Math.PI / 180.0));
				float yawSin = MathHelper.sin(this.getYaw() * (float) (Math.PI / 180.0));
				float yawCos = MathHelper.cos(this.getYaw() * (float) (Math.PI / 180.0));
				float randomOffset = this.random.nextFloat() * (float) (Math.PI * 2);
				float randomSpread = 0.02F * this.random.nextFloat();
				itemEntity.setVelocity(
					-yawSin * pitchCos * 0.3F + Math.cos(randomOffset) * randomSpread,
					-pitchSin * 0.3F + 0.1F + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F,
					yawCos * pitchCos * 0.3F + Math.sin(randomOffset) * randomSpread
				);
			}

			return itemEntity;
		}
	}

	protected void tickItemStackUsage(ItemStack stack) {
		stack.usageTick(this.getEntityWorld(), this, this.getItemUseTimeLeft());
		if (--this.itemUseTimeLeft == 0 && !this.getEntityWorld().isClient() && !stack.isUsedOnRelease()) {
			this.consumeItem();
		}
	}

	private void updateLeaningPitch() {
		this.lastLeaningPitch = this.leaningPitch;
		if (this.isInSwimmingPose()) {
			this.leaningPitch = Math.min(1.0F, this.leaningPitch + 0.09F);
		}
		else {
			this.leaningPitch = Math.max(0.0F, this.leaningPitch - 0.09F);
		}
	}

	protected void setLivingFlag(int mask, boolean value) {
		int i = this.dataTracker.get(LIVING_FLAGS);
		if (value) {
			i |= mask;
		}
		else {
			i &= ~mask;
		}

		this.dataTracker.set(LIVING_FLAGS, (byte) i);
	}

	public void setCurrentHand(Hand hand) {
		ItemStack itemStack = this.getStackInHand(hand);
		if (!itemStack.isEmpty() && !this.isUsingItem()) {
			this.activeItemStack = itemStack;
			this.itemUseTimeLeft = itemStack.getMaxUseTime(this);
			if (!this.getEntityWorld().isClient()) {
				this.setLivingFlag(1, true);
				this.setLivingFlag(2, hand == Hand.OFF_HAND);
				this.activeItemStack.emitUseGameEvent(this, GameEvent.ITEM_INTERACT_START);
				if (this.activeItemStack.contains(DataComponentTypes.KINETIC_WEAPON)) {
					this.piercingCooldowns = new Object2LongOpenHashMap();
				}
			}
		}
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (SLEEPING_POSITION.equals(data)) {
			if (this.getEntityWorld().isClient()) {
				this.getSleepingPosition().ifPresent(this::setPositionInBed);
			}
		}
		else if (LIVING_FLAGS.equals(data) && this.getEntityWorld().isClient()) {
			if (this.isUsingItem() && this.activeItemStack.isEmpty()) {
				this.activeItemStack = this.getStackInHand(this.getActiveHand());
				if (!this.activeItemStack.isEmpty()) {
					this.itemUseTimeLeft = this.activeItemStack.getMaxUseTime(this);
				}
			}
			else if (!this.isUsingItem() && !this.activeItemStack.isEmpty()) {
				this.activeItemStack = ItemStack.EMPTY;
				this.itemUseTimeLeft = 0;
			}
		}
	}

	@Override
	public void lookAt(EntityAnchorArgumentType.EntityAnchor anchorPoint, Vec3d target) {
		super.lookAt(anchorPoint, target);
		this.lastHeadYaw = this.headYaw;
		this.bodyYaw = this.headYaw;
		this.lastBodyYaw = this.bodyYaw;
	}

	@Override
	public float lerpYaw(float tickProgress) {
		return MathHelper.lerp(tickProgress, this.lastBodyYaw, this.bodyYaw);
	}

	public void spawnItemParticles(ItemStack stack, int count) {
		for (int i = 0; i < count; i++) {
			Vec3d velocity = new Vec3d(
				(this.random.nextFloat() - 0.5) * 0.1,
				this.random.nextFloat() * 0.1 + 0.1,
				0.0
			);
			velocity = velocity.rotateX(-this.getPitch() * (float) (Math.PI / 180.0));
			velocity = velocity.rotateY(-this.getYaw() * (float) (Math.PI / 180.0));

			double backOffset = -this.random.nextFloat() * 0.6 - 0.3;
			Vec3d spawnPos = new Vec3d((this.random.nextFloat() - 0.5) * 0.3, backOffset, 0.6);
			spawnPos = spawnPos.rotateX(-this.getPitch() * (float) (Math.PI / 180.0));
			spawnPos = spawnPos.rotateY(-this.getYaw() * (float) (Math.PI / 180.0));
			spawnPos = spawnPos.add(this.getX(), this.getEyeY(), this.getZ());

			this.getEntityWorld()
			    .addParticleClient(
					    new ItemStackParticleEffect(ParticleTypes.ITEM, stack),
					    spawnPos.x,
					    spawnPos.y,
					    spawnPos.z,
					    velocity.x,
					    velocity.y + 0.05,
					    velocity.z
			    );
		}
	}

	protected void consumeItem() {
		if (!this.getEntityWorld().isClient() || this.isUsingItem()) {
			Hand hand = this.getActiveHand();
			if (!this.activeItemStack.equals(this.getStackInHand(hand))) {
				this.stopUsingItem();
			}
			else {
				if (!this.activeItemStack.isEmpty() && this.isUsingItem()) {
					ItemStack itemStack = this.activeItemStack.finishUsing(this.getEntityWorld(), this);
					if (itemStack != this.activeItemStack) {
						this.setStackInHand(hand, itemStack);
					}

					this.clearActiveItem();
				}
			}
		}
	}

	public void giveOrDropStack(ItemStack stack) {
	}

	public ItemStack getActiveItem() {
		return this.activeItemStack;
	}

	public int getItemUseTimeLeft() {
		return this.itemUseTimeLeft;
	}

	public int getItemUseTime() {
		return this.isUsingItem() ? this.activeItemStack.getMaxUseTime(this) - this.getItemUseTimeLeft() : 0;
	}

	public float getItemUseTime(float baseTime) {
		return !this.isUsingItem() ? 0.0F : this.getItemUseTime() + baseTime;
	}

	public void stopUsingItem() {
		ItemStack itemStack = this.getStackInHand(this.getActiveHand());
		if (!this.activeItemStack.isEmpty() && ItemStack.areItemsEqual(itemStack, this.activeItemStack)) {
			this.activeItemStack = itemStack;
			this.activeItemStack.onStoppedUsing(this.getEntityWorld(), this, this.getItemUseTimeLeft());
			if (this.activeItemStack.isUsedOnRelease()) {
				this.tickActiveItemStack();
			}
		}

		this.clearActiveItem();
	}

	/**
	 * Очищает активный предмет, сбрасывает флаги использования и испускает игровое событие завершения.
	 */
	public void clearActiveItem() {
		if (!getEntityWorld().isClient()) {
			boolean wasUsingItem = isUsingItem();
			piercingCooldowns = null;
			setLivingFlag(1, false);

			if (wasUsingItem) {
				activeItemStack.emitUseGameEvent(this, GameEvent.ITEM_INTERACT_FINISH);
			}
		}

		activeItemStack = ItemStack.EMPTY;
		itemUseTimeLeft = 0;
	}

	public boolean isBlocking() {
		return this.getBlockingItem() != null;
	}

	public @Nullable ItemStack getBlockingItem() {
		if (!this.isUsingItem()) {
			return null;
		}
		else {
			BlocksAttacksComponent blocksAttacksComponent = this.activeItemStack.get(DataComponentTypes.BLOCKS_ATTACKS);
			if (blocksAttacksComponent != null) {
				int i = this.activeItemStack.getItem().getMaxUseTime(this.activeItemStack, this) - this.itemUseTimeLeft;
				if (i >= blocksAttacksComponent.getBlockDelayTicks()) {
					return this.activeItemStack;
				}
			}

			return null;
		}
	}

	public boolean isHoldingOntoLadder() {
		return this.isSneaking();
	}

	public boolean isGliding() {
		return this.getFlag(7);
	}

	@Override
	public boolean isInSwimmingPose() {
		return super.isInSwimmingPose() || !this.isGliding() && this.isInPose(EntityPose.GLIDING);
	}

	public int getGlidingTicks() {
		return this.glidingTicks;
	}

	public boolean teleport(double x, double y, double z, boolean particleEffects) {
		double originalX = this.getX();
		double originalY = this.getY();
		double originalZ = this.getZ();
		double targetY = y;
		BlockPos blockPos = BlockPos.ofFloored(x, y, z);
		World world = this.getEntityWorld();

		if (!world.isChunkLoaded(blockPos)) {
			this.requestTeleport(originalX, originalY, originalZ);
			return false;
		}

		boolean foundGround = false;

		while (!foundGround && blockPos.getY() > world.getBottomY()) {
			BlockPos below = blockPos.down();
			BlockState blockState = world.getBlockState(below);
			if (blockState.blocksMovement()) {
				foundGround = true;
			}
			else {
				targetY--;
				blockPos = below;
			}
		}

		if (!foundGround) {
			this.requestTeleport(originalX, originalY, originalZ);
			return false;
		}

		this.requestTeleport(x, targetY, z);
		boolean teleported = world.isSpaceEmpty(this) && !world.containsFluid(this.getBoundingBox());

		if (!teleported) {
			this.requestTeleport(originalX, originalY, originalZ);
			return false;
		}

		if (particleEffects) {
			world.sendEntityStatus(this, (byte) 46);
		}

		if (this instanceof PathAwareEntity pathAwareEntity) {
			pathAwareEntity.getNavigation().stop();
		}

		return true;
	}

	public boolean isAffectedBySplashPotions() {
		return !this.isDead();
	}

	public boolean isMobOrPlayer() {
		return true;
	}

	public void setNearbySongPlaying(BlockPos songPosition, boolean playing) {
	}

	public boolean canPickUpLoot() {
		return false;
	}

	@Override
	public final EntityDimensions getDimensions(EntityPose pose) {
		return pose == EntityPose.SLEEPING ? SLEEPING_DIMENSIONS : this.getBaseDimensions(pose).scaled(this.getScale());
	}

	protected EntityDimensions getBaseDimensions(EntityPose pose) {
		return this.getType().getDimensions().scaled(this.getScaleFactor());
	}

	public ImmutableList<EntityPose> getPoses() {
		return ImmutableList.of(EntityPose.STANDING);
	}

	public Box getBoundingBox(EntityPose pose) {
		EntityDimensions entityDimensions = this.getDimensions(pose);
		return new Box(
				-entityDimensions.width() / 2.0F,
				0.0,
				-entityDimensions.width() / 2.0F,
				entityDimensions.width() / 2.0F,
				entityDimensions.height(),
				entityDimensions.width() / 2.0F
		);
	}

	/**
	 * Проверяет, не задохнётся ли существо в данной позе (например, при смене позы в узком пространстве).
	 *
	 * @return boolean — результат операции
	 */
	protected boolean wouldNotSuffocateInPose(EntityPose pose) {
		Box box = this.getDimensions(pose).getBoxAt(this.getEntityPos());
		return this.getEntityWorld().isBlockSpaceEmpty(this, box);
	}

	@Override
	public boolean canUsePortals(boolean allowVehicles) {
		return super.canUsePortals(allowVehicles) && !this.isSleeping();
	}

	public Optional<BlockPos> getSleepingPosition() {
		return this.dataTracker.get(SLEEPING_POSITION);
	}

	public void setSleepingPosition(BlockPos pos) {
		this.dataTracker.set(SLEEPING_POSITION, Optional.of(pos));
	}

	/**
	 * Очищает sleeping position.
	 */
	public void clearSleepingPosition() {
		this.dataTracker.set(SLEEPING_POSITION, Optional.empty());
	}

	public boolean isSleeping() {
		return this.getSleepingPosition().isPresent();
	}

	/**
	 * Sleep.
	 *
	 * @param pos pos
	 */
	public void sleep(BlockPos pos) {
		if (this.hasVehicle()) {
			this.stopRiding();
		}

		BlockState blockState = this.getEntityWorld().getBlockState(pos);
		if (blockState.getBlock() instanceof BedBlock) {
			this.getEntityWorld().setBlockState(pos, blockState.with(BedBlock.OCCUPIED, true), 3);
		}

		this.setPose(EntityPose.SLEEPING);
		this.setPositionInBed(pos);
		this.setSleepingPosition(pos);
		this.setVelocity(Vec3d.ZERO);
		this.velocityDirty = true;
	}

	private void setPositionInBed(BlockPos pos) {
		this.setPosition(pos.getX() + 0.5, pos.getY() + 0.6875, pos.getZ() + 0.5);
	}

	private boolean isSleepingInBed() {
		return this
				.getSleepingPosition()
				.map(pos -> this.getEntityWorld().getBlockState(pos).getBlock() instanceof BedBlock)
				.orElse(false);
	}

	public void wakeUp() {
		this.getSleepingPosition().filter(this.getEntityWorld()::isChunkLoaded).ifPresent(pos -> {
			BlockState blockState = this.getEntityWorld().getBlockState(pos);
			if (blockState.getBlock() instanceof BedBlock) {
				Direction direction = blockState.get(BedBlock.FACING);
				this.getEntityWorld().setBlockState(pos, blockState.with(BedBlock.OCCUPIED, false), 3);
				Vec3d wakePos = BedBlock
					.findWakeUpPosition(this.getType(), this.getEntityWorld(), pos, direction, this.getYaw())
					.orElseGet(() -> {
						BlockPos above = pos.up();
						return new Vec3d(above.getX() + 0.5, above.getY() + 0.1, above.getZ() + 0.5);
					});
				Vec3d facingDir = Vec3d.ofBottomCenter(pos).subtract(wakePos).normalize();
				float wakeYaw = (float) MathHelper.wrapDegrees(
					MathHelper.atan2(facingDir.z, facingDir.x) * 180.0F / (float) Math.PI - 90.0
				);
				this.setPosition(wakePos.x, wakePos.y, wakePos.z);
				this.setYaw(wakeYaw);
				this.setPitch(0.0F);
			}
		});
		Vec3d currentPos = this.getEntityPos();
		this.setPose(EntityPose.STANDING);
		this.setPosition(currentPos.x, currentPos.y, currentPos.z);
		this.clearSleepingPosition();
	}

	public @Nullable Direction getSleepingDirection() {
		BlockPos blockPos = this.getSleepingPosition().orElse(null);
		return blockPos != null ? BedBlock.getDirection(this.getEntityWorld(), blockPos) : null;
	}

	@Override
	public boolean isInsideWall() {
		return !this.isSleeping() && super.isInsideWall();
	}

	public ItemStack getProjectileType(ItemStack stack) {
		return ItemStack.EMPTY;
	}

	private static byte getEquipmentBreakStatus(EquipmentSlot slot) {
		return switch (slot) {
			case MAINHAND -> 47;
			case OFFHAND -> 48;
			case HEAD -> 49;
			case CHEST -> 50;
			case FEET -> 52;
			case LEGS -> 51;
			case BODY -> 65;
			case SADDLE -> 68;
		};
	}

	public void sendEquipmentBreakStatus(Item item, EquipmentSlot slot) {
		this.getEntityWorld().sendEntityStatus(this, getEquipmentBreakStatus(slot));
		this.onEquipmentRemoved(this.getEquippedStack(slot), slot, this.attributes);
	}

	private void onEquipmentRemoved(ItemStack removedEquipment, EquipmentSlot slot, AttributeContainer container) {
		removedEquipment.applyAttributeModifiers(
				slot, (attribute, modifier) -> {
					EntityAttributeInstance entityAttributeInstance = container.getCustomInstance(attribute);
					if (entityAttributeInstance != null) {
						entityAttributeInstance.removeModifier(modifier);
					}
				}
		);
		EnchantmentHelper.removeLocationBasedEffects(removedEquipment, this, slot);
	}

	public final boolean canEquipFromDispenser(ItemStack stack) {
		if (!this.isAlive() || this.isSpectator()) {
			return false;
		}

		EquippableComponent equippableComponent = stack.get(DataComponentTypes.EQUIPPABLE);

		if (equippableComponent == null || !equippableComponent.dispensable()) {
			return false;
		}

		EquipmentSlot equipmentSlot = equippableComponent.slot();

		return this.canUseSlot(equipmentSlot)
			&& equippableComponent.allows(this.getType())
			&& this.getEquippedStack(equipmentSlot).isEmpty()
			&& this.canDispenserEquipSlot(equipmentSlot);
	}

	protected boolean canDispenserEquipSlot(EquipmentSlot slot) {
		return true;
	}

	public final EquipmentSlot getPreferredEquipmentSlot(ItemStack stack) {
		EquippableComponent equippableComponent = stack.get(DataComponentTypes.EQUIPPABLE);
		return equippableComponent != null && this.canUseSlot(equippableComponent.slot()) ? equippableComponent.slot()
		                                                                                  : EquipmentSlot.MAINHAND;
	}

	public final boolean canEquip(ItemStack stack, EquipmentSlot slot) {
		EquippableComponent equippableComponent = stack.get(DataComponentTypes.EQUIPPABLE);
		return equippableComponent == null
		       ? slot == EquipmentSlot.MAINHAND && this.canUseSlot(EquipmentSlot.MAINHAND)
		       : slot == equippableComponent.slot() && this.canUseSlot(equippableComponent.slot())
		         && equippableComponent.allows(this.getType());
	}

	private static StackReference getStackReference(LivingEntity entity, EquipmentSlot slot) {
		return slot != EquipmentSlot.HEAD && slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND
		       ? StackReference.of(
				entity,
				slot,
				stack -> stack.isEmpty() || entity.getPreferredEquipmentSlot(stack) == slot
		)
		       : StackReference.of(entity, slot);
	}

	private static @Nullable EquipmentSlot getEquipmentSlot(int slotId) {
		if (slotId == 100 + EquipmentSlot.HEAD.getEntitySlotId()) {
			return EquipmentSlot.HEAD;
		}
		else if (slotId == 100 + EquipmentSlot.CHEST.getEntitySlotId()) {
			return EquipmentSlot.CHEST;
		}
		else if (slotId == 100 + EquipmentSlot.LEGS.getEntitySlotId()) {
			return EquipmentSlot.LEGS;
		}
		else if (slotId == 100 + EquipmentSlot.FEET.getEntitySlotId()) {
			return EquipmentSlot.FEET;
		}
		else if (slotId == EQUIPMENT_SLOT_ID) {
			return EquipmentSlot.MAINHAND;
		}
		else if (slotId == 99) {
			return EquipmentSlot.OFFHAND;
		}
		else if (slotId == ENTITY_STATUS_DEATH_SOUND) {
			return EquipmentSlot.BODY;
		}
		else {
			return slotId == ENTITY_STATUS_DEATH_PARTICLES ? EquipmentSlot.SADDLE : null;
		}
	}

	@Override
	public @Nullable StackReference getStackReference(int slot) {
		EquipmentSlot equipmentSlot = getEquipmentSlot(slot);
		return equipmentSlot != null ? getStackReference(this, equipmentSlot) : super.getStackReference(slot);
	}

	@Override
	public boolean canFreeze() {
		if (this.isSpectator()) {
			return false;
		}
		else {
			for (EquipmentSlot equipmentSlot : AttributeModifierSlot.ARMOR) {
				if (this.getEquippedStack(equipmentSlot).isIn(ItemTags.FREEZE_IMMUNE_WEARABLES)) {
					return false;
				}
			}

			return super.canFreeze();
		}
	}

	@Override
	public boolean isGlowing() {
		return !this.getEntityWorld().isClient() && this.hasStatusEffect(StatusEffects.GLOWING) || super.isGlowing();
	}

	@Override
	public float getBodyYaw() {
		return this.bodyYaw;
	}

	@Override
	public void onSpawnPacket(EntitySpawnS2CPacket packet) {
		double spawnX = packet.getX();
		double spawnY = packet.getY();
		double spawnZ = packet.getZ();
		float spawnYaw = packet.getYaw();
		float spawnPitch = packet.getPitch();
		this.updateTrackedPosition(spawnX, spawnY, spawnZ);
		this.bodyYaw = packet.getHeadYaw();
		this.headYaw = packet.getHeadYaw();
		this.lastBodyYaw = this.bodyYaw;
		this.lastHeadYaw = this.headYaw;
		this.setId(packet.getEntityId());
		this.setUuid(packet.getUuid());
		this.updatePositionAndAngles(spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
		this.setVelocity(packet.getVelocity());
	}

	public float getWeaponDisableBlockingForSeconds() {
		ItemStack itemStack = this.getWeaponStack();
		WeaponComponent weaponComponent = itemStack.get(DataComponentTypes.WEAPON);
		return weaponComponent != null && itemStack == this.getActiveOrMainHandStack()
		       ? weaponComponent.disableBlockingForSeconds() : 0.0F;
	}

	@Override
	public float getStepHeight() {
		float stepHeight = (float) this.getAttributeValue(EntityAttributes.STEP_HEIGHT);
		return this.getControllingPassenger() instanceof PlayerEntity ? Math.max(stepHeight, 1.0F) : stepHeight;
	}

	@Override
	public Vec3d getPassengerRidingPos(Entity passenger) {
		return this
				.getEntityPos()
				.add(this.getPassengerAttachmentPos(
						passenger,
						this.getDimensions(this.getPose()),
						this.getScale() * this.getScaleFactor()
				));
	}

	protected void lerpHeadYaw(int headTrackingIncrements, double serverHeadYaw) {
		this.headYaw =
				(float) MathHelper.lerpAngleDegrees(1.0 / headTrackingIncrements, (double) this.headYaw, serverHeadYaw);
	}

	@Override
	public void setOnFireForTicks(int ticks) {
		super.setOnFireForTicks(MathHelper.ceil(ticks * this.getAttributeValue(EntityAttributes.BURNING_TIME)));
	}

	public boolean isInCreativeMode() {
		return false;
	}

	public boolean isInvulnerableTo(ServerWorld world, DamageSource source) {
		return this.isAlwaysInvulnerableTo(source) || EnchantmentHelper.isInvulnerableTo(world, this, source);
	}

	public static boolean canGlideWith(ItemStack stack, EquipmentSlot slot) {
		if (!stack.contains(DataComponentTypes.GLIDER)) {
			return false;
		}
		else {
			EquippableComponent equippableComponent = stack.get(DataComponentTypes.EQUIPPABLE);
			return equippableComponent != null && slot == equippableComponent.slot() && !stack.willBreakNextUse();
		}
	}

	@VisibleForTesting
	public int getPlayerHitTimer() {
		return this.playerHitTimer;
	}

	@Override
	public boolean hasWaypoint() {
		return this.getAttributeValue(EntityAttributes.WAYPOINT_TRANSMIT_RANGE) > 0.0;
	}

	@Override
	public Optional<ServerWaypoint.WaypointTracker> createTracker(ServerPlayerEntity receiver) {
		if (this.firstUpdate || receiver == this) {
			return Optional.empty();
		}
		else if (ServerWaypoint.cannotReceive(this, receiver)) {
			return Optional.empty();
		}
		else {
			Waypoint.Config config = this.waypointConfig.withTeamColorOf(this);
			if (ServerWaypoint.shouldUseAzimuth(this, receiver)) {
				return Optional.of(new ServerWaypoint.AzimuthWaypointTracker(this, config, receiver));
			}
			else {
				return !ServerWaypoint.canReceive(this.getChunkPos(), receiver)
				       ? Optional.of(new ServerWaypoint.ChunkWaypointTracker(this, config, receiver))
				       : Optional.of(new ServerWaypoint.PositionalWaypointTracker(this, config, receiver));
			}
		}
	}

	@Override
	public Waypoint.Config getWaypointConfig() {
		return this.waypointConfig;
	}

	/** Пара звуков падения: {@code small} — при небольшом падении, {@code big} — при сильном ударе. */
	public record FallSounds(SoundEvent small, SoundEvent big) {
	}
}

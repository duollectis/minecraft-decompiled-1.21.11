package net.minecraft.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleListIterator;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.data.DataTracked;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.StackReference;
import net.minecraft.inventory.StackReferenceGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.*;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.*;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.*;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.DebugTrackable;
import net.minecraft.world.dimension.NetherPortal;
import net.minecraft.world.dimension.PortalManager;
import net.minecraft.world.entity.EntityChangeListener;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.listener.EntityGameEventHandler;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.waypoint.ServerWaypoint;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Базовый класс для всех игровых сущностей.
 * <p>Управляет позицией, скоростью, физикой движения, коллизиями, сериализацией
 * и взаимодействием с миром. Конкретные сущности (игроки, мобы, снаряды)
 * наследуют этот класс и расширяют его поведение.</p>
 */
public abstract class Entity
		implements DataTracked,
		DebugTrackable,
		Nameable,
		HeldItemContext,
		StackReferenceGetter,
		EntityLike,
		ScoreHolder,
		ComponentsAccess,
		AttachmentTarget {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final String ID_KEY = "id";
	public static final String UUID_KEY = "UUID";
	public static final String PASSENGERS_KEY = "Passengers";
	public static final String CUSTOM_DATA_KEY = "data";
	public static final String POS_KEY = "Pos";
	public static final String MOTION_KEY = "Motion";
	public static final String ROTATION_KEY = "Rotation";
	public static final String PORTAL_COOLDOWN_KEY = "PortalCooldown";
	public static final String NO_GRAVITY_KEY = "NoGravity";
	public static final String AIR_KEY = "Air";
	public static final String ON_GROUND_KEY = "OnGround";
	public static final String FALL_DISTANCE_KEY = "fall_distance";
	public static final String FIRE_KEY = "Fire";
	public static final String SILENT_KEY = "Silent";
	public static final String GLOWING_KEY = "Glowing";
	public static final String INVULNERABLE_KEY = "Invulnerable";
	public static final String CUSTOM_NAME_KEY = "CustomName";
	private static final AtomicInteger CURRENT_ID = new AtomicInteger();
	public static final int INITIAL_ID = 0;
	public static final int MAX_RIDING_COOLDOWN = 60;
	public static final int DEFAULT_PORTAL_COOLDOWN = 300;
	public static final int DEFAULT_MAX_AIR = 300;
	/** Интервал нанесения урона от огня (в тиках). */
	public static final int FIRE_DAMAGE_INTERVAL = 20;
	/** Магическое число статуса для эффекта мёда (EntityStatuses). */
	private static final byte STATUS_HONEY_BLOCK_SLIDE = 53;
	/** Максимальная координата мира по осям X/Z. */
	private static final double WORLD_BORDER_COORD = 3.0000512E7;
	public static final int MAX_COMMAND_TAGS = 1024;
	private static final Codec<List<String>> TAG_LIST_CODEC = Codec.STRING.sizeLimitedListOf(MAX_COMMAND_TAGS);
	public static final float MOVEMENT_SPEED_THRESHOLD = 0.2F;
	public static final double POSITION_LOWER_BOUND = 0.500001;
	public static final double POSITION_UPPER_BOUND = 0.999999;
	public static final int DEFAULT_MIN_FREEZE_DAMAGE_TICKS = 140;
	public static final int FREEZING_DAMAGE_INTERVAL = 40;
	public static final int TELEPORT_ATTEMPTS = 3;
	private static final Box NULL_BOX = new Box(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
	private static final double SPEED_IN_WATER = 0.014;
	private static final double SPEED_IN_LAVA_IN_NETHER = 0.007;
	private static final double SPEED_IN_LAVA = 0.0023333333333333335;
	private static final int COLLISION_CHECK_RADIUS = 16;
	private static final double COLLISION_CHECK_HALF_RADIUS = 8.0;
	private static double renderDistanceMultiplier = 1.0;
	private final EntityType<?> type;
	private boolean alwaysSyncAbsolute;
	private int id = CURRENT_ID.incrementAndGet();
	public boolean intersectionChecked;
	private ImmutableList<Entity> passengerList = ImmutableList.of();
	public int ridingCooldown;
	private @Nullable Entity vehicle;
	private World world;
	public double lastX;
	public double lastY;
	public double lastZ;
	private Vec3d pos;
	private BlockPos blockPos;
	private ChunkPos chunkPos;
	private Vec3d velocity = Vec3d.ZERO;
	private float yaw;
	private float pitch;
	public float lastYaw;
	public float lastPitch;
	private Box boundingBox = NULL_BOX;
	private boolean onGround;
	public boolean horizontalCollision;
	public boolean verticalCollision;
	public boolean groundCollision;
	public boolean collidedSoftly;
	public boolean knockedBack;
	protected Vec3d movementMultiplier = Vec3d.ZERO;
	private Entity.@Nullable RemovalReason removalReason;
	public static final float DEFAULT_FRICTION = 0.6F;
	public static final float MIN_RISING_BUBBLE_COLUMN_SPEED = 1.8F;
	public float distanceTraveled;
	public float speed;
	public double fallDistance;
	private float nextStepSoundDistance = 1.0F;
	public double lastRenderX;
	public double lastRenderY;
	public double lastRenderZ;
	public boolean noClip;
	protected final Random random = Random.create();
	public int age;
	private int fireTicks;
	protected boolean touchingWater;
	protected Object2DoubleMap<TagKey<Fluid>> fluidHeight = new Object2DoubleArrayMap(2);
	protected boolean submergedInWater;
	private final Set<TagKey<Fluid>> submergedFluidTag = new HashSet<>();
	public int timeUntilRegen;
	protected boolean firstUpdate = true;
	protected final DataTracker dataTracker;
	protected static final TrackedData<Byte>
			FLAGS =
			DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.BYTE);
	protected static final int ON_FIRE_FLAG_INDEX = 0;
	private static final int SNEAKING_FLAG_INDEX = 1;
	private static final int SPRINTING_FLAG_INDEX = 3;
	private static final int SWIMMING_FLAG_INDEX = 4;
	private static final int INVISIBLE_FLAG_INDEX = 5;
	protected static final int GLOWING_FLAG_INDEX = 6;
	protected static final int GLIDING_FLAG_INDEX = 7;
	private static final TrackedData<Integer>
			AIR =
			DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Optional<Text>>
			CUSTOM_NAME =
			DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.OPTIONAL_TEXT_COMPONENT);
	private static final TrackedData<Boolean>
			NAME_VISIBLE =
			DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean>
			SILENT =
			DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean>
			NO_GRAVITY =
			DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.BOOLEAN);
	protected static final TrackedData<EntityPose>
			POSE =
			DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.ENTITY_POSE);
	private static final TrackedData<Integer>
			FROZEN_TICKS =
			DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.INTEGER);
	private EntityChangeListener changeListener = EntityChangeListener.NONE;
	private final TrackedPosition trackedPosition = new TrackedPosition();
	public boolean velocityDirty;
	public @Nullable PortalManager portalManager;
	private int portalCooldown;
	private boolean invulnerable;
	protected UUID uuid = MathHelper.randomUuid(this.random);
	protected String uuidString = this.uuid.toString();
	private boolean glowing;
	private final Set<String> commandTags = Sets.newHashSet();
	private final double[] pistonMovementDelta = new double[]{0.0, 0.0, 0.0};
	private long pistonMovementTick;
	private EntityDimensions dimensions;
	private float standingEyeHeight;
	public boolean inPowderSnow;
	public boolean wasInPowderSnow;
	public Optional<BlockPos> supportingBlockPos = Optional.empty();
	private boolean forceUpdateSupportingBlockPos = false;
	private float lastChimeIntensity;
	private int lastChimeAge;
	private boolean hasVisualFire;
	private Vec3d movement = Vec3d.ZERO;
	private @Nullable Vec3d lastPos;
	private @Nullable BlockState stateAtPos = null;
	public static final int MAX_QUEUED_COLLISION_CHECKS = 100;
	private final ArrayDeque<Entity.QueuedCollisionCheck> queuedCollisionChecks = new ArrayDeque<>(MAX_QUEUED_COLLISION_CHECKS);
	private final List<Entity.QueuedCollisionCheck> currentlyCheckedCollisions = new ObjectArrayList();
	private final LongSet collidedBlockPositions = new LongOpenHashSet();
	private final EntityCollisionHandler.Impl collisionHandler = new EntityCollisionHandler.Impl();
	private NbtComponent customData = NbtComponent.DEFAULT;

	public Entity(EntityType<?> type, World world) {
		this.type = type;
		this.world = world;
		this.dimensions = type.getDimensions();
		this.pos = Vec3d.ZERO;
		this.blockPos = BlockPos.ORIGIN;
		this.chunkPos = ChunkPos.ORIGIN;
		DataTracker.Builder builder = new DataTracker.Builder(this);
		builder.add(FLAGS, (byte) 0);
		builder.add(AIR, this.getMaxAir());
		builder.add(NAME_VISIBLE, false);
		builder.add(CUSTOM_NAME, Optional.empty());
		builder.add(SILENT, false);
		builder.add(NO_GRAVITY, false);
		builder.add(POSE, EntityPose.STANDING);
		builder.add(FROZEN_TICKS, 0);
		this.initDataTracker(builder);
		this.dataTracker = builder.build();
		this.setPosition(0.0, 0.0, 0.0);
		this.standingEyeHeight = this.dimensions.eyeHeight();
	}

	public boolean collidesWithStateAtPos(BlockPos pos, BlockState state) {
		VoxelShape voxelShape = state.getCollisionShape(this.getEntityWorld(), pos, ShapeContext.of(this)).offset(pos);
		return VoxelShapes.matchesAnywhere(
				voxelShape,
				VoxelShapes.cuboid(this.getBoundingBox()),
				BooleanBiFunction.AND
		);
	}

	public int getTeamColorValue() {
		AbstractTeam abstractTeam = this.getScoreboardTeam();
		return abstractTeam != null && abstractTeam.getColor().getColorValue() != null ? abstractTeam
		                                                                                 .getColor()
		                                                                                 .getColorValue() : 16777215;
	}

	public boolean isSpectator() {
		return false;
	}

	public boolean isInteractable() {
		return this.isAlive() && !this.isRemoved() && !this.isSpectator();
	}

	public final void detach() {
		if (this.hasPassengers()) {
			this.removeAllPassengers();
		}

		if (this.hasVehicle()) {
			this.stopRiding();
		}
	}

	public void updateTrackedPosition(double x, double y, double z) {
		this.trackedPosition.setPos(new Vec3d(x, y, z));
	}

	public TrackedPosition getTrackedPosition() {
		return this.trackedPosition;
	}

	public EntityType<?> getType() {
		return this.type;
	}

	public boolean shouldAlwaysSyncAbsolute() {
		return this.alwaysSyncAbsolute;
	}

	public void setAlwaysSyncAbsolute(boolean alwaysSyncAbsolute) {
		this.alwaysSyncAbsolute = alwaysSyncAbsolute;
	}

	@Override
	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Set<String> getCommandTags() {
		return this.commandTags;
	}

	public boolean addCommandTag(String tag) {
		return commandTags.size() < MAX_COMMAND_TAGS && commandTags.add(tag);
	}

	public boolean removeCommandTag(String tag) {
		return commandTags.remove(tag);
	}

	public void kill(ServerWorld world) {
		this.remove(Entity.RemovalReason.KILLED);
		this.emitGameEvent(GameEvent.ENTITY_DIE);
	}

	public final void discard() {
		this.remove(Entity.RemovalReason.DISCARDED);
	}

	protected abstract void initDataTracker(DataTracker.Builder builder);

	public DataTracker getDataTracker() {
		return this.dataTracker;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Entity other && other.id == id;
	}

	@Override
	public int hashCode() {
		return this.id;
	}

	public void remove(Entity.RemovalReason reason) {
		this.setRemoved(reason);
	}

	public void onRemoved() {
	}

	public void onRemove(Entity.RemovalReason reason) {
	}

	public void setPose(EntityPose pose) {
		this.dataTracker.set(POSE, pose);
	}

	public EntityPose getPose() {
		return this.dataTracker.get(POSE);
	}

	public boolean isInPose(EntityPose pose) {
		return this.getPose() == pose;
	}

	public boolean isInRange(Entity entity, double radius) {
		return this.getEntityPos().isInRange(entity.getEntityPos(), radius);
	}

	public boolean isInRange(Entity entity, double horizontalRadius, double verticalRadius) {
		double deltaX = entity.getX() - this.getX();
		double deltaY = entity.getY() - this.getY();
		double deltaZ = entity.getZ() - this.getZ();
		return MathHelper.squaredHypot(deltaX, deltaZ) < MathHelper.square(horizontalRadius)
				&& MathHelper.square(deltaY) < MathHelper.square(verticalRadius);
	}

	protected void setRotation(float yaw, float pitch) {
		this.setYaw(yaw % 360.0F);
		this.setPitch(pitch % 360.0F);
	}

	public final void setPosition(Vec3d pos) {
		this.setPosition(pos.getX(), pos.getY(), pos.getZ());
	}

	public void setPosition(double x, double y, double z) {
		this.setPos(x, y, z);
		this.setBoundingBox(this.calculateBoundingBox());
	}

	protected final Box calculateBoundingBox() {
		return this.calculateDefaultBoundingBox(this.pos);
	}

	protected Box calculateDefaultBoundingBox(Vec3d pos) {
		return this.dimensions.getBoxAt(pos);
	}

	protected void refreshPosition() {
		this.lastPos = null;
		this.setPosition(this.pos.x, this.pos.y, this.pos.z);
	}

	public void changeLookDirection(double cursorDeltaX, double cursorDeltaY) {
		float pitchDelta = (float) cursorDeltaY * 0.15F;
		float yawDelta = (float) cursorDeltaX * 0.15F;
		this.setPitch(this.getPitch() + pitchDelta);
		this.setYaw(this.getYaw() + yawDelta);
		this.setPitch(MathHelper.clamp(this.getPitch(), -90.0F, 90.0F));
		this.lastPitch += pitchDelta;
		this.lastYaw += yawDelta;
		this.lastPitch = MathHelper.clamp(this.lastPitch, -90.0F, 90.0F);
		if (this.vehicle != null) {
			this.vehicle.onPassengerLookAround(this);
		}
	}

	public void beforePacketsSent() {
	}

	public void tick() {
		this.baseTick();
	}

	public void baseTick() {
		Profiler profiler = Profilers.get();
		profiler.push("entityBaseTick");
		this.tickLastPos();
		this.stateAtPos = null;
		if (this.hasVehicle() && this.getVehicle().isRemoved()) {
			this.stopRiding();
		}

		if (this.ridingCooldown > 0) {
			this.ridingCooldown--;
		}

		this.tickPortalTeleportation();
		if (this.shouldSpawnSprintingParticles()) {
			this.spawnSprintingParticles();
		}

		this.wasInPowderSnow = this.inPowderSnow;
		this.inPowderSnow = false;
		this.updateWaterState();
		this.updateSubmergedInWaterState();
		this.updateSwimming();
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			if (this.fireTicks > 0) {
				if (this.isFireImmune()) {
					this.extinguish();
				}
				else {
					if (this.fireTicks % FIRE_DAMAGE_INTERVAL == 0 && !this.isInLava()) {
						this.damage(serverWorld, this.getDamageSources().onFire(), 1.0F);
					}

					this.setFireTicks(this.fireTicks - 1);
				}
			}
		}
		else {
			this.extinguish();
		}

		if (this.isInLava()) {
			this.fallDistance *= 0.5;
		}

		this.attemptTickInVoid();
		if (!this.getEntityWorld().isClient()) {
			this.setOnFire(this.fireTicks > 0);
		}

		this.firstUpdate = false;
		if (this.getEntityWorld() instanceof ServerWorld serverWorldx && this instanceof Leashable) {
			Leashable.tickLeash(serverWorldx, (Entity & Leashable) this);
		}

		profiler.pop();
	}

	protected void tickLastPos() {
		if (this.lastPos == null) {
			this.lastPos = this.getEntityPos();
		}

		this.movement = this.getEntityPos().subtract(this.lastPos);
		this.lastPos = this.getEntityPos();
	}

	public void setOnFire(boolean onFire) {
		this.setFlag(ON_FIRE_FLAG_INDEX, onFire || this.hasVisualFire);
	}

	public void attemptTickInVoid() {
		if (this.getY() < this.getEntityWorld().getBottomY() - 64) {
			this.tickInVoid();
		}
	}

	public void resetPortalCooldown() {
		this.portalCooldown = this.getDefaultPortalCooldown();
	}

	public void setPortalCooldown(int portalCooldown) {
		this.portalCooldown = portalCooldown;
	}

	public int getPortalCooldown() {
		return this.portalCooldown;
	}

	public boolean hasPortalCooldown() {
		return this.portalCooldown > 0;
	}

	protected void tickPortalCooldown() {
		if (this.hasPortalCooldown()) {
			this.portalCooldown--;
		}
	}

	public void igniteByLava() {
		if (!this.isFireImmune()) {
			this.setOnFireFor(15.0F);
		}
	}

	public void setOnFireFromLava() {
		if (!this.isFireImmune()) {
			if (this.getEntityWorld() instanceof ServerWorld serverWorld
					&& this.damage(serverWorld, this.getDamageSources().lava(), 4.0F)
					&& this.shouldPlayBurnSoundInLava()
					&& !this.isSilent()) {
				serverWorld.playSound(
						null,
						this.getX(),
						this.getY(),
						this.getZ(),
						SoundEvents.ENTITY_GENERIC_BURN,
						this.getSoundCategory(),
						0.4F,
						2.0F + this.random.nextFloat() * 0.4F
				);
			}
		}
	}

	protected boolean shouldPlayBurnSoundInLava() {
		return true;
	}

	public final void setOnFireFor(float seconds) {
		this.setOnFireForTicks(MathHelper.floor(seconds * 20.0F));
	}

	public void setOnFireForTicks(int ticks) {
		if (this.fireTicks < ticks) {
			this.setFireTicks(ticks);
		}

		this.defrost();
	}

	public void setFireTicks(int fireTicks) {
		this.fireTicks = fireTicks;
	}

	public int getFireTicks() {
		return this.fireTicks;
	}

	public void extinguish() {
		this.setFireTicks(Math.min(0, this.getFireTicks()));
	}

	protected void tickInVoid() {
		this.discard();
	}

	public boolean doesNotCollide(double offsetX, double offsetY, double offsetZ) {
		return this.doesNotCollide(this.getBoundingBox().offset(offsetX, offsetY, offsetZ));
	}

	private boolean doesNotCollide(Box box) {
		return this.getEntityWorld().isSpaceEmpty(this, box) && !this.getEntityWorld().containsFluid(box);
	}

	public void setOnGround(boolean onGround) {
		this.onGround = onGround;
		this.updateSupportingBlockPos(onGround, null);
	}

	public void setMovement(boolean onGround, Vec3d movement) {
		this.setMovement(onGround, this.horizontalCollision, movement);
	}

	public void setMovement(boolean onGround, boolean horizontalCollision, Vec3d movement) {
		this.onGround = onGround;
		this.horizontalCollision = horizontalCollision;
		this.updateSupportingBlockPos(onGround, movement);
	}

	public boolean isSupportedBy(BlockPos pos) {
		return this.supportingBlockPos.isPresent() && this.supportingBlockPos.get().equals(pos);
	}

	protected void updateSupportingBlockPos(boolean onGround, @Nullable Vec3d movement) {
		if (onGround) {
			Box box = this.getBoundingBox();
			Box box2 = new Box(box.minX, box.minY - 1.0E-6, box.minZ, box.maxX, box.minY, box.maxZ);
			Optional<BlockPos> optional = this.world.findSupportingBlockPos(this, box2);
			if (optional.isPresent() || this.forceUpdateSupportingBlockPos) {
				this.supportingBlockPos = optional;
			}
			else if (movement != null) {
				Box box3 = box2.offset(-movement.x, 0.0, -movement.z);
				optional = this.world.findSupportingBlockPos(this, box3);
				this.supportingBlockPos = optional;
			}

			this.forceUpdateSupportingBlockPos = optional.isEmpty();
		}
		else {
			this.forceUpdateSupportingBlockPos = false;
			if (this.supportingBlockPos.isPresent()) {
				this.supportingBlockPos = Optional.empty();
			}
		}
	}

	public boolean isOnGround() {
		return this.onGround;
	}

	public void move(MovementType type, Vec3d movement) {
		if (this.noClip) {
			this.setPosition(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
			this.horizontalCollision = false;
			this.verticalCollision = false;
			this.groundCollision = false;
			this.collidedSoftly = false;
		}
		else {
			if (type == MovementType.PISTON) {
				movement = this.adjustMovementForPiston(movement);
				if (movement.equals(Vec3d.ZERO)) {
					return;
				}
			}

			Profiler profiler = Profilers.get();
			profiler.push("move");
			if (this.movementMultiplier.lengthSquared() > 1.0E-7) {
				if (type != MovementType.PISTON) {
					movement = movement.multiply(this.movementMultiplier);
				}

				this.movementMultiplier = Vec3d.ZERO;
				this.setVelocity(Vec3d.ZERO);
			}

			movement = this.adjustMovementForSneaking(movement, type);
			Vec3d adjustedMovement = this.adjustMovementForCollisions(movement);
			double adjustedLengthSq = adjustedMovement.lengthSquared();
			if (adjustedLengthSq > 1.0E-7 || movement.lengthSquared() - adjustedLengthSq < 1.0E-7) {
				if (this.fallDistance != 0.0 && adjustedLengthSq >= 1.0) {
					double raycastDistance = Math.min(adjustedMovement.length(), 8.0);
					Vec3d raycastTarget = this.getEntityPos().add(adjustedMovement.normalize().multiply(raycastDistance));
					BlockHitResult blockHitResult = this.getEntityWorld()
					                                    .raycast(
							                                    new RaycastContext(
									                                    this.getEntityPos(),
									                                    raycastTarget,
									                                    RaycastContext.ShapeType.FALLDAMAGE_RESETTING,
									                                    RaycastContext.FluidHandling.WATER,
									                                    this
							                                    )
					                                    );
					if (blockHitResult.getType() != HitResult.Type.MISS) {
						this.onLanding();
					}
				}
	
				Vec3d fromPos = this.getEntityPos();
				Vec3d toPos = fromPos.add(adjustedMovement);
				this.addQueuedCollisionChecks(new Entity.QueuedCollisionCheck(fromPos, toPos, movement));
				this.setPosition(toPos);
			}
	
			profiler.pop();
			profiler.push("rest");
			boolean xCollision = !MathHelper.approximatelyEquals(movement.x, adjustedMovement.x);
			boolean zCollision = !MathHelper.approximatelyEquals(movement.z, adjustedMovement.z);
			this.horizontalCollision = xCollision || zCollision;
			if (Math.abs(movement.y) > 0.0 || this.isLogicalSideForUpdatingMovement()) {
				this.verticalCollision = movement.y != adjustedMovement.y;
				this.groundCollision = this.verticalCollision && movement.y < 0.0;
				this.setMovement(this.groundCollision, this.horizontalCollision, adjustedMovement);
			}
	
			this.collidedSoftly = this.horizontalCollision && this.hasCollidedSoftly(adjustedMovement);
	
			BlockPos landingPos = this.getLandingPos();
			BlockState blockState = this.getEntityWorld().getBlockState(landingPos);
			if (this.isLogicalSideForUpdatingMovement()) {
				this.fall(adjustedMovement.y, this.isOnGround(), blockState, landingPos);
			}
	
			if (this.isRemoved()) {
				profiler.pop();
			}
			else {
				if (this.horizontalCollision) {
					Vec3d currentVelocity = this.getVelocity();
					this.setVelocity(xCollision ? 0.0 : currentVelocity.x, currentVelocity.y, zCollision ? 0.0 : currentVelocity.z);
				}
	
				if (this.canMoveVoluntarily()) {
					Block block = blockState.getBlock();
					if (movement.y != adjustedMovement.y) {
						block.onEntityLand(this.getEntityWorld(), this);
					}
				}
	
				if (!this.getEntityWorld().isClient() || this.isLogicalSideForUpdatingMovement()) {
					Entity.MoveEffect moveEffect = this.getMoveEffect();
					if (moveEffect.hasAny() && !this.hasVehicle()) {
						this.applyMoveEffect(moveEffect, adjustedMovement, blockPos, blockState);
					}
				}
	
				float velocityMultiplier = this.getVelocityMultiplier();
				this.setVelocity(this.getVelocity().multiply(velocityMultiplier, 1.0, velocityMultiplier));
				profiler.pop();
			}
		}
	}

	private void applyMoveEffect(
			Entity.MoveEffect moveEffect,
			Vec3d movement,
			BlockPos landingPos,
			BlockState landingState
	) {
		float totalLength = (float) (movement.length() * DEFAULT_FRICTION);
		float horizontalLength = (float) (movement.horizontalLength() * DEFAULT_FRICTION);
		BlockPos steppingPos = this.getSteppingPos();
		BlockState blockState = this.getEntityWorld().getBlockState(steppingPos);
		boolean canClimb = this.canClimb(blockState);
		this.distanceTraveled += canClimb ? totalLength : horizontalLength;
		this.speed += totalLength;
		if (this.distanceTraveled > this.nextStepSoundDistance && !blockState.isAir()) {
			boolean isOnLandingBlock = blockPos.equals(landingPos);
			boolean steppedOnBlock = this.stepOnBlock(landingPos, landingState, moveEffect.playsSounds(), isOnLandingBlock, movement);
			if (!isOnLandingBlock) {
				steppedOnBlock |= this.stepOnBlock(blockPos, blockState, false, moveEffect.emitsGameEvents(), movement);
			}

			if (steppedOnBlock) {
				this.nextStepSoundDistance = this.calculateNextStepSoundDistance();
			}
			else if (this.isTouchingWater()) {
				this.nextStepSoundDistance = this.calculateNextStepSoundDistance();
				if (moveEffect.playsSounds()) {
					this.playSwimSound();
				}

				if (moveEffect.emitsGameEvents()) {
					this.emitGameEvent(GameEvent.SWIM);
				}
			}
		}
		else if (blockState.isAir()) {
			this.addAirTravelEffects();
		}
	}

	protected void tickBlockCollision() {
		this.currentlyCheckedCollisions.clear();
		this.currentlyCheckedCollisions.addAll(this.queuedCollisionChecks);
		this.queuedCollisionChecks.clear();
		if (this.currentlyCheckedCollisions.isEmpty()) {
			this.currentlyCheckedCollisions.add(new Entity.QueuedCollisionCheck(
					this.getLastRenderPos(),
					this.getEntityPos()
			));
		}
		else if (this.currentlyCheckedCollisions.getLast().to.squaredDistanceTo(this.getEntityPos()) > 9.9999994E-11F) {
			this.currentlyCheckedCollisions.add(new Entity.QueuedCollisionCheck(
					this.currentlyCheckedCollisions.getLast().to,
					this.getEntityPos()
			));
		}

		this.tickBlockCollisions(this.currentlyCheckedCollisions);
	}

	private void addQueuedCollisionChecks(Entity.QueuedCollisionCheck queuedCollisionCheck) {
		if (this.queuedCollisionChecks.size() >= MAX_QUEUED_COLLISION_CHECKS) {
			Entity.QueuedCollisionCheck queuedCollisionCheck2 = this.queuedCollisionChecks.removeFirst();
			Entity.QueuedCollisionCheck queuedCollisionCheck3 = this.queuedCollisionChecks.removeFirst();
			Entity.QueuedCollisionCheck
					queuedCollisionCheck4 =
					new Entity.QueuedCollisionCheck(queuedCollisionCheck2.from(), queuedCollisionCheck3.to());
			this.queuedCollisionChecks.addFirst(queuedCollisionCheck4);
		}

		this.queuedCollisionChecks.add(queuedCollisionCheck);
	}

	public void popQueuedCollisionCheck() {
		if (!this.queuedCollisionChecks.isEmpty()) {
			this.queuedCollisionChecks.removeLast();
		}
	}

	protected void clearQueuedCollisionChecks() {
		this.queuedCollisionChecks.clear();
	}

	public boolean isMovingHorizontally() {
		return Math.abs(this.movement.horizontalLength()) > 1.0E-5F;
	}

	public void tickBlockCollision(Vec3d lastRenderPos, Vec3d pos) {
		this.tickBlockCollisions(List.of(new Entity.QueuedCollisionCheck(lastRenderPos, pos)));
	}

	private void tickBlockCollisions(List<Entity.QueuedCollisionCheck> checks) {
		if (!this.shouldTickBlockCollision()) {
			return;
		}

		if (this.isOnGround()) {
			BlockPos landingPos = this.getLandingPos();
			BlockState landingState = this.getEntityWorld().getBlockState(landingPos);
			landingState.getBlock().onSteppedOn(this.getEntityWorld(), landingPos, landingState, this);
		}

		boolean wasOnFire = this.isOnFire();
		boolean wasEscapingPowderSnow = this.shouldEscapePowderSnow();
		int fireTicksBefore = this.getFireTicks();
		this.checkBlockCollisions(checks, this.collisionHandler);
		this.collisionHandler.runCallbacks(this);

		if (this.isBeingRainedOn()) {
			this.extinguish();
		}

		if (wasOnFire && !this.isOnFire() || wasEscapingPowderSnow && !this.shouldEscapePowderSnow()) {
			this.playExtinguishSound();
		}

		boolean fireTicksIncreased = this.getFireTicks() > fireTicksBefore;
		if (!this.getEntityWorld().isClient() && !this.isOnFire() && !fireTicksIncreased) {
			this.setFireTicks(-this.getBurningDuration());
		}
	}

	protected boolean shouldTickBlockCollision() {
		return !this.isRemoved() && !this.noClip;
	}

	private boolean canClimb(BlockState state) {
		return state.isIn(BlockTags.CLIMBABLE) || state.isOf(Blocks.POWDER_SNOW);
	}

	private boolean stepOnBlock(BlockPos pos, BlockState state, boolean playSound, boolean emitEvent, Vec3d movement) {
		if (state.isAir()) {
			return false;
		}

		boolean canClimb = this.canClimb(state);
		boolean shouldStep = (this.isOnGround() || canClimb || this.isInSneakingPose() && movement.y == 0.0 || this.isOnRail())
				&& !this.isSwimming();

		if (!shouldStep) {
			return false;
		}

		if (playSound) {
			this.playStepSounds(pos, state);
		}

		if (emitEvent) {
			this.getEntityWorld().emitGameEvent(GameEvent.STEP, this.getEntityPos(), GameEvent.Emitter.of(this, state));
		}

		return true;
	}

	protected boolean hasCollidedSoftly(Vec3d adjustedMovement) {
		return false;
	}

	protected void playExtinguishSound() {
		if (!this.world.isClient()) {
			this.getEntityWorld()
			    .playSound(
					    null,
					    this.getX(),
					    this.getY(),
					    this.getZ(),
					    SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE,
					    this.getSoundCategory(),
					    0.7F,
					    1.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F
			    );
		}
	}

	public void extinguishWithSound() {
		if (this.isOnFire()) {
			this.playExtinguishSound();
		}

		this.extinguish();
	}

	protected void addAirTravelEffects() {
		if (this.isFlappingWings()) {
			this.addFlapEffects();
			if (this.getMoveEffect().emitsGameEvents()) {
				this.emitGameEvent(GameEvent.FLAP);
			}
		}
	}

	@Deprecated
	public BlockPos getLandingPos() {
		return this.getPosWithYOffset(MOVEMENT_SPEED_THRESHOLD);
	}

	public BlockPos getVelocityAffectingPos() {
		return this.getPosWithYOffset((float) POSITION_LOWER_BOUND);
	}

	public BlockPos getSteppingPos() {
		return this.getPosWithYOffset(1.0E-5F);
	}

	protected BlockPos getPosWithYOffset(float offset) {
		if (this.supportingBlockPos.isPresent()) {
			BlockPos supportPos = this.supportingBlockPos.get();
			if (offset <= 1.0E-5F) {
				return supportPos;
			}

			BlockState blockState = this.getEntityWorld().getBlockState(supportPos);
			return (!(offset <= 0.5) || !blockState.isIn(BlockTags.FENCES))
					       && !blockState.isIn(BlockTags.WALLS)
					       && !(blockState.getBlock() instanceof FenceGateBlock)
			       ? supportPos.withY(MathHelper.floor(this.pos.y - offset))
			       : supportPos;
		}

		int floorX = MathHelper.floor(this.pos.x);
		int floorY = MathHelper.floor(this.pos.y - offset);
		int floorZ = MathHelper.floor(this.pos.z);
		return new BlockPos(floorX, floorY, floorZ);
	}

	protected float getJumpVelocityMultiplier() {
		float blockMultiplier = this.getEntityWorld().getBlockState(this.getBlockPos()).getBlock().getJumpVelocityMultiplier();
		float velocityMultiplier = this.getEntityWorld().getBlockState(this.getVelocityAffectingPos()).getBlock().getJumpVelocityMultiplier();
		return blockMultiplier == 1.0F ? velocityMultiplier : blockMultiplier;
	}

	protected float getVelocityMultiplier() {
		BlockState blockState = this.getEntityWorld().getBlockState(this.getBlockPos());
		float multiplier = blockState.getBlock().getVelocityMultiplier();
		if (blockState.isOf(Blocks.WATER) || blockState.isOf(Blocks.BUBBLE_COLUMN)) {
			return multiplier;
		}

		return multiplier == 1.0F
				? this.getEntityWorld().getBlockState(this.getVelocityAffectingPos()).getBlock().getVelocityMultiplier()
				: multiplier;
	}

	protected Vec3d adjustMovementForSneaking(Vec3d movement, MovementType type) {
		return movement;
	}

	protected Vec3d adjustMovementForPiston(Vec3d movement) {
		if (movement.lengthSquared() <= 1.0E-7) {
			return movement;
		}
		else {
			long l = this.getEntityWorld().getTime();
			if (l != this.pistonMovementTick) {
				Arrays.fill(this.pistonMovementDelta, 0.0);
				this.pistonMovementTick = l;
			}

			if (movement.x != 0.0) {
				double d = this.calculatePistonMovementFactor(Direction.Axis.X, movement.x);
				return Math.abs(d) <= 1.0E-5F ? Vec3d.ZERO : new Vec3d(d, 0.0, 0.0);
			}
			else if (movement.y != 0.0) {
				double d = this.calculatePistonMovementFactor(Direction.Axis.Y, movement.y);
				return Math.abs(d) <= 1.0E-5F ? Vec3d.ZERO : new Vec3d(0.0, d, 0.0);
			}
			else if (movement.z != 0.0) {
				double d = this.calculatePistonMovementFactor(Direction.Axis.Z, movement.z);
				return Math.abs(d) <= 1.0E-5F ? Vec3d.ZERO : new Vec3d(0.0, 0.0, d);
			}
			else {
				return Vec3d.ZERO;
			}
		}
	}

	private double calculatePistonMovementFactor(Direction.Axis axis, double offsetFactor) {
		int axisIndex = axis.ordinal();
		double clamped = MathHelper.clamp(offsetFactor + this.pistonMovementDelta[axisIndex], -0.51, 0.51);
		double result = clamped - this.pistonMovementDelta[axisIndex];
		this.pistonMovementDelta[axisIndex] = clamped;
		return result;
	}

	public double calcDistanceFromBottomCollision(double checkedDistance) {
		Box box = this.getBoundingBox();
		Box box2 = box.withMinY(box.minY - checkedDistance).withMaxY(box.minY);
		List<VoxelShape> list = findCollisions(this, this.world, box2);
		return list.isEmpty() ? checkedDistance
		                      : -VoxelShapes.calculateMaxOffset(Direction.Axis.Y, box, list, -checkedDistance);
	}

	private Vec3d adjustMovementForCollisions(Vec3d movement) {
		Box box = this.getBoundingBox();
		List<VoxelShape> list = this.getEntityWorld().getEntityCollisions(this, box.stretch(movement));
		Vec3d adjusted = movement.lengthSquared() == 0.0
				? movement
				: adjustMovementForCollisions(this, movement, box, this.getEntityWorld(), list);
		boolean xBlocked = movement.x != adjusted.x;
		boolean yBlocked = movement.y != adjusted.y;
		boolean zBlocked = movement.z != adjusted.z;
		boolean fallingBlocked = yBlocked && movement.y < 0.0;
		if (this.getStepHeight() > 0.0F && (fallingBlocked || this.isOnGround()) && (xBlocked || zBlocked)) {
			Box stepBox = fallingBlocked ? box.offset(0.0, adjusted.y, 0.0) : box;
			Box stretchedBox = stepBox.stretch(movement.x, this.getStepHeight(), movement.z);
			if (!fallingBlocked) {
				stretchedBox = stretchedBox.stretch(0.0, -1.0E-5F, 0.0);
			}

			List<VoxelShape> stepCollisions = findCollisionsForMovement(this, this.world, list, stretchedBox);
			float currentYOffset = (float) adjusted.y;
			float[] stepHeights = collectStepHeights(stepBox, stepCollisions, this.getStepHeight(), currentYOffset);

			for (float stepHeight : stepHeights) {
				Vec3d stepped = adjustMovementForCollisions(new Vec3d(movement.x, stepHeight, movement.z), stepBox, stepCollisions);
				if (stepped.horizontalLengthSquared() > adjusted.horizontalLengthSquared()) {
					double yDelta = box.minY - stepBox.minY;
					return stepped.subtract(0.0, yDelta, 0.0);
				}
			}
		}

		return adjusted;
	}

	private static float[] collectStepHeights(
			Box collisionBox,
			List<VoxelShape> collisions,
			float maxStepHeight,
			float currentYOffset
	) {
		FloatSet floatSet = new FloatArraySet(4);

		for (VoxelShape voxelShape : collisions) {
			for (double pointY : voxelShape.getPointPositions(Direction.Axis.Y)) {
				float relativeHeight = (float) (pointY - collisionBox.minY);
				if (relativeHeight >= 0.0F && relativeHeight != maxStepHeight) {
					if (relativeHeight > currentYOffset) {
						break;
					}

					floatSet.add(relativeHeight);
				}
			}
		}

		float[] heights = floatSet.toFloatArray();
		FloatArrays.unstableSort(heights);
		return heights;
	}

	public static Vec3d adjustMovementForCollisions(
			@Nullable Entity entity,
			Vec3d movement,
			Box entityBoundingBox,
			World world,
			List<VoxelShape> collisions
	) {
		List<VoxelShape>
				list =
				findCollisionsForMovement(entity, world, collisions, entityBoundingBox.stretch(movement));
		return adjustMovementForCollisions(movement, entityBoundingBox, list);
	}

	public static List<VoxelShape> findCollisions(@Nullable Entity entity, World world, Box box) {
		List<VoxelShape> list = world.getEntityCollisions(entity, box);
		return findCollisionsForMovement(entity, world, list, box);
	}

	private static List<VoxelShape> findCollisionsForMovement(
			@Nullable Entity entity, World world, List<VoxelShape> regularCollisions, Box movingEntityBoundingBox
	) {
		Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(regularCollisions.size() + 1);
		if (!regularCollisions.isEmpty()) {
			builder.addAll(regularCollisions);
		}

		WorldBorder worldBorder = world.getWorldBorder();
		boolean bl = entity != null && worldBorder.canCollide(entity, movingEntityBoundingBox);
		if (bl) {
			builder.add(worldBorder.asVoxelShape());
		}

		builder.addAll(world.getBlockCollisions(entity, movingEntityBoundingBox));
		return builder.build();
	}

	private static Vec3d adjustMovementForCollisions(
			Vec3d movement,
			Box entityBoundingBox,
			List<VoxelShape> collisions
	) {
		if (collisions.isEmpty()) {
			return movement;
		}

		Vec3d result = Vec3d.ZERO;

		for (Direction.Axis axis : Direction.getCollisionOrder(movement)) {
			double component = movement.getComponentAlongAxis(axis);
			if (component != 0.0) {
				double offset = VoxelShapes.calculateMaxOffset(axis, entityBoundingBox.offset(result), collisions, component);
				result = result.withAxis(axis, offset);
			}
		}

		return result;
	}

	protected float calculateNextStepSoundDistance() {
		return (int) this.distanceTraveled + 1;
	}

	protected SoundEvent getSwimSound() {
		return SoundEvents.ENTITY_GENERIC_SWIM;
	}

	protected SoundEvent getSplashSound() {
		return SoundEvents.ENTITY_GENERIC_SPLASH;
	}

	protected SoundEvent getHighSpeedSplashSound() {
		return SoundEvents.ENTITY_GENERIC_SPLASH;
	}

	private void checkBlockCollisions(
			List<Entity.QueuedCollisionCheck> queuedCollisionChecks,
			EntityCollisionHandler.Impl collisionHandler
	) {
		if (!this.shouldTickBlockCollision()) {
			return;
		}

		LongSet collidedPositions = this.collidedBlockPositions;

		for (Entity.QueuedCollisionCheck check : queuedCollisionChecks) {
			Vec3d from = check.from;
			Vec3d delta = check.to().subtract(check.from());
			int remainingChecks = COLLISION_CHECK_RADIUS;

			if (check.axisDependentOriginalMovement().isPresent() && delta.lengthSquared() > 0.0) {
				for (Direction.Axis axis : Direction.getCollisionOrder(check.axisDependentOriginalMovement().get())) {
					double component = delta.getComponentAlongAxis(axis);
					if (component != 0.0) {
						Vec3d to = from.offset(axis.getPositiveDirection(), component);
						remainingChecks -= this.checkBlockCollision(from, to, collisionHandler, collidedPositions, remainingChecks);
						from = to;
					}
				}
			}
			else {
				remainingChecks -= this.checkBlockCollision(
						check.from(),
						check.to(),
						collisionHandler,
						collidedPositions,
						COLLISION_CHECK_RADIUS
				);
			}

			if (remainingChecks <= 0) {
				this.checkBlockCollision(check.to(), check.to(), collisionHandler, collidedPositions, 1);
			}
		}

		collidedPositions.clear();
	}

	private int checkBlockCollision(
			Vec3d from,
			Vec3d to,
			EntityCollisionHandler.Impl collisionHandler,
			LongSet collidedBlockPositions,
			int maxChecks
	) {
		Box box = this.calculateDefaultBoundingBox(to).contract(1.0E-5F);
		boolean isLongMove = from.squaredDistanceTo(to) > MathHelper.square(0.9999900000002526);
		boolean debugEnabled = this.world instanceof ServerWorld serverWorld
				&& serverWorld
				.getServer()
				.getSubscriberTracker()
				.hasSubscriber(DebugSubscriptionTypes.ENTITY_BLOCK_INTERSECTIONS);
		AtomicInteger lastCheckIndex = new AtomicInteger();
		BlockView.collectCollisionsBetween(
				from, to, box, (blockPos, checkIndex) -> {
					if (!this.isAlive()) {
						return false;
					}

					if (checkIndex >= maxChecks) {
						return false;
					}

					lastCheckIndex.set(checkIndex);
					BlockState blockState = this.getEntityWorld().getBlockState(blockPos);
					if (blockState.isAir()) {
						if (debugEnabled) {
							this.afterCollisionCheck(
									(ServerWorld) this.getEntityWorld(),
									blockPos.toImmutable(),
									false,
									false
							);
						}

						return true;
					}

					VoxelShape voxelShape = blockState.getInsideCollisionShape(this.getEntityWorld(), blockPos, this);
					boolean blockCollision = voxelShape == VoxelShapes.fullCube() || this.collides(
							from,
							to,
							voxelShape.offset(new Vec3d(blockPos)).getBoundingBoxes()
					);
					boolean fluidCollision = this.collidesWithFluid(blockState.getFluidState(), blockPos, from, to);
					if ((blockCollision || fluidCollision) && collidedBlockPositions.add(blockPos.asLong())) {
						if (blockCollision) {
							try {
								boolean isFullyInside = isLongMove || box.contains(blockPos);
								collisionHandler.updateIfNecessary(checkIndex);
								blockState.onEntityCollision(
										this.getEntityWorld(),
										blockPos,
										this,
										collisionHandler,
										isFullyInside
								);
								this.onBlockCollision(blockState);
							}
							catch (Throwable error) {
								CrashReport crashReport = CrashReport.create(error, "Colliding entity with block");
								CrashReportSection crashReportSection = crashReport.addElement("Block being collided with");
								CrashReportSection.addBlockInfo(
										crashReportSection,
										this.getEntityWorld(),
										blockPos,
										blockState
								);
								CrashReportSection crashReportSection2 = crashReport.addElement("Entity being checked for collision");
								this.populateCrashReport(crashReportSection2);
								throw new CrashException(crashReport);
							}
						}

						if (fluidCollision) {
							collisionHandler.updateIfNecessary(checkIndex);
							blockState
									.getFluidState()
									.onEntityCollision(this.getEntityWorld(), blockPos, this, collisionHandler);
						}

						if (debugEnabled) {
							this.afterCollisionCheck(
									(ServerWorld) this.getEntityWorld(),
									blockPos.toImmutable(),
									blockCollision,
									fluidCollision
							);
						}
					}

					return true;
				}
		);
		return lastCheckIndex.get() + 1;
	}

	private void afterCollisionCheck(ServerWorld world, BlockPos pos, boolean blockCollision, boolean fluidCollision) {
		EntityBlockIntersectionType intersectionType = fluidCollision
				? EntityBlockIntersectionType.IN_FLUID
				: blockCollision
						? EntityBlockIntersectionType.IN_BLOCK
						: EntityBlockIntersectionType.IN_AIR;

		world
				.getSubscriptionTracker()
				.sendBlockDebugData(pos, DebugSubscriptionTypes.ENTITY_BLOCK_INTERSECTIONS, intersectionType);
	}

	public boolean collidesWithFluid(FluidState state, BlockPos fluidPos, Vec3d oldPos, Vec3d newPos) {
		Box box = state.getCollisionBox(this.getEntityWorld(), fluidPos);
		return box != null && this.collides(oldPos, newPos, List.of(box));
	}

	public boolean collides(Vec3d oldPos, Vec3d newPos, List<Box> boxes) {
		Box box = this.calculateDefaultBoundingBox(oldPos);
		Vec3d vec3d = newPos.subtract(oldPos);
		return box.collides(vec3d, boxes);
	}

	protected void onBlockCollision(BlockState state) {
	}

	public BlockPos getWorldSpawnPos(ServerWorld world, BlockPos basePos) {
		BlockPos spawnPoint = world.getSpawnPoint().getPos();
		Vec3d center = spawnPoint.toCenterPos();
		int spawnY = world.getWorldChunk(spawnPoint)
				.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, spawnPoint.getX(), spawnPoint.getZ()) + 1;
		return BlockPos.ofFloored(center.x, spawnY, center.z);
	}

	public void emitGameEvent(RegistryEntry<GameEvent> event, @Nullable Entity entity) {
		this.getEntityWorld().emitGameEvent(entity, event, this.pos);
	}

	public void emitGameEvent(RegistryEntry<GameEvent> event) {
		this.emitGameEvent(event, this);
	}

	private void playStepSounds(BlockPos pos, BlockState state) {
		this.playStepSound(pos, state);
		if (this.shouldPlayAmethystChimeSound(state)) {
			this.playAmethystChimeSound();
		}
	}

	protected void playSwimSound() {
		Entity controller = Objects.requireNonNullElse(this.getControllingPassenger(), this);
		float volumeFactor = controller == this ? 0.35F : 0.4F;
		Vec3d velocity = controller.getVelocity();
		float volume = Math.min(
				1.0F,
				(float) Math.sqrt(
						velocity.x * velocity.x * MOVEMENT_SPEED_THRESHOLD
								+ velocity.y * velocity.y
								+ velocity.z * velocity.z * 0.2F
				) * volumeFactor
		);
		this.playSwimSound(volume);
	}

	protected BlockPos getStepSoundPos(BlockPos pos) {
		BlockPos posAbove = pos.up();
		BlockState blockState = this.getEntityWorld().getBlockState(posAbove);
		return !blockState.isIn(BlockTags.INSIDE_STEP_SOUND_BLOCKS)
				       && !blockState.isIn(BlockTags.COMBINATION_STEP_SOUND_BLOCKS) ? pos : posAbove;
	}

	protected void playCombinationStepSounds(BlockState primaryState, BlockState secondaryState) {
		BlockSoundGroup blockSoundGroup = primaryState.getSoundGroup();
		this.playSound(blockSoundGroup.getStepSound(), blockSoundGroup.getVolume() * 0.15F, blockSoundGroup.getPitch());
		this.playSecondaryStepSound(secondaryState);
	}

	protected void playSecondaryStepSound(BlockState state) {
		BlockSoundGroup blockSoundGroup = state.getSoundGroup();
		this.playSound(
				blockSoundGroup.getStepSound(),
				blockSoundGroup.getVolume() * 0.05F,
				blockSoundGroup.getPitch() * 0.8F
		);
	}

	protected void playStepSound(BlockPos pos, BlockState state) {
		BlockSoundGroup blockSoundGroup = state.getSoundGroup();
		this.playSound(blockSoundGroup.getStepSound(), blockSoundGroup.getVolume() * 0.15F, blockSoundGroup.getPitch());
	}

	private boolean shouldPlayAmethystChimeSound(BlockState state) {
		return state.isIn(BlockTags.CRYSTAL_SOUND_BLOCKS) && this.age >= this.lastChimeAge + 20;
	}

	private void playAmethystChimeSound() {
		this.lastChimeIntensity = this.lastChimeIntensity * (float) Math.pow(0.997, this.age - this.lastChimeAge);
		this.lastChimeIntensity = Math.min(1.0F, this.lastChimeIntensity + 0.07F);
		float f = 0.5F + this.lastChimeIntensity * this.random.nextFloat() * 1.2F;
		float g = 0.1F + this.lastChimeIntensity * 1.2F;
		this.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, g, f);
		this.lastChimeAge = this.age;
	}

	protected void playSwimSound(float volume) {
		this.playSound(this.getSwimSound(), volume, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
	}

	protected void addFlapEffects() {
	}

	protected boolean isFlappingWings() {
		return false;
	}

	public void playSound(SoundEvent sound, float volume, float pitch) {
		if (!this.isSilent()) {
			this
					.getEntityWorld()
					.playSound(
							null,
							this.getX(),
							this.getY(),
							this.getZ(),
							sound,
							this.getSoundCategory(),
							volume,
							pitch
					);
		}
	}

	public void playSoundIfNotSilent(SoundEvent event) {
		if (!this.isSilent()) {
			this.playSound(event, 1.0F, 1.0F);
		}
	}

	public boolean isSilent() {
		return this.dataTracker.get(SILENT);
	}

	public void setSilent(boolean silent) {
		this.dataTracker.set(SILENT, silent);
	}

	public boolean hasNoGravity() {
		return this.dataTracker.get(NO_GRAVITY);
	}

	public void setNoGravity(boolean noGravity) {
		this.dataTracker.set(NO_GRAVITY, noGravity);
	}

	protected double getGravity() {
		return 0.0;
	}

	public final double getFinalGravity() {
		return this.hasNoGravity() ? 0.0 : this.getGravity();
	}

	protected void applyGravity() {
		double d = this.getFinalGravity();
		if (d != 0.0) {
			this.setVelocity(this.getVelocity().add(0.0, -d, 0.0));
		}
	}

	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.ALL;
	}

	public boolean occludeVibrationSignals() {
		return false;
	}

	public final void handleFall(double xDifference, double yDifference, double zDifference, boolean onGround) {
		if (!this.isRegionUnloaded()) {
			this.updateSupportingBlockPos(onGround, new Vec3d(xDifference, yDifference, zDifference));
			BlockPos landingPos = this.getLandingPos();
			BlockState blockState = this.getEntityWorld().getBlockState(landingPos);
			this.fall(yDifference, onGround, blockState, landingPos);
		}
	}

	protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
		if (!this.isTouchingWater() && heightDifference < 0.0) {
			this.fallDistance -= (float) heightDifference;
		}

		if (onGround) {
			if (this.fallDistance > 0.0) {
				state.getBlock().onLandedUpon(this.getEntityWorld(), state, landedPosition, this, this.fallDistance);
				this.getEntityWorld()
				    .emitGameEvent(
						    GameEvent.HIT_GROUND,
						    this.pos,
						    GameEvent.Emitter.of(
								    this,
								    this.supportingBlockPos
										    .<BlockState>map(pos -> this.getEntityWorld().getBlockState(pos))
										    .orElse(state)
						    )
				    );
			}

			this.onLanding();
		}
	}

	public boolean isFireImmune() {
		return this.getType().isFireImmune();
	}

	public boolean handleFallDamage(double fallDistance, float damagePerDistance, DamageSource damageSource) {
		if (this.type.isIn(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
			return false;
		}
		else {
			this.handleFallDamageForPassengers(fallDistance, damagePerDistance, damageSource);
			return false;
		}
	}

	protected void handleFallDamageForPassengers(
			double fallDistance,
			float damagePerDistance,
			DamageSource damageSource
	) {
		if (this.hasPassengers()) {
			for (Entity entity : this.getPassengerList()) {
				entity.handleFallDamage(fallDistance, damagePerDistance, damageSource);
			}
		}
	}

	public boolean isTouchingWater() {
		return this.touchingWater;
	}

	boolean isBeingRainedOn() {
		BlockPos currentPos = this.getBlockPos();
		return this.getEntityWorld().hasRain(currentPos)
				|| this
				.getEntityWorld()
				.hasRain(BlockPos.ofFloored(currentPos.getX(), this.getBoundingBox().maxY, currentPos.getZ()));
	}

	public boolean isTouchingWaterOrRain() {
		return this.isTouchingWater() || this.isBeingRainedOn();
	}

	public boolean isInFluid() {
		return this.isTouchingWater() || this.isInLava();
	}

	public boolean isSubmergedInWater() {
		return this.submergedInWater && this.isTouchingWater();
	}

	public boolean isPartlyTouchingWater() {
		return this.isTouchingWater() && !this.isSubmergedInWater();
	}

	public boolean isAtCloudHeight() {
		int cloudAlpha = ColorHelper.getAlpha(
				this.world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.CLOUD_COLOR_VISUAL, this.getEntityPos())
		);
		if (cloudAlpha == 0) {
			return false;
		}

		float cloudHeight = this.world.getEnvironmentAttributes()
				.getAttributeValue(EnvironmentAttributes.CLOUD_HEIGHT_VISUAL, this.getEntityPos());
		if (this.getY() + this.getHeight() < cloudHeight) {
			return false;
		}

		return this.getY() <= cloudHeight + 4.0F;
	}

	public void updateSwimming() {
		if (this.isSwimming()) {
			this.setSwimming(this.isSprinting() && this.isTouchingWater() && !this.hasVehicle());
		}
		else {
			this.setSwimming(
					this.isSprinting() && this.isSubmergedInWater() && !this.hasVehicle() && this
							.getEntityWorld()
							.getFluidState(this.blockPos)
							.isIn(FluidTags.WATER)
			);
		}
	}

	protected boolean updateWaterState() {
		this.fluidHeight.clear();
		this.checkWaterState();
		double
				d =
				this.world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.FAST_LAVA_GAMEPLAY)
				? SPEED_IN_LAVA_IN_NETHER : SPEED_IN_LAVA;
		boolean bl = this.updateMovementInFluid(FluidTags.LAVA, d);
		return this.isTouchingWater() || bl;
	}

	void checkWaterState() {
		if (this.getVehicle() instanceof AbstractBoatEntity abstractBoatEntity
				&& !abstractBoatEntity.isSubmergedInWater()) {
			this.touchingWater = false;
		}
		else if (this.updateMovementInFluid(FluidTags.WATER, SPEED_IN_WATER)) {
			if (!this.touchingWater && !this.firstUpdate) {
				this.onSwimmingStart();
			}

			this.onLanding();
			this.touchingWater = true;
		}
		else {
			this.touchingWater = false;
		}
	}

	private void updateSubmergedInWaterState() {
		this.submergedInWater = this.isSubmergedIn(FluidTags.WATER);
		this.submergedFluidTag.clear();
		double eyeY = this.getEyeY();

		if (this.getVehicle() instanceof AbstractBoatEntity boat
				&& !boat.isSubmergedInWater()
				&& boat.getBoundingBox().maxY >= eyeY
				&& boat.getBoundingBox().minY <= eyeY) {
			return;
		}

		BlockPos eyePos = BlockPos.ofFloored(this.getX(), eyeY, this.getZ());
		FluidState fluidState = this.getEntityWorld().getFluidState(eyePos);
		double fluidTop = eyePos.getY() + fluidState.getHeight(this.getEntityWorld(), eyePos);
		if (fluidTop > eyeY) {
			fluidState.streamTags().forEach(this.submergedFluidTag::add);
		}
	}

	protected void onSwimmingStart() {
		Entity controller = Objects.requireNonNullElse(this.getControllingPassenger(), this);
		float splashFactor = controller == this ? 0.2F : 0.9F;
		Vec3d velocity = controller.getVelocity();
		float splashVolume = Math.min(
				1.0F,
				(float) Math.sqrt(
						velocity.x * velocity.x * MOVEMENT_SPEED_THRESHOLD
								+ velocity.y * velocity.y
								+ velocity.z * velocity.z * 0.2F
				) * splashFactor
		);

		SoundEvent splashSound = splashVolume < 0.25F ? this.getSplashSound() : this.getHighSpeedSplashSound();
		this.playSound(splashSound, splashVolume, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);

		float surfaceY = MathHelper.floor(this.getY());
		int particleCount = (int) (1.0F + this.dimensions.width() * 20.0F);

		for (int index = 0; index < particleCount; index++) {
			double offsetX = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width();
			double offsetZ = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width();
			this.getEntityWorld().addParticleClient(
					ParticleTypes.BUBBLE,
					this.getX() + offsetX,
					surfaceY + 1.0F,
					this.getZ() + offsetZ,
					velocity.x,
					velocity.y - this.random.nextDouble() * MOVEMENT_SPEED_THRESHOLD,
					velocity.z
			);
		}

		for (int index = 0; index < particleCount; index++) {
			double offsetX = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width();
			double offsetZ = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width();
			this.getEntityWorld().addParticleClient(
					ParticleTypes.SPLASH,
					this.getX() + offsetX,
					surfaceY + 1.0F,
					this.getZ() + offsetZ,
					velocity.x,
					velocity.y,
					velocity.z
			);
		}

		this.emitGameEvent(GameEvent.SPLASH);
	}

	@Deprecated
	protected BlockState getLandingBlockState() {
		return this.getEntityWorld().getBlockState(this.getLandingPos());
	}

	public BlockState getSteppingBlockState() {
		return this.getEntityWorld().getBlockState(this.getSteppingPos());
	}

	public boolean shouldSpawnSprintingParticles() {
		return this.isSprinting() && !this.isTouchingWater() && !this.isSpectator() && !this.isInSneakingPose()
				&& !this.isInLava() && this.isAlive();
	}

	protected void spawnSprintingParticles() {
		BlockPos landingPos = this.getLandingPos();
		BlockState blockState = this.getEntityWorld().getBlockState(landingPos);
		if (blockState.getRenderType() == BlockRenderType.INVISIBLE) {
			return;
		}

		Vec3d velocity = this.getVelocity();
		BlockPos currentBlockPos = this.getBlockPos();
		double particleX = this.getX() + (this.random.nextDouble() - 0.5) * this.dimensions.width();
		double particleZ = this.getZ() + (this.random.nextDouble() - 0.5) * this.dimensions.width();

		if (currentBlockPos.getX() != blockPos.getX()) {
			particleX = MathHelper.clamp(particleX, (double) blockPos.getX(), blockPos.getX() + 1.0);
		}

		if (currentBlockPos.getZ() != blockPos.getZ()) {
			particleZ = MathHelper.clamp(particleZ, (double) blockPos.getZ(), blockPos.getZ() + 1.0);
		}

		this.getEntityWorld().addParticleClient(
				new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState),
				particleX,
				this.getY() + 0.1,
				particleZ,
				velocity.x * -4.0,
				1.5,
				velocity.z * -4.0
		);
	}

	public boolean isSubmergedIn(TagKey<Fluid> fluidTag) {
		return this.submergedFluidTag.contains(fluidTag);
	}

	public boolean isInLava() {
		return !this.firstUpdate && this.fluidHeight.getDouble(FluidTags.LAVA) > 0.0;
	}

	public void updateVelocity(float speed, Vec3d movementInput) {
		Vec3d vec3d = movementInputToVelocity(movementInput, speed, this.getYaw());
		this.setVelocity(this.getVelocity().add(vec3d));
	}

	protected static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
		double lengthSq = movementInput.lengthSquared();
		if (lengthSq < 1.0E-7) {
			return Vec3d.ZERO;
		}

		Vec3d scaled = (lengthSq > 1.0 ? movementInput.normalize() : movementInput).multiply(speed);
		float sinYaw = MathHelper.sin(yaw * (float) (Math.PI / 180.0));
		float cosYaw = MathHelper.cos(yaw * (float) (Math.PI / 180.0));
		return new Vec3d(scaled.x * cosYaw - scaled.z * sinYaw, scaled.y, scaled.z * cosYaw + scaled.x * sinYaw);
	}

	@Deprecated
	public float getBrightnessAtEyes() {
		return this.getEntityWorld().isPosLoaded(this.getBlockX(), this.getBlockZ())
		       ? this.getEntityWorld().getBrightness(BlockPos.ofFloored(this.getX(), this.getEyeY(), this.getZ()))
		       : 0.0F;
	}

	public void updatePositionAndAngles(double x, double y, double z, float yaw, float pitch) {
		this.updatePosition(x, y, z);
		this.setAngles(yaw, pitch);
	}

	public void setAngles(float yaw, float pitch) {
		this.setYaw(yaw % 360.0F);
		this.setPitch(MathHelper.clamp(pitch, -90.0F, 90.0F) % 360.0F);
		this.lastYaw = this.getYaw();
		this.lastPitch = this.getPitch();
	}

	public void updatePosition(double x, double y, double z) {
		double d = MathHelper.clamp(x, -3.0E7, 3.0E7);
		double e = MathHelper.clamp(z, -3.0E7, 3.0E7);
		this.lastX = d;
		this.lastY = y;
		this.lastZ = e;
		this.setPosition(d, y, e);
	}

	public void refreshPositionAfterTeleport(Vec3d pos) {
		this.refreshPositionAfterTeleport(pos.x, pos.y, pos.z);
	}

	public void refreshPositionAfterTeleport(double x, double y, double z) {
		this.refreshPositionAndAngles(x, y, z, this.getYaw(), this.getPitch());
	}

	public void refreshPositionAndAngles(BlockPos pos, float yaw, float pitch) {
		this.refreshPositionAndAngles(pos.toBottomCenterPos(), yaw, pitch);
	}

	public void refreshPositionAndAngles(Vec3d pos, float yaw, float pitch) {
		this.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, pitch);
	}

	public void refreshPositionAndAngles(double x, double y, double z, float yaw, float pitch) {
		this.setPos(x, y, z);
		this.setYaw(yaw);
		this.setPitch(pitch);
		this.resetPosition();
		this.refreshPosition();
	}

	public final void resetPosition() {
		this.updateLastPosition();
		this.updateLastAngles();
	}

	public final void setLastPositionAndAngles(Vec3d pos, float yaw, float pitch) {
		this.setLastPosition(pos);
		this.setLastAngles(yaw, pitch);
	}

	protected void updateLastPosition() {
		this.setLastPosition(this.pos);
	}

	public void updateLastAngles() {
		this.setLastAngles(this.getYaw(), this.getPitch());
	}

	private void setLastPosition(Vec3d pos) {
		this.lastX = this.lastRenderX = pos.x;
		this.lastY = this.lastRenderY = pos.y;
		this.lastZ = this.lastRenderZ = pos.z;
	}

	private void setLastAngles(float lastYaw, float lastPitch) {
		this.lastYaw = lastYaw;
		this.lastPitch = lastPitch;
	}

	public final Vec3d getLastRenderPos() {
		return new Vec3d(this.lastRenderX, this.lastRenderY, this.lastRenderZ);
	}

	public float distanceTo(Entity entity) {
		float deltaX = (float) (this.getX() - entity.getX());
		float deltaY = (float) (this.getY() - entity.getY());
		float deltaZ = (float) (this.getZ() - entity.getZ());
		return MathHelper.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
	}

	public double squaredDistanceTo(double x, double y, double z) {
		double deltaX = this.getX() - x;
		double deltaY = this.getY() - y;
		double deltaZ = this.getZ() - z;
		return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
	}

	public double squaredDistanceTo(Entity entity) {
		return this.squaredDistanceTo(entity.getEntityPos());
	}

	public double squaredDistanceTo(Vec3d vector) {
		double deltaX = this.getX() - vector.x;
		double deltaY = this.getY() - vector.y;
		double deltaZ = this.getZ() - vector.z;
		return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
	}

	public void onPlayerCollision(PlayerEntity player) {
	}

	public void pushAwayFrom(Entity entity) {
		if (!this.isConnectedThroughVehicle(entity)) {
			if (!entity.noClip && !this.noClip) {
				double deltaX = entity.getX() - this.getX();
				double deltaZ = entity.getZ() - this.getZ();
				double maxDelta = MathHelper.absMax(deltaX, deltaZ);
				if (maxDelta >= 0.01F) {
					maxDelta = Math.sqrt(maxDelta);
					deltaX /= maxDelta;
					deltaZ /= maxDelta;
					double pushStrength = 1.0 / maxDelta;
					if (pushStrength > 1.0) {
						pushStrength = 1.0;
					}

					deltaX *= pushStrength;
					deltaZ *= pushStrength;
					deltaX *= 0.05F;
					deltaZ *= 0.05F;
					if (!this.hasPassengers() && this.isPushable()) {
						this.addVelocity(-deltaX, 0.0, -deltaZ);
					}

					if (!entity.hasPassengers() && entity.isPushable()) {
						entity.addVelocity(deltaX, 0.0, deltaZ);
					}
				}
			}
		}
	}

	public void addVelocity(Vec3d vec) {
		if (vec.isFinite()) {
			this.addVelocity(vec.x, vec.y, vec.z);
		}
	}

	public void addVelocity(double deltaX, double deltaY, double deltaZ) {
		if (Double.isFinite(deltaX) && Double.isFinite(deltaY) && Double.isFinite(deltaZ)) {
			this.setVelocity(this.getVelocity().add(deltaX, deltaY, deltaZ));
			this.velocityDirty = true;
		}
	}

	protected void scheduleVelocityUpdate() {
		this.knockedBack = true;
	}

	/**
	 * @deprecated Используй {@link #damage(ServerWorld, DamageSource, float)} напрямую.
	 */
	@Deprecated
	public final void serverDamage(DamageSource source, float amount) {
		if (this.world instanceof ServerWorld serverWorld) {
			this.damage(serverWorld, source, amount);
		}
	}

	/**
	 * @deprecated Используй {@link #damage(ServerWorld, DamageSource, float)} напрямую.
	 */
	@Deprecated
	public final boolean sidedDamage(DamageSource source, float amount) {
		return this.world instanceof ServerWorld serverWorld ? this.damage(serverWorld, source, amount)
		                                                     : this.clientDamage(source);
	}

	public abstract boolean damage(ServerWorld world, DamageSource source, float amount);

	public boolean clientDamage(DamageSource source) {
		return false;
	}

	public final Vec3d getRotationVec(float tickProgress) {
		return this.getRotationVector(this.getPitch(tickProgress), this.getYaw(tickProgress));
	}

	public Direction getFacing() {
		return Direction.getFacing(this.getRotationVec(1.0F));
	}

	public float getPitch(float tickProgress) {
		return this.getLerpedPitch(tickProgress);
	}

	public float getYaw(float tickProgress) {
		return this.getLerpedYaw(tickProgress);
	}

	public float getLerpedPitch(float tickProgress) {
		return tickProgress == 1.0F ? this.getPitch() : MathHelper.lerp(tickProgress, this.lastPitch, this.getPitch());
	}

	public float getLerpedYaw(float tickProgress) {
		return tickProgress == 1.0F ? this.getYaw()
		                            : MathHelper.lerpAngleDegrees(tickProgress, this.lastYaw, this.getYaw());
	}

	public final Vec3d getRotationVector(float pitch, float yaw) {
		float pitchRad = pitch * (float) (Math.PI / 180.0);
		float yawRad = -yaw * (float) (Math.PI / 180.0);
		float cosYaw = MathHelper.cos(yawRad);
		float sinYaw = MathHelper.sin(yawRad);
		float cosPitch = MathHelper.cos(pitchRad);
		float sinPitch = MathHelper.sin(pitchRad);
		return new Vec3d(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
	}

	public final Vec3d getOppositeRotationVector(float tickProgress) {
		return this.getOppositeRotationVector(this.getPitch(tickProgress), this.getYaw(tickProgress));
	}

	protected final Vec3d getOppositeRotationVector(float pitch, float yaw) {
		return this.getRotationVector(pitch - 90.0F, yaw);
	}

	public final Vec3d getEyePos() {
		return new Vec3d(this.getX(), this.getEyeY(), this.getZ());
	}

	public final Vec3d getCameraPosVec(float tickProgress) {
		double lerpX = MathHelper.lerp((double) tickProgress, this.lastX, this.getX());
		double lerpY = MathHelper.lerp((double) tickProgress, this.lastY, this.getY()) + this.getStandingEyeHeight();
		double lerpZ = MathHelper.lerp((double) tickProgress, this.lastZ, this.getZ());
		return new Vec3d(lerpX, lerpY, lerpZ);
	}

	public Vec3d getClientCameraPosVec(float tickProgress) {
		return this.getCameraPosVec(tickProgress);
	}

	public final Vec3d getLerpedPos(float deltaTicks) {
		double lerpX = MathHelper.lerp((double) deltaTicks, this.lastX, this.getX());
		double lerpY = MathHelper.lerp((double) deltaTicks, this.lastY, this.getY());
		double lerpZ = MathHelper.lerp((double) deltaTicks, this.lastZ, this.getZ());
		return new Vec3d(lerpX, lerpY, lerpZ);
	}

	public HitResult raycast(double maxDistance, float tickProgress, boolean includeFluids) {
		Vec3d vec3d = this.getCameraPosVec(tickProgress);
		Vec3d vec3d2 = this.getRotationVec(tickProgress);
		Vec3d vec3d3 = vec3d.add(vec3d2.x * maxDistance, vec3d2.y * maxDistance, vec3d2.z * maxDistance);
		return this.getEntityWorld()
		           .raycast(
				           new RaycastContext(
						           vec3d,
						           vec3d3,
						           RaycastContext.ShapeType.OUTLINE,
						           includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE,
						           this
				           )
		           );
	}

	public boolean canBeHitByProjectile() {
		return this.isAlive() && this.canHit();
	}

	public boolean canHit() {
		return false;
	}

	public boolean isPushable() {
		return false;
	}

	public void updateKilledAdvancementCriterion(Entity entityKilled, DamageSource damageSource) {
		if (entityKilled instanceof ServerPlayerEntity) {
			Criteria.ENTITY_KILLED_PLAYER.trigger((ServerPlayerEntity) entityKilled, this, damageSource);
		}
	}

	public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
		double deltaX = this.getX() - cameraX;
		double deltaY = this.getY() - cameraY;
		double deltaZ = this.getZ() - cameraZ;
		double distanceSq = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
		return this.shouldRender(distanceSq);
	}

	public boolean shouldRender(double distance) {
		double sideLength = this.getBoundingBox().getAverageSideLength();
		if (Double.isNaN(sideLength)) {
			sideLength = 1.0;
		}

		sideLength *= 64.0 * renderDistanceMultiplier;
		return distance < sideLength * sideLength;
	}

	public boolean saveSelfData(WriteView view) {
		if (this.removalReason != null && !this.removalReason.shouldSave()) {
			return false;
		}
		else {
			String string = this.getSavedEntityId();
			if (string == null) {
				return false;
			}
			else {
				view.putString("id", string);
				this.writeData(view);
				return true;
			}
		}
	}

	public boolean saveData(WriteView view) {
		return this.hasVehicle() ? false : this.saveSelfData(view);
	}

	public void writeData(WriteView view) {
		try {
			if (this.vehicle != null) {
				view.put("Pos", Vec3d.CODEC, new Vec3d(this.vehicle.getX(), this.getY(), this.vehicle.getZ()));
			}
			else {
				view.put("Pos", Vec3d.CODEC, this.getEntityPos());
			}

			view.put("Motion", Vec3d.CODEC, this.getVelocity());
			view.put("Rotation", Vec2f.CODEC, new Vec2f(this.getYaw(), this.getPitch()));
			view.putDouble("fall_distance", this.fallDistance);
			view.putShort("Fire", (short) this.fireTicks);
			view.putShort("Air", (short) this.getAir());
			view.putBoolean("OnGround", this.isOnGround());
			view.putBoolean("Invulnerable", this.invulnerable);
			view.putInt("PortalCooldown", this.portalCooldown);
			view.put("UUID", Uuids.INT_STREAM_CODEC, this.getUuid());
			view.putNullable("CustomName", TextCodecs.CODEC, this.getCustomName());
			if (this.isCustomNameVisible()) {
				view.putBoolean("CustomNameVisible", this.isCustomNameVisible());
			}

			if (this.isSilent()) {
				view.putBoolean("Silent", this.isSilent());
			}

			if (this.hasNoGravity()) {
				view.putBoolean("NoGravity", this.hasNoGravity());
			}

			if (this.glowing) {
				view.putBoolean("Glowing", true);
			}

			int frozenTicks = this.getFrozenTicks();
			if (frozenTicks > 0) {
				view.putInt("TicksFrozen", frozenTicks);
			}

			if (this.hasVisualFire) {
				view.putBoolean("HasVisualFire", this.hasVisualFire);
			}

			if (!this.commandTags.isEmpty()) {
				view.put("Tags", TAG_LIST_CODEC, List.copyOf(this.commandTags));
			}

			if (!this.customData.isEmpty()) {
				view.put("data", NbtComponent.CODEC, this.customData);
			}

			this.writeCustomData(view);
			if (this.hasPassengers()) {
				WriteView.ListView listView = view.getList("Passengers");

				for (Entity entity : this.getPassengerList()) {
					WriteView writeView = listView.add();
					if (!entity.saveSelfData(writeView)) {
						listView.removeLast();
					}
				}

				if (listView.isEmpty()) {
					view.remove("Passengers");
				}
			}
		}
		catch (Throwable error) {
			CrashReport crashReport = CrashReport.create(error, "Saving entity NBT");
			CrashReportSection crashReportSection = crashReport.addElement("Entity being saved");
			this.populateCrashReport(crashReportSection);
			throw new CrashException(crashReport);
		}
	}

	public void readData(ReadView view) {
		try {
			Vec3d pos = view.<Vec3d>read("Pos", Vec3d.CODEC).orElse(Vec3d.ZERO);
			Vec3d motion = view.<Vec3d>read("Motion", Vec3d.CODEC).orElse(Vec3d.ZERO);
			Vec2f rotation = view.<Vec2f>read("Rotation", Vec2f.CODEC).orElse(Vec2f.ZERO);
			this.setVelocity(
					Math.abs(motion.x) > 10.0 ? 0.0 : motion.x,
					Math.abs(motion.y) > 10.0 ? 0.0 : motion.y,
					Math.abs(motion.z) > 10.0 ? 0.0 : motion.z
			);
			this.velocityDirty = true;
			this.setPos(
					MathHelper.clamp(pos.x, -WORLD_BORDER_COORD, WORLD_BORDER_COORD),
					MathHelper.clamp(pos.y, -2.0E7, 2.0E7),
					MathHelper.clamp(pos.z, -WORLD_BORDER_COORD, WORLD_BORDER_COORD)
			);
			this.setYaw(rotation.x);
			this.setPitch(rotation.y);
			this.resetPosition();
			this.setHeadYaw(this.getYaw());
			this.setBodyYaw(this.getYaw());
			this.fallDistance = view.getDouble("fall_distance", 0.0);
			this.fireTicks = view.getShort("Fire", (short) 0);
			this.setAir(view.getInt("Air", this.getMaxAir()));
			this.onGround = view.getBoolean("OnGround", false);
			this.invulnerable = view.getBoolean("Invulnerable", false);
			this.portalCooldown = view.getInt("PortalCooldown", 0);
			view.<UUID>read("UUID", Uuids.INT_STREAM_CODEC).ifPresent(uuid -> {
				this.uuid = uuid;
				this.uuidString = this.uuid.toString();
			});
			if (!Double.isFinite(this.getX()) || !Double.isFinite(this.getY()) || !Double.isFinite(this.getZ())) {
				throw new IllegalStateException("Entity has invalid position");
			}
			else if (Double.isFinite(this.getYaw()) && Double.isFinite(this.getPitch())) {
				this.refreshPosition();
				this.setRotation(this.getYaw(), this.getPitch());
				this.setCustomName(view.<Text>read("CustomName", TextCodecs.CODEC).orElse(null));
				this.setCustomNameVisible(view.getBoolean("CustomNameVisible", false));
				this.setSilent(view.getBoolean("Silent", false));
				this.setNoGravity(view.getBoolean("NoGravity", false));
				this.setGlowing(view.getBoolean("Glowing", false));
				this.setFrozenTicks(view.getInt("TicksFrozen", 0));
				this.hasVisualFire = view.getBoolean("HasVisualFire", false);
				this.customData = view.<NbtComponent>read("data", NbtComponent.CODEC).orElse(NbtComponent.DEFAULT);
				this.commandTags.clear();
				view.<List<String>>read("Tags", TAG_LIST_CODEC).ifPresent(this.commandTags::addAll);
				this.readCustomData(view);
				if (this.shouldSetPositionOnLoad()) {
					this.refreshPosition();
				}
			}
			else {
				throw new IllegalStateException("Entity has invalid rotation");
			}
		}
		catch (Throwable error) {
			CrashReport crashReport = CrashReport.create(error, "Loading entity NBT");
			CrashReportSection crashReportSection = crashReport.addElement("Entity being loaded");
			this.populateCrashReport(crashReportSection);
			throw new CrashException(crashReport);
		}
	}

	protected boolean shouldSetPositionOnLoad() {
		return true;
	}

	protected final @Nullable String getSavedEntityId() {
		EntityType<?> entityType = this.getType();
		Identifier identifier = EntityType.getId(entityType);
		return !entityType.isSaveable() ? null : identifier.toString();
	}

	protected abstract void readCustomData(ReadView view);

	protected abstract void writeCustomData(WriteView view);

	public @Nullable ItemEntity dropItem(ServerWorld world, ItemConvertible item) {
		return this.dropStack(world, new ItemStack(item), 0.0F);
	}

	public @Nullable ItemEntity dropStack(ServerWorld world, ItemStack stack) {
		return this.dropStack(world, stack, 0.0F);
	}

	public @Nullable ItemEntity dropStack(ServerWorld world, ItemStack stack, Vec3d offset) {
		if (stack.isEmpty()) {
			return null;
		}
		else {
			ItemEntity
					itemEntity =
					new ItemEntity(
							world,
							this.getX() + offset.x,
							this.getY() + offset.y,
							this.getZ() + offset.z,
							stack
					);
			itemEntity.setToDefaultPickupDelay();
			world.spawnEntity(itemEntity);
			return itemEntity;
		}
	}

	public @Nullable ItemEntity dropStack(ServerWorld world, ItemStack stack, float yOffset) {
		return this.dropStack(world, stack, new Vec3d(0.0, yOffset, 0.0));
	}

	public boolean isAlive() {
		return !this.isRemoved();
	}

	public boolean isInsideWall() {
		if (this.noClip) {
			return false;
		}
		else {
			float f = this.dimensions.width() * 0.8F;
			Box box = Box.of(this.getEyePos(), f, 1.0E-6, f);
			return BlockPos.stream(box)
			               .anyMatch(
					               pos -> {
						               BlockState blockState = this.getEntityWorld().getBlockState(pos);
						               return !blockState.isAir()
								               && blockState.shouldSuffocate(this.getEntityWorld(), pos)
								               && VoxelShapes.matchesAnywhere(
								               blockState.getCollisionShape(this.getEntityWorld(), pos).offset(pos),
								               VoxelShapes.cuboid(box),
								               BooleanBiFunction.AND
						               );
					               }
			               );
		}
	}

	public ActionResult interact(PlayerEntity player, Hand hand) {
		if (!this.getEntityWorld().isClient()
				&& player.shouldCancelInteraction()
				&& this instanceof Leashable leashable
				&& leashable.canBeLeashed()
				&& this.isAlive()
				&& !(this instanceof LivingEntity livingEntity && livingEntity.isBaby())) {
			List<Leashable>
					list =
					Leashable.collectLeashablesAround(this, leashablex -> leashablex.getLeashHolder() == player);
			if (!list.isEmpty()) {
				boolean anyLeashed = false;
	
				for (Leashable leashable2 : list) {
					if (leashable2.canBeLeashedTo(this)) {
						leashable2.attachLeash(this, true);
						anyLeashed = true;
					}
				}
	
				if (anyLeashed) {
					this
							.getEntityWorld()
							.emitGameEvent(GameEvent.ENTITY_ACTION, this.getBlockPos(), GameEvent.Emitter.of(player));
					this.playSoundIfNotSilent(SoundEvents.ITEM_LEAD_TIED);
					return ActionResult.SUCCESS_SERVER.noIncrementStat();
				}
			}
		}

		ItemStack itemStack = player.getStackInHand(hand);
		if (itemStack.isOf(Items.SHEARS) && this.snipAllHeldLeashes(player)) {
			itemStack.damage(1, player, hand);
			return ActionResult.SUCCESS;
		}
		else if (this instanceof MobEntity mobEntity
				&& itemStack.isOf(Items.SHEARS)
				&& mobEntity.canRemoveSaddle(player)
				&& !player.shouldCancelInteraction()
				&& this.shearEquipment(player, hand, itemStack, mobEntity)) {
			return ActionResult.SUCCESS;
		}
		else {
			if (this.isAlive() && this instanceof Leashable leashable3) {
				if (leashable3.getLeashHolder() == player) {
					if (!this.getEntityWorld().isClient()) {
						if (player.isInCreativeMode()) {
							leashable3.detachLeashWithoutDrop();
						}
						else {
							leashable3.detachLeash();
						}

						this.emitGameEvent(GameEvent.ENTITY_INTERACT, player);
						this.playSoundIfNotSilent(SoundEvents.ITEM_LEAD_UNTIED);
					}

					return ActionResult.SUCCESS.noIncrementStat();
				}

				ItemStack itemStack2 = player.getStackInHand(hand);
				if (itemStack2.isOf(Items.LEAD) && !(leashable3.getLeashHolder() instanceof PlayerEntity)) {
					if (this.getEntityWorld().isClient()) {
						return ActionResult.CONSUME;
					}

					if (leashable3.canBeLeashedTo(player)) {
						if (leashable3.isLeashed()) {
							leashable3.detachLeash();
						}

						leashable3.attachLeash(player, true);
						this.playSoundIfNotSilent(SoundEvents.ITEM_LEAD_TIED);
						itemStack2.decrement(1);
						return ActionResult.SUCCESS_SERVER;
					}
				}
			}

			return ActionResult.PASS;
		}
	}

	/** Перерезает все поводки, удерживаемые этой сущностью, и воспроизводит звук ножниц. */
	public boolean snipAllHeldLeashes(@Nullable PlayerEntity player) {
		boolean detached = this.detachAllHeldLeashes(player);
		if (detached && this.getEntityWorld() instanceof ServerWorld serverWorld) {
			serverWorld.playSound(
					null,
					this.getBlockPos(),
					SoundEvents.ITEM_SHEARS_SNIP,
					player != null ? player.getSoundCategory() : this.getSoundCategory()
			);
		}

		return detached;
	}

	/** Отсоединяет все поводки, удерживаемые этой сущностью, и испускает игровое событие SHEAR. */
	public boolean detachAllHeldLeashes(@Nullable PlayerEntity player) {
		List<Leashable> held = Leashable.collectLeashablesHeldBy(this);
		boolean anyDetached = !held.isEmpty();
		if (this instanceof Leashable leashable && leashable.isLeashed()) {
			leashable.detachLeash();
			anyDetached = true;
		}

		for (Leashable leashable2 : held) {
			leashable2.detachLeash();
		}

		if (anyDetached) {
			this.emitGameEvent(GameEvent.SHEAR, player);
			return true;
		}

		return false;
	}

	private boolean shearEquipment(PlayerEntity player, Hand hand, ItemStack shears, MobEntity entity) {
		for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
			ItemStack itemStack = entity.getEquippedStack(equipmentSlot);
			EquippableComponent equippableComponent = itemStack.get(DataComponentTypes.EQUIPPABLE);
			if (equippableComponent != null
					&& equippableComponent.canBeSheared()
					&& (!EnchantmentHelper.hasAnyEnchantmentsWith(
					itemStack,
					EnchantmentEffectComponentTypes.PREVENT_ARMOR_CHANGE
			) || player.isCreative()
			)) {
				shears.damage(1, player, hand.getEquipmentSlot());
				Vec3d dropOffset = this.dimensions.attachments().getPointOrDefault(EntityAttachmentType.PASSENGER);
				entity.equipLootStack(equipmentSlot, ItemStack.EMPTY);
				this.emitGameEvent(GameEvent.SHEAR, player);
				this.playSoundIfNotSilent(equippableComponent.shearingSound().value());
				if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
					this.dropStack(serverWorld, itemStack, dropOffset);
					Criteria.PLAYER_SHEARED_EQUIPMENT.trigger((ServerPlayerEntity) player, itemStack, entity);
				}

				return true;
			}
		}

		return false;
	}

	public boolean collidesWith(Entity other) {
		return other.isCollidable(this) && !this.isConnectedThroughVehicle(other);
	}

	public boolean isCollidable(@Nullable Entity entity) {
		return false;
	}

	public void tickRiding() {
		this.setVelocity(Vec3d.ZERO);
		this.tick();
		if (this.hasVehicle()) {
			this.getVehicle().updatePassengerPosition(this);
		}
	}

	public final void updatePassengerPosition(Entity passenger) {
		if (this.hasPassenger(passenger)) {
			this.updatePassengerPosition(passenger, Entity::setPosition);
		}
	}

	protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
		Vec3d ridingPos = this.getPassengerRidingPos(passenger);
		Vec3d attachmentPos = passenger.getVehicleAttachmentPos(this);
		positionUpdater.accept(passenger, ridingPos.x - attachmentPos.x, ridingPos.y - attachmentPos.y, ridingPos.z - attachmentPos.z);
	}

	public void onPassengerLookAround(Entity passenger) {
	}

	public Vec3d getVehicleAttachmentPos(Entity vehicle) {
		return this.getAttachments().getPoint(EntityAttachmentType.VEHICLE, 0, this.yaw);
	}

	public Vec3d getPassengerRidingPos(Entity passenger) {
		return this.getEntityPos().add(this.getPassengerAttachmentPos(passenger, this.dimensions, 1.0F));
	}

	protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
		return getPassengerAttachmentPos(this, passenger, dimensions.attachments());
	}

	protected static Vec3d getPassengerAttachmentPos(Entity vehicle, Entity passenger, EntityAttachments attachments) {
		int passengerIndex = vehicle.getPassengerList().indexOf(passenger);
		return attachments.getPointOrDefault(EntityAttachmentType.PASSENGER, passengerIndex, vehicle.yaw);
	}

	public final boolean startRiding(Entity entity) {
		return this.startRiding(entity, false, true);
	}

	public boolean isLiving() {
		return this instanceof LivingEntity;
	}

	public boolean startRiding(Entity entity, boolean force, boolean emitEvent) {
		if (entity == this.vehicle) {
			return false;
		}
		else if (!entity.couldAcceptPassenger()) {
			return false;
		}
		else if (!this.getEntityWorld().isClient() && !entity.type.isSaveable()) {
			return false;
		}
		else {
			for (Entity entity2 = entity; entity2.vehicle != null; entity2 = entity2.vehicle) {
				if (entity2.vehicle == this) {
					return false;
				}
			}

			if (force || this.canStartRiding(entity) && entity.canAddPassenger(this)) {
				if (this.hasVehicle()) {
					this.stopRiding();
				}

				this.setPose(EntityPose.STANDING);
				this.vehicle = entity;
				this.vehicle.addPassenger(this);
				if (emitEvent) {
					this.getEntityWorld().emitGameEvent(this, GameEvent.ENTITY_MOUNT, this.vehicle.pos);
					entity.streamIntoPassengers()
					      .filter(passenger -> passenger instanceof ServerPlayerEntity)
					      .forEach(player -> Criteria.STARTED_RIDING.trigger((ServerPlayerEntity) player));
				}

				return true;
			}
			else {
				return false;
			}
		}
	}

	protected boolean canStartRiding(Entity entity) {
		return !this.isSneaking() && this.ridingCooldown <= 0;
	}

	public void removeAllPassengers() {
		for (int index = this.passengerList.size() - 1; index >= 0; index--) {
			this.passengerList.get(index).stopRiding();
		}
	}

	public void dismountVehicle() {
		if (this.vehicle != null) {
			Entity entity = this.vehicle;
			this.vehicle = null;
			entity.removePassenger(this);
			Entity.RemovalReason currentRemovalReason = this.getRemovalReason();
			if (currentRemovalReason == null || currentRemovalReason.shouldDestroy()) {
				this.getEntityWorld().emitGameEvent(this, GameEvent.ENTITY_DISMOUNT, entity.pos);
			}
		}
	}

	public void stopRiding() {
		this.dismountVehicle();
	}

	protected void addPassenger(Entity passenger) {
		if (passenger.getVehicle() != this) {
			throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
		}
		else {
			if (this.passengerList.isEmpty()) {
				this.passengerList = ImmutableList.of(passenger);
			}
			else {
				List<Entity> list = Lists.newArrayList(this.passengerList);
				if (!this.getEntityWorld().isClient() && passenger instanceof PlayerEntity
						&& !(this.getFirstPassenger() instanceof PlayerEntity)) {
					list.add(0, passenger);
				}
				else {
					list.add(passenger);
				}

				this.passengerList = ImmutableList.copyOf(list);
			}
		}
	}

	protected void removePassenger(Entity passenger) {
		if (passenger.getVehicle() == this) {
			throw new IllegalStateException("Use x.stopRiding(y), not y.removePassenger(x)");
		}
		else {
			if (this.passengerList.size() == 1 && this.passengerList.get(0) == passenger) {
				this.passengerList = ImmutableList.of();
			}
			else {
				this.passengerList =
						this.passengerList
								.stream()
								.filter(entity -> entity != passenger)
								.collect(ImmutableList.toImmutableList());
			}

			passenger.ridingCooldown = MAX_RIDING_COOLDOWN;
		}
	}

	protected boolean canAddPassenger(Entity passenger) {
		return this.passengerList.isEmpty();
	}

	protected boolean couldAcceptPassenger() {
		return true;
	}

	public final boolean isInterpolating() {
		return this.getInterpolator() != null && this.getInterpolator().isInterpolating();
	}

	public final void updateTrackedPositionAndAngles(Vec3d pos, float yaw, float pitch) {
		this.updateTrackedPositionAndAngles(Optional.of(pos), Optional.of(yaw), Optional.of(pitch));
	}

	public final void updateTrackedAngles(float yaw, float pitch) {
		this.updateTrackedPositionAndAngles(Optional.empty(), Optional.of(yaw), Optional.of(pitch));
	}

	public final void updateTrackedPosition(Vec3d pos) {
		this.updateTrackedPositionAndAngles(Optional.of(pos), Optional.empty(), Optional.empty());
	}

	public final void updateTrackedPositionAndAngles(
			Optional<Vec3d> optional,
			Optional<Float> optional2,
			Optional<Float> optional3
	) {
		PositionInterpolator positionInterpolator = this.getInterpolator();
		if (positionInterpolator != null) {
			positionInterpolator.refreshPositionAndAngles(
					optional.orElse(positionInterpolator.getLerpedPos()),
					optional2.orElse(positionInterpolator.getLerpedYaw()),
					optional3.orElse(positionInterpolator.getLerpedPitch())
			);
		}
		else {
			optional.ifPresent(this::setPosition);
			optional2.ifPresent(float_ -> this.setYaw(float_ % 360.0F));
			optional3.ifPresent(float_ -> this.setPitch(float_ % 360.0F));
		}
	}

	public @Nullable PositionInterpolator getInterpolator() {
		return null;
	}

	public void updateTrackedHeadRotation(float yaw, int interpolationSteps) {
		this.setHeadYaw(yaw);
	}

	public float getTargetingMargin() {
		return 0.0F;
	}

	public Vec3d getRotationVector() {
		return this.getRotationVector(this.getPitch(), this.getYaw());
	}

	public Vec3d getHeadRotationVector() {
		return this.getRotationVector(this.getPitch(), this.getHeadYaw());
	}

	public Vec3d getHandPosOffset(Item item) {
		if (!(this instanceof PlayerEntity playerEntity)) {
			return Vec3d.ZERO;
		}
		else {
			boolean isOffHand = playerEntity.getOffHandStack().isOf(item) && !playerEntity.getMainHandStack().isOf(item);
			Arm arm = isOffHand ? playerEntity.getMainArm().getOpposite() : playerEntity.getMainArm();
			return this.getRotationVector(0.0F, this.getYaw() + (arm == Arm.RIGHT ? 80 : -80)).multiply(0.5);
		}
	}

	public Vec2f getRotationClient() {
		return new Vec2f(this.getPitch(), this.getYaw());
	}

	public Vec3d getRotationVecClient() {
		return Vec3d.fromPolar(this.getRotationClient());
	}

	public void tryUsePortal(Portal portal, BlockPos pos) {
		if (this.hasPortalCooldown()) {
			this.resetPortalCooldown();
		}
		else {
			if (this.portalManager == null || !this.portalManager.portalMatches(portal)) {
				this.portalManager = new PortalManager(portal, pos.toImmutable());
			}
			else if (!this.portalManager.isInPortal()) {
				this.portalManager.setPortalPos(pos.toImmutable());
				this.portalManager.setInPortal(true);
			}
		}
	}

	protected void tickPortalTeleportation() {
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			this.tickPortalCooldown();
			if (this.portalManager != null) {
				if (this.portalManager.tick(serverWorld, this, this.canUsePortals(false))) {
					Profiler profiler = Profilers.get();
					profiler.push("portal");
					this.resetPortalCooldown();
					TeleportTarget teleportTarget = this.portalManager.createTeleportTarget(serverWorld, this);
					if (teleportTarget != null) {
						ServerWorld serverWorld2 = teleportTarget.world();
						if (serverWorld.isEnterableWithPortal(serverWorld2)
								&& (serverWorld2.getRegistryKey() == serverWorld.getRegistryKey()
								|| this.canTeleportBetween(serverWorld, serverWorld2)
						)) {
							this.teleportTo(teleportTarget);
						}
					}

					profiler.pop();
				}
				else if (this.portalManager.hasExpired()) {
					this.portalManager = null;
				}
			}
		}
	}

	public int getDefaultPortalCooldown() {
		Entity entity = this.getFirstPassenger();
		return entity instanceof ServerPlayerEntity ? entity.getDefaultPortalCooldown() : DEFAULT_PORTAL_COOLDOWN;
	}

	public void setVelocityClient(Vec3d clientVelocity) {
		this.setVelocity(clientVelocity);
	}

	public void onDamaged(DamageSource damageSource) {
	}

	public void handleStatus(byte status) {
		switch (status) {
			case STATUS_HONEY_BLOCK_SLIDE:
				HoneyBlock.addRegularParticles(this);
		}
	}

	public void animateDamage(float yaw) {
	}

	public boolean isOnFire() {
		boolean isClient = this.getEntityWorld() != null && this.getEntityWorld().isClient();
		return !this.isFireImmune() && (this.fireTicks > 0 || isClient && this.getFlag(ON_FIRE_FLAG_INDEX));
	}

	public boolean hasVehicle() {
		return this.getVehicle() != null;
	}

	public boolean hasPassengers() {
		return !this.passengerList.isEmpty();
	}

	public boolean shouldDismountUnderwater() {
		return this.getType().isIn(EntityTypeTags.DISMOUNTS_UNDERWATER);
	}

	public boolean shouldControlVehicles() {
		return !this.getType().isIn(EntityTypeTags.NON_CONTROLLING_RIDER);
	}

	public void setSneaking(boolean sneaking) {
		this.setFlag(SNEAKING_FLAG_INDEX, sneaking);
	}

	public boolean isSneaking() {
		return this.getFlag(SNEAKING_FLAG_INDEX);
	}

	public boolean bypassesSteppingEffects() {
		return this.isSneaking();
	}

	public boolean bypassesLandingEffects() {
		return this.isSneaking();
	}

	public boolean isSneaky() {
		return this.isSneaking();
	}

	public boolean isDescending() {
		return this.isSneaking();
	}

	public boolean isInSneakingPose() {
		return this.isInPose(EntityPose.CROUCHING);
	}

	public boolean isSprinting() {
		return this.getFlag(SPRINTING_FLAG_INDEX);
	}

	public void setSprinting(boolean sprinting) {
		this.setFlag(SPRINTING_FLAG_INDEX, sprinting);
	}

	public boolean isSwimming() {
		return this.getFlag(SWIMMING_FLAG_INDEX);
	}

	public boolean isInSwimmingPose() {
		return this.isInPose(EntityPose.SWIMMING);
	}

	public boolean isCrawling() {
		return this.isInSwimmingPose() && !this.isTouchingWater();
	}

	public void setSwimming(boolean swimming) {
		this.setFlag(SWIMMING_FLAG_INDEX, swimming);
	}

	public final boolean isGlowingLocal() {
		return this.glowing;
	}

	public final void setGlowing(boolean glowing) {
		this.glowing = glowing;
		this.setFlag(GLOWING_FLAG_INDEX, this.isGlowing());
	}

	public boolean isGlowing() {
		return this.getEntityWorld().isClient() ? this.getFlag(GLOWING_FLAG_INDEX) : this.glowing;
	}

	public boolean isInvisible() {
		return this.getFlag(INVISIBLE_FLAG_INDEX);
	}

	public boolean isInvisibleTo(PlayerEntity player) {
		if (player.isSpectator()) {
			return false;
		}

		AbstractTeam team = this.getScoreboardTeam();
		boolean visibleToTeam = team != null
				&& player.getScoreboardTeam() == team
				&& team.shouldShowFriendlyInvisibles();
		return !visibleToTeam && this.isInvisible();
	}

	public boolean isOnRail() {
		return false;
	}

	public void updateEventHandler(BiConsumer<EntityGameEventHandler<?>, ServerWorld> callback) {
	}

	public @Nullable Team getScoreboardTeam() {
		return this.getEntityWorld().getScoreboard().getScoreHolderTeam(this.getNameForScoreboard());
	}

	public final boolean isTeammate(@Nullable Entity other) {
		return other != null && (this == other || this.isInSameTeam(other) || other.isInSameTeam(this));
	}

	public boolean isInSameTeam(Entity other) {
		return this.isTeamPlayer(other.getScoreboardTeam());
	}

	public boolean isTeamPlayer(@Nullable AbstractTeam team) {
		return this.getScoreboardTeam() != null && this.getScoreboardTeam().isEqual(team);
	}

	public void setInvisible(boolean invisible) {
		this.setFlag(INVISIBLE_FLAG_INDEX, invisible);
	}

	protected boolean getFlag(int index) {
		return (this.dataTracker.get(FLAGS) & 1 << index) != 0;
	}

	public void setFlag(int index, boolean value) {
		byte flags = this.dataTracker.get(FLAGS);
		byte updated = value
				? (byte) (flags | 1 << index)
				: (byte) (flags & ~(1 << index));
		this.dataTracker.set(FLAGS, updated);
	}

	public int getMaxAir() {
		return DEFAULT_MAX_AIR;
	}

	public int getAir() {
		return this.dataTracker.get(AIR);
	}

	public void setAir(int air) {
		this.dataTracker.set(AIR, air);
	}

	public void defrost() {
		this.setFrozenTicks(0);
	}

	public int getFrozenTicks() {
		return this.dataTracker.get(FROZEN_TICKS);
	}

	public void setFrozenTicks(int frozenTicks) {
		this.dataTracker.set(FROZEN_TICKS, frozenTicks);
	}

	public float getFreezingScale() {
		int minFreezeTicks = this.getMinFreezeDamageTicks();
		return (float) Math.min(this.getFrozenTicks(), minFreezeTicks) / minFreezeTicks;
	}

	public boolean isFrozen() {
		return this.getFrozenTicks() >= this.getMinFreezeDamageTicks();
	}

	public int getMinFreezeDamageTicks() {
		return DEFAULT_MIN_FREEZE_DAMAGE_TICKS;
	}

	public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
		this.setFireTicks(this.fireTicks + 1);
		if (this.fireTicks == 0) {
			this.setOnFireFor(8.0F);
		}

		this.damage(world, this.getDamageSources().lightningBolt(), 5.0F);
	}

	public void onBubbleColumnSurfaceCollision(boolean drag, BlockPos pos) {
		applyBubbleColumnSurfaceEffects(this, drag, pos);
	}

	protected static void applyBubbleColumnSurfaceEffects(Entity entity, boolean drag, BlockPos pos) {
		Vec3d velocity = entity.getVelocity();
		double newY = drag
				? Math.max(-0.9, velocity.y - 0.03)
				: Math.min(MIN_RISING_BUBBLE_COLUMN_SPEED, velocity.y + 0.1);

		entity.setVelocity(velocity.x, newY, velocity.z);
		spawnBubbleColumnParticles(entity.world, pos);
	}

	protected static void spawnBubbleColumnParticles(World world, BlockPos pos) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}

		for (int pass = 0; pass < 2; pass++) {
			serverWorld.spawnParticles(
					ParticleTypes.SPLASH,
					pos.getX() + world.random.nextDouble(),
					pos.getY() + 1,
					pos.getZ() + world.random.nextDouble(),
					1,
					0.0,
					0.0,
					0.0,
					1.0
			);
			serverWorld.spawnParticles(
					ParticleTypes.BUBBLE,
					pos.getX() + world.random.nextDouble(),
					pos.getY() + 1,
					pos.getZ() + world.random.nextDouble(),
					1,
					0.0,
					0.01,
					0.0,
					MOVEMENT_SPEED_THRESHOLD
			);
		}
	}

	public void onBubbleColumnCollision(boolean drag) {
		applyBubbleColumnEffects(this, drag);
	}

	protected static void applyBubbleColumnEffects(Entity entity, boolean drag) {
		Vec3d velocity = entity.getVelocity();
		double newY = drag
				? Math.max(-0.3, velocity.y - 0.03)
				: Math.min(0.7, velocity.y + 0.06);

		entity.setVelocity(velocity.x, newY, velocity.z);
		entity.onLanding();
	}

	public boolean onKilledOther(ServerWorld world, LivingEntity other, DamageSource damageSource) {
		return true;
	}

	public void limitFallDistance() {
		if (this.getVelocity().getY() > -0.5 && this.fallDistance > 1.0) {
			this.fallDistance = 1.0;
		}
	}

	public void onLanding() {
		this.fallDistance = 0.0;
	}

	protected void pushOutOfBlocks(double x, double y, double z) {
		BlockPos flooredPos = BlockPos.ofFloored(x, y, z);
		Vec3d localOffset = new Vec3d(x - flooredPos.getX(), y - flooredPos.getY(), z - flooredPos.getZ());
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		Direction pushDirection = Direction.UP;
		double minDistance = Double.MAX_VALUE;

		for (Direction candidate : new Direction[]{
				Direction.NORTH,
				Direction.SOUTH,
				Direction.WEST,
				Direction.EAST,
				Direction.UP
		}) {
			mutable.set(blockPos, candidate);
			if (!this.getEntityWorld().getBlockState(mutable).isFullCube(this.getEntityWorld(), mutable)) {
				double component = localOffset.getComponentAlongAxis(candidate.getAxis());
				double distToEdge = candidate.getDirection() == Direction.AxisDirection.POSITIVE ? 1.0 - component : component;
				if (distToEdge < minDistance) {
					minDistance = distToEdge;
					pushDirection = candidate;
				}
			}
		}

		float pushStrength = this.random.nextFloat() * 0.2F + 0.1F;
		float directionOffset = pushDirection.getDirection().offset();
		Vec3d dampedVelocity = this.getVelocity().multiply(0.75);
		if (pushDirection.getAxis() == Direction.Axis.X) {
			this.setVelocity(directionOffset * pushStrength, dampedVelocity.y, dampedVelocity.z);
		}
		else if (pushDirection.getAxis() == Direction.Axis.Y) {
			this.setVelocity(dampedVelocity.x, directionOffset * pushStrength, dampedVelocity.z);
		}
		else if (pushDirection.getAxis() == Direction.Axis.Z) {
			this.setVelocity(dampedVelocity.x, dampedVelocity.y, directionOffset * pushStrength);
		}
	}

	public void slowMovement(BlockState state, Vec3d multiplier) {
		this.onLanding();
		this.movementMultiplier = multiplier;
	}

	private static Text removeClickEvents(Text textComponent) {
		MutableText
				mutableText =
				textComponent.copyContentOnly().setStyle(textComponent.getStyle().withClickEvent(null));

		for (Text text : textComponent.getSiblings()) {
			mutableText.append(removeClickEvents(text));
		}

		return mutableText;
	}

	@Override
	public Text getName() {
		Text text = this.getCustomName();
		return text != null ? removeClickEvents(text) : this.getDefaultName();
	}

	protected Text getDefaultName() {
		return this.type.getName();
	}

	public boolean isPartOf(Entity entity) {
		return this == entity;
	}

	public float getHeadYaw() {
		return 0.0F;
	}

	public void setHeadYaw(float headYaw) {
	}

	public void setBodyYaw(float bodyYaw) {
	}

	public boolean isAttackable() {
		return true;
	}

	public boolean handleAttack(Entity attacker) {
		return false;
	}

	@Override
	public String toString() {
		String string = this.getEntityWorld() == null ? "~NULL~" : this.getEntityWorld().toString();
		return this.removalReason != null
		       ? String.format(
				Locale.ROOT,
				"%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f, removed=%s]",
				this.getClass().getSimpleName(),
				this.getStringifiedName(),
				this.id,
				string,
				this.getX(),
				this.getY(),
				this.getZ(),
				this.removalReason
		)
		       : String.format(
				       Locale.ROOT,
				       "%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f]",
				       this.getClass().getSimpleName(),
				       this.getStringifiedName(),
				       this.id,
				       string,
				       this.getX(),
				       this.getY(),
				       this.getZ()
		       );
	}

	protected final boolean isAlwaysInvulnerableTo(DamageSource damageSource) {
		return this.isRemoved()
				|| this.invulnerable && !damageSource.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)
				&& !damageSource.isSourceCreativePlayer()
				|| damageSource.isIn(DamageTypeTags.IS_FIRE) && this.isFireImmune()
				|| damageSource.isIn(DamageTypeTags.IS_FALL) && this.getType().isIn(EntityTypeTags.FALL_DAMAGE_IMMUNE);
	}

	public boolean isInvulnerable() {
		return this.invulnerable;
	}

	public void setInvulnerable(boolean invulnerable) {
		this.invulnerable = invulnerable;
	}

	public void copyPositionAndRotation(Entity entity) {
		this.refreshPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), entity.getYaw(), entity.getPitch());
	}

	public void copyFrom(Entity original) {
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(this.getErrorReporterContext(), LOGGER)) {
			NbtWriteView nbtWriteView = NbtWriteView.create(logging, original.getRegistryManager());
			original.writeData(nbtWriteView);
			this.readData(NbtReadView.create(logging, this.getRegistryManager(), nbtWriteView.getNbt()));
		}

		this.portalCooldown = original.portalCooldown;
		this.portalManager = original.portalManager;
	}

	/**
	 * Телепортирует сущность к указанной цели телепортации.
	 * При кросс-измерении создаёт новую сущность в целевом мире и удаляет текущую.
	 */
	public @Nullable Entity teleportTo(TeleportTarget teleportTarget) {
		if (this.getEntityWorld() instanceof ServerWorld serverWorld && !this.isRemoved()) {
			ServerWorld targetWorld = teleportTarget.world();
			boolean crossDimension = targetWorld.getRegistryKey() != serverWorld.getRegistryKey();
			if (!teleportTarget.asPassenger()) {
				this.stopRiding();
			}

			return crossDimension
					? this.teleportCrossDimension(serverWorld, targetWorld, teleportTarget)
					: this.teleportSameDimension(serverWorld, teleportTarget);
		}
		else {
			return null;
		}
	}

	private Entity teleportSameDimension(ServerWorld world, TeleportTarget teleportTarget) {
		for (Entity entity : this.getPassengerList()) {
			entity.teleportTo(this.getPassengerTeleportTarget(teleportTarget, entity));
		}

		Profiler profiler = Profilers.get();
		profiler.push("teleportSameDimension");
		this.setPosition(EntityPosition.fromTeleportTarget(teleportTarget), teleportTarget.relatives());
		if (!teleportTarget.asPassenger()) {
			this.sendTeleportPacket(teleportTarget);
		}

		teleportTarget.postTeleportTransition().onTransition(this);
		profiler.pop();
		return this;
	}

	private @Nullable Entity teleportCrossDimension(ServerWorld from, ServerWorld to, TeleportTarget teleportTarget) {
		List<Entity> passengers = this.getPassengerList();
		List<Entity> teleportedPassengers = new ArrayList<>(passengers.size());
		this.removeAllPassengers();

		for (Entity passenger : passengers) {
			Entity teleported = passenger.teleportTo(this.getPassengerTeleportTarget(teleportTarget, passenger));
			if (teleported != null) {
				teleportedPassengers.add(teleported);
			}
		}

		Profiler profiler = Profilers.get();
		profiler.push("teleportCrossDimension");
		Entity newEntity = this.getType().create(to, SpawnReason.DIMENSION_TRAVEL);
		if (newEntity == null) {
			profiler.pop();
			return null;
		}

		newEntity.copyFrom(this);
		this.removeFromDimension();
		newEntity.setPosition(
				EntityPosition.fromEntity(this),
				EntityPosition.fromTeleportTarget(teleportTarget),
				teleportTarget.relatives()
		);
		to.onDimensionChanged(newEntity);

		for (Entity teleportedPassenger : teleportedPassengers) {
			teleportedPassenger.startRiding(newEntity, true, false);
		}

		to.resetIdleTimeout();
		teleportTarget.postTeleportTransition().onTransition(newEntity);
		this.teleportSpectatingPlayers(teleportTarget, from);
		profiler.pop();
		return newEntity;
	}

	/** Телепортирует всех наблюдателей, чья камера привязана к этой сущности, к целевой точке. */
	protected void teleportSpectatingPlayers(TeleportTarget teleportTarget, ServerWorld from) {
		for (ServerPlayerEntity serverPlayerEntity : List.copyOf(from.getPlayers())) {
			if (serverPlayerEntity.getCameraEntity() == this) {
				serverPlayerEntity.teleportTo(teleportTarget);
				serverPlayerEntity.setCameraEntity(null);
			}
		}
	}

	private TeleportTarget getPassengerTeleportTarget(TeleportTarget teleportTarget, Entity passenger) {
		float passengerYaw = teleportTarget.yaw() + (teleportTarget.relatives().contains(PositionFlag.Y_ROT)
				? 0.0F
				: passenger.getYaw() - this.getYaw());
		float passengerPitch = teleportTarget.pitch() + (teleportTarget.relatives().contains(PositionFlag.X_ROT)
				? 0.0F
				: passenger.getPitch() - this.getPitch());
		Vec3d offset = passenger.getEntityPos().subtract(this.getEntityPos());
		Vec3d passengerPos = teleportTarget.position()
		                                   .add(
				                                   teleportTarget.relatives().contains(PositionFlag.X) ? 0.0 : offset.getX(),
				                                   teleportTarget.relatives().contains(PositionFlag.Y) ? 0.0 : offset.getY(),
				                                   teleportTarget.relatives().contains(PositionFlag.Z) ? 0.0 : offset.getZ()
		                                   );
		return teleportTarget.withPosition(passengerPos).withRotation(passengerYaw, passengerPitch).withAsPassenger();
	}

	private void sendTeleportPacket(TeleportTarget teleportTarget) {
		Entity entity = this.getControllingPassenger();

		for (Entity entity2 : this.getPassengersDeep()) {
			if (entity2 instanceof ServerPlayerEntity serverPlayerEntity) {
				if (entity != null && serverPlayerEntity.getId() == entity.getId()) {
					serverPlayerEntity.networkHandler
							.sendPacket(
									EntityPositionS2CPacket.create(
											this.getId(),
											EntityPosition.fromTeleportTarget(teleportTarget),
											teleportTarget.relatives(),
											this.onGround
									)
							);
				}
				else {
					serverPlayerEntity.networkHandler
							.sendPacket(EntityPositionS2CPacket.create(
									this.getId(),
									EntityPosition.fromEntity(this),
									Set.of(),
									this.onGround
							));
				}
			}
		}
	}

	public void setPosition(EntityPosition pos, Set<PositionFlag> flags) {
		this.setPosition(EntityPosition.fromEntity(this), pos, flags);
	}

	public void setPosition(EntityPosition currentPos, EntityPosition newPos, Set<PositionFlag> flags) {
		EntityPosition entityPosition = EntityPosition.apply(currentPos, newPos, flags);
		this.setPos(entityPosition.position().x, entityPosition.position().y, entityPosition.position().z);
		this.setYaw(entityPosition.yaw());
		this.setHeadYaw(entityPosition.yaw());
		this.setPitch(entityPosition.pitch());
		this.refreshPosition();
		this.resetPosition();
		this.setVelocity(entityPosition.deltaMovement());
		this.clearQueuedCollisionChecks();
	}

	public void rotate(float yaw, boolean relativeYaw, float pitch, boolean relativePitch) {
		Set<PositionFlag> set = PositionFlag.ofRot(relativeYaw, relativePitch);
		EntityPosition entityPosition = EntityPosition.fromEntity(this);
		EntityPosition entityPosition2 = entityPosition.withRotation(yaw, pitch);
		EntityPosition entityPosition3 = EntityPosition.apply(entityPosition, entityPosition2, set);
		this.setYaw(entityPosition3.yaw());
		this.setHeadYaw(entityPosition3.yaw());
		this.setPitch(entityPosition3.pitch());
		this.updateLastAngles();
	}

	public void addPortalChunkTicketAt(BlockPos pos) {
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			serverWorld.getChunkManager().addTicket(ChunkTicketType.PORTAL, new ChunkPos(pos), 3);
		}
	}

	protected void removeFromDimension() {
		this.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
		if (this instanceof Leashable leashable) {
			leashable.detachLeashWithoutDrop();
		}

		if (this instanceof ServerWaypoint serverWaypoint && this.world instanceof ServerWorld serverWorld) {
			serverWorld.getWaypointHandler().onUntrack(serverWaypoint);
		}
	}

	/**
	 * Вычисляет позицию сущности внутри портала с учётом оси и прямоугольника портала.
	 * Делегирует вычисление в {@link NetherPortal#entityPosInPortal}.
	 */
	public Vec3d positionInPortal(Direction.Axis portalAxis, BlockLocating.Rectangle portalRect) {
		return NetherPortal.entityPosInPortal(
				portalRect,
				portalAxis,
				this.getEntityPos(),
				this.getDimensions(this.getPose())
		);
	}

	public boolean canUsePortals(boolean allowVehicles) {
		return (allowVehicles || !this.hasVehicle()) && this.isAlive();
	}

	public boolean canTeleportBetween(World from, World to) {
		if (from.getRegistryKey() == World.END && to.getRegistryKey() == World.OVERWORLD) {
			for (Entity entity : this.getPassengerList()) {
				if (entity instanceof ServerPlayerEntity serverPlayerEntity && !serverPlayerEntity.seenCredits) {
					return false;
				}
			}
		}

		return true;
	}

	public float getEffectiveExplosionResistance(
			Explosion explosion,
			BlockView world,
			BlockPos pos,
			BlockState blockState,
			FluidState fluidState,
			float max
	) {
		return max;
	}

	public boolean canExplosionDestroyBlock(
			Explosion explosion,
			BlockView world,
			BlockPos pos,
			BlockState state,
			float explosionPower
	) {
		return true;
	}

	public int getSafeFallDistance() {
		return 3;
	}

	public boolean canAvoidTraps() {
		return false;
	}

	public void populateCrashReport(CrashReportSection section) {
		section.add(
				"Entity Type",
				() -> EntityType.getId(this.getType()) + " (" + this.getClass().getCanonicalName() + ")"
		);
		section.add("Entity ID", this.id);
		section.add("Entity Name", () -> this.getStringifiedName());
		section.add(
				"Entity's Exact location",
				String.format(Locale.ROOT, "%.2f, %.2f, %.2f", this.getX(), this.getY(), this.getZ())
		);
		section.add(
				"Entity's Block location",
				CrashReportSection.createPositionString(
						this.getEntityWorld(),
						MathHelper.floor(this.getX()),
						MathHelper.floor(this.getY()),
						MathHelper.floor(this.getZ())
				)
		);
		Vec3d velocity = this.getVelocity();
		section.add("Entity's Momentum", String.format(Locale.ROOT, "%.2f, %.2f, %.2f", velocity.x, velocity.y, velocity.z));
		section.add("Entity's Passengers", () -> this.getPassengerList().toString());
		section.add("Entity's Vehicle", () -> String.valueOf(this.getVehicle()));
	}

	public boolean doesRenderOnFire() {
		return this.isOnFire() && !this.isSpectator();
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
		this.uuidString = this.uuid.toString();
	}

	@Override
	public UUID getUuid() {
		return this.uuid;
	}

	public String getUuidAsString() {
		return this.uuidString;
	}

	@Override
	public String getNameForScoreboard() {
		return this.uuidString;
	}

	public boolean isPushedByFluids() {
		return true;
	}

	public static double getRenderDistanceMultiplier() {
		return renderDistanceMultiplier;
	}

	public static void setRenderDistanceMultiplier(double value) {
		renderDistanceMultiplier = value;
	}

	@Override
	public Text getDisplayName() {
		return Team.decorateName(this.getScoreboardTeam(), this.getName())
		           .styled(style -> style.withHoverEvent(this.getHoverEvent()).withInsertion(this.getUuidAsString()));
	}

	public void setCustomName(@Nullable Text name) {
		this.dataTracker.set(CUSTOM_NAME, Optional.ofNullable(name));
	}

	@Override
	public @Nullable Text getCustomName() {
		return this.dataTracker.get(CUSTOM_NAME).orElse(null);
	}

	@Override
	public boolean hasCustomName() {
		return this.dataTracker.get(CUSTOM_NAME).isPresent();
	}

	public void setCustomNameVisible(boolean visible) {
		this.dataTracker.set(NAME_VISIBLE, visible);
	}

	public boolean isCustomNameVisible() {
		return this.dataTracker.get(NAME_VISIBLE);
	}

	public boolean teleport(
			ServerWorld world,
			double destX,
			double destY,
			double destZ,
			Set<PositionFlag> flags,
			float yaw,
			float pitch,
			boolean resetCamera
	) {
		Entity
				entity =
				this.teleportTo(new TeleportTarget(
						world,
						new Vec3d(destX, destY, destZ),
						Vec3d.ZERO,
						yaw,
						pitch,
						flags,
						TeleportTarget.NO_OP
				));
		return entity != null;
	}

	public void requestTeleportAndDismount(double destX, double destY, double destZ) {
		this.requestTeleport(destX, destY, destZ);
	}

	public void requestTeleport(double destX, double destY, double destZ) {
		if (this.getEntityWorld() instanceof ServerWorld) {
			this.refreshPositionAndAngles(destX, destY, destZ, this.getYaw(), this.getPitch());
			this.teleportPassengers();
		}
	}

	private void teleportPassengers() {
		this.streamSelfAndPassengers().forEach(entity -> {
			for (Entity passenger : entity.passengerList) {
				entity.updatePassengerPosition(passenger, Entity::refreshPositionAfterTeleport);
			}
		});
	}

	public void requestTeleportOffset(double offsetX, double offsetY, double offsetZ) {
		this.requestTeleport(this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ);
	}

	public boolean shouldRenderName() {
		return this.isCustomNameVisible();
	}

	@Override
	public void onDataTrackerUpdate(List<DataTracker.SerializedEntry<?>> entries) {
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (POSE.equals(data)) {
			this.calculateDimensions();
		}
	}

	/** @deprecated Используй {@link #calculateDimensions()} вместо этого. */
	@Deprecated
	protected void reinitDimensions() {
		EntityPose entityPose = this.getPose();
		EntityDimensions entityDimensions = this.getDimensions(entityPose);
		this.dimensions = entityDimensions;
		this.standingEyeHeight = entityDimensions.eyeHeight();
	}

	public void calculateDimensions() {
		EntityDimensions previousDimensions = this.dimensions;
		EntityPose pose = this.getPose();
		EntityDimensions newDimensions = this.getDimensions(pose);
		this.dimensions = newDimensions;
		this.standingEyeHeight = newDimensions.eyeHeight();
		this.refreshPosition();
		boolean fitsInWorld = newDimensions.width() <= 4.0F && newDimensions.height() <= 4.0F;
		if (!this.world.isClient()
				&& !this.firstUpdate
				&& !this.noClip
				&& fitsInWorld
				&& (newDimensions.width() > previousDimensions.width()
				|| newDimensions.height() > previousDimensions.height()
		)
				&& !(this instanceof PlayerEntity)) {
			this.recalculateDimensions(previousDimensions);
		}
	}

	public boolean recalculateDimensions(EntityDimensions previous) {
		EntityDimensions current = this.getDimensions(this.getPose());
		Vec3d center = this.getEntityPos().add(0.0, previous.height() / 2.0, 0.0);
		double widthDelta = Math.max(0.0F, current.width() - previous.width()) + 1.0E-6;
		double heightDelta = Math.max(0.0F, current.height() - previous.height()) + 1.0E-6;
		VoxelShape searchShape = VoxelShapes.cuboid(Box.of(center, widthDelta, heightDelta, widthDelta));
		Optional<Vec3d> collision = this.world
				.findClosestCollision(
						this,
						searchShape,
						center,
						current.width(),
						current.height(),
						current.width()
				);
		if (collision.isPresent()) {
			this.setPosition(collision.get().add(0.0, -current.height() / 2.0, 0.0));
			return true;
		}

		if (current.width() > previous.width() && current.height() > previous.height()) {
			VoxelShape flatShape = VoxelShapes.cuboid(Box.of(center, widthDelta, 1.0E-6, widthDelta));
			Optional<Vec3d> flatCollision = this.world
					.findClosestCollision(
							this,
							flatShape,
							center,
							current.width(),
							previous.height(),
							current.width()
					);
			if (flatCollision.isPresent()) {
				this.setPosition(flatCollision.get().add(0.0, -previous.height() / 2.0 + 1.0E-6, 0.0));
				return true;
			}
		}

		return false;
	}

	public Direction getHorizontalFacing() {
		return Direction.fromHorizontalDegrees(this.getYaw());
	}

	public Direction getMovementDirection() {
		return this.getHorizontalFacing();
	}

	protected HoverEvent getHoverEvent() {
		return new HoverEvent.ShowEntity(new HoverEvent.EntityContent(this.getType(), this.getUuid(), this.getName()));
	}

	public boolean canBeSpectated(ServerPlayerEntity spectator) {
		return true;
	}

	@Override
	public final Box getBoundingBox() {
		return this.boundingBox;
	}

	public final void setBoundingBox(Box boundingBox) {
		this.boundingBox = boundingBox;
	}

	public final float getEyeHeight(EntityPose pose) {
		return this.getDimensions(pose).eyeHeight();
	}

	public final float getStandingEyeHeight() {
		return this.standingEyeHeight;
	}

	@Override
	public @Nullable StackReference getStackReference(int slot) {
		return null;
	}

	public ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand) {
		return ActionResult.PASS;
	}

	public boolean isImmuneToExplosion(Explosion explosion) {
		return false;
	}

	public void onStartedTrackingBy(ServerPlayerEntity player) {
	}

	public void onStoppedTrackingBy(ServerPlayerEntity player) {
	}

	public float applyRotation(BlockRotation rotation) {
		float yaw = MathHelper.wrapDegrees(this.getYaw());

		return switch (rotation) {
			case CLOCKWISE_180 -> yaw + 180.0F;
			case COUNTERCLOCKWISE_90 -> yaw + 270.0F;
			case CLOCKWISE_90 -> yaw + 90.0F;
			default -> yaw;
		};
	}

	public float applyMirror(BlockMirror mirror) {
		float yaw = MathHelper.wrapDegrees(this.getYaw());

		return switch (mirror) {
			case FRONT_BACK -> -yaw;
			case LEFT_RIGHT -> 180.0F - yaw;
			default -> yaw;
		};
	}

	public ProjectileDeflection getProjectileDeflection(ProjectileEntity projectile) {
		return this.getType().isIn(EntityTypeTags.DEFLECTS_PROJECTILES) ? ProjectileDeflection.SIMPLE
		                                                                : ProjectileDeflection.NONE;
	}

	public @Nullable LivingEntity getControllingPassenger() {
		return null;
	}

	public final boolean hasControllingPassenger() {
		return this.getControllingPassenger() != null;
	}

	public final List<Entity> getPassengerList() {
		return this.passengerList;
	}

	public @Nullable Entity getFirstPassenger() {
		return this.passengerList.isEmpty() ? null : this.passengerList.get(0);
	}

	public boolean hasPassenger(Entity passenger) {
		return this.passengerList.contains(passenger);
	}

	public boolean hasPassenger(Predicate<Entity> predicate) {
		for (Entity passenger : this.passengerList) {
			if (predicate.test(passenger)) {
				return true;
			}
		}

		return false;
	}

	private Stream<Entity> streamIntoPassengers() {
		return this.passengerList.stream().flatMap(Entity::streamSelfAndPassengers);
	}

	@Override
	public Stream<Entity> streamSelfAndPassengers() {
		return Stream.concat(Stream.of(this), this.streamIntoPassengers());
	}

	@Override
	public Stream<Entity> streamPassengersAndSelf() {
		return Stream.concat(this.passengerList.stream().flatMap(Entity::streamPassengersAndSelf), Stream.of(this));
	}

	public Iterable<Entity> getPassengersDeep() {
		return () -> this.streamIntoPassengers().iterator();
	}

	public int getPlayerPassengers() {
		return (int) this.streamIntoPassengers().filter(passenger -> passenger instanceof PlayerEntity).count();
	}

	public boolean hasPlayerRider() {
		return this.getPlayerPassengers() == 1;
	}

	public Entity getRootVehicle() {
		Entity entity = this;

		while (entity.hasVehicle()) {
			entity = entity.getVehicle();
		}

		return entity;
	}

	public boolean isConnectedThroughVehicle(Entity entity) {
		return this.getRootVehicle() == entity.getRootVehicle();
	}

	public boolean hasPassengerDeep(Entity passenger) {
		if (!passenger.hasVehicle()) {
			return false;
		}

		Entity vehicle = passenger.getVehicle();
		return vehicle == this || this.hasPassengerDeep(vehicle);
	}

	public final boolean isLogicalSideForUpdatingMovement() {
		return this.world.isClient() ? this.isControlledByMainPlayer() : !this.isControlledByPlayer();
	}

	protected boolean isControlledByMainPlayer() {
		LivingEntity livingEntity = this.getControllingPassenger();
		return livingEntity != null && livingEntity.isControlledByMainPlayer();
	}

	public boolean isControlledByPlayer() {
		LivingEntity livingEntity = this.getControllingPassenger();
		return livingEntity != null && livingEntity.isControlledByPlayer();
	}

	public boolean canMoveVoluntarily() {
		return this.isLogicalSideForUpdatingMovement();
	}

	public boolean canActVoluntarily() {
		return this.isLogicalSideForUpdatingMovement();
	}

	protected static Vec3d getPassengerDismountOffset(double vehicleWidth, double passengerWidth, float passengerYaw) {
		double halfGap = (vehicleWidth + passengerWidth + 1.0E-5F) / 2.0;
		float sinYaw = -MathHelper.sin(passengerYaw * (float) (Math.PI / 180.0));
		float cosYaw = MathHelper.cos(passengerYaw * (float) (Math.PI / 180.0));
		float maxComponent = Math.max(Math.abs(sinYaw), Math.abs(cosYaw));
		return new Vec3d(sinYaw * halfGap / maxComponent, 0.0, cosYaw * halfGap / maxComponent);
	}

	public Vec3d updatePassengerForDismount(LivingEntity passenger) {
		return new Vec3d(this.getX(), this.getBoundingBox().maxY, this.getZ());
	}

	public @Nullable Entity getVehicle() {
		return this.vehicle;
	}

	public @Nullable Entity getControllingVehicle() {
		return this.vehicle != null && this.vehicle.getControllingPassenger() == this ? this.vehicle : null;
	}

	public PistonBehavior getPistonBehavior() {
		return PistonBehavior.NORMAL;
	}

	public SoundCategory getSoundCategory() {
		return SoundCategory.NEUTRAL;
	}

	protected int getBurningDuration() {
		return 0;
	}

	public ServerCommandSource getCommandSource(ServerWorld world) {
		return new ServerCommandSource(
				CommandOutput.DUMMY,
				this.getEntityPos(),
				this.getRotationClient(),
				world,
				PermissionPredicate.NONE,
				this.getStringifiedName(),
				this.getDisplayName(),
				world.getServer(),
				this
		);
	}

	public void lookAt(EntityAnchorArgumentType.EntityAnchor anchorPoint, Vec3d target) {
		Vec3d anchorPos = anchorPoint.positionAt(this);
		double deltaX = target.x - anchorPos.x;
		double deltaY = target.y - anchorPos.y;
		double deltaZ = target.z - anchorPos.z;
		double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
		this.setPitch(MathHelper.wrapDegrees((float) (-(MathHelper.atan2(deltaY, horizontalDist) * 180.0F / (float) Math.PI))));
		this.setYaw(MathHelper.wrapDegrees((float) (MathHelper.atan2(deltaZ, deltaX) * 180.0F / (float) Math.PI) - 90.0F));
		this.setHeadYaw(this.getYaw());
		this.lastPitch = this.getPitch();
		this.lastYaw = this.getYaw();
	}

	public float lerpYaw(float tickProgress) {
		return MathHelper.lerp(tickProgress, this.lastYaw, this.yaw);
	}

	public boolean updateMovementInFluid(TagKey<Fluid> tag, double speed) {
		if (this.isRegionUnloaded()) {
			return false;
		}

		Box box = this.getBoundingBox().contract(0.001);
		int minBlockX = MathHelper.floor(box.minX);
		int maxBlockX = MathHelper.ceil(box.maxX);
		int minBlockY = MathHelper.floor(box.minY);
		int maxBlockY = MathHelper.ceil(box.maxY);
		int minBlockZ = MathHelper.floor(box.minZ);
		int maxBlockZ = MathHelper.ceil(box.maxZ);
		double fluidDepth = 0.0;
		boolean pushedByFluids = this.isPushedByFluids();
		boolean touchingFluid = false;
		Vec3d fluidFlow = Vec3d.ZERO;
		int flowSamples = 0;
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int bx = minBlockX; bx < maxBlockX; bx++) {
			for (int by = minBlockY; by < maxBlockY; by++) {
				for (int bz = minBlockZ; bz < maxBlockZ; bz++) {
					mutable.set(bx, by, bz);
					FluidState fluidState = this.getEntityWorld().getFluidState(mutable);
					if (fluidState.isIn(tag)) {
						double fluidTop = by + fluidState.getHeight(this.getEntityWorld(), mutable);
						if (fluidTop >= box.minY) {
							touchingFluid = true;
							fluidDepth = Math.max(fluidTop - box.minY, fluidDepth);
							if (pushedByFluids) {
								Vec3d flowVelocity = fluidState.getVelocity(this.getEntityWorld(), mutable);
								if (fluidDepth < 0.4) {
									flowVelocity = flowVelocity.multiply(fluidDepth);
								}

								fluidFlow = fluidFlow.add(flowVelocity);
								flowSamples++;
							}
						}
					}
				}
			}
		}

		if (fluidFlow.length() > 0.0) {
			if (flowSamples > 0) {
				fluidFlow = fluidFlow.multiply(1.0 / flowSamples);
			}

			if (!(this instanceof PlayerEntity)) {
				fluidFlow = fluidFlow.normalize();
			}

			Vec3d currentVelocity = this.getVelocity();
			fluidFlow = fluidFlow.multiply(speed);
			// Минимальный порог скорости для предотвращения "дрожания" в жидкости
			if (Math.abs(currentVelocity.x) < 0.003 && Math.abs(currentVelocity.z) < 0.003
					&& fluidFlow.length() < 0.0045000000000000005) {
				fluidFlow = fluidFlow.normalize().multiply(0.0045000000000000005);
			}

			this.setVelocity(this.getVelocity().add(fluidFlow));
		}

		this.fluidHeight.put(tag, fluidDepth);
		return touchingFluid;
	}

	public boolean isRegionUnloaded() {
		Box box = this.getBoundingBox().expand(1.0);
		int minX = MathHelper.floor(box.minX);
		int maxX = MathHelper.ceil(box.maxX);
		int minZ = MathHelper.floor(box.minZ);
		int maxZ = MathHelper.ceil(box.maxZ);
		return !this.getEntityWorld().isRegionLoaded(minX, minZ, maxX, maxZ);
	}

	public double getFluidHeight(TagKey<Fluid> fluid) {
		return this.fluidHeight.getDouble(fluid);
	}

	public double getSwimHeight() {
		return this.getStandingEyeHeight() < 0.4 ? 0.0 : 0.4;
	}

	public final float getWidth() {
		return this.dimensions.width();
	}

	public final float getHeight() {
		return this.dimensions.height();
	}

	public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
		return new EntitySpawnS2CPacket(this, entityTrackerEntry);
	}

	public EntityDimensions getDimensions(EntityPose pose) {
		return this.type.getDimensions();
	}

	public final EntityAttachments getAttachments() {
		return this.dimensions.attachments();
	}

	@Override
	public Vec3d getEntityPos() {
		return this.pos;
	}

	public Vec3d getSyncedPos() {
		return this.getEntityPos();
	}

	@Override
	public BlockPos getBlockPos() {
		return this.blockPos;
	}

	public BlockState getBlockStateAtPos() {
		if (this.stateAtPos == null) {
			this.stateAtPos = this.getEntityWorld().getBlockState(this.getBlockPos());
		}

		return this.stateAtPos;
	}

	public ChunkPos getChunkPos() {
		return this.chunkPos;
	}

	public Vec3d getVelocity() {
		return this.velocity;
	}

	public void setVelocity(Vec3d velocity) {
		if (velocity.isFinite()) {
			this.velocity = velocity;
		}
	}

	public void addVelocityInternal(Vec3d velocity) {
		if (velocity.isFinite()) {
			this.setVelocity(this.getVelocity().add(velocity));
		}
	}

	public void setVelocity(double x, double y, double z) {
		this.setVelocity(new Vec3d(x, y, z));
	}

	public final int getBlockX() {
		return this.blockPos.getX();
	}

	public final double getX() {
		return this.pos.x;
	}

	public double getBodyX(double widthScale) {
		return this.pos.x + this.getWidth() * widthScale;
	}

	public double getParticleX(double widthScale) {
		return this.getBodyX((2.0 * this.random.nextDouble() - 1.0) * widthScale);
	}

	public final int getBlockY() {
		return this.blockPos.getY();
	}

	public final double getY() {
		return this.pos.y;
	}

	public double getBodyY(double heightScale) {
		return this.pos.y + this.getHeight() * heightScale;
	}

	public double getRandomBodyY() {
		return this.getBodyY(this.random.nextDouble());
	}

	public double getEyeY() {
		return this.pos.y + this.standingEyeHeight;
	}

	public final int getBlockZ() {
		return this.blockPos.getZ();
	}

	public final double getZ() {
		return this.pos.z;
	}

	public double getBodyZ(double widthScale) {
		return this.pos.z + this.getWidth() * widthScale;
	}

	public double getParticleZ(double widthScale) {
		return this.getBodyZ((2.0 * this.random.nextDouble() - 1.0) * widthScale);
	}

	public final void setPos(double x, double y, double z) {
		if (this.pos.x != x || this.pos.y != y || this.pos.z != z) {
			this.pos = new Vec3d(x, y, z);
			int blockX = MathHelper.floor(x);
			int blockY = MathHelper.floor(y);
			int blockZ = MathHelper.floor(z);
			if (blockX != this.blockPos.getX() || blockY != this.blockPos.getY() || blockZ != this.blockPos.getZ()) {
				this.blockPos = new BlockPos(blockX, blockY, blockZ);
				this.stateAtPos = null;
				if (ChunkSectionPos.getSectionCoord(blockX) != this.chunkPos.x
						|| ChunkSectionPos.getSectionCoord(blockZ) != this.chunkPos.z) {
					this.chunkPos = new ChunkPos(this.blockPos);
				}
			}

			this.changeListener.updateEntityPosition();
			if (!this.firstUpdate && this.world instanceof ServerWorld serverWorld && !this.isRemoved()) {
				if (this instanceof ServerWaypoint serverWaypoint && serverWaypoint.hasWaypoint()) {
					serverWorld.getWaypointHandler().onUpdate(serverWaypoint);
				}

				if (this instanceof ServerPlayerEntity serverPlayerEntity && serverPlayerEntity.canReceiveWaypoints()
						&& serverPlayerEntity.networkHandler != null) {
					serverWorld.getWaypointHandler().updatePlayerPos(serverPlayerEntity);
				}
			}
		}
	}

	public void checkDespawn() {
	}

	public Vec3d[] getHeldQuadLeashOffsets() {
		return Leashable.createQuadLeashOffsets(this, 0.0, 0.5, 0.5, 0.0);
	}

	public boolean hasQuadLeashAttachmentPoints() {
		return false;
	}

	public void tickHeldLeash(Leashable leashedEntity) {
	}

	public void onHeldLeashUpdate(Leashable heldLeashable) {
	}

	public Vec3d getLeashPos(float tickProgress) {
		return this.getLerpedPos(tickProgress).add(0.0, this.standingEyeHeight * 0.7, 0.0);
	}

	public void onSpawnPacket(EntitySpawnS2CPacket packet) {
		int entityId = packet.getEntityId();
		double spawnX = packet.getX();
		double spawnY = packet.getY();
		double spawnZ = packet.getZ();
		this.updateTrackedPosition(spawnX, spawnY, spawnZ);
		this.refreshPositionAndAngles(spawnX, spawnY, spawnZ, packet.getYaw(), packet.getPitch());
		this.setId(entityId);
		this.setUuid(packet.getUuid());
		this.setVelocity(packet.getVelocity());
	}

	public @Nullable ItemStack getPickBlockStack() {
		return null;
	}

	public void setInPowderSnow(boolean inPowderSnow) {
		this.inPowderSnow = inPowderSnow;
	}

	public boolean canFreeze() {
		return !this.getType().isIn(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES);
	}

	public boolean shouldEscapePowderSnow() {
		return this.getFrozenTicks() > 0;
	}

	public float getYaw() {
		return this.yaw;
	}

	@Override
	public float getBodyYaw() {
		return this.getYaw();
	}

	public void setYaw(float yaw) {
		if (!Float.isFinite(yaw)) {
			Util.logErrorOrPause("Invalid entity rotation: " + yaw + ", discarding.");
		}
		else {
			this.yaw = yaw;
		}
	}

	public float getPitch() {
		return this.pitch;
	}

	public void setPitch(float pitch) {
		if (!Float.isFinite(pitch)) {
			Util.logErrorOrPause("Invalid entity rotation: " + pitch + ", discarding.");
		}
		else {
			this.pitch = Math.clamp(pitch % 360.0F, -90.0F, 90.0F);
		}
	}

	public boolean canSprintAsVehicle() {
		return false;
	}

	public float getStepHeight() {
		return 0.0F;
	}

	public void onExplodedBy(@Nullable Entity entity) {
	}

	@Override
	public final boolean isRemoved() {
		return this.removalReason != null;
	}

	public Entity.@Nullable RemovalReason getRemovalReason() {
		return this.removalReason;
	}

	@Override
	public final void setRemoved(Entity.RemovalReason reason) {
		if (this.removalReason == null) {
			this.removalReason = reason;
		}

		if (this.removalReason.shouldDestroy()) {
			this.stopRiding();
		}

		this.getPassengerList().forEach(Entity::stopRiding);
		this.changeListener.remove(reason);
		this.onRemove(reason);
	}

	/** Сбрасывает причину удаления, возвращая сущность в активное состояние. */
	protected void unsetRemoved() {
		this.removalReason = null;
	}

	@Override
	public void setChangeListener(EntityChangeListener changeListener) {
		this.changeListener = changeListener;
	}

	@Override
	public boolean shouldSave() {
		if (this.removalReason != null && !this.removalReason.shouldSave()) {
			return false;
		}

		return !this.hasVehicle() && (!this.hasPassengers() || !this.hasPlayerRider());
	}

	@Override
	public boolean isPlayer() {
		return false;
	}

	public boolean canModifyAt(ServerWorld world, BlockPos pos) {
		return true;
	}

	public boolean isFlyingVehicle() {
		return false;
	}

	@Override
	public World getEntityWorld() {
		return this.world;
	}

	protected void setWorld(World world) {
		this.world = world;
	}

	public DamageSources getDamageSources() {
		return this.getEntityWorld().getDamageSources();
	}

	public DynamicRegistryManager getRegistryManager() {
		return this.getEntityWorld().getRegistryManager();
	}

	protected void lerpPosAndRotation(int step, double x, double y, double z, double yaw, double pitch) {
		double factor = 1.0 / step;
		double lerpX = MathHelper.lerp(factor, this.getX(), x);
		double lerpY = MathHelper.lerp(factor, this.getY(), y);
		double lerpZ = MathHelper.lerp(factor, this.getZ(), z);
		float lerpYaw = (float) MathHelper.lerpAngleDegrees(factor, (double) this.getYaw(), yaw);
		float lerpPitch = (float) MathHelper.lerp(factor, (double) this.getPitch(), pitch);
		this.setPosition(lerpX, lerpY, lerpZ);
		this.setRotation(lerpYaw, lerpPitch);
	}

	public Random getRandom() {
		return this.random;
	}

	public Vec3d getMovement() {
		return this.getControllingPassenger() instanceof PlayerEntity playerEntity && this.isAlive()
		       ? playerEntity.getMovement() : this.getVelocity();
	}

	public Vec3d getKineticAttackMovement() {
		return this.getControllingPassenger() instanceof PlayerEntity playerEntity && this.isAlive()
		       ? playerEntity.getKineticAttackMovement() : this.movement;
	}

	public @Nullable ItemStack getWeaponStack() {
		return null;
	}

	public Optional<RegistryKey<LootTable>> getLootTableKey() {
		return this.type.getLootTableKey();
	}

	protected void copyComponentsFrom(ComponentsAccess from) {
		this.copyComponentFrom(from, DataComponentTypes.CUSTOM_NAME);
		this.copyComponentFrom(from, DataComponentTypes.CUSTOM_DATA);
	}

	public final void copyComponentsFrom(ItemStack stack) {
		this.copyComponentsFrom(stack.getComponents());
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		if (type == DataComponentTypes.CUSTOM_NAME) {
			return castComponentValue((ComponentType<T>) type, this.getCustomName());
		}
		else {
			return type == DataComponentTypes.CUSTOM_DATA ? castComponentValue((ComponentType<T>) type, this.customData)
			                                              : null;
		}
	}

	@Contract("_,!null->!null;_,_->_")
	protected static <T> @Nullable T castComponentValue(ComponentType<T> type, @Nullable Object value) {
		return (T) value;
	}

	public <T> void setComponent(ComponentType<T> type, T value) {
		this.setApplicableComponent(type, value);
	}

	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.CUSTOM_NAME) {
			this.setCustomName(castComponentValue(DataComponentTypes.CUSTOM_NAME, value));
			return true;
		}
		else if (type == DataComponentTypes.CUSTOM_DATA) {
			this.customData = castComponentValue(DataComponentTypes.CUSTOM_DATA, value);
			return true;
		}
		else {
			return false;
		}
	}

	protected <T> boolean copyComponentFrom(ComponentsAccess from, ComponentType<T> type) {
		T object = from.get(type);
		return object != null && this.setApplicableComponent(type, object);
	}

	public ErrorReporter.Context getErrorReporterContext() {
		return new Entity.ErrorReporterContext(this);
	}

	@Override
	public void registerTracking(ServerWorld world, DebugTrackable.Tracker tracker) {
	}

	record ErrorReporterContext(Entity entity) implements ErrorReporter.Context {

		@Override
		public String getName() {
			return this.entity.toString();
		}
	}

	public enum MoveEffect {
		NONE(false, false),
		SOUNDS(true, false),
		EVENTS(false, true),
		ALL(true, true);

		private final boolean sounds;
		private final boolean events;

		MoveEffect(boolean sounds, boolean events) {
			this.sounds = sounds;
			this.events = events;
		}

		public boolean hasAny() {
			return this.events || this.sounds;
		}

		public boolean emitsGameEvents() {
			return this.events;
		}

		public boolean playsSounds() {
			return this.sounds;
		}
	}

	/** Функциональный интерфейс для обновления позиции пассажира на транспортном средстве. */
	@FunctionalInterface
	public interface PositionUpdater {

		void accept(Entity entity, double x, double y, double z);
	}

	record QueuedCollisionCheck(Vec3d from, Vec3d to, Optional<Vec3d> axisDependentOriginalMovement) {

		public QueuedCollisionCheck(Vec3d from, Vec3d to, Vec3d originalMovement) {
			this(from, to, Optional.of(originalMovement));
		}

		public QueuedCollisionCheck(Vec3d from, Vec3d to) {
			this(from, to, Optional.empty());
		}
	}

	public enum RemovalReason {
		KILLED(true, false),
		DISCARDED(true, false),
		UNLOADED_TO_CHUNK(false, true),
		UNLOADED_WITH_PLAYER(false, false),
		CHANGED_DIMENSION(false, false);

		private final boolean destroy;
		private final boolean save;

		RemovalReason(boolean destroy, boolean save) {
			this.destroy = destroy;
			this.save = save;
		}

		public boolean shouldDestroy() {
			return this.destroy;
		}

		public boolean shouldSave() {
			return this.save;
		}
	}
}

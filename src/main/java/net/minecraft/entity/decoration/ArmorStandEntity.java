package net.minecraft.entity.decoration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * {@code ArmorStandEntity}.
 */
public class ArmorStandEntity extends LivingEntity {

	public static final int PUNCH_DAMAGE = 5;
	private static final boolean PUSHABLE = true;
	public static final EulerAngle DEFAULT_HEAD_ROTATION = new EulerAngle(0.0F, 0.0F, 0.0F);
	public static final EulerAngle DEFAULT_BODY_ROTATION = new EulerAngle(0.0F, 0.0F, 0.0F);
	public static final EulerAngle DEFAULT_LEFT_ARM_ROTATION = new EulerAngle(-10.0F, 0.0F, -10.0F);
	public static final EulerAngle DEFAULT_RIGHT_ARM_ROTATION = new EulerAngle(-15.0F, 0.0F, 10.0F);
	public static final EulerAngle DEFAULT_LEFT_LEG_ROTATION = new EulerAngle(-1.0F, 0.0F, -1.0F);
	public static final EulerAngle DEFAULT_RIGHT_LEG_ROTATION = new EulerAngle(1.0F, 0.0F, 1.0F);
	private static final EntityDimensions MARKER_DIMENSIONS = EntityDimensions.fixed(0.0F, 0.0F);
	private static final EntityDimensions
			SMALL_DIMENSIONS =
			EntityType.ARMOR_STAND.getDimensions().scaled(0.5F).withEyeHeight(0.9875F);
	private static final double FEET_SLOT_Y_MIN = 0.1;
	private static final double CHEST_SLOT_Y_MIN = 0.9;
	private static final double LEGS_SLOT_Y_MIN = 0.4;
	private static final double HEAD_SLOT_Y_MIN = 1.6;
	public static final int DISABLE_TAKING_FLAG = 8;
	public static final int DISABLE_PUTTING_FLAG = 16;
	public static final int SMALL_FLAG = 1;
	public static final int SHOW_ARMS_FLAG = 4;
	public static final int HIDE_BASE_PLATE_FLAG = 8;
	public static final int MARKER_FLAG = 16;
	public static final TrackedData<Byte>
			ARMOR_STAND_FLAGS =
			DataTracker.registerData(ArmorStandEntity.class, TrackedDataHandlerRegistry.BYTE);
	public static final TrackedData<EulerAngle>
			TRACKER_HEAD_ROTATION =
			DataTracker.registerData(ArmorStandEntity.class, TrackedDataHandlerRegistry.ROTATION);
	public static final TrackedData<EulerAngle>
			TRACKER_BODY_ROTATION =
			DataTracker.registerData(ArmorStandEntity.class, TrackedDataHandlerRegistry.ROTATION);
	public static final TrackedData<EulerAngle>
			TRACKER_LEFT_ARM_ROTATION =
			DataTracker.registerData(ArmorStandEntity.class, TrackedDataHandlerRegistry.ROTATION);
	public static final TrackedData<EulerAngle> TRACKER_RIGHT_ARM_ROTATION = DataTracker.registerData(
			ArmorStandEntity.class, TrackedDataHandlerRegistry.ROTATION
	);
	public static final TrackedData<EulerAngle>
			TRACKER_LEFT_LEG_ROTATION =
			DataTracker.registerData(ArmorStandEntity.class, TrackedDataHandlerRegistry.ROTATION);
	public static final TrackedData<EulerAngle> TRACKER_RIGHT_LEG_ROTATION = DataTracker.registerData(
			ArmorStandEntity.class, TrackedDataHandlerRegistry.ROTATION
	);
	private static final Predicate<Entity>
			RIDEABLE_MINECART_PREDICATE =
			entity -> entity instanceof AbstractMinecartEntity abstractMinecartEntity
					&& abstractMinecartEntity.isRideable();
	private static final boolean SLOT_MAINHAND_DISABLED = false;
	private static final int DEFAULT_DISABLED_SLOTS = 0;
	private static final boolean SLOT_OFFHAND_DISABLED = false;
	private static final boolean SLOT_FEET_DISABLED = false;
	private static final boolean SLOT_LEGS_DISABLED = false;
	private static final boolean SLOT_CHEST_DISABLED = false;
	private boolean invisible = false;
	public long lastHitTime;
	private int disabledSlots = 0;

	public ArmorStandEntity(EntityType<? extends ArmorStandEntity> entityType, World world) {
		super(entityType, world);
	}

	public ArmorStandEntity(World world, double x, double y, double z) {
		this(EntityType.ARMOR_STAND, world);
		this.setPosition(x, y, z);
	}

	public static DefaultAttributeContainer.Builder createArmorStandAttributes() {
		return createLivingAttributes().add(EntityAttributes.STEP_HEIGHT, 0.0);
	}

	@Override
	public void calculateDimensions() {
		double d = this.getX();
		double e = this.getY();
		double f = this.getZ();
		super.calculateDimensions();
		this.setPosition(d, e, f);
	}

	private boolean canClip() {
		return !this.isMarker() && !this.hasNoGravity();
	}

	@Override
	public boolean canActVoluntarily() {
		return super.canActVoluntarily() && this.canClip();
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(ARMOR_STAND_FLAGS, (byte) 0);
		builder.add(TRACKER_HEAD_ROTATION, DEFAULT_HEAD_ROTATION);
		builder.add(TRACKER_BODY_ROTATION, DEFAULT_BODY_ROTATION);
		builder.add(TRACKER_LEFT_ARM_ROTATION, DEFAULT_LEFT_ARM_ROTATION);
		builder.add(TRACKER_RIGHT_ARM_ROTATION, DEFAULT_RIGHT_ARM_ROTATION);
		builder.add(TRACKER_LEFT_LEG_ROTATION, DEFAULT_LEFT_LEG_ROTATION);
		builder.add(TRACKER_RIGHT_LEG_ROTATION, DEFAULT_RIGHT_LEG_ROTATION);
	}

	@Override
	public boolean canUseSlot(EquipmentSlot slot) {
		return slot != EquipmentSlot.BODY && slot != EquipmentSlot.SADDLE && !this.isSlotDisabled(slot);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("Invisible", this.isInvisible());
		view.putBoolean("Small", this.isSmall());
		view.putBoolean("ShowArms", this.shouldShowArms());
		view.putInt("DisabledSlots", this.disabledSlots);
		view.putBoolean("NoBasePlate", !this.shouldShowBasePlate());
		if (this.isMarker()) {
			view.putBoolean("Marker", this.isMarker());
		}

		view.put("Pose", ArmorStandEntity.PackedRotation.CODEC, this.packRotation());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		this.setInvisible(view.getBoolean("Invisible", false));
		this.setSmall(view.getBoolean("Small", false));
		this.setShowArms(view.getBoolean("ShowArms", false));
		this.disabledSlots = view.getInt("DisabledSlots", 0);
		this.setHideBasePlate(view.getBoolean("NoBasePlate", false));
		this.setMarker(view.getBoolean("Marker", false));
		this.noClip = !this.canClip();
		view
				.<ArmorStandEntity.PackedRotation>read("Pose", ArmorStandEntity.PackedRotation.CODEC)
				.ifPresent(this::unpackRotation);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void pushAway(Entity entity) {
	}

	@Override
	protected void tickCramming() {
		for (Entity entity : this
				.getEntityWorld()
				.getOtherEntities(this, this.getBoundingBox(), RIDEABLE_MINECART_PREDICATE)) {
			if (this.squaredDistanceTo(entity) <= 0.2) {
				entity.pushAwayFrom(this);
			}
		}
	}

	@Override
	public ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand) {
		ItemStack itemStack = player.getStackInHand(hand);
		if (this.isMarker() || itemStack.isOf(Items.NAME_TAG)) {
			return ActionResult.PASS;
		}
		else if (player.isSpectator()) {
			return ActionResult.SUCCESS;
		}
		else if (player.getEntityWorld().isClient()) {
			return ActionResult.SUCCESS_SERVER;
		}
		else {
			EquipmentSlot equipmentSlot = this.getPreferredEquipmentSlot(itemStack);
			if (itemStack.isEmpty()) {
				EquipmentSlot equipmentSlot2 = this.getSlotFromPosition(hitPos);
				EquipmentSlot equipmentSlot3 = this.isSlotDisabled(equipmentSlot2) ? equipmentSlot : equipmentSlot2;
				if (this.hasStackEquipped(equipmentSlot3) && this.equip(player, equipmentSlot3, itemStack, hand)) {
					return ActionResult.SUCCESS_SERVER;
				}
			}
			else {
				if (this.isSlotDisabled(equipmentSlot)) {
					return ActionResult.FAIL;
				}

				if (equipmentSlot.getType() == EquipmentSlot.Type.HAND && !this.shouldShowArms()) {
					return ActionResult.FAIL;
				}

				if (this.equip(player, equipmentSlot, itemStack, hand)) {
					return ActionResult.SUCCESS_SERVER;
				}
			}

			return ActionResult.PASS;
		}
	}

	private EquipmentSlot getSlotFromPosition(Vec3d hitPos) {
		EquipmentSlot equipmentSlot = EquipmentSlot.MAINHAND;
		boolean bl = this.isSmall();
		double d = hitPos.y / (this.getScale() * this.getScaleFactor());
		EquipmentSlot equipmentSlot2 = EquipmentSlot.FEET;
		if (d >= 0.1 && d < 0.1 + (bl ? 0.8 : 0.45) && this.hasStackEquipped(equipmentSlot2)) {
			equipmentSlot = EquipmentSlot.FEET;
		}
		else if (d >= 0.9 + (bl ? 0.3 : 0.0) && d < 0.9 + (bl ? 1.0 : 0.7)
				&& this.hasStackEquipped(EquipmentSlot.CHEST)) {
			equipmentSlot = EquipmentSlot.CHEST;
		}
		else if (d >= 0.4 && d < 0.4 + (bl ? 1.0 : 0.8) && this.hasStackEquipped(EquipmentSlot.LEGS)) {
			equipmentSlot = EquipmentSlot.LEGS;
		}
		else if (d >= 1.6 && this.hasStackEquipped(EquipmentSlot.HEAD)) {
			equipmentSlot = EquipmentSlot.HEAD;
		}
		else if (!this.hasStackEquipped(EquipmentSlot.MAINHAND) && this.hasStackEquipped(EquipmentSlot.OFFHAND)) {
			equipmentSlot = EquipmentSlot.OFFHAND;
		}

		return equipmentSlot;
	}

	private boolean isSlotDisabled(EquipmentSlot slot) {
		return (this.disabledSlots & 1 << slot.getOffsetIndex(0)) != 0
				|| slot.getType() == EquipmentSlot.Type.HAND && !this.shouldShowArms();
	}

	private boolean equip(PlayerEntity player, EquipmentSlot slot, ItemStack stack, Hand hand) {
		ItemStack itemStack = this.getEquippedStack(slot);
		if (!itemStack.isEmpty() && (this.disabledSlots & 1 << slot.getOffsetIndex(8)) != 0) {
			return false;
		}
		else if (itemStack.isEmpty() && (this.disabledSlots & 1 << slot.getOffsetIndex(16)) != 0) {
			return false;
		}
		else if (player.isInCreativeMode() && itemStack.isEmpty() && !stack.isEmpty()) {
			this.equipStack(slot, stack.copyWithCount(1));
			return true;
		}
		else if (stack.isEmpty() || stack.getCount() <= 1) {
			this.equipStack(slot, stack);
			player.setStackInHand(hand, itemStack);
			return true;
		}
		else if (!itemStack.isEmpty()) {
			return false;
		}
		else {
			this.equipStack(slot, stack.split(1));
			return true;
		}
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (this.isRemoved()) {
			return false;
		}
		else if (!world.getGameRules().getValue(GameRules.DO_MOB_GRIEFING)
				&& source.getAttacker() instanceof MobEntity) {
			return false;
		}
		else if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			this.kill(world);
			return false;
		}
		else if (this.isInvulnerableTo(world, source) || this.invisible || this.isMarker()) {
			return false;
		}
		else if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
			this.onBreak(world, source);
			this.kill(world);
			return false;
		}
		else if (source.isIn(DamageTypeTags.IGNITES_ARMOR_STANDS)) {
			if (this.isOnFire()) {
				this.updateHealth(world, source, 0.15F);
			}
			else {
				this.setOnFireFor(5.0F);
			}

			return false;
		}
		else if (source.isIn(DamageTypeTags.BURNS_ARMOR_STANDS) && this.getHealth() > 0.5F) {
			this.updateHealth(world, source, 4.0F);
			return false;
		}
		else {
			boolean bl = source.isIn(DamageTypeTags.CAN_BREAK_ARMOR_STAND);
			boolean bl2 = source.isIn(DamageTypeTags.ALWAYS_KILLS_ARMOR_STANDS);
			if (!bl && !bl2) {
				return false;
			}
			else if (source.getAttacker() instanceof PlayerEntity playerEntity
					&& !playerEntity.getAbilities().allowModifyWorld) {
				return false;
			}
			else if (source.isSourceCreativePlayer()) {
				this.playBreakSound();
				this.spawnBreakParticles();
				this.kill(world);
				return true;
			}
			else {
				long l = world.getTime();
				if (l - this.lastHitTime > 5L && !bl2) {
					world.sendEntityStatus(this, (byte) 32);
					this.emitGameEvent(GameEvent.ENTITY_DAMAGE, source.getAttacker());
					this.lastHitTime = l;
				}
				else {
					this.breakAndDropItem(world, source);
					this.spawnBreakParticles();
					this.kill(world);
				}

				return true;
			}
		}
	}

	@Override
	public void handleStatus(byte status) {
		if (status == 32) {
			if (this.getEntityWorld().isClient()) {
				this.getEntityWorld()
				    .playSoundClient(
						    this.getX(),
						    this.getY(),
						    this.getZ(),
						    SoundEvents.ENTITY_ARMOR_STAND_HIT,
						    this.getSoundCategory(),
						    0.3F,
						    1.0F,
						    false
				    );
				this.lastHitTime = this.getEntityWorld().getTime();
			}
		}
		else {
			super.handleStatus(status);
		}
	}

	@Override
	public boolean shouldRender(double distance) {
		double d = this.getBoundingBox().getAverageSideLength() * 4.0;
		if (Double.isNaN(d) || d == 0.0) {
			d = 4.0;
		}

		d *= 64.0;
		return distance < d * d;
	}

	private void spawnBreakParticles() {
		if (this.getEntityWorld() instanceof ServerWorld) {
			((ServerWorld) this.getEntityWorld())
					.spawnParticles(
							new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.getDefaultState()),
							this.getX(),
							this.getBodyY(0.6666666666666666),
							this.getZ(),
							10,
							this.getWidth() / 4.0F,
							this.getHeight() / 4.0F,
							this.getWidth() / 4.0F,
							0.05
					);
		}
	}

	private void updateHealth(ServerWorld world, DamageSource damageSource, float amount) {
		float f = this.getHealth();
		f -= amount;
		if (f <= 0.5F) {
			this.onBreak(world, damageSource);
			this.kill(world);
		}
		else {
			this.setHealth(f);
			this.emitGameEvent(GameEvent.ENTITY_DAMAGE, damageSource.getAttacker());
		}
	}

	private void breakAndDropItem(ServerWorld world, DamageSource damageSource) {
		ItemStack itemStack = new ItemStack(Items.ARMOR_STAND);
		itemStack.set(DataComponentTypes.CUSTOM_NAME, this.getCustomName());
		Block.dropStack(this.getEntityWorld(), this.getBlockPos(), itemStack);
		this.onBreak(world, damageSource);
	}

	private void onBreak(ServerWorld world, DamageSource damageSource) {
		this.playBreakSound();
		this.drop(world, damageSource);

		for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
			ItemStack itemStack = this.equipment.put(equipmentSlot, ItemStack.EMPTY);
			if (!itemStack.isEmpty()) {
				Block.dropStack(this.getEntityWorld(), this.getBlockPos().up(), itemStack);
			}
		}
	}

	private void playBreakSound() {
		this
				.getEntityWorld()
				.playSound(
						null,
						this.getX(),
						this.getY(),
						this.getZ(),
						SoundEvents.ENTITY_ARMOR_STAND_BREAK,
						this.getSoundCategory(),
						1.0F,
						1.0F
				);
	}

	@Override
	protected void turnHead(float bodyRotation) {
		this.lastBodyYaw = this.lastYaw;
		this.bodyYaw = this.getYaw();
	}

	@Override
	public void travel(Vec3d movementInput) {
		if (this.canClip()) {
			super.travel(movementInput);
		}
	}

	@Override
	public void setBodyYaw(float bodyYaw) {
		this.lastBodyYaw = this.lastYaw = bodyYaw;
		this.lastHeadYaw = this.headYaw = bodyYaw;
	}

	@Override
	public void setHeadYaw(float headYaw) {
		this.lastBodyYaw = this.lastYaw = headYaw;
		this.lastHeadYaw = this.headYaw = headYaw;
	}

	@Override
	protected void updatePotionVisibility() {
		this.setInvisible(this.invisible);
	}

	@Override
	public void setInvisible(boolean invisible) {
		this.invisible = invisible;
		super.setInvisible(invisible);
	}

	@Override
	public boolean isBaby() {
		return this.isSmall();
	}

	@Override
	public void kill(ServerWorld world) {
		this.remove(Entity.RemovalReason.KILLED);
		this.emitGameEvent(GameEvent.ENTITY_DIE);
	}

	@Override
	public boolean isImmuneToExplosion(Explosion explosion) {
		return explosion.preservesDecorativeEntities() ? this.isInvisible() : true;
	}

	@Override
	public PistonBehavior getPistonBehavior() {
		return this.isMarker() ? PistonBehavior.IGNORE : super.getPistonBehavior();
	}

	@Override
	public boolean canAvoidTraps() {
		return this.isMarker();
	}

	private void setSmall(boolean small) {
		this.dataTracker.set(ARMOR_STAND_FLAGS, this.setBitField(this.dataTracker.get(ARMOR_STAND_FLAGS), 1, small));
	}

	public boolean isSmall() {
		return (this.dataTracker.get(ARMOR_STAND_FLAGS) & 1) != 0;
	}

	public void setShowArms(boolean showArms) {
		this.dataTracker.set(ARMOR_STAND_FLAGS, this.setBitField(this.dataTracker.get(ARMOR_STAND_FLAGS), 4, showArms));
	}

	/**
	 * Определяет, следует ли show arms.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldShowArms() {
		return (this.dataTracker.get(ARMOR_STAND_FLAGS) & 4) != 0;
	}

	public void setHideBasePlate(boolean hideBasePlate) {
		this.dataTracker.set(
				ARMOR_STAND_FLAGS,
				this.setBitField(this.dataTracker.get(ARMOR_STAND_FLAGS), 8, hideBasePlate)
		);
	}

	/**
	 * Определяет, следует ли show base plate.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldShowBasePlate() {
		return (this.dataTracker.get(ARMOR_STAND_FLAGS) & 8) == 0;
	}

	private void setMarker(boolean marker) {
		this.dataTracker.set(ARMOR_STAND_FLAGS, this.setBitField(this.dataTracker.get(ARMOR_STAND_FLAGS), 16, marker));
	}

	public boolean isMarker() {
		return (this.dataTracker.get(ARMOR_STAND_FLAGS) & 16) != 0;
	}

	private byte setBitField(byte value, int bitField, boolean set) {
		if (set) {
			value = (byte) (value | bitField);
		}
		else {
			value = (byte) (value & ~bitField);
		}

		return value;
	}

	public void setHeadRotation(EulerAngle angle) {
		this.dataTracker.set(TRACKER_HEAD_ROTATION, angle);
	}

	public void setBodyRotation(EulerAngle angle) {
		this.dataTracker.set(TRACKER_BODY_ROTATION, angle);
	}

	public void setLeftArmRotation(EulerAngle angle) {
		this.dataTracker.set(TRACKER_LEFT_ARM_ROTATION, angle);
	}

	public void setRightArmRotation(EulerAngle angle) {
		this.dataTracker.set(TRACKER_RIGHT_ARM_ROTATION, angle);
	}

	public void setLeftLegRotation(EulerAngle angle) {
		this.dataTracker.set(TRACKER_LEFT_LEG_ROTATION, angle);
	}

	public void setRightLegRotation(EulerAngle angle) {
		this.dataTracker.set(TRACKER_RIGHT_LEG_ROTATION, angle);
	}

	public EulerAngle getHeadRotation() {
		return this.dataTracker.get(TRACKER_HEAD_ROTATION);
	}

	public EulerAngle getBodyRotation() {
		return this.dataTracker.get(TRACKER_BODY_ROTATION);
	}

	public EulerAngle getLeftArmRotation() {
		return this.dataTracker.get(TRACKER_LEFT_ARM_ROTATION);
	}

	public EulerAngle getRightArmRotation() {
		return this.dataTracker.get(TRACKER_RIGHT_ARM_ROTATION);
	}

	public EulerAngle getLeftLegRotation() {
		return this.dataTracker.get(TRACKER_LEFT_LEG_ROTATION);
	}

	public EulerAngle getRightLegRotation() {
		return this.dataTracker.get(TRACKER_RIGHT_LEG_ROTATION);
	}

	@Override
	public boolean canHit() {
		return super.canHit() && !this.isMarker();
	}

	@Override
	public boolean handleAttack(Entity attacker) {
		return attacker instanceof PlayerEntity playerEntity && !this
				.getEntityWorld()
				.canEntityModifyAt(playerEntity, this.getBlockPos());
	}

	@Override
	public Arm getMainArm() {
		return Arm.RIGHT;
	}

	@Override
	public LivingEntity.FallSounds getFallSounds() {
		return new LivingEntity.FallSounds(SoundEvents.ENTITY_ARMOR_STAND_FALL, SoundEvents.ENTITY_ARMOR_STAND_FALL);
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_ARMOR_STAND_HIT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_ARMOR_STAND_BREAK;
	}

	@Override
	public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
	}

	@Override
	public boolean isAffectedBySplashPotions() {
		return false;
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (ARMOR_STAND_FLAGS.equals(data)) {
			this.calculateDimensions();
			this.intersectionChecked = !this.isMarker();
		}

		super.onTrackedDataSet(data);
	}

	@Override
	public boolean isMobOrPlayer() {
		return false;
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		return this.getDimensions(this.isMarker());
	}

	private EntityDimensions getDimensions(boolean marker) {
		if (marker) {
			return MARKER_DIMENSIONS;
		}
		else {
			return this.isBaby() ? SMALL_DIMENSIONS : this.getType().getDimensions();
		}
	}

	@Override
	public Vec3d getClientCameraPosVec(float tickProgress) {
		if (this.isMarker()) {
			Box box = this.getDimensions(false).getBoxAt(this.getEntityPos());
			BlockPos blockPos = this.getBlockPos();
			int i = Integer.MIN_VALUE;

			for (BlockPos blockPos2 : BlockPos.iterate(
					BlockPos.ofFloored(box.minX, box.minY, box.minZ),
					BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ)
			)) {
				int
						j =
						Math.max(
								this.getEntityWorld().getLightLevel(LightType.BLOCK, blockPos2),
								this.getEntityWorld().getLightLevel(LightType.SKY, blockPos2)
						);
				if (j == 15) {
					return Vec3d.ofCenter(blockPos2);
				}

				if (j > i) {
					i = j;
					blockPos = blockPos2.toImmutable();
				}
			}

			return Vec3d.ofCenter(blockPos);
		}
		else {
			return super.getClientCameraPosVec(tickProgress);
		}
	}

	@Override
	public ItemStack getPickBlockStack() {
		return new ItemStack(Items.ARMOR_STAND);
	}

	@Override
	public boolean isPartOfGame() {
		return !this.isInvisible() && !this.isMarker();
	}

	/**
	 * Unpack rotation.
	 *
	 * @param packedRotation packed rotation
	 */
	public void unpackRotation(ArmorStandEntity.PackedRotation packedRotation) {
		this.setHeadRotation(packedRotation.head());
		this.setBodyRotation(packedRotation.body());
		this.setLeftArmRotation(packedRotation.leftArm());
		this.setRightArmRotation(packedRotation.rightArm());
		this.setLeftLegRotation(packedRotation.leftLeg());
		this.setRightLegRotation(packedRotation.rightLeg());
	}

	public ArmorStandEntity.PackedRotation packRotation() {
		return new ArmorStandEntity.PackedRotation(
				this.getHeadRotation(),
				this.getBodyRotation(),
				this.getLeftArmRotation(),
				this.getRightArmRotation(),
				this.getLeftLegRotation(),
				this.getRightLegRotation()
		);
	}

	/**
	 * {@code PackedRotation}.
	 */
	public record PackedRotation(
			EulerAngle head,
			EulerAngle body,
			EulerAngle leftArm,
			EulerAngle rightArm,
			EulerAngle leftLeg,
			EulerAngle rightLeg
	) {

		public static final ArmorStandEntity.PackedRotation DEFAULT = new ArmorStandEntity.PackedRotation(
				ArmorStandEntity.DEFAULT_HEAD_ROTATION,
				ArmorStandEntity.DEFAULT_BODY_ROTATION,
				ArmorStandEntity.DEFAULT_LEFT_ARM_ROTATION,
				ArmorStandEntity.DEFAULT_RIGHT_ARM_ROTATION,
				ArmorStandEntity.DEFAULT_LEFT_LEG_ROTATION,
				ArmorStandEntity.DEFAULT_RIGHT_LEG_ROTATION
		);
		public static final Codec<ArmorStandEntity.PackedRotation> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    EulerAngle.CODEC
								                    .optionalFieldOf("Head", ArmorStandEntity.DEFAULT_HEAD_ROTATION)
								                    .forGetter(ArmorStandEntity.PackedRotation::head),
						                    EulerAngle.CODEC
								                    .optionalFieldOf("Body", ArmorStandEntity.DEFAULT_BODY_ROTATION)
								                    .forGetter(ArmorStandEntity.PackedRotation::body),
						                    EulerAngle.CODEC
								                    .optionalFieldOf("LeftArm", ArmorStandEntity.DEFAULT_LEFT_ARM_ROTATION)
								                    .forGetter(ArmorStandEntity.PackedRotation::leftArm),
						                    EulerAngle.CODEC
								                    .optionalFieldOf("RightArm", ArmorStandEntity.DEFAULT_RIGHT_ARM_ROTATION)
								                    .forGetter(ArmorStandEntity.PackedRotation::rightArm),
						                    EulerAngle.CODEC
								                    .optionalFieldOf("LeftLeg", ArmorStandEntity.DEFAULT_LEFT_LEG_ROTATION)
								                    .forGetter(ArmorStandEntity.PackedRotation::leftLeg),
						                    EulerAngle.CODEC
								                    .optionalFieldOf("RightLeg", ArmorStandEntity.DEFAULT_RIGHT_LEG_ROTATION)
								                    .forGetter(ArmorStandEntity.PackedRotation::rightLeg)
				                    )
				                    .apply(instance, ArmorStandEntity.PackedRotation::new)
		);
	}
}

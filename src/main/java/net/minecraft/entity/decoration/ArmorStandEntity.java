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
 * Стойка для брони — декоративная живая сущность, способная носить снаряжение.
 * Поддерживает режимы: маленький, маркер (нулевой хитбокс), скрытая подставка, видимые руки.
 * Слоты экипировки могут быть заблокированы побитово через {@code disabledSlots}.
 */
public class ArmorStandEntity extends LivingEntity {

	public static final int PUNCH_DAMAGE = 5;
	public static final EulerAngle DEFAULT_HEAD_ROTATION = new EulerAngle(0.0F, 0.0F, 0.0F);
	public static final EulerAngle DEFAULT_BODY_ROTATION = new EulerAngle(0.0F, 0.0F, 0.0F);
	public static final EulerAngle DEFAULT_LEFT_ARM_ROTATION = new EulerAngle(-10.0F, 0.0F, -10.0F);
	public static final EulerAngle DEFAULT_RIGHT_ARM_ROTATION = new EulerAngle(-15.0F, 0.0F, 10.0F);
	public static final EulerAngle DEFAULT_LEFT_LEG_ROTATION = new EulerAngle(-1.0F, 0.0F, -1.0F);
	public static final EulerAngle DEFAULT_RIGHT_LEG_ROTATION = new EulerAngle(1.0F, 0.0F, 1.0F);

	/** Флаг бита в {@code ARMOR_STAND_FLAGS}: маленький размер. */
	public static final int SMALL_FLAG = 1;
	/** Флаг бита в {@code ARMOR_STAND_FLAGS}: показывать руки. */
	public static final int SHOW_ARMS_FLAG = 4;
	/** Флаг бита в {@code ARMOR_STAND_FLAGS}: скрыть подставку. */
	public static final int HIDE_BASE_PLATE_FLAG = 8;
	/** Флаг бита в {@code ARMOR_STAND_FLAGS}: режим маркера (нулевой хитбокс). */
	public static final int MARKER_FLAG = 16;

	/**
	 * Смещение индекса слота для проверки запрета снятия предмета.
	 * Используется в побитовой маске {@code disabledSlots}.
	 */
	public static final int DISABLE_TAKING_FLAG = 8;
	/**
	 * Смещение индекса слота для проверки запрета надевания предмета.
	 * Используется в побитовой маске {@code disabledSlots}.
	 */
	public static final int DISABLE_PUTTING_FLAG = 16;

	public static final TrackedData<Byte> ARMOR_STAND_FLAGS =
			DataTracker.registerData(ArmorStandEntity.class, TrackedDataHandlerRegistry.BYTE);
	public static final TrackedData<EulerAngle> TRACKER_HEAD_ROTATION =
			DataTracker.registerData(ArmorStandEntity.class, TrackedDataHandlerRegistry.ROTATION);
	public static final TrackedData<EulerAngle> TRACKER_BODY_ROTATION =
			DataTracker.registerData(ArmorStandEntity.class, TrackedDataHandlerRegistry.ROTATION);
	public static final TrackedData<EulerAngle> TRACKER_LEFT_ARM_ROTATION =
			DataTracker.registerData(ArmorStandEntity.class, TrackedDataHandlerRegistry.ROTATION);
	public static final TrackedData<EulerAngle> TRACKER_RIGHT_ARM_ROTATION =
			DataTracker.registerData(ArmorStandEntity.class, TrackedDataHandlerRegistry.ROTATION);
	public static final TrackedData<EulerAngle> TRACKER_LEFT_LEG_ROTATION =
			DataTracker.registerData(ArmorStandEntity.class, TrackedDataHandlerRegistry.ROTATION);
	public static final TrackedData<EulerAngle> TRACKER_RIGHT_LEG_ROTATION =
			DataTracker.registerData(ArmorStandEntity.class, TrackedDataHandlerRegistry.ROTATION);

	private static final EntityDimensions MARKER_DIMENSIONS = EntityDimensions.fixed(0.0F, 0.0F);
	private static final EntityDimensions SMALL_DIMENSIONS =
			EntityType.ARMOR_STAND.getDimensions().scaled(0.5F).withEyeHeight(0.9875F);

	private static final double FEET_SLOT_Y_MIN = 0.1;
	private static final double CHEST_SLOT_Y_MIN = 0.9;
	private static final double LEGS_SLOT_Y_MIN = 0.4;
	private static final double HEAD_SLOT_Y_MIN = 1.6;

	/** Максимальный уровень освещения, при котором прекращается поиск лучшей позиции камеры. */
	private static final int MAX_LIGHT_LEVEL = 15;

	/** Статус-код для клиентского события «удар по стойке» (звук + обновление таймера). */
	private static final byte STATUS_HIT = 32;

	/** Минимальный интервал между ударами (в тиках) до разрушения стойки. */
	private static final long HIT_COOLDOWN_TICKS = 5L;

	private static final Predicate<Entity> RIDEABLE_MINECART_PREDICATE =
			entity -> entity instanceof AbstractMinecartEntity cart && cart.isRideable();

	private boolean invisible = false;
	public long lastHitTime;
	private int disabledSlots = 0;

	public ArmorStandEntity(EntityType<? extends ArmorStandEntity> entityType, World world) {
		super(entityType, world);
	}

	public ArmorStandEntity(World world, double x, double y, double z) {
		this(EntityType.ARMOR_STAND, world);
		setPosition(x, y, z);
	}

	public static DefaultAttributeContainer.Builder createArmorStandAttributes() {
		return createLivingAttributes().add(EntityAttributes.STEP_HEIGHT, 0.0);
	}

	@Override
	public void calculateDimensions() {
		double x = getX();
		double y = getY();
		double z = getZ();
		super.calculateDimensions();
		setPosition(x, y, z);
	}

	private boolean canClip() {
		return !isMarker() && !hasNoGravity();
	}

	@Override
	public boolean canActVoluntarily() {
		return super.canActVoluntarily() && canClip();
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
		return slot != EquipmentSlot.BODY && slot != EquipmentSlot.SADDLE && !isSlotDisabled(slot);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("Invisible", isInvisible());
		view.putBoolean("Small", isSmall());
		view.putBoolean("ShowArms", shouldShowArms());
		view.putInt("DisabledSlots", disabledSlots);
		view.putBoolean("NoBasePlate", !shouldShowBasePlate());
		if (isMarker()) {
			view.putBoolean("Marker", true);
		}

		view.put("Pose", ArmorStandEntity.PackedRotation.CODEC, packRotation());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setInvisible(view.getBoolean("Invisible", false));
		setSmall(view.getBoolean("Small", false));
		setShowArms(view.getBoolean("ShowArms", false));
		disabledSlots = view.getInt("DisabledSlots", 0);
		setHideBasePlate(view.getBoolean("NoBasePlate", false));
		setMarker(view.getBoolean("Marker", false));
		noClip = !canClip();
		view.<ArmorStandEntity.PackedRotation>read("Pose", ArmorStandEntity.PackedRotation.CODEC)
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
		for (Entity entity : getEntityWorld().getOtherEntities(this, getBoundingBox(), RIDEABLE_MINECART_PREDICATE)) {
			if (squaredDistanceTo(entity) <= 0.2) {
				entity.pushAwayFrom(this);
			}
		}
	}

	/**
	 * Обрабатывает взаимодействие игрока со стойкой: надевание/снятие снаряжения.
	 * Определяет целевой слот по позиции клика {@code hitPos} или по типу предмета в руке.
	 */
	@Override
	public ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand) {
		ItemStack itemStack = player.getStackInHand(hand);
		if (isMarker() || itemStack.isOf(Items.NAME_TAG)) {
			return ActionResult.PASS;
		}

		if (player.isSpectator()) {
			return ActionResult.SUCCESS;
		}

		if (player.getEntityWorld().isClient()) {
			return ActionResult.SUCCESS_SERVER;
		}

		EquipmentSlot preferredSlot = getPreferredEquipmentSlot(itemStack);
		if (itemStack.isEmpty()) {
			EquipmentSlot positionSlot = getSlotFromPosition(hitPos);
			EquipmentSlot targetSlot = isSlotDisabled(positionSlot) ? preferredSlot : positionSlot;
			if (hasStackEquipped(targetSlot) && equip(player, targetSlot, itemStack, hand)) {
				return ActionResult.SUCCESS_SERVER;
			}
		} else {
			if (isSlotDisabled(preferredSlot)) {
				return ActionResult.FAIL;
			}

			if (preferredSlot.getType() == EquipmentSlot.Type.HAND && !shouldShowArms()) {
				return ActionResult.FAIL;
			}

			if (equip(player, preferredSlot, itemStack, hand)) {
				return ActionResult.SUCCESS_SERVER;
			}
		}

		return ActionResult.PASS;
	}

	/**
	 * Определяет слот экипировки по вертикальной позиции клика относительно высоты стойки.
	 * Учитывает масштаб и режим «маленькой» стойки.
	 */
	private EquipmentSlot getSlotFromPosition(Vec3d hitPos) {
		EquipmentSlot result = EquipmentSlot.MAINHAND;
		boolean small = isSmall();
		double relativeY = hitPos.y / (getScale() * getScaleFactor());

		if (relativeY >= FEET_SLOT_Y_MIN
				&& relativeY < 0.1 + (small ? 0.8 : 0.45)
				&& hasStackEquipped(EquipmentSlot.FEET)
		) {
			result = EquipmentSlot.FEET;
		} else if (relativeY >= CHEST_SLOT_Y_MIN + (small ? 0.3 : 0.0)
				&& relativeY < 0.9 + (small ? 1.0 : 0.7)
				&& hasStackEquipped(EquipmentSlot.CHEST)
		) {
			result = EquipmentSlot.CHEST;
		} else if (relativeY >= LEGS_SLOT_Y_MIN
				&& relativeY < 0.4 + (small ? 1.0 : 0.8)
				&& hasStackEquipped(EquipmentSlot.LEGS)
		) {
			result = EquipmentSlot.LEGS;
		} else if (relativeY >= HEAD_SLOT_Y_MIN && hasStackEquipped(EquipmentSlot.HEAD)) {
			result = EquipmentSlot.HEAD;
		} else if (!hasStackEquipped(EquipmentSlot.MAINHAND) && hasStackEquipped(EquipmentSlot.OFFHAND)) {
			result = EquipmentSlot.OFFHAND;
		}

		return result;
	}

	private boolean isSlotDisabled(EquipmentSlot slot) {
		return (disabledSlots & 1 << slot.getOffsetIndex(0)) != 0
				|| slot.getType() == EquipmentSlot.Type.HAND && !shouldShowArms();
	}

	/**
	 * Выполняет обмен предметом между рукой игрока и слотом стойки.
	 * Учитывает флаги запрета снятия/надевания и режим творчества.
	 */
	private boolean equip(PlayerEntity player, EquipmentSlot slot, ItemStack stack, Hand hand) {
		ItemStack currentStack = getEquippedStack(slot);
		if (!currentStack.isEmpty() && (disabledSlots & 1 << slot.getOffsetIndex(DISABLE_TAKING_FLAG)) != 0) {
			return false;
		}

		if (currentStack.isEmpty() && (disabledSlots & 1 << slot.getOffsetIndex(DISABLE_PUTTING_FLAG)) != 0) {
			return false;
		}

		if (player.isInCreativeMode() && currentStack.isEmpty() && !stack.isEmpty()) {
			equipStack(slot, stack.copyWithCount(1));
			return true;
		}

		if (stack.isEmpty() || stack.getCount() <= 1) {
			equipStack(slot, stack);
			player.setStackInHand(hand, currentStack);
			return true;
		}

		if (!currentStack.isEmpty()) {
			return false;
		}

		equipStack(slot, stack.split(1));
		return true;
	}

	/**
	 * Обрабатывает урон по стойке. Логика зависит от типа урона:
	 * взрывы — мгновенное уничтожение, огонь — поджог или урон здоровью,
	 * обычный удар — двойной клик для разрушения с дропом предмета стойки.
	 */
	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (isRemoved()) {
			return false;
		}

		if (!world.getGameRules().getValue(GameRules.DO_MOB_GRIEFING)
				&& source.getAttacker() instanceof MobEntity) {
			return false;
		}

		if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			kill(world);
			return false;
		}

		if (isInvulnerableTo(world, source) || invisible || isMarker()) {
			return false;
		}

		if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
			onBreak(world, source);
			kill(world);
			return false;
		}

		if (source.isIn(DamageTypeTags.IGNITES_ARMOR_STANDS)) {
			if (isOnFire()) {
				updateHealth(world, source, 0.15F);
			} else {
				setOnFireFor(5.0F);
			}

			return false;
		}

		if (source.isIn(DamageTypeTags.BURNS_ARMOR_STANDS) && getHealth() > 0.5F) {
			updateHealth(world, source, 4.0F);
			return false;
		}

		boolean canBreak = source.isIn(DamageTypeTags.CAN_BREAK_ARMOR_STAND);
		boolean alwaysKills = source.isIn(DamageTypeTags.ALWAYS_KILLS_ARMOR_STANDS);
		if (!canBreak && !alwaysKills) {
			return false;
		}

		if (source.getAttacker() instanceof PlayerEntity playerEntity
				&& !playerEntity.getAbilities().allowModifyWorld
		) {
			return false;
		}

		if (source.isSourceCreativePlayer()) {
			playBreakSound();
			spawnBreakParticles();
			kill(world);
			return true;
		}

		long currentTime = world.getTime();
		if (currentTime - lastHitTime > HIT_COOLDOWN_TICKS && !alwaysKills) {
			world.sendEntityStatus(this, STATUS_HIT);
			emitGameEvent(GameEvent.ENTITY_DAMAGE, source.getAttacker());
			lastHitTime = currentTime;
		} else {
			breakAndDropItem(world, source);
			spawnBreakParticles();
			kill(world);
		}

		return true;
	}

	@Override
	public void handleStatus(byte status) {
		if (status == STATUS_HIT) {
			if (getEntityWorld().isClient()) {
				getEntityWorld().playSoundClient(
						getX(), getY(), getZ(),
						SoundEvents.ENTITY_ARMOR_STAND_HIT,
						getSoundCategory(),
						0.3F, 1.0F, false
				);
				lastHitTime = getEntityWorld().getTime();
			}

			return;
		}

		super.handleStatus(status);
	}

	@Override
	public boolean shouldRender(double distance) {
		double sideLength = getBoundingBox().getAverageSideLength() * 4.0;
		if (Double.isNaN(sideLength) || sideLength == 0.0) {
			sideLength = 4.0;
		}

		sideLength *= 64.0;
		return distance < sideLength * sideLength;
	}

	private void spawnBreakParticles() {
		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			serverWorld.spawnParticles(
					new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.getDefaultState()),
					getX(),
					getBodyY(0.6666666666666666),
					getZ(),
					10,
					getWidth() / 4.0F,
					getHeight() / 4.0F,
					getWidth() / 4.0F,
					0.05
			);
		}
	}

	private void updateHealth(ServerWorld world, DamageSource damageSource, float amount) {
		float newHealth = getHealth() - amount;
		if (newHealth <= 0.5F) {
			onBreak(world, damageSource);
			kill(world);
			return;
		}

		setHealth(newHealth);
		emitGameEvent(GameEvent.ENTITY_DAMAGE, damageSource.getAttacker());
	}

	private void breakAndDropItem(ServerWorld world, DamageSource damageSource) {
		ItemStack itemStack = new ItemStack(Items.ARMOR_STAND);
		itemStack.set(DataComponentTypes.CUSTOM_NAME, getCustomName());
		Block.dropStack(getEntityWorld(), getBlockPos(), itemStack);
		onBreak(world, damageSource);
	}

	private void onBreak(ServerWorld world, DamageSource damageSource) {
		playBreakSound();
		drop(world, damageSource);

		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			ItemStack itemStack = equipment.put(slot, ItemStack.EMPTY);
			if (!itemStack.isEmpty()) {
				Block.dropStack(getEntityWorld(), getBlockPos().up(), itemStack);
			}
		}
	}

	private void playBreakSound() {
		getEntityWorld().playSound(
				null, getX(), getY(), getZ(),
				SoundEvents.ENTITY_ARMOR_STAND_BREAK,
				getSoundCategory(),
				1.0F, 1.0F
		);
	}

	@Override
	protected void turnHead(float bodyRotation) {
		lastBodyYaw = lastYaw;
		bodyYaw = getYaw();
	}

	@Override
	public void travel(Vec3d movementInput) {
		if (canClip()) {
			super.travel(movementInput);
		}
	}

	@Override
	public void setBodyYaw(float bodyYaw) {
		lastBodyYaw = lastYaw = bodyYaw;
		lastHeadYaw = headYaw = bodyYaw;
	}

	@Override
	public void setHeadYaw(float headYaw) {
		lastBodyYaw = lastYaw = headYaw;
		lastHeadYaw = this.headYaw = headYaw;
	}

	@Override
	protected void updatePotionVisibility() {
		setInvisible(invisible);
	}

	@Override
	public void setInvisible(boolean invisible) {
		this.invisible = invisible;
		super.setInvisible(invisible);
	}

	@Override
	public boolean isBaby() {
		return isSmall();
	}

	@Override
	public void kill(ServerWorld world) {
		remove(Entity.RemovalReason.KILLED);
		emitGameEvent(GameEvent.ENTITY_DIE);
	}

	@Override
	public boolean isImmuneToExplosion(Explosion explosion) {
		return explosion.preservesDecorativeEntities() ? isInvisible() : true;
	}

	@Override
	public PistonBehavior getPistonBehavior() {
		return isMarker() ? PistonBehavior.IGNORE : super.getPistonBehavior();
	}

	@Override
	public boolean canAvoidTraps() {
		return isMarker();
	}

	private void setSmall(boolean small) {
		dataTracker.set(ARMOR_STAND_FLAGS, setBitField(dataTracker.get(ARMOR_STAND_FLAGS), SMALL_FLAG, small));
	}

	public boolean isSmall() {
		return (dataTracker.get(ARMOR_STAND_FLAGS) & SMALL_FLAG) != 0;
	}

	public void setShowArms(boolean showArms) {
		dataTracker.set(ARMOR_STAND_FLAGS, setBitField(dataTracker.get(ARMOR_STAND_FLAGS), SHOW_ARMS_FLAG, showArms));
	}

	public boolean shouldShowArms() {
		return (dataTracker.get(ARMOR_STAND_FLAGS) & SHOW_ARMS_FLAG) != 0;
	}

	public void setHideBasePlate(boolean hideBasePlate) {
		dataTracker.set(
				ARMOR_STAND_FLAGS,
				setBitField(dataTracker.get(ARMOR_STAND_FLAGS), HIDE_BASE_PLATE_FLAG, hideBasePlate)
		);
	}

	public boolean shouldShowBasePlate() {
		return (dataTracker.get(ARMOR_STAND_FLAGS) & HIDE_BASE_PLATE_FLAG) == 0;
	}

	private void setMarker(boolean marker) {
		dataTracker.set(ARMOR_STAND_FLAGS, setBitField(dataTracker.get(ARMOR_STAND_FLAGS), MARKER_FLAG, marker));
	}

	public boolean isMarker() {
		return (dataTracker.get(ARMOR_STAND_FLAGS) & MARKER_FLAG) != 0;
	}

	private byte setBitField(byte value, int bitField, boolean set) {
		return set
				? (byte) (value | bitField)
				: (byte) (value & ~bitField);
	}

	public void setHeadRotation(EulerAngle angle) {
		dataTracker.set(TRACKER_HEAD_ROTATION, angle);
	}

	public void setBodyRotation(EulerAngle angle) {
		dataTracker.set(TRACKER_BODY_ROTATION, angle);
	}

	public void setLeftArmRotation(EulerAngle angle) {
		dataTracker.set(TRACKER_LEFT_ARM_ROTATION, angle);
	}

	public void setRightArmRotation(EulerAngle angle) {
		dataTracker.set(TRACKER_RIGHT_ARM_ROTATION, angle);
	}

	public void setLeftLegRotation(EulerAngle angle) {
		dataTracker.set(TRACKER_LEFT_LEG_ROTATION, angle);
	}

	public void setRightLegRotation(EulerAngle angle) {
		dataTracker.set(TRACKER_RIGHT_LEG_ROTATION, angle);
	}

	public EulerAngle getHeadRotation() {
		return dataTracker.get(TRACKER_HEAD_ROTATION);
	}

	public EulerAngle getBodyRotation() {
		return dataTracker.get(TRACKER_BODY_ROTATION);
	}

	public EulerAngle getLeftArmRotation() {
		return dataTracker.get(TRACKER_LEFT_ARM_ROTATION);
	}

	public EulerAngle getRightArmRotation() {
		return dataTracker.get(TRACKER_RIGHT_ARM_ROTATION);
	}

	public EulerAngle getLeftLegRotation() {
		return dataTracker.get(TRACKER_LEFT_LEG_ROTATION);
	}

	public EulerAngle getRightLegRotation() {
		return dataTracker.get(TRACKER_RIGHT_LEG_ROTATION);
	}

	@Override
	public boolean canHit() {
		return super.canHit() && !isMarker();
	}

	@Override
	public boolean handleAttack(Entity attacker) {
		return attacker instanceof PlayerEntity playerEntity
				&& !getEntityWorld().canEntityModifyAt(playerEntity, getBlockPos());
	}

	@Override
	public Arm getMainArm() {
		return Arm.RIGHT;
	}

	@Override
	public LivingEntity.FallSounds getFallSounds() {
		return new LivingEntity.FallSounds(
				SoundEvents.ENTITY_ARMOR_STAND_FALL,
				SoundEvents.ENTITY_ARMOR_STAND_FALL
		);
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
			calculateDimensions();
			intersectionChecked = !isMarker();
		}

		super.onTrackedDataSet(data);
	}

	@Override
	public boolean isMobOrPlayer() {
		return false;
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		return getDimensions(isMarker());
	}

	private EntityDimensions getDimensions(boolean marker) {
		if (marker) {
			return MARKER_DIMENSIONS;
		}

		return isBaby() ? SMALL_DIMENSIONS : getType().getDimensions();
	}

	/**
	 * Для маркер-стоек ищет позицию с максимальным уровнем освещения в пределах
	 * реального (немаркерного) хитбокса, чтобы камера не застревала в блоке.
	 */
	@Override
	public Vec3d getClientCameraPosVec(float tickProgress) {
		if (!isMarker()) {
			return super.getClientCameraPosVec(tickProgress);
		}

		Box box = getDimensions(false).getBoxAt(getEntityPos());
		BlockPos bestPos = getBlockPos();
		int maxLight = Integer.MIN_VALUE;

		for (BlockPos candidate : BlockPos.iterate(
				BlockPos.ofFloored(box.minX, box.minY, box.minZ),
				BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ)
		)) {
			int lightLevel = Math.max(
					getEntityWorld().getLightLevel(LightType.BLOCK, candidate),
					getEntityWorld().getLightLevel(LightType.SKY, candidate)
			);
			if (lightLevel == MAX_LIGHT_LEVEL) {
				return Vec3d.ofCenter(candidate);
			}

			if (lightLevel > maxLight) {
				maxLight = lightLevel;
				bestPos = candidate.toImmutable();
			}
		}

		return Vec3d.ofCenter(bestPos);
	}

	@Override
	public ItemStack getPickBlockStack() {
		return new ItemStack(Items.ARMOR_STAND);
	}

	@Override
	public boolean isPartOfGame() {
		return !isInvisible() && !isMarker();
	}

	public void unpackRotation(ArmorStandEntity.PackedRotation packedRotation) {
		setHeadRotation(packedRotation.head());
		setBodyRotation(packedRotation.body());
		setLeftArmRotation(packedRotation.leftArm());
		setRightArmRotation(packedRotation.rightArm());
		setLeftLegRotation(packedRotation.leftLeg());
		setRightLegRotation(packedRotation.rightLeg());
	}

	public ArmorStandEntity.PackedRotation packRotation() {
		return new ArmorStandEntity.PackedRotation(
				getHeadRotation(),
				getBodyRotation(),
				getLeftArmRotation(),
				getRightArmRotation(),
				getLeftLegRotation(),
				getRightLegRotation()
		);
	}

	/**
		* Упакованные углы поворота всех частей тела стойки для сериализации и сетевой передачи.
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

package net.minecraft.entity.decoration;

import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Рамка для предметов — декоративная сущность, прикреплённая к блоку.
 * Хранит один предмет и его угол поворота (0–7, шаг 45°).
 * Поддерживает режим «фиксированной» рамки, которую нельзя сломать обычными способами.
 * Выдаёт сигнал компаратору: {@code rotation % 8 + 1} (или 0 если пусто).
 */
public class ItemFrameEntity extends AbstractDecorationEntity {

	private static final TrackedData<ItemStack> ITEM_STACK =
			DataTracker.registerData(ItemFrameEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
	private static final TrackedData<Integer> ROTATION =
			DataTracker.registerData(ItemFrameEntity.class, TrackedDataHandlerRegistry.INTEGER);

	public static final int ROTATION_COUNT = 8;

	private static final float FRAME_THICKNESS = 0.0625F;
	private static final float FRAME_WIDTH = 0.75F;
	/** Смещение центра рамки от поверхности блока (15/32 блока). */
	private static final float FRAME_OFFSET = 0.46875F;

	private float itemDropChance = 1.0F;
	private boolean fixed = false;

	public ItemFrameEntity(EntityType<? extends ItemFrameEntity> entityType, World world) {
		super(entityType, world);
		setInvisible(false);
	}

	public ItemFrameEntity(World world, BlockPos pos, Direction facing) {
		this(EntityType.ITEM_FRAME, world, pos, facing);
	}

	public ItemFrameEntity(EntityType<? extends ItemFrameEntity> type, World world, BlockPos pos, Direction facing) {
		super(type, world, pos);
		setFacing(facing);
		setInvisible(false);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(ITEM_STACK, ItemStack.EMPTY);
		builder.add(ROTATION, 0);
	}

	/**
	 * Переопределяет установку направления для поддержки вертикальных поверхностей (пол/потолок).
	 * Для горизонтальных направлений — стандартный yaw, для вертикальных — pitch ±90°.
	 */
	@Override
	protected void setFacing(Direction facing) {
		Objects.requireNonNull(facing);
		super.setFacingInternal(facing);
		if (facing.getAxis().isHorizontal()) {
			setPitch(0.0F);
			setYaw(facing.getHorizontalQuarterTurns() * 90);
		} else {
			setPitch(-90 * facing.getDirection().offset());
			setYaw(0.0F);
		}

		lastPitch = getPitch();
		lastYaw = getYaw();
		updateAttachmentPosition();
	}

	@Override
	protected final void updateAttachmentPosition() {
		super.updateAttachmentPosition();
		updateTrackedPosition(getX(), getY(), getZ());
	}

	@Override
	protected Box calculateBoundingBox(BlockPos pos, Direction side) {
		return computeBoundingBox(pos, side, containsMap());
	}

	@Override
	protected Box getCheckBoundingBox() {
		return computeBoundingBox(attachedBlockPos, getHorizontalFacing(), false);
	}

	/**
	 * Вычисляет AABB рамки. Если внутри карта — размер 1×1, иначе 0.75×0.75.
	 * Толщина по оси прикрепления всегда {@value #FRAME_THICKNESS} блока.
	 */
	private Box computeBoundingBox(BlockPos blockPos, Direction direction, boolean containsMap) {
		Vec3d center = Vec3d.ofCenter(blockPos).offset(direction, -FRAME_OFFSET);
		float sideSize = containsMap ? 1.0F : FRAME_WIDTH;
		Direction.Axis axis = direction.getAxis();
		double sizeX = axis == Direction.Axis.X ? FRAME_THICKNESS : sideSize;
		double sizeY = axis == Direction.Axis.Y ? FRAME_THICKNESS : sideSize;
		double sizeZ = axis == Direction.Axis.Z ? FRAME_THICKNESS : sideSize;
		return Box.of(center, sizeX, sizeY, sizeZ);
	}

	@Override
	public boolean canStayAttached() {
		if (fixed) {
			return true;
		}

		if (isSpaceBlocked(getCheckBoundingBox())) {
			return false;
		}

		BlockState blockState = getEntityWorld()
				.getBlockState(attachedBlockPos.offset(getHorizontalFacing().getOpposite()));
		return blockState.isSolid()
				|| getHorizontalFacing().getAxis().isHorizontal()
				&& AbstractRedstoneGateBlock.isRedstoneGate(blockState)
			? hasNoIntersectingDecoration(true)
			: false;
	}

	@Override
	public void move(MovementType type, Vec3d movement) {
		if (!fixed) {
			super.move(type, movement);
		}
	}

	@Override
	public void addVelocity(double deltaX, double deltaY, double deltaZ) {
		if (!fixed) {
			super.addVelocity(deltaX, deltaY, deltaZ);
		}
	}

	@Override
	public void kill(ServerWorld world) {
		removeFromFrame(getHeldItemStack());
		super.kill(world);
	}

	private boolean shouldDropHeldStackWhenDamaged(DamageSource damageSource) {
		return !damageSource.isIn(DamageTypeTags.IS_EXPLOSION) && !getHeldItemStack().isEmpty();
	}

	private static boolean canDamageWhenFixed(DamageSource damageSource) {
		return damageSource.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)
				|| damageSource.isSourceCreativePlayer();
	}

	@Override
	public boolean clientDamage(DamageSource source) {
		return fixed && !canDamageWhenFixed(source) ? false : !isAlwaysInvulnerableTo(source);
	}

	/**
	 * Обрабатывает урон по рамке. Если рамка не фиксирована и в ней есть предмет —
	 * сначала выбрасывает предмет без разрушения самой рамки.
	 */
	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (fixed) {
			return canDamageWhenFixed(source) && super.damage(world, source, amount);
		}

		if (isAlwaysInvulnerableTo(source)) {
			return false;
		}

		if (shouldDropHeldStackWhenDamaged(source)) {
			dropHeldStack(world, source.getAttacker(), false);
			emitGameEvent(GameEvent.BLOCK_CHANGE, source.getAttacker());
			playSound(getRemoveItemSound(), 1.0F, 1.0F);
			return true;
		}

		return super.damage(world, source, amount);
	}

	public SoundEvent getRemoveItemSound() {
		return SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM;
	}

	@Override
	public boolean shouldRender(double distance) {
		double renderDistance = 16.0 * 64.0 * getRenderDistanceMultiplier();
		return distance < renderDistance * renderDistance;
	}

	@Override
	public void onBreak(ServerWorld world, @Nullable Entity breaker) {
		playSound(getBreakSound(), 1.0F, 1.0F);
		dropHeldStack(world, breaker, true);
		emitGameEvent(GameEvent.BLOCK_CHANGE, breaker);
	}

	public SoundEvent getBreakSound() {
		return SoundEvents.ENTITY_ITEM_FRAME_BREAK;
	}

	@Override
	public void onPlace() {
		playSound(getPlaceSound(), 1.0F, 1.0F);
	}

	public SoundEvent getPlaceSound() {
		return SoundEvents.ENTITY_ITEM_FRAME_PLACE;
	}

	/**
	 * Выбрасывает содержимое рамки с учётом правил мира и режима игрока.
	 * В режиме творчества или при отключённых дропах — только очищает карту от метки.
	 *
	 * @param dropSelf если {@code true} — также дропает саму рамку как предмет
	 */
	private void dropHeldStack(ServerWorld world, @Nullable Entity entity, boolean dropSelf) {
		if (fixed) {
			return;
		}

		ItemStack itemStack = getHeldItemStack();
		setHeldItemStack(ItemStack.EMPTY);
		if (!world.getGameRules().getValue(GameRules.ENTITY_DROPS)) {
			if (entity == null) {
				removeFromFrame(itemStack);
			}

			return;
		}

		if (entity instanceof PlayerEntity playerEntity && playerEntity.isInCreativeMode()) {
			removeFromFrame(itemStack);
			return;
		}

		if (dropSelf) {
			dropStack(world, getAsItemStack());
		}

		if (!itemStack.isEmpty()) {
			itemStack = itemStack.copy();
			removeFromFrame(itemStack);
			if (random.nextFloat() < itemDropChance) {
				dropStack(world, itemStack);
			}
		}
	}

	private void removeFromFrame(ItemStack stack) {
		MapIdComponent mapId = getMapId(stack);
		if (mapId != null) {
			MapState mapState = FilledMapItem.getMapState(mapId, getEntityWorld());
			if (mapState != null) {
				mapState.removeFrame(attachedBlockPos, getId());
			}
		}

		stack.setHolder(null);
	}

	public ItemStack getHeldItemStack() {
		return getDataTracker().get(ITEM_STACK);
	}

	public @Nullable MapIdComponent getMapId(ItemStack stack) {
		return stack.get(DataComponentTypes.MAP_ID);
	}

	public boolean containsMap() {
		return getHeldItemStack().contains(DataComponentTypes.MAP_ID);
	}

	public void setHeldItemStack(ItemStack stack) {
		setHeldItemStack(stack, true);
	}

	public void setHeldItemStack(ItemStack value, boolean update) {
		if (!value.isEmpty()) {
			value = value.copyWithCount(1);
		}

		setAsStackHolder(value);
		getDataTracker().set(ITEM_STACK, value);
		if (!value.isEmpty()) {
			playSound(getAddItemSound(), 1.0F, 1.0F);
		}

		if (update && attachedBlockPos != null) {
			getEntityWorld().updateComparators(attachedBlockPos, Blocks.AIR);
		}
	}

	public SoundEvent getAddItemSound() {
		return SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM;
	}

	@Override
	public @Nullable StackReference getStackReference(int slot) {
		return slot == 0
				? StackReference.of(this::getHeldItemStack, this::setHeldItemStack)
				: super.getStackReference(slot);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (data.equals(ITEM_STACK)) {
			setAsStackHolder(getHeldItemStack());
		}
	}

	private void setAsStackHolder(ItemStack stack) {
		if (!stack.isEmpty() && stack.getFrame() != this) {
			stack.setHolder(this);
		}

		updateAttachmentPosition();
	}

	public int getRotation() {
		return getDataTracker().get(ROTATION);
	}

	public void setRotation(int value) {
		setRotation(value, true);
	}

	private void setRotation(int value, boolean updateComparators) {
		getDataTracker().set(ROTATION, value % ROTATION_COUNT);
		if (updateComparators && attachedBlockPos != null) {
			getEntityWorld().updateComparators(attachedBlockPos, Blocks.AIR);
		}
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		ItemStack itemStack = getHeldItemStack();
		if (!itemStack.isEmpty()) {
			view.put("Item", ItemStack.CODEC, itemStack);
		}

		view.putByte("ItemRotation", (byte) getRotation());
		view.putFloat("ItemDropChance", itemDropChance);
		view.put("Facing", Direction.INDEX_CODEC, getHorizontalFacing());
		view.putBoolean("Invisible", isInvisible());
		view.putBoolean("Fixed", fixed);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		ItemStack newStack = view.<ItemStack>read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		ItemStack currentStack = getHeldItemStack();
		if (!currentStack.isEmpty() && !ItemStack.areEqual(newStack, currentStack)) {
			removeFromFrame(currentStack);
		}

		setHeldItemStack(newStack, false);
		setRotation(view.getByte("ItemRotation", (byte) 0), false);
		itemDropChance = view.getFloat("ItemDropChance", 1.0F);
		setFacing(view.<Direction>read("Facing", Direction.INDEX_CODEC).orElse(Direction.DOWN));
		setInvisible(view.getBoolean("Invisible", false));
		fixed = view.getBoolean("Fixed", false);
	}

	/**
	 * Обрабатывает взаимодействие игрока с рамкой.
	 * Если рамка пуста — вставляет предмет из руки.
	 * Если рамка занята — поворачивает предмет на 45°.
	 * Карты с более чем 256 декорациями не принимаются.
	 */
	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		ItemStack itemStack = player.getStackInHand(hand);
		boolean hasItem = !getHeldItemStack().isEmpty();
		boolean handHasItem = !itemStack.isEmpty();
		if (fixed) {
			return ActionResult.PASS;
		}

		if (player.getEntityWorld().isClient()) {
			return !hasItem && !handHasItem ? ActionResult.PASS : ActionResult.SUCCESS;
		}

		if (hasItem) {
			playSound(getRotateItemSound(), 1.0F, 1.0F);
			setRotation(getRotation() + 1);
			emitGameEvent(GameEvent.BLOCK_CHANGE, player);
			return ActionResult.SUCCESS;
		}

		if (!handHasItem || isRemoved()) {
			return ActionResult.PASS;
		}

		MapState mapState = FilledMapItem.getMapState(itemStack, getEntityWorld());
		if (mapState != null && mapState.decorationCountNotLessThan(256)) {
			return ActionResult.FAIL;
		}

		setHeldItemStack(itemStack);
		emitGameEvent(GameEvent.BLOCK_CHANGE, player);
		itemStack.decrementUnlessCreative(1, player);
		return ActionResult.SUCCESS;
	}

	public SoundEvent getRotateItemSound() {
		return SoundEvents.ENTITY_ITEM_FRAME_ROTATE_ITEM;
	}

	/** Возвращает мощность сигнала компаратора: 0 если пусто, иначе {@code rotation % 8 + 1}. */
	public int getComparatorPower() {
		return getHeldItemStack().isEmpty() ? 0 : getRotation() % ROTATION_COUNT + 1;
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
		return new EntitySpawnS2CPacket(this, getHorizontalFacing().getIndex(), getAttachedBlockPos());
	}

	@Override
	public void onSpawnPacket(EntitySpawnS2CPacket packet) {
		super.onSpawnPacket(packet);
		setFacing(Direction.byIndex(packet.getEntityData()));
	}

	@Override
	public ItemStack getPickBlockStack() {
		ItemStack itemStack = getHeldItemStack();
		return itemStack.isEmpty() ? getAsItemStack() : itemStack.copy();
	}

	protected ItemStack getAsItemStack() {
		return new ItemStack(Items.ITEM_FRAME);
	}

	@Override
	public float getBodyYaw() {
		Direction direction = getHorizontalFacing();
		int verticalOffset = direction.getAxis().isVertical()
				? 90 * direction.getDirection().offset()
				: 0;
		return MathHelper.wrapDegrees(
				180 + direction.getHorizontalQuarterTurns() * 90 + getRotation() * 45 + verticalOffset
		);
	}
}

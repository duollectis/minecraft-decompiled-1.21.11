package net.minecraft.block.entity;

import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Блок-сущность воронки. Управляет инвентарём, логикой всасывания предметов
 * из мира и вставки в соседние инвентари.
 */
public class HopperBlockEntity extends LootableContainerBlockEntity implements Hopper {

	public static final int TRANSFER_COOLDOWN = 8;
	public static final int INVENTORY_SIZE = 5;

	private static final int[][] AVAILABLE_SLOTS_CACHE = new int[54][];
	private static final int DEFAULT_TRANSFER_COOLDOWN = -1;
	private static final Text CONTAINER_NAME_TEXT = Text.translatable("container.hopper");

	private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(5, ItemStack.EMPTY);
	private int transferCooldown = -1;
	private long lastTickTime;
	private Direction facing;

	/**
	 * @param pos   позиция блока в мире
	 * @param state состояние блока воронки
	 */
	public HopperBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.HOPPER, pos, state);
		this.facing = state.get(HopperBlock.FACING);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);

		if (!this.readLootTable(view)) {
			Inventories.readData(view, this.inventory);
		}

		this.transferCooldown = view.getInt("TransferCooldown", -1);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);

		if (!this.writeLootTable(view)) {
			Inventories.writeData(view, this.inventory);
		}

		view.putInt("TransferCooldown", this.transferCooldown);
	}

	@Override
	public int size() {
		return this.inventory.size();
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		this.generateLoot(null);
		return Inventories.splitStack(this.getHeldStacks(), slot, amount);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		this.generateLoot(null);
		this.getHeldStacks().set(slot, stack);
		stack.capCount(this.getMaxCount(stack));
	}

	@Override
	public void setCachedState(BlockState state) {
		super.setCachedState(state);
		this.facing = state.get(HopperBlock.FACING);
	}

	@Override
	protected Text getContainerName() {
		return CONTAINER_NAME_TEXT;
	}

	/**
	 * Серверный тик воронки: уменьшает кулдаун и запускает перенос предметов.
	 *
	 * @param world       мир
	 * @param pos         позиция воронки
	 * @param state       состояние блока
	 * @param blockEntity блок-сущность воронки
	 */
	public static void serverTick(World world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity) {
		blockEntity.transferCooldown--;
		blockEntity.lastTickTime = world.getTime();

		if (!blockEntity.needsCooldown()) {
			blockEntity.setTransferCooldown(0);
			insertAndExtract(world, pos, state, blockEntity, () -> extract(world, blockEntity));
		}
	}

	/**
	 * Выполняет вставку и извлечение предметов, устанавливая кулдаун при успехе.
	 */
	private static boolean insertAndExtract(
			World world,
			BlockPos pos,
			BlockState state,
			HopperBlockEntity blockEntity,
			BooleanSupplier booleanSupplier
	) {
		if (world.isClient()) {
			return false;
		}

		if (!blockEntity.needsCooldown() && state.get(HopperBlock.ENABLED)) {
			boolean transferred = false;

			if (!blockEntity.isEmpty()) {
				transferred = insert(world, pos, blockEntity);
			}

			if (!blockEntity.isFull()) {
				transferred |= booleanSupplier.getAsBoolean();
			}

			if (transferred) {
				blockEntity.setTransferCooldown(8);
				markDirty(world, pos, state);
				return true;
			}
		}

		return false;
	}

	/**
	 * @return {@code true}, если все слоты инвентаря заполнены до максимума
	 */
	private boolean isFull() {
		for (ItemStack itemStack : this.inventory) {
			if (itemStack.isEmpty() || itemStack.getCount() != itemStack.getMaxCount()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Вставляет предметы из воронки в выходной инвентарь.
	 */
	private static boolean insert(World world, BlockPos pos, HopperBlockEntity blockEntity) {
		Inventory outputInventory = getOutputInventory(world, pos, blockEntity);

		if (outputInventory == null) {
			return false;
		}

		Direction direction = blockEntity.facing.getOpposite();

		if (isInventoryFull(outputInventory, direction)) {
			return false;
		}

		for (int i = 0; i < blockEntity.size(); i++) {
			ItemStack itemStack = blockEntity.getStack(i);

			if (!itemStack.isEmpty()) {
				int j = itemStack.getCount();
				ItemStack remainder = transfer(blockEntity, outputInventory, blockEntity.removeStack(i, 1), direction);

				if (remainder.isEmpty()) {
					outputInventory.markDirty();
					return true;
				}

				itemStack.setCount(j);

				if (j == 1) {
					blockEntity.setStack(i, itemStack);
				}
			}
		}

		return false;
	}

	/**
	 * Возвращает доступные слоты инвентаря с кэшированием для несайдированных инвентарей.
	 */
	private static int[] getAvailableSlots(Inventory inv, Direction side) {
		if (inv instanceof SidedInventory sidedInventory) {
			return sidedInventory.getAvailableSlots(side);
		}

		int size = inv.size();

		if (size < AVAILABLE_SLOTS_CACHE.length) {
			int[] cached = AVAILABLE_SLOTS_CACHE[size];

			if (cached != null) {
				return cached;
			}

			int[] computed = indexArray(size);
			AVAILABLE_SLOTS_CACHE[size] = computed;
			return computed;
		}

		return indexArray(size);
	}

	/**
	 * Создаёт массив индексов от 0 до {@code size - 1}.
	 */
	private static int[] indexArray(int size) {
		int[] result = new int[size];
		int i = 0;

		while (i < result.length) {
			result[i] = i++;
		}

		return result;
	}

	/**
	 * @return {@code true}, если все доступные слоты инвентаря заполнены
	 */
	private static boolean isInventoryFull(Inventory inv, Direction direction) {
		int[] slots = getAvailableSlots(inv, direction);

		for (int slot : slots) {
			ItemStack itemStack = inv.getStack(slot);

			if (itemStack.getCount() < itemStack.getMaxCount()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Извлекает предметы из инвентаря над воронкой или из предметных сущностей.
	 *
	 * @param world  мир
	 * @param hopper воронка-получатель
	 * @return {@code true}, если хотя бы один предмет был извлечён
	 */
	public static boolean extract(World world, Hopper hopper) {
		BlockPos blockPos = BlockPos.ofFloored(hopper.getHopperX(), hopper.getHopperY() + 1.0, hopper.getHopperZ());
		BlockState blockState = world.getBlockState(blockPos);
		Inventory inputInventory = getInputInventory(world, hopper, blockPos, blockState);

		if (inputInventory != null) {
			Direction direction = Direction.DOWN;

			for (int slot : getAvailableSlots(inputInventory, direction)) {
				if (extract(hopper, inputInventory, slot, direction)) {
					return true;
				}
			}

			return false;
		}

		boolean blocked = hopper.canBlockFromAbove()
				&& blockState.isFullCube(world, blockPos)
				&& !blockState.isIn(BlockTags.DOES_NOT_BLOCK_HOPPERS);

		if (blocked) {
			return false;
		}

		for (ItemEntity itemEntity : getInputItemEntities(world, hopper)) {
			if (extract(hopper, itemEntity)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Извлекает один предмет из конкретного слота инвентаря в воронку.
	 */
	private static boolean extract(Hopper hopper, Inventory sourceInventory, int slot, Direction side) {
		ItemStack itemStack = sourceInventory.getStack(slot);

		if (itemStack.isEmpty() || !canExtract(hopper, sourceInventory, itemStack, slot, side)) {
			return false;
		}

		int count = itemStack.getCount();
		ItemStack remainder = transfer(sourceInventory, hopper, sourceInventory.removeStack(slot, 1), null);

		if (remainder.isEmpty()) {
			sourceInventory.markDirty();
			return true;
		}

		itemStack.setCount(count);

		if (count == 1) {
			sourceInventory.setStack(slot, itemStack);
		}

		return false;
	}

	/**
	 * Извлекает предмет из предметной сущности в инвентарь.
	 *
	 * @param targetInventory целевой инвентарь
	 * @param itemEntity      предметная сущность
	 * @return {@code true}, если предмет был полностью поглощён
	 */
	public static boolean extract(Inventory targetInventory, ItemEntity itemEntity) {
		boolean extracted = false;
		ItemStack itemStack = itemEntity.getStack().copy();
		ItemStack remainder = transfer(null, targetInventory, itemStack, null);

		if (remainder.isEmpty()) {
			extracted = true;
			itemEntity.setStack(ItemStack.EMPTY);
			itemEntity.discard();
		}
		else {
			itemEntity.setStack(remainder);
		}

		return extracted;
	}

	/**
	 * Переносит стек предметов в целевой инвентарь по всем доступным слотам.
	 *
	 * @param from  источник (может быть {@code null})
	 * @param to    целевой инвентарь
	 * @param stack переносимый стек
	 * @param side  сторона вставки (может быть {@code null})
	 * @return остаток стека после переноса
	 */
	public static ItemStack transfer(
			@Nullable Inventory from,
			Inventory to,
			ItemStack stack,
			@Nullable Direction side
	) {
		if (to instanceof SidedInventory sidedInventory && side != null) {
			int[] slots = sidedInventory.getAvailableSlots(side);

			for (int i = 0; i < slots.length && !stack.isEmpty(); i++) {
				stack = transfer(from, to, stack, slots[i], side);
			}
		}
		else {
			int size = to.size();

			for (int i = 0; i < size && !stack.isEmpty(); i++) {
				stack = transfer(from, to, stack, i, side);
			}
		}

		return stack;
	}

	/**
	 * @return {@code true}, если предмет можно вставить в указанный слот инвентаря
	 */
	private static boolean canInsert(Inventory inv, ItemStack stack, int slot, @Nullable Direction side) {
		return !inv.isValid(slot, stack)
		       ? false
		       : !(inv instanceof SidedInventory sidedInventory && !sidedInventory.canInsert(slot, stack, side));
	}

	/**
	 * @return {@code true}, если предмет можно извлечь из указанного слота инвентаря
	 */
	private static boolean canExtract(
			Inventory hopperInventory,
			Inventory fromInventory,
			ItemStack stack,
			int slot,
			Direction facing
	) {
		return !fromInventory.canTransferTo(hopperInventory, slot, stack)
		       ? false
		       : !(fromInventory instanceof SidedInventory sidedInventory && !sidedInventory.canExtract(
				       slot,
				       stack,
				       facing
		       )
		       );
	}

	/**
	 * Переносит предмет в конкретный слот целевого инвентаря.
	 */
	private static ItemStack transfer(
			@Nullable Inventory from,
			Inventory to,
			ItemStack stack,
			int slot,
			@Nullable Direction side
	) {
		ItemStack slotStack = to.getStack(slot);

		if (canInsert(to, stack, slot, side)) {
			boolean transferred = false;
			boolean wasEmpty = to.isEmpty();

			if (slotStack.isEmpty()) {
				to.setStack(slot, stack);
				stack = ItemStack.EMPTY;
				transferred = true;
			}
			else if (canMergeItems(slotStack, stack)) {
				int space = stack.getMaxCount() - slotStack.getCount();
				int amount = Math.min(stack.getCount(), space);
				stack.decrement(amount);
				slotStack.increment(amount);
				transferred = amount > 0;
			}

			if (transferred) {
				if (wasEmpty && to instanceof HopperBlockEntity hopperBlockEntity && !hopperBlockEntity.isDisabled()) {
					int cooldownOffset = 0;

					if (from instanceof HopperBlockEntity sourceHopper
							&& hopperBlockEntity.lastTickTime >= sourceHopper.lastTickTime) {
						cooldownOffset = 1;
					}

					hopperBlockEntity.setTransferCooldown(8 - cooldownOffset);
				}

				to.markDirty();
			}
		}

		return stack;
	}

	/**
	 * @return инвентарь, в который воронка должна вставлять предметы, или {@code null}
	 */
	private static @Nullable Inventory getOutputInventory(World world, BlockPos pos, HopperBlockEntity blockEntity) {
		return getInventoryAt(world, pos.offset(blockEntity.facing));
	}

	/**
	 * @return инвентарь над воронкой, из которого она должна извлекать предметы, или {@code null}
	 */
	private static @Nullable Inventory getInputInventory(World world, Hopper hopper, BlockPos pos, BlockState state) {
		return getInventoryAt(world, pos, state, hopper.getHopperX(), hopper.getHopperY() + 1.0, hopper.getHopperZ());
	}

	/**
	 * Возвращает список предметных сущностей в зоне всасывания воронки.
	 *
	 * @param world  мир
	 * @param hopper воронка
	 * @return список предметных сущностей
	 */
	public static List<ItemEntity> getInputItemEntities(World world, Hopper hopper) {
		Box
				box =
				hopper
						.getInputAreaShape()
						.offset(hopper.getHopperX() - 0.5, hopper.getHopperY() - 0.5, hopper.getHopperZ() - 0.5);
		return world.getEntitiesByClass(ItemEntity.class, box, EntityPredicates.VALID_ENTITY);
	}

	/**
	 * Возвращает инвентарь в центре указанного блока.
	 *
	 * @param world мир
	 * @param pos   позиция блока
	 * @return инвентарь или {@code null}
	 */
	public static @Nullable Inventory getInventoryAt(World world, BlockPos pos) {
		return getInventoryAt(
				world,
				pos,
				world.getBlockState(pos),
				pos.getX() + 0.5,
				pos.getY() + 0.5,
				pos.getZ() + 0.5
		);
	}

	/**
	 * Возвращает инвентарь блока или сущности в указанной точке.
	 */
	private static @Nullable Inventory getInventoryAt(
			World world,
			BlockPos pos,
			BlockState state,
			double x,
			double y,
			double z
	) {
		Inventory foundInventory = getBlockInventoryAt(world, pos, state);

		if (foundInventory == null) {
			foundInventory = getEntityInventoryAt(world, x, y, z);
		}

		return foundInventory;
	}

	/**
	 * Возвращает инвентарь блока в указанной позиции, включая двойные сундуки.
	 */
	private static @Nullable Inventory getBlockInventoryAt(World world, BlockPos pos, BlockState state) {
		Block block = state.getBlock();

		if (block instanceof InventoryProvider) {
			return ((InventoryProvider) block).getInventory(state, world, pos);
		}

		if (state.hasBlockEntity() && world.getBlockEntity(pos) instanceof Inventory blockInventory) {
			if (blockInventory instanceof ChestBlockEntity && block instanceof ChestBlock chestBlock) {
				return ChestBlock.getInventory(chestBlock, state, world, pos, true);
			}

			return blockInventory;
		}

		return null;
	}

	/**
	 * Возвращает инвентарь сущности в указанной точке (случайный, если их несколько).
	 */
	private static @Nullable Inventory getEntityInventoryAt(World world, double x, double y, double z) {
		List<Entity> entities = world.getOtherEntities(
				(Entity) null,
				new Box(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5),
				EntityPredicates.VALID_INVENTORIES
		);

		return !entities.isEmpty() ? (Inventory) entities.get(world.random.nextInt(entities.size())) : null;
	}

	/**
	 * @return {@code true}, если два стека можно объединить (одинаковый предмет и компоненты)
	 */
	private static boolean canMergeItems(ItemStack first, ItemStack second) {
		return first.getCount() <= first.getMaxCount() && ItemStack.areItemsAndComponentsEqual(first, second);
	}

	@Override
	public double getHopperX() {
		return this.pos.getX() + 0.5;
	}

	@Override
	public double getHopperY() {
		return this.pos.getY() + 0.5;
	}

	@Override
	public double getHopperZ() {
		return this.pos.getZ() + 0.5;
	}

	@Override
	public boolean canBlockFromAbove() {
		return true;
	}

	/**
	 * Устанавливает кулдаун передачи предметов.
	 *
	 * @param cooldown значение кулдауна в тиках
	 */
	private void setTransferCooldown(int cooldown) {
		this.transferCooldown = cooldown;
	}

	/**
	 * @return {@code true}, если воронка находится на кулдауне
	 */
	private boolean needsCooldown() {
		return this.transferCooldown > 0;
	}

	/**
	 * @return {@code true}, если воронка отключена (кулдаун превышает максимум)
	 */
	private boolean isDisabled() {
		return this.transferCooldown > 8;
	}

	@Override
	protected DefaultedList<ItemStack> getHeldStacks() {
		return this.inventory;
	}

	@Override
	protected void setHeldStacks(DefaultedList<ItemStack> inventory) {
		this.inventory = inventory;
	}

	/**
	 * Обрабатывает столкновение предметной сущности с воронкой.
	 *
	 * @param world       мир
	 * @param pos         позиция воронки
	 * @param state       состояние блока
	 * @param entity      сущность, столкнувшаяся с воронкой
	 * @param blockEntity блок-сущность воронки
	 */
	public static void onEntityCollided(
			World world,
			BlockPos pos,
			BlockState state,
			Entity entity,
			HopperBlockEntity blockEntity
	) {
		if (entity instanceof ItemEntity itemEntity
				&& !itemEntity.getStack().isEmpty()
				&& entity
				.getBoundingBox()
				.offset(-pos.getX(), -pos.getY(), -pos.getZ())
				.intersects(blockEntity.getInputAreaShape())
		) {
			insertAndExtract(world, pos, state, blockEntity, () -> extract(blockEntity, itemEntity));
		}
	}

	@Override
	protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
		return new HopperScreenHandler(syncId, playerInventory, this);
	}
}

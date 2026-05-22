package net.minecraft.entity.vehicle;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Вагонетка с воронкой. Автоматически всасывает предметы с земли и из контейнеров сверху.
 * Может быть отключена активированным рельсом-активатором.
 */
public class HopperMinecartEntity extends StorageMinecartEntity implements Hopper {

	private static final int INVENTORY_SIZE = 5;
	private static final int DEFAULT_BLOCK_OFFSET = 1;
	private static final double HOPPER_Y_OFFSET = 0.5;
	private static final double ITEM_PICKUP_EXPAND = 0.25;

	private boolean enabled = true;
	private boolean hopperTicked = false;

	public HopperMinecartEntity(EntityType<? extends HopperMinecartEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	public BlockState getDefaultContainedBlock() {
		return Blocks.HOPPER.getDefaultState();
	}

	@Override
	public int getDefaultBlockOffset() {
		return DEFAULT_BLOCK_OFFSET;
	}

	@Override
	public int size() {
		return INVENTORY_SIZE;
	}

	/**
	 * Включает или отключает воронку в зависимости от сигнала активатора.
	 * Активированный рельс (powered=true) отключает воронку.
	 */
	@Override
	public void onActivatorRail(ServerWorld serverWorld, int x, int y, int z, boolean powered) {
		boolean shouldBeEnabled = !powered;

		if (shouldBeEnabled != enabled) {
			setEnabled(shouldBeEnabled);
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public double getHopperX() {
		return getX();
	}

	@Override
	public double getHopperY() {
		return getY() + HOPPER_Y_OFFSET;
	}

	@Override
	public double getHopperZ() {
		return getZ();
	}

	@Override
	public boolean canBlockFromAbove() {
		return false;
	}

	@Override
	public void tick() {
		hopperTicked = false;
		super.tick();
		tickHopper();
	}

	@Override
	protected double moveAlongTrack(BlockPos pos, RailShape shape, double remainingMovement) {
		double remaining = super.moveAlongTrack(pos, shape, remainingMovement);
		tickHopper();
		return remaining;
	}

	private void tickHopper() {
		if (getEntityWorld().isClient() || !isAlive() || !enabled || hopperTicked) {
			return;
		}

		if (canOperate()) {
			hopperTicked = true;
			markDirty();
		}
	}

	/**
	 * Пытается извлечь предметы из контейнера сверху или подобрать предметы с земли.
	 *
	 * @return {@code true} если хотя бы один предмет был перемещён
	 */
	public boolean canOperate() {
		if (HopperBlockEntity.extract(getEntityWorld(), this)) {
			return true;
		}

		for (ItemEntity itemEntity : getEntityWorld().getEntitiesByClass(
				ItemEntity.class,
				getBoundingBox().expand(ITEM_PICKUP_EXPAND, 0.0, ITEM_PICKUP_EXPAND),
				EntityPredicates.VALID_ENTITY
		)) {
			if (HopperBlockEntity.extract(this, itemEntity)) {
				return true;
			}
		}

		return false;
	}

	@Override
	protected Item asItem() {
		return Items.HOPPER_MINECART;
	}

	@Override
	public ItemStack getPickBlockStack() {
		return new ItemStack(Items.HOPPER_MINECART);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("Enabled", enabled);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		enabled = view.getBoolean("Enabled", true);
	}

	@Override
	public ScreenHandler getScreenHandler(int syncId, PlayerInventory playerInventory) {
		return new HopperScreenHandler(syncId, playerInventory, this);
	}
}

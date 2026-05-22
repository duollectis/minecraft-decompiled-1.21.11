package net.minecraft.entity.vehicle;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.ContainerUser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Вагонетка с сундуком. Вмещает 27 предметов (3×9 слотов).
 * При открытии уведомляет мозг пиглина об взаимодействии с охраняемым блоком.
 */
public class ChestMinecartEntity extends StorageMinecartEntity {

	private static final int INVENTORY_SIZE = 27;
	private static final int DEFAULT_BLOCK_OFFSET = 8;

	public ChestMinecartEntity(EntityType<? extends ChestMinecartEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected Item asItem() {
		return Items.CHEST_MINECART;
	}

	@Override
	public ItemStack getPickBlockStack() {
		return new ItemStack(Items.CHEST_MINECART);
	}

	@Override
	public int size() {
		return INVENTORY_SIZE;
	}

	@Override
	public BlockState getDefaultContainedBlock() {
		return Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.NORTH);
	}

	@Override
	public int getDefaultBlockOffset() {
		return DEFAULT_BLOCK_OFFSET;
	}

	@Override
	public ScreenHandler getScreenHandler(int syncId, PlayerInventory playerInventory) {
		return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, this);
	}

	@Override
	public void onClose(ContainerUser user) {
		getEntityWorld().emitGameEvent(
				GameEvent.CONTAINER_CLOSE,
				getEntityPos(),
				GameEvent.Emitter.of(user.asLivingEntity())
		);
	}

	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		ActionResult result = open(player);

		if (result.isAccepted() && player.getEntityWorld() instanceof ServerWorld serverWorld) {
			emitGameEvent(GameEvent.CONTAINER_OPEN, player);
			PiglinBrain.onGuardedBlockInteracted(serverWorld, player, true);
		}

		return result;
	}
}

package net.minecraft.entity.vehicle;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.spawner.MobSpawnerLogic;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Вагонетка с мобспавнером — переносит логику {@link MobSpawnerLogic} в движущуюся сущность.
 *
 * <p>Тикер выбирается один раз при создании сущности в зависимости от стороны (сервер/клиент),
 * чтобы избежать проверки {@code instanceof ServerWorld} на каждом тике.
 */
public class SpawnerMinecartEntity extends AbstractMinecartEntity {

	private final MobSpawnerLogic logic = new MobSpawnerLogic() {
		@Override
		public void sendStatus(World world, BlockPos pos, int status) {
			world.sendEntityStatus(SpawnerMinecartEntity.this, (byte) status);
		}
	};

	private final Runnable ticker;

	public SpawnerMinecartEntity(EntityType<? extends SpawnerMinecartEntity> entityType, World world) {
		super(entityType, world);
		ticker = buildTicker(world);
	}

	@Override
	protected Item asItem() {
		return Items.MINECART;
	}

	@Override
	public ItemStack getPickBlockStack() {
		return new ItemStack(Items.MINECART);
	}

	@Override
	public BlockState getDefaultContainedBlock() {
		return Blocks.SPAWNER.getDefaultState();
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		logic.readData(getEntityWorld(), getBlockPos(), view);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		logic.writeData(view);
	}

	@Override
	public void handleStatus(byte status) {
		logic.handleStatus(getEntityWorld(), status);
	}

	@Override
	public void tick() {
		super.tick();
		ticker.run();
	}

	public MobSpawnerLogic getLogic() {
		return logic;
	}

	/**
	 * Создаёт тикер, специфичный для стороны выполнения.
	 *
	 * <p>Разделение на серверный и клиентский тикер позволяет избежать
	 * повторной проверки типа мира на каждом игровом тике.
	 */
	private Runnable buildTicker(World world) {
		return world instanceof ServerWorld serverWorld
			? () -> logic.serverTick(serverWorld, getBlockPos())
			: () -> logic.clientTick(world, getBlockPos());
	}
}

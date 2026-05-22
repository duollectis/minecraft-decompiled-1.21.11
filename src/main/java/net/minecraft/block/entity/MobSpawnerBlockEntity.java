package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.spawner.MobSpawnerEntry;
import net.minecraft.block.spawner.MobSpawnerLogic;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Блок-сущность спавнера мобов. Делегирует всю логику спавна в {@link MobSpawnerLogic},
 * синхронизирует состояние через синхронизированные события блока.
 */
public class MobSpawnerBlockEntity extends BlockEntity implements Spawner {

	private final MobSpawnerLogic logic = new MobSpawnerLogic() {
		@Override
		public void sendStatus(World world, BlockPos pos, int status) {
			world.addSyncedBlockEvent(pos, Blocks.SPAWNER, status, 0);
		}

		@Override
		public void setSpawnEntry(@Nullable World world, BlockPos pos, MobSpawnerEntry spawnEntry) {
			super.setSpawnEntry(world, pos, spawnEntry);
			if (world != null) {
				BlockState blockState = world.getBlockState(pos);
				world.updateListeners(pos, blockState, blockState, 260);
			}
		}
	};

	public MobSpawnerBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.MOB_SPAWNER, pos, state);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		logic.readData(world, pos, view);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		logic.writeData(view);
	}

	public static void clientTick(World world, BlockPos pos, BlockState state, MobSpawnerBlockEntity blockEntity) {
		blockEntity.logic.clientTick(world, pos);
	}

	public static void serverTick(World world, BlockPos pos, BlockState state, MobSpawnerBlockEntity blockEntity) {
		blockEntity.logic.serverTick((ServerWorld) world, pos);
	}

	@Override
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		NbtCompound nbt = createComponentlessNbt(registries);
		nbt.remove("SpawnPotentials");
		return nbt;
	}

	@Override
	public boolean onSyncedBlockEvent(int type, int data) {
		return logic.handleStatus(world, type) ? true : super.onSyncedBlockEvent(type, data);
	}

	@Override
	public void setEntityType(EntityType<?> type, Random random) {
		logic.setEntityId(type, world, random, pos);
		markDirty();
	}

	public MobSpawnerLogic getLogic() {
		return logic;
	}
}

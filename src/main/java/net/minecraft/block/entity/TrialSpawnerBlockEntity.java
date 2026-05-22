package net.minecraft.block.entity;

import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.TrialSpawnerBlock;
import net.minecraft.block.enums.TrialSpawnerState;
import net.minecraft.block.spawner.EntityDetector;
import net.minecraft.block.spawner.TrialSpawnerLogic;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.state.property.Properties;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Блок-сущность испытательного спаунера. Делегирует всю логику спауна в {@link TrialSpawnerLogic},
 * управляет состоянием блока и синхронизацией с клиентом.
 */
public class TrialSpawnerBlockEntity extends BlockEntity implements Spawner, TrialSpawnerLogic.TrialSpawner {

	private final TrialSpawnerLogic logic = createDefaultLogic();

	public TrialSpawnerBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.TRIAL_SPAWNER, pos, state);
	}

	private TrialSpawnerLogic createDefaultLogic() {
		EntityDetector entityDetector = SharedConstants.TRIAL_SPAWNER_DETECTS_SHEEP_AS_PLAYERS
				? EntityDetector.SHEEP
				: EntityDetector.SURVIVAL_PLAYERS;

		return new TrialSpawnerLogic(
				TrialSpawnerLogic.FullConfig.DEFAULT,
				this,
				entityDetector,
				EntityDetector.Selector.IN_WORLD
		);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		logic.readData(view);

		if (world != null) {
			updateListeners();
		}
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		logic.writeData(view);
	}

	@Override
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return logic.getData().getSpawnDataNbt(getCachedState().get(TrialSpawnerBlock.TRIAL_SPAWNER_STATE));
	}

	@Override
	public void setEntityType(EntityType<?> type, Random random) {
		if (world == null) {
			Util.logErrorOrPause("Expected non-null level");
			return;
		}

		logic.setEntityType(type, world);
		markDirty();
	}

	public TrialSpawnerLogic getSpawner() {
		return logic;
	}

	@Override
	public TrialSpawnerState getSpawnerState() {
		return getCachedState().contains(Properties.TRIAL_SPAWNER_STATE)
				? getCachedState().get(Properties.TRIAL_SPAWNER_STATE)
				: TrialSpawnerState.INACTIVE;
	}

	@Override
	public void setSpawnerState(World world, TrialSpawnerState spawnerState) {
		markDirty();
		world.setBlockState(pos, getCachedState().with(Properties.TRIAL_SPAWNER_STATE, spawnerState));
	}

	@Override
	public void updateListeners() {
		markDirty();
		if (world != null) {
			world.updateListeners(pos, getCachedState(), getCachedState(), 3);
		}
	}
}

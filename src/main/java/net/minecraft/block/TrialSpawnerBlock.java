package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.block.enums.TrialSpawnerState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * {@code TrialSpawnerBlock}.
 */
public class TrialSpawnerBlock extends BlockWithEntity {

	public static final MapCodec<TrialSpawnerBlock> CODEC = createCodec(TrialSpawnerBlock::new);
	public static final EnumProperty<TrialSpawnerState> TRIAL_SPAWNER_STATE = Properties.TRIAL_SPAWNER_STATE;
	public static final BooleanProperty OMINOUS = Properties.OMINOUS;

	@Override
	public MapCodec<TrialSpawnerBlock> getCodec() {
		return CODEC;
	}

	public TrialSpawnerBlock(AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager
				.getDefaultState()
				.with(TRIAL_SPAWNER_STATE, TrialSpawnerState.INACTIVE)
				.with(OMINOUS, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(TRIAL_SPAWNER_STATE, OMINOUS);
	}

	@Override
	public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new TrialSpawnerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
			World world,
			BlockState state,
			BlockEntityType<T> type
	) {
		return world instanceof ServerWorld serverWorld
		       ? validateTicker(
				type,
				BlockEntityType.TRIAL_SPAWNER,
				(worldx, pos, statex, blockEntity) -> blockEntity
				                                      .getSpawner()
				                                      .tickServer(
						                                      serverWorld,
						                                      pos,
						                                      statex.getOrEmpty(Properties.OMINOUS).orElse(false)
				                                      )
		)
		       : validateTicker(
				       type,
				       BlockEntityType.TRIAL_SPAWNER,
				       (worldx, pos, statex, blockEntity) -> blockEntity
				                                             .getSpawner()
				                                             .tickClient(
						                                             worldx,
						                                             pos,
						                                             statex.getOrEmpty(Properties.OMINOUS).orElse(false)
				                                             )
		       );
	}
}

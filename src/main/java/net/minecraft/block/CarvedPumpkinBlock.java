package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.BlockPatternBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.CopperGolemEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.item.HoneycombItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.predicate.block.BlockStatePredicate;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Блок вырезанной тыквы. При размещении проверяет наличие паттернов голема
 * (снежного, железного или медного) и при совпадении спавнит соответствующую сущность.
 */
public class CarvedPumpkinBlock extends HorizontalFacingBlock {

	public static final MapCodec<CarvedPumpkinBlock> CODEC = createCodec(CarvedPumpkinBlock::new);
	public static final EnumProperty<Direction> FACING = HorizontalFacingBlock.FACING;
	private @Nullable BlockPattern snowGolemDispenserPattern;
	private @Nullable BlockPattern snowGolemPattern;
	private @Nullable BlockPattern ironGolemDispenserPattern;
	private @Nullable BlockPattern ironGolemPattern;
	private @Nullable BlockPattern copperGolemDispenserPattern;
	private @Nullable BlockPattern copperGolemPattern;
	private static final Predicate<BlockState>
			IS_GOLEM_HEAD_PREDICATE =
			state -> state.isOf(Blocks.CARVED_PUMPKIN) || state.isOf(Blocks.JACK_O_LANTERN);

	@Override
	public MapCodec<? extends CarvedPumpkinBlock> getCodec() {
		return CODEC;
	}

	public CarvedPumpkinBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager.getDefaultState().with(FACING, Direction.NORTH));
	}

	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		if (!oldState.isOf(state.getBlock())) {
			this.trySpawnEntity(world, pos);
		}
	}

	/**
	 * Проверяет, можно ли использовать диспенсер для спавна голема в данной позиции.
	 * Возвращает {@code true}, если рядом есть паттерн снежного, железного или медного голема.
	 */
	public boolean canDispense(WorldView world, BlockPos pos) {
		return getSnowGolemDispenserPattern().searchAround(world, pos) != null
				|| getIronGolemDispenserPattern().searchAround(world, pos) != null
				|| getCopperGolemDispenserPattern().searchAround(world, pos) != null;
	}

	private void trySpawnEntity(World world, BlockPos pos) {
		BlockPattern.Result snowResult = getSnowGolemPattern().searchAround(world, pos);
		if (snowResult != null) {
			SnowGolemEntity snowGolem = EntityType.SNOW_GOLEM.create(world, SpawnReason.TRIGGERED);
			if (snowGolem != null) {
				spawnEntity(world, snowResult, snowGolem, snowResult.translate(0, 2, 0).getBlockPos());
				return;
			}
		}

		BlockPattern.Result ironResult = getIronGolemPattern().searchAround(world, pos);
		if (ironResult != null) {
			IronGolemEntity ironGolem = EntityType.IRON_GOLEM.create(world, SpawnReason.TRIGGERED);
			if (ironGolem != null) {
				ironGolem.setPlayerCreated(true);
				spawnEntity(world, ironResult, ironGolem, ironResult.translate(1, 2, 0).getBlockPos());
				return;
			}
		}

		BlockPattern.Result copperResult = getCopperGolemPattern().searchAround(world, pos);
		if (copperResult != null) {
			CopperGolemEntity copperGolem = EntityType.COPPER_GOLEM.create(world, SpawnReason.TRIGGERED);
			if (copperGolem != null) {
				spawnEntity(world, copperResult, copperGolem, copperResult.translate(0, 0, 0).getBlockPos());
				replaceCopperBlockWithChest(world, copperResult);
				copperGolem.onSpawn(getOxidationLevel(copperResult));
			}
		}
	}

	private Oxidizable.OxidationLevel getOxidationLevel(BlockPattern.Result patternResult) {
		BlockState bodyState = patternResult.translate(0, 1, 0).getBlockState();
		return bodyState.getBlock() instanceof Oxidizable oxidizable
			? oxidizable.getDegradationLevel()
			: Optional.ofNullable((Block) HoneycombItem.WAXED_TO_UNWAXED_BLOCKS.get().get(bodyState.getBlock()))
				.filter(block -> block instanceof Oxidizable)
				.map(block -> (Oxidizable) block)
				.orElse((Oxidizable) Blocks.COPPER_BLOCK)
				.getDegradationLevel();
	}

	private static void spawnEntity(World world, BlockPattern.Result patternResult, Entity entity, BlockPos pos) {
		breakPatternBlocks(world, patternResult);
		entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5, 0.0F, 0.0F);
		world.spawnEntity(entity);

		for (ServerPlayerEntity nearbyPlayer : world.getNonSpectatingEntities(
			ServerPlayerEntity.class,
			entity.getBoundingBox().expand(5.0)
		)) {
			Criteria.SUMMONED_ENTITY.trigger(nearbyPlayer, entity);
		}

		updatePatternBlocks(world, patternResult);
	}

	/**
	 * Разрушает все блоки паттерна голема, заменяя их воздухом, и воспроизводит
	 * звуковой эффект разрушения для каждого блока (world event 2001).
	 */
	public static void breakPatternBlocks(World world, BlockPattern.Result patternResult) {
		for (int col = 0; col < patternResult.getWidth(); col++) {
			for (int row = 0; row < patternResult.getHeight(); row++) {
				CachedBlockPosition cached = patternResult.translate(col, row, 0);
				world.setBlockState(cached.getBlockPos(), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
				world.syncWorldEvent(
					WorldEvents.BLOCK_BROKEN,
					cached.getBlockPos(),
					Block.getRawIdFromState(cached.getBlockState())
				);
			}
		}
	}

	/**
	 * Уведомляет соседей всех блоков паттерна об изменении (после спавна голема).
	 */
	public static void updatePatternBlocks(World world, BlockPattern.Result patternResult) {
		for (int col = 0; col < patternResult.getWidth(); col++) {
			for (int row = 0; row < patternResult.getHeight(); row++) {
				CachedBlockPosition cached = patternResult.translate(col, row, 0);
				world.updateNeighbors(cached.getBlockPos(), Blocks.AIR);
			}
		}
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	private BlockPattern getSnowGolemDispenserPattern() {
		if (this.snowGolemDispenserPattern == null) {
			this.snowGolemDispenserPattern = BlockPatternBuilder.start()
			                                                    .aisle(" ", "#", "#")
			                                                    .where(
					                                                    '#',
					                                                    CachedBlockPosition.matchesBlockState(
							                                                    BlockStatePredicate.forBlock(Blocks.SNOW_BLOCK))
			                                                    )
			                                                    .build();
		}

		return this.snowGolemDispenserPattern;
	}

	private BlockPattern getSnowGolemPattern() {
		if (this.snowGolemPattern == null) {
			this.snowGolemPattern = BlockPatternBuilder.start()
			                                           .aisle("^", "#", "#")
			                                           .where(
					                                           '^',
					                                           CachedBlockPosition.matchesBlockState(
							                                           IS_GOLEM_HEAD_PREDICATE)
			                                           )
			                                           .where(
					                                           '#',
					                                           CachedBlockPosition.matchesBlockState(BlockStatePredicate.forBlock(
							                                           Blocks.SNOW_BLOCK))
			                                           )
			                                           .build();
		}

		return this.snowGolemPattern;
	}

	private BlockPattern getIronGolemDispenserPattern() {
		if (this.ironGolemDispenserPattern == null) {
			this.ironGolemDispenserPattern = BlockPatternBuilder.start()
			                                                    .aisle("~ ~", "###", "~#~")
			                                                    .where(
					                                                    '#',
					                                                    CachedBlockPosition.matchesBlockState(
							                                                    BlockStatePredicate.forBlock(Blocks.IRON_BLOCK))
			                                                    )
			                                                    .where(
					                                                    '~',
					                                                    CachedBlockPosition.matchesBlockState(
							                                                    AbstractBlock.AbstractBlockState::isAir)
			                                                    )
			                                                    .build();
		}

		return this.ironGolemDispenserPattern;
	}

	private BlockPattern getIronGolemPattern() {
		if (this.ironGolemPattern == null) {
			this.ironGolemPattern = BlockPatternBuilder.start()
			                                           .aisle("~^~", "###", "~#~")
			                                           .where(
					                                           '^',
					                                           CachedBlockPosition.matchesBlockState(
							                                           IS_GOLEM_HEAD_PREDICATE)
			                                           )
			                                           .where(
					                                           '#',
					                                           CachedBlockPosition.matchesBlockState(BlockStatePredicate.forBlock(
							                                           Blocks.IRON_BLOCK))
			                                           )
			                                           .where(
					                                           '~',
					                                           CachedBlockPosition.matchesBlockState(AbstractBlock.AbstractBlockState::isAir)
			                                           )
			                                           .build();
		}

		return this.ironGolemPattern;
	}

	private BlockPattern getCopperGolemDispenserPattern() {
		if (this.copperGolemDispenserPattern == null) {
			this.copperGolemDispenserPattern = BlockPatternBuilder.start()
			                                                      .aisle(" ", "#")
			                                                      .where(
					                                                      '#',
					                                                      CachedBlockPosition.matchesBlockState(state -> state.isIn(
							                                                      BlockTags.COPPER))
			                                                      )
			                                                      .build();
		}

		return this.copperGolemDispenserPattern;
	}

	private BlockPattern getCopperGolemPattern() {
		if (this.copperGolemPattern == null) {
			this.copperGolemPattern = BlockPatternBuilder.start()
			                                             .aisle("^", "#")
			                                             .where(
					                                             '^',
					                                             CachedBlockPosition.matchesBlockState(
							                                             IS_GOLEM_HEAD_PREDICATE)
			                                             )
			                                             .where(
					                                             '#',
					                                             CachedBlockPosition.matchesBlockState(state -> state.isIn(
							                                             BlockTags.COPPER))
			                                             )
			                                             .build();
		}

		return this.copperGolemPattern;
	}

	/**
	 * Заменяет медный блок в теле голема на медный сундук соответствующего уровня окисления.
	 * Направление сундука берётся из состояния тыквы (головы голема).
	 */
	public void replaceCopperBlockWithChest(World world, BlockPattern.Result patternResult) {
		CachedBlockPosition bodyPos = patternResult.translate(0, 1, 0);
		CachedBlockPosition headPos = patternResult.translate(0, 0, 0);
		Direction facing = headPos.getBlockState().get(FACING);
		BlockState chestState = CopperChestBlock.fromCopperBlock(
			bodyPos.getBlockState().getBlock(), facing, world, bodyPos.getBlockPos()
		);
		world.setBlockState(bodyPos.getBlockPos(), chestState, Block.NOTIFY_LISTENERS);
	}
}

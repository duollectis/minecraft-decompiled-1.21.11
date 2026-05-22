package net.minecraft.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.recipe.CampfireCookingRecipe;
import net.minecraft.recipe.RecipePropertySet;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

/**
 * Блок костра. Поддерживает приготовление пищи, сигнальный дым (при размещении на сене),
 * заливание водой и поджигание горящими снарядами.
 */
public class CampfireBlock extends BlockWithEntity implements Waterloggable {

	public static final MapCodec<CampfireBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Codec.BOOL.fieldOf("spawn_particles").forGetter(block -> block.emitsParticles),
					Codec.intRange(0, 1000).fieldOf("fire_damage").forGetter(block -> block.fireDamage),
					createSettingsCodec()
			)
			.apply(instance, CampfireBlock::new)
	);
	public static final BooleanProperty LIT = Properties.LIT;
	public static final BooleanProperty SIGNAL_FIRE = Properties.SIGNAL_FIRE;
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
	private static final VoxelShape SHAPE = Block.createColumnShape(16.0, 0.0, 7.0);
	private static final VoxelShape SMOKEY_SHAPE = Block.createColumnShape(4.0, 0.0, 16.0);
	private static final int MAX_CAMPFIRE_SEARCH_DEPTH = 5;
	private final boolean emitsParticles;
	private final int fireDamage;

	@Override
	public MapCodec<CampfireBlock> getCodec() {
		return CODEC;
	}

	public CampfireBlock(boolean emitsParticles, int fireDamage, AbstractBlock.Settings settings) {
		super(settings);
		this.emitsParticles = emitsParticles;
		this.fireDamage = fireDamage;
		setDefaultState(stateManager.getDefaultState()
				.with(LIT, true)
				.with(SIGNAL_FIRE, false)
				.with(WATERLOGGED, false)
				.with(FACING, Direction.NORTH));
	}

	@Override
	protected ActionResult onUseWithItem(
			ItemStack stack,
			BlockState state,
			World world,
			BlockPos pos,
			PlayerEntity player,
			Hand hand,
			BlockHitResult hit
	) {
		if (world.getBlockEntity(pos) instanceof CampfireBlockEntity campfireBlockEntity) {
			ItemStack itemStack = player.getStackInHand(hand);
			if (world.getRecipeManager().getPropertySet(RecipePropertySet.CAMPFIRE_INPUT).canUse(itemStack)) {
				if (world instanceof ServerWorld serverWorld && campfireBlockEntity.addItem(
						serverWorld,
						player,
						itemStack
				)) {
					player.incrementStat(Stats.INTERACT_WITH_CAMPFIRE);
					return ActionResult.SUCCESS_SERVER;
				}

				return ActionResult.CONSUME;
			}
		}

		return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
	}

	@Override
	protected void onEntityCollision(
			BlockState state,
			World world,
			BlockPos pos,
			Entity entity,
			EntityCollisionHandler handler,
			boolean firstCollision
	) {
		if (state.get(LIT) && entity instanceof LivingEntity) {
			entity.serverDamage(world.getDamageSources().campfire(), fireDamage);
		}

		super.onEntityCollision(state, world, pos, entity, handler, firstCollision);
	}

	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		WorldAccess world = ctx.getWorld();
		BlockPos pos = ctx.getBlockPos();
		boolean isWaterlogged = world.getFluidState(pos).getFluid() == Fluids.WATER;
		return getDefaultState()
				.with(WATERLOGGED, isWaterlogged)
				.with(SIGNAL_FIRE, isSignalFireBaseBlock(world.getBlockState(pos.down())))
				.with(LIT, !isWaterlogged)
				.with(FACING, ctx.getHorizontalPlayerFacing());
	}

	@Override
	protected BlockState getStateForNeighborUpdate(
			BlockState state,
			WorldView world,
			ScheduledTickView tickView,
			BlockPos pos,
			Direction direction,
			BlockPos neighborPos,
			BlockState neighborState,
			Random random
	) {
		if (state.get(WATERLOGGED)) {
			tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		}

		return direction == Direction.DOWN
		       ? state.with(SIGNAL_FIRE, this.isSignalFireBaseBlock(neighborState))
		       : super.getStateForNeighborUpdate(
				       state,
				       world,
				       tickView,
				       pos,
				       direction,
				       neighborPos,
				       neighborState,
				       random
		       );
	}

	private boolean isSignalFireBaseBlock(BlockState state) {
		return state.isOf(Blocks.HAY_BLOCK);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (state.get(LIT) == false) {
			return;
		}

		if (random.nextInt(10) == 0) {
			world.playSoundClient(
					pos.getX() + 0.5,
					pos.getY() + 0.5,
					pos.getZ() + 0.5,
					SoundEvents.BLOCK_CAMPFIRE_CRACKLE,
					SoundCategory.BLOCKS,
					0.5F + random.nextFloat(),
					random.nextFloat() * 0.7F + 0.6F,
					false
			);
		}

		if (emitsParticles && random.nextInt(5) == 0) {
			int lavaParticleCount = random.nextInt(1) + 1;
			for (int particle = 0; particle < lavaParticleCount; particle++) {
				world.addParticleClient(
						ParticleTypes.LAVA,
						pos.getX() + 0.5,
						pos.getY() + 0.5,
						pos.getZ() + 0.5,
						random.nextFloat() / 2.0F,
						5.0E-5,
						random.nextFloat() / 2.0F
				);
			}
		}
	}

	/**
	 * Гасит костёр: на клиенте спавнит 20 частиц дыма, на сервере — испускает игровое событие.
	 */
	public static void extinguish(@Nullable Entity entity, WorldAccess world, BlockPos pos, BlockState state) {
		if (world.isClient()) {
			for (int smoke = 0; smoke < 20; smoke++) {
				spawnSmokeParticle((World) world, pos, state.get(SIGNAL_FIRE), true);
			}
		}

		world.emitGameEvent(entity, GameEvent.BLOCK_CHANGE, pos);
	}

	@Override
	public boolean tryFillWithFluid(WorldAccess world, BlockPos pos, BlockState state, FluidState fluidState) {
		if (state.get(Properties.WATERLOGGED) || fluidState.getFluid() != Fluids.WATER) {
			return false;
		}

		if (state.get(LIT)) {
			if (world.isClient() == false) {
				world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.BLOCKS, 1.0F, 1.0F);
			}

			extinguish(null, world, pos, state);
		}

		world.setBlockState(pos, state.with(WATERLOGGED, true).with(LIT, false), 3);
		world.scheduleFluidTick(pos, fluidState.getFluid(), fluidState.getFluid().getTickRate(world));
		return true;
	}

	@Override
	protected void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
		BlockPos hitPos = hit.getBlockPos();
		if (world instanceof ServerWorld serverWorld
				&& projectile.isOnFire()
				&& projectile.canModifyAt(serverWorld, hitPos)
				&& state.get(LIT) == false
				&& state.get(WATERLOGGED) == false
		) {
			world.setBlockState(hitPos, state.with(Properties.LIT, true), 11);
		}
	}

	/**
	 * Спавнит частицы дыма над костром. При {@code lotsOfSmoke = true} добавляет дополнительную
	 * частицу обычного дыма — используется при тушении.
	 */
	public static void spawnSmokeParticle(World world, BlockPos pos, boolean isSignal, boolean lotsOfSmoke) {
		Random random = world.getRandom();
		SimpleParticleType smokeType = isSignal ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE;
		world.addImportantParticleClient(
				smokeType,
				true,
				pos.getX() + 0.5 + random.nextDouble() / 3.0 * (random.nextBoolean() ? 1 : -1),
				pos.getY() + random.nextDouble() + random.nextDouble(),
				pos.getZ() + 0.5 + random.nextDouble() / 3.0 * (random.nextBoolean() ? 1 : -1),
				0.0,
				0.07,
				0.0
		);

		if (lotsOfSmoke) {
			world.addParticleClient(
					ParticleTypes.SMOKE,
					pos.getX() + 0.5 + random.nextDouble() / 4.0 * (random.nextBoolean() ? 1 : -1),
					pos.getY() + 0.4,
					pos.getZ() + 0.5 + random.nextDouble() / 4.0 * (random.nextBoolean() ? 1 : -1),
					0.0,
					0.005,
					0.0
			);
		}
	}

	/**
	 * Проверяет, есть ли горящий костёр в пределах {@link #MAX_CAMPFIRE_SEARCH_DEPTH} блоков ниже.
	 * Учитывает блоки с коллизией, совпадающей с формой дымохода {@link #SMOKEY_SHAPE}.
	 */
	public static boolean isLitCampfireInRange(World world, BlockPos pos) {
		for (int depth = 1; depth <= MAX_CAMPFIRE_SEARCH_DEPTH; depth++) {
			BlockPos checkPos = pos.down(depth);
			BlockState checkState = world.getBlockState(checkPos);

			if (isLitCampfire(checkState)) {
				return true;
			}

			boolean blockedByShape = VoxelShapes.matchesAnywhere(
					SMOKEY_SHAPE,
					checkState.getCollisionShape(world, pos, ShapeContext.absent()),
					BooleanBiFunction.AND
			);

			if (blockedByShape) {
				return isLitCampfire(world.getBlockState(checkPos.down()));
			}
		}

		return false;
	}

	public static boolean isLitCampfire(BlockState state) {
		return state.contains(LIT) && state.isIn(BlockTags.CAMPFIRES) && state.get(LIT);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(LIT, SIGNAL_FIRE, WATERLOGGED, FACING);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new CampfireBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
			World world,
			BlockState state,
			BlockEntityType<T> type
	) {
		if (world instanceof ServerWorld serverWorld) {
			if (state.get(LIT)) {
				ServerRecipeManager.MatchGetter<SingleStackRecipeInput, CampfireCookingRecipe> matchGetter =
						ServerRecipeManager.createCachedMatchGetter(RecipeType.CAMPFIRE_COOKING);
				return validateTicker(
						type,
						BlockEntityType.CAMPFIRE,
						(tickWorld, pos, tickState, blockEntity) -> CampfireBlockEntity.litServerTick(
								serverWorld,
								pos,
								tickState,
								blockEntity,
								matchGetter
						)
				);
			}

			return validateTicker(type, BlockEntityType.CAMPFIRE, CampfireBlockEntity::unlitServerTick);
		}

		return state.get(LIT)
				? validateTicker(type, BlockEntityType.CAMPFIRE, CampfireBlockEntity::clientTick)
				: null;
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}

	/**
	 * Возвращает {@code true}, если костёр можно поджечь: он принадлежит тегу {@code CAMPFIRES},
	 * содержит свойства {@code WATERLOGGED} и {@code LIT}, и при этом не залит водой и не горит.
	 */
	public static boolean canBeLit(BlockState state) {
		return state.isIn(BlockTags.CAMPFIRES, s -> s.contains(WATERLOGGED) && s.contains(LIT))
				&& state.get(WATERLOGGED) == false
				&& state.get(LIT) == false;
	}
}

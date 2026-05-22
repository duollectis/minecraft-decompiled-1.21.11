package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.entity.*;
import net.minecraft.block.enums.Orientation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeCache;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Блок-крафтер: автоматически выполняет крафт при получении сигнала редстоуна.
 * Результат крафта выбрасывается в направлении грани {@code ORIENTATION} или
 * передаётся в соседний инвентарь. Поддерживает компаратор для отображения заполненности.
 */
public class CrafterBlock extends BlockWithEntity {

	public static final MapCodec<CrafterBlock> CODEC = createCodec(CrafterBlock::new);
	public static final BooleanProperty CRAFTING = Properties.CRAFTING;
	public static final BooleanProperty TRIGGERED = Properties.TRIGGERED;
	private static final EnumProperty<Orientation> ORIENTATION = Properties.ORIENTATION;
	private static final int CRAFTING_TICKS = 6;
	private static final int TRIGGER_DELAY = 4;
	private static final RecipeCache RECIPE_CACHE = new RecipeCache(10);
	private static final int PLAYER_SEARCH_RADIUS = 17;
	/** Смещение точки выброса предмета от центра блока в направлении выхода. */
	private static final double EJECT_OFFSET = 0.7;

	public CrafterBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager
				.getDefaultState()
				.with(ORIENTATION, Orientation.NORTH_UP)
				.with(TRIGGERED, false)
				.with(CRAFTING, false));
	}

	@Override
	protected MapCodec<CrafterBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected boolean hasComparatorOutput(BlockState state) {
		return true;
	}

	@Override
	protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
		return world.getBlockEntity(pos) instanceof CrafterBlockEntity crafterBlockEntity
		       ? crafterBlockEntity.getComparatorOutput() : 0;
	}

	@Override
	protected void neighborUpdate(
			BlockState state,
			World world,
			BlockPos pos,
			Block sourceBlock,
			@Nullable WireOrientation wireOrientation,
			boolean notify
	) {
		boolean powered = world.isReceivingRedstonePower(pos);
		boolean wasTriggered = state.get(TRIGGERED);
		BlockEntity blockEntity = world.getBlockEntity(pos);

		if (powered && !wasTriggered) {
			world.scheduleBlockTick(pos, this, TRIGGER_DELAY);
			world.setBlockState(pos, state.with(TRIGGERED, true), Block.NOTIFY_LISTENERS);
			setTriggered(blockEntity, true);
		} else if (!powered && wasTriggered) {
			world.setBlockState(pos, state.with(TRIGGERED, false).with(CRAFTING, false), Block.NOTIFY_LISTENERS);
			setTriggered(blockEntity, false);
		}
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		craft(state, world, pos);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
			World world,
			BlockState state,
			BlockEntityType<T> type
	) {
		return world.isClient() ? null
		                        : validateTicker(type, BlockEntityType.CRAFTER, CrafterBlockEntity::tickCrafting);
	}

	private void setTriggered(@Nullable BlockEntity blockEntity, boolean triggered) {
		if (blockEntity instanceof CrafterBlockEntity crafterBlockEntity) {
			crafterBlockEntity.setTriggered(triggered);
		}
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		CrafterBlockEntity crafterBlockEntity = new CrafterBlockEntity(pos, state);
		crafterBlockEntity.setTriggered(state.contains(TRIGGERED) && state.get(TRIGGERED));
		return crafterBlockEntity;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		Direction facing = ctx.getPlayerLookDirection().getOpposite();

		Direction secondary = switch (facing) {
			case DOWN -> ctx.getHorizontalPlayerFacing().getOpposite();
			case UP -> ctx.getHorizontalPlayerFacing();
			case NORTH, SOUTH, WEST, EAST -> Direction.UP;
		};

		return getDefaultState()
				.with(ORIENTATION, Orientation.byDirections(facing, secondary))
				.with(TRIGGERED, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()));
	}

	@Override
	public void onPlaced(
			World world,
			BlockPos pos,
			BlockState state,
			@Nullable LivingEntity placer,
			ItemStack itemStack
	) {
		if (state.get(TRIGGERED)) {
			world.scheduleBlockTick(pos, this, TRIGGER_DELAY);
		}
	}

	@Override
	protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
		ItemScatterer.onStateReplaced(state, world, pos);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!world.isClient() && world.getBlockEntity(pos) instanceof CrafterBlockEntity crafterBlockEntity) {
			player.openHandledScreen(crafterBlockEntity);
		}

		return ActionResult.SUCCESS;
	}

	/**
	 * Выполняет крафт: ищет подходящий рецепт, создаёт результат и передаёт/выбрасывает
	 * его и остатки рецепта. При отсутствии рецепта или пустом результате воспроизводит
	 * звук неудачи (syncWorldEvent 1050).
	 */
	protected void craft(BlockState state, ServerWorld world, BlockPos pos) {
		if (!(world.getBlockEntity(pos) instanceof CrafterBlockEntity crafter)) {
			return;
		}

		CraftingRecipeInput recipeInput = crafter.createRecipeInput();
		Optional<RecipeEntry<CraftingRecipe>> recipeOptional = getCraftingRecipe(world, recipeInput);

		if (recipeOptional.isEmpty()) {
			world.syncWorldEvent(1050, pos, 0);
			return;
		}

		RecipeEntry<CraftingRecipe> recipeEntry = recipeOptional.get();
		ItemStack result = recipeEntry.value().craft(recipeInput, world.getRegistryManager());

		if (result.isEmpty()) {
			world.syncWorldEvent(1050, pos, 0);
			return;
		}

		crafter.setCraftingTicksRemaining(CRAFTING_TICKS);
		world.setBlockState(pos, state.with(CRAFTING, true), Block.NOTIFY_LISTENERS);
		result.onCraftByCrafter(world);
		transferOrSpawnStack(world, pos, crafter, result, state, recipeEntry);

		for (ItemStack remainder : recipeEntry.value().getRecipeRemainders(recipeInput)) {
			if (!remainder.isEmpty()) {
				transferOrSpawnStack(world, pos, crafter, remainder, state, recipeEntry);
			}
		}

		crafter.getHeldStacks().forEach(stack -> {
			if (!stack.isEmpty()) {
				stack.decrement(1);
			}
		});

		crafter.markDirty();
	}

	public static Optional<RecipeEntry<CraftingRecipe>> getCraftingRecipe(
			ServerWorld world,
			CraftingRecipeInput input
	) {
		return RECIPE_CACHE.getRecipe(world, input);
	}

	private void transferOrSpawnStack(
			ServerWorld world,
			BlockPos pos,
			CrafterBlockEntity blockEntity,
			ItemStack stack,
			BlockState state,
			RecipeEntry<?> recipe
	) {
		Direction facing = state.get(ORIENTATION).getFacing();
		Inventory inventory = HopperBlockEntity.getInventoryAt(world, pos.offset(facing));
		ItemStack remaining = stack.copy();

		if (inventory != null && (inventory instanceof CrafterBlockEntity || stack.getCount() > inventory.getMaxCount(stack))) {
			// Передаём по одному предмету, чтобы обойти ограничение стека у соседнего крафтера
			while (!remaining.isEmpty()) {
				ItemStack single = remaining.copyWithCount(1);
				ItemStack rejected = HopperBlockEntity.transfer(blockEntity, inventory, single, facing.getOpposite());
				if (!rejected.isEmpty()) {
					break;
				}

				remaining.decrement(1);
			}
		} else if (inventory != null) {
			while (!remaining.isEmpty()) {
				int prevCount = remaining.getCount();
				remaining = HopperBlockEntity.transfer(blockEntity, inventory, remaining, facing.getOpposite());
				if (prevCount == remaining.getCount()) {
					break;
				}
			}
		}

		if (!remaining.isEmpty()) {
			Vec3d center = Vec3d.ofCenter(pos);
			Vec3d ejectPoint = center.offset(facing, EJECT_OFFSET);
			ItemDispenserBehavior.spawnItem(world, remaining, CRAFTING_TICKS, facing, ejectPoint);

			double searchDiameter = PLAYER_SEARCH_RADIUS * 2.0;
			for (ServerPlayerEntity player : world.getNonSpectatingEntities(
					ServerPlayerEntity.class,
					Box.of(center, searchDiameter, searchDiameter, searchDiameter)
			)) {
				Criteria.CRAFTER_RECIPE_CRAFTED.trigger(player, recipe.id(), blockEntity.getHeldStacks());
			}

			world.syncWorldEvent(1049, pos, 0);
			world.syncWorldEvent(2010, pos, facing.getIndex());
		}
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(
				ORIENTATION,
				rotation.getDirectionTransformation().mapJigsawOrientation(state.get(ORIENTATION))
		);
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.with(
				ORIENTATION,
				mirror.getDirectionTransformation().mapJigsawOrientation(state.get(ORIENTATION))
		);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(ORIENTATION, TRIGGERED, CRAFTING);
	}
}

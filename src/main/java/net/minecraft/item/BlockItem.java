package net.minecraft.item;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.command.DefaultPermissions;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TypedEntityData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Предмет, представляющий блок в инвентаре. Обеспечивает размещение блока в мире
 * при использовании на поверхности другого блока.
 * <p>Поддерживает копирование компонентов предмета в блок-сущность при размещении,
 * а также предупреждения оператора для блоков с потенциально опасными командами.</p>
 */
public class BlockItem extends Item {

	@Deprecated
	private final Block block;

	public BlockItem(Block block, Item.Settings settings) {
		super(settings);
		this.block = block;
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		ActionResult actionResult = place(new ItemPlacementContext(context));
		return !actionResult.isAccepted() && context.getStack().contains(DataComponentTypes.CONSUMABLE)
		       ? super.use(context.getWorld(), context.getPlayer(), context.getHand())
		       : actionResult;
	}

	/**
	 * Размещает блок в мире по контексту размещения.
	 * <p>Выполняет полную цепочку: проверка фич → получение состояния → размещение →
	 * копирование NBT → уведомление блока → звук → игровое событие.</p>
	 *
	 * @param context контекст размещения предмета
	 * @return результат размещения
	 */
	public ActionResult place(ItemPlacementContext context) {
		if (!getBlock().isEnabled(context.getWorld().getEnabledFeatures())) {
			return ActionResult.FAIL;
		}

		if (!context.canPlace()) {
			return ActionResult.FAIL;
		}

		ItemPlacementContext placementContext = getPlacementContext(context);
		if (placementContext == null) {
			return ActionResult.FAIL;
		}

		BlockState blockState = getPlacementState(placementContext);
		if (blockState == null) {
			return ActionResult.FAIL;
		}

		if (!place(placementContext, blockState)) {
			return ActionResult.FAIL;
		}

		BlockPos blockPos = placementContext.getBlockPos();
		World world = placementContext.getWorld();
		PlayerEntity player = placementContext.getPlayer();
		ItemStack itemStack = placementContext.getStack();
		BlockState placedState = world.getBlockState(blockPos);

		if (placedState.isOf(blockState.getBlock())) {
			placedState = placeFromNbt(blockPos, world, itemStack, placedState);
			postPlacement(blockPos, world, player, itemStack, placedState);
			copyComponentsToBlockEntity(world, blockPos, itemStack);
			placedState.getBlock().onPlaced(world, blockPos, placedState, player, itemStack);

			if (player instanceof ServerPlayerEntity serverPlayer) {
				Criteria.PLACED_BLOCK.trigger(serverPlayer, blockPos, itemStack);
			}
		}

		BlockSoundGroup soundGroup = placedState.getSoundGroup();
		world.playSound(
				player,
				blockPos,
				getPlaceSound(placedState),
				SoundCategory.BLOCKS,
				(soundGroup.getVolume() + 1.0F) / 2.0F,
				soundGroup.getPitch() * 0.8F
		);
		world.emitGameEvent(GameEvent.BLOCK_PLACE, blockPos, GameEvent.Emitter.of(player, placedState));
		itemStack.decrementUnlessCreative(1, player);
		return ActionResult.SUCCESS;
	}

	protected SoundEvent getPlaceSound(BlockState state) {
		return state.getSoundGroup().getPlaceSound();
	}

	public @Nullable ItemPlacementContext getPlacementContext(ItemPlacementContext context) {
		return context;
	}

	private static void copyComponentsToBlockEntity(World world, BlockPos pos, ItemStack stack) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null) {
			blockEntity.readComponents(stack);
			blockEntity.markDirty();
		}
	}

	protected boolean postPlacement(
			BlockPos pos,
			World world,
			@Nullable PlayerEntity player,
			ItemStack stack,
			BlockState state
	) {
		return writeNbtToBlockEntity(world, player, pos, stack);
	}

	protected @Nullable BlockState getPlacementState(ItemPlacementContext context) {
		BlockState blockState = getBlock().getPlacementState(context);
		return blockState != null && canPlace(context, blockState) ? blockState : null;
	}

	/**
	 * Применяет компонент {@code BLOCK_STATE} из стека к уже размещённому блоку.
	 * <p>Позволяет сохранять состояние блока (например, направление) при размещении
	 * из инвентаря с сохранёнными данными.</p>
	 */
	private BlockState placeFromNbt(BlockPos pos, World world, ItemStack stack, BlockState state) {
		BlockStateComponent blockStateComponent =
				stack.getOrDefault(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT);

		if (blockStateComponent.isEmpty()) {
			return state;
		}

		BlockState updatedState = blockStateComponent.applyToState(state);
		if (updatedState != state) {
			world.setBlockState(pos, updatedState, 2);
		}

		return updatedState;
	}

	/**
	 * Проверяет, можно ли разместить блок в данном состоянии на данной позиции.
	 *
	 * @param context контекст размещения
	 * @param state   состояние блока для размещения
	 * @return {@code true} если размещение допустимо
	 */
	protected boolean canPlace(ItemPlacementContext context, BlockState state) {
		PlayerEntity player = context.getPlayer();
		return (!checkStatePlacement() || state.canPlaceAt(context.getWorld(), context.getBlockPos()))
				&& context.getWorld().canPlace(state, context.getBlockPos(), ShapeContext.ofPlacement(player));
	}

	protected boolean checkStatePlacement() {
		return true;
	}

	protected boolean place(ItemPlacementContext context, BlockState state) {
		return context.getWorld().setBlockState(context.getBlockPos(), state, Block.NOTIFY_ALL_AND_REDRAW);
	}

	/**
	 * Записывает NBT-данные из компонента {@code BLOCK_ENTITY_DATA} стека в блок-сущность.
	 * <p>Запись разрешена только если тип блок-сущности совпадает и либо блок не может
	 * выполнять команды, либо игрок является оператором уровня 2.</p>
	 *
	 * @param world  мир
	 * @param player игрок или {@code null}
	 * @param pos    позиция блока
	 * @param stack  стек предмета с данными
	 * @return {@code true} если данные были успешно записаны
	 */
	public static boolean writeNbtToBlockEntity(
			World world,
			@Nullable PlayerEntity player,
			BlockPos pos,
			ItemStack stack
	) {
		if (world.isClient()) {
			return false;
		}

		TypedEntityData<BlockEntityType<?>> blockEntityData = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
		if (blockEntityData == null) {
			return false;
		}

		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity == null) {
			return false;
		}

		BlockEntityType<?> blockEntityType = blockEntity.getType();
		if (blockEntityType != blockEntityData.getType()) {
			return false;
		}

		if (!blockEntityType.canPotentiallyExecuteCommands()
				|| player != null && player.isCreativeLevelTwoOp()) {
			return blockEntityData.applyToBlockEntity(blockEntity, world.getRegistryManager());
		}

		return false;
	}

	@Override
	public boolean shouldShowOperatorBlockWarnings(ItemStack stack, @Nullable PlayerEntity player) {
		if (player == null || !player.getPermissions().hasPermission(DefaultPermissions.GAMEMASTERS)) {
			return false;
		}

		TypedEntityData<BlockEntityType<?>> blockEntityData = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
		return blockEntityData != null && blockEntityData.getType().canPotentiallyExecuteCommands();
	}

	public Block getBlock() {
		return block;
	}

	public void appendBlocks(Map<Block, Item> map, Item item) {
		map.put(getBlock(), item);
	}

	@Override
	public boolean canBeNested() {
		return !(getBlock() instanceof ShulkerBoxBlock);
	}

	@Override
	public void onItemEntityDestroyed(ItemEntity entity) {
		ContainerComponent container =
				entity.getStack().set(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
		if (container != null) {
			ItemUsage.spawnItemContents(entity, container.iterateNonEmptyCopy());
		}
	}

	public static void setBlockEntityData(ItemStack stack, BlockEntityType<?> type, NbtWriteView view) {
		view.remove("id");
		if (view.isEmpty()) {
			stack.remove(DataComponentTypes.BLOCK_ENTITY_DATA);
		} else {
			BlockEntity.writeId(view, type);
			stack.set(DataComponentTypes.BLOCK_ENTITY_DATA, TypedEntityData.create(type, view.getNbt()));
		}
	}

	@Override
	public FeatureSet getRequiredFeatures() {
		return getBlock().getRequiredFeatures();
	}
}

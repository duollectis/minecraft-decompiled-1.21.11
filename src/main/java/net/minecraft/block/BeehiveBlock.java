package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.rule.GameRules;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Блок улья/гнезда пчёл. Хранит до трёх пчёл внутри и накапливает мёд (уровень 0–5).
 * При достижении максимального уровня мёда ({@link #FULL_HONEY_LEVEL}) позволяет
 * собрать соты ножницами или наполнить бутылку мёдом. Если рядом нет костра —
 * тревожит пчёл при сборе урожая. При разрушении или взрыве выпускает пчёл в режиме
 * атаки на ближайших игроков.
 */
public class BeehiveBlock extends BlockWithEntity {

	public static final MapCodec<BeehiveBlock> CODEC = createCodec(BeehiveBlock::new);
	public static final EnumProperty<Direction> FACING = HorizontalFacingBlock.FACING;
	public static final IntProperty HONEY_LEVEL = Properties.HONEY_LEVEL;
	public static final int FULL_HONEY_LEVEL = 5;

	@Override
	public MapCodec<BeehiveBlock> getCodec() {
		return CODEC;
	}

	public BeehiveBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager.getDefaultState().with(HONEY_LEVEL, 0).with(FACING, Direction.NORTH));
	}

	@Override
	protected boolean hasComparatorOutput(BlockState state) {
		return true;
	}

	@Override
	protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
		return state.get(HONEY_LEVEL);
	}

	/**
	 * Вызывается после разрушения блока игроком. Если инструмент не имеет зачарования
	 * «Шёлковое касание для пчёл» — выпускает пчёл в режиме атаки и тревожит ближайших.
	 * Всегда активирует критерий достижения уничтожения гнезда.
	 */
	@Override
	public void afterBreak(
			World world,
			PlayerEntity player,
			BlockPos pos,
			BlockState state,
			@Nullable BlockEntity blockEntity,
			ItemStack tool
	) {
		super.afterBreak(world, player, pos, state, blockEntity, tool);

		if (world.isClient() || blockEntity instanceof BeehiveBlockEntity == false) {
			return;
		}

		BeehiveBlockEntity beehive = (BeehiveBlockEntity) blockEntity;

		if (EnchantmentHelper.hasAnyEnchantmentsIn(tool, EnchantmentTags.PREVENTS_BEE_SPAWNS_WHEN_MINING) == false) {
			beehive.angerBees(player, state, BeehiveBlockEntity.BeeState.EMERGENCY);
			ItemScatterer.onStateReplaced(state, world, pos);
			angerNearbyBees(world, pos);
		}

		Criteria.BEE_NEST_DESTROYED.trigger(
				(ServerPlayerEntity) player,
				state,
				tool,
				beehive.getBeeCount()
		);
	}

	@Override
	protected void onExploded(
			BlockState state,
			ServerWorld world,
			BlockPos pos,
			Explosion explosion,
			BiConsumer<ItemStack, BlockPos> stackMerger
	) {
		super.onExploded(state, world, pos, explosion, stackMerger);
		angerNearbyBees(world, pos);
	}

	private void angerNearbyBees(World world, BlockPos pos) {
		Box searchBox = new Box(pos).expand(8.0, 6.0, 8.0);
		List<BeeEntity> nearbyBees = world.getNonSpectatingEntities(BeeEntity.class, searchBox);

		if (nearbyBees.isEmpty()) {
			return;
		}

		List<PlayerEntity> nearbyPlayers = world.getNonSpectatingEntities(PlayerEntity.class, searchBox);

		if (nearbyPlayers.isEmpty()) {
			return;
		}

		for (BeeEntity bee : nearbyBees) {
			if (bee.getTarget() == null) {
				bee.setTarget(Util.getRandom(nearbyPlayers, world.random));
			}
		}
	}

	public static void dropHoneycomb(
			ServerWorld world,
			ItemStack tool,
			BlockState state,
			@Nullable BlockEntity blockEntity,
			@Nullable Entity interactingEntity,
			BlockPos pos
	) {
		generateBlockInteractLoot(
				world,
				LootTables.BEEHIVE_HARVEST,
				state,
				blockEntity,
				tool,
				interactingEntity,
				(worldx, stack) -> dropStack(worldx, pos, stack)
		);
	}

	/**
	 * Обрабатывает взаимодействие с предметом в руке. При полном улье (уровень ≥ 5):
	 * ножницы срезают соты, стеклянная бутылка наполняется мёдом. Если рядом нет
	 * горящего костра — тревожит пчёл при сборе урожая.
	 */
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
		int honeyLevel = state.get(HONEY_LEVEL);

		if (honeyLevel < FULL_HONEY_LEVEL) {
			return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
		}

		Item usedItem = stack.getItem();
		boolean harvested = false;

		if (world instanceof ServerWorld serverWorld && stack.isOf(Items.SHEARS)) {
			dropHoneycomb(serverWorld, stack, state, world.getBlockEntity(pos), player, pos);
			world.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.BLOCK_BEEHIVE_SHEAR, SoundCategory.BLOCKS, 1.0F, 1.0F);
			stack.damage(1, player, hand.getEquipmentSlot());
			world.emitGameEvent(player, GameEvent.SHEAR, pos);
			harvested = true;
		} else if (stack.isOf(Items.GLASS_BOTTLE)) {
			stack.decrement(1);
			world.playSound(player, player.getX(), player.getY(), player.getZ(),
					SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);

			ItemStack honeyBottle = new ItemStack(Items.HONEY_BOTTLE);

			if (stack.isEmpty()) {
				player.setStackInHand(hand, honeyBottle);
			} else if (player.getInventory().insertStack(honeyBottle) == false) {
				player.dropItem(honeyBottle, false);
			}

			world.emitGameEvent(player, GameEvent.FLUID_PICKUP, pos);
			harvested = true;
		}

		if (harvested == false) {
			return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
		}

		if (world.isClient() == false) {
			player.incrementStat(Stats.USED.getOrCreateStat(usedItem));
		}

		if (CampfireBlock.isLitCampfireInRange(world, pos)) {
			takeHoney(world, state, pos);
		} else {
			if (hasBees(world, pos)) {
				angerNearbyBees(world, pos);
			}

			takeHoney(world, state, pos, player, BeehiveBlockEntity.BeeState.EMERGENCY);
		}

		return ActionResult.SUCCESS;
	}

	private boolean hasBees(World world, BlockPos pos) {
		return world.getBlockEntity(pos) instanceof BeehiveBlockEntity beehive
				? beehive.hasNoBees() == false
				: false;
	}

	public void takeHoney(
			World world,
			BlockState state,
			BlockPos pos,
			@Nullable PlayerEntity player,
			BeehiveBlockEntity.BeeState beeState
	) {
		takeHoney(world, state, pos);

		if (world.getBlockEntity(pos) instanceof BeehiveBlockEntity beehive) {
			beehive.angerBees(player, state, beeState);
		}
	}

	public void takeHoney(World world, BlockState state, BlockPos pos) {
		world.setBlockState(pos, state.with(HONEY_LEVEL, 0), 3);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (state.get(HONEY_LEVEL) < FULL_HONEY_LEVEL) {
			return;
		}

		int particleCount = random.nextInt(1) + 1;

		for (int step = 0; step < particleCount; step++) {
			spawnHoneyParticles(world, pos, state);
		}
	}

	private void spawnHoneyParticles(World world, BlockPos pos, BlockState state) {
		if (state.getFluidState().isEmpty() == false || world.random.nextFloat() < 0.3F) {
			return;
		}

		VoxelShape shape = state.getCollisionShape(world, pos);
		double topY = shape.getMax(Direction.Axis.Y);

		if (topY < 1.0 || state.isIn(BlockTags.IMPERMEABLE)) {
			return;
		}

		double bottomY = shape.getMin(Direction.Axis.Y);

		if (bottomY > 0.0) {
			addHoneyParticle(world, pos, shape, pos.getY() + bottomY - 0.05);
			return;
		}

		BlockPos belowPos = pos.down();
		BlockState belowState = world.getBlockState(belowPos);
		VoxelShape belowShape = belowState.getCollisionShape(world, belowPos);
		double belowTopY = belowShape.getMax(Direction.Axis.Y);

		if ((belowTopY < 1.0 || belowState.isFullCube(world, belowPos) == false)
				&& belowState.getFluidState().isEmpty()
		) {
			addHoneyParticle(world, pos, shape, pos.getY() - 0.05);
		}
	}

	private void addHoneyParticle(World world, BlockPos pos, VoxelShape shape, double height) {
		addHoneyParticle(
				world,
				pos.getX() + shape.getMin(Direction.Axis.X),
				pos.getX() + shape.getMax(Direction.Axis.X),
				pos.getZ() + shape.getMin(Direction.Axis.Z),
				pos.getZ() + shape.getMax(Direction.Axis.Z),
				height
		);
	}

	private void addHoneyParticle(World world, double minX, double maxX, double minZ, double maxZ, double height) {
		world.addParticleClient(
				ParticleTypes.DRIPPING_HONEY,
				MathHelper.lerp(world.random.nextDouble(), minX, maxX),
				height,
				MathHelper.lerp(world.random.nextDouble(), minZ, maxZ),
				0.0,
				0.0,
				0.0
		);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(HONEY_LEVEL, FACING);
	}

	@Override
	public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BeehiveBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
			World world,
			BlockState state,
			BlockEntityType<T> type
	) {
		return world.isClient() ? null : validateTicker(type, BlockEntityType.BEEHIVE, BeehiveBlockEntity::serverTick);
	}

	/**
	 * Вызывается при начале разрушения блока. Если игрок в режиме выживания и включены
	 * дропы — вручную спавнит предмет улья с сохранёнными данными пчёл и уровнем мёда,
	 * чтобы не потерять содержимое при разрушении.
	 */
	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (world instanceof ServerWorld serverWorld
				&& player.shouldSkipBlockDrops()
				&& serverWorld.getGameRules().getValue(GameRules.DO_TILE_DROPS)
				&& world.getBlockEntity(pos) instanceof BeehiveBlockEntity beehive
		) {
			int honeyLevel = state.get(HONEY_LEVEL);
			boolean hasBees = beehive.hasNoBees() == false;

			if (hasBees || honeyLevel > 0) {
				ItemStack drop = new ItemStack(this);
				drop.applyComponentsFrom(beehive.createComponentMap());
				drop.set(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT.with(HONEY_LEVEL, honeyLevel));

				ItemEntity itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), drop);
				itemEntity.setToDefaultPickupDelay();
				world.spawnEntity(itemEntity);
			}
		}

		return super.onBreak(world, pos, state, player);
	}

	@Override
	protected List<ItemStack> getDroppedStacks(BlockState state, LootWorldContext.Builder builder) {
		Entity entity = builder.getOptional(LootContextParameters.THIS_ENTITY);

		boolean destroyedByExplosion = entity instanceof TntEntity
				|| entity instanceof CreeperEntity
				|| entity instanceof WitherSkullEntity
				|| entity instanceof WitherEntity
				|| entity instanceof TntMinecartEntity;

		if (destroyedByExplosion
				&& builder.getOptional(LootContextParameters.BLOCK_ENTITY) instanceof BeehiveBlockEntity beehive
		) {
			beehive.angerBees(null, state, BeehiveBlockEntity.BeeState.EMERGENCY);
		}

		return super.getDroppedStacks(state, builder);
	}

	@Override
	protected ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
		ItemStack stack = super.getPickStack(world, pos, state, includeData);

		if (includeData) {
			stack.set(
					DataComponentTypes.BLOCK_STATE,
					BlockStateComponent.DEFAULT.with(HONEY_LEVEL, state.get(HONEY_LEVEL))
			);
		}

		return stack;
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
		if (world.getBlockState(neighborPos).getBlock() instanceof FireBlock
				&& world.getBlockEntity(pos) instanceof BeehiveBlockEntity beehive
		) {
			beehive.angerBees(null, state, BeehiveBlockEntity.BeeState.EMERGENCY);
		}

		return super.getStateForNeighborUpdate(
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

	@Override
	public BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}
}

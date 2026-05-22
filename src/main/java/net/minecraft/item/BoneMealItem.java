package net.minecraft.item;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DeadCoralWallFanBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ParticleUtil;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

/**
 * Предмет костной муки. Ускоряет рост растений и распространяет кораллы
 * при использовании на воде в биомах с коралловыми рифами.
 */
public class BoneMealItem extends Item {

	public static final int CORAL_SPREAD_RADIUS = 3;
	public static final int CORAL_SPREAD_STEP = 1;
	public static final int CORAL_SPREAD_RANGE = 3;

	/** Количество итераций при распространении подводной растительности. */
	private static final int UNDERWATER_SPREAD_ITERATIONS = 128;
	/** Делитель для вычисления количества шагов случайного блуждания. */
	private static final int WALK_STEP_DIVISOR = 16;
	/** Флаг обновления блока при установке кораллов. */
	private static final int BLOCK_UPDATE_FLAGS = 3;
	/** Шанс (1 из N) вырастить подводное растение вместо морской травы. */
	private static final int UNDERWATER_PLANT_CHANCE = 4;
	/** Шанс (1 из N) вырастить морскую траву при наличии удобренной морской травы. */
	private static final int SEAGRASS_GROW_CHANCE = 10;
	/** Уровень жидкости, соответствующий полному источнику воды. */
	private static final int FULL_WATER_LEVEL = 8;
	/** Код мирового события для эффекта частиц костной муки. */
	private static final int BONEMEAL_PARTICLE_EVENT = 1505;
	/** Цвет частиц для эффекта костной муки. */
	private static final int BONEMEAL_PARTICLE_COLOR = 15;

	public BoneMealItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos blockPos = context.getBlockPos();
		BlockPos adjacentPos = blockPos.offset(context.getSide());
		ItemStack stack = context.getStack();

		if (useOnFertilizable(stack, world, blockPos)) {
			if (!world.isClient()) {
				stack.emitUseGameEvent(context.getPlayer(), GameEvent.ITEM_INTERACT_FINISH);
				world.syncWorldEvent(BONEMEAL_PARTICLE_EVENT, blockPos, BONEMEAL_PARTICLE_COLOR);
			}

			return ActionResult.SUCCESS;
		}

		BlockState blockState = world.getBlockState(blockPos);
		boolean isSolidSide = blockState.isSideSolidFullSquare(world, blockPos, context.getSide());

		if (isSolidSide && useOnGround(stack, world, adjacentPos, context.getSide())) {
			if (!world.isClient()) {
				stack.emitUseGameEvent(context.getPlayer(), GameEvent.ITEM_INTERACT_FINISH);
				world.syncWorldEvent(BONEMEAL_PARTICLE_EVENT, adjacentPos, BONEMEAL_PARTICLE_COLOR);
			}

			return ActionResult.SUCCESS;
		}

		return ActionResult.PASS;
	}

	/**
	 * Применяет костную муку к удобряемому блоку ({@link Fertilizable}).
	 * На сервере вызывает рост блока и уменьшает стек.
	 *
	 * @param stack стек костной муки
	 * @param world мир
	 * @param pos   позиция блока
	 * @return {@code true}, если блок является удобряемым и принял удобрение
	 */
	public static boolean useOnFertilizable(ItemStack stack, World world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);

		if (!(blockState.getBlock() instanceof Fertilizable fertilizable)
			|| !fertilizable.isFertilizable(world, pos, blockState)
		) {
			return false;
		}

		if (world instanceof ServerWorld serverWorld) {
			if (fertilizable.canGrow(world, world.random, pos, blockState)) {
				fertilizable.grow(serverWorld, world.random, pos, blockState);
			}

			stack.decrement(1);
		}

		return true;
	}

	/**
	 * Применяет костную муку к поверхности воды, распространяя морскую траву и кораллы.
	 * Работает только на полных источниках воды ({@code level == 8}).
	 *
	 * @param stack    стек костной муки
	 * @param world    мир
	 * @param blockPos позиция блока воды
	 * @param facing   направление, с которого применяется костная мука
	 * @return {@code true}, если применение прошло успешно
	 */
	public static boolean useOnGround(ItemStack stack, World world, BlockPos blockPos, @Nullable Direction facing) {
		if (!world.getBlockState(blockPos).isOf(Blocks.WATER)
			|| world.getFluidState(blockPos).getLevel() != FULL_WATER_LEVEL
		) {
			return false;
		}

		if (!(world instanceof ServerWorld serverWorld)) {
			return true;
		}

		Random random = world.getRandom();

		label80:
		for (int iteration = 0; iteration < UNDERWATER_SPREAD_ITERATIONS; iteration++) {
			BlockPos currentPos = blockPos;
			BlockState targetState = Blocks.SEAGRASS.getDefaultState();

			for (int step = 0; step < iteration / WALK_STEP_DIVISOR; step++) {
				currentPos = currentPos.add(
					random.nextInt(3) - 1,
					(random.nextInt(3) - 1) * random.nextInt(3) / 2,
					random.nextInt(3) - 1
				);

				if (world.getBlockState(currentPos).isFullCube(world, currentPos)) {
					continue label80;
				}
			}

			RegistryEntry<Biome> biome = world.getBiome(currentPos);

			if (biome.isIn(BiomeTags.PRODUCES_CORALS_FROM_BONEMEAL)) {
				if (iteration == 0 && facing != null && facing.getAxis().isHorizontal()) {
					targetState = Registries.BLOCK
						.getRandomEntry(BlockTags.WALL_CORALS, world.random)
						.map(entry -> entry.value().getDefaultState())
						.orElse(targetState);

					if (targetState.contains(DeadCoralWallFanBlock.FACING)) {
						targetState = targetState.with(DeadCoralWallFanBlock.FACING, facing);
					}
				} else if (random.nextInt(UNDERWATER_PLANT_CHANCE) == 0) {
					targetState = Registries.BLOCK
						.getRandomEntry(BlockTags.UNDERWATER_BONEMEALS, world.random)
						.map(entry -> entry.value().getDefaultState())
						.orElse(targetState);
				}
			}

			if (targetState.isIn(BlockTags.WALL_CORALS, state -> state.contains(DeadCoralWallFanBlock.FACING))) {
				for (int attempt = 0; !targetState.canPlaceAt(world, currentPos) && attempt < CORAL_SPREAD_RANGE; attempt++) {
					targetState = targetState.with(
						DeadCoralWallFanBlock.FACING,
						Direction.Type.HORIZONTAL.random(random)
					);
				}
			}

			if (!targetState.canPlaceAt(world, currentPos)) {
				continue;
			}

			BlockState existingState = world.getBlockState(currentPos);

			if (existingState.isOf(Blocks.WATER) && world.getFluidState(currentPos).getLevel() == FULL_WATER_LEVEL) {
				world.setBlockState(currentPos, targetState, BLOCK_UPDATE_FLAGS);
			} else if (existingState.isOf(Blocks.SEAGRASS)
				&& ((Fertilizable) Blocks.SEAGRASS).isFertilizable(world, currentPos, existingState)
				&& random.nextInt(SEAGRASS_GROW_CHANCE) == 0
			) {
				((Fertilizable) Blocks.SEAGRASS).grow(serverWorld, random, currentPos, existingState);
			}
		}

		stack.decrement(1);
		return true;
	}

	/**
	 * Создаёт визуальные частицы удобрения вокруг блока.
	 * Тип и количество частиц зависят от типа удобряемого блока.
	 *
	 * @param world мир
	 * @param pos   позиция блока
	 * @param count базовое количество частиц
	 */
	public static void createParticles(WorldAccess world, BlockPos pos, int count) {
		BlockState blockState = world.getBlockState(pos);

		if (blockState.getBlock() instanceof Fertilizable fertilizable) {
			BlockPos particlePos = fertilizable.getFertilizeParticlePos(pos);

			switch (fertilizable.getFertilizableType()) {
				case NEIGHBOR_SPREADER -> ParticleUtil.spawnParticlesAround(
					world, particlePos, count * 3, 3.0, 1.0, false, ParticleTypes.HAPPY_VILLAGER
				);
				case GROWER -> ParticleUtil.spawnParticlesAround(
					world, particlePos, count, ParticleTypes.HAPPY_VILLAGER
				);
			}
		} else if (blockState.isOf(Blocks.WATER)) {
			ParticleUtil.spawnParticlesAround(world, pos, count * 3, 3.0, 1.0, false, ParticleTypes.HAPPY_VILLAGER);
		}
	}
}

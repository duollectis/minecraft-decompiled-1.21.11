package net.minecraft.fluid;

import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.CollisionEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Реализация лавы как текучей жидкости.
 *
 * <p>Лава медленнее воды: в обычном измерении тик каждые 30 игровых тиков,
 * расстояние растекания — 2 блока. В Нижнем мире (fast lava gameplay) — 10 тиков
 * и 4 блока соответственно. При контакте с водой сверху образует камень.
 * При случайных тиках может поджигать соседние горючие блоки.</p>
 */
public abstract class LavaFluid extends FlowableFluid {

	/**
	 * Минимальная высота лавы, при которой она может быть вытеснена водой снизу.
	 * Соответствует уровню 4 из 9 (≈4/9 ≈ 0.4444).
	 */
	public static final float MIN_HEIGHT_TO_REPLACE = 0.44444445F;

	/** Код события синхронизации мира для звука тушения лавы. */
	private static final int EXTINGUISH_WORLD_EVENT = 1501;

	/** Вероятность 1/100 появления частицы лавы и звука «pop» над поверхностью. */
	private static final int LAVA_PARTICLE_CHANCE = 100;

	/** Вероятность 1/200 воспроизведения фонового звука лавы. */
	private static final int LAVA_AMBIENT_SOUND_CHANCE = 200;

	/** Количество попыток поджечь блоки при случайном тике. */
	private static final int FIRE_SPREAD_ATTEMPTS = 3;

	@Override
	public Fluid getFlowing() {
		return Fluids.FLOWING_LAVA;
	}

	@Override
	public Fluid getStill() {
		return Fluids.LAVA;
	}

	@Override
	public Item getBucketItem() {
		return Items.LAVA_BUCKET;
	}

	/**
	 * Воспроизводит визуальные и звуковые эффекты лавы на клиенте.
	 * Частицы и звук «pop» появляются с вероятностью 1/100,
	 * фоновый звук — с вероятностью 1/200.
	 */
	@Override
	public void randomDisplayTick(World world, BlockPos pos, FluidState state, Random random) {
		BlockPos above = pos.up();

		BlockState aboveBlock = world.getBlockState(above);
		if (!aboveBlock.isAir() || aboveBlock.isOpaqueFullCube()) {
			return;
		}

		if (random.nextInt(LAVA_PARTICLE_CHANCE) == 0) {
			double particleX = pos.getX() + random.nextDouble();
			double particleY = pos.getY() + 1.0;
			double particleZ = pos.getZ() + random.nextDouble();

			world.addParticleClient(ParticleTypes.LAVA, particleX, particleY, particleZ, 0.0, 0.0, 0.0);
			world.playSoundClient(
				particleX, particleY, particleZ,
				SoundEvents.BLOCK_LAVA_POP,
				SoundCategory.AMBIENT,
				0.2F + random.nextFloat() * 0.2F,
				0.9F + random.nextFloat() * 0.15F,
				false
			);
		}

		if (random.nextInt(LAVA_AMBIENT_SOUND_CHANCE) == 0) {
			world.playSoundClient(
				pos.getX(), pos.getY(), pos.getZ(),
				SoundEvents.BLOCK_LAVA_AMBIENT,
				SoundCategory.AMBIENT,
				0.2F + random.nextFloat() * 0.2F,
				0.9F + random.nextFloat() * 0.15F,
				false
			);
		}
	}

	/**
	 * При случайном тике лава пытается поджечь соседние блоки.
	 *
	 * <p>Если {@code random.nextInt(3) > 0}: выбирает случайную позицию выше и поджигает воздух
	 * рядом с горючим блоком. Иначе: ищет горючие блоки на том же уровне и ставит огонь сверху.</p>
	 */
	@Override
	public void onRandomTick(ServerWorld world, BlockPos pos, FluidState state, Random random) {
		if (!world.canFireSpread(pos)) {
			return;
		}

		int fireMode = random.nextInt(FIRE_SPREAD_ATTEMPTS);

		if (fireMode > 0) {
			BlockPos firePos = pos;

			for (int attempt = 0; attempt < fireMode; attempt++) {
				firePos = firePos.add(random.nextInt(3) - 1, 1, random.nextInt(3) - 1);

				if (!world.isPosLoaded(firePos)) {
					return;
				}

				BlockState fireBlock = world.getBlockState(firePos);

				if (fireBlock.isAir()) {
					if (canLightFire(world, firePos)) {
						world.setBlockState(firePos, AbstractFireBlock.getState(world, firePos));
						return;
					}
				} else if (fireBlock.blocksMovement()) {
					return;
				}
			}
		} else {
			for (int attempt = 0; attempt < FIRE_SPREAD_ATTEMPTS; attempt++) {
				BlockPos candidate = pos.add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);

				if (!world.isPosLoaded(candidate)) {
					return;
				}

				if (world.isAir(candidate.up()) && hasBurnableBlock(world, candidate)) {
					world.setBlockState(candidate.up(), AbstractFireBlock.getState(world, candidate));
				}
			}
		}
	}

	@Override
	protected void onEntityCollision(World world, BlockPos pos, Entity entity, EntityCollisionHandler handler) {
		handler.addEvent(CollisionEvent.CLEAR_FREEZE);
		handler.addEvent(CollisionEvent.LAVA_IGNITE);
		handler.addPostCallback(CollisionEvent.LAVA_IGNITE, Entity::setOnFireFromLava);
	}

	/** Проверяет, есть ли хотя бы один горючий блок рядом с позицией (во всех 6 направлениях). */
	private boolean canLightFire(WorldView world, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			if (hasBurnableBlock(world, pos.offset(direction))) {
				return true;
			}
		}

		return false;
	}

	private boolean hasBurnableBlock(WorldView world, BlockPos pos) {
		if (!world.isInHeightLimit(pos.getY())) {
			return false;
		}

		return world.isChunkLoaded(pos) && world.getBlockState(pos).isBurnable();
	}

	@Override
	public @Nullable ParticleEffect getParticle() {
		return ParticleTypes.DRIPPING_LAVA;
	}

	@Override
	protected void beforeBreakingBlock(WorldAccess world, BlockPos pos, BlockState state) {
		playExtinguishEvent(world, pos);
	}

	@Override
	public int getMaxFlowDistance(WorldView world) {
		return shouldLavaFlowFaster(world) ? 4 : 2;
	}

	@Override
	public BlockState toBlockState(FluidState state) {
		return Blocks.LAVA.getDefaultState().with(FluidBlock.LEVEL, getBlockStateLevel(state));
	}

	@Override
	public boolean matchesType(Fluid fluid) {
		return fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA;
	}

	@Override
	public int getLevelDecreasePerBlock(WorldView world) {
		return shouldLavaFlowFaster(world) ? 1 : 2;
	}

	/**
	 * Лава может быть вытеснена водой только снизу и только если высота лавы
	 * достаточна (≥ {@link #MIN_HEIGHT_TO_REPLACE}).
	 */
	@Override
	public boolean canBeReplacedWith(
		FluidState state,
		BlockView world,
		BlockPos pos,
		Fluid fluid,
		Direction direction
	) {
		return state.getHeight(world, pos) >= MIN_HEIGHT_TO_REPLACE && fluid.isIn(FluidTags.WATER);
	}

	@Override
	public int getTickRate(WorldView world) {
		return shouldLavaFlowFaster(world) ? 10 : 30;
	}

	/**
	 * Замедляет тик лавы в 4 раза при подъёме уровня (с вероятностью 3/4).
	 * Это имитирует более медленное «заполнение» ямы лавой снизу вверх.
	 */
	@Override
	public int getNextTickDelay(World world, BlockPos pos, FluidState oldState, FluidState newState) {
		int tickRate = getTickRate(world);

		if (!oldState.isEmpty()
			&& !newState.isEmpty()
			&& !oldState.get(FALLING)
			&& !newState.get(FALLING)
			&& newState.getHeight(world, pos) > oldState.getHeight(world, pos)
			&& world.getRandom().nextInt(4) != 0
		) {
			tickRate *= 4;
		}

		return tickRate;
	}

	private void playExtinguishEvent(WorldAccess world, BlockPos pos) {
		world.syncWorldEvent(EXTINGUISH_WORLD_EVENT, pos, 0);
	}

	@Override
	protected boolean isInfinite(ServerWorld world) {
		return world.getGameRules().getValue(GameRules.LAVA_SOURCE_CONVERSION);
	}

	/**
	 * При падении лавы вниз на блок воды — образует камень вместо вытеснения.
	 * В остальных случаях делегирует стандартной логике.
	 */
	@Override
	protected void flow(WorldAccess world, BlockPos pos, BlockState state, Direction direction, FluidState fluidState) {
		if (direction == Direction.DOWN) {
			FluidState existingFluid = world.getFluidState(pos);

			if (isIn(FluidTags.LAVA) && existingFluid.isIn(FluidTags.WATER)) {
				if (state.getBlock() instanceof FluidBlock) {
					world.setBlockState(pos, Blocks.STONE.getDefaultState(), 3);
				}

				playExtinguishEvent(world, pos);
				return;
			}
		}

		super.flow(world, pos, state, direction, fluidState);
	}

	@Override
	protected boolean hasRandomTicks() {
		return true;
	}

	@Override
	protected float getBlastResistance() {
		return 100.0F;
	}

	@Override
	public Optional<SoundEvent> getBucketFillSound() {
		return Optional.of(SoundEvents.ITEM_BUCKET_FILL_LAVA);
	}

	/**
	 * Определяет, должна ли лава течь быстрее (режим Нижнего мира).
	 * Зависит от атрибута окружения {@link EnvironmentAttributes#FAST_LAVA_GAMEPLAY}.
	 */
	private static boolean shouldLavaFlowFaster(WorldView world) {
		return world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.FAST_LAVA_GAMEPLAY);
	}

	// -------------------------------------------------------------------------
	// Вложенные классы
	// -------------------------------------------------------------------------

	/** Текущая (flowing) лава с изменяемым уровнем 1–8. */
	public static class Flowing extends LavaFluid {

		@Override
		protected void appendProperties(StateManager.Builder<Fluid, FluidState> builder) {
			super.appendProperties(builder);
			builder.add(LEVEL);
		}

		@Override
		public int getLevel(FluidState state) {
			return state.get(LEVEL);
		}

		@Override
		public boolean isStill(FluidState state) {
			return false;
		}
	}

	/** Стоячая (still/source) лава с фиксированным уровнем 8. */
	public static class Still extends LavaFluid {

		@Override
		public int getLevel(FluidState state) {
			return 8;
		}

		@Override
		public boolean isStill(FluidState state) {
			return true;
		}
	}
}

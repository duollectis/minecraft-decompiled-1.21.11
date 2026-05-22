package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.CollisionEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.dimension.NetherPortal;

import java.util.Optional;

/**
 * Базовый класс для всех типов огня (обычный огонь и огонь душ).
 * Управляет логикой поджигания сущностей, спавна частиц дыма,
 * активации нижеровских порталов и звуковыми эффектами.
 */
public abstract class AbstractFireBlock extends Block {

	private static final int SET_ON_FIRE_SECONDS = 8;
	private static final int MIN_FIRE_TICK_INCREMENT = 1;
	private static final int MAX_FIRE_TICK_INCREMENT = 3;
	/** Вероятность воспроизведения звука огня (1 из 24 тиков отображения). */
	private static final int SOUND_PLAY_CHANCE = 24;
	/** Количество частиц дыма для горящих боковых блоков. */
	private static final int SIDE_SMOKE_PARTICLE_COUNT = 2;
	/** Количество частиц дыма для горящего основания. */
	private static final int BASE_SMOKE_PARTICLE_COUNT = 3;

	protected static final VoxelShape BASE_SHAPE = Block.createColumnShape(16.0, 0.0, 1.0);

	private final float damage;

	public AbstractFireBlock(AbstractBlock.Settings settings, float damage) {
		super(settings);
		this.damage = damage;
	}

	@Override
	protected abstract MapCodec<? extends AbstractFireBlock> getCodec();

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return getState(ctx.getWorld(), ctx.getBlockPos());
	}

	/**
	 * Определяет тип огня для размещения в указанной позиции:
	 * огонь душ — если под блоком находится основание для огня душ,
	 * иначе — обычный огонь с учётом соседних горючих блоков.
	 */
	public static BlockState getState(BlockView world, BlockPos pos) {
		BlockState below = world.getBlockState(pos.down());
		return SoulFireBlock.isSoulBase(below)
			? Blocks.SOUL_FIRE.getDefaultState()
			: ((FireBlock) Blocks.FIRE).getStateForPosition(world, pos);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return BASE_SHAPE;
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (random.nextInt(SOUND_PLAY_CHANCE) == 0) {
			world.playSoundClient(
				pos.getX() + 0.5,
				pos.getY() + 0.5,
				pos.getZ() + 0.5,
				SoundEvents.BLOCK_FIRE_AMBIENT,
				SoundCategory.BLOCKS,
				1.0F + random.nextFloat(),
				random.nextFloat() * 0.7F + 0.3F,
				false
			);
		}

		BlockPos belowPos = pos.down();
		BlockState belowState = world.getBlockState(belowPos);

		if (isFlammable(belowState) || belowState.isSideSolidFullSquare(world, belowPos, Direction.UP)) {
			spawnBaseSmoke(world, pos, random);
		} else {
			spawnSideSmoke(world, pos, random);
		}
	}

	private void spawnBaseSmoke(World world, BlockPos pos, Random random) {
		for (int i = 0; i < BASE_SMOKE_PARTICLE_COUNT; i++) {
			double x = pos.getX() + random.nextDouble();
			double y = pos.getY() + random.nextDouble() * 0.5 + 0.5;
			double z = pos.getZ() + random.nextDouble();
			world.addParticleClient(ParticleTypes.LARGE_SMOKE, x, y, z, 0.0, 0.0, 0.0);
		}
	}

	private void spawnSideSmoke(World world, BlockPos pos, Random random) {
		if (isFlammable(world.getBlockState(pos.west()))) {
			spawnSideSmokeParticles(world, random,
				pos.getX() + random.nextDouble() * 0.1F,
				pos.getY() + random.nextDouble(),
				pos.getZ() + random.nextDouble()
			);
		}

		if (isFlammable(world.getBlockState(pos.east()))) {
			spawnSideSmokeParticles(world, random,
				pos.getX() + 1 - random.nextDouble() * 0.1F,
				pos.getY() + random.nextDouble(),
				pos.getZ() + random.nextDouble()
			);
		}

		if (isFlammable(world.getBlockState(pos.north()))) {
			spawnSideSmokeParticles(world, random,
				pos.getX() + random.nextDouble(),
				pos.getY() + random.nextDouble(),
				pos.getZ() + random.nextDouble() * 0.1F
			);
		}

		if (isFlammable(world.getBlockState(pos.south()))) {
			spawnSideSmokeParticles(world, random,
				pos.getX() + random.nextDouble(),
				pos.getY() + random.nextDouble(),
				pos.getZ() + 1 - random.nextDouble() * 0.1F
			);
		}

		if (isFlammable(world.getBlockState(pos.up()))) {
			spawnSideSmokeParticles(world, random,
				pos.getX() + random.nextDouble(),
				pos.getY() + 1 - random.nextDouble() * 0.1F,
				pos.getZ() + random.nextDouble()
			);
		}
	}

	private static void spawnSideSmokeParticles(World world, Random random, double x, double y, double z) {
		for (int i = 0; i < SIDE_SMOKE_PARTICLE_COUNT; i++) {
			world.addParticleClient(ParticleTypes.LARGE_SMOKE, x, y, z, 0.0, 0.0, 0.0);
		}
	}

	protected abstract boolean isFlammable(BlockState state);

	@Override
	protected void onEntityCollision(
		BlockState state,
		World world,
		BlockPos pos,
		Entity entity,
		EntityCollisionHandler handler,
		boolean firstCollision
	) {
		handler.addEvent(CollisionEvent.CLEAR_FREEZE);
		handler.addEvent(CollisionEvent.FIRE_IGNITE);
		handler.addPostCallback(
			CollisionEvent.FIRE_IGNITE,
			target -> target.serverDamage(target.getEntityWorld().getDamageSources().inFire(), damage)
		);
	}

	/**
	 * Постепенно поджигает сущность: увеличивает счётчик тиков огня
	 * и устанавливает горение на {@value #SET_ON_FIRE_SECONDS} секунд при достижении порога.
	 * Для серверных игроков добавляет случайный инкремент от {@value #MIN_FIRE_TICK_INCREMENT}
	 * до {@value #MAX_FIRE_TICK_INCREMENT} тиков.
	 */
	public static void igniteEntity(Entity entity) {
		if (entity.isFireImmune()) {
			return;
		}

		if (entity.getFireTicks() < 0) {
			entity.setFireTicks(entity.getFireTicks() + 1);
		} else if (entity instanceof ServerPlayerEntity) {
			int increment = entity.getEntityWorld().getRandom().nextBetweenExclusive(
				MIN_FIRE_TICK_INCREMENT,
				MAX_FIRE_TICK_INCREMENT
			);
			entity.setFireTicks(entity.getFireTicks() + increment);
		}

		if (entity.getFireTicks() >= 0) {
			entity.setOnFireFor(SET_ON_FIRE_SECONDS);
		}
	}

	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		if (oldState.isOf(state.getBlock())) {
			return;
		}

		if (isOverworldOrNether(world)) {
			Optional<NetherPortal> portal = NetherPortal.getNewPortal(world, pos, Direction.Axis.X);
			if (portal.isPresent()) {
				portal.get().createPortal(world);
				return;
			}
		}

		if (!state.canPlaceAt(world, pos)) {
			world.removeBlock(pos, false);
		}
	}

	private static boolean isOverworldOrNether(World world) {
		return world.getRegistryKey() == World.OVERWORLD || world.getRegistryKey() == World.NETHER;
	}

	@Override
	protected void spawnBreakParticles(World world, PlayerEntity player, BlockPos pos, BlockState state) {
	}

	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!world.isClient()) {
			world.syncWorldEvent(null, 1009, pos, 0);
		}

		return super.onBreak(world, pos, state, player);
	}

	/**
	 * Проверяет, можно ли разместить огонь в указанной позиции:
	 * позиция должна быть воздухом, а огонь должен либо иметь опору,
	 * либо активировать нижеровский портал.
	 */
	public static boolean canPlaceAt(World world, BlockPos pos, Direction direction) {
		BlockState blockState = world.getBlockState(pos);
		return blockState.isAir()
			? getState(world, pos).canPlaceAt(world, pos) || shouldLightPortalAt(world, pos, direction)
			: false;
	}

	/**
	 * Проверяет, можно ли зажечь нижеровский портал в данной позиции.
	 * Требует наличия хотя бы одного блока обсидиана среди соседей
	 * и корректной ориентации рамки портала.
	 */
	private static boolean shouldLightPortalAt(World world, BlockPos pos, Direction direction) {
		if (!isOverworldOrNether(world)) {
			return false;
		}

		BlockPos.Mutable mutable = pos.mutableCopy();
		boolean hasObsidian = false;

		for (Direction dir : Direction.values()) {
			if (world.getBlockState(mutable.set(pos).move(dir)).isOf(Blocks.OBSIDIAN)) {
				hasObsidian = true;
				break;
			}
		}

		if (!hasObsidian) {
			return false;
		}

		Direction.Axis axis = direction.getAxis().isHorizontal()
			? direction.rotateYCounterclockwise().getAxis()
			: Direction.Type.HORIZONTAL.randomAxis(world.random);

		return NetherPortal.getNewPortal(world, pos, axis).isPresent();
	}
}

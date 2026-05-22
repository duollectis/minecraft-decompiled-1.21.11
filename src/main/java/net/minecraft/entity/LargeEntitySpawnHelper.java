package net.minecraft.entity;

import net.minecraft.block.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.Optional;

/**
 * Вспомогательный класс для спавна крупных сущностей (Железный голем, Хранитель, Скрипун).
 * Выполняет поиск подходящей позиции спавна в заданном радиусе с учётом требований к блокам.
 */
public class LargeEntitySpawnHelper {

	/**
	 * Пытается заспавнить сущность в случайной позиции в заданном радиусе от точки.
	 * Перебирает {@code tries} попыток, для каждой ищет подходящую вертикальную позицию.
	 *
	 * @param entityType       тип спавнимой сущности
	 * @param reason           причина спавна
	 * @param world            серверный мир
	 * @param pos              центральная позиция поиска
	 * @param tries            количество попыток
	 * @param horizontalRange  горизонтальный радиус поиска в блоках
	 * @param verticalRange    вертикальный радиус поиска в блоках
	 * @param requirements     требования к блокам для спавна
	 * @param requireEmptySpace требовать ли свободное пространство для хитбокса
	 * @return заспавненная сущность или пустой Optional
	 */
	public static <T extends MobEntity> Optional<T> trySpawnAt(
		EntityType<T> entityType,
		SpawnReason reason,
		ServerWorld world,
		BlockPos pos,
		int tries,
		int horizontalRange,
		int verticalRange,
		LargeEntitySpawnHelper.Requirements requirements,
		boolean requireEmptySpace
	) {
		BlockPos.Mutable mutable = pos.mutableCopy();

		for (int attempt = 0; attempt < tries; attempt++) {
			int offsetX = MathHelper.nextBetween(world.random, -horizontalRange, horizontalRange);
			int offsetZ = MathHelper.nextBetween(world.random, -horizontalRange, horizontalRange);
			mutable.set(pos, offsetX, verticalRange, offsetZ);

			if (!world.getWorldBorder().contains(mutable)) {
				continue;
			}

			if (!findSpawnPos(world, verticalRange, mutable, requirements)) {
				continue;
			}

			if (requireEmptySpace && !world.isSpaceEmpty(entityType.getSpawnBox(
				mutable.getX() + 0.5,
				mutable.getY(),
				mutable.getZ() + 0.5
			))) {
				continue;
			}

			T mobEntity = (T) entityType.create(world, null, mutable, reason, false, false);
			if (mobEntity == null) {
				continue;
			}

			if (mobEntity.canSpawn(world, reason) && mobEntity.canSpawn(world)) {
				world.spawnEntityAndPassengers(mobEntity);
				mobEntity.playAmbientSound();
				return Optional.of(mobEntity);
			}

			mobEntity.discard();
		}

		return Optional.empty();
	}

	private static boolean findSpawnPos(
		ServerWorld world,
		int verticalRange,
		BlockPos.Mutable pos,
		LargeEntitySpawnHelper.Requirements requirements
	) {
		BlockPos.Mutable abovePos = new BlockPos.Mutable().set(pos);
		BlockState currentState = world.getBlockState(abovePos);

		for (int offset = verticalRange; offset >= -verticalRange; offset--) {
			pos.move(Direction.DOWN);
			abovePos.set(pos, Direction.UP);
			BlockState belowState = world.getBlockState(pos);

			if (requirements.canSpawnOn(world, pos, belowState, abovePos, currentState)) {
				pos.move(Direction.UP);
				return true;
			}

			currentState = belowState;
		}

		return false;
	}

	/**
	 * Требования к блокам для спавна крупной сущности.
	 * Определяет, можно ли заспавнить сущность на данном блоке.
	 */
	public interface Requirements {

		@Deprecated
		LargeEntitySpawnHelper.Requirements IRON_GOLEM = (world, pos, state, abovePos, aboveState) ->
			!state.isOf(Blocks.COBWEB)
				&& !state.isOf(Blocks.CACTUS)
				&& !state.isOf(Blocks.GLASS_PANE)
				&& !(state.getBlock() instanceof StainedGlassPaneBlock)
				&& !(state.getBlock() instanceof StainedGlassBlock)
				&& !(state.getBlock() instanceof LeavesBlock)
				&& !state.isOf(Blocks.CONDUIT)
				&& !state.isOf(Blocks.ICE)
				&& !state.isOf(Blocks.TNT)
				&& !state.isOf(Blocks.GLOWSTONE)
				&& !state.isOf(Blocks.BEACON)
				&& !state.isOf(Blocks.SEA_LANTERN)
				&& !state.isOf(Blocks.FROSTED_ICE)
				&& !state.isOf(Blocks.TINTED_GLASS)
				&& !state.isOf(Blocks.GLASS)
				&& (aboveState.isAir() || aboveState.isLiquid())
				&& (state.isSolid() || state.isOf(Blocks.POWDER_SNOW));

		LargeEntitySpawnHelper.Requirements WARDEN = (world, pos, state, abovePos, aboveState) ->
			aboveState.getCollisionShape(world, abovePos).isEmpty()
				&& Block.isFaceFullSquare(state.getCollisionShape(world, pos), Direction.UP);

		LargeEntitySpawnHelper.Requirements CREAKING = (world, pos, state, abovePos, aboveState) ->
			aboveState.getCollisionShape(world, abovePos).isEmpty()
				&& !state.isIn(BlockTags.LEAVES)
				&& Block.isFaceFullSquare(state.getCollisionShape(world, pos), Direction.UP);

		boolean canSpawnOn(ServerWorld world, BlockPos pos, BlockState state, BlockPos abovePos, BlockState aboveState);
	}
}

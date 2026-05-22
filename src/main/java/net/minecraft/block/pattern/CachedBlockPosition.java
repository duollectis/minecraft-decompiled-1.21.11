package net.minecraft.block.pattern;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Обёртка над позицией блока с ленивым кэшированием его состояния и блок-сущности.
 * <p>
 * Используется в {@link BlockPattern} для избежания повторных обращений к миру
 * при многократной проверке одной и той же позиции в ходе поиска шаблона.
 * Состояние блока загружается только при первом обращении и только если чанк загружен
 * (или установлен флаг {@code forceLoad}).
 */
public class CachedBlockPosition {

	private final WorldView world;
	private final BlockPos pos;
	private final boolean forceLoad;
	private @Nullable BlockState state;
	private @Nullable BlockEntity blockEntity;
	private boolean cachedEntity;

	public CachedBlockPosition(WorldView world, BlockPos pos, boolean forceLoad) {
		this.world = world;
		this.pos = pos.toImmutable();
		this.forceLoad = forceLoad;
	}

	public @Nullable BlockState getBlockState() {
		if (state == null && (forceLoad || world.isChunkLoaded(pos))) {
			state = world.getBlockState(pos);
		}

		return state;
	}

	public @Nullable BlockEntity getBlockEntity() {
		if (blockEntity == null && cachedEntity == false) {
			blockEntity = world.getBlockEntity(pos);
			cachedEntity = true;
		}

		return blockEntity;
	}

	public WorldView getWorld() {
		return world;
	}

	public BlockPos getBlockPos() {
		return pos;
	}

	/**
	 * Создаёт предикат, проверяющий состояние блока в кэшированной позиции.
	 * <p>
	 * Возвращает {@code false} для {@code null}-позиций (незагруженные чанки),
	 * что позволяет безопасно использовать предикат в {@link BlockPattern}.
	 *
	 * @param statePredicate условие проверки состояния блока
	 * @return предикат над {@link CachedBlockPosition}, безопасный к {@code null}
	 */
	public static Predicate<@Nullable CachedBlockPosition> matchesBlockState(Predicate<BlockState> statePredicate) {
		return cached -> cached != null && statePredicate.test(cached.getBlockState());
	}
}

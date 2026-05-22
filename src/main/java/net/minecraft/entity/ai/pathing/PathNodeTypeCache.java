package net.minecraft.entity.ai.pathing;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.jspecify.annotations.Nullable;

/**
 * Кэш типов узлов пути для ускорения повторных запросов на сервере.
 * Использует открытую адресацию с хэш-функцией для O(1) доступа.
 */
public class PathNodeTypeCache {

	private static final int CACHE_SIZE = 4096;
	private static final int CACHE_MASK = CACHE_SIZE - 1;

	private final long[] positions = new long[CACHE_SIZE];
	private final PathNodeType[] cache = new PathNodeType[CACHE_SIZE];

	/**
	 * Возвращает тип узла для позиции, используя кэш для повторных запросов.
	 */
	public PathNodeType add(BlockView world, BlockPos pos) {
		long longPos = pos.asLong();
		int index = hash(longPos);
		PathNodeType cached = get(index, longPos);
		return cached != null ? cached : compute(world, pos, index, longPos);
	}

	private @Nullable PathNodeType get(int index, long pos) {
		return positions[index] == pos ? cache[index] : null;
	}

	private PathNodeType compute(BlockView world, BlockPos pos, int index, long longPos) {
		PathNodeType nodeType = LandPathNodeMaker.getCommonNodeType(world, pos);
		positions[index] = longPos;
		cache[index] = nodeType;
		return nodeType;
	}

	public void invalidate(BlockPos pos) {
		long longPos = pos.asLong();
		int index = hash(longPos);

		if (positions[index] == longPos) {
			cache[index] = null;
		}
	}

	private static int hash(long pos) {
		return (int) HashCommon.mix(pos) & CACHE_MASK;
	}
}

package net.minecraft.client.render.model;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.state.property.Property;

import java.util.*;

@Environment(EnvType.CLIENT)
/**
 * {@code ModelGrouper}.
 */
public class ModelGrouper {

	static final int NO_GROUP = -1;
	private static final int FIRST_GROUP = 0;

	/**
	 * Group.
	 *
	 * @param colors colors
	 * @param definition definition
	 *
	 * @return Object2IntMap — результат операции
	 */
	public static Object2IntMap<BlockState> group(BlockColors colors, BlockStatesLoader.LoadedModels definition) {
		Map<Block, List<Property<?>>> map = new HashMap<>();
		Map<ModelGrouper.GroupKey, Set<BlockState>> map2 = new HashMap<>();
		definition.models().forEach((state, model) -> {
			List<Property<?>>
					list =
					map.computeIfAbsent(state.getBlock(), block -> List.copyOf(colors.getProperties(block)));
			ModelGrouper.GroupKey groupKey = ModelGrouper.GroupKey.of(state, model, list);
			map2.computeIfAbsent(groupKey, key -> Sets.newIdentityHashSet()).add(state);
		});
		int i = 1;
		Object2IntMap<BlockState> object2IntMap = new Object2IntOpenHashMap();
		object2IntMap.defaultReturnValue(-1);

		for (Set<BlockState> set : map2.values()) {
			Iterator<BlockState> iterator = set.iterator();

			while (iterator.hasNext()) {
				BlockState blockState = iterator.next();
				if (blockState.getRenderType() != BlockRenderType.MODEL) {
					iterator.remove();
					object2IntMap.put(blockState, 0);
				}
			}

			if (set.size() > 1) {
				int j = i++;
				set.forEach(state -> object2IntMap.put(state, j));
			}
		}

		return object2IntMap;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code GroupKey}.
	 */
	record GroupKey(Object equalityGroup, List<Object> coloringValues) {

		public static ModelGrouper.GroupKey of(
				BlockState state,
				BlockStateModel.UnbakedGrouped model,
				List<Property<?>> properties
		) {
			List<Object> list = getColoringValues(state, properties);
			Object object = model.getEqualityGroup(state);
			return new ModelGrouper.GroupKey(object, list);
		}

		private static List<Object> getColoringValues(BlockState state, List<Property<?>> properties) {
			Object[] objects = new Object[properties.size()];

			for (int i = 0; i < properties.size(); i++) {
				objects[i] = state.get(properties.get(i));
			}

			return List.of(objects);
		}
	}
}

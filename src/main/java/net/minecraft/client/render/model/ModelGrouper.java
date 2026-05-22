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

/**
 * Группирует состояния блоков по идентичности модели и цветовым свойствам.
 * Состояния в одной группе могут использовать одинаковые запечённые данные,
 * что позволяет оптимизировать перерисовку при изменении состояния блока.
 */
@Environment(EnvType.CLIENT)
public class ModelGrouper {

	public static final int NO_GROUP = -1;
	private static final int FIRST_GROUP = 0;

	/**
	 * Строит карту групп для всех состояний блоков из загруженных моделей.
	 * Состояния с одинаковой группой модели и одинаковыми цветовыми свойствами
	 * получают один и тот же номер группы; состояния без модели получают {@link #FIRST_GROUP}.
	 */
	public static Object2IntMap<BlockState> group(BlockColors colors, BlockStatesLoader.LoadedModels definition) {
		Map<Block, List<Property<?>>> colorPropertiesByBlock = new HashMap<>();
		Map<ModelGrouper.GroupKey, Set<BlockState>> statesByGroup = new HashMap<>();

		definition.models().forEach((state, model) -> {
			List<Property<?>> colorProperties = colorPropertiesByBlock.computeIfAbsent(
					state.getBlock(),
					block -> List.copyOf(colors.getProperties(block))
			);
			ModelGrouper.GroupKey groupKey = ModelGrouper.GroupKey.of(state, model, colorProperties);
			statesByGroup.computeIfAbsent(groupKey, key -> Sets.newIdentityHashSet()).add(state);
		});

		int nextGroupId = 1;
		Object2IntMap<BlockState> result = new Object2IntOpenHashMap<>();
		result.defaultReturnValue(NO_GROUP);

		for (Set<BlockState> group : statesByGroup.values()) {
			Iterator<BlockState> iterator = group.iterator();

			while (iterator.hasNext()) {
				BlockState state = iterator.next();

				if (state.getRenderType() != BlockRenderType.MODEL) {
					iterator.remove();
					result.put(state, FIRST_GROUP);
				}
			}

			if (group.size() > 1) {
				int groupId = nextGroupId++;
				group.forEach(state -> result.put(state, groupId));
			}
		}

		return result;
	}

	/**
	 * Ключ группировки: объединяет группу равенства модели и список цветовых значений свойств.
	 * Два состояния блока попадают в одну группу тогда и только тогда, когда их ключи равны.
	 */
	@Environment(EnvType.CLIENT)
	record GroupKey(Object equalityGroup, List<Object> coloringValues) {

		public static ModelGrouper.GroupKey of(
				BlockState state,
				BlockStateModel.UnbakedGrouped model,
				List<Property<?>> properties
		) {
			return new ModelGrouper.GroupKey(
					model.getEqualityGroup(state),
					getColoringValues(state, properties)
			);
		}

		private static List<Object> getColoringValues(BlockState state, List<Property<?>> properties) {
			Object[] values = new Object[properties.size()];

			for (int i = 0; i < properties.size(); i++) {
				values[i] = state.get(properties.get(i));
			}

			return List.of(values);
		}
	}
}

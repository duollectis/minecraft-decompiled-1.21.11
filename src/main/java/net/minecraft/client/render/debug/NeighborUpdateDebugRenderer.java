package net.minecraft.client.render.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.TextGizmo;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Отладочный рендерер обновлений соседних блоков.
 * <p>
 * Визуализирует события {@code NEIGHBOR_UPDATES}: для каждой позиции блока рисует
 * сжимающийся куб (чем старше событие — тем меньше куб) и счётчик накопленных
 * обновлений в виде текстовой метки.
 */
@Environment(EnvType.CLIENT)
public class NeighborUpdateDebugRenderer implements DebugRenderer.Renderer {

	@Override
	public void render(
			double cameraX,
			double cameraY,
			double cameraZ,
			DebugDataStore store,
			Frustum frustum,
			float tickProgress
	) {
		int expiry = DebugSubscriptionTypes.NEIGHBOR_UPDATES.getExpiry();
		double shrinkPerAge = 1.0 / (expiry * 2);

		Map<BlockPos, Update> updatesByPos = new HashMap<>();

		store.forEachEvent(
				DebugSubscriptionTypes.NEIGHBOR_UPDATES, (blockPos, currentTime, eventTime) -> {
					long age = eventTime - currentTime;
					Update existing = updatesByPos.getOrDefault(blockPos, Update.EMPTY);
					updatesByPos.put(blockPos, existing.withAge((int) age));
				}
		);

		for (Entry<BlockPos, Update> entry : updatesByPos.entrySet()) {
			BlockPos blockPos = entry.getKey();
			Update update = entry.getValue();
			Box box = new Box(blockPos).expand(0.002).contract(shrinkPerAge * update.age);
			GizmoDrawing.box(box, DrawStyle.stroked(-1));
		}

		for (Entry<BlockPos, Update> entry : updatesByPos.entrySet()) {
			BlockPos blockPos = entry.getKey();
			Update update = entry.getValue();
			GizmoDrawing.text(String.valueOf(update.count), Vec3d.ofCenter(blockPos), TextGizmo.Style.left());
		}
	}

	/**
	 * Агрегированное состояние обновлений для одной позиции блока.
	 * <p>
	 * Хранит количество накопленных обновлений ({@code count}) и возраст
	 * самого свежего из них ({@code age}). Чем меньше {@code age} — тем
	 * новее событие и тем крупнее будет отрисованный куб.
	 */
	@Environment(EnvType.CLIENT)
	record Update(int count, int age) {

		static final Update EMPTY = new Update(0, Integer.MAX_VALUE);

		Update withAge(int newAge) {
			if (newAge == age) {
				return new Update(count + 1, newAge);
			}

			return newAge < age ? new Update(1, newAge) : this;
		}
	}
}

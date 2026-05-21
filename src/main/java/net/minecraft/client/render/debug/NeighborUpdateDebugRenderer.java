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

@Environment(EnvType.CLIENT)
/**
 * {@code NeighborUpdateDebugRenderer}.
 */
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
		int i = DebugSubscriptionTypes.NEIGHBOR_UPDATES.getExpiry();
		double d = 1.0 / (i * 2);
		Map<BlockPos, NeighborUpdateDebugRenderer.Update> map = new HashMap<>();
		store.forEachEvent(
				DebugSubscriptionTypes.NEIGHBOR_UPDATES, (blockPosx, ix, j) -> {
					long l = j - ix;
					NeighborUpdateDebugRenderer.Update
							updatex =
							map.getOrDefault(blockPosx, NeighborUpdateDebugRenderer.Update.EMPTY);
					map.put(blockPosx, updatex.withAge((int) l));
				}
		);

		for (Entry<BlockPos, NeighborUpdateDebugRenderer.Update> entry : map.entrySet()) {
			BlockPos blockPos = entry.getKey();
			NeighborUpdateDebugRenderer.Update update = entry.getValue();
			Box box = new Box(blockPos).expand(0.002).contract(d * update.age);
			GizmoDrawing.box(box, DrawStyle.stroked(-1));
		}

		for (Entry<BlockPos, NeighborUpdateDebugRenderer.Update> entry : map.entrySet()) {
			BlockPos blockPos = entry.getKey();
			NeighborUpdateDebugRenderer.Update update = entry.getValue();
			GizmoDrawing.text(String.valueOf(update.count), Vec3d.ofCenter(blockPos), TextGizmo.Style.left());
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Update}.
	 */
	record Update(int count, int age) {

		static final NeighborUpdateDebugRenderer.Update
				EMPTY =
				new NeighborUpdateDebugRenderer.Update(0, Integer.MAX_VALUE);

		public NeighborUpdateDebugRenderer.Update withAge(int age) {
			if (age == this.age) {
				return new NeighborUpdateDebugRenderer.Update(this.count + 1, age);
			}
			else {
				return age < this.age ? new NeighborUpdateDebugRenderer.Update(1, age) : this;
			}
		}
	}
}

package net.minecraft.client.render.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.util.math.Direction;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Состояние рендера границы мира: координаты AABB, цвет и прозрачность.
 * Метод {@link #nearestBorder} возвращает список сторон, отсортированных
 * по расстоянию от точки наблюдателя до каждой грани границы.
 */
@Environment(EnvType.CLIENT)
public class WorldBorderRenderState implements FabricRenderState {

	public double minX;
	public double maxX;
	public double minZ;
	public double maxZ;
	public int tint;
	public double alpha;

	public List<WorldBorderRenderState.Distance> nearestBorder(double x, double z) {
		WorldBorderRenderState.Distance[] distances = new WorldBorderRenderState.Distance[]{
				new WorldBorderRenderState.Distance(Direction.NORTH, z - minZ),
				new WorldBorderRenderState.Distance(Direction.SOUTH, maxZ - z),
				new WorldBorderRenderState.Distance(Direction.WEST, x - minX),
				new WorldBorderRenderState.Distance(Direction.EAST, maxX - x)
		};
		return Arrays.stream(distances)
				.sorted(Comparator.comparingDouble(d -> d.value))
				.toList();
	}

	public void clear() {
		alpha = 0.0;
	}

	/** Расстояние от наблюдателя до конкретной стороны границы мира. */
	@Environment(EnvType.CLIENT)
	public record Distance(Direction direction, double value) {
	}
}

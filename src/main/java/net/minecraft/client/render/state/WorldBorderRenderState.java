package net.minecraft.client.render.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.util.math.Direction;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code WorldBorderRenderState}.
 */
public class WorldBorderRenderState implements FabricRenderState {

	public double minX;
	public double maxX;
	public double minZ;
	public double maxZ;
	public int tint;
	public double alpha;

	public List<WorldBorderRenderState.Distance> nearestBorder(double x, double z) {
		WorldBorderRenderState.Distance[] distances = new WorldBorderRenderState.Distance[]{
				new WorldBorderRenderState.Distance(Direction.NORTH, z - this.minZ),
				new WorldBorderRenderState.Distance(Direction.SOUTH, this.maxZ - z),
				new WorldBorderRenderState.Distance(Direction.WEST, x - this.minX),
				new WorldBorderRenderState.Distance(Direction.EAST, this.maxX - x)
		};
		return Arrays.stream(distances).sorted(Comparator.comparingDouble(d -> d.value)).toList();
	}

	/**
	 * Clear.
	 */
	public void clear() {
		this.alpha = 0.0;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Distance}.
	 */
	public record Distance(Direction direction, double value) {
	}
}

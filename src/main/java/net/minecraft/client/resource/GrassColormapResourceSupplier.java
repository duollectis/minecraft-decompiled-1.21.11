package net.minecraft.client.resource;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.RawTextureDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.biome.GrassColors;

import java.io.IOException;

@Environment(EnvType.CLIENT)
/**
 * {@code GrassColormapResourceSupplier}.
 */
public class GrassColormapResourceSupplier extends SinglePreparationResourceReloader<int[]> {

	private static final Identifier GRASS_COLORMAP_LOC = Identifier.ofVanilla("textures/colormap/grass.png");

	/**
	 * Prepare.
	 *
	 * @param resourceManager resource manager
	 * @param profiler profiler
	 *
	 * @return int[] — результат операции
	 */
	protected int[] prepare(ResourceManager resourceManager, Profiler profiler) {
		try {
			return RawTextureDataLoader.loadRawTextureData(resourceManager, GRASS_COLORMAP_LOC);
		}
		catch (IOException var4) {
			throw new IllegalStateException("Failed to load grass color texture", var4);
		}
	}

	/**
	 * Apply.
	 *
	 * @param is is
	 * @param resourceManager resource manager
	 * @param profiler profiler
	 */
	protected void apply(int[] is, ResourceManager resourceManager, Profiler profiler) {
		GrassColors.setColorMap(is);
	}
}

package net.minecraft.client.resource;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.RawTextureDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.biome.DryFoliageColors;

import java.io.IOException;

@Environment(EnvType.CLIENT)
/**
 * {@code DryFoliageColormapResourceSupplier}.
 */
public class DryFoliageColormapResourceSupplier extends SinglePreparationResourceReloader<int[]> {

	private static final Identifier DRY_FOLIAGE_COLORMAP = Identifier.ofVanilla("textures/colormap/dry_foliage.png");

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
			return RawTextureDataLoader.loadRawTextureData(resourceManager, DRY_FOLIAGE_COLORMAP);
		}
		catch (IOException var4) {
			throw new IllegalStateException("Failed to load dry foliage color texture", var4);
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
		DryFoliageColors.setColorMap(is);
	}
}

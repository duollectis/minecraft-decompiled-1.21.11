package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;

@Environment(EnvType.CLIENT)
/**
 * {@code RawTextureDataLoader}.
 */
public class RawTextureDataLoader {

	@Deprecated
	/**
	 * Загружает raw texture data.
	 *
	 * @param resourceManager resource manager
	 * @param id id
	 *
	 * @return int[] — результат операции
	 */
	public static int[] loadRawTextureData(ResourceManager resourceManager, Identifier id) throws IOException {
		int[] var4;
		try (
				InputStream inputStream = resourceManager.open(id);
				NativeImage nativeImage = NativeImage.read(inputStream);
		) {
			var4 = nativeImage.makePixelArray();
		}

		return var4;
	}
}

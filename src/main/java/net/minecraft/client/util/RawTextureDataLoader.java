package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;

/**
 * Устаревший загрузчик пиксельных данных текстур в виде массива ARGB-значений.
 *
 * @deprecated используйте {@link net.minecraft.client.texture.NativeImage} напрямую
 */
@Environment(EnvType.CLIENT)
public class RawTextureDataLoader {

	/**
	 * Загружает пиксельные данные текстуры как массив ARGB-значений.
	 *
	 * @param resourceManager менеджер ресурсов для открытия файла
	 * @param id              идентификатор текстуры
	 * @return массив ARGB-пикселей
	 * @throws IOException если ресурс не найден или не может быть прочитан
	 * @deprecated используйте {@link NativeImage} напрямую
	 */
	@Deprecated
	public static int[] loadRawTextureData(ResourceManager resourceManager, Identifier id) throws IOException {
		try (
			InputStream inputStream = resourceManager.open(id);
			NativeImage nativeImage = NativeImage.read(inputStream)
		) {
			return nativeImage.makePixelArray();
		}
	}
}

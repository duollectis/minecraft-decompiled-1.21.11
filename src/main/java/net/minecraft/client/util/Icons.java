package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import org.apache.commons.lang3.ArrayUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Перечисление наборов иконок приложения для релизной и снапшот-версий игры.
 * Предоставляет доступ к иконкам разных размеров из ресурс-пака.
 */
@Environment(EnvType.CLIENT)
public enum Icons {
	RELEASE("icons"),
	SNAPSHOT("icons", "snapshot");

	private final String[] path;

	Icons(String... path) {
		this.path = path;
	}

	public List<InputSupplier<InputStream>> getIcons(ResourcePack resourcePack) throws IOException {
		return List.of(
			getIcon(resourcePack, "icon_16x16.png"),
			getIcon(resourcePack, "icon_32x32.png"),
			getIcon(resourcePack, "icon_48x48.png"),
			getIcon(resourcePack, "icon_128x128.png"),
			getIcon(resourcePack, "icon_256x256.png")
		);
	}

	public InputSupplier<InputStream> getMacIcon(ResourcePack resourcePack) throws IOException {
		return getIcon(resourcePack, "minecraft.icns");
	}

	private InputSupplier<InputStream> getIcon(ResourcePack resourcePack, String fileName) throws IOException {
		String[] fullPath = (String[]) ArrayUtils.add(path, fileName);
		InputSupplier<InputStream> supplier = resourcePack.openRoot(fullPath);
		if (supplier == null) {
			throw new FileNotFoundException(String.join("/", fullPath));
		}

		return supplier;
	}
}

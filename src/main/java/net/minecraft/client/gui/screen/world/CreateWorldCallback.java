package net.minecraft.client.gui.screen.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.ServerDynamicRegistryType;
import net.minecraft.world.level.LevelProperties;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * Колбэк, вызываемый при подтверждении создания нового мира.
 * Возвращает {@code true} если мир был успешно создан.
 */
@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface CreateWorldCallback {

	boolean create(
			CreateWorldScreen screen,
			CombinedDynamicRegistries<ServerDynamicRegistryType> dynamicRegistries,
			LevelProperties levelProperties,
			@Nullable Path dataPackTempDir
	);
}

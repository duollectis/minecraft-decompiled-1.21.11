package net.minecraft.client.gl;

import com.mojang.blaze3d.shaders.ShaderType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Функциональный интерфейс для получения исходного кода шейдера по идентификатору и типу.
 */
@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface ShaderSourceGetter {

	@Nullable String get(Identifier id, ShaderType type);
}

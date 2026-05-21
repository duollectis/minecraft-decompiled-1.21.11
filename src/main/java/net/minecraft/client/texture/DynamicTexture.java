package net.minecraft.client.texture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Path;

@Environment(EnvType.CLIENT)
/**
 * {@code DynamicTexture}.
 */
public interface DynamicTexture {

	void save(Identifier id, Path path) throws IOException;
}

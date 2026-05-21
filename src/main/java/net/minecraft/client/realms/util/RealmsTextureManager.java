package net.minecraft.client.realms.util;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsTextureManager}.
 */
public class RealmsTextureManager {

	private static final Map<String, RealmsTextureManager.RealmsTexture> TEXTURES = Maps.newHashMap();
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Identifier ISLES = Identifier.ofVanilla("textures/gui/presets/isles.png");

	public static Identifier getTextureId(String id, @Nullable String image) {
		return image == null ? ISLES : getTextureIdInternal(id, image);
	}

	private static Identifier getTextureIdInternal(String id, String image) {
		RealmsTextureManager.RealmsTexture realmsTexture = TEXTURES.get(id);
		if (realmsTexture != null && realmsTexture.image().equals(image)) {
			return realmsTexture.textureId;
		}
		else {
			NativeImage nativeImage = loadImage(image);
			if (nativeImage == null) {
				Identifier identifier = MissingSprite.getMissingSpriteId();
				TEXTURES.put(id, new RealmsTextureManager.RealmsTexture(image, identifier));
				return identifier;
			}
			else {
				Identifier identifier = Identifier.of("realms", "dynamic/" + id);
				MinecraftClient
						.getInstance()
						.getTextureManager()
						.registerTexture(identifier, new NativeImageBackedTexture(identifier::toString, nativeImage));
				TEXTURES.put(id, new RealmsTextureManager.RealmsTexture(image, identifier));
				return identifier;
			}
		}
	}

	private static @Nullable NativeImage loadImage(String image) {
		byte[] bs = Base64.getDecoder().decode(image);
		ByteBuffer byteBuffer = MemoryUtil.memAlloc(bs.length);

		try {
			return NativeImage.read(byteBuffer.put(bs).flip());
		}
		catch (IOException var7) {
			LOGGER.warn("Failed to load world image: {}", image, var7);
		}
		finally {
			MemoryUtil.memFree(byteBuffer);
		}

		return null;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code RealmsTexture}.
	 */
	public record RealmsTexture(String image, Identifier textureId) {
	}
}

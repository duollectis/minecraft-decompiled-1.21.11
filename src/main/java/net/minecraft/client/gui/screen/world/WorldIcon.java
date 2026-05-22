package net.minecraft.client.gui.screen.world;

import com.google.common.hash.Hashing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * Управляет текстурой иконки мира или сервера.
 * Загружает изображение 64×64 в GPU-текстуру и освобождает её при закрытии.
 */
@Environment(EnvType.CLIENT)
public class WorldIcon implements AutoCloseable {

	private static final Identifier UNKNOWN_SERVER_ID = Identifier.ofVanilla("textures/misc/unknown_server.png");
	private static final int ICON_WIDTH = 64;
	private static final int ICON_HEIGHT = 64;
	private final TextureManager textureManager;
	private final Identifier id;
	private @Nullable NativeImageBackedTexture texture;
	private boolean closed;

	private WorldIcon(TextureManager textureManager, Identifier id) {
		this.textureManager = textureManager;
		this.id = id;
	}

	public static WorldIcon forWorld(TextureManager textureManager, String worldName) {
		return new WorldIcon(
				textureManager,
				Identifier.ofVanilla(
						"worlds/" + Util.replaceInvalidChars(worldName, Identifier::isPathCharacterValid) + "/"
								+ Hashing.sha1().hashUnencodedChars(worldName) + "/icon"
				)
		);
	}

	public static WorldIcon forServer(TextureManager textureManager, String serverAddress) {
		return new WorldIcon(
				textureManager,
				Identifier.ofVanilla("servers/" + Hashing.sha1().hashUnencodedChars(serverAddress) + "/icon")
		);
	}

	public void load(NativeImage image) {
		if (image.getWidth() != ICON_WIDTH || image.getHeight() != ICON_HEIGHT) {
			image.close();
			throw new IllegalArgumentException(
					"Icon must be 64x64, but was " + image.getWidth() + "x" + image.getHeight());
		}

		try {
			assertOpen();
			if (texture == null) {
				texture = new NativeImageBackedTexture(() -> "Favicon " + id, image);
			} else {
				texture.setImage(image);
				texture.upload();
			}

			textureManager.registerTexture(id, texture);
		} catch (Throwable error) {
			image.close();
			destroy();
			throw error;
		}
	}

	public void destroy() {
		assertOpen();
		if (texture == null) {
			return;
		}

		textureManager.destroyTexture(id);
		texture.close();
		texture = null;
	}

	public Identifier getTextureId() {
		return texture != null ? id : UNKNOWN_SERVER_ID;
	}

	@Override
	public void close() {
		destroy();
		closed = true;
	}

	public boolean isClosed() {
		return closed;
	}

	private void assertOpen() {
		if (closed) {
			throw new IllegalStateException("Icon already closed");
		}
	}
}

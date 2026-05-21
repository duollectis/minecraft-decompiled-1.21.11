package net.minecraft.client.texture.atlas;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Environment(EnvType.CLIENT)
/**
 * {@code AtlasSprite}.
 */
public class AtlasSprite {

	private final Identifier id;
	private final Resource resource;
	private final AtomicReference<@Nullable NativeImage> image = new AtomicReference<>();
	private final AtomicInteger regionCount;

	public AtlasSprite(Identifier id, Resource resource, int regionCount) {
		this.id = id;
		this.resource = resource;
		this.regionCount = new AtomicInteger(regionCount);
	}

	/**
	 * Read.
	 *
	 * @return NativeImage — результат операции
	 */
	public NativeImage read() throws IOException {
		NativeImage nativeImage = this.image.get();
		if (nativeImage == null) {
			synchronized (this) {
				nativeImage = this.image.get();
				if (nativeImage == null) {
					try (InputStream inputStream = this.resource.getInputStream()) {
						nativeImage = NativeImage.read(inputStream);
						this.image.set(nativeImage);
					}
					catch (IOException var9) {
						throw new IOException("Failed to load image " + this.id, var9);
					}
				}
			}
		}

		return nativeImage;
	}

	/**
	 * Close.
	 */
	public void close() {
		int i = this.regionCount.decrementAndGet();
		if (i <= 0) {
			NativeImage nativeImage = this.image.getAndSet(null);
			if (nativeImage != null) {
				nativeImage.close();
			}
		}
	}
}

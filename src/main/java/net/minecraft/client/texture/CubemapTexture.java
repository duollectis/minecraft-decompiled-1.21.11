package net.minecraft.client.texture;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.TextureFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.metadata.TextureResourceMetadata;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;

/**
 * Текстура кубической карты (cubemap), собираемая из 6 отдельных граней.
 * Грани загружаются по суффиксам и объединяются в одно вертикальное изображение
 * высотой {@code faceHeight * 6}, которое затем передаётся в GPU как массив слоёв.
 */
@Environment(EnvType.CLIENT)
public class CubemapTexture extends ReloadableTexture {

	private static final int FACE_COUNT = 6;
	private static final String[] TEXTURE_SUFFIXES = {"_1.png", "_3.png", "_5.png", "_4.png", "_0.png", "_2.png"};

	public CubemapTexture(Identifier identifier) {
		super(identifier);
	}

	@Override
	public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
		Identifier identifier = getId();

		try (TextureContents firstFace = TextureContents.load(
				resourceManager,
				identifier.withSuffixedPath(TEXTURE_SUFFIXES[0])
		)) {
			int faceWidth = firstFace.image().getWidth();
			int faceHeight = firstFace.image().getHeight();
			NativeImage combined = new NativeImage(faceWidth, faceHeight * FACE_COUNT, false);
			firstFace.image().copyRect(combined, 0, 0, 0, 0, faceWidth, faceHeight, false, true);

			for (int k = 1; k < FACE_COUNT; k++) {
				try (TextureContents face = TextureContents.load(
						resourceManager,
						identifier.withSuffixedPath(TEXTURE_SUFFIXES[k])
				)) {
					if (face.image().getWidth() != faceWidth || face.image().getHeight() != faceHeight) {
						throw new IOException(
								"Image dimensions of cubemap '"
										+ identifier
										+ "' sides do not match: part 0 is "
										+ faceWidth
										+ "x"
										+ faceHeight
										+ ", but part "
										+ k
										+ " is "
										+ face.image().getWidth()
										+ "x"
										+ face.image().getHeight()
						);
					}

					face.image().copyRect(combined, 0, 0, 0, k * faceHeight, faceWidth, faceHeight, false, true);
				}
			}

			return new TextureContents(
					combined,
					new TextureResourceMetadata(true, false, MipmapStrategy.MEAN, 0.0F)
			);
		}
	}

	@Override
	protected void load(NativeImage image) {
		GpuDevice gpuDevice = RenderSystem.getDevice();
		int faceWidth = image.getWidth();
		int totalHeight = image.getHeight();

		// Если пришёл fallback (высота не кратна 6), используем квадратный размер грани.
		int faceHeight = totalHeight % FACE_COUNT == 0 ? totalHeight / FACE_COUNT : faceWidth;

		close();
		glTexture = gpuDevice.createTexture(getId()::toString, 21, TextureFormat.RGBA8, faceWidth, faceHeight, FACE_COUNT, 1);
		glTextureView = gpuDevice.createTextureView(glTexture);

		for (int k = 0; k < FACE_COUNT; k++) {
			int srcY = k * faceHeight;

			if (srcY + faceHeight <= totalHeight) {
				gpuDevice.createCommandEncoder().writeToTexture(glTexture, image, 0, k, 0, 0, faceWidth, faceHeight, 0, srcY);
			}
		}
	}
}

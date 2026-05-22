package net.minecraft.client.texture;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.path.PathUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Environment(EnvType.CLIENT)
/**
 * {@code PlayerSkinTextureDownloader}.
 */
public class PlayerSkinTextureDownloader {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int SKIN_WIDTH = 64;
	private static final int SKIN_HEIGHT = 64;
	private static final int OLD_SKIN_HEIGHT = 32;
	private final Proxy proxy;
	private final TextureManager textureManager;
	private final Executor executor;

	public PlayerSkinTextureDownloader(Proxy proxy, TextureManager textureManager, Executor executor) {
		this.proxy = proxy;
		this.textureManager = textureManager;
		this.executor = executor;
	}

	public CompletableFuture<AssetInfo.TextureAsset> downloadAndRegisterTexture(
			Identifier id,
			Path path,
			String url,
			boolean remap
	) {
		AssetInfo.SkinAssetInfo skinAssetInfo = new AssetInfo.SkinAssetInfo(id, url);
		return CompletableFuture.<NativeImage>supplyAsync(
				() -> {
					NativeImage nativeImage;
					try {
						nativeImage = this.download(path, skinAssetInfo.url());
					}
					catch (IOException var6) {
						throw new UncheckedIOException(var6);
					}

					return remap ? remapTexture(nativeImage, skinAssetInfo.url()) : nativeImage;
				}, Util.getDownloadWorkerExecutor().named("downloadTexture")
		).thenCompose(image -> this.registerTexture(skinAssetInfo, image));
	}

	private NativeImage download(Path path, String url) throws IOException {
		if (Files.isRegularFile(path)) {
			LOGGER.debug("Loading HTTP texture from local cache ({})", path);

			NativeImage var18;
			try (InputStream inputStream = Files.newInputStream(path)) {
				var18 = NativeImage.read(inputStream);
			}

			return var18;
		}
		else {
			HttpURLConnection httpURLConnection = null;
			LOGGER.debug("Downloading HTTP texture from {} to {}", url, path);
			URI uRI = URI.create(url);

			NativeImage iOException;
			try {
				httpURLConnection = (HttpURLConnection) uRI.toURL().openConnection(this.proxy);
				httpURLConnection.setDoInput(true);
				httpURLConnection.setDoOutput(false);
				httpURLConnection.connect();
				int i = httpURLConnection.getResponseCode();
				if (i / 100 != 2) {
					throw new IOException("Failed to open " + uRI + ", HTTP error code: " + i);
				}

				byte[] bs = httpURLConnection.getInputStream().readAllBytes();

				try {
					PathUtil.createDirectories(path.getParent());
					Files.write(path, bs);
				}
				catch (IOException var14) {
					LOGGER.warn("Failed to cache texture {} in {}", url, path);
				}

				iOException = NativeImage.read(bs);
			}
			finally {
				if (httpURLConnection != null) {
					httpURLConnection.disconnect();
				}
			}

			return iOException;
		}
	}

	private CompletableFuture<AssetInfo.TextureAsset> registerTexture(
			AssetInfo.TextureAsset textureAsset,
			NativeImage image
	) {
		return CompletableFuture.supplyAsync(
				() -> {
					NativeImageBackedTexture
							nativeImageBackedTexture =
							new NativeImageBackedTexture(textureAsset.texturePath()::toString, image);
					this.textureManager.registerTexture(textureAsset.texturePath(), nativeImageBackedTexture);
					return textureAsset;
				}, this.executor
		);
	}

	private static NativeImage remapTexture(NativeImage image, String uri) {
		int i = image.getHeight();
		int j = image.getWidth();
		if (j == SKIN_HEIGHT && (i == OLD_SKIN_HEIGHT || i == 64)) {
			boolean bl = i == OLD_SKIN_HEIGHT;
			if (bl) {
				NativeImage nativeImage = new NativeImage(SKIN_WIDTH, 64, true);
				nativeImage.copyFrom(image);
				image.close();
				image = nativeImage;
				nativeImage.fillRect(0, OLD_SKIN_HEIGHT, SKIN_HEIGHT, OLD_SKIN_HEIGHT, 0);
				nativeImage.copyRect(4, 16, 16, OLD_SKIN_HEIGHT, 4, 4, true, false);
				nativeImage.copyRect(8, 16, 16, OLD_SKIN_HEIGHT, 4, 4, true, false);
				nativeImage.copyRect(0, 20, 24, OLD_SKIN_HEIGHT, 4, 12, true, false);
				nativeImage.copyRect(4, 20, 16, OLD_SKIN_HEIGHT, 4, 12, true, false);
				nativeImage.copyRect(8, 20, 8, OLD_SKIN_HEIGHT, 4, 12, true, false);
				nativeImage.copyRect(12, 20, 16, OLD_SKIN_HEIGHT, 4, 12, true, false);
				nativeImage.copyRect(44, 16, -8, OLD_SKIN_HEIGHT, 4, 4, true, false);
				nativeImage.copyRect(48, 16, -8, OLD_SKIN_HEIGHT, 4, 4, true, false);
				nativeImage.copyRect(40, 20, 0, OLD_SKIN_HEIGHT, 4, 12, true, false);
				nativeImage.copyRect(44, 20, -8, OLD_SKIN_HEIGHT, 4, 12, true, false);
				nativeImage.copyRect(48, 20, -16, OLD_SKIN_HEIGHT, 4, 12, true, false);
				nativeImage.copyRect(52, 20, -8, OLD_SKIN_HEIGHT, 4, 12, true, false);
			}

			stripAlpha(image, 0, 0, OLD_SKIN_HEIGHT, 16);
			if (bl) {
				stripColor(image, OLD_SKIN_HEIGHT, 0, SKIN_HEIGHT, OLD_SKIN_HEIGHT);
			}

			stripAlpha(image, 0, 16, SKIN_HEIGHT, OLD_SKIN_HEIGHT);
			stripAlpha(image, 16, 48, 48, SKIN_WIDTH);
			return image;
		}
		else {
			image.close();
			throw new IllegalStateException(
					"Discarding incorrectly sized (" + j + "x" + i + ") skin texture from " + uri);
		}
	}

	private static void stripColor(NativeImage image, int x1, int y1, int x2, int y2) {
		for (int i = x1; i < x2; i++) {
			for (int j = y1; j < y2; j++) {
				int k = image.getColorArgb(i, j);
				if (ColorHelper.getAlpha(k) < 128) {
					return;
				}
			}
		}

		for (int i = x1; i < x2; i++) {
			for (int jx = y1; jx < y2; jx++) {
				image.setColorArgb(i, jx, image.getColorArgb(i, jx) & 16777215);
			}
		}
	}

	private static void stripAlpha(NativeImage image, int x1, int y1, int x2, int y2) {
		for (int i = x1; i < x2; i++) {
			for (int j = y1; j < y2; j++) {
				image.setColorArgb(i, j, ColorHelper.fullAlpha(image.getColorArgb(i, j)));
			}
		}
	}
}

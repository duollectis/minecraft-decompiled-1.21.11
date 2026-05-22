package net.minecraft.client.realms.util;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.realms.FileUpload;
import net.minecraft.client.realms.RealmsClient;
import net.minecraft.client.realms.dto.RealmsSlot;
import net.minecraft.client.realms.dto.UploadInfo;
import net.minecraft.client.realms.exception.RealmsServiceException;
import net.minecraft.client.realms.exception.RealmsUploadException;
import net.minecraft.client.realms.exception.RetryCallException;
import net.minecraft.client.realms.exception.upload.CancelledRealmsUploadException;
import net.minecraft.client.realms.exception.upload.CloseFailureRealmsUploadException;
import net.minecraft.client.realms.exception.upload.FailedRealmsUploadException;
import net.minecraft.client.session.Session;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsUploader}.
 */
public class RealmsUploader {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final int MAX_ATTEMPTS = 20;
	private final RealmsClient client = RealmsClient.create();
	private final Path directory;
	private final RealmsSlot options;
	private final Session session;
	private final long worldId;
	private final UploadProgressTracker progressTracker;
	private volatile boolean cancelled;
	private volatile @Nullable CompletableFuture<?> upload;

	public RealmsUploader(
			Path directory,
			RealmsSlot options,
			Session session,
			long worldId,
			UploadProgressTracker progressTracker
	) {
		this.directory = directory;
		this.options = options;
		this.session = session;
		this.worldId = worldId;
		this.progressTracker = progressTracker;
	}

	/**
	 * Upload.
	 *
	 * @return CompletableFuture — результат операции
	 */
	public CompletableFuture<?> upload() {
		return CompletableFuture.runAsync(
				() -> {
					File file = null;

					try {
						UploadInfo uploadInfo = this.uploadSync();
						file = UploadCompressor.compress(this.directory, () -> this.cancelled);
						this.progressTracker.updateProgressDisplay();

						try (FileUpload fileUpload = new FileUpload(
								file,
								this.worldId,
								this.options.slotId,
								uploadInfo,
								this.session,
								SharedConstants.getGameVersion().name(),
								this.options.options.version,
								this.progressTracker.getUploadProgress()
						)
						) {
							CompletableFuture<UploadResult> completableFuture = fileUpload.upload();
							this.upload = completableFuture;
							if (!this.cancelled) {
								UploadResult uploadResult;
								try {
									uploadResult = completableFuture.join();
								}
								catch (CompletionException var17) {
									throw var17.getCause();
								}

								String string = uploadResult.getErrorMessage();
								if (string != null) {
									throw new FailedRealmsUploadException(string);
								}

								UploadTokenCache.invalidate(this.worldId);
								this.client.updateSlot(
										this.worldId,
										this.options.slotId,
										this.options.options,
										this.options.settings
								);
								return;
							}

							completableFuture.cancel(true);
						}
					}
					catch (RealmsServiceException var19) {
						throw new FailedRealmsUploadException(var19.error.getText());
					}
					catch (CancellationException | InterruptedException var20) {
						throw new CancelledRealmsUploadException();
					}
					catch (RealmsUploadException var21) {
						throw var21;
					}
					catch (Throwable var22) {
						if (var22 instanceof Error error) {
							throw error;
						}

						throw new FailedRealmsUploadException(var22.getMessage());
					}
					finally {
						if (file != null) {
							LOGGER.debug("Deleting file {}", file.getAbsolutePath());
							file.delete();
						}
					}
				},
				Util.getMainWorkerExecutor()
		);
	}

	/**
	 * Проверяет возможность cel.
	 */
	public void cancel() {
		this.cancelled = true;
		CompletableFuture<?> completableFuture = this.upload;
		if (completableFuture != null) {
			completableFuture.cancel(true);
		}
	}

	private UploadInfo uploadSync() throws RealmsServiceException, InterruptedException {
		for (int i = 0; i < MAX_ATTEMPTS; i++) {
			try {
				UploadInfo uploadInfo = this.client.upload(this.worldId);
				if (this.cancelled) {
					throw new CancelledRealmsUploadException();
				}

				if (uploadInfo != null) {
					if (!uploadInfo.worldClosed()) {
						throw new CloseFailureRealmsUploadException();
					}

					return uploadInfo;
				}
			}
			catch (RetryCallException var3) {
				Thread.sleep(var3.delaySeconds * 1000L);
			}
		}

		throw new CloseFailureRealmsUploadException();
	}
}

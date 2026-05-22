package net.minecraft.client.realms.gui.screen;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.realms.SizeUnit;
import net.minecraft.client.realms.dto.RealmsSettingDto;
import net.minecraft.client.realms.dto.RealmsSlot;
import net.minecraft.client.realms.dto.RealmsWorldOptions;
import net.minecraft.client.realms.exception.RealmsUploadException;
import net.minecraft.client.realms.task.LongRunningTask;
import net.minecraft.client.realms.task.SwitchSlotTask;
import net.minecraft.client.realms.task.WorldCreationTask;
import net.minecraft.client.realms.util.RealmsUploader;
import net.minecraft.client.realms.util.UploadProgress;
import net.minecraft.client.realms.util.UploadProgressTracker;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.world.level.storage.LevelSummary;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsUploadScreen}.
 */
public class RealmsUploadScreen extends RealmsScreen implements UploadProgressTracker {

	private static final int PROGRESS_BAR_WIDTH = 200;
	private static final int PROGRESS_BAR_TOP = 80;
	private static final int PROGRESS_BAR_BOTTOM = 95;
	private static final int PROGRESS_BAR_BORDER = 1;
	private static final String[] DOTS = new String[]{"", ".", ". .", ". . ."};
	private static final Text VERIFYING_TEXT = Text.translatable("mco.upload.verifying");
	private final RealmsCreateWorldScreen parent;
	private final LevelSummary selectedLevel;
	private final @Nullable WorldCreationTask creationTask;
	private final long worldId;
	private final int slotId;
	final AtomicReference<@Nullable RealmsUploader> uploader = new AtomicReference<>();
	private final UploadProgress uploadProgress;
	private final RateLimiter narrationRateLimiter;
	private volatile Text @Nullable [] statusTexts;
	private volatile Text status = Text.translatable("mco.upload.preparing");
	private volatile @Nullable String progress;
	private volatile boolean cancelled;
	private volatile boolean uploadFinished;
	private volatile boolean showDots = true;
	private volatile boolean uploadStarted;
	private @Nullable ButtonWidget backButton;
	private @Nullable ButtonWidget cancelButton;
	private int animTick;
	private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);

	public RealmsUploadScreen(
			@Nullable WorldCreationTask creationTask,
			long worldId,
			int slotId,
			RealmsCreateWorldScreen parent,
			LevelSummary selectedLevel
	) {
		super(NarratorManager.EMPTY);
		this.creationTask = creationTask;
		this.worldId = worldId;
		this.slotId = slotId;
		this.parent = parent;
		this.selectedLevel = selectedLevel;
		this.uploadProgress = new UploadProgress();
		this.narrationRateLimiter = RateLimiter.create(0.1F);
	}

	@Override
	public void init() {
		this.backButton =
				this.layout.addFooter(ButtonWidget.builder(ScreenTexts.BACK, button -> this.onBack()).build());
		this.backButton.visible = false;
		this.cancelButton =
				this.layout.addFooter(ButtonWidget.builder(ScreenTexts.CANCEL, button -> this.onCancel()).build());
		if (!this.uploadStarted) {
			if (this.parent.slot == -1) {
				this.uploadStarted = true;
				this.upload();
			}
			else {
				List<LongRunningTask> list = new ArrayList<>();
				if (this.creationTask != null) {
					list.add(this.creationTask);
				}

				list.add(new SwitchSlotTask(
						this.worldId, this.parent.slot, () -> {
					if (!this.uploadStarted) {
						this.uploadStarted = true;
						this.client.execute(() -> {
							this.client.setScreen(this);
							this.upload();
						});
					}
				}
				));
				this.client.setScreen(new RealmsLongRunningMcoTaskScreen(
						this.parent,
						list.toArray(new LongRunningTask[0])
				));
			}
		}

		this.layout.forEachChild(child -> {
			ClickableWidget var10000 = this.addDrawableChild(child);
		});
		this.refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		this.layout.refreshPositions();
	}

	private void onBack() {
		this.client.setScreen(new RealmsConfigureWorldScreen(new RealmsMainScreen(new TitleScreen()), this.worldId));
	}

	private void onCancel() {
		this.cancelled = true;
		RealmsUploader realmsUploader = this.uploader.get();
		if (realmsUploader != null) {
			realmsUploader.cancel();
		}
		else {
			this.client.setScreen(this.parent);
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.key() == 256) {
			if (this.showDots) {
				this.onCancel();
			}
			else {
				this.onBack();
			}

			return true;
		}
		else {
			return super.keyPressed(input);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		if (!this.uploadFinished && this.uploadProgress.hasWrittenBytes() && this.uploadProgress.hasWrittenAllBytes()
				&& this.cancelButton != null) {
			this.status = VERIFYING_TEXT;
			this.cancelButton.active = false;
		}

		context.drawCenteredTextWithShadow(this.textRenderer, this.status, this.width / 2, 50, -1);
		if (this.showDots) {
			context.drawTextWithShadow(
					this.textRenderer,
					DOTS[this.animTick / 10 % DOTS.length],
					this.width / 2 + this.textRenderer.getWidth(this.status) / 2 + 5,
					50,
					-1
			);
		}

		if (this.uploadProgress.hasWrittenBytes() && !this.cancelled) {
			this.drawProgressBar(context);
			this.drawUploadSpeed(context);
		}

		Text[] texts = this.statusTexts;
		if (texts != null) {
			for (int i = 0; i < texts.length; i++) {
				context.drawCenteredTextWithShadow(this.textRenderer, texts[i], this.width / 2, 110 + 12 * i, -65536);
			}
		}
	}

	private void drawProgressBar(DrawContext context) {
		double d = this.uploadProgress.getFractionBytesWritten();
		this.progress = String.format(Locale.ROOT, "%.1f", d * 100.0);
		int i = (this.width - PROGRESS_BAR_WIDTH) / 2;
		int j = i + (int) Math.round(200.0 * d);
		context.fill(i - 1, 79, j + 1, 96, -1);
		context.fill(i, PROGRESS_BAR_TOP, j, PROGRESS_BAR_BOTTOM, -8355712);
		context.drawCenteredTextWithShadow(
				this.textRenderer,
				Text.translatable("mco.upload.percent", this.progress),
				this.width / 2,
				84,
				-1
		);
	}

	private void drawUploadSpeed(DrawContext context) {
		this.drawUploadSpeed0(context, this.uploadProgress.getBytesPerSecond());
	}

	private void drawUploadSpeed0(DrawContext context, long bytesPerSecond) {
		String string = this.progress;
		if (bytesPerSecond > 0L && string != null) {
			int i = this.textRenderer.getWidth(string);
			String string2 = "(" + SizeUnit.getUserFriendlyString(bytesPerSecond) + "/s)";
			context.drawTextWithShadow(this.textRenderer, string2, this.width / 2 + i / 2 + 15, 84, -1);
		}
	}

	@Override
	public void tick() {
		super.tick();
		this.animTick++;
		this.uploadProgress.tick();
		if (this.narrationRateLimiter.tryAcquire(1)) {
			Text text = this.getNarration();
			this.client.getNarratorManager().narrateSystemImmediately(text);
		}
	}

	private Text getNarration() {
		List<Text> list = Lists.newArrayList();
		list.add(this.status);
		if (this.progress != null) {
			list.add(Text.translatable("mco.upload.percent", this.progress));
		}

		Text[] texts = this.statusTexts;
		if (texts != null) {
			list.addAll(Arrays.asList(texts));
		}

		return ScreenTexts.joinLines(list);
	}

	private void upload() {
		Path path = this.client.runDirectory.toPath().resolve("saves").resolve(this.selectedLevel.getName());
		RealmsWorldOptions
				realmsWorldOptions =
				RealmsWorldOptions.create(
						this.selectedLevel.getLevelInfo(),
						this.selectedLevel.getVersionInfo().getVersionName()
				);
		RealmsSlot realmsSlot = new RealmsSlot(
				this.slotId,
				realmsWorldOptions,
				List.of(RealmsSettingDto.ofHardcore(this.selectedLevel.getLevelInfo().isHardcore()))
		);
		RealmsUploader
				realmsUploader =
				new RealmsUploader(path, realmsSlot, this.client.getSession(), this.worldId, this);
		if (!this.uploader.compareAndSet(null, realmsUploader)) {
			throw new IllegalStateException("Tried to start uploading but was already uploading");
		}
		else {
			realmsUploader.upload().handleAsync(
					(v, throwable) -> {
						if (throwable != null) {
							if (throwable instanceof CompletionException completionException) {
								throwable = completionException.getCause();
							}

							if (throwable instanceof RealmsUploadException realmsUploadException) {
								if (realmsUploadException.getStatus() != null) {
									this.status = realmsUploadException.getStatus();
								}

								this.setStatusTexts(realmsUploadException.getStatusTexts());
							}
							else {
								this.status = Text.translatable("mco.upload.failed", throwable.getMessage());
							}
						}
						else {
							this.status = Text.translatable("mco.upload.done");
							if (this.backButton != null) {
								this.backButton.setMessage(ScreenTexts.DONE);
							}
						}

						this.uploadFinished = true;
						this.showDots = false;
						if (this.backButton != null) {
							this.backButton.visible = true;
						}

						if (this.cancelButton != null) {
							this.cancelButton.visible = false;
						}

						this.uploader.set(null);
						return null;
					}, this.client
			);
		}
	}

	private void setStatusTexts(Text @Nullable ... statusTexts) {
		this.statusTexts = statusTexts;
	}

	@Override
	public UploadProgress getUploadProgress() {
		return this.uploadProgress;
	}

	@Override
	public void updateProgressDisplay() {
		this.status = Text.translatable("mco.upload.uploading", this.selectedLevel.getDisplayName());
	}
}

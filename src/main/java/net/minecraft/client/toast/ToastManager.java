package net.minecraft.client.toast;

import com.google.common.collect.Queues;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.MusicToastMode;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;

import java.util.*;

@Environment(EnvType.CLIENT)
/**
 * {@code ToastManager}.
 */
public class ToastManager {

	private static final int SPACES = 5;
	private static final int NO_TOAST = -1;
	final MinecraftClient client;
	private final List<ToastManager.Entry<?>> visibleEntries = new ArrayList<>();
	private final BitSet occupiedSpaces = new BitSet(5);
	private final Deque<Toast> toastQueue = Queues.newArrayDeque();
	private final Set<SoundEvent> queuedToastSounds = new HashSet<>();
	private ToastManager.@Nullable Entry<NowPlayingToast> nowPlayingToast;

	public ToastManager(MinecraftClient client, GameOptions gameOptions) {
		this.client = client;
		this.initMusicToast(gameOptions.getMusicToast().getValue());
	}

	/**
	 * Update.
	 */
	public void update() {
		MutableBoolean mutableBoolean = new MutableBoolean(false);
		this.visibleEntries.removeIf(entry -> {
			Toast.Visibility visibility = entry.visibility;
			entry.update();
			if (entry.visibility != visibility && mutableBoolean.isFalse()) {
				mutableBoolean.setTrue();
				entry.visibility.playSound(this.client.getSoundManager());
			}

			if (entry.isFinishedRendering()) {
				this.occupiedSpaces.clear(entry.topIndex, entry.topIndex + entry.requiredSpaceCount);
				return true;
			}
			else {
				return false;
			}
		});
		if (!this.toastQueue.isEmpty() && this.getEmptySpaceCount() > 0) {
			this.toastQueue.removeIf(toast -> {
				int i = toast.getRequiredSpaceCount();
				int j = this.getTopIndex(i);
				if (j == -1) {
					return false;
				}
				else {
					this.visibleEntries.add(new ToastManager.Entry<>(toast, j, i));
					this.occupiedSpaces.set(j, j + i);
					SoundEvent soundEvent = toast.getSoundEvent();
					if (soundEvent != null && this.queuedToastSounds.add(soundEvent)) {
						this.client.getSoundManager().play(PositionedSoundInstance.master(soundEvent, 1.0F, 1.0F));
					}

					return true;
				}
			});
		}

		this.queuedToastSounds.clear();
		if (this.nowPlayingToast != null) {
			this.nowPlayingToast.update();
		}
	}

	/**
	 * Draw.
	 *
	 * @param context context
	 */
	public void draw(DrawContext context) {
		if (!this.client.options.hudHidden) {
			int i = context.getScaledWindowWidth();
			if (!this.visibleEntries.isEmpty()) {
				context.createNewRootLayer();
			}

			for (ToastManager.Entry<?> entry : this.visibleEntries) {
				entry.draw(context, i);
			}

			if (this.client.options.getMusicToast().getValue().canShowAsToast()
					&& this.nowPlayingToast != null
					&& (this.client.currentScreen == null || !(this.client.currentScreen instanceof GameMenuScreen))) {
				this.nowPlayingToast.draw(context, i);
			}
		}
	}

	private int getTopIndex(int requiredSpaces) {
		if (this.getEmptySpaceCount() >= requiredSpaces) {
			int i = 0;

			for (int j = 0; j < 5; j++) {
				if (this.occupiedSpaces.get(j)) {
					i = 0;
				}
				else if (++i == requiredSpaces) {
					return j + 1 - i;
				}
			}
		}

		return -1;
	}

	private int getEmptySpaceCount() {
		return 5 - this.occupiedSpaces.cardinality();
	}

	public <T extends Toast> @Nullable T getToast(Class<? extends T> toastClass, Object type) {
		for (ToastManager.Entry<?> entry : this.visibleEntries) {
			if (toastClass.isAssignableFrom(entry.getInstance().getClass()) && entry
					.getInstance()
					.getType()
					.equals(type)) {
				return (T) entry.getInstance();
			}
		}

		for (Toast toast : this.toastQueue) {
			if (toastClass.isAssignableFrom(toast.getClass()) && toast.getType().equals(type)) {
				return (T) toast;
			}
		}

		return null;
	}

	/**
	 * Clear.
	 */
	public void clear() {
		this.occupiedSpaces.clear();
		this.visibleEntries.clear();
		this.toastQueue.clear();
	}

	/**
	 * Add.
	 *
	 * @param toast toast
	 */
	public void add(Toast toast) {
		this.toastQueue.add(toast);
	}

	/**
	 * Обрабатывает событие music track start.
	 */
	public void onMusicTrackStart() {
		if (this.nowPlayingToast != null) {
			this.nowPlayingToast.init();
			this.nowPlayingToast.getInstance().show(this.client.options);
		}
	}

	/**
	 * Обрабатывает событие music track stop.
	 */
	public void onMusicTrackStop() {
		if (this.nowPlayingToast != null) {
			this.nowPlayingToast.getInstance().setVisibility(Toast.Visibility.HIDE);
		}
	}

	public MinecraftClient getClient() {
		return this.client;
	}

	public double getNotificationDisplayTimeMultiplier() {
		return this.client.options.getNotificationDisplayTime().getValue();
	}

	private void initMusicToast(MusicToastMode toastMode) {
		switch (toastMode) {
			case PAUSE:
			case PAUSE_AND_TOAST:
				this.nowPlayingToast = new ToastManager.Entry<>(new NowPlayingToast(), 0, 0);
		}
	}

	/**
	 * Обрабатывает событие music toast mode updated.
	 *
	 * @param toastMode toast mode
	 */
	public void onMusicToastModeUpdated(MusicToastMode toastMode) {
		switch (toastMode) {
			case PAUSE:
				this.nowPlayingToast = new ToastManager.Entry<>(new NowPlayingToast(), 0, 0);
				break;
			case PAUSE_AND_TOAST:
				this.nowPlayingToast = new ToastManager.Entry<>(new NowPlayingToast(), 0, 0);
				if (this.client.options.getSoundVolume(SoundCategory.MUSIC) > 0.0F) {
					this.nowPlayingToast.getInstance().show(this.client.options);
				}
				break;
			case NEVER:
				this.nowPlayingToast = null;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Entry}.
	 */
	class Entry<T extends Toast> {

		private static final long DISAPPEAR_TIME = 600L;
		private final T instance;
		final int topIndex;
		final int requiredSpaceCount;
		private long startTime;
		private long fullyVisibleTime;
		Toast.Visibility visibility;
		private long showTime;
		private float visibleWidthPortion;
		protected boolean finishedRendering;

		Entry(final T instance, final int topIndex, final int requiredSpaceCount) {
			this.instance = instance;
			this.topIndex = topIndex;
			this.requiredSpaceCount = requiredSpaceCount;
			this.init();
		}

		public T getInstance() {
			return this.instance;
		}

		/**
		 * Init.
		 */
		public void init() {
			this.startTime = -1L;
			this.fullyVisibleTime = -1L;
			this.visibility = Toast.Visibility.HIDE;
			this.showTime = 0L;
			this.visibleWidthPortion = 0.0F;
			this.finishedRendering = false;
		}

		public boolean isFinishedRendering() {
			return this.finishedRendering;
		}

		private void updateVisibleWidthPortion(long time) {
			float f = MathHelper.clamp((float) (time - this.startTime) / 600.0F, 0.0F, 1.0F);
			f *= f;
			if (this.visibility == Toast.Visibility.HIDE) {
				this.visibleWidthPortion = 1.0F - f;
			}
			else {
				this.visibleWidthPortion = f;
			}
		}

		/**
		 * Update.
		 */
		public void update() {
			long l = Util.getMeasuringTimeMs();
			if (this.startTime == -1L) {
				this.startTime = l;
				this.visibility = Toast.Visibility.SHOW;
			}

			if (this.visibility == Toast.Visibility.SHOW && l - this.startTime <= 600L) {
				this.fullyVisibleTime = l;
			}

			this.showTime = l - this.fullyVisibleTime;
			this.updateVisibleWidthPortion(l);
			this.instance.update(ToastManager.this, this.showTime);
			Toast.Visibility visibility = this.instance.getVisibility();
			if (visibility != this.visibility) {
				this.startTime = l - (int) ((1.0F - this.visibleWidthPortion) * 600.0F);
				this.visibility = visibility;
			}

			boolean bl = this.finishedRendering;
			this.finishedRendering = this.visibility == Toast.Visibility.HIDE && l - this.startTime > 600L;
			if (this.finishedRendering && !bl) {
				this.instance.onFinishedRendering();
			}
		}

		/**
		 * Draw.
		 *
		 * @param context context
		 * @param scaledWindowWidth scaled window width
		 */
		public void draw(DrawContext context, int scaledWindowWidth) {
			if (!this.finishedRendering) {
				context.getMatrices().pushMatrix();
				context
						.getMatrices()
						.translate(
								this.instance.getXPos(scaledWindowWidth, this.visibleWidthPortion),
								this.instance.getYPos(this.topIndex)
						);
				this.instance.draw(context, ToastManager.this.client.textRenderer, this.showTime);
				context.getMatrices().popMatrix();
			}
		}
	}
}

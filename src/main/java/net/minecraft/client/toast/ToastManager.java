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

/**
 * Менеджер всплывающих уведомлений (toast).
 *
 * <p>Управляет очередью ожидающих toast ({@code toastQueue}) и списком
 * активно отображаемых ({@code visibleEntries}). Каждый тик перемещает
 * toast из очереди в видимые, если есть свободные слоты (максимум {@link #SPACES}).
 * Отдельно управляет {@link NowPlayingToast} для музыкальных уведомлений.
 */
@Environment(EnvType.CLIENT)
public class ToastManager {

	private static final int SPACES = 5;
	private static final int NO_SLOT = -1;

	final MinecraftClient client;
	private final List<Entry<?>> visibleEntries = new ArrayList<>();
	private final BitSet occupiedSpaces = new BitSet(SPACES);
	private final Deque<Toast> toastQueue = Queues.newArrayDeque();
	private final Set<SoundEvent> queuedToastSounds = new HashSet<>();
	private @Nullable Entry<NowPlayingToast> nowPlayingToast;

	public ToastManager(MinecraftClient client, GameOptions gameOptions) {
		this.client = client;
		initMusicToast(gameOptions.getMusicToast().getValue());
	}

	/**
	 * Обновляет состояние всех видимых toast и перемещает ожидающие из очереди.
	 * Воспроизводит звук при первом изменении видимости за тик.
	 */
	public void update() {
		MutableBoolean soundPlayed = new MutableBoolean(false);

		visibleEntries.removeIf(entry -> {
			Toast.Visibility prevVisibility = entry.visibility;
			entry.update();

			if (entry.visibility != prevVisibility && soundPlayed.isFalse()) {
				soundPlayed.setTrue();
				entry.visibility.playSound(client.getSoundManager());
			}

			if (entry.isFinishedRendering()) {
				occupiedSpaces.clear(entry.topIndex, entry.topIndex + entry.requiredSpaceCount);
				return true;
			}

			return false;
		});

		if (!toastQueue.isEmpty() && getEmptySpaceCount() > 0) {
			toastQueue.removeIf(toast -> {
				int requiredSpaces = toast.getRequiredSpaceCount();
				int topIndex = getTopIndex(requiredSpaces);

				if (topIndex == NO_SLOT) {
					return false;
				}

				visibleEntries.add(new Entry<>(toast, topIndex, requiredSpaces));
				occupiedSpaces.set(topIndex, topIndex + requiredSpaces);

				SoundEvent soundEvent = toast.getSoundEvent();
				if (soundEvent != null && queuedToastSounds.add(soundEvent)) {
					client.getSoundManager().play(PositionedSoundInstance.master(soundEvent, 1.0F, 1.0F));
				}

				return true;
			});
		}

		queuedToastSounds.clear();

		if (nowPlayingToast != null) {
			nowPlayingToast.update();
		}
	}

	public void draw(DrawContext context) {
		if (client.options.hudHidden) {
			return;
		}

		int windowWidth = context.getScaledWindowWidth();

		if (!visibleEntries.isEmpty()) {
			context.createNewRootLayer();
		}

		for (Entry<?> entry : visibleEntries) {
			entry.draw(context, windowWidth);
		}

		if (client.options.getMusicToast().getValue().canShowAsToast()
			&& nowPlayingToast != null
			&& (client.currentScreen == null || !(client.currentScreen instanceof GameMenuScreen))
		) {
			nowPlayingToast.draw(context, windowWidth);
		}
	}

	/**
	 * Ищет первый непрерывный блок из {@code requiredSpaces} свободных слотов.
	 * Возвращает {@link #NO_SLOT} если места нет.
	 */
	private int getTopIndex(int requiredSpaces) {
		if (getEmptySpaceCount() < requiredSpaces) {
			return NO_SLOT;
		}

		int consecutive = 0;

		for (int slot = 0; slot < SPACES; slot++) {
			if (occupiedSpaces.get(slot)) {
				consecutive = 0;
			} else if (++consecutive == requiredSpaces) {
				return slot + 1 - consecutive;
			}
		}

		return NO_SLOT;
	}

	private int getEmptySpaceCount() {
		return SPACES - occupiedSpaces.cardinality();
	}

	@SuppressWarnings("unchecked")
	public <T extends Toast> @Nullable T getToast(Class<? extends T> toastClass, Object type) {
		for (Entry<?> entry : visibleEntries) {
			if (toastClass.isAssignableFrom(entry.getInstance().getClass())
				&& entry.getInstance().getType().equals(type)
			) {
				return (T) entry.getInstance();
			}
		}

		for (Toast toast : toastQueue) {
			if (toastClass.isAssignableFrom(toast.getClass()) && toast.getType().equals(type)) {
				return (T) toast;
			}
		}

		return null;
	}

	public void clear() {
		occupiedSpaces.clear();
		visibleEntries.clear();
		toastQueue.clear();
	}

	public void add(Toast toast) {
		toastQueue.add(toast);
	}

	public void onMusicTrackStart() {
		if (nowPlayingToast != null) {
			nowPlayingToast.init();
			nowPlayingToast.getInstance().show(client.options);
		}
	}

	public void onMusicTrackStop() {
		if (nowPlayingToast != null) {
			nowPlayingToast.getInstance().setVisibility(Toast.Visibility.HIDE);
		}
	}

	public MinecraftClient getClient() {
		return client;
	}

	public double getNotificationDisplayTimeMultiplier() {
		return client.options.getNotificationDisplayTime().getValue();
	}

	private void initMusicToast(MusicToastMode toastMode) {
		switch (toastMode) {
			case PAUSE, PAUSE_AND_TOAST -> nowPlayingToast = new Entry<>(new NowPlayingToast(), 0, 0);
		}
	}

	/**
	 * Обновляет режим музыкального toast при изменении настройки.
	 * При {@code PAUSE_AND_TOAST} немедленно показывает toast, если музыка играет.
	 */
	public void onMusicToastModeUpdated(MusicToastMode toastMode) {
		switch (toastMode) {
			case PAUSE -> nowPlayingToast = new Entry<>(new NowPlayingToast(), 0, 0);
			case PAUSE_AND_TOAST -> {
				nowPlayingToast = new Entry<>(new NowPlayingToast(), 0, 0);
				if (client.options.getSoundVolume(SoundCategory.MUSIC) > 0.0F) {
					nowPlayingToast.getInstance().show(client.options);
				}
			}
			case NEVER -> nowPlayingToast = null;
		}
	}

	/**
	 * Запись активного toast: хранит экземпляр, позицию в стеке и состояние анимации.
	 */
	@Environment(EnvType.CLIENT)
	class Entry<T extends Toast> {

		private static final long SLIDE_DURATION_MS = 600L;

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
			init();
		}

		public T getInstance() {
			return instance;
		}

		public void init() {
			startTime = -1L;
			fullyVisibleTime = -1L;
			visibility = Toast.Visibility.HIDE;
			showTime = 0L;
			visibleWidthPortion = 0.0F;
			finishedRendering = false;
		}

		public boolean isFinishedRendering() {
			return finishedRendering;
		}

		private void updateVisibleWidthPortion(long time) {
			float progress = MathHelper.clamp((float) (time - startTime) / SLIDE_DURATION_MS, 0.0F, 1.0F);
			float eased = progress * progress;
			visibleWidthPortion = visibility == Toast.Visibility.HIDE ? 1.0F - eased : eased;
		}

		public void update() {
			long now = Util.getMeasuringTimeMs();

			if (startTime == -1L) {
				startTime = now;
				visibility = Toast.Visibility.SHOW;
			}

			if (visibility == Toast.Visibility.SHOW && now - startTime <= SLIDE_DURATION_MS) {
				fullyVisibleTime = now;
			}

			showTime = now - fullyVisibleTime;
			updateVisibleWidthPortion(now);
			instance.update(ToastManager.this, showTime);

			Toast.Visibility newVisibility = instance.getVisibility();

			if (newVisibility != visibility) {
				startTime = now - (int) ((1.0F - visibleWidthPortion) * SLIDE_DURATION_MS);
				visibility = newVisibility;
			}

			boolean wasFinished = finishedRendering;
			finishedRendering = visibility == Toast.Visibility.HIDE && now - startTime > SLIDE_DURATION_MS;

			if (finishedRendering && !wasFinished) {
				instance.onFinishedRendering();
			}
		}

		public void draw(DrawContext context, int scaledWindowWidth) {
			if (finishedRendering) {
				return;
			}

			context.getMatrices().pushMatrix();
			context.getMatrices().translate(
				instance.getXPos(scaledWindowWidth, visibleWidthPortion),
				instance.getYPos(topIndex)
			);
			instance.draw(context, ToastManager.this.client.textRenderer, showTime);
			context.getMatrices().popMatrix();
		}
	}
}

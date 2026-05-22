package net.minecraft.client.sound;

import com.google.common.collect.*;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.Camera;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Ядро звуковой системы клиента. Управляет жизненным циклом звуков:
 * воспроизведением, паузой, остановкой, обновлением позиции слушателя
 * и автоматической перезагрузкой при смене аудиоустройства.
 */
@Environment(EnvType.CLIENT)
public class SoundSystem {

	private static final Marker MARKER = MarkerFactory.getMarker("SOUNDS");
	private static final Logger LOGGER = LogUtils.getLogger();

	private static final float MIN_PITCH = 0.5F;
	private static final float MAX_PITCH = 2.0F;
	private static final float MIN_VOLUME = 0.0F;
	private static final float MAX_VOLUME = 1.0F;
	private static final int FADE_TICKS = 20;
	private static final long MIN_TIME_INTERVAL_TO_RELOAD_SOUNDS = 1000L;

	public static final String FOR_THE_DEBUG = "FOR THE DEBUG!";
	public static final String OPENAL_SOFT_ON = "OpenAL Soft on ";
	public static final int OPENAL_SOFT_ON_LENGTH = "OpenAL Soft on ".length();

	private static final Set<Identifier> UNKNOWN_SOUNDS = Sets.newHashSet();

	private final SoundManager soundManager;
	private final GameOptions options;
	private boolean started;
	private final SoundEngine soundEngine = new SoundEngine();
	private final SoundListener listener = soundEngine.getListener();
	private final SoundLoader soundLoader;
	private final SoundExecutor taskQueue = new SoundExecutor();
	private final Channel channel = new Channel(soundEngine, taskQueue);
	private int ticks;
	private long lastSoundDeviceCheckTime;
	private final AtomicReference<SoundSystem.DeviceChangeStatus> deviceChangeStatus =
		new AtomicReference<>(SoundSystem.DeviceChangeStatus.NO_CHANGE);
	private final Map<SoundInstance, Channel.SourceManager> sources = Maps.newHashMap();
	private final Multimap<SoundCategory, SoundInstance> sounds = HashMultimap.create();
	private final Object2FloatMap<SoundCategory> volumes =
		Util.make(new Object2FloatOpenHashMap<>(), map -> map.defaultReturnValue(1.0F));
	private final List<TickableSoundInstance> tickingSounds = Lists.newArrayList();
	private final Map<SoundInstance, Integer> soundStartTicks = Maps.newHashMap();
	private final Map<SoundInstance, Integer> soundEndTicks = Maps.newHashMap();
	private final List<SoundInstanceListener> listeners = Lists.newArrayList();
	private final List<TickableSoundInstance> soundsToPlayNextTick = Lists.newArrayList();
	private final List<Sound> preloadedSounds = Lists.newArrayList();

	public SoundSystem(SoundManager soundManager, GameOptions options, ResourceFactory resourceFactory) {
		this.soundManager = soundManager;
		this.options = options;
		soundLoader = new SoundLoader(resourceFactory);
	}

	public void reloadSounds() {
		UNKNOWN_SOUNDS.clear();

		for (SoundEvent soundEvent : Registries.SOUND_EVENT) {
			if (soundEvent == SoundEvents.INTENTIONALLY_EMPTY) {
				continue;
			}

			Identifier id = soundEvent.id();
			if (soundManager.get(id) == null) {
				LOGGER.warn("Missing sound for event: {}", Registries.SOUND_EVENT.getId(soundEvent));
				UNKNOWN_SOUNDS.add(id);
			}
		}

		stop();
		start();
	}

	private synchronized void start() {
		if (started) {
			return;
		}

		try {
			String device = options.getSoundDevice().getValue();
			soundEngine.init("".equals(device) ? null : device, options.getDirectionalAudio().getValue());
			listener.init();
			soundLoader.loadStatic(preloadedSounds).thenRun(preloadedSounds::clear);
			started = true;
			LOGGER.info(MARKER, "Sound engine started");
		} catch (RuntimeException ex) {
			LOGGER.error(MARKER, "Error starting SoundSystem. Turning off sounds & music", ex);
		}
	}

	public void refreshSoundVolumes(SoundCategory category) {
		if (started == false) {
			return;
		}

		sources.forEach((sound, manager) -> {
			if (category == sound.getCategory() || category == SoundCategory.MASTER) {
				float volume = getAdjustedVolume(sound);
				manager.run(source -> source.setVolume(volume));
			}
		});
	}

	public void stop() {
		if (started == false) {
			return;
		}

		stopAll();
		soundLoader.close();
		soundEngine.close();
		started = false;
	}

	public void stopAbruptly() {
		if (started) {
			soundEngine.close();
		}
	}

	public void stop(SoundInstance sound) {
		if (started == false) {
			return;
		}

		Channel.SourceManager manager = sources.get(sound);
		if (manager != null) {
			manager.run(Source::stop);
		}
	}

	public void setVolume(SoundCategory category, float volume) {
		volumes.put(category, MathHelper.clamp(volume, MIN_VOLUME, MAX_VOLUME));
		refreshSoundVolumes(category);
	}

	public void stopAll() {
		if (started == false) {
			return;
		}

		taskQueue.stop();
		sources.clear();
		channel.close();
		soundStartTicks.clear();
		tickingSounds.clear();
		sounds.clear();
		soundEndTicks.clear();
		soundsToPlayNextTick.clear();
		volumes.clear();
		taskQueue.restart();
	}

	public void registerListener(SoundInstanceListener listener) {
		listeners.add(listener);
	}

	public void unregisterListener(SoundInstanceListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Проверяет, нужно ли перезагрузить звуковую систему из-за смены аудиоустройства.
	 * Опрос устройства выполняется не чаще раза в {@link #MIN_TIME_INTERVAL_TO_RELOAD_SOUNDS} мс
	 * в фоновом потоке IO, чтобы не блокировать игровой поток.
	 */
	private boolean shouldReloadSounds() {
		if (soundEngine.isDeviceUnavailable()) {
			LOGGER.info("Audio device was lost!");
			return true;
		}

		long now = Util.getMeasuringTimeMs();
		boolean timeElapsed = now - lastSoundDeviceCheckTime >= MIN_TIME_INTERVAL_TO_RELOAD_SOUNDS;
		if (timeElapsed) {
			lastSoundDeviceCheckTime = now;
			if (deviceChangeStatus.compareAndSet(
				SoundSystem.DeviceChangeStatus.NO_CHANGE,
				SoundSystem.DeviceChangeStatus.ONGOING
			)) {
				String preferredDevice = options.getSoundDevice().getValue();
				Util.getIoWorkerExecutor().execute(() -> {
					if ("".equals(preferredDevice)) {
						if (soundEngine.updateDeviceSpecifier()) {
							LOGGER.info("System default audio device has changed!");
							deviceChangeStatus.compareAndSet(
								SoundSystem.DeviceChangeStatus.ONGOING,
								SoundSystem.DeviceChangeStatus.CHANGE_DETECTED
							);
						}
					} else if (soundEngine.getCurrentDeviceName().equals(preferredDevice) == false
						&& soundEngine.getSoundDevices().contains(preferredDevice)
					) {
						LOGGER.info("Preferred audio device has become available!");
						deviceChangeStatus.compareAndSet(
							SoundSystem.DeviceChangeStatus.ONGOING,
							SoundSystem.DeviceChangeStatus.CHANGE_DETECTED
						);
					}

					deviceChangeStatus.compareAndSet(
						SoundSystem.DeviceChangeStatus.ONGOING,
						SoundSystem.DeviceChangeStatus.NO_CHANGE
					);
				});
			}
		}

		return deviceChangeStatus.compareAndSet(
			SoundSystem.DeviceChangeStatus.CHANGE_DETECTED,
			SoundSystem.DeviceChangeStatus.NO_CHANGE
		);
	}

	public void tick(boolean paused) {
		if (shouldReloadSounds()) {
			reloadSounds();
		}

		if (paused) {
			tickPaused();
		} else {
			tick();
		}

		channel.tick();
	}

	private void tick() {
		ticks++;
		soundsToPlayNextTick.stream().filter(SoundInstance::canPlay).forEach(this::play);
		soundsToPlayNextTick.clear();

		for (TickableSoundInstance tickable : tickingSounds) {
			if (tickable.canPlay() == false) {
				stop(tickable);
			}

			tickable.tick();
			if (tickable.isDone()) {
				stop(tickable);
			} else {
				float volume = getAdjustedVolume(tickable);
				float pitch = getAdjustedPitch(tickable);
				Vec3d pos = new Vec3d(tickable.getX(), tickable.getY(), tickable.getZ());
				Channel.SourceManager manager = sources.get(tickable);
				if (manager != null) {
					manager.run(source -> {
						source.setVolume(volume);
						source.setPitch(pitch);
						source.setPosition(pos);
					});
				}
			}
		}

		Iterator<Entry<SoundInstance, Channel.SourceManager>> iterator = sources.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<SoundInstance, Channel.SourceManager> entry = iterator.next();
			Channel.SourceManager manager = entry.getValue();
			SoundInstance sound = entry.getKey();

			if (manager.isStopped() == false) {
				continue;
			}

			int endTick = soundEndTicks.get(sound);
			if (endTick > ticks) {
				continue;
			}

			if (shouldDelayRepeat(sound)) {
				soundStartTicks.put(sound, ticks + sound.getRepeatDelay());
			}

			iterator.remove();
			LOGGER.debug(MARKER, "Removed channel {} because it's not playing anymore", manager);
			soundEndTicks.remove(sound);

			try {
				sounds.remove(sound.getCategory(), sound);
			} catch (RuntimeException ignored) {
			}

			if (sound instanceof TickableSoundInstance) {
				tickingSounds.remove(sound);
			}
		}

		Iterator<Entry<SoundInstance, Integer>> startIterator = soundStartTicks.entrySet().iterator();
		while (startIterator.hasNext()) {
			Entry<SoundInstance, Integer> entry = startIterator.next();
			if (ticks < entry.getValue()) {
				continue;
			}

			SoundInstance sound = entry.getKey();
			if (sound instanceof TickableSoundInstance tickable) {
				tickable.tick();
			}

			play(sound);
			startIterator.remove();
		}
	}

	private void tickPaused() {
		Iterator<Entry<SoundInstance, Channel.SourceManager>> iterator = sources.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<SoundInstance, Channel.SourceManager> entry = iterator.next();
			Channel.SourceManager manager = entry.getValue();
			SoundInstance sound = entry.getKey();

			if (sound.getCategory() == SoundCategory.MUSIC && manager.isStopped()) {
				iterator.remove();
				LOGGER.debug(MARKER, "Removed channel {} because it's not playing anymore", manager);
				soundEndTicks.remove(sound);
				sounds.remove(sound.getCategory(), sound);
			}
		}
	}

	private static boolean hasRepeatDelay(SoundInstance sound) {
		return sound.getRepeatDelay() > 0;
	}

	private static boolean shouldDelayRepeat(SoundInstance sound) {
		return sound.isRepeatable() && hasRepeatDelay(sound);
	}

	private static boolean shouldRepeatInstantly(SoundInstance sound) {
		return sound.isRepeatable() && hasRepeatDelay(sound) == false;
	}

	public boolean isPlaying(SoundInstance sound) {
		if (started == false) {
			return false;
		}

		return soundEndTicks.containsKey(sound) && soundEndTicks.get(sound) <= ticks
			? true
			: sources.containsKey(sound);
	}

	/**
	 * Запускает воспроизведение звука. Загружает аудиобуфер асинхронно,
	 * настраивает параметры источника (громкость, высоту, затухание, позицию)
	 * и уведомляет зарегистрированных слушателей.
	 *
	 * @param sound экземпляр звука для воспроизведения
	 * @return результат попытки воспроизведения
	 */
	public SoundSystem.PlayResult play(SoundInstance sound) {
		if (started == false) {
			return SoundSystem.PlayResult.NOT_STARTED;
		}

		if (sound.canPlay() == false) {
			return SoundSystem.PlayResult.NOT_STARTED;
		}

		WeightedSoundSet soundSet = sound.getSoundSet(soundManager);
		Identifier id = sound.getId();

		if (soundSet == null) {
			if (UNKNOWN_SOUNDS.add(id)) {
				LOGGER.warn(MARKER, "Unable to play unknown soundEvent: {}", id);
			}

			if (SharedConstants.SUBTITLES == false) {
				return SoundSystem.PlayResult.NOT_STARTED;
			}

			soundSet = new WeightedSoundSet(id, FOR_THE_DEBUG);
		}

		Sound soundData = sound.getSound();
		if (soundData == SoundManager.INTENTIONALLY_EMPTY_SOUND) {
			return SoundSystem.PlayResult.NOT_STARTED;
		}

		if (soundData == SoundManager.MISSING_SOUND) {
			if (UNKNOWN_SOUNDS.add(id)) {
				LOGGER.warn(MARKER, "Unable to play empty soundEvent: {}", id);
			}

			return SoundSystem.PlayResult.NOT_STARTED;
		}

		float rawVolume = sound.getVolume();
		float attenuation = Math.max(rawVolume, 1.0F) * soundData.getAttenuation();
		SoundCategory category = sound.getCategory();
		float adjustedVolume = getAdjustedVolume(rawVolume, category);
		float pitch = getAdjustedPitch(sound);
		SoundInstance.AttenuationType attenuationType = sound.getAttenuationType();
		boolean relative = sound.isRelative();

		if (listeners.isEmpty() == false) {
			float range = relative == false && attenuationType != SoundInstance.AttenuationType.NONE
				? attenuation
				: Float.POSITIVE_INFINITY;

			for (SoundInstanceListener listener : listeners) {
				listener.onSoundPlayed(sound, soundSet, range);
			}
		}

		boolean silent = false;
		if (adjustedVolume == 0.0F) {
			if (sound.shouldAlwaysPlay() == false && category != SoundCategory.MUSIC) {
				LOGGER.debug(MARKER, "Skipped playing sound {}, volume was zero.", soundData.getIdentifier());
				return SoundSystem.PlayResult.NOT_STARTED;
			}

			silent = true;
		}

		Vec3d pos = new Vec3d(sound.getX(), sound.getY(), sound.getZ());
		boolean instantRepeat = shouldRepeatInstantly(sound);
		boolean streamed = soundData.isStreamed();

		CompletableFuture<Channel.SourceManager> future = channel.createSource(
			streamed ? SoundEngine.RunMode.STREAMING : SoundEngine.RunMode.STATIC
		);
		Channel.SourceManager manager = future.join();

		if (manager == null) {
			if (SharedConstants.isDevelopment) {
				LOGGER.warn("Failed to create new sound handle");
			}

			return SoundSystem.PlayResult.NOT_STARTED;
		}

		LOGGER.debug(MARKER, "Playing sound {} for event {}", soundData.getIdentifier(), id);
		soundEndTicks.put(sound, ticks + FADE_TICKS);
		sources.put(sound, manager);
		sounds.put(category, sound);

		final float finalPitch = pitch;
		final float finalVolume = adjustedVolume;
		manager.run(source -> {
			source.setPitch(finalPitch);
			source.setVolume(finalVolume);
			if (attenuationType == SoundInstance.AttenuationType.LINEAR) {
				source.setAttenuation(attenuation);
			} else {
				source.disableAttenuation();
			}

			source.setLooping(instantRepeat && streamed == false);
			source.setPosition(pos);
			source.setRelative(relative);
		});

		if (streamed == false) {
			soundLoader.loadStatic(soundData.getLocation())
				.thenAccept(buffer -> manager.run(source -> {
					source.setBuffer(buffer);
					source.play();
				}));
		} else {
			soundLoader.loadStreamed(soundData.getLocation(), instantRepeat)
				.thenAccept(stream -> manager.run(source -> {
					source.setStream(stream);
					source.play();
				}));
		}

		if (sound instanceof TickableSoundInstance tickable) {
			tickingSounds.add(tickable);
		}

		return silent ? SoundSystem.PlayResult.STARTED_SILENTLY : SoundSystem.PlayResult.STARTED;
	}

	public void playNextTick(TickableSoundInstance sound) {
		soundsToPlayNextTick.add(sound);
	}

	public void addPreloadedSound(Sound sound) {
		preloadedSounds.add(sound);
	}

	private float getAdjustedPitch(SoundInstance sound) {
		return MathHelper.clamp(sound.getPitch(), MIN_PITCH, MAX_PITCH);
	}

	private float getAdjustedVolume(SoundInstance sound) {
		return getAdjustedVolume(sound.getVolume(), sound.getCategory());
	}

	private float getAdjustedVolume(float volume, SoundCategory category) {
		return MathHelper.clamp(volume, MIN_VOLUME, MAX_VOLUME)
			* MathHelper.clamp(options.getSoundVolume(category), MIN_VOLUME, MAX_VOLUME)
			* volumes.getFloat(category);
	}

	public void pauseAllExcept(SoundCategory... categories) {
		if (started == false) {
			return;
		}

		List<SoundCategory> excluded = List.of(categories);
		for (Entry<SoundInstance, Channel.SourceManager> entry : sources.entrySet()) {
			if (excluded.contains(entry.getKey().getCategory()) == false) {
				entry.getValue().run(Source::pause);
			}
		}
	}

	public void resumeAll() {
		if (started) {
			channel.execute(stream -> stream.forEach(Source::resume));
		}
	}

	public void play(SoundInstance sound, int delay) {
		soundStartTicks.put(sound, ticks + delay);
	}

	public void updateListenerPosition(Camera camera) {
		if (started == false || camera.isReady() == false) {
			return;
		}

		SoundListenerTransform transform = new SoundListenerTransform(
			camera.getCameraPos(),
			new Vec3d(camera.getHorizontalPlane()),
			new Vec3d(camera.getVerticalPlane())
		);
		taskQueue.execute(() -> listener.setTransform(transform));
	}

	public void stopSounds(@Nullable Identifier id, @Nullable SoundCategory category) {
		if (category != null) {
			for (SoundInstance sound : sounds.get(category)) {
				if (id == null || sound.getId().equals(id)) {
					stop(sound);
				}
			}
		} else if (id == null) {
			stopAll();
		} else {
			for (SoundInstance sound : sources.keySet()) {
				if (sound.getId().equals(id)) {
					stop(sound);
				}
			}
		}
	}

	public String getDebugString() {
		return soundEngine.getDebugString();
	}

	public List<String> getSoundDevices() {
		return soundEngine.getSoundDevices();
	}

	public SoundListenerTransform getListenerTransform() {
		return listener.getTransform();
	}

	@Environment(EnvType.CLIENT)
	enum DeviceChangeStatus {
		ONGOING,
		CHANGE_DETECTED,
		NO_CHANGE
	}

	@Environment(EnvType.CLIENT)
	public enum PlayResult {
		STARTED,
		STARTED_SILENTLY,
		NOT_STARTED
	}
}

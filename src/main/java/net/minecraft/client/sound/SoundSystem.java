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

@Environment(EnvType.CLIENT)
/**
 * {@code SoundSystem}.
 */
public class SoundSystem {

	private static final Marker MARKER = MarkerFactory.getMarker("SOUNDS");
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final float MIN_PITCH = 0.5F;
	private static final float MAX_PITCH = 2.0F;
	private static final float MIN_VOLUME = 0.0F;
	private static final float MAX_VOLUME = 1.0F;
	private static final int FADE_TICKS = 20;
	private static final Set<Identifier> UNKNOWN_SOUNDS = Sets.newHashSet();
	private static final long MIN_TIME_INTERVAL_TO_RELOAD_SOUNDS = 1000L;
	public static final String FOR_THE_DEBUG = "FOR THE DEBUG!";
	public static final String OPENAL_SOFT_ON = "OpenAL Soft on ";
	public static final int OPENAL_SOFT_ON_LENGTH = "OpenAL Soft on ".length();
	private final SoundManager soundManager;
	private final GameOptions options;
	private boolean started;
	private final SoundEngine soundEngine = new SoundEngine();
	private final SoundListener listener = this.soundEngine.getListener();
	private final SoundLoader soundLoader;
	private final SoundExecutor taskQueue = new SoundExecutor();
	private final Channel channel = new Channel(this.soundEngine, this.taskQueue);
	private int ticks;
	private long lastSoundDeviceCheckTime;
	private final AtomicReference<SoundSystem.DeviceChangeStatus>
			deviceChangeStatus =
			new AtomicReference<>(SoundSystem.DeviceChangeStatus.NO_CHANGE);
	private final Map<SoundInstance, Channel.SourceManager> sources = Maps.newHashMap();
	private final Multimap<SoundCategory, SoundInstance> sounds = HashMultimap.create();
	private final Object2FloatMap<SoundCategory>
			volumes =
			Util.make(new Object2FloatOpenHashMap(), map -> map.defaultReturnValue(1.0F));
	private final List<TickableSoundInstance> tickingSounds = Lists.newArrayList();
	private final Map<SoundInstance, Integer> soundStartTicks = Maps.newHashMap();
	private final Map<SoundInstance, Integer> soundEndTicks = Maps.newHashMap();
	private final List<SoundInstanceListener> listeners = Lists.newArrayList();
	private final List<TickableSoundInstance> soundsToPlayNextTick = Lists.newArrayList();
	private final List<Sound> preloadedSounds = Lists.newArrayList();

	public SoundSystem(SoundManager soundManager, GameOptions options, ResourceFactory resourceFactory) {
		this.soundManager = soundManager;
		this.options = options;
		this.soundLoader = new SoundLoader(resourceFactory);
	}

	/**
	 * Reload sounds.
	 */
	public void reloadSounds() {
		UNKNOWN_SOUNDS.clear();

		for (SoundEvent soundEvent : Registries.SOUND_EVENT) {
			if (soundEvent != SoundEvents.INTENTIONALLY_EMPTY) {
				Identifier identifier = soundEvent.id();
				if (this.soundManager.get(identifier) == null) {
					LOGGER.warn("Missing sound for event: {}", Registries.SOUND_EVENT.getId(soundEvent));
					UNKNOWN_SOUNDS.add(identifier);
				}
			}
		}

		this.stop();
		this.start();
	}

	private synchronized void start() {
		if (!this.started) {
			try {
				String string = this.options.getSoundDevice().getValue();
				this.soundEngine.init("".equals(string) ? null : string, this.options.getDirectionalAudio().getValue());
				this.listener.init();
				this.soundLoader.loadStatic(this.preloadedSounds).thenRun(this.preloadedSounds::clear);
				this.started = true;
				LOGGER.info(MARKER, "Sound engine started");
			}
			catch (RuntimeException var2) {
				LOGGER.error(MARKER, "Error starting SoundSystem. Turning off sounds & music", var2);
			}
		}
	}

	/**
	 * Refresh sound volumes.
	 *
	 * @param category category
	 */
	public void refreshSoundVolumes(SoundCategory category) {
		if (this.started) {
			this.sources.forEach((sound, manager) -> {
				if (category == sound.getCategory() || category == SoundCategory.MASTER) {
					float f = this.getAdjustedVolume(sound);
					manager.run(source -> source.setVolume(f));
				}
			});
		}
	}

	/**
	 * Stop.
	 */
	public void stop() {
		if (this.started) {
			this.stopAll();
			this.soundLoader.close();
			this.soundEngine.close();
			this.started = false;
		}
	}

	/**
	 * Останавливает abruptly.
	 */
	public void stopAbruptly() {
		if (this.started) {
			this.soundEngine.close();
		}
	}

	/**
	 * Stop.
	 *
	 * @param sound sound
	 */
	public void stop(SoundInstance sound) {
		if (this.started) {
			Channel.SourceManager sourceManager = this.sources.get(sound);
			if (sourceManager != null) {
				sourceManager.run(Source::stop);
			}
		}
	}

	public void setVolume(SoundCategory category, float volume) {
		this.volumes.put(category, MathHelper.clamp(volume, 0.0F, 1.0F));
		this.refreshSoundVolumes(category);
	}

	/**
	 * Останавливает all.
	 */
	public void stopAll() {
		if (this.started) {
			this.taskQueue.stop();
			this.sources.clear();
			this.channel.close();
			this.soundStartTicks.clear();
			this.tickingSounds.clear();
			this.sounds.clear();
			this.soundEndTicks.clear();
			this.soundsToPlayNextTick.clear();
			this.volumes.clear();
			this.taskQueue.restart();
		}
	}

	/**
	 * Регистрирует listener.
	 *
	 * @param listener listener
	 */
	public void registerListener(SoundInstanceListener listener) {
		this.listeners.add(listener);
	}

	/**
	 * Отменяет регистрацию listener.
	 *
	 * @param listener listener
	 */
	public void unregisterListener(SoundInstanceListener listener) {
		this.listeners.remove(listener);
	}

	private boolean shouldReloadSounds() {
		if (this.soundEngine.isDeviceUnavailable()) {
			LOGGER.info("Audio device was lost!");
			return true;
		}
		else {
			long l = Util.getMeasuringTimeMs();
			boolean bl = l - this.lastSoundDeviceCheckTime >= 1000L;
			if (bl) {
				this.lastSoundDeviceCheckTime = l;
				if (this.deviceChangeStatus.compareAndSet(
						SoundSystem.DeviceChangeStatus.NO_CHANGE,
						SoundSystem.DeviceChangeStatus.ONGOING
				)) {
					String string = this.options.getSoundDevice().getValue();
					Util.getIoWorkerExecutor().execute(() -> {
						if ("".equals(string)) {
							if (this.soundEngine.updateDeviceSpecifier()) {
								LOGGER.info("System default audio device has changed!");
								this.deviceChangeStatus.compareAndSet(
										SoundSystem.DeviceChangeStatus.ONGOING,
										SoundSystem.DeviceChangeStatus.CHANGE_DETECTED
								);
							}
						}
						else if (!this.soundEngine.getCurrentDeviceName().equals(string) && this.soundEngine
								.getSoundDevices()
								.contains(string)) {
							LOGGER.info("Preferred audio device has become available!");
							this.deviceChangeStatus.compareAndSet(
									SoundSystem.DeviceChangeStatus.ONGOING,
									SoundSystem.DeviceChangeStatus.CHANGE_DETECTED
							);
						}

						this.deviceChangeStatus.compareAndSet(
								SoundSystem.DeviceChangeStatus.ONGOING,
								SoundSystem.DeviceChangeStatus.NO_CHANGE
						);
					});
				}
			}

			return this.deviceChangeStatus.compareAndSet(
					SoundSystem.DeviceChangeStatus.CHANGE_DETECTED,
					SoundSystem.DeviceChangeStatus.NO_CHANGE
			);
		}
	}

	/**
	 * Tick.
	 *
	 * @param paused paused
	 */
	public void tick(boolean paused) {
		if (this.shouldReloadSounds()) {
			this.reloadSounds();
		}

		if (!paused) {
			this.tick();
		}
		else {
			this.tickPaused();
		}

		this.channel.tick();
	}

	private void tick() {
		this.ticks++;
		this.soundsToPlayNextTick.stream().filter(SoundInstance::canPlay).forEach(this::play);
		this.soundsToPlayNextTick.clear();

		for (TickableSoundInstance tickableSoundInstance : this.tickingSounds) {
			if (!tickableSoundInstance.canPlay()) {
				this.stop(tickableSoundInstance);
			}

			tickableSoundInstance.tick();
			if (tickableSoundInstance.isDone()) {
				this.stop(tickableSoundInstance);
			}
			else {
				float f = this.getAdjustedVolume(tickableSoundInstance);
				float g = this.getAdjustedPitch(tickableSoundInstance);
				Vec3d
						vec3d =
						new Vec3d(
								tickableSoundInstance.getX(),
								tickableSoundInstance.getY(),
								tickableSoundInstance.getZ()
						);
				Channel.SourceManager sourceManager = this.sources.get(tickableSoundInstance);
				if (sourceManager != null) {
					sourceManager.run(source -> {
						source.setVolume(f);
						source.setPitch(g);
						source.setPosition(vec3d);
					});
				}
			}
		}

		Iterator<Entry<SoundInstance, Channel.SourceManager>> iterator = this.sources.entrySet().iterator();

		while (iterator.hasNext()) {
			Entry<SoundInstance, Channel.SourceManager> entry = iterator.next();
			Channel.SourceManager sourceManager2 = entry.getValue();
			SoundInstance soundInstance = entry.getKey();
			if (sourceManager2.isStopped()) {
				int i = this.soundEndTicks.get(soundInstance);
				if (i <= this.ticks) {
					if (shouldDelayRepeat(soundInstance)) {
						this.soundStartTicks.put(soundInstance, this.ticks + soundInstance.getRepeatDelay());
					}

					iterator.remove();
					LOGGER.debug(MARKER, "Removed channel {} because it's not playing anymore", sourceManager2);
					this.soundEndTicks.remove(soundInstance);

					try {
						this.sounds.remove(soundInstance.getCategory(), soundInstance);
					}
					catch (RuntimeException var7) {
					}

					if (soundInstance instanceof TickableSoundInstance) {
						this.tickingSounds.remove(soundInstance);
					}
				}
			}
		}

		Iterator<Entry<SoundInstance, Integer>> iterator2 = this.soundStartTicks.entrySet().iterator();

		while (iterator2.hasNext()) {
			Entry<SoundInstance, Integer> entry2 = iterator2.next();
			if (this.ticks >= entry2.getValue()) {
				SoundInstance soundInstance = entry2.getKey();
				if (soundInstance instanceof TickableSoundInstance) {
					((TickableSoundInstance) soundInstance).tick();
				}

				this.play(soundInstance);
				iterator2.remove();
			}
		}
	}

	private void tickPaused() {
		Iterator<Entry<SoundInstance, Channel.SourceManager>> iterator = this.sources.entrySet().iterator();

		while (iterator.hasNext()) {
			Entry<SoundInstance, Channel.SourceManager> entry = iterator.next();
			Channel.SourceManager sourceManager = entry.getValue();
			SoundInstance soundInstance = entry.getKey();
			if (soundInstance.getCategory() == SoundCategory.MUSIC && sourceManager.isStopped()) {
				iterator.remove();
				LOGGER.debug(MARKER, "Removed channel {} because it's not playing anymore", sourceManager);
				this.soundEndTicks.remove(soundInstance);
				this.sounds.remove(soundInstance.getCategory(), soundInstance);
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
		return sound.isRepeatable() && !hasRepeatDelay(sound);
	}

	public boolean isPlaying(SoundInstance sound) {
		if (!this.started) {
			return false;
		}
		else {
			return this.soundEndTicks.containsKey(sound) && this.soundEndTicks.get(sound) <= this.ticks ? true
			                                                                                            : this.sources.containsKey(
					                                                                                            sound);
		}
	}

	public SoundSystem.PlayResult play(SoundInstance sound) {
		if (!this.started) {
			return SoundSystem.PlayResult.NOT_STARTED;
		}
		else if (!sound.canPlay()) {
			return SoundSystem.PlayResult.NOT_STARTED;
		}
		else {
			WeightedSoundSet weightedSoundSet = sound.getSoundSet(this.soundManager);
			Identifier identifier = sound.getId();
			if (weightedSoundSet == null) {
				if (UNKNOWN_SOUNDS.add(identifier)) {
					LOGGER.warn(MARKER, "Unable to play unknown soundEvent: {}", identifier);
				}

				if (!SharedConstants.SUBTITLES) {
					return SoundSystem.PlayResult.NOT_STARTED;
				}

				weightedSoundSet = new WeightedSoundSet(identifier, "FOR THE DEBUG!");
			}

			Sound sound2 = sound.getSound();
			if (sound2 == SoundManager.INTENTIONALLY_EMPTY_SOUND) {
				return SoundSystem.PlayResult.NOT_STARTED;
			}
			else if (sound2 == SoundManager.MISSING_SOUND) {
				if (UNKNOWN_SOUNDS.add(identifier)) {
					LOGGER.warn(MARKER, "Unable to play empty soundEvent: {}", identifier);
				}

				return SoundSystem.PlayResult.NOT_STARTED;
			}
			else {
				float f = sound.getVolume();
				float g = Math.max(f, 1.0F) * sound2.getAttenuation();
				SoundCategory soundCategory = sound.getCategory();
				float h = this.getAdjustedVolume(f, soundCategory);
				float i = this.getAdjustedPitch(sound);
				SoundInstance.AttenuationType attenuationType = sound.getAttenuationType();
				boolean bl = sound.isRelative();
				if (!this.listeners.isEmpty()) {
					float
							j =
							!bl && attenuationType != SoundInstance.AttenuationType.NONE ? g : Float.POSITIVE_INFINITY;

					for (SoundInstanceListener soundInstanceListener : this.listeners) {
						soundInstanceListener.onSoundPlayed(sound, weightedSoundSet, j);
					}
				}

				boolean bl2 = false;
				if (h == 0.0F) {
					if (!sound.shouldAlwaysPlay() && soundCategory != SoundCategory.MUSIC) {
						LOGGER.debug(MARKER, "Skipped playing sound {}, volume was zero.", sound2.getIdentifier());
						return SoundSystem.PlayResult.NOT_STARTED;
					}

					bl2 = true;
				}

				Vec3d vec3d = new Vec3d(sound.getX(), sound.getY(), sound.getZ());
				boolean bl3 = shouldRepeatInstantly(sound);
				boolean bl4 = sound2.isStreamed();
				CompletableFuture<Channel.SourceManager> completableFuture = this.channel
						.createSource(sound2.isStreamed() ? SoundEngine.RunMode.STREAMING : SoundEngine.RunMode.STATIC);
				Channel.SourceManager sourceManager = completableFuture.join();
				if (sourceManager == null) {
					if (SharedConstants.isDevelopment) {
						LOGGER.warn("Failed to create new sound handle");
					}

					return SoundSystem.PlayResult.NOT_STARTED;
				}
				else {
					LOGGER.debug(MARKER, "Playing sound {} for event {}", sound2.getIdentifier(), identifier);
					this.soundEndTicks.put(sound, this.ticks + 20);
					this.sources.put(sound, sourceManager);
					this.sounds.put(soundCategory, sound);
					sourceManager.run(source -> {
						source.setPitch(i);
						source.setVolume(h);
						if (attenuationType == SoundInstance.AttenuationType.LINEAR) {
							source.setAttenuation(g);
						}
						else {
							source.disableAttenuation();
						}

						source.setLooping(bl3 && !bl4);
						source.setPosition(vec3d);
						source.setRelative(bl);
					});
					if (!bl4) {
						this.soundLoader
								.loadStatic(sound2.getLocation())
								.thenAccept(soundx -> sourceManager.run(source -> {
									source.setBuffer(soundx);
									source.play();
								}));
					}
					else {
						this.soundLoader
								.loadStreamed(sound2.getLocation(), bl3)
								.thenAccept(stream -> sourceManager.run(source -> {
									source.setStream(stream);
									source.play();
								}));
					}

					if (sound instanceof TickableSoundInstance) {
						this.tickingSounds.add((TickableSoundInstance) sound);
					}

					return bl2 ? SoundSystem.PlayResult.STARTED_SILENTLY : SoundSystem.PlayResult.STARTED;
				}
			}
		}
	}

	/**
	 * Play next tick.
	 *
	 * @param sound sound
	 */
	public void playNextTick(TickableSoundInstance sound) {
		this.soundsToPlayNextTick.add(sound);
	}

	/**
	 * Добавляет preloaded sound.
	 *
	 * @param sound sound
	 */
	public void addPreloadedSound(Sound sound) {
		this.preloadedSounds.add(sound);
	}

	private float getAdjustedPitch(SoundInstance sound) {
		return MathHelper.clamp(sound.getPitch(), 0.5F, 2.0F);
	}

	private float getAdjustedVolume(SoundInstance sound) {
		return this.getAdjustedVolume(sound.getVolume(), sound.getCategory());
	}

	private float getAdjustedVolume(float volume, SoundCategory category) {
		return MathHelper.clamp(volume, 0.0F, 1.0F) * MathHelper.clamp(
				this.options.getSoundVolume(category),
				0.0F,
				1.0F
		) * this.volumes.getFloat(category);
	}

	/**
	 * Pause all except.
	 *
	 * @param categories categories
	 */
	public void pauseAllExcept(SoundCategory... categories) {
		if (this.started) {
			for (Entry<SoundInstance, Channel.SourceManager> entry : this.sources.entrySet()) {
				if (!List.of(categories).contains(entry.getKey().getCategory())) {
					entry.getValue().run(Source::pause);
				}
			}
		}
	}

	/**
	 * Resume all.
	 */
	public void resumeAll() {
		if (this.started) {
			this.channel.execute(sources -> sources.forEach(Source::resume));
		}
	}

	/**
	 * Play.
	 *
	 * @param sound sound
	 * @param delay delay
	 */
	public void play(SoundInstance sound, int delay) {
		this.soundStartTicks.put(sound, this.ticks + delay);
	}

	/**
	 * Обновляет listener position.
	 *
	 * @param camera camera
	 */
	public void updateListenerPosition(Camera camera) {
		if (this.started && camera.isReady()) {
			SoundListenerTransform soundListenerTransform = new SoundListenerTransform(
					camera.getCameraPos(), new Vec3d(camera.getHorizontalPlane()), new Vec3d(camera.getVerticalPlane())
			);
			this.taskQueue.execute(() -> this.listener.setTransform(soundListenerTransform));
		}
	}

	/**
	 * Останавливает sounds.
	 *
	 * @param id id
	 * @param category category
	 */
	public void stopSounds(@Nullable Identifier id, @Nullable SoundCategory category) {
		if (category != null) {
			for (SoundInstance soundInstance : this.sounds.get(category)) {
				if (id == null || soundInstance.getId().equals(id)) {
					this.stop(soundInstance);
				}
			}
		}
		else if (id == null) {
			this.stopAll();
		}
		else {
			for (SoundInstance soundInstancex : this.sources.keySet()) {
				if (soundInstancex.getId().equals(id)) {
					this.stop(soundInstancex);
				}
			}
		}
	}

	public String getDebugString() {
		return this.soundEngine.getDebugString();
	}

	public List<String> getSoundDevices() {
		return this.soundEngine.getSoundDevices();
	}

	public SoundListenerTransform getListenerTransform() {
		return this.listener.getTransform();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code DeviceChangeStatus}.
	 */
	static enum DeviceChangeStatus {
		ONGOING,
		CHANGE_DETECTED,
		NO_CHANGE;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code PlayResult}.
	 */
	public static enum PlayResult {
		STARTED,
		STARTED_SILENTLY,
		NOT_STARTED;
	}
}

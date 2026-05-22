package net.minecraft.client.sound;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.Camera;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Texts;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.floatprovider.ConstantFloatProvider;
import net.minecraft.util.math.floatprovider.MultipliedFloatSupplier;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.ScopedProfiler;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Менеджер звуков: загружает и регистрирует звуковые события из {@code sounds.json}
 * всех ресурспаков, управляет {@link SoundSystem} и предоставляет API воспроизведения.
 * Является {@link SinglePreparationResourceReloader} — перезагружается при смене ресурспаков.
 */
@Environment(EnvType.CLIENT)
public class SoundManager extends SinglePreparationResourceReloader<SoundManager.SoundList> {

	public static final Identifier EMPTY_ID = Identifier.ofVanilla("empty");
	public static final Sound MISSING_SOUND = new Sound(
		EMPTY_ID,
		ConstantFloatProvider.create(1.0F),
		ConstantFloatProvider.create(1.0F),
		1,
		Sound.RegistrationType.FILE,
		false,
		false,
		16
	);
	public static final Identifier INTENTIONALLY_EMPTY_ID = Identifier.ofVanilla("intentionally_empty");
	public static final WeightedSoundSet INTENTIONALLY_EMPTY_SOUND_SET =
		new WeightedSoundSet(INTENTIONALLY_EMPTY_ID, null);
	public static final Sound INTENTIONALLY_EMPTY_SOUND = new Sound(
		INTENTIONALLY_EMPTY_ID,
		ConstantFloatProvider.create(1.0F),
		ConstantFloatProvider.create(1.0F),
		1,
		Sound.RegistrationType.FILE,
		false,
		false,
		16
	);

	static final Logger LOGGER = LogUtils.getLogger();

	private static final String SOUNDS_JSON = "sounds.json";
	private static final Gson GSON = new GsonBuilder()
		.registerTypeAdapter(SoundEntry.class, new SoundEntryDeserializer())
		.create();
	private static final TypeToken<Map<String, SoundEntry>> TYPE = new TypeToken<Map<String, SoundEntry>>() {};

	private final Map<Identifier, WeightedSoundSet> sounds = Maps.newHashMap();
	private final SoundSystem soundSystem;
	private final Map<Identifier, Resource> soundResources = new HashMap<>();

	public SoundManager(GameOptions gameOptions) {
		soundSystem = new SoundSystem(this, gameOptions, ResourceFactory.fromMap(soundResources));
	}

	@Override
	protected SoundManager.SoundList prepare(ResourceManager resourceManager, Profiler profiler) {
		SoundManager.SoundList soundList = new SoundManager.SoundList();

		try (ScopedProfiler ignored = profiler.scoped("list")) {
			soundList.findSounds(resourceManager);
		}

		for (String namespace : resourceManager.getAllNamespaces()) {
			try (ScopedProfiler ignored = profiler.scoped(namespace)) {
				for (Resource resource : resourceManager.getAllResources(Identifier.of(namespace, SOUNDS_JSON))) {
					profiler.push(resource.getPackId());

					try (Reader reader = resource.getReader()) {
						profiler.push("parse");
						Map<String, SoundEntry> map = JsonHelper.deserialize(GSON, reader, TYPE);
						profiler.swap("register");

						for (Entry<String, SoundEntry> entry : map.entrySet()) {
							soundList.register(Identifier.of(namespace, entry.getKey()), entry.getValue());
						}

						profiler.pop();
					} catch (RuntimeException ex) {
						LOGGER.warn(
							"Invalid {} in resourcepack: '{}'",
							SOUNDS_JSON,
							resource.getPackId(),
							ex
						);
					}

					profiler.pop();
				}
			} catch (IOException ignored) {
			}
		}

		return soundList;
	}

	@Override
	protected void apply(SoundManager.SoundList soundList, ResourceManager resourceManager, Profiler profiler) {
		soundList.reload(sounds, soundResources, soundSystem);

		if (SharedConstants.isDevelopment) {
			for (Identifier id : sounds.keySet()) {
				WeightedSoundSet soundSet = sounds.get(id);
				if (Texts.hasTranslation(soundSet.getSubtitle()) == false
					&& Registries.SOUND_EVENT.containsId(id)
				) {
					LOGGER.error("Missing subtitle {} for sound event: {}", soundSet.getSubtitle(), id);
				}
			}
		}

		if (LOGGER.isDebugEnabled()) {
			for (Identifier id : sounds.keySet()) {
				if (Registries.SOUND_EVENT.containsId(id) == false) {
					LOGGER.debug("Not having sound event for: {}", id);
				}
			}
		}

		soundSystem.reloadSounds();
	}

	public List<String> getSoundDevices() {
		return soundSystem.getSoundDevices();
	}

	public SoundListenerTransform getListenerTransform() {
		return soundSystem.getListenerTransform();
	}

	static boolean isSoundResourcePresent(Sound sound, Identifier id, ResourceFactory resourceFactory) {
		Identifier location = sound.getLocation();
		if (resourceFactory.getResource(location).isEmpty()) {
			LOGGER.warn("File {} does not exist, cannot add it to event {}", location, id);
			return false;
		}

		return true;
	}

	public @Nullable WeightedSoundSet get(Identifier id) {
		return sounds.get(id);
	}

	public Collection<Identifier> getKeys() {
		return sounds.keySet();
	}

	public void playNextTick(TickableSoundInstance sound) {
		soundSystem.playNextTick(sound);
	}

	public SoundSystem.PlayResult play(SoundInstance sound) {
		return soundSystem.play(sound);
	}

	public void play(SoundInstance sound, int delay) {
		soundSystem.play(sound, delay);
	}

	public void updateListenerPosition(Camera camera) {
		soundSystem.updateListenerPosition(camera);
	}

	public void pauseAllExcept(SoundCategory... categories) {
		soundSystem.pauseAllExcept(categories);
	}

	public void stopAll() {
		soundSystem.stopAll();
	}

	public void close() {
		soundSystem.stop();
	}

	public void stopAbruptly() {
		soundSystem.stopAbruptly();
	}

	public void tick(boolean paused) {
		soundSystem.tick(paused);
	}

	public void resumeAll() {
		soundSystem.resumeAll();
	}

	public void refreshSoundVolumes(SoundCategory category) {
		soundSystem.refreshSoundVolumes(category);
	}

	public void stop(SoundInstance sound) {
		soundSystem.stop(sound);
	}

	public void setVolume(SoundCategory category, float volume) {
		soundSystem.setVolume(category, volume);
	}

	public boolean isPlaying(SoundInstance sound) {
		return soundSystem.isPlaying(sound);
	}

	public void registerListener(SoundInstanceListener listener) {
		soundSystem.registerListener(listener);
	}

	public void unregisterListener(SoundInstanceListener listener) {
		soundSystem.unregisterListener(listener);
	}

	public void stopSounds(@Nullable Identifier id, @Nullable SoundCategory soundCategory) {
		soundSystem.stopSounds(id, soundCategory);
	}

	public String getDebugString() {
		return soundSystem.getDebugString();
	}

	public void reloadSounds() {
		soundSystem.reloadSounds();
	}

	/**
	 * Промежуточный контейнер, собираемый в фоновом потоке во время {@code prepare}.
	 * Хранит найденные звуковые файлы и зарегистрированные наборы звуков
	 * до их применения в основном потоке через {@link #reload}.
	 */
	@Environment(EnvType.CLIENT)
	protected static class SoundList {

		final Map<Identifier, WeightedSoundSet> loadedSounds = Maps.newHashMap();
		private Map<Identifier, Resource> foundSounds = Map.of();

		void findSounds(ResourceManager resourceManager) {
			foundSounds = Sound.FINDER.findResources(resourceManager);
		}

		void register(Identifier id, SoundEntry entry) {
			WeightedSoundSet soundSet = loadedSounds.get(id);
			boolean isNew = soundSet == null;

			if (isNew || entry.canReplace()) {
				if (isNew == false) {
					SoundManager.LOGGER.debug("Replaced sound event location {}", id);
				}

				soundSet = new WeightedSoundSet(id, entry.getSubtitle());
				loadedSounds.put(id, soundSet);
			}

			ResourceFactory resourceFactory = ResourceFactory.fromMap(foundSounds);

			for (final Sound sound : entry.getSounds()) {
				final Identifier soundId = sound.getIdentifier();
				SoundContainer<Sound> container;

				switch (sound.getRegistrationType()) {
					case FILE:
						if (SoundManager.isSoundResourcePresent(sound, id, resourceFactory) == false) {
							continue;
						}
						container = sound;
						break;

					case SOUND_EVENT:
						container = new SoundContainer<Sound>() {
							@Override
							public int getWeight() {
								WeightedSoundSet set = SoundList.this.loadedSounds.get(soundId);
								return set == null ? 0 : set.getWeight();
							}

							@Override
							public Sound getSound(Random random) {
								WeightedSoundSet set = SoundList.this.loadedSounds.get(soundId);
								if (set == null) {
									return SoundManager.MISSING_SOUND;
								}

								Sound resolved = set.getSound(random);
								return new Sound(
									resolved.getIdentifier(),
									new MultipliedFloatSupplier(resolved.getVolume(), sound.getVolume()),
									new MultipliedFloatSupplier(resolved.getPitch(), sound.getPitch()),
									sound.getWeight(),
									Sound.RegistrationType.FILE,
									resolved.isStreamed() || sound.isStreamed(),
									resolved.isPreloaded(),
									resolved.getAttenuation()
								);
							}

							@Override
							public void preload(SoundSystem soundSystem) {
								WeightedSoundSet set = SoundList.this.loadedSounds.get(soundId);
								if (set != null) {
									set.preload(soundSystem);
								}
							}
						};
						break;

					default:
						throw new IllegalStateException(
							"Unknown SoundEventRegistration type: " + sound.getRegistrationType()
						);
				}

				soundSet.add(container);
			}
		}

		public void reload(
			Map<Identifier, WeightedSoundSet> sounds,
			Map<Identifier, Resource> soundResources,
			SoundSystem system
		) {
			sounds.clear();
			soundResources.clear();
			soundResources.putAll(foundSounds);

			for (Entry<Identifier, WeightedSoundSet> entry : loadedSounds.entrySet()) {
				sounds.put(entry.getKey(), entry.getValue());
				entry.getValue().preload(system);
			}
		}
	}
}

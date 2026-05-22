package net.minecraft.client.sound;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;

import java.nio.IntBuffer;
import java.util.*;

/**
 * Низкоуровневая обёртка над OpenAL. Управляет устройством воспроизведения,
 * контекстом и пулами источников звука (статических и потоковых).
 * Инициализирует HRTF (объёмный звук) при наличии расширения {@code ALC_SOFT_HRTF}.
 */
@Environment(EnvType.CLIENT)
public class SoundEngine {

	static final Logger LOGGER = LogUtils.getLogger();

	private static final int INITIAL_CHANNEL_INDEX = 0;
	private static final int FADE_TICKS = 30;

	// ALC_SOFT_HRTF константы (расширение OpenAL Soft)
	private static final int ALC_HRTF_SOFT = 0x1992;
	private static final int ALC_NUM_HRTF_SPECIFIERS_SOFT = 0x1994;
	private static final int ALC_HRTF_SPECIFIER_SOFT = 0x1996;
	private static final int ALC_OUTPUT_LIMITER_SOFT = 0x199A;

	// ALC_EXT_disconnect константа
	private static final int ALC_CONNECTED = 0x313;

	// AL_EXT_source_distance_model
	private static final int AL_SOURCE_DISTANCE_MODEL = 0x200;

	private long devicePointer;
	private long contextPointer;
	private boolean disconnectExtensionPresent;
	private @Nullable String deviceSpecifier;

	private static final SoundEngine.SourceSet EMPTY_SOURCE_SET = new SoundEngine.SourceSet() {
		@Override
		public @Nullable Source createSource() {
			return null;
		}

		@Override
		public boolean release(Source source) {
			return false;
		}

		@Override
		public void close() {
		}

		@Override
		public int getMaxSourceCount() {
			return 0;
		}

		@Override
		public int getSourceCount() {
			return 0;
		}
	};

	private SoundEngine.SourceSet streamingSources = EMPTY_SOURCE_SET;
	private SoundEngine.SourceSet staticSources = EMPTY_SOURCE_SET;
	private final SoundListener listener = new SoundListener();

	public SoundEngine() {
		deviceSpecifier = findAvailableDeviceSpecifier();
	}

	/**
	 * Инициализирует OpenAL: открывает устройство, создаёт контекст,
	 * настраивает HRTF и пулы источников звука.
	 * Требует расширений {@code AL_EXT_source_distance_model} и {@code AL_EXT_LINEAR_DISTANCE}.
	 *
	 * @param deviceSpecifier имя аудиоустройства или {@code null} для устройства по умолчанию
	 * @param directionalAudio включить HRTF (объёмный звук)
	 * @throws IllegalStateException если инициализация OpenAL не удалась
	 */
	public void init(@Nullable String deviceSpecifier, boolean directionalAudio) {
		devicePointer = openDeviceOrFallback(deviceSpecifier);
		disconnectExtensionPresent = false;

		ALCCapabilities alcCapabilities = ALC.createCapabilities(devicePointer);
		if (AlUtil.checkAlcErrors(devicePointer, "Get capabilities")) {
			throw new IllegalStateException("Failed to get OpenAL capabilities");
		}

		if (alcCapabilities.OpenALC11 == false) {
			throw new IllegalStateException("OpenAL 1.1 not supported");
		}

		MemoryStack memoryStack = MemoryStack.stackPush();
		try {
			IntBuffer attributes = createAttributes(memoryStack, alcCapabilities.ALC_SOFT_HRTF && directionalAudio);
			contextPointer = ALC10.alcCreateContext(devicePointer, attributes);
		} catch (Throwable ex) {
			if (memoryStack != null) {
				try {
					memoryStack.close();
				} catch (Throwable suppressed) {
					ex.addSuppressed(suppressed);
				}
			}
			throw ex;
		}

		if (memoryStack != null) {
			memoryStack.close();
		}

		if (AlUtil.checkAlcErrors(devicePointer, "Create context")) {
			throw new IllegalStateException("Unable to create OpenAL context");
		}

		ALC10.alcMakeContextCurrent(contextPointer);

		int monoSources = getMonoSourceCount();
		int streamingCount = MathHelper.clamp((int) MathHelper.sqrt(monoSources), 2, 8);
		int staticCount = MathHelper.clamp(monoSources - streamingCount, 8, 255);
		streamingSources = new SoundEngine.SourceSetImpl(staticCount);
		staticSources = new SoundEngine.SourceSetImpl(streamingCount);

		ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
		AlUtil.checkErrors("Initialization");

		if (alCapabilities.AL_EXT_source_distance_model == false) {
			throw new IllegalStateException("AL_EXT_source_distance_model is not supported");
		}

		AL10.alEnable(AL_SOURCE_DISTANCE_MODEL);

		if (alCapabilities.AL_EXT_LINEAR_DISTANCE == false) {
			throw new IllegalStateException("AL_EXT_LINEAR_DISTANCE is not supported");
		}

		AlUtil.checkErrors("Enable per-source distance models");
		LOGGER.info("OpenAL initialized on device {}", getCurrentDeviceName());
		disconnectExtensionPresent = ALC10.alcIsExtensionPresent(devicePointer, "ALC_EXT_disconnect");
	}

	private IntBuffer createAttributes(MemoryStack stack, boolean directionalAudio) {
		// Буфер: до 5 пар ключ-значение + завершающий 0
		IntBuffer buffer = stack.callocInt(11);

		int hrtfCount = ALC10.alcGetInteger(devicePointer, ALC_NUM_HRTF_SPECIFIERS_SOFT);
		if (hrtfCount > 0) {
			buffer.put(ALC_HRTF_SOFT).put(directionalAudio ? 1 : 0);
			buffer.put(ALC_HRTF_SPECIFIER_SOFT).put(0);
		}

		buffer.put(ALC_OUTPUT_LIMITER_SOFT).put(1);
		return buffer.put(0).flip();
	}

	private int getMonoSourceCount() {
		MemoryStack memoryStack = MemoryStack.stackPush();
		try {
			int attributesSize = ALC10.alcGetInteger(devicePointer, ALC10.ALC_ATTRIBUTES_SIZE);
			if (AlUtil.checkAlcErrors(devicePointer, "Get attributes size")) {
				throw new IllegalStateException("Failed to get OpenAL attributes");
			}

			IntBuffer attributes = memoryStack.mallocInt(attributesSize);
			ALC10.alcGetIntegerv(devicePointer, ALC10.ALC_ALL_ATTRIBUTES, attributes);
			if (AlUtil.checkAlcErrors(devicePointer, "Get attributes")) {
				throw new IllegalStateException("Failed to get OpenAL attributes");
			}

			int index = 0;
			while (index < attributesSize) {
				int key = attributes.get(index++);
				if (key == 0) {
					break;
				}

				int value = attributes.get(index++);
				if (key == 0x1010) {
					if (memoryStack != null) {
						memoryStack.close();
					}
					return value;
				}
			}
		} catch (Throwable ex) {
			if (memoryStack != null) {
				try {
					memoryStack.close();
				} catch (Throwable suppressed) {
					ex.addSuppressed(suppressed);
				}
			}
			throw ex;
		}

		if (memoryStack != null) {
			memoryStack.close();
		}

		return FADE_TICKS;
	}

	/**
	 * Возвращает спецификатор предпочтительного аудиоустройства системы.
	 * Требует расширения {@code ALC_ENUMERATE_ALL_EXT}.
	 *
	 * @return имя устройства или {@code null}, если расширение недоступно
	 */
	public static @Nullable String findAvailableDeviceSpecifier() {
		if (ALC10.alcIsExtensionPresent(0L, "ALC_ENUMERATE_ALL_EXT") == false) {
			return null;
		}

		ALUtil.getStringList(0L, 0x1013);
		return ALC10.alcGetString(0L, 0x1012);
	}

	public String getCurrentDeviceName() {
		String name = ALC10.alcGetString(devicePointer, 0x1013);
		if (name == null) {
			name = ALC10.alcGetString(devicePointer, ALC10.ALC_DEVICE_SPECIFIER);
		}

		return name != null ? name : "Unknown";
	}

	public synchronized boolean updateDeviceSpecifier() {
		String current = findAvailableDeviceSpecifier();
		if (Objects.equals(deviceSpecifier, current)) {
			return false;
		}

		deviceSpecifier = current;
		return true;
	}

	private static long openDeviceOrFallback(@Nullable String deviceSpecifier) {
		OptionalLong handle = OptionalLong.empty();

		if (deviceSpecifier != null) {
			handle = openDevice(deviceSpecifier);
		}

		if (handle.isEmpty()) {
			handle = openDevice(findAvailableDeviceSpecifier());
		}

		if (handle.isEmpty()) {
			handle = openDevice(null);
		}

		if (handle.isEmpty()) {
			throw new IllegalStateException("Failed to open OpenAL device");
		}

		return handle.getAsLong();
	}

	private static OptionalLong openDevice(@Nullable String deviceSpecifier) {
		long handle = ALC10.alcOpenDevice(deviceSpecifier);
		return handle != 0L && AlUtil.checkAlcErrors(handle, "Open device") == false
			? OptionalLong.of(handle)
			: OptionalLong.empty();
	}

	public void close() {
		streamingSources.close();
		staticSources.close();
		ALC10.alcDestroyContext(contextPointer);
		if (devicePointer != 0L) {
			ALC10.alcCloseDevice(devicePointer);
		}
	}

	public SoundListener getListener() {
		return listener;
	}

	public @Nullable Source createSource(SoundEngine.RunMode mode) {
		return (mode == SoundEngine.RunMode.STREAMING ? staticSources : streamingSources).createSource();
	}

	public void release(Source source) {
		if (streamingSources.release(source) == false && staticSources.release(source) == false) {
			throw new IllegalStateException("Tried to release unknown channel");
		}
	}

	public String getDebugString() {
		return String.format(
			Locale.ROOT,
			"Sounds: %d/%d + %d/%d",
			streamingSources.getSourceCount(),
			streamingSources.getMaxSourceCount(),
			staticSources.getSourceCount(),
			staticSources.getMaxSourceCount()
		);
	}

	public List<String> getSoundDevices() {
		List<String> devices = ALUtil.getStringList(0L, 0x1013);
		return devices == null ? Collections.emptyList() : devices;
	}

	public boolean isDeviceUnavailable() {
		return disconnectExtensionPresent && ALC11.alcGetInteger(devicePointer, ALC_CONNECTED) == 0;
	}

	/**
	 * Режим воспроизведения источника: статический (буферизованный) или потоковый.
	 */
	@Environment(EnvType.CLIENT)
	public enum RunMode {
		STATIC,
		STREAMING
	}

	@Environment(EnvType.CLIENT)
	interface SourceSet {

		@Nullable Source createSource();

		boolean release(Source source);

		void close();

		int getMaxSourceCount();

		int getSourceCount();
	}

	@Environment(EnvType.CLIENT)
	static class SourceSetImpl implements SoundEngine.SourceSet {

		private final int maxSourceCount;
		private final Set<Source> sources = Sets.newIdentityHashSet();

		public SourceSetImpl(int maxSourceCount) {
			this.maxSourceCount = maxSourceCount;
		}

		@Override
		public @Nullable Source createSource() {
			if (sources.size() >= maxSourceCount) {
				if (SharedConstants.isDevelopment) {
					SoundEngine.LOGGER.warn("Maximum sound pool size {} reached", maxSourceCount);
				}

				return null;
			}

			Source source = Source.create();
			if (source != null) {
				sources.add(source);
			}

			return source;
		}

		@Override
		public boolean release(Source source) {
			if (sources.remove(source) == false) {
				return false;
			}

			source.close();
			return true;
		}

		@Override
		public void close() {
			sources.forEach(Source::close);
			sources.clear();
		}

		@Override
		public int getMaxSourceCount() {
			return maxSourceCount;
		}

		@Override
		public int getSourceCount() {
			return sources.size();
		}
	}
}

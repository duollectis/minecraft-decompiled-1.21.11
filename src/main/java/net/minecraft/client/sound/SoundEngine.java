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

@Environment(EnvType.CLIENT)
/**
 * {@code SoundEngine}.
 */
public class SoundEngine {

	static final Logger LOGGER = LogUtils.getLogger();
	private static final int INITIAL_CHANNEL_INDEX = 0;
	private static final int FADE_TICKS = 30;
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
		this.deviceSpecifier = findAvailableDeviceSpecifier();
	}

	/**
	 * Init.
	 *
	 * @param deviceSpecifier device specifier
	 * @param directionalAudio directional audio
	 */
	public void init(@Nullable String deviceSpecifier, boolean directionalAudio) {
		this.devicePointer = openDeviceOrFallback(deviceSpecifier);
		this.disconnectExtensionPresent = false;
		ALCCapabilities aLCCapabilities = ALC.createCapabilities(this.devicePointer);
		if (AlUtil.checkAlcErrors(this.devicePointer, "Get capabilities")) {
			throw new IllegalStateException("Failed to get OpenAL capabilities");
		}
		else if (!aLCCapabilities.OpenALC11) {
			throw new IllegalStateException("OpenAL 1.1 not supported");
		}
		else {
			MemoryStack memoryStack = MemoryStack.stackPush();

			try {
				IntBuffer
						intBuffer =
						this.createAttributes(memoryStack, aLCCapabilities.ALC_SOFT_HRTF && directionalAudio);
				this.contextPointer = ALC10.alcCreateContext(this.devicePointer, intBuffer);
			}
			catch (Throwable var9) {
				if (memoryStack != null) {
					try {
						memoryStack.close();
					}
					catch (Throwable var8) {
						var9.addSuppressed(var8);
					}
				}

				throw var9;
			}

			if (memoryStack != null) {
				memoryStack.close();
			}

			if (AlUtil.checkAlcErrors(this.devicePointer, "Create context")) {
				throw new IllegalStateException("Unable to create OpenAL context");
			}
			else {
				ALC10.alcMakeContextCurrent(this.contextPointer);
				int i = this.getMonoSourceCount();
				int j = MathHelper.clamp((int) MathHelper.sqrt(i), 2, 8);
				int k = MathHelper.clamp(i - j, 8, 255);
				this.streamingSources = new SoundEngine.SourceSetImpl(k);
				this.staticSources = new SoundEngine.SourceSetImpl(j);
				ALCapabilities aLCapabilities = AL.createCapabilities(aLCCapabilities);
				AlUtil.checkErrors("Initialization");
				if (!aLCapabilities.AL_EXT_source_distance_model) {
					throw new IllegalStateException("AL_EXT_source_distance_model is not supported");
				}
				else {
					AL10.alEnable(512);
					if (!aLCapabilities.AL_EXT_LINEAR_DISTANCE) {
						throw new IllegalStateException("AL_EXT_LINEAR_DISTANCE is not supported");
					}
					else {
						AlUtil.checkErrors("Enable per-source distance models");
						LOGGER.info("OpenAL initialized on device {}", this.getCurrentDeviceName());
						this.disconnectExtensionPresent =
								ALC10.alcIsExtensionPresent(this.devicePointer, "ALC_EXT_disconnect");
					}
				}
			}
		}
	}

	private IntBuffer createAttributes(MemoryStack stack, boolean directionalAudio) {
		int i = 5;
		IntBuffer intBuffer = stack.callocInt(11);
		int j = ALC10.alcGetInteger(this.devicePointer, 6548);
		if (j > 0) {
			intBuffer.put(6546).put(directionalAudio ? 1 : 0);
			intBuffer.put(6550).put(0);
		}

		intBuffer.put(6554).put(1);
		return intBuffer.put(0).flip();
	}

	private int getMonoSourceCount() {
		MemoryStack memoryStack = MemoryStack.stackPush();

		int var7;
		label58:
		{
			try {
				int i = ALC10.alcGetInteger(this.devicePointer, 4098);
				if (AlUtil.checkAlcErrors(this.devicePointer, "Get attributes size")) {
					throw new IllegalStateException("Failed to get OpenAL attributes");
				}

				IntBuffer intBuffer = memoryStack.mallocInt(i);
				ALC10.alcGetIntegerv(this.devicePointer, 4099, intBuffer);
				if (AlUtil.checkAlcErrors(this.devicePointer, "Get attributes")) {
					throw new IllegalStateException("Failed to get OpenAL attributes");
				}

				int j = 0;

				while (j < i) {
					int k = intBuffer.get(j++);
					if (k == 0) {
						break;
					}

					int l = intBuffer.get(j++);
					if (k == 4112) {
						var7 = l;
						break label58;
					}
				}
			}
			catch (Throwable var9) {
				if (memoryStack != null) {
					try {
						memoryStack.close();
					}
					catch (Throwable var8) {
						var9.addSuppressed(var8);
					}
				}

				throw var9;
			}

			if (memoryStack != null) {
				memoryStack.close();
			}

			return 30;
		}

		if (memoryStack != null) {
			memoryStack.close();
		}

		return var7;
	}

	/**
	 * Ищет available device specifier.
	 *
	 * @return @Nullable String — available device specifier
	 */
	public static @Nullable String findAvailableDeviceSpecifier() {
		if (!ALC10.alcIsExtensionPresent(0L, "ALC_ENUMERATE_ALL_EXT")) {
			return null;
		}
		else {
			ALUtil.getStringList(0L, 4115);
			return ALC10.alcGetString(0L, 4114);
		}
	}

	public String getCurrentDeviceName() {
		String string = ALC10.alcGetString(this.devicePointer, 4115);
		if (string == null) {
			string = ALC10.alcGetString(this.devicePointer, 4101);
		}

		if (string == null) {
			string = "Unknown";
		}

		return string;
	}

	/**
	 * Обновляет device specifier.
	 *
	 * @return boolean — результат операции
	 */
	public synchronized boolean updateDeviceSpecifier() {
		String string = findAvailableDeviceSpecifier();
		if (Objects.equals(this.deviceSpecifier, string)) {
			return false;
		}
		else {
			this.deviceSpecifier = string;
			return true;
		}
	}

	private static long openDeviceOrFallback(@Nullable String deviceSpecifier) {
		OptionalLong optionalLong = OptionalLong.empty();
		if (deviceSpecifier != null) {
			optionalLong = openDevice(deviceSpecifier);
		}

		if (optionalLong.isEmpty()) {
			optionalLong = openDevice(findAvailableDeviceSpecifier());
		}

		if (optionalLong.isEmpty()) {
			optionalLong = openDevice(null);
		}

		if (optionalLong.isEmpty()) {
			throw new IllegalStateException("Failed to open OpenAL device");
		}
		else {
			return optionalLong.getAsLong();
		}
	}

	private static OptionalLong openDevice(@Nullable String deviceSpecifier) {
		long l = ALC10.alcOpenDevice(deviceSpecifier);
		return l != 0L && !AlUtil.checkAlcErrors(l, "Open device") ? OptionalLong.of(l) : OptionalLong.empty();
	}

	/**
	 * Close.
	 */
	public void close() {
		this.streamingSources.close();
		this.staticSources.close();
		ALC10.alcDestroyContext(this.contextPointer);
		if (this.devicePointer != 0L) {
			ALC10.alcCloseDevice(this.devicePointer);
		}
	}

	public SoundListener getListener() {
		return this.listener;
	}

	/**
	 * Создаёт source.
	 *
	 * @param mode mode
	 *
	 * @return @Nullable Source — результат операции
	 */
	public @Nullable Source createSource(SoundEngine.RunMode mode) {
		return (mode == SoundEngine.RunMode.STREAMING ? this.staticSources : this.streamingSources).createSource();
	}

	/**
	 * Release.
	 *
	 * @param source source
	 */
	public void release(Source source) {
		if (!this.streamingSources.release(source) && !this.staticSources.release(source)) {
			throw new IllegalStateException("Tried to release unknown channel");
		}
	}

	public String getDebugString() {
		return String.format(
				Locale.ROOT,
				"Sounds: %d/%d + %d/%d",
				this.streamingSources.getSourceCount(),
				this.streamingSources.getMaxSourceCount(),
				this.staticSources.getSourceCount(),
				this.staticSources.getMaxSourceCount()
		);
	}

	public List<String> getSoundDevices() {
		List<String> list = ALUtil.getStringList(0L, 4115);
		return list == null ? Collections.emptyList() : list;
	}

	public boolean isDeviceUnavailable() {
		return this.disconnectExtensionPresent && ALC11.alcGetInteger(this.devicePointer, 787) == 0;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code RunMode}.
	 */
	public static enum RunMode {
		STATIC,
		STREAMING;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SourceSet}.
	 */
	interface SourceSet {

		@Nullable Source createSource();

		boolean release(Source source);

		void close();

		int getMaxSourceCount();

		int getSourceCount();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SourceSetImpl}.
	 */
	static class SourceSetImpl implements SoundEngine.SourceSet {

		private final int maxSourceCount;
		private final Set<Source> sources = Sets.newIdentityHashSet();

		public SourceSetImpl(int maxSourceCount) {
			this.maxSourceCount = maxSourceCount;
		}

		@Override
		public @Nullable Source createSource() {
			if (this.sources.size() >= this.maxSourceCount) {
				if (SharedConstants.isDevelopment) {
					SoundEngine.LOGGER.warn("Maximum sound pool size {} reached", this.maxSourceCount);
				}

				return null;
			}
			else {
				Source source = Source.create();
				if (source != null) {
					this.sources.add(source);
				}

				return source;
			}
		}

		@Override
		public boolean release(Source source) {
			if (!this.sources.remove(source)) {
				return false;
			}
			else {
				source.close();
				return true;
			}
		}

		@Override
		public void close() {
			this.sources.forEach(Source::close);
			this.sources.clear();
		}

		@Override
		public int getMaxSourceCount() {
			return this.maxSourceCount;
		}

		@Override
		public int getSourceCount() {
			return this.sources.size();
		}
	}
}

package net.minecraft.client.sound;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL10;
import org.slf4j.Logger;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Обёртка над OpenAL-источником звука. Управляет буферами, потоковым воспроизведением
 * и параметрами источника (позиция, громкость, высота, затухание).
 * Потокобезопасность обеспечивается через {@link AtomicBoolean} для флага воспроизведения.
 */
@Environment(EnvType.CLIENT)
public class Source {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final int BUFFER_COUNT = 4;
	public static final int STREAM_BUFFER_COUNT = 1;

	// Размер буфера по умолчанию для потокового воспроизведения (в байтах)
	private static final int DEFAULT_BUFFER_SIZE = 16384;

	private final int pointer;
	private final AtomicBoolean playing = new AtomicBoolean(true);
	private int bufferSize = DEFAULT_BUFFER_SIZE;
	private @Nullable AudioStream stream;

	static @Nullable Source create() {
		int[] ids = new int[1];
		AL10.alGenSources(ids);
		return AlUtil.checkErrors("Allocate new source") ? null : new Source(ids[0]);
	}

	private Source(int pointer) {
		this.pointer = pointer;
	}

	public void close() {
		if (playing.compareAndSet(true, false) == false) {
			return;
		}

		AL10.alSourceStop(pointer);
		AlUtil.checkErrors("Stop");

		if (stream != null) {
			try {
				stream.close();
			} catch (IOException ex) {
				LOGGER.error("Failed to close audio stream", ex);
			}

			removeProcessedBuffers();
			stream = null;
		}

		AL10.alDeleteSources(new int[]{pointer});
		AlUtil.checkErrors("Cleanup");
	}

	public void play() {
		AL10.alSourcePlay(pointer);
	}

	private int getSourceState() {
		return playing.get() == false
			? AL10.AL_STOPPED
			: AL10.alGetSourcei(pointer, AL10.AL_SOURCE_STATE);
	}

	public void pause() {
		if (getSourceState() == AL10.AL_PLAYING) {
			AL10.alSourcePause(pointer);
		}
	}

	public void resume() {
		if (getSourceState() == AL10.AL_PAUSED) {
			AL10.alSourcePlay(pointer);
		}
	}

	public void stop() {
		if (playing.get()) {
			AL10.alSourceStop(pointer);
			AlUtil.checkErrors("Stop");
		}
	}

	public boolean isPlaying() {
		return getSourceState() == AL10.AL_PLAYING;
	}

	public boolean isStopped() {
		return getSourceState() == AL10.AL_STOPPED;
	}

	public void setPosition(Vec3d pos) {
		AL10.alSourcefv(pointer, AL10.AL_POSITION, new float[]{(float) pos.x, (float) pos.y, (float) pos.z});
	}

	public void setPitch(float pitch) {
		AL10.alSourcef(pointer, AL10.AL_PITCH, pitch);
	}

	public void setLooping(boolean looping) {
		AL10.alSourcei(pointer, AL10.AL_LOOPING, looping ? 1 : 0);
	}

	public void setVolume(float volume) {
		AL10.alSourcef(pointer, AL10.AL_GAIN, volume);
	}

	/**
	 * Отключает затухание звука по расстоянию, устанавливая модель {@code AL_NONE}.
	 */
	public void disableAttenuation() {
		AL10.alSourcei(pointer, AL10.AL_DISTANCE_MODEL, AL10.AL_NONE);
	}

	/**
	 * Настраивает линейное затухание звука по расстоянию.
	 * Использует модель {@code AL_LINEAR_DISTANCE} с заданным максимальным расстоянием.
	 *
	 * @param attenuation максимальное расстояние затухания
	 */
	public void setAttenuation(float attenuation) {
		AL10.alSourcei(pointer, 0xD000, 0xD003);
		AL10.alSourcef(pointer, AL10.AL_MAX_DISTANCE, attenuation);
		AL10.alSourcef(pointer, AL10.AL_REFERENCE_DISTANCE, 1.0F);
		AL10.alSourcef(pointer, AL10.AL_ROLLOFF_FACTOR, 0.0F);
	}

	public void setRelative(boolean relative) {
		AL10.alSourcei(pointer, AL10.AL_SOURCE_RELATIVE, relative ? 1 : 0);
	}

	public void setBuffer(StaticSound sound) {
		sound.getStreamBufferPointer().ifPresent(bufPtr -> AL10.alSourcei(pointer, AL10.AL_BUFFER, bufPtr));
	}

	public void setStream(AudioStream stream) {
		this.stream = stream;
		AudioFormat format = stream.getFormat();
		bufferSize = getBufferSize(format, 1);
		read(BUFFER_COUNT);
	}

	private static int getBufferSize(AudioFormat format, int time) {
		return (int) (time * format.getSampleSizeInBits() / 8.0F * format.getChannels() * format.getSampleRate());
	}

	private void read(int count) {
		if (stream == null) {
			return;
		}

		try {
			for (int i = 0; i < count; i++) {
				ByteBuffer data = stream.read(bufferSize);
				if (data != null) {
					new StaticSound(data, stream.getFormat())
						.takeStreamBufferPointer()
						.ifPresent(bufPtr -> AL10.alSourceQueueBuffers(pointer, new int[]{bufPtr}));
				}
			}
		} catch (IOException ex) {
			LOGGER.error("Failed to read from audio stream", ex);
		}
	}

	public void tick() {
		if (stream != null) {
			int processed = removeProcessedBuffers();
			read(processed);
		}
	}

	private int removeProcessedBuffers() {
		int count = AL10.alGetSourcei(pointer, AL10.AL_BUFFERS_PROCESSED);
		if (count > 0) {
			int[] buffers = new int[count];
			AL10.alSourceUnqueueBuffers(pointer, buffers);
			AlUtil.checkErrors("Unqueue buffers");
			AL10.alDeleteBuffers(buffers);
			AlUtil.checkErrors("Remove processed buffers");
		}

		return count;
	}
}

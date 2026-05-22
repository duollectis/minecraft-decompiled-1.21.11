package net.minecraft.client.sound;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.slf4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;

/**
 * Утилитарный класс для работы с OpenAL: проверка ошибок AL и ALC,
 * а также определение формата аудиобуфера по {@link AudioFormat}.
 */
@Environment(EnvType.CLIENT)
public class AlUtil {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final int MONO_8_BIT_FORMAT = 4352;
	private static final int MONO_16_BIT_FORMAT = 4353;
	private static final int STEREO_8_BIT_FORMAT = 4354;
	private static final int STEREO_16_BIT_FORMAT = 4355;

	private static final int MONO_CHANNELS = 1;
	private static final int STEREO_CHANNELS = 2;
	private static final int BITS_8 = 8;
	private static final int BITS_16 = 16;

	private static String getErrorMessage(int errorCode) {
		return switch (errorCode) {
			case AL10.AL_INVALID_NAME -> "Invalid name parameter.";
			case AL10.AL_INVALID_ENUM -> "Invalid enumerated parameter value.";
			case AL10.AL_INVALID_VALUE -> "Invalid parameter parameter value.";
			case AL10.AL_INVALID_OPERATION -> "Invalid operation.";
			case AL10.AL_OUT_OF_MEMORY -> "Unable to allocate memory.";
			default -> "An unrecognized error occurred.";
		};
	}

	static boolean checkErrors(String sectionName) {
		int errorCode = AL10.alGetError();
		if (errorCode == AL10.AL_NO_ERROR) {
			return false;
		}

		LOGGER.error("{}: {}", sectionName, getErrorMessage(errorCode));
		return true;
	}

	private static String getAlcErrorMessage(int errorCode) {
		return switch (errorCode) {
			case ALC10.ALC_INVALID_DEVICE -> "Invalid device.";
			case ALC10.ALC_INVALID_CONTEXT -> "Invalid context.";
			case ALC10.ALC_INVALID_ENUM -> "Illegal enum.";
			case ALC10.ALC_INVALID_VALUE -> "Invalid value.";
			case ALC10.ALC_OUT_OF_MEMORY -> "Unable to allocate memory.";
			default -> "An unrecognized error occurred.";
		};
	}

	static boolean checkAlcErrors(long deviceHandle, String sectionName) {
		int errorCode = ALC10.alcGetError(deviceHandle);
		if (errorCode == ALC10.ALC_NO_ERROR) {
			return false;
		}

		LOGGER.error("{} ({}): {}", sectionName, deviceHandle, getAlcErrorMessage(errorCode));
		return true;
	}

	/**
	 * Определяет идентификатор формата OpenAL-буфера по параметрам {@link AudioFormat}.
	 * Поддерживает PCM (знаковый и беззнаковый), моно и стерео, 8 и 16 бит.
	 *
	 * @param format формат аудиоданных
	 * @return константа формата OpenAL (AL_FORMAT_MONO8, AL_FORMAT_MONO16, и т.д.)
	 * @throws IllegalArgumentException если формат не поддерживается
	 */
	static int getFormatId(AudioFormat format) {
		Encoding encoding = format.getEncoding();
		int channels = format.getChannels();
		int bitsPerSample = format.getSampleSizeInBits();

		boolean isPcm = encoding.equals(Encoding.PCM_UNSIGNED) || encoding.equals(Encoding.PCM_SIGNED);
		if (isPcm == false) {
			throw new IllegalArgumentException("Invalid audio format: " + format);
		}

		if (channels == MONO_CHANNELS) {
			if (bitsPerSample == BITS_8) {
				return MONO_8_BIT_FORMAT;
			}

			if (bitsPerSample == BITS_16) {
				return MONO_16_BIT_FORMAT;
			}
		} else if (channels == STEREO_CHANNELS) {
			if (bitsPerSample == BITS_8) {
				return STEREO_8_BIT_FORMAT;
			}

			if (bitsPerSample == BITS_16) {
				return STEREO_16_BIT_FORMAT;
			}
		}

		throw new IllegalArgumentException("Invalid audio format: " + format);
	}
}

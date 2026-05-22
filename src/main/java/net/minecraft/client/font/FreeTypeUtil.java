package net.minecraft.client.font;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;
import org.slf4j.Logger;

/**
 * Утилитарный класс для работы с нативной библиотекой FreeType через LWJGL.
 * Управляет жизненным циклом глобального экземпляра FreeType и предоставляет
 * вспомогательные методы для проверки ошибок и конвертации координат.
 */
@Environment(EnvType.CLIENT)
public class FreeTypeUtil {

	private static final Logger LOGGER = LogUtils.getLogger();
	// Масштабный коэффициент FreeType: 1 пиксель = 64 единицы
	private static final float FREETYPE_SCALE = 64.0F;
	public static final Object LOCK = new Object();
	private static long freeType = 0L;

	/**
	 * Инициализирует глобальный экземпляр FreeType, если он ещё не создан.
	 * Метод потокобезопасен — синхронизирован по {@link #LOCK}.
	 *
	 * @return нативный указатель на библиотеку FreeType
	 */
	public static long initialize() {
		synchronized (LOCK) {
			if (freeType == 0L) {
				try (MemoryStack stack = MemoryStack.stackPush()) {
					PointerBuffer pointer = stack.mallocPointer(1);
					checkFatalError(FreeType.FT_Init_FreeType(pointer), "Initializing FreeType library");
					freeType = pointer.get();
				}
			}

			return freeType;
		}
	}

	/**
	 * Проверяет код возврата FreeType и выбрасывает исключение при ошибке.
	 *
	 * @param code код возврата FreeType (0 = успех)
	 * @param description описание операции для сообщения об ошибке
	 */
	public static void checkFatalError(int code, String description) {
		if (code != 0) {
			throw new IllegalStateException("FreeType error: " + getErrorMessage(code) + " (" + description + ")");
		}
	}

	/**
	 * Проверяет код возврата FreeType и логирует ошибку при её наличии.
	 *
	 * @param code код возврата FreeType (0 = успех)
	 * @param description описание операции для сообщения об ошибке
	 * @return {@code true} если произошла ошибка
	 */
	public static boolean checkError(int code, String description) {
		if (code != 0) {
			LOGGER.error("FreeType error: {} ({})", getErrorMessage(code), description);
			return true;
		}

		return false;
	}

	private static String getErrorMessage(int code) {
		String message = FreeType.FT_Error_String(code);
		return message != null ? message : "Unrecognized error: 0x" + Integer.toHexString(code);
	}

	/**
	 * Устанавливает координаты вектора FreeType, конвертируя пиксели в единицы FreeType (×64).
	 *
	 * @param vec целевой вектор FreeType
	 * @param x координата X в пикселях
	 * @param y координата Y в пикселях
	 * @return тот же вектор {@code vec} с обновлёнными координатами
	 */
	public static FT_Vector set(FT_Vector vec, float x, float y) {
		long scaledX = Math.round(x * FREETYPE_SCALE);
		long scaledY = Math.round(y * FREETYPE_SCALE);
		return vec.set(scaledX, scaledY);
	}

	public static float getX(FT_Vector vec) {
		return (float) vec.x() / FREETYPE_SCALE;
	}

	public static void release() {
		synchronized (LOCK) {
			if (freeType != 0L) {
				FreeType.FT_Done_Library(freeType);
				freeType = 0L;
			}
		}
	}
}

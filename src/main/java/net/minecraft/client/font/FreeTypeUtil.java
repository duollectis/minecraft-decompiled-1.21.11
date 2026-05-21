package net.minecraft.client.font;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
/**
 * {@code FreeTypeUtil}.
 */
public class FreeTypeUtil {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final Object LOCK = new Object();
	private static long freeType = 0L;

	/**
	 * Инициализирует ialize.
	 *
	 * @return long — результат операции
	 */
	public static long initialize() {
		synchronized (LOCK) {
			if (freeType == 0L) {
				MemoryStack memoryStack = MemoryStack.stackPush();

				try {
					PointerBuffer pointerBuffer = memoryStack.mallocPointer(1);
					checkFatalError(FreeType.FT_Init_FreeType(pointerBuffer), "Initializing FreeType library");
					freeType = pointerBuffer.get();
				}
				catch (Throwable var6) {
					if (memoryStack != null) {
						try {
							memoryStack.close();
						}
						catch (Throwable var5) {
							var6.addSuppressed(var5);
						}
					}

					throw var6;
				}

				if (memoryStack != null) {
					memoryStack.close();
				}
			}

			return freeType;
		}
	}

	/**
	 * Проверяет fatal error.
	 *
	 * @param code code
	 * @param description description
	 */
	public static void checkFatalError(int code, String description) {
		if (code != 0) {
			throw new IllegalStateException("FreeType error: " + getErrorMessage(code) + " (" + description + ")");
		}
	}

	/**
	 * Проверяет error.
	 *
	 * @param code code
	 * @param description description
	 *
	 * @return boolean — результат операции
	 */
	public static boolean checkError(int code, String description) {
		if (code != 0) {
			LOGGER.error("FreeType error: {} ({})", getErrorMessage(code), description);
			return true;
		}
		else {
			return false;
		}
	}

	private static String getErrorMessage(int code) {
		String string = FreeType.FT_Error_String(code);
		return string != null ? string : "Unrecognized error: 0x" + Integer.toHexString(code);
	}

	/**
	 * Set.
	 *
	 * @param vec vec
	 * @param x x
	 * @param y y
	 *
	 * @return FT_Vector — результат операции
	 */
	public static FT_Vector set(FT_Vector vec, float x, float y) {
		long l = Math.round(x * 64.0F);
		long m = Math.round(y * 64.0F);
		return vec.set(l, m);
	}

	public static float getX(FT_Vector vec) {
		return (float) vec.x() / 64.0F;
	}

	/**
	 * Release.
	 */
	public static void release() {
		synchronized (LOCK) {
			if (freeType != 0L) {
				FreeType.FT_Done_Library(freeType);
				freeType = 0L;
			}
		}
	}
}

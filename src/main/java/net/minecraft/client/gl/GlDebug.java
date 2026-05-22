package net.minecraft.client.gl;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.Untracker;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.*;
import org.slf4j.Logger;

import java.util.HexFormat;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Обработчик отладочных сообщений OpenGL через расширения KHR_debug или ARB_debug_output.
 * Хранит кольцевую очередь последних {@code DEBUG_MESSAGE_QUEUE_SIZE} сообщений
 * и дедуплицирует повторяющиеся через счётчик {@code count}.
 */
@Environment(EnvType.CLIENT)
public class GlDebug {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int DEBUG_MESSAGE_QUEUE_SIZE = 10;

	// Уровни серьёзности KHR: HIGH=37190, MEDIUM=37191, LOW=37192, NOTIFICATION=33387
	private static final List<Integer> KHR_VERBOSITY_LEVELS = ImmutableList.of(37190, 37191, 37192, 33387);
	// ARB не поддерживает NOTIFICATION
	private static final List<Integer> ARB_VERBOSITY_LEVELS = ImmutableList.of(37190, 37191, 37192);

	// GL_DEBUG_OUTPUT=37600, GL_DEBUG_OUTPUT_SYNCHRONOUS=33346
	private static final int GL_DEBUG_OUTPUT = 37600;
	private static final int GL_DEBUG_OUTPUT_SYNCHRONOUS = 33346;
	private static final int GL_DONT_CARE = 4352;

	private final Queue<DebugMessage> debugMessages = EvictingQueue.create(DEBUG_MESSAGE_QUEUE_SIZE);
	private volatile @Nullable DebugMessage lastDebugMessage;

	public static String getSource(int opcode) {
		return switch (opcode) {
			case 33350 -> "API";
			case 33351 -> "WINDOW SYSTEM";
			case 33352 -> "SHADER COMPILER";
			case 33353 -> "THIRD PARTY";
			case 33354 -> "APPLICATION";
			case 33355 -> "OTHER";
			default -> unknown(opcode);
		};
	}

	public static String getType(int opcode) {
		return switch (opcode) {
			case 33356 -> "ERROR";
			case 33357 -> "DEPRECATED BEHAVIOR";
			case 33358 -> "UNDEFINED BEHAVIOR";
			case 33359 -> "PORTABILITY";
			case 33360 -> "PERFORMANCE";
			case 33361 -> "OTHER";
			case 33384 -> "MARKER";
			default -> unknown(opcode);
		};
	}

	public static String getSeverity(int opcode) {
		return switch (opcode) {
			case 33387 -> "NOTIFICATION";
			case 37190 -> "HIGH";
			case 37191 -> "MEDIUM";
			case 37192 -> "LOW";
			default -> unknown(opcode);
		};
	}

	/**
	 * Включает отладочный вывод OpenGL с заданным уровнем детализации.
	 * Предпочитает KHR_debug, при отсутствии — ARB_debug_output.
	 * Возвращает {@code null} если verbosity <= 0 или расширения недоступны.
	 */
	public static @Nullable GlDebug enableDebug(int verbosity, boolean sync, Set<String> usedGlCaps) {
		if (verbosity <= 0) {
			return null;
		}

		GLCapabilities capabilities = GL.getCapabilities();

		if (capabilities.GL_KHR_debug && GlBackend.allowGlKhrDebug) {
			GlDebug glDebug = new GlDebug();
			usedGlCaps.add("GL_KHR_debug");
			GL11.glEnable(GL_DEBUG_OUTPUT);

			if (sync) {
				GL11.glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
			}

			for (int level = 0; level < KHR_VERBOSITY_LEVELS.size(); level++) {
				boolean enabled = level < verbosity;
				KHRDebug.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, KHR_VERBOSITY_LEVELS.get(level), (int[]) null, enabled);
			}

			KHRDebug.glDebugMessageCallback(
				GLX.make(GLDebugMessageCallback.create(glDebug::onDebugMessage), Untracker::untrack),
				0L
			);
			return glDebug;
		}

		if (capabilities.GL_ARB_debug_output && GlBackend.allowGlArbDebugOutput) {
			GlDebug glDebug = new GlDebug();
			usedGlCaps.add("GL_ARB_debug_output");

			if (sync) {
				GL11.glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
			}

			for (int level = 0; level < ARB_VERBOSITY_LEVELS.size(); level++) {
				boolean enabled = level < verbosity;
				ARBDebugOutput.glDebugMessageControlARB(GL_DONT_CARE, GL_DONT_CARE, ARB_VERBOSITY_LEVELS.get(level), (int[]) null, enabled);
			}

			ARBDebugOutput.glDebugMessageCallbackARB(
				GLX.make(GLDebugMessageARBCallback.create(glDebug::onDebugMessage), Untracker::untrack),
				0L
			);
			return glDebug;
		}

		return null;
	}

	public List<String> collectDebugMessages() {
		synchronized (debugMessages) {
			List<String> result = Lists.newArrayListWithCapacity(debugMessages.size());

			for (DebugMessage msg : debugMessages) {
				result.add(msg + " x " + msg.count);
			}

			return result;
		}
	}

	private void onDebugMessage(int source, int type, int id, int severity, int length, long message, long userParam) {
		String text = GLDebugMessageCallback.getMessage(length, message);
		DebugMessage debugMessage;

		synchronized (debugMessages) {
			debugMessage = lastDebugMessage;

			if (debugMessage != null && debugMessage.equals(source, type, id, severity, text)) {
				debugMessage.count++;
			}
			else {
				debugMessage = new DebugMessage(source, type, id, severity, text);
				debugMessages.add(debugMessage);
				lastDebugMessage = debugMessage;
			}
		}

		LOGGER.info("OpenGL debug message: {}", debugMessage);
	}

	private static String unknown(int opcode) {
		return "Unknown (0x" + HexFormat.of().withUpperCase().toHexDigits(opcode) + ")";
	}

	@Environment(EnvType.CLIENT)
	static class DebugMessage {

		private final int id;
		private final int source;
		private final int type;
		private final int severity;
		private final String message;
		int count = 1;

		DebugMessage(int source, int type, int id, int severity, String message) {
			this.id = id;
			this.source = source;
			this.type = type;
			this.severity = severity;
			this.message = message;
		}

		boolean equals(int source, int type, int id, int severity, String message) {
			return type == this.type
				&& source == this.source
				&& id == this.id
				&& severity == this.severity
				&& message.equals(this.message);
		}

		@Override
		public String toString() {
			return "id=" + id
				+ ", source=" + GlDebug.getSource(source)
				+ ", type=" + GlDebug.getType(type)
				+ ", severity=" + GlDebug.getSeverity(severity)
				+ ", message='" + message + "'";
		}
	}
}

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

@Environment(EnvType.CLIENT)
/**
 * {@code GlDebug}.
 */
public class GlDebug {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int DEBUG_MESSAGE_QUEUE_SIZE = 10;
	private final Queue<GlDebug.DebugMessage> debugMessages = EvictingQueue.create(10);
	private volatile GlDebug.@Nullable DebugMessage lastDebugMessage;
	private static final List<Integer> KHR_VERBOSITY_LEVELS = ImmutableList.of(37190, 37191, 37192, 33387);
	private static final List<Integer> ARB_VERBOSITY_LEVELS = ImmutableList.of(37190, 37191, 37192);

	private static String unknown(int opcode) {
		return "Unknown (0x" + HexFormat.of().withUpperCase().toHexDigits(opcode) + ")";
	}

	public static String getSource(int opcode) {
		switch (opcode) {
			case 33350:
				return "API";
			case 33351:
				return "WINDOW SYSTEM";
			case 33352:
				return "SHADER COMPILER";
			case 33353:
				return "THIRD PARTY";
			case 33354:
				return "APPLICATION";
			case 33355:
				return "OTHER";
			default:
				return unknown(opcode);
		}
	}

	public static String getType(int opcode) {
		switch (opcode) {
			case 33356:
				return "ERROR";
			case 33357:
				return "DEPRECATED BEHAVIOR";
			case 33358:
				return "UNDEFINED BEHAVIOR";
			case 33359:
				return "PORTABILITY";
			case 33360:
				return "PERFORMANCE";
			case 33361:
				return "OTHER";
			case 33384:
				return "MARKER";
			default:
				return unknown(opcode);
		}
	}

	public static String getSeverity(int opcode) {
		switch (opcode) {
			case 33387:
				return "NOTIFICATION";
			case 37190:
				return "HIGH";
			case 37191:
				return "MEDIUM";
			case 37192:
				return "LOW";
			default:
				return unknown(opcode);
		}
	}

	private void onDebugMessage(int source, int type, int id, int severity, int length, long message, long l) {
		String string = GLDebugMessageCallback.getMessage(length, message);
		GlDebug.DebugMessage debugMessage;
		synchronized (this.debugMessages) {
			debugMessage = this.lastDebugMessage;
			if (debugMessage != null && debugMessage.equals(source, type, id, severity, string)) {
				debugMessage.count++;
			}
			else {
				debugMessage = new GlDebug.DebugMessage(source, type, id, severity, string);
				this.debugMessages.add(debugMessage);
				this.lastDebugMessage = debugMessage;
			}
		}

		LOGGER.info("OpenGL debug message: {}", debugMessage);
	}

	public List<String> collectDebugMessages() {
		synchronized (this.debugMessages) {
			List<String> list = Lists.newArrayListWithCapacity(this.debugMessages.size());

			for (GlDebug.DebugMessage debugMessage : this.debugMessages) {
				list.add(debugMessage + " x " + debugMessage.count);
			}

			return list;
		}
	}

	public static @Nullable GlDebug enableDebug(int verbosity, boolean sync, Set<String> usedGlCaps) {
		if (verbosity <= 0) {
			return null;
		}
		else {
			GLCapabilities gLCapabilities = GL.getCapabilities();
			if (gLCapabilities.GL_KHR_debug && GlBackend.allowGlKhrDebug) {
				GlDebug glDebug = new GlDebug();
				usedGlCaps.add("GL_KHR_debug");
				GL11.glEnable(37600);
				if (sync) {
					GL11.glEnable(33346);
				}

				for (int i = 0; i < KHR_VERBOSITY_LEVELS.size(); i++) {
					boolean bl = i < verbosity;
					KHRDebug.glDebugMessageControl(4352, 4352, KHR_VERBOSITY_LEVELS.get(i), (int[]) null, bl);
				}

				KHRDebug.glDebugMessageCallback(
						GLX.make(
								GLDebugMessageCallback.create(glDebug::onDebugMessage),
								Untracker::untrack
						), 0L
				);
				return glDebug;
			}
			else if (gLCapabilities.GL_ARB_debug_output && GlBackend.allowGlArbDebugOutput) {
				GlDebug glDebug = new GlDebug();
				usedGlCaps.add("GL_ARB_debug_output");
				if (sync) {
					GL11.glEnable(33346);
				}

				for (int i = 0; i < ARB_VERBOSITY_LEVELS.size(); i++) {
					boolean bl = i < verbosity;
					ARBDebugOutput.glDebugMessageControlARB(4352, 4352, ARB_VERBOSITY_LEVELS.get(i), (int[]) null, bl);
				}

				ARBDebugOutput.glDebugMessageCallbackARB(
						GLX.make(
								GLDebugMessageARBCallback.create(glDebug::onDebugMessage),
								Untracker::untrack
						), 0L
				);
				return glDebug;
			}
			else {
				return null;
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code DebugMessage}.
	 */
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
			return type == this.type && source == this.source && id == this.id && severity == this.severity
					&& message.equals(this.message);
		}

		@Override
		public String toString() {
			return "id="
					+ this.id
					+ ", source="
					+ GlDebug.getSource(this.source)
					+ ", type="
					+ GlDebug.getType(this.type)
					+ ", severity="
					+ GlDebug.getSeverity(this.severity)
					+ ", message='"
					+ this.message
					+ "'";
		}
	}
}

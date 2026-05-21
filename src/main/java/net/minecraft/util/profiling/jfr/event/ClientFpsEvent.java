package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.*;
import net.minecraft.obfuscate.DontObfuscate;

@Name("minecraft.ClientFps")
@Label("Client fps")
@Category({"Minecraft", "Ticking"})
@StackTrace(false)
@Period("1 s")
@DontObfuscate
/**
 * {@code ClientFpsEvent}.
 */
public class ClientFpsEvent extends Event {

	public static final String EVENT_NAME = "minecraft.ClientFps";
	public static final EventType TYPE = EventType.getEventType(ClientFpsEvent.class);
	@Name("fps")
	@Label("Client fps")
	public final int fps;

	public ClientFpsEvent(int fps) {
		this.fps = fps;
	}

	/**
	 * {@code Names}.
	 */
	public static class Names {

		public static final String FPS = "fps";

		private Names() {
		}
	}
}

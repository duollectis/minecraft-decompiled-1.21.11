package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.*;
import net.minecraft.obfuscate.DontObfuscate;

/**
 * JFR-событие частоты кадров клиента. Записывается раз в секунду
 * и содержит текущее значение FPS для профилирования производительности рендеринга.
 */
@Name("minecraft.ClientFps")
@Label("Client fps")
@Category({"Minecraft", "Ticking"})
@StackTrace(false)
@Period("1 s")
@DontObfuscate
public class ClientFpsEvent extends Event {

	public static final String EVENT_NAME = "minecraft.ClientFps";
	public static final EventType TYPE = EventType.getEventType(ClientFpsEvent.class);
	@Name("fps")
	@Label("Client fps")
	public final int fps;

	public ClientFpsEvent(int fps) {
		this.fps = fps;
	}

	public static class Names {

		public static final String FPS = "fps";

		private Names() {
		}
	}
}

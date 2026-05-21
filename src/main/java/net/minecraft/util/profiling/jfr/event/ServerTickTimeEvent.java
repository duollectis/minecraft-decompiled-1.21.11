package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.*;
import net.minecraft.obfuscate.DontObfuscate;

@Name("minecraft.ServerTickTime")
@Label("Server Tick Time")
@Category({"Minecraft", "Ticking"})
@StackTrace(false)
@Period("1 s")
@DontObfuscate
/**
 * {@code ServerTickTimeEvent}.
 */
public class ServerTickTimeEvent extends Event {

	public static final String EVENT_NAME = "minecraft.ServerTickTime";
	public static final EventType TYPE = EventType.getEventType(ServerTickTimeEvent.class);
	@Name("averageTickDuration")
	@Label("Average Server Tick Duration")
	@Timespan
	public final long averageTickDurationNanos;

	public ServerTickTimeEvent(float averageTickMilliseconds) {
		this.averageTickDurationNanos = (long) (1000000.0F * averageTickMilliseconds);
	}

	/**
	 * {@code Names}.
	 */
	public static class Names {

		public static final String AVERAGE_TICK_DURATION = "averageTickDuration";

		private Names() {
		}
	}
}

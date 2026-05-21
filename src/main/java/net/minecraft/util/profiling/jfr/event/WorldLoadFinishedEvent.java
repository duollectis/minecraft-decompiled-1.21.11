package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.*;
import net.minecraft.obfuscate.DontObfuscate;

@Name("minecraft.LoadWorld")
@Label("Create/Load World")
@Category({"Minecraft", "World Generation"})
@StackTrace(false)
@DontObfuscate
/**
 * {@code WorldLoadFinishedEvent}.
 */
public class WorldLoadFinishedEvent extends Event {

	public static final String EVENT_NAME = "minecraft.LoadWorld";
	public static final EventType TYPE = EventType.getEventType(WorldLoadFinishedEvent.class);
}

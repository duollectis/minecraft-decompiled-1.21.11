package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.*;
import net.minecraft.obfuscate.DontObfuscate;

/** JFR-событие завершения загрузки или создания мира: маркер для измерения полного времени инициализации уровня. */
@Name("minecraft.LoadWorld")
@Label("Create/Load World")
@Category({"Minecraft", "World Generation"})
@StackTrace(false)
@DontObfuscate
public class WorldLoadFinishedEvent extends Event {

	public static final String EVENT_NAME = "minecraft.LoadWorld";
	public static final EventType TYPE = EventType.getEventType(WorldLoadFinishedEvent.class);
}

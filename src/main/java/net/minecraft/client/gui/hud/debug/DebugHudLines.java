package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

import java.util.Collection;

/**
 * Приёмник строк для отладочного HUD. Позволяет добавлять строки
 * как в приоритетный список, так и в именованные секции.
 */
@Environment(EnvType.CLIENT)
public interface DebugHudLines {

	void addPriorityLine(String line);

	void addLine(String line);

	void addLinesToSection(Identifier sectionId, Collection<String> lines);

	void addLineToSection(Identifier sectionId, String line);
}

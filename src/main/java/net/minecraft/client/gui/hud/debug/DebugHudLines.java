package net.minecraft.client.gui.hud.debug;

import java.util.Collection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public interface DebugHudLines {
   void addPriorityLine(String line);

   void addLine(String line);

   void addLinesToSection(Identifier sectionId, Collection<String> lines);

   void addLineToSection(Identifier sectionId, String line);
}

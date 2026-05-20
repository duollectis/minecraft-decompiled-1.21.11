package net.minecraft.screen;

import net.fabricmc.fabric.api.screenhandler.v1.FabricScreenHandlerFactory;
import net.minecraft.text.Text;

public interface NamedScreenHandlerFactory extends ScreenHandlerFactory, FabricScreenHandlerFactory {
   Text getDisplayName();
}

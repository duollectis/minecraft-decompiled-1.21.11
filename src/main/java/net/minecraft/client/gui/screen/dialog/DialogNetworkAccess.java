package net.minecraft.client.gui.screen.dialog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.ServerLinks;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Интерфейс сетевого доступа для диалоговых экранов.
 * Предоставляет методы для отправки команд, показа диалогов и отключения от сервера.
 */
@Environment(EnvType.CLIENT)
public interface DialogNetworkAccess {

	void disconnect(Text reason);

	void runClickEventCommand(String command, @Nullable Screen afterActionScreen);

	void showDialog(RegistryEntry<Dialog> dialog, @Nullable Screen afterActionScreen);

	void sendCustomClickActionPacket(Identifier id, Optional<NbtElement> payload);

	ServerLinks getServerLinks();
}

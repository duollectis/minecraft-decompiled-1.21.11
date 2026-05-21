package net.minecraft.server.dedicated.gui;

import net.minecraft.server.MinecraftServer;

import javax.swing.*;
import java.util.Vector;

/**
 * {@code PlayerListGui}.
 */
public class PlayerListGui extends JList<String> {

	private final MinecraftServer server;
	private int tick;

	public PlayerListGui(MinecraftServer server) {
		this.server = server;
		server.addServerGuiTickable(this::tick);
	}

	/**
	 * Tick.
	 */
	public void tick() {
		if (this.tick++ % 20 == 0) {
			Vector<String> vector = new Vector<>();

			for (int i = 0; i < this.server.getPlayerManager().getPlayerList().size(); i++) {
				vector.add(this.server.getPlayerManager().getPlayerList().get(i).getGameProfile().name());
			}

			this.setListData(vector);
		}
	}
}

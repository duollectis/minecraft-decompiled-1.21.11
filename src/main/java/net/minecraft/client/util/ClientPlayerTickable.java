package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Маркерный интерфейс для объектов, которые должны тикаться вместе с клиентским игроком.
 */
@Environment(EnvType.CLIENT)
public interface ClientPlayerTickable {

	void tick();
}

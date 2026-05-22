package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

/**
 * Запись отладочного HUD: идентификатор активного пост-эффекта рендеринга.
 */
@Environment(EnvType.CLIENT)
public class PostEffectDebugHudEntry implements DebugHudEntry {

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		Identifier postProcessorId = MinecraftClient.getInstance().gameRenderer.getPostProcessorId();

		if (postProcessorId != null) {
			lines.addLine("Post: " + postProcessorId);
		}
	}
}

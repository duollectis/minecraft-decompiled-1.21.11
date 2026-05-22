package net.minecraft.client.gui.hud.bar;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

/**
 * Полоса опыта игрока. Отображает прогресс до следующего уровня
 * и рисует числовой уровень через {@link Bar#drawExperienceLevel}.
 */
@Environment(EnvType.CLIENT)
public class ExperienceBar implements Bar {

	private static final Identifier BACKGROUND = Identifier.ofVanilla("hud/experience_bar_background");
	private static final Identifier PROGRESS = Identifier.ofVanilla("hud/experience_bar_progress");

	/** Максимальная ширина заполненной полосы в пикселях (на 1 больше WIDTH для корректного масштабирования). */
	private static final int PROGRESS_MAX_WIDTH = 183;

	private final MinecraftClient client;

	public ExperienceBar(MinecraftClient client) {
		this.client = client;
	}

	@Override
	public void renderBar(DrawContext context, RenderTickCounter tickCounter) {
		ClientPlayerEntity player = client.player;
		int barX = getCenterX(client.getWindow());
		int barY = getCenterY(client.getWindow());
		int nextLevelXp = player.getNextLevelExperience();

		if (nextLevelXp <= 0) {
			return;
		}

		int progressWidth = (int) (player.experienceProgress * PROGRESS_MAX_WIDTH);

		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND, barX, barY, WIDTH, HEIGHT);

		if (progressWidth > 0) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, PROGRESS, WIDTH, HEIGHT, 0, 0, barX, barY, progressWidth, HEIGHT);
		}
	}

	@Override
	public void renderAddons(DrawContext context, RenderTickCounter tickCounter) {
	}
}

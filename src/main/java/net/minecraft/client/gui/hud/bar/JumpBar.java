package net.minecraft.client.gui.hud.bar;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.JumpingMount;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.Objects;

/**
 * Полоса заряда прыжка верхового животного (лошадь, осёл и т.п.).
 * Показывает либо кулдаун прыжка, либо текущий заряд.
 */
@Environment(EnvType.CLIENT)
public class JumpBar implements Bar {

	private static final Identifier BACKGROUND = Identifier.ofVanilla("hud/jump_bar_background");
	private static final Identifier COOLDOWN = Identifier.ofVanilla("hud/jump_bar_cooldown");
	private static final Identifier PROGRESS = Identifier.ofVanilla("hud/jump_bar_progress");

	private final MinecraftClient client;
	private final JumpingMount jumpingMount;

	public JumpBar(MinecraftClient client) {
		this.client = client;
		jumpingMount = Objects.requireNonNull(Objects.requireNonNull(client.player).getJumpingMount());
	}

	@Override
	public void renderBar(DrawContext context, RenderTickCounter tickCounter) {
		int barX = getCenterX(client.getWindow());
		int barY = getCenterY(client.getWindow());

		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND, barX, barY, WIDTH, HEIGHT);

		if (jumpingMount.getJumpCooldown() > 0) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, COOLDOWN, barX, barY, WIDTH, HEIGHT);
			return;
		}

		int chargeWidth = MathHelper.lerpPositive(client.player.getMountJumpStrength(), 0, WIDTH);

		if (chargeWidth > 0) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, PROGRESS, WIDTH, HEIGHT, 0, 0, barX, barY, chargeWidth, HEIGHT);
		}
	}

	@Override
	public void renderAddons(DrawContext context, RenderTickCounter tickCounter) {
	}
}

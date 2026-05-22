package net.minecraft.client.gui.screen.ingame;

import com.google.common.collect.Ordering;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Отрисовывает панель активных эффектов статуса справа от инвентаря.
 * Показывается только если справа от контейнера достаточно места (минимум {@code EFFECT_BACKGROUND_HEIGHT} пикселей).
 */
@Environment(EnvType.CLIENT)
public class StatusEffectsDisplay {

	private static final Identifier BACKGROUND_TEXTURE = Identifier.ofVanilla("container/inventory/effect_background");
	private static final Identifier AMBIENT_BACKGROUND_TEXTURE = Identifier.ofVanilla("container/inventory/effect_background_ambient");
	private static final int EFFECT_ICON_SIZE = 18;
	public static final int EFFECT_ICON_OFFSET = 7;
	private static final int EFFECT_BACKGROUND_HEIGHT = 32;
	public static final int MIN_PANEL_WIDTH = 32;

	private final HandledScreen<?> parent;
	private final MinecraftClient client;

	public StatusEffectsDisplay(HandledScreen<?> parent) {
		this.parent = parent;
		client = MinecraftClient.getInstance();
	}

	public boolean shouldHideStatusEffectHud() {
		int panelLeft = parent.x + parent.backgroundWidth + 2;
		int availableWidth = parent.width - panelLeft;
		return availableWidth >= EFFECT_BACKGROUND_HEIGHT;
	}

	public void render(DrawContext context, int mouseX, int mouseY) {
		int panelLeft = parent.x + parent.backgroundWidth + 2;
		int availableWidth = parent.width - panelLeft;
		Collection<StatusEffectInstance> effects = client.player.getStatusEffects();

		if (effects.isEmpty() || availableWidth < EFFECT_BACKGROUND_HEIGHT) {
			return;
		}

		int effectWidth = availableWidth >= 120 ? availableWidth - 7 : MIN_PANEL_WIDTH;
		int effectSpacing = 33;
		if (effects.size() > 5) {
			effectSpacing = 132 / (effects.size() - 1);
		}

		drawStatusEffects(context, effects, panelLeft, effectSpacing, mouseX, mouseY, effectWidth);
	}

	private void drawStatusEffects(
		DrawContext context,
		Collection<StatusEffectInstance> effects,
		int x,
		int spacing,
		int mouseX,
		int mouseY,
		int width
	) {
		Iterable<StatusEffectInstance> sorted = Ordering.natural().sortedCopy(effects);
		int currentY = parent.y;
		TextRenderer textRenderer = parent.getTextRenderer();

		for (StatusEffectInstance effect : sorted) {
			boolean isAmbient = effect.isAmbient();
			Text description = getStatusEffectDescription(effect);
			Text duration = StatusEffectUtil.getDurationText(
				effect,
				1.0F,
				client.world.getTickManager().getTickRate()
			);
			int backgroundWidth = drawStatusEffectBackgrounds(context, textRenderer, description, duration, x, currentY, isAmbient, width);
			drawTexts(context, description, duration, textRenderer, x, currentY, backgroundWidth, spacing, mouseX, mouseY);
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				InGameHud.getEffectTexture(effect.getEffectType()),
				x + EFFECT_ICON_OFFSET,
				currentY + EFFECT_ICON_OFFSET,
				EFFECT_ICON_SIZE,
				EFFECT_ICON_SIZE
			);
			currentY += spacing;
		}
	}

	private int drawStatusEffectBackgrounds(
		DrawContext context,
		TextRenderer textRenderer,
		Text description,
		Text duration,
		int x,
		int y,
		boolean ambient,
		int width
	) {
		int descriptionWidth = EFFECT_BACKGROUND_HEIGHT + textRenderer.getWidth(description) + EFFECT_ICON_OFFSET;
		int durationWidth = EFFECT_BACKGROUND_HEIGHT + textRenderer.getWidth(duration) + EFFECT_ICON_OFFSET;
		int backgroundWidth = Math.min(width, Math.max(descriptionWidth, durationWidth));
		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			ambient ? AMBIENT_BACKGROUND_TEXTURE : BACKGROUND_TEXTURE,
			x,
			y,
			backgroundWidth,
			EFFECT_BACKGROUND_HEIGHT
		);
		return backgroundWidth;
	}

	private void drawTexts(
		DrawContext context,
		Text description,
		Text duration,
		TextRenderer textRenderer,
		int x,
		int y,
		int width,
		int height,
		int mouseX,
		int mouseY
	) {
		int textX = x + EFFECT_BACKGROUND_HEIGHT;
		int textY = y + EFFECT_ICON_OFFSET;
		int availableTextWidth = width - EFFECT_BACKGROUND_HEIGHT - EFFECT_ICON_OFFSET;
		boolean needsTooltip;

		if (availableTextWidth > 0) {
			boolean isTruncated = textRenderer.getWidth(description) > availableTextWidth;
			OrderedText displayText = isTruncated
				? TextWidget.trim(description, textRenderer, availableTextWidth)
				: description.asOrderedText();
			context.drawTextWithShadow(textRenderer, displayText, textX, textY, -1);
			context.drawTextWithShadow(textRenderer, duration, textX, textY + 9, -8355712);
			needsTooltip = isTruncated;
		} else {
			needsTooltip = true;
		}

		if (needsTooltip && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
			context.drawTooltip(
				parent.getTextRenderer(),
				List.of(description, duration),
				Optional.empty(),
				mouseX,
				mouseY
			);
		}
	}

	private Text getStatusEffectDescription(StatusEffectInstance statusEffect) {
		MutableText name = statusEffect.getEffectType().value().getName().copy();
		if (statusEffect.getAmplifier() >= 1 && statusEffect.getAmplifier() <= 9) {
			name
				.append(ScreenTexts.SPACE)
				.append(Text.translatable("enchantment.level." + (statusEffect.getAmplifier() + 1)));
		}

		return name;
	}
}

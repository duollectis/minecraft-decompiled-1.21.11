package net.minecraft.client.toast;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
/**
 * {@code AdvancementToast}.
 */
public class AdvancementToast implements Toast {

	private static final Identifier TEXTURE = Identifier.ofVanilla("toast/advancement");
	public static final int DEFAULT_DURATION_MS = 5000;
	private final AdvancementEntry advancement;
	private Toast.Visibility visibility = Toast.Visibility.HIDE;

	public AdvancementToast(AdvancementEntry advancement) {
		this.advancement = advancement;
	}

	@Override
	public Toast.Visibility getVisibility() {
		return this.visibility;
	}

	@Override
	public void update(ToastManager manager, long time) {
		AdvancementDisplay advancementDisplay = this.advancement.value().display().orElse(null);
		if (advancementDisplay == null) {
			this.visibility = Toast.Visibility.HIDE;
		}
		else {
			this.visibility =
					time >= 5000.0 * manager.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE
					                                                                : Toast.Visibility.SHOW;
		}
	}

	@Override
	public @Nullable SoundEvent getSoundEvent() {
		return this.isChallenge() ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : null;
	}

	private boolean isChallenge() {
		Optional<AdvancementDisplay> optional = this.advancement.value().display();
		return optional.isPresent() && optional.get().getFrame().equals(AdvancementFrame.CHALLENGE);
	}

	@Override
	public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
		AdvancementDisplay advancementDisplay = this.advancement.value().display().orElse(null);
		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, this.getWidth(), this.getHeight());
		if (advancementDisplay != null) {
			List<OrderedText> list = textRenderer.wrapLines(advancementDisplay.getTitle(), 125);
			int i = advancementDisplay.getFrame() == AdvancementFrame.CHALLENGE ? -30465 : -256;
			if (list.size() == 1) {
				context.drawText(textRenderer, advancementDisplay.getFrame().getToastText(), 30, 7, i, false);
				context.drawText(textRenderer, list.get(0), 30, 18, -1, false);
			}
			else {
				int j = 1500;
				float f = 300.0F;
				if (startTime < 1500L) {
					int
							k =
							MathHelper.floor(
									MathHelper.clamp((float) (1500L - startTime) / 300.0F, 0.0F, 1.0F) * 255.0F);
					context.drawText(
							textRenderer,
							advancementDisplay.getFrame().getToastText(),
							30,
							11,
							ColorHelper.withAlpha(k, i),
							false
					);
				}
				else {
					int
							k =
							MathHelper.floor(
									MathHelper.clamp((float) (startTime - 1500L) / 300.0F, 0.0F, 1.0F) * 252.0F);
					int l = this.getHeight() / 2 - list.size() * 9 / 2;

					for (OrderedText orderedText : list) {
						context.drawText(textRenderer, orderedText, 30, l, ColorHelper.whiteWithAlpha(k), false);
						l += 9;
					}
				}
			}

			context.drawItemWithoutEntity(advancementDisplay.getIcon(), 8, 8);
		}
	}
}

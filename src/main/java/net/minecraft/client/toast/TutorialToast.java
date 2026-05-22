package net.minecraft.client.toast;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Toast обучающей подсказки с иконкой, текстом и опциональной полосой прогресса.
 *
 * <p>Если задан {@code displayDuration > 0}, прогресс-бар заполняется автоматически
 * за указанное время, после чего toast скрывается. Иначе прогресс управляется
 * вручную через {@link #setProgress(float)}.
 */
@Environment(EnvType.CLIENT)
public class TutorialToast implements Toast {

	private static final Identifier TEXTURE = Identifier.ofVanilla("toast/tutorial");
	public static final int PROGRESS_BAR_WIDTH = 154;
	public static final int PROGRESS_BAR_HEIGHT = 1;
	public static final int PROGRESS_BAR_X = 3;
	public static final int PADDING = 4;
	private static final int ICON_PADDING = 7;
	private static final int TEXT_PADDING = 3;
	private static final int TITLE_PADDING = 11;
	private static final int ICON_WIDTH = 30;
	private static final int TEXT_WIDTH = 126;
	private static final float LERP_SPEED = 100.0F;

	private final Type type;
	private final List<OrderedText> text;
	private Toast.Visibility visibility = Toast.Visibility.SHOW;
	private long lastTime;
	private float lastProgress;
	private float progress;
	private final boolean hasProgressBar;
	private final int displayDuration;

	public TutorialToast(
		TextRenderer textRenderer,
		Type type,
		Text title,
		@Nullable Text description,
		boolean hasProgressBar,
		int displayDuration
	) {
		this.type = type;
		text = new ArrayList<>(2);
		text.addAll(textRenderer.wrapLines(title.copy().withColor(-11534256), TEXT_WIDTH));

		if (description != null) {
			text.addAll(textRenderer.wrapLines(description, TEXT_WIDTH));
		}

		this.hasProgressBar = hasProgressBar;
		this.displayDuration = displayDuration;
	}

	public TutorialToast(
		TextRenderer textRenderer,
		Type type,
		Text title,
		@Nullable Text description,
		boolean hasProgressBar
	) {
		this(textRenderer, type, title, description, hasProgressBar, 0);
	}

	@Override
	public Toast.Visibility getVisibility() {
		return visibility;
	}

	@Override
	public void update(ToastManager manager, long time) {
		if (displayDuration > 0) {
			progress = Math.min((float) time / displayDuration, 1.0F);
			lastProgress = progress;
			lastTime = time;

			if (time > displayDuration) {
				hide();
			}
		} else if (hasProgressBar) {
			lastProgress = MathHelper.clampedLerp(
				(float) (time - lastTime) / LERP_SPEED,
				lastProgress,
				progress
			);
			lastTime = time;
		}
	}

	@Override
	public int getHeight() {
		return ICON_PADDING + getTextHeight() + TEXT_PADDING;
	}

	private int getTextHeight() {
		return Math.max(text.size(), 2) * TITLE_PADDING;
	}

	@Override
	public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
		int height = getHeight();
		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, getWidth(), height);
		type.drawIcon(context, 6, 6);

		int textBlockHeight = text.size() * TITLE_PADDING;
		int textStartY = ICON_PADDING + (getTextHeight() - textBlockHeight) / 2;

		for (int lineIndex = 0; lineIndex < text.size(); lineIndex++) {
			context.drawText(textRenderer, text.get(lineIndex), ICON_WIDTH, textStartY + lineIndex * TITLE_PADDING, -16777216, false);
		}

		if (hasProgressBar) {
			int barY = height - PADDING;
			context.fill(PROGRESS_BAR_X, barY, 157, barY + PROGRESS_BAR_HEIGHT, -1);
			int barColor = progress >= lastProgress ? -16755456 : -11206656;
			context.fill(PROGRESS_BAR_X, barY, (int) (PROGRESS_BAR_X + PROGRESS_BAR_WIDTH * lastProgress), barY + PROGRESS_BAR_HEIGHT, barColor);
		}
	}

	public void hide() {
		visibility = Toast.Visibility.HIDE;
	}

	public void setProgress(float progress) {
		this.progress = progress;
	}

	/** Тип обучающего toast с иконкой, отображаемой слева от текста. */
	@Environment(EnvType.CLIENT)
	public enum Type {
		MOVEMENT_KEYS(Identifier.ofVanilla("toast/movement_keys")),
		MOUSE(Identifier.ofVanilla("toast/mouse")),
		TREE(Identifier.ofVanilla("toast/tree")),
		RECIPE_BOOK(Identifier.ofVanilla("toast/recipe_book")),
		WOODEN_PLANKS(Identifier.ofVanilla("toast/wooden_planks")),
		SOCIAL_INTERACTIONS(Identifier.ofVanilla("toast/social_interactions")),
		RIGHT_CLICK(Identifier.ofVanilla("toast/right_click"));

		private final Identifier texture;

		Type(final Identifier texture) {
			this.texture = texture;
		}

		public void drawIcon(DrawContext context, int x, int y) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 20, 20);
		}
	}
}

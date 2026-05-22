package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Alignment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2f;

/**
 * Рендерер анимированного «сплэш-текста» на экране заголовка.
 * Текст вращается под углом и пульсирует по размеру в такт синусоиде.
 */
@Environment(EnvType.CLIENT)
public class SplashTextRenderer {

	public static final SplashTextRenderer MERRY_X_MAS = new SplashTextRenderer(SplashTextResourceSupplier.MERRY_X_MAS_);
	public static final SplashTextRenderer HAPPY_NEW_YEAR = new SplashTextRenderer(SplashTextResourceSupplier.HAPPY_NEW_YEAR_);
	public static final SplashTextRenderer OOOOO_O_O_OOOOO__SPOOKY = new SplashTextRenderer(SplashTextResourceSupplier.OOOOO_O_O_OOOOO__SPOOKY_);

	private static final int TEXT_X = 123;
	private static final int TEXT_Y = 69;
	private static final float TEXT_ROTATION = (float) (-Math.PI / 9);
	private static final float PULSE_AMPLITUDE = 0.1F;
	private static final float PULSE_BASE = 1.8F;
	private static final float SCALE_PADDING = 32.0F;
	private static final float SCALE_FACTOR = 100.0F;
	private static final long PULSE_PERIOD_MS = 1000L;

	private final Text text;

	public SplashTextRenderer(Text text) {
		this.text = text;
	}

	/**
	 * Рендерит сплэш-текст с пульсирующим масштабом и фиксированным наклоном.
	 * Масштаб вычисляется так, чтобы текст вписывался в отведённую ширину с учётом анимации.
	 *
	 * @param context      контекст отрисовки GUI
	 * @param screenWidth  ширина экрана в пикселях
	 * @param textRenderer рендерер шрифта
	 * @param alpha        прозрачность текста (0.0–1.0)
	 */
	public void render(DrawContext context, int screenWidth, TextRenderer textRenderer, float alpha) {
		int textWidth = textRenderer.getWidth(text);
		DrawnTextConsumer textConsumer = context.getTextConsumer();

		float pulse = PULSE_BASE - MathHelper.abs(
			MathHelper.sin((float) (Util.getMeasuringTimeMs() % PULSE_PERIOD_MS) / (float) PULSE_PERIOD_MS * (float) (Math.PI * 2))
				* PULSE_AMPLITUDE
		);
		float scale = pulse * SCALE_FACTOR / (textWidth + SCALE_PADDING);

		Matrix3x2f matrix = new Matrix3x2f(textConsumer.getTransformation().pose())
			.translate(screenWidth / 2.0F + TEXT_X, TEXT_Y)
			.rotate(TEXT_ROTATION)
			.scale(scale);

		DrawnTextConsumer.Transformation transformation = textConsumer.getTransformation()
			.withOpacity(alpha)
			.withPose(matrix);

		textConsumer.text(Alignment.LEFT, -textWidth / 2, -8, transformation, text);
	}
}

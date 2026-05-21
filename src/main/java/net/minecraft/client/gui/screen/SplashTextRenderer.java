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

@Environment(EnvType.CLIENT)
/**
 * {@code SplashTextRenderer}.
 */
public class SplashTextRenderer {

	public static final SplashTextRenderer
			MERRY_X_MAS =
			new SplashTextRenderer(SplashTextResourceSupplier.MERRY_X_MAS_);
	public static final SplashTextRenderer
			HAPPY_NEW_YEAR =
			new SplashTextRenderer(SplashTextResourceSupplier.HAPPY_NEW_YEAR_);
	public static final SplashTextRenderer
			OOOOO_O_O_OOOOO__SPOOKY =
			new SplashTextRenderer(SplashTextResourceSupplier.OOOOO_O_O_OOOOO__SPOOKY_);
	private static final int TEXT_X = 123;
	private static final int TEXT_Y = 69;
	private static final float TEXT_ROTATION = (float) (-Math.PI / 9);
	private final Text text;

	public SplashTextRenderer(Text text) {
		this.text = text;
	}

	/**
	 * Render.
	 *
	 * @param context context
	 * @param screenWidth screen width
	 * @param textRenderer text renderer
	 * @param alpha alpha
	 */
	public void render(DrawContext context, int screenWidth, TextRenderer textRenderer, float alpha) {
		int i = textRenderer.getWidth(this.text);
		DrawnTextConsumer drawnTextConsumer = context.getTextConsumer();
		float
				f =
				1.8F - MathHelper.abs(
						MathHelper.sin((float) (Util.getMeasuringTimeMs() % 1000L) / 1000.0F * (float) (Math.PI * 2))
								* 0.1F);
		float g = f * 100.0F / (i + 32);
		Matrix3x2f matrix3x2f = new Matrix3x2f(drawnTextConsumer.getTransformation().pose())
				.translate(screenWidth / 2.0F + 123.0F, 69.0F)
				.rotate((float) (-Math.PI / 9))
				.scale(g);
		DrawnTextConsumer.Transformation
				transformation =
				drawnTextConsumer.getTransformation().withOpacity(alpha).withPose(matrix3x2f);
		drawnTextConsumer.text(Alignment.LEFT, -i / 2, -8, transformation, this.text);
	}
}

package net.minecraft.client.gui.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.render.state.special.BookModelGuiElementRenderState;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BookModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Рендерер 3D-модели книги в GUI (например, в интерфейсе зачарования).
 * Управляет анимацией открытия книги и перелистывания страниц.
 */
@Environment(EnvType.CLIENT)
public class BookModelGuiElementRenderer extends SpecialGuiElementRenderer<BookModelGuiElementRenderState> {

	private static final float BOOK_X_ROTATION_DEGREES = 25.0F;
	private static final float BOOK_Y_ROTATION_DEGREES = 180.0F;
	private static final float BOOK_FLIP_PHASE_OFFSET_A = 0.25F;
	private static final float BOOK_FLIP_PHASE_OFFSET_B = 0.75F;
	private static final float BOOK_FLIP_SCALE = 1.6F;
	private static final float BOOK_FLIP_CLAMP_SHIFT = 0.3F;
	private static final float BOOK_Y_OFFSET_SCALE = 17;

	public BookModelGuiElementRenderer(VertexConsumerProvider.Immediate immediate) {
		super(immediate);
	}

	@Override
	public Class<BookModelGuiElementRenderState> getElementClass() {
		return BookModelGuiElementRenderState.class;
	}

	/**
	 * Отрисовывает анимированную 3D-модель книги.
	 * Вычисляет прогресс перелистывания страниц через дробную часть фазы анимации,
	 * чтобы создать плавный эффект перелистывания двух страниц одновременно.
	 */
	@Override
	protected void render(BookModelGuiElementRenderState state, MatrixStack matrices) {
		MinecraftClient.getInstance().gameRenderer
				.getDiffuseLighting()
				.setShaderLights(DiffuseLighting.Type.ENTITY_IN_UI);

		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(BOOK_Y_ROTATION_DEGREES));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(BOOK_X_ROTATION_DEGREES));

		float openProgress = state.open();
		matrices.translate(
				(1.0F - openProgress) * 0.2F,
				(1.0F - openProgress) * 0.1F,
				(1.0F - openProgress) * 0.25F
		);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-(1.0F - openProgress) * 90.0F - 90.0F));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(BOOK_Y_ROTATION_DEGREES));

		float flipPhase = state.flip();
		float pageA = MathHelper.clamp(
				MathHelper.fractionalPart(flipPhase + BOOK_FLIP_PHASE_OFFSET_A) * BOOK_FLIP_SCALE - BOOK_FLIP_CLAMP_SHIFT,
				0.0F,
				1.0F
		);
		float pageB = MathHelper.clamp(
				MathHelper.fractionalPart(flipPhase + BOOK_FLIP_PHASE_OFFSET_B) * BOOK_FLIP_SCALE - BOOK_FLIP_CLAMP_SHIFT,
				0.0F,
				1.0F
		);

		BookModel bookModel = state.bookModel();
		bookModel.setAngles(new BookModel.BookModelState(0.0F, pageA, pageB, openProgress));

		Identifier texture = state.texture();
		VertexConsumer vertexConsumer = vertexConsumers.getBuffer(bookModel.getLayer(texture));
		bookModel.render(matrices, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV);
	}

	@Override
	protected float getYOffset(int height, int windowScaleFactor) {
		return BOOK_Y_OFFSET_SCALE * windowScaleFactor;
	}

	@Override
	protected String getName() {
		return "book model";
	}
}

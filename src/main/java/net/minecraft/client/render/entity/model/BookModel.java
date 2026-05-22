package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.math.MathHelper;

/**
 * Модель открытой книги с анимацией перелистывания страниц.
 * <p>
 * Используется для отображения книги на подставке (enchanting table).
 * Анимация управляется через {@link BookModelState}: угол раскрытия обложек,
 * скорость перелистывания и прогресс перелистывания каждой страницы.
 */
@Environment(EnvType.CLIENT)
public class BookModel extends Model<BookModel.BookModelState> {

	private static final String LEFT_LID = "left_lid";
	private static final String RIGHT_LID = "right_lid";
	private static final String LEFT_PAGES = "left_pages";
	private static final String RIGHT_PAGES = "right_pages";
	private static final String FLIP_PAGE1 = "flip_page1";
	private static final String FLIP_PAGE2 = "flip_page2";

	private final ModelPart leftCover;
	private final ModelPart rightCover;
	private final ModelPart leftPages;
	private final ModelPart rightPages;
	private final ModelPart leftFlippingPage;
	private final ModelPart rightFlippingPage;

	public BookModel(ModelPart root) {
		super(root, RenderLayers::entitySolid);
		leftCover = root.getChild(LEFT_LID);
		rightCover = root.getChild(RIGHT_LID);
		leftPages = root.getChild(LEFT_PAGES);
		rightPages = root.getChild(RIGHT_PAGES);
		leftFlippingPage = root.getChild(FLIP_PAGE1);
		rightFlippingPage = root.getChild(FLIP_PAGE2);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();
		root.addChild(
				LEFT_LID,
				ModelPartBuilder.create().uv(0, 0).cuboid(-6.0F, -5.0F, -0.005F, 6.0F, 10.0F, 0.005F),
				ModelTransform.origin(0.0F, 0.0F, -1.0F)
		);
		root.addChild(
				RIGHT_LID,
				ModelPartBuilder.create().uv(16, 0).cuboid(0.0F, -5.0F, -0.005F, 6.0F, 10.0F, 0.005F),
				ModelTransform.origin(0.0F, 0.0F, 1.0F)
		);
		root.addChild(
				"seam",
				ModelPartBuilder.create().uv(12, 0).cuboid(-1.0F, -5.0F, 0.0F, 2.0F, 10.0F, 0.005F),
				ModelTransform.rotation(0.0F, (float) (Math.PI / 2), 0.0F)
		);
		root.addChild(
				LEFT_PAGES,
				ModelPartBuilder.create().uv(0, 10).cuboid(0.0F, -4.0F, -0.99F, 5.0F, 8.0F, 1.0F),
				ModelTransform.NONE
		);
		root.addChild(
				RIGHT_PAGES,
				ModelPartBuilder.create().uv(12, 10).cuboid(0.0F, -4.0F, -0.01F, 5.0F, 8.0F, 1.0F),
				ModelTransform.NONE
		);
		ModelPartBuilder flipPageBuilder =
				ModelPartBuilder.create().uv(24, 10).cuboid(0.0F, -4.0F, 0.0F, 5.0F, 8.0F, 0.005F);
		root.addChild(FLIP_PAGE1, flipPageBuilder, ModelTransform.NONE);
		root.addChild(FLIP_PAGE2, flipPageBuilder, ModelTransform.NONE);
		return TexturedModelData.of(modelData, 64, 32);
	}

	@Override
	public void setAngles(BookModel.BookModelState state) {
		super.setAngles(state);
		float openAngle = (MathHelper.sin(state.pageTurnAmount * 0.02F) * 0.1F + 1.25F) * state.pageTurnSpeed;
		leftCover.yaw = (float) Math.PI + openAngle;
		rightCover.yaw = -openAngle;
		leftPages.yaw = openAngle;
		rightPages.yaw = -openAngle;
		leftFlippingPage.yaw = openAngle - openAngle * 2.0F * state.leftFlipAmount;
		rightFlippingPage.yaw = openAngle - openAngle * 2.0F * state.rightFlipAmount;
		leftPages.originX = MathHelper.sin(openAngle);
		rightPages.originX = MathHelper.sin(openAngle);
		leftFlippingPage.originX = MathHelper.sin(openAngle);
		rightFlippingPage.originX = MathHelper.sin(openAngle);
	}

	/**
	 * Состояние анимации книги: параметры раскрытия и перелистывания страниц.
	 *
	 * @param pageTurnAmount  накопленный счётчик перелистываний (влияет на угол раскрытия)
	 * @param leftFlipAmount  прогресс перелистывания левой страницы [0..1]
	 * @param rightFlipAmount прогресс перелистывания правой страницы [0..1]
	 * @param pageTurnSpeed   скорость перелистывания (масштабирует угол раскрытия)
	 */
	@Environment(EnvType.CLIENT)
	public record BookModelState(
			float pageTurnAmount,
			float leftFlipAmount,
			float rightFlipAmount,
			float pageTurnSpeed
	) {
	}
}

package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Экран редактирования обычной таблички. Отрисовывает 3D-модель таблички
 * с масштабированием {@link #BACKGROUND_SCALE}.
 */
@Environment(EnvType.CLIENT)
public class SignEditScreen extends AbstractSignEditScreen {

	public static final float BACKGROUND_SCALE = 62.500004F;
	public static final float TEXT_SCALE_MULTIPLIER = 0.9765628F;

	private static final Vector3f TEXT_SCALE = new Vector3f(TEXT_SCALE_MULTIPLIER, TEXT_SCALE_MULTIPLIER, TEXT_SCALE_MULTIPLIER);
	private static final float Y_OFFSET = 90.0F;
	private static final int SIGN_LEFT_OFFSET = 48;
	private static final int SIGN_TOP = 66;
	private static final int SIGN_BOTTOM = 168;

	private Model.@Nullable SinglePartModel model;

	public SignEditScreen(SignBlockEntity sign, boolean filtered, boolean front) {
		super(sign, filtered, front);
	}

	@Override
	protected void init() {
		super.init();
		boolean isWallSign = blockEntity.getCachedState().getBlock() instanceof SignBlock;
		model = SignBlockEntityRenderer.createSignModel(client.getLoadedEntityModels(), signType, isWallSign);
	}

	@Override
	protected float getYOffset() {
		return Y_OFFSET;
	}

	@Override
	protected void renderSignBackground(DrawContext context) {
		if (model == null) {
			return;
		}

		int centerX = width / 2;
		context.addSign(model, BACKGROUND_SCALE, signType, centerX - SIGN_LEFT_OFFSET, SIGN_TOP, centerX + SIGN_LEFT_OFFSET, SIGN_BOTTOM);
	}

	@Override
	protected Vector3f getTextScale() {
		return TEXT_SCALE;
	}
}

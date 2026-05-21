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

@Environment(EnvType.CLIENT)
/**
 * {@code SignEditScreen}.
 */
public class SignEditScreen extends AbstractSignEditScreen {

	public static final float BACKGROUND_SCALE = 62.500004F;
	public static final float TEXT_SCALE_MULTIPLIER = 0.9765628F;
	private static final Vector3f TEXT_SCALE = new Vector3f(0.9765628F, 0.9765628F, 0.9765628F);
	private Model.@Nullable SinglePartModel model;

	public SignEditScreen(SignBlockEntity sign, boolean filtered, boolean bl) {
		super(sign, filtered, bl);
	}

	@Override
	protected void init() {
		super.init();
		boolean bl = this.blockEntity.getCachedState().getBlock() instanceof SignBlock;
		this.model = SignBlockEntityRenderer.createSignModel(this.client.getLoadedEntityModels(), this.signType, bl);
	}

	@Override
	protected float getYOffset() {
		return 90.0F;
	}

	@Override
	protected void renderSignBackground(DrawContext context) {
		if (this.model != null) {
			int i = this.width / 2;
			int j = i - 48;
			int k = 66;
			int l = i + 48;
			int m = 168;
			context.addSign(this.model, 62.500004F, this.signType, j, 66, l, 168);
		}
	}

	@Override
	protected Vector3f getTextScale() {
		return TEXT_SCALE;
	}
}

package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
/**
 * {@code PlayerSkinWidget}.
 */
public class PlayerSkinWidget extends ClickableWidget {

	private static final float SCALE = 2.125F;
	private static final float BODY_SCALE = 0.97F;
	private static final float Y_OFFSET = 2.5F;
	private static final float TILT_ANGLE = -5.0F;
	private static final float ROTATION_ANGLE = 30.0F;
	private static final float VIEW_ANGLE = 50.0F;
	private final PlayerEntityModel wideModel;
	private final PlayerEntityModel slimModel;
	private final Supplier<SkinTextures> skinSupplier;
	private float xRotation = -5.0F;
	private float yRotation = ROTATION_ANGLE;

	public PlayerSkinWidget(
			int width,
			int height,
			LoadedEntityModels entityModels,
			Supplier<SkinTextures> skinSupplier
	) {
		super(0, 0, width, height, ScreenTexts.EMPTY);
		this.wideModel = new PlayerEntityModel(entityModels.getModelPart(EntityModelLayers.PLAYER), false);
		this.slimModel = new PlayerEntityModel(entityModels.getModelPart(EntityModelLayers.PLAYER_SLIM), true);
		this.skinSupplier = skinSupplier;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		float f = BODY_SCALE * this.getHeight() / SCALE;
		float g = -1.0625F;
		SkinTextures skinTextures = this.skinSupplier.get();
		PlayerEntityModel
				playerEntityModel =
				skinTextures.model() == PlayerSkinType.SLIM ? this.slimModel : this.wideModel;
		context.addPlayerSkin(
				playerEntityModel,
				skinTextures.body().texturePath(),
				f,
				this.xRotation,
				this.yRotation,
				-1.0625F,
				this.getX(),
				this.getY(),
				this.getRight(),
				this.getBottom()
		);
	}

	@Override
	protected void onDrag(Click click, double offsetX, double offsetY) {
		this.xRotation = MathHelper.clamp(this.xRotation - (float) offsetY * Y_OFFSET, -VIEW_ANGLE, VIEW_ANGLE);
		this.yRotation += (float) offsetX * Y_OFFSET;
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
	}

	@Override
	public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
		return null;
	}
}

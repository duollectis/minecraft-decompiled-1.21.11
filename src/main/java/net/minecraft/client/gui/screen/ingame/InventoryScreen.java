package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenPos;
import net.minecraft.client.gui.screen.recipebook.CraftingRecipeBookWidget;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Экран инвентаря игрока. Отображает 3D-модель персонажа, слоты брони,
 * книгу рецептов и статус-эффекты.
 */
@Environment(EnvType.CLIENT)
public class InventoryScreen extends RecipeBookScreen<PlayerScreenHandler> {

	private static final int TITLE_X = 97;
	private static final int RECIPE_BOOK_BUTTON_X_OFFSET = 104;
	private static final int RECIPE_BOOK_BUTTON_Y_OFFSET = 22;
	private static final int ENTITY_DISPLAY_SIZE = 30;
	private static final float ENTITY_DISPLAY_SCALE = 0.0625F;
	private static final float MOUSE_ROTATION_DIVISOR = 40.0F;
	private static final float PITCH_SCALE = 20.0F;
	private static final float FULL_ROTATION = (float) Math.PI;
	private static final float PITCH_TO_RADIANS = (float) (Math.PI / 180.0);
	private static final int FULL_BRIGHT = 15728880;

	private float mouseX;
	private float mouseY;
	private boolean mouseDown;
	private final StatusEffectsDisplay statusEffectsDisplay;

	public InventoryScreen(PlayerEntity player) {
		super(
			player.playerScreenHandler,
			new CraftingRecipeBookWidget(player.playerScreenHandler),
			player.getInventory(),
			Text.translatable("container.crafting")
		);
		titleX = TITLE_X;
		statusEffectsDisplay = new StatusEffectsDisplay(this);
	}

	@Override
	public void handledScreenTick() {
		super.handledScreenTick();

		if (client.player.isInCreativeMode()) {
			client.setScreen(new CreativeInventoryScreen(
				client.player,
				client.player.networkHandler.getEnabledFeatures(),
				client.options.getOperatorItemsTab().getValue()
			));
		}
	}

	@Override
	protected void init() {
		if (client.player.isInCreativeMode()) {
			client.setScreen(new CreativeInventoryScreen(
				client.player,
				client.player.networkHandler.getEnabledFeatures(),
				client.options.getOperatorItemsTab().getValue()
			));
			return;
		}

		super.init();
	}

	@Override
	protected ScreenPos getRecipeBookButtonPos() {
		return new ScreenPos(x + RECIPE_BOOK_BUTTON_X_OFFSET, height / 2 - RECIPE_BOOK_BUTTON_Y_OFFSET);
	}

	@Override
	protected void onRecipeBookToggled() {
		mouseDown = true;
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawText(textRenderer, title, titleX, titleY, -12566464, false);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		statusEffectsDisplay.render(context, mouseX, mouseY);
		super.render(context, mouseX, mouseY, deltaTicks);
		this.mouseX = mouseX;
		this.mouseY = mouseY;
	}

	@Override
	public boolean showsStatusEffects() {
		return statusEffectsDisplay.shouldHideStatusEffectHud();
	}

	@Override
	protected boolean shouldAddPaddingToGhostResult() {
		return false;
	}

	@Override
	protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			BACKGROUND_TEXTURE,
			x,
			y,
			0.0F,
			0.0F,
			backgroundWidth,
			backgroundHeight,
			256,
			256
		);
		drawEntity(context, x + 26, y + 8, x + 75, y + 78, ENTITY_DISPLAY_SIZE, ENTITY_DISPLAY_SCALE, this.mouseX, this.mouseY, client.player);
	}

	/**
	 * Отрисовывает 3D-модель живого существа в заданной области экрана.
	 * Поворот модели зависит от положения курсора мыши.
	 */
	public static void drawEntity(
		DrawContext context,
		int x1,
		int y1,
		int x2,
		int y2,
		int size,
		float scale,
		float mouseX,
		float mouseY,
		LivingEntity entity
	) {
		float centerX = (x1 + x2) / 2.0F;
		float centerY = (y1 + y2) / 2.0F;
		float yawAngle = (float) Math.atan((centerX - mouseX) / MOUSE_ROTATION_DIVISOR);
		float pitchAngle = (float) Math.atan((centerY - mouseY) / MOUSE_ROTATION_DIVISOR);
		Quaternionf rotation = new Quaternionf().rotateZ(FULL_ROTATION);
		Quaternionf pitchRotation = new Quaternionf().rotateX(pitchAngle * PITCH_SCALE * PITCH_TO_RADIANS);
		rotation.mul(pitchRotation);

		EntityRenderState renderState = prepareEntityRenderState(entity);

		if (renderState instanceof LivingEntityRenderState livingState) {
			livingState.bodyYaw = 180.0F + yawAngle * PITCH_SCALE;
			livingState.relativeHeadYaw = yawAngle * PITCH_SCALE;
			livingState.pitch = livingState.pose != EntityPose.GLIDING ? -pitchAngle * PITCH_SCALE : 0.0F;
			livingState.width = livingState.width / livingState.baseScale;
			livingState.height = livingState.height / livingState.baseScale;
			livingState.baseScale = 1.0F;
		}

		Vector3f offset = new Vector3f(0.0F, renderState.height / 2.0F + scale, 0.0F);
		context.addEntity(renderState, size, offset, rotation, pitchRotation, x1, y1, x2, y2);
	}

	private static EntityRenderState prepareEntityRenderState(LivingEntity entity) {
		EntityRenderManager renderManager = MinecraftClient.getInstance().getEntityRenderDispatcher();
		EntityRenderer<? super LivingEntity, ?> renderer = renderManager.getRenderer(entity);
		EntityRenderState renderState = renderer.getAndUpdateRenderState(entity, 1.0F);
		renderState.light = FULL_BRIGHT;
		renderState.shadowPieces.clear();
		renderState.outlineColor = 0;
		return renderState;
	}

	@Override
	public boolean mouseReleased(Click click) {
		if (mouseDown) {
			mouseDown = false;
			return true;
		}

		return super.mouseReleased(click);
	}
}

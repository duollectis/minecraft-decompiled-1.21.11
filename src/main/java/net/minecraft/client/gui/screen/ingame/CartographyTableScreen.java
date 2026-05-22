package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Экран картографического стола. Отображает предпросмотр карты в зависимости от режима:
 * масштабирование, клонирование, блокировка или обычный просмотр.
 */
@Environment(EnvType.CLIENT)
public class CartographyTableScreen extends HandledScreen<CartographyTableScreenHandler> {

	private static final Identifier ERROR_TEXTURE = Identifier.ofVanilla("container/cartography_table/error");
	private static final Identifier SCALED_MAP_TEXTURE = Identifier.ofVanilla("container/cartography_table/scaled_map");
	private static final Identifier DUPLICATED_MAP_TEXTURE = Identifier.ofVanilla("container/cartography_table/duplicated_map");
	private static final Identifier MAP_TEXTURE = Identifier.ofVanilla("container/cartography_table/map");
	private static final Identifier LOCKED_TEXTURE = Identifier.ofVanilla("container/cartography_table/locked");
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/cartography_table.png");

	private final MapRenderState mapRenderState = new MapRenderState();

	public CartographyTableScreen(CartographyTableScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		titleY -= 2;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			TEXTURE,
			x,
			y,
			0.0F,
			0.0F,
			backgroundWidth,
			backgroundHeight,
			256,
			256
		);

		ItemStack secondSlotStack = handler.getSlot(1).getStack();
		boolean isCloneMode = secondSlotStack.isOf(Items.MAP);
		boolean isExpandMode = secondSlotStack.isOf(Items.PAPER);
		boolean isLockMode = secondSlotStack.isOf(Items.GLASS_PANE);
		ItemStack mapStack = handler.getSlot(0).getStack();
		MapIdComponent mapId = mapStack.get(DataComponentTypes.MAP_ID);
		boolean cannotExpand = false;
		MapState mapState;

		if (mapId != null) {
			mapState = FilledMapItem.getMapState(mapId, client.world);
			if (mapState != null) {
				if (mapState.locked) {
					cannotExpand = true;
					if (isExpandMode || isLockMode) {
						context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ERROR_TEXTURE, x + 35, y + 31, 28, 21);
					}
				}

				if (isExpandMode && mapState.scale >= 4) {
					cannotExpand = true;
					context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ERROR_TEXTURE, x + 35, y + 31, 28, 21);
				}
			}
		} else {
			mapState = null;
		}

		drawMapPreview(context, mapId, mapState, isCloneMode, isExpandMode, isLockMode, cannotExpand);
	}

	private void drawMapPreview(
		DrawContext context,
		@Nullable MapIdComponent mapId,
		@Nullable MapState mapState,
		boolean cloneMode,
		boolean expandMode,
		boolean lockMode,
		boolean cannotExpand
	) {
		if (expandMode && !cannotExpand) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SCALED_MAP_TEXTURE, x + 67, y + 13, 66, 66);
			drawMap(context, mapId, mapState, x + 85, y + 31, 0.226F);
		} else if (cloneMode) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, DUPLICATED_MAP_TEXTURE, x + 67 + 16, y + 13, 50, 66);
			drawMap(context, mapId, mapState, x + 86, y + 16, 0.34F);
			context.createNewRootLayer();
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, DUPLICATED_MAP_TEXTURE, x + 67, y + 13 + 16, 50, 66);
			drawMap(context, mapId, mapState, x + 70, y + 32, 0.34F);
		} else if (lockMode) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, MAP_TEXTURE, x + 67, y + 13, 66, 66);
			drawMap(context, mapId, mapState, x + 71, y + 17, 0.45F);
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, LOCKED_TEXTURE, x + 118, y + 60, 10, 14);
		} else {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, MAP_TEXTURE, x + 67, y + 13, 66, 66);
			drawMap(context, mapId, mapState, x + 71, y + 17, 0.45F);
		}
	}

	private void drawMap(
		DrawContext context,
		@Nullable MapIdComponent mapId,
		@Nullable MapState mapState,
		int drawX,
		int drawY,
		float scale
	) {
		if (mapId == null || mapState == null) {
			return;
		}

		context.getMatrices().pushMatrix();
		context.getMatrices().translate(drawX, drawY);
		context.getMatrices().scale(scale, scale);
		client.getMapRenderer().update(mapId, mapState, mapRenderState);
		context.drawMap(mapRenderState);
		context.getMatrices().popMatrix();
	}
}

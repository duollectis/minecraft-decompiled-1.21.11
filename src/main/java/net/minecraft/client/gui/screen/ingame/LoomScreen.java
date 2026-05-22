package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.block.entity.model.BannerFlagBlockModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.Sprite;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BannerItem;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Экран ткацкого станка. Отображает список узоров для баннера с прокруткой,
 * предпросмотр результата и слоты для баннера, красителя и шаблона.
 */
@Environment(EnvType.CLIENT)
public class LoomScreen extends HandledScreen<LoomScreenHandler> {

	private static final Identifier BANNER_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/banner");
	private static final Identifier DYE_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/dye");
	private static final Identifier PATTERN_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/banner_pattern");
	private static final Identifier SCROLLER_TEXTURE = Identifier.ofVanilla("container/loom/scroller");
	private static final Identifier SCROLLER_DISABLED_TEXTURE = Identifier.ofVanilla("container/loom/scroller_disabled");
	private static final Identifier PATTERN_SELECTED_TEXTURE = Identifier.ofVanilla("container/loom/pattern_selected");
	private static final Identifier PATTERN_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("container/loom/pattern_highlighted");
	private static final Identifier PATTERN_TEXTURE = Identifier.ofVanilla("container/loom/pattern");
	private static final Identifier ERROR_TEXTURE = Identifier.ofVanilla("container/loom/error");
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/loom.png");
	private static final int PATTERN_LIST_COLUMNS = 4;
	private static final int PATTERN_LIST_ROWS = 4;
	private static final int SCROLLBAR_WIDTH = 12;
	private static final int SCROLLBAR_HEIGHT = 15;
	private static final int PATTERN_ENTRY_SIZE = 14;
	private static final int SCROLLBAR_AREA_HEIGHT = 56;
	private static final int PATTERN_LIST_OFFSET_X = 60;
	private static final int PATTERN_LIST_OFFSET_Y = 13;
	private static final float PATTERN_LIST_WIDTH = 64.0F;
	private static final float PATTERN_LIST_HEIGHT = 40.0F;
	private static final int MAX_BANNER_PATTERNS = 6;

	private BannerFlagBlockModel bannerField;
	private @Nullable BannerPatternsComponent bannerPatterns;
	private ItemStack banner = ItemStack.EMPTY;
	private ItemStack dye = ItemStack.EMPTY;
	private ItemStack pattern = ItemStack.EMPTY;
	private boolean canApplyDyePattern;
	private boolean hasTooManyPatterns;
	private float scrollPosition;
	private boolean scrollbarClicked;
	private int visibleTopRow;

	public LoomScreen(LoomScreenHandler screenHandler, PlayerInventory inventory, Text title) {
		super(screenHandler, inventory, title);
		screenHandler.setInventoryChangeListener(this::onInventoryChanged);
		titleY -= 2;
	}

	@Override
	protected void init() {
		super.init();
		ModelPart modelPart = client.getLoadedEntityModels().getModelPart(EntityModelLayers.STANDING_BANNER_FLAG);
		bannerField = new BannerFlagBlockModel(modelPart);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		drawMouseoverTooltip(context, mouseX, mouseY);
	}

	private int getRows() {
		return MathHelper.ceilDiv(handler.getBannerPatterns().size(), PATTERN_LIST_COLUMNS);
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

		Slot bannerSlot = handler.getBannerSlot();
		Slot dyeSlot = handler.getDyeSlot();
		Slot patternSlot = handler.getPatternSlot();
		Slot outputSlot = handler.getOutputSlot();

		if (!bannerSlot.hasStack()) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, BANNER_SLOT_TEXTURE, x + bannerSlot.x, y + bannerSlot.y, 16, 16);
		}

		if (!dyeSlot.hasStack()) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, DYE_SLOT_TEXTURE, x + dyeSlot.x, y + dyeSlot.y, 16, 16);
		}

		if (!patternSlot.hasStack()) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, PATTERN_SLOT_TEXTURE, x + patternSlot.x, y + patternSlot.y, 16, 16);
		}

		int scrollY = (int) (41.0F * scrollPosition);
		Identifier scrollerTexture = canApplyDyePattern ? SCROLLER_TEXTURE : SCROLLER_DISABLED_TEXTURE;
		int scrollX = x + 119;
		int scrollerY = y + PATTERN_LIST_OFFSET_Y + scrollY;
		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, scrollerTexture, scrollX, scrollerY, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);

		if (mouseX >= scrollX && mouseX < scrollX + SCROLLBAR_WIDTH && mouseY >= scrollerY && mouseY < scrollerY + SCROLLBAR_HEIGHT) {
			context.setCursor(scrollbarClicked ? StandardCursors.RESIZE_NS : StandardCursors.POINTING_HAND);
		}

		if (bannerPatterns != null && !hasTooManyPatterns) {
			DyeColor dyeColor = ((BannerItem) outputSlot.getStack().getItem()).getColor();
			int bannerX = x + 141;
			int bannerY = y + 8;
			context.addBannerResult(bannerField, dyeColor, bannerPatterns, bannerX, bannerY, bannerX + 20, bannerY + 40);
		} else if (hasTooManyPatterns) {
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				ERROR_TEXTURE,
				x + outputSlot.x - 5,
				y + outputSlot.y - 5,
				26,
				26
			);
		}

		if (!canApplyDyePattern) {
			MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ITEMS_3D);
			return;
		}

		int listX = x + PATTERN_LIST_OFFSET_X;
		int listY = y + PATTERN_LIST_OFFSET_Y;
		List<RegistryEntry<BannerPattern>> patterns = handler.getBannerPatterns();

		outer:
		for (int row = 0; row < PATTERN_LIST_ROWS; row++) {
			for (int col = 0; col < PATTERN_LIST_COLUMNS; col++) {
				int absoluteRow = row + visibleTopRow;
				int patternIndex = absoluteRow * PATTERN_LIST_COLUMNS + col;
				if (patternIndex >= patterns.size()) {
					break outer;
				}

				int entryX = listX + col * PATTERN_ENTRY_SIZE;
				int entryY = listY + row * PATTERN_ENTRY_SIZE;
				RegistryEntry<BannerPattern> patternEntry = patterns.get(patternIndex);
				boolean isHovered = mouseX >= entryX && mouseY >= entryY
					&& mouseX < entryX + PATTERN_ENTRY_SIZE
					&& mouseY < entryY + PATTERN_ENTRY_SIZE;

				Identifier entryTexture;
				if (patternIndex == handler.getSelectedPattern()) {
					entryTexture = PATTERN_SELECTED_TEXTURE;
				} else if (isHovered) {
					entryTexture = PATTERN_HIGHLIGHTED_TEXTURE;
					DyeColor dyeColor = ((DyeItem) dye.getItem()).getColor();
					context.drawTooltip(
						Text.translatable(patternEntry.value().translationKey() + "." + dyeColor.getId()),
						mouseX,
						mouseY
					);
					context.setCursor(StandardCursors.POINTING_HAND);
				} else {
					entryTexture = PATTERN_TEXTURE;
				}

				context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, entryTexture, entryX, entryY, PATTERN_ENTRY_SIZE, PATTERN_ENTRY_SIZE);
				Sprite sprite = context.getSprite(TexturedRenderLayers.getBannerPatternTextureId(patternEntry));
				drawBanner(context, entryX, entryY, sprite);
			}
		}

		MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ITEMS_3D);
	}

	private void drawBanner(DrawContext context, int drawX, int drawY, Sprite sprite) {
		context.getMatrices().pushMatrix();
		context.getMatrices().translate(drawX + 4, drawY + 2);
		float minU = sprite.getMinU();
		float maxU = minU + (sprite.getMaxU() - minU) * 21.0F / PATTERN_LIST_WIDTH;
		float uRange = sprite.getMaxV() - sprite.getMinV();
		float minV = sprite.getMinV() + uRange / PATTERN_LIST_WIDTH;
		float maxV = minV + uRange * PATTERN_LIST_HEIGHT / PATTERN_LIST_WIDTH;
		context.fill(0, 0, 5, 10, DyeColor.GRAY.getEntityColor());
		context.drawTexturedQuad(sprite.getAtlasId(), 0, 0, 5, 10, minU, maxU, minV, maxV);
		context.getMatrices().popMatrix();
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (canApplyDyePattern) {
			int listX = x + PATTERN_LIST_OFFSET_X;
			int listY = y + PATTERN_LIST_OFFSET_Y;

			outer:
			for (int row = 0; row < PATTERN_LIST_ROWS; row++) {
				for (int col = 0; col < PATTERN_LIST_COLUMNS; col++) {
					int absoluteRow = row + visibleTopRow;
					int patternIndex = absoluteRow * PATTERN_LIST_COLUMNS + col;
					if (patternIndex >= handler.getBannerPatterns().size()) {
						break outer;
					}

					double relX = click.x() - (listX + col * PATTERN_ENTRY_SIZE);
					double relY = click.y() - (listY + row * PATTERN_ENTRY_SIZE);

					if (relX >= 0.0 && relY >= 0.0 && relX < PATTERN_ENTRY_SIZE && relY < PATTERN_ENTRY_SIZE) {
						((net.minecraft.screen.LoomScreenHandler) handler).selectedPattern.set(patternIndex);
						client.interactionManager.clickButton(handler.syncId, patternIndex);
						client.getSoundManager().play(
							PositionedSoundInstance.master(SoundEvents.UI_LOOM_SELECT_PATTERN, 1.0F)
						);
						return true;
					}
				}
			}

			int scrollbarX = x + 119;
			int scrollbarY = y + PATTERN_LIST_OFFSET_Y;
			if (click.x() >= scrollbarX
				&& click.x() < scrollbarX + SCROLLBAR_WIDTH
				&& click.y() >= scrollbarY
				&& click.y() < scrollbarY + SCROLLBAR_AREA_HEIGHT
			) {
				scrollbarClicked = true;
			}
		}

		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (scrollbarClicked && canApplyDyePattern) {
			int topY = y + PATTERN_LIST_OFFSET_Y;
			int bottomY = topY + SCROLLBAR_AREA_HEIGHT;
			scrollPosition = ((float) click.y() - topY - 7.5F) / (bottomY - topY - 15.0F);
			scrollPosition = MathHelper.clamp(scrollPosition, 0.0F, 1.0F);
			visibleTopRow = Math.max((int) (scrollPosition * getMaxScroll() + 0.5F), 0);
			return true;
		}

		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(Click click) {
		scrollbarClicked = false;
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
			return true;
		}

		if (canApplyDyePattern) {
			int maxScroll = getMaxScroll();
			float delta = (float) verticalAmount / maxScroll;
			scrollPosition = MathHelper.clamp(scrollPosition - delta, 0.0F, 1.0F);
			visibleTopRow = Math.max((int) (scrollPosition * maxScroll + 0.5F), 0);
		}

		return true;
	}

	@Override
	protected boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top) {
		return mouseX < left
			|| mouseY < top
			|| mouseX >= left + backgroundWidth
			|| mouseY >= top + backgroundHeight;
	}

	private int getMaxScroll() {
		return Math.max(0, getRows() - PATTERN_LIST_ROWS);
	}

	private void onInventoryChanged() {
		ItemStack newBanner = handler.getBannerSlot().getStack();
		ItemStack newDye = handler.getDyeSlot().getStack();
		ItemStack newPattern = handler.getPatternSlot().getStack();

		boolean bannerChanged = !ItemStack.areItemsEqual(newBanner, banner);
		boolean dyeChanged = !ItemStack.areItemsEqual(newDye, dye);
		boolean patternChanged = !ItemStack.areItemsEqual(newPattern, pattern);

		banner = newBanner;
		dye = newDye;
		pattern = newPattern;

		if (bannerChanged || dyeChanged || patternChanged) {
			canApplyDyePattern = ((net.minecraft.screen.LoomScreenHandler) handler).canApplyDyePattern();
			hasTooManyPatterns = ((net.minecraft.screen.LoomScreenHandler) handler).hasTooManyPatterns();
			ItemStack outputStack = handler.getOutputSlot().getStack();
			bannerPatterns = outputStack.get(DataComponentTypes.BANNER_PATTERNS);

			if (!canApplyDyePattern) {
				scrollPosition = 0.0F;
				visibleTopRow = 0;
			}
		}
	}
}

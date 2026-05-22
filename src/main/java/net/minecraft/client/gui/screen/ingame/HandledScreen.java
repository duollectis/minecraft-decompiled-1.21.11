package net.minecraft.client.gui.screen.ingame;

import com.google.common.collect.Sets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.BundleTooltipSubmenuHandler;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipSubmenuHandler;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Базовый абстрактный экран для всех контейнеров (инвентарей) игры.
 * Управляет отрисовкой слотов, перетаскиванием предметов мышью и тачскрином,
 * а также обработкой горячих клавиш хотбара.
 */
@Environment(EnvType.CLIENT)
public abstract class HandledScreen<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {

	public static final Identifier BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/container/inventory.png");
	private static final Identifier SLOT_HIGHLIGHT_BACK_TEXTURE = Identifier.ofVanilla("container/slot_highlight_back");
	private static final Identifier SLOT_HIGHLIGHT_FRONT_TEXTURE = Identifier.ofVanilla("container/slot_highlight_front");
	protected static final int BACKGROUND_TEXTURE_SIZE = 256;
	private static final float LET_GO_ANIMATION_DURATION_MS = 100.0F;
	private static final int TOUCH_DROP_DELAY_MS = 500;
	private static final int TITLE_COLOR = -12566464;
	private static final int HOTBAR_SIZE = 9;
	private static final int SWAP_HAND_BUTTON = 40;
	private static final int SLOT_ID_OUTSIDE = -999;

	protected int backgroundWidth = 176;
	protected int backgroundHeight = 166;
	protected int titleX;
	protected int titleY;
	protected int playerInventoryTitleX;
	protected int playerInventoryTitleY;
	private final List<TooltipSubmenuHandler> tooltipSubmenuHandlers;
	protected final T handler;
	protected final Text playerInventoryTitle;
	protected @Nullable Slot focusedSlot;
	private @Nullable Slot touchDragSlotStart;
	private @Nullable Slot touchHoveredSlot;
	private @Nullable Slot lastClickedSlot;
	private HandledScreen.@Nullable LetGoTouchStack letGoTouchStack;
	protected int x;
	protected int y;
	private boolean touchIsRightClickDrag;
	private ItemStack touchDragStack = ItemStack.EMPTY;
	private long touchDropTimer;
	protected final Set<Slot> cursorDragSlots = Sets.newHashSet();
	protected boolean cursorDragging;
	private int heldButtonType;
	@MouseInput.ButtonCode
	private int heldButtonCode;
	private boolean cancelNextRelease;
	private int draggedStackRemainder;
	private boolean doubleClicking;
	private ItemStack quickMovingStack = ItemStack.EMPTY;

	public HandledScreen(T handler, PlayerInventory inventory, Text title) {
		super(title);
		this.handler = handler;
		playerInventoryTitle = inventory.getDisplayName();
		cancelNextRelease = true;
		titleX = 8;
		titleY = 6;
		playerInventoryTitleX = 8;
		playerInventoryTitleY = backgroundHeight - 94;
		tooltipSubmenuHandlers = new ArrayList<>();
	}

	@Override
	protected void init() {
		x = (width - backgroundWidth) / 2;
		y = (height - backgroundHeight) / 2;
		tooltipSubmenuHandlers.clear();
		addTooltipSubmenuHandler(new BundleTooltipSubmenuHandler(client));
	}

	protected void addTooltipSubmenuHandler(TooltipSubmenuHandler handler) {
		tooltipSubmenuHandlers.add(handler);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		renderMain(context, mouseX, mouseY, deltaTicks);
		renderCursorStack(context, mouseX, mouseY);
		renderLetGoTouchStack(context);
	}

	public void renderMain(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		int bgX = x;
		int bgY = y;
		super.render(context, mouseX, mouseY, deltaTicks);
		context.getMatrices().pushMatrix();
		context.getMatrices().translate(bgX, bgY);
		drawForeground(context, mouseX, mouseY);
		Slot previousFocused = focusedSlot;
		focusedSlot = getSlotAt(mouseX, mouseY);
		drawSlotHighlightBack(context);
		drawSlots(context, mouseX, mouseY);
		drawSlotHighlightFront(context);
		if (previousFocused != null && previousFocused != focusedSlot) {
			resetTooltipSubmenus(previousFocused);
		}

		context.getMatrices().popMatrix();
	}

	public void renderCursorStack(DrawContext context, int mouseX, int mouseY) {
		ItemStack displayStack = touchDragStack.isEmpty() ? handler.getCursorStack() : touchDragStack;
		if (displayStack.isEmpty()) {
			return;
		}

		int yOffset = 8;
		int stackYOffset = touchDragStack.isEmpty() ? 8 : 16;
		String amountLabel = null;

		if (!touchDragStack.isEmpty() && touchIsRightClickDrag) {
			displayStack = displayStack.copyWithCount(MathHelper.ceil(displayStack.getCount() / 2.0F));
		} else if (cursorDragging && cursorDragSlots.size() > 1) {
			displayStack = displayStack.copyWithCount(draggedStackRemainder);
			if (displayStack.isEmpty()) {
				amountLabel = Formatting.YELLOW + "0";
			}
		}

		context.createNewRootLayer();
		drawItem(context, displayStack, mouseX - yOffset, mouseY - stackYOffset, amountLabel);
	}

	public void renderLetGoTouchStack(DrawContext context) {
		if (letGoTouchStack == null) {
			return;
		}

		float progress = MathHelper.clamp(
			(float) (Util.getMeasuringTimeMs() - letGoTouchStack.time) / LET_GO_ANIMATION_DURATION_MS,
			0.0F,
			1.0F
		);
		int deltaX = letGoTouchStack.end.x - letGoTouchStack.start.x;
		int deltaY = letGoTouchStack.end.y - letGoTouchStack.start.y;
		int currentX = letGoTouchStack.start.x + (int) (deltaX * progress);
		int currentY = letGoTouchStack.start.y + (int) (deltaY * progress);
		context.createNewRootLayer();
		drawItem(context, letGoTouchStack.item, currentX, currentY, null);

		if (progress >= 1.0F) {
			letGoTouchStack = null;
		}
	}

	protected void drawSlots(DrawContext context, int mouseX, int mouseY) {
		for (Slot slot : handler.slots) {
			if (slot.isEnabled()) {
				drawSlot(context, slot, mouseX, mouseY);
			}
		}
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.renderBackground(context, mouseX, mouseY, deltaTicks);
		drawBackground(context, deltaTicks, mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (focusedSlot == null || !focusedSlot.hasStack()) {
			return false;
		}

		for (TooltipSubmenuHandler submenuHandler : tooltipSubmenuHandlers) {
			if (submenuHandler.isApplicableTo(focusedSlot)
				&& submenuHandler.onScroll(
					horizontalAmount,
					verticalAmount,
					focusedSlot.id,
					focusedSlot.getStack()
				)
			) {
				return true;
			}
		}

		return false;
	}

	private void drawSlotHighlightBack(DrawContext context) {
		if (focusedSlot != null && focusedSlot.canBeHighlighted()) {
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				SLOT_HIGHLIGHT_BACK_TEXTURE,
				focusedSlot.x - 4,
				focusedSlot.y - 4,
				24,
				24
			);
		}
	}

	private void drawSlotHighlightFront(DrawContext context) {
		if (focusedSlot != null && focusedSlot.canBeHighlighted()) {
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				SLOT_HIGHLIGHT_FRONT_TEXTURE,
				focusedSlot.x - 4,
				focusedSlot.y - 4,
				24,
				24
			);
		}
	}

	protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
		if (focusedSlot == null || !focusedSlot.hasStack()) {
			return;
		}

		ItemStack itemStack = focusedSlot.getStack();
		if (handler.getCursorStack().isEmpty() || isItemTooltipSticky(itemStack)) {
			context.drawTooltip(
				textRenderer,
				getTooltipFromItem(itemStack),
				itemStack.getTooltipData(),
				mouseX,
				mouseY,
				itemStack.get(DataComponentTypes.TOOLTIP_STYLE)
			);
		}
	}

	private boolean isItemTooltipSticky(ItemStack item) {
		return item.getTooltipData().map(TooltipComponent::of).map(TooltipComponent::isSticky).orElse(false);
	}

	protected List<Text> getTooltipFromItem(ItemStack stack) {
		return getTooltipFromItem(client, stack);
	}

	private void drawItem(DrawContext context, ItemStack stack, int drawX, int drawY, @Nullable String amountText) {
		context.drawItem(stack, drawX, drawY);
		context.drawStackOverlay(textRenderer, stack, drawX, drawY - (touchDragStack.isEmpty() ? 0 : 8), amountText);
	}

	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawText(textRenderer, title, titleX, titleY, TITLE_COLOR, false);
		context.drawText(textRenderer, playerInventoryTitle, playerInventoryTitleX, playerInventoryTitleY, TITLE_COLOR, false);
	}

	protected abstract void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY);

	/**
	 * Отрисовывает один слот контейнера с учётом состояния перетаскивания курсором и тачскрином.
	 * Обрабатывает частичное заполнение слота при drag-операции и отображение фоновой иконки пустого слота.
	 */
	protected void drawSlot(DrawContext context, Slot slot, int mouseX, int mouseY) {
		int slotX = slot.x;
		int slotY = slot.y;
		ItemStack slotStack = slot.getStack();
		boolean highlighted = false;
		boolean isDragSource = slot == touchDragSlotStart && !touchDragStack.isEmpty() && !touchIsRightClickDrag;
		ItemStack cursorStack = handler.getCursorStack();
		String amountLabel = null;

		if (slot == touchDragSlotStart && !touchDragStack.isEmpty() && touchIsRightClickDrag && !slotStack.isEmpty()) {
			slotStack = slotStack.copyWithCount(slotStack.getCount() / 2);
		} else if (cursorDragging && cursorDragSlots.contains(slot) && !cursorStack.isEmpty()) {
			if (cursorDragSlots.size() == 1) {
				return;
			}

			if (ScreenHandler.canInsertItemIntoSlot(slot, cursorStack, true) && handler.canInsertIntoSlot(slot)) {
				highlighted = true;
				int maxCount = Math.min(cursorStack.getMaxCount(), slot.getMaxItemCount(cursorStack));
				int existingCount = slot.getStack().isEmpty() ? 0 : slot.getStack().getCount();
				int newCount = ScreenHandler.calculateStackSize(cursorDragSlots, heldButtonType, cursorStack) + existingCount;

				if (newCount > maxCount) {
					newCount = maxCount;
					amountLabel = Formatting.YELLOW.toString() + maxCount;
				}

				slotStack = cursorStack.copyWithCount(newCount);
			} else {
				cursorDragSlots.remove(slot);
				calculateOffset();
			}
		}

		if (slotStack.isEmpty() && slot.isEnabled()) {
			Identifier backgroundSprite = slot.getBackgroundSprite();
			if (backgroundSprite != null) {
				context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, backgroundSprite, slotX, slotY, 16, 16);
				isDragSource = true;
			}
		}

		if (isDragSource) {
			return;
		}

		if (highlighted) {
			context.fill(slotX, slotY, slotX + 16, slotY + 16, -2130706433);
		}

		int slotSeed = slot.x + slot.y * backgroundWidth;
		if (slot.disablesDynamicDisplay()) {
			context.drawItemWithoutEntity(slotStack, slotX, slotY, slotSeed);
		} else {
			context.drawItem(slotStack, slotX, slotY, slotSeed);
		}

		context.drawStackOverlay(textRenderer, slotStack, slotX, slotY, amountLabel);
	}

	private void calculateOffset() {
		ItemStack cursorStack = handler.getCursorStack();
		if (cursorStack.isEmpty() || !cursorDragging) {
			return;
		}

		if (heldButtonType == 2) {
			draggedStackRemainder = cursorStack.getMaxCount();
			return;
		}

		draggedStackRemainder = cursorStack.getCount();

		for (Slot slot : cursorDragSlots) {
			ItemStack slotStack = slot.getStack();
			int existingCount = slotStack.isEmpty() ? 0 : slotStack.getCount();
			int maxAllowed = Math.min(cursorStack.getMaxCount(), slot.getMaxItemCount(cursorStack));
			int newCount = Math.min(
				ScreenHandler.calculateStackSize(cursorDragSlots, heldButtonType, cursorStack) + existingCount,
				maxAllowed
			);
			draggedStackRemainder -= newCount - existingCount;
		}
	}

	private @Nullable Slot getSlotAt(double mouseX, double mouseY) {
		for (Slot slot : handler.slots) {
			if (slot.isEnabled() && isPointOverSlot(slot, mouseX, mouseY)) {
				return slot;
			}
		}

		return null;
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (super.mouseClicked(click, doubled)) {
			return true;
		}

		boolean isPickItem = client.options.pickItemKey.matchesMouse(click) && client.player.isInCreativeMode();
		Slot slot = getSlotAt(click.x(), click.y());
		doubleClicking = lastClickedSlot == slot && doubled;
		cancelNextRelease = false;

		if (click.button() != 0 && click.button() != 1 && !isPickItem) {
			onMouseClick(click);
		} else {
			boolean isOutside = isClickOutsideBounds(click.x(), click.y(), x, y);
			int slotId = slot != null ? slot.id : -1;

			if (isOutside) {
				slotId = SLOT_ID_OUTSIDE;
			}

			if (client.options.getTouchscreen().getValue() && isOutside && handler.getCursorStack().isEmpty()) {
				close();
				return true;
			}

			if (slotId != -1) {
				if (client.options.getTouchscreen().getValue()) {
					if (slot != null && slot.hasStack()) {
						touchDragSlotStart = slot;
						touchDragStack = ItemStack.EMPTY;
						touchIsRightClickDrag = click.button() == 1;
					} else {
						touchDragSlotStart = null;
					}
				} else if (!cursorDragging) {
					if (handler.getCursorStack().isEmpty()) {
						if (isPickItem) {
							onMouseClick(slot, slotId, click.button(), SlotActionType.CLONE);
						} else {
							boolean isQuickMove = slotId != SLOT_ID_OUTSIDE && click.hasShift();
							SlotActionType actionType = SlotActionType.PICKUP;

							if (isQuickMove) {
								quickMovingStack = slot != null && slot.hasStack()
									? slot.getStack().copy()
									: ItemStack.EMPTY;
								actionType = SlotActionType.QUICK_MOVE;
							} else if (slotId == SLOT_ID_OUTSIDE) {
								actionType = SlotActionType.THROW;
							}

							onMouseClick(slot, slotId, click.button(), actionType);
						}

						cancelNextRelease = true;
					} else {
						cursorDragging = true;
						heldButtonCode = click.button();
						cursorDragSlots.clear();

						if (click.button() == 0) {
							heldButtonType = 0;
						} else if (click.button() == 1) {
							heldButtonType = 1;
						} else if (isPickItem) {
							heldButtonType = 2;
						}
					}
				}
			}
		}

		lastClickedSlot = slot;
		return true;
	}

	private void onMouseClick(Click click) {
		if (focusedSlot == null || !handler.getCursorStack().isEmpty()) {
			return;
		}

		if (client.options.swapHandsKey.matchesMouse(click)) {
			onMouseClick(focusedSlot, focusedSlot.id, SWAP_HAND_BUTTON, SlotActionType.SWAP);
			return;
		}

		for (int hotbarIndex = 0; hotbarIndex < HOTBAR_SIZE; hotbarIndex++) {
			if (client.options.hotbarKeys[hotbarIndex].matchesMouse(click)) {
				onMouseClick(focusedSlot, focusedSlot.id, hotbarIndex, SlotActionType.SWAP);
			}
		}
	}

	protected boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top) {
		return mouseX < left
			|| mouseY < top
			|| mouseX >= left + backgroundWidth
			|| mouseY >= top + backgroundHeight;
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		Slot slot = getSlotAt(click.x(), click.y());
		ItemStack cursorStack = handler.getCursorStack();

		if (touchDragSlotStart != null && client.options.getTouchscreen().getValue()) {
			if (click.button() == 0 || click.button() == 1) {
				if (touchDragStack.isEmpty()) {
					if (slot != touchDragSlotStart && !touchDragSlotStart.getStack().isEmpty()) {
						touchDragStack = touchDragSlotStart.getStack().copy();
					}
				} else if (touchDragStack.getCount() > 1
					&& slot != null
					&& ScreenHandler.canInsertItemIntoSlot(slot, touchDragStack, false)
				) {
					long now = Util.getMeasuringTimeMs();
					if (touchHoveredSlot == slot) {
						if (now - touchDropTimer > TOUCH_DROP_DELAY_MS) {
							onMouseClick(touchDragSlotStart, touchDragSlotStart.id, 0, SlotActionType.PICKUP);
							onMouseClick(slot, slot.id, 1, SlotActionType.PICKUP);
							onMouseClick(touchDragSlotStart, touchDragSlotStart.id, 0, SlotActionType.PICKUP);
							touchDropTimer = now + 750L;
							touchDragStack.decrement(1);
						}
					} else {
						touchHoveredSlot = slot;
						touchDropTimer = now;
					}
				}
			}

			return true;
		}

		if (cursorDragging
			&& slot != null
			&& !cursorStack.isEmpty()
			&& (cursorStack.getCount() > cursorDragSlots.size() || heldButtonType == 2)
			&& ScreenHandler.canInsertItemIntoSlot(slot, cursorStack, true)
			&& slot.canInsert(cursorStack)
			&& handler.canInsertIntoSlot(slot)
		) {
			cursorDragSlots.add(slot);
			calculateOffset();
			return true;
		}

		return slot == null && handler.getCursorStack().isEmpty()
			? super.mouseDragged(click, offsetX, offsetY)
			: true;
	}

	@Override
	public boolean mouseReleased(Click click) {
		Slot slot = getSlotAt(click.x(), click.y());
		boolean isOutside = isClickOutsideBounds(click.x(), click.y(), x, y);
		int slotId = slot != null ? slot.id : -1;

		if (isOutside) {
			slotId = SLOT_ID_OUTSIDE;
		}

		if (doubleClicking && slot != null && click.button() == 0
			&& handler.canInsertIntoSlot(ItemStack.EMPTY, slot)
		) {
			if (click.hasShift()) {
				if (!quickMovingStack.isEmpty()) {
					for (Slot otherSlot : handler.slots) {
						if (otherSlot != null
							&& otherSlot.canTakeItems(client.player)
							&& otherSlot.hasStack()
							&& otherSlot.inventory == slot.inventory
							&& ScreenHandler.canInsertItemIntoSlot(otherSlot, quickMovingStack, true)
						) {
							onMouseClick(otherSlot, otherSlot.id, click.button(), SlotActionType.QUICK_MOVE);
						}
					}
				}
			} else {
				onMouseClick(slot, slotId, click.button(), SlotActionType.PICKUP_ALL);
			}

			doubleClicking = false;
		} else {
			if (cursorDragging && heldButtonCode != click.button()) {
				cursorDragging = false;
				cursorDragSlots.clear();
				cancelNextRelease = true;
				return true;
			}

			if (cancelNextRelease) {
				cancelNextRelease = false;
				return true;
			}

			if (touchDragSlotStart != null && client.options.getTouchscreen().getValue()) {
				if (click.button() == 0 || click.button() == 1) {
					if (touchDragStack.isEmpty() && slot != touchDragSlotStart) {
						touchDragStack = touchDragSlotStart.getStack();
					}

					boolean canInsert = ScreenHandler.canInsertItemIntoSlot(slot, touchDragStack, false);
					if (slotId != -1 && !touchDragStack.isEmpty() && canInsert) {
						onMouseClick(touchDragSlotStart, touchDragSlotStart.id, click.button(), SlotActionType.PICKUP);
						onMouseClick(slot, slotId, 0, SlotActionType.PICKUP);

						if (handler.getCursorStack().isEmpty()) {
							letGoTouchStack = null;
						} else {
							onMouseClick(touchDragSlotStart, touchDragSlotStart.id, click.button(), SlotActionType.PICKUP);
							letGoTouchStack = new HandledScreen.LetGoTouchStack(
								touchDragStack,
								new Vector2i((int) click.x(), (int) click.y()),
								new Vector2i(touchDragSlotStart.x + x, touchDragSlotStart.y + y),
								Util.getMeasuringTimeMs()
							);
						}
					} else if (!touchDragStack.isEmpty()) {
						letGoTouchStack = new HandledScreen.LetGoTouchStack(
							touchDragStack,
							new Vector2i((int) click.x(), (int) click.y()),
							new Vector2i(touchDragSlotStart.x + x, touchDragSlotStart.y + y),
							Util.getMeasuringTimeMs()
						);
					}

					endTouchDrag();
				}
			} else if (cursorDragging && !cursorDragSlots.isEmpty()) {
				onMouseClick(null, SLOT_ID_OUTSIDE, ScreenHandler.packQuickCraftData(0, heldButtonType), SlotActionType.QUICK_CRAFT);

				for (Slot dragSlot : cursorDragSlots) {
					onMouseClick(
						dragSlot,
						dragSlot.id,
						ScreenHandler.packQuickCraftData(1, heldButtonType),
						SlotActionType.QUICK_CRAFT
					);
				}

				onMouseClick(null, SLOT_ID_OUTSIDE, ScreenHandler.packQuickCraftData(2, heldButtonType), SlotActionType.QUICK_CRAFT);
			} else if (!handler.getCursorStack().isEmpty()) {
				if (client.options.pickItemKey.matchesMouse(click)) {
					onMouseClick(slot, slotId, click.button(), SlotActionType.CLONE);
				} else {
					boolean isQuickMove = slotId != SLOT_ID_OUTSIDE && click.hasShift();
					if (isQuickMove) {
						quickMovingStack = slot != null && slot.hasStack()
							? slot.getStack().copy()
							: ItemStack.EMPTY;
					}

					onMouseClick(slot, slotId, click.button(), isQuickMove ? SlotActionType.QUICK_MOVE : SlotActionType.PICKUP);
				}
			}
		}

		cursorDragging = false;
		return true;
	}

	public void endTouchDrag() {
		touchDragStack = ItemStack.EMPTY;
		touchDragSlotStart = null;
	}

	private boolean isPointOverSlot(Slot slot, double pointX, double pointY) {
		return isPointWithinBounds(slot.x, slot.y, 16, 16, pointX, pointY);
	}

	protected boolean isPointWithinBounds(int boundsX, int boundsY, int width, int height, double pointX, double pointY) {
		double relativeX = pointX - x;
		double relativeY = pointY - y;
		return relativeX >= boundsX - 1
			&& relativeX < boundsX + width + 1
			&& relativeY >= boundsY - 1
			&& relativeY < boundsY + height + 1;
	}

	private void resetTooltipSubmenus(Slot slot) {
		if (!slot.hasStack()) {
			return;
		}

		for (TooltipSubmenuHandler submenuHandler : tooltipSubmenuHandlers) {
			if (submenuHandler.isApplicableTo(slot)) {
				submenuHandler.reset(slot);
			}
		}
	}

	protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
		if (slot != null) {
			slotId = slot.id;
		}

		onMouseClick(slot, actionType);
		client.interactionManager.clickSlot(handler.syncId, slotId, button, actionType, client.player);
	}

	void onMouseClick(@Nullable Slot slot, SlotActionType actionType) {
		if (slot == null || !slot.hasStack()) {
			return;
		}

		for (TooltipSubmenuHandler submenuHandler : tooltipSubmenuHandlers) {
			if (submenuHandler.isApplicableTo(slot)) {
				submenuHandler.onMouseClick(slot, actionType);
			}
		}
	}

	protected void onSlotChangedState(int slotId, int handlerId, boolean newState) {
		client.interactionManager.slotChangedState(slotId, handlerId, newState);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (super.keyPressed(input)) {
			return true;
		}

		if (client.options.inventoryKey.matchesKey(input)) {
			close();
			return true;
		}

		handleHotbarKeyPressed(input);

		if (focusedSlot != null && focusedSlot.hasStack()) {
			if (client.options.pickItemKey.matchesKey(input)) {
				onMouseClick(focusedSlot, focusedSlot.id, 0, SlotActionType.CLONE);
			} else if (client.options.dropKey.matchesKey(input)) {
				onMouseClick(
					focusedSlot,
					focusedSlot.id,
					input.hasCtrl() ? 1 : 0,
					SlotActionType.THROW
				);
			}
		}

		return false;
	}

	protected boolean handleHotbarKeyPressed(KeyInput keyInput) {
		if (!handler.getCursorStack().isEmpty() || focusedSlot == null) {
			return false;
		}

		if (client.options.swapHandsKey.matchesKey(keyInput)) {
			onMouseClick(focusedSlot, focusedSlot.id, SWAP_HAND_BUTTON, SlotActionType.SWAP);
			return true;
		}

		for (int hotbarIndex = 0; hotbarIndex < HOTBAR_SIZE; hotbarIndex++) {
			if (client.options.hotbarKeys[hotbarIndex].matchesKey(keyInput)) {
				onMouseClick(focusedSlot, focusedSlot.id, hotbarIndex, SlotActionType.SWAP);
				return true;
			}
		}

		return false;
	}

	@Override
	public void removed() {
		if (client.player != null) {
			handler.onClosed(client.player);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean deferSubtitles() {
		return true;
	}

	@Override
	public final void tick() {
		super.tick();
		if (client.player.isAlive() && !client.player.isRemoved()) {
			handledScreenTick();
		} else {
			client.player.closeHandledScreen();
		}
	}

	protected void handledScreenTick() {
	}

	@Override
	public T getScreenHandler() {
		return handler;
	}

	@Override
	public void close() {
		client.player.closeHandledScreen();
		if (focusedSlot != null) {
			resetTooltipSubmenus(focusedSlot);
		}

		super.close();
	}

	/**
	 * {@code LetGoTouchStack} — данные анимации возврата предмета при отпускании тачскрина.
	 */
	@Environment(EnvType.CLIENT)
	record LetGoTouchStack(ItemStack item, Vector2i start, Vector2i end, long time) {
	}
}

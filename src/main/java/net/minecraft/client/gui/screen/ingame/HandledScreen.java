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

@Environment(EnvType.CLIENT)
/**
 * {@code HandledScreen}.
 */
public abstract class HandledScreen<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {

	public static final Identifier BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/container/inventory.png");
	private static final Identifier SLOT_HIGHLIGHT_BACK_TEXTURE = Identifier.ofVanilla("container/slot_highlight_back");
	private static final Identifier
			SLOT_HIGHLIGHT_FRONT_TEXTURE =
			Identifier.ofVanilla("container/slot_highlight_front");
	protected static final int BACKGROUND_TEXTURE_SIZE = 256;
	protected static final int BACKGROUND_TEXTURE_SIZE_ALT = 256;
	private static final float LET_GO_ANIMATION_DURATION_MS = 100.0F;
	private static final int TOUCH_DROP_DELAY_MS = 500;
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
		this.playerInventoryTitle = inventory.getDisplayName();
		this.cancelNextRelease = true;
		this.titleX = 8;
		this.titleY = 6;
		this.playerInventoryTitleX = 8;
		this.playerInventoryTitleY = this.backgroundHeight - 94;
		this.tooltipSubmenuHandlers = new ArrayList<>();
	}

	@Override
	protected void init() {
		this.x = (this.width - this.backgroundWidth) / 2;
		this.y = (this.height - this.backgroundHeight) / 2;
		this.tooltipSubmenuHandlers.clear();
		this.addTooltipSubmenuHandler(new BundleTooltipSubmenuHandler(this.client));
	}

	/**
	 * Добавляет tooltip submenu handler.
	 *
	 * @param handler handler
	 */
	protected void addTooltipSubmenuHandler(TooltipSubmenuHandler handler) {
		this.tooltipSubmenuHandlers.add(handler);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		this.renderMain(context, mouseX, mouseY, deltaTicks);
		this.renderCursorStack(context, mouseX, mouseY);
		this.renderLetGoTouchStack(context);
	}

	/**
	 * Отрисовывает main.
	 *
	 * @param context context
	 * @param mouseX mouse x
	 * @param mouseY mouse y
	 * @param deltaTicks delta ticks
	 */
	public void renderMain(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		int i = this.x;
		int j = this.y;
		super.render(context, mouseX, mouseY, deltaTicks);
		context.getMatrices().pushMatrix();
		context.getMatrices().translate(i, j);
		this.drawForeground(context, mouseX, mouseY);
		Slot slot = this.focusedSlot;
		this.focusedSlot = this.getSlotAt(mouseX, mouseY);
		this.drawSlotHighlightBack(context);
		this.drawSlots(context, mouseX, mouseY);
		this.drawSlotHighlightFront(context);
		if (slot != null && slot != this.focusedSlot) {
			this.resetTooltipSubmenus(slot);
		}

		context.getMatrices().popMatrix();
	}

	/**
	 * Отрисовывает cursor stack.
	 *
	 * @param context context
	 * @param mouseX mouse x
	 * @param mouseY mouse y
	 */
	public void renderCursorStack(DrawContext context, int mouseX, int mouseY) {
		ItemStack itemStack = this.touchDragStack.isEmpty() ? this.handler.getCursorStack() : this.touchDragStack;
		if (!itemStack.isEmpty()) {
			int i = 8;
			int j = this.touchDragStack.isEmpty() ? 8 : 16;
			String string = null;
			if (!this.touchDragStack.isEmpty() && this.touchIsRightClickDrag) {
				itemStack = itemStack.copyWithCount(MathHelper.ceil(itemStack.getCount() / 2.0F));
			}
			else if (this.cursorDragging && this.cursorDragSlots.size() > 1) {
				itemStack = itemStack.copyWithCount(this.draggedStackRemainder);
				if (itemStack.isEmpty()) {
					string = Formatting.YELLOW + "0";
				}
			}

			context.createNewRootLayer();
			this.drawItem(context, itemStack, mouseX - 8, mouseY - j, string);
		}
	}

	/**
	 * Отрисовывает let go touch stack.
	 *
	 * @param context context
	 */
	public void renderLetGoTouchStack(DrawContext context) {
		if (this.letGoTouchStack != null) {
			float
					f =
					MathHelper.clamp(
							(float) (Util.getMeasuringTimeMs() - this.letGoTouchStack.time) / 100.0F,
							0.0F,
							1.0F
					);
			int i = this.letGoTouchStack.end.x - this.letGoTouchStack.start.x;
			int j = this.letGoTouchStack.end.y - this.letGoTouchStack.start.y;
			int k = this.letGoTouchStack.start.x + (int) (i * f);
			int l = this.letGoTouchStack.start.y + (int) (j * f);
			context.createNewRootLayer();
			this.drawItem(context, this.letGoTouchStack.item, k, l, null);
			if (f >= 1.0F) {
				this.letGoTouchStack = null;
			}
		}
	}

	/**
	 * Draw slots.
	 *
	 * @param context context
	 * @param mouseX mouse x
	 * @param mouseY mouse y
	 */
	protected void drawSlots(DrawContext context, int mouseX, int mouseY) {
		for (Slot slot : this.handler.slots) {
			if (slot.isEnabled()) {
				this.drawSlot(context, slot, mouseX, mouseY);
			}
		}
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.renderBackground(context, mouseX, mouseY, deltaTicks);
		this.drawBackground(context, deltaTicks, mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
			for (TooltipSubmenuHandler tooltipSubmenuHandler : this.tooltipSubmenuHandlers) {
				if (tooltipSubmenuHandler.isApplicableTo(this.focusedSlot)
						&& tooltipSubmenuHandler.onScroll(
						horizontalAmount,
						verticalAmount,
						this.focusedSlot.id,
						this.focusedSlot.getStack()
				)) {
					return true;
				}
			}
		}

		return false;
	}

	private void drawSlotHighlightBack(DrawContext context) {
		if (this.focusedSlot != null && this.focusedSlot.canBeHighlighted()) {
			context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					SLOT_HIGHLIGHT_BACK_TEXTURE,
					this.focusedSlot.x - 4,
					this.focusedSlot.y - 4,
					24,
					24
			);
		}
	}

	private void drawSlotHighlightFront(DrawContext context) {
		if (this.focusedSlot != null && this.focusedSlot.canBeHighlighted()) {
			context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					SLOT_HIGHLIGHT_FRONT_TEXTURE,
					this.focusedSlot.x - 4,
					this.focusedSlot.y - 4,
					24,
					24
			);
		}
	}

	/**
	 * Draw mouseover tooltip.
	 *
	 * @param context context
	 * @param x x
	 * @param y y
	 */
	protected void drawMouseoverTooltip(DrawContext context, int x, int y) {
		if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
			ItemStack itemStack = this.focusedSlot.getStack();
			if (this.handler.getCursorStack().isEmpty() || this.isItemTooltipSticky(itemStack)) {
				context.drawTooltip(
						this.textRenderer,
						this.getTooltipFromItem(itemStack),
						itemStack.getTooltipData(),
						x,
						y,
						itemStack.get(DataComponentTypes.TOOLTIP_STYLE)
				);
			}
		}
	}

	private boolean isItemTooltipSticky(ItemStack item) {
		return item.getTooltipData().map(TooltipComponent::of).map(TooltipComponent::isSticky).orElse(false);
	}

	protected List<Text> getTooltipFromItem(ItemStack stack) {
		return getTooltipFromItem(this.client, stack);
	}

	private void drawItem(DrawContext context, ItemStack stack, int x, int y, @Nullable String amountText) {
		context.drawItem(stack, x, y);
		context.drawStackOverlay(this.textRenderer, stack, x, y - (this.touchDragStack.isEmpty() ? 0 : 8), amountText);
	}

	/**
	 * Draw foreground.
	 *
	 * @param context context
	 * @param mouseX mouse x
	 * @param mouseY mouse y
	 */
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, -12566464, false);
		context.drawText(
				this.textRenderer,
				this.playerInventoryTitle,
				this.playerInventoryTitleX,
				this.playerInventoryTitleY,
				-12566464,
				false
		);
	}

	/**
	 * Draw background.
	 *
	 * @param context context
	 * @param deltaTicks delta ticks
	 * @param mouseX mouse x
	 * @param mouseY mouse y
	 */
	protected abstract void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY);

	/**
	 * Draw slot.
	 *
	 * @param context context
	 * @param slot slot
	 * @param mouseX mouse x
	 * @param mouseY mouse y
	 */
	protected void drawSlot(DrawContext context, Slot slot, int mouseX, int mouseY) {
		int i = slot.x;
		int j = slot.y;
		ItemStack itemStack = slot.getStack();
		boolean bl = false;
		boolean bl2 = slot == this.touchDragSlotStart && !this.touchDragStack.isEmpty() && !this.touchIsRightClickDrag;
		ItemStack itemStack2 = this.handler.getCursorStack();
		String string = null;
		if (slot == this.touchDragSlotStart && !this.touchDragStack.isEmpty() && this.touchIsRightClickDrag
				&& !itemStack.isEmpty()) {
			itemStack = itemStack.copyWithCount(itemStack.getCount() / 2);
		}
		else if (this.cursorDragging && this.cursorDragSlots.contains(slot) && !itemStack2.isEmpty()) {
			if (this.cursorDragSlots.size() == 1) {
				return;
			}

			if (ScreenHandler.canInsertItemIntoSlot(slot, itemStack2, true) && this.handler.canInsertIntoSlot(slot)) {
				bl = true;
				int k = Math.min(itemStack2.getMaxCount(), slot.getMaxItemCount(itemStack2));
				int l = slot.getStack().isEmpty() ? 0 : slot.getStack().getCount();
				int m = ScreenHandler.calculateStackSize(this.cursorDragSlots, this.heldButtonType, itemStack2) + l;
				if (m > k) {
					m = k;
					string = Formatting.YELLOW.toString() + k;
				}

				itemStack = itemStack2.copyWithCount(m);
			}
			else {
				this.cursorDragSlots.remove(slot);
				this.calculateOffset();
			}
		}

		if (itemStack.isEmpty() && slot.isEnabled()) {
			Identifier identifier = slot.getBackgroundSprite();
			if (identifier != null) {
				context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier, i, j, 16, 16);
				bl2 = true;
			}
		}

		if (!bl2) {
			if (bl) {
				context.fill(i, j, i + 16, j + 16, -2130706433);
			}

			int k = slot.x + slot.y * this.backgroundWidth;
			if (slot.disablesDynamicDisplay()) {
				context.drawItemWithoutEntity(itemStack, i, j, k);
			}
			else {
				context.drawItem(itemStack, i, j, k);
			}

			context.drawStackOverlay(this.textRenderer, itemStack, i, j, string);
		}
	}

	private void calculateOffset() {
		ItemStack itemStack = this.handler.getCursorStack();
		if (!itemStack.isEmpty() && this.cursorDragging) {
			if (this.heldButtonType == 2) {
				this.draggedStackRemainder = itemStack.getMaxCount();
			}
			else {
				this.draggedStackRemainder = itemStack.getCount();

				for (Slot slot : this.cursorDragSlots) {
					ItemStack itemStack2 = slot.getStack();
					int i = itemStack2.isEmpty() ? 0 : itemStack2.getCount();
					int j = Math.min(itemStack.getMaxCount(), slot.getMaxItemCount(itemStack));
					int
							k =
							Math.min(
									ScreenHandler.calculateStackSize(
											this.cursorDragSlots,
											this.heldButtonType,
											itemStack
									) + i, j
							);
					this.draggedStackRemainder -= k - i;
				}
			}
		}
	}

	private @Nullable Slot getSlotAt(double mouseX, double mouseY) {
		for (Slot slot : this.handler.slots) {
			if (slot.isEnabled() && this.isPointOverSlot(slot, mouseX, mouseY)) {
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
		else {
			boolean bl = this.client.options.pickItemKey.matchesMouse(click) && this.client.player.isInCreativeMode();
			Slot slot = this.getSlotAt(click.x(), click.y());
			this.doubleClicking = this.lastClickedSlot == slot && doubled;
			this.cancelNextRelease = false;
			if (click.button() != 0 && click.button() != 1 && !bl) {
				this.onMouseClick(click);
			}
			else {
				int i = this.x;
				int j = this.y;
				boolean bl2 = this.isClickOutsideBounds(click.x(), click.y(), i, j);
				int k = -1;
				if (slot != null) {
					k = slot.id;
				}

				if (bl2) {
					k = -999;
				}

				if (this.client.options.getTouchscreen().getValue() && bl2 && this.handler.getCursorStack().isEmpty()) {
					this.close();
					return true;
				}

				if (k != -1) {
					if (this.client.options.getTouchscreen().getValue()) {
						if (slot != null && slot.hasStack()) {
							this.touchDragSlotStart = slot;
							this.touchDragStack = ItemStack.EMPTY;
							this.touchIsRightClickDrag = click.button() == 1;
						}
						else {
							this.touchDragSlotStart = null;
						}
					}
					else if (!this.cursorDragging) {
						if (this.handler.getCursorStack().isEmpty()) {
							if (bl) {
								this.onMouseClick(slot, k, click.button(), SlotActionType.CLONE);
							}
							else {
								boolean bl3 = k != -999 && click.hasShift();
								SlotActionType slotActionType = SlotActionType.PICKUP;
								if (bl3) {
									this.quickMovingStack =
											slot != null && slot.hasStack() ? slot.getStack().copy() : ItemStack.EMPTY;
									slotActionType = SlotActionType.QUICK_MOVE;
								}
								else if (k == -999) {
									slotActionType = SlotActionType.THROW;
								}

								this.onMouseClick(slot, k, click.button(), slotActionType);
							}

							this.cancelNextRelease = true;
						}
						else {
							this.cursorDragging = true;
							this.heldButtonCode = click.button();
							this.cursorDragSlots.clear();
							if (click.button() == 0) {
								this.heldButtonType = 0;
							}
							else if (click.button() == 1) {
								this.heldButtonType = 1;
							}
							else if (bl) {
								this.heldButtonType = 2;
							}
						}
					}
				}
			}

			this.lastClickedSlot = slot;
			return true;
		}
	}

	private void onMouseClick(Click click) {
		if (this.focusedSlot != null && this.handler.getCursorStack().isEmpty()) {
			if (this.client.options.swapHandsKey.matchesMouse(click)) {
				this.onMouseClick(this.focusedSlot, this.focusedSlot.id, 40, SlotActionType.SWAP);
				return;
			}

			for (int i = 0; i < 9; i++) {
				if (this.client.options.hotbarKeys[i].matchesMouse(click)) {
					this.onMouseClick(this.focusedSlot, this.focusedSlot.id, i, SlotActionType.SWAP);
				}
			}
		}
	}

	protected boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top) {
		return mouseX < left || mouseY < top || mouseX >= left + this.backgroundWidth
				|| mouseY >= top + this.backgroundHeight;
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		Slot slot = this.getSlotAt(click.x(), click.y());
		ItemStack itemStack = this.handler.getCursorStack();
		if (this.touchDragSlotStart != null && this.client.options.getTouchscreen().getValue()) {
			if (click.button() == 0 || click.button() == 1) {
				if (this.touchDragStack.isEmpty()) {
					if (slot != this.touchDragSlotStart && !this.touchDragSlotStart.getStack().isEmpty()) {
						this.touchDragStack = this.touchDragSlotStart.getStack().copy();
					}
				}
				else if (this.touchDragStack.getCount() > 1 && slot != null && ScreenHandler.canInsertItemIntoSlot(
						slot,
						this.touchDragStack,
						false
				)) {
					long l = Util.getMeasuringTimeMs();
					if (this.touchHoveredSlot == slot) {
						if (l - this.touchDropTimer > 500L) {
							this.onMouseClick(
									this.touchDragSlotStart,
									this.touchDragSlotStart.id,
									0,
									SlotActionType.PICKUP
							);
							this.onMouseClick(slot, slot.id, 1, SlotActionType.PICKUP);
							this.onMouseClick(
									this.touchDragSlotStart,
									this.touchDragSlotStart.id,
									0,
									SlotActionType.PICKUP
							);
							this.touchDropTimer = l + 750L;
							this.touchDragStack.decrement(1);
						}
					}
					else {
						this.touchHoveredSlot = slot;
						this.touchDropTimer = l;
					}
				}
			}

			return true;
		}
		else if (this.cursorDragging
				&& slot != null
				&& !itemStack.isEmpty()
				&& (itemStack.getCount() > this.cursorDragSlots.size() || this.heldButtonType == 2)
				&& ScreenHandler.canInsertItemIntoSlot(slot, itemStack, true)
				&& slot.canInsert(itemStack)
				&& this.handler.canInsertIntoSlot(slot)) {
			this.cursorDragSlots.add(slot);
			this.calculateOffset();
			return true;
		}
		else {
			return slot == null && this.handler.getCursorStack().isEmpty() ? super.mouseDragged(click, offsetX, offsetY)
			                                                               : true;
		}
	}

	@Override
	public boolean mouseReleased(Click click) {
		Slot slot = this.getSlotAt(click.x(), click.y());
		int i = this.x;
		int j = this.y;
		boolean bl = this.isClickOutsideBounds(click.x(), click.y(), i, j);
		int k = -1;
		if (slot != null) {
			k = slot.id;
		}

		if (bl) {
			k = -999;
		}

		if (this.doubleClicking && slot != null && click.button() == 0
				&& this.handler.canInsertIntoSlot(ItemStack.EMPTY, slot)) {
			if (click.hasShift()) {
				if (!this.quickMovingStack.isEmpty()) {
					for (Slot slot2 : this.handler.slots) {
						if (slot2 != null
								&& slot2.canTakeItems(this.client.player)
								&& slot2.hasStack()
								&& slot2.inventory == slot.inventory
								&& ScreenHandler.canInsertItemIntoSlot(slot2, this.quickMovingStack, true)) {
							this.onMouseClick(slot2, slot2.id, click.button(), SlotActionType.QUICK_MOVE);
						}
					}
				}
			}
			else {
				this.onMouseClick(slot, k, click.button(), SlotActionType.PICKUP_ALL);
			}

			this.doubleClicking = false;
		}
		else {
			if (this.cursorDragging && this.heldButtonCode != click.button()) {
				this.cursorDragging = false;
				this.cursorDragSlots.clear();
				this.cancelNextRelease = true;
				return true;
			}

			if (this.cancelNextRelease) {
				this.cancelNextRelease = false;
				return true;
			}

			if (this.touchDragSlotStart != null && this.client.options.getTouchscreen().getValue()) {
				if (click.button() == 0 || click.button() == 1) {
					if (this.touchDragStack.isEmpty() && slot != this.touchDragSlotStart) {
						this.touchDragStack = this.touchDragSlotStart.getStack();
					}

					boolean bl2 = ScreenHandler.canInsertItemIntoSlot(slot, this.touchDragStack, false);
					if (k != -1 && !this.touchDragStack.isEmpty() && bl2) {
						this.onMouseClick(
								this.touchDragSlotStart,
								this.touchDragSlotStart.id,
								click.button(),
								SlotActionType.PICKUP
						);
						this.onMouseClick(slot, k, 0, SlotActionType.PICKUP);
						if (this.handler.getCursorStack().isEmpty()) {
							this.letGoTouchStack = null;
						}
						else {
							this.onMouseClick(
									this.touchDragSlotStart,
									this.touchDragSlotStart.id,
									click.button(),
									SlotActionType.PICKUP
							);
							this.letGoTouchStack = new HandledScreen.LetGoTouchStack(
									this.touchDragStack,
									new Vector2i((int) click.x(), (int) click.y()),
									new Vector2i(this.touchDragSlotStart.x + i, this.touchDragSlotStart.y + j),
									Util.getMeasuringTimeMs()
							);
						}
					}
					else if (!this.touchDragStack.isEmpty()) {
						this.letGoTouchStack = new HandledScreen.LetGoTouchStack(
								this.touchDragStack,
								new Vector2i((int) click.x(), (int) click.y()),
								new Vector2i(this.touchDragSlotStart.x + i, this.touchDragSlotStart.y + j),
								Util.getMeasuringTimeMs()
						);
					}

					this.endTouchDrag();
				}
			}
			else if (this.cursorDragging && !this.cursorDragSlots.isEmpty()) {
				this.onMouseClick(
						null,
						-999,
						ScreenHandler.packQuickCraftData(0, this.heldButtonType),
						SlotActionType.QUICK_CRAFT
				);

				for (Slot slot2x : this.cursorDragSlots) {
					this.onMouseClick(
							slot2x,
							slot2x.id,
							ScreenHandler.packQuickCraftData(1, this.heldButtonType),
							SlotActionType.QUICK_CRAFT
					);
				}

				this.onMouseClick(
						null,
						-999,
						ScreenHandler.packQuickCraftData(2, this.heldButtonType),
						SlotActionType.QUICK_CRAFT
				);
			}
			else if (!this.handler.getCursorStack().isEmpty()) {
				if (this.client.options.pickItemKey.matchesMouse(click)) {
					this.onMouseClick(slot, k, click.button(), SlotActionType.CLONE);
				}
				else {
					boolean bl2 = k != -999 && click.hasShift();
					if (bl2) {
						this.quickMovingStack =
								slot != null && slot.hasStack() ? slot.getStack().copy() : ItemStack.EMPTY;
					}

					this.onMouseClick(slot, k, click.button(), bl2 ? SlotActionType.QUICK_MOVE : SlotActionType.PICKUP);
				}
			}
		}

		this.cursorDragging = false;
		return true;
	}

	/**
	 * End touch drag.
	 */
	public void endTouchDrag() {
		this.touchDragStack = ItemStack.EMPTY;
		this.touchDragSlotStart = null;
	}

	private boolean isPointOverSlot(Slot slot, double pointX, double pointY) {
		return this.isPointWithinBounds(slot.x, slot.y, 16, 16, pointX, pointY);
	}

	protected boolean isPointWithinBounds(int x, int y, int width, int height, double pointX, double pointY) {
		int i = this.x;
		int j = this.y;
		pointX -= i;
		pointY -= j;
		return pointX >= x - 1 && pointX < x + width + 1 && pointY >= y - 1 && pointY < y + height + 1;
	}

	private void resetTooltipSubmenus(Slot slot) {
		if (slot.hasStack()) {
			for (TooltipSubmenuHandler tooltipSubmenuHandler : this.tooltipSubmenuHandlers) {
				if (tooltipSubmenuHandler.isApplicableTo(slot)) {
					tooltipSubmenuHandler.reset(slot);
				}
			}
		}
	}

	/**
	 * Обрабатывает событие mouse click.
	 *
	 * @param slot slot
	 * @param slotId slot id
	 * @param button button
	 * @param actionType action type
	 */
	protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
		if (slot != null) {
			slotId = slot.id;
		}

		this.onMouseClick(slot, actionType);
		this.client.interactionManager.clickSlot(this.handler.syncId, slotId, button, actionType, this.client.player);
	}

	void onMouseClick(@Nullable Slot slot, SlotActionType actionType) {
		if (slot != null && slot.hasStack()) {
			for (TooltipSubmenuHandler tooltipSubmenuHandler : this.tooltipSubmenuHandlers) {
				if (tooltipSubmenuHandler.isApplicableTo(slot)) {
					tooltipSubmenuHandler.onMouseClick(slot, actionType);
				}
			}
		}
	}

	/**
	 * Обрабатывает событие slot changed state.
	 *
	 * @param slotId slot id
	 * @param handlerId handler id
	 * @param newState new state
	 */
	protected void onSlotChangedState(int slotId, int handlerId, boolean newState) {
		this.client.interactionManager.slotChangedState(slotId, handlerId, newState);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (super.keyPressed(input)) {
			return true;
		}
		else if (this.client.options.inventoryKey.matchesKey(input)) {
			this.close();
			return true;
		}
		else {
			this.handleHotbarKeyPressed(input);
			if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
				if (this.client.options.pickItemKey.matchesKey(input)) {
					this.onMouseClick(this.focusedSlot, this.focusedSlot.id, 0, SlotActionType.CLONE);
				}
				else if (this.client.options.dropKey.matchesKey(input)) {
					this.onMouseClick(
							this.focusedSlot,
							this.focusedSlot.id,
							input.hasCtrl() ? 1 : 0,
							SlotActionType.THROW
					);
				}
			}

			return false;
		}
	}

	/**
	 * Обрабатывает hotbar key pressed.
	 *
	 * @param keyInput key input
	 *
	 * @return boolean — результат операции
	 */
	protected boolean handleHotbarKeyPressed(KeyInput keyInput) {
		if (this.handler.getCursorStack().isEmpty() && this.focusedSlot != null) {
			if (this.client.options.swapHandsKey.matchesKey(keyInput)) {
				this.onMouseClick(this.focusedSlot, this.focusedSlot.id, 40, SlotActionType.SWAP);
				return true;
			}

			for (int i = 0; i < 9; i++) {
				if (this.client.options.hotbarKeys[i].matchesKey(keyInput)) {
					this.onMouseClick(this.focusedSlot, this.focusedSlot.id, i, SlotActionType.SWAP);
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public void removed() {
		if (this.client.player != null) {
			this.handler.onClosed(this.client.player);
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
		if (this.client.player.isAlive() && !this.client.player.isRemoved()) {
			this.handledScreenTick();
		}
		else {
			this.client.player.closeHandledScreen();
		}
	}

	/**
	 * Обрабатывает d screen tick.
	 */
	protected void handledScreenTick() {
	}

	@Override
	public T getScreenHandler() {
		return this.handler;
	}

	@Override
	public void close() {
		this.client.player.closeHandledScreen();
		if (this.focusedSlot != null) {
			this.resetTooltipSubmenus(this.focusedSlot);
		}

		super.close();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code LetGoTouchStack}.
	 */
	record LetGoTouchStack(ItemStack item, Vector2i start, Vector2i end, long time) {
	}
}

package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CrafterScreenHandler;
import net.minecraft.screen.slot.CrafterInputSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Экран автоматического крафтера. Поддерживает отключение отдельных слотов
 * для управления рецептом крафта.
 */
@Environment(EnvType.CLIENT)
public class CrafterScreen extends HandledScreen<CrafterScreenHandler> {

	private static final Identifier DISABLED_SLOT_TEXTURE = Identifier.ofVanilla("container/crafter/disabled_slot");
	private static final Identifier POWERED_REDSTONE_TEXTURE = Identifier.ofVanilla("container/crafter/powered_redstone");
	private static final Identifier UNPOWERED_REDSTONE_TEXTURE = Identifier.ofVanilla("container/crafter/unpowered_redstone");
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/crafter.png");
	private static final Text TOGGLEABLE_SLOT_TEXT = Text.translatable("gui.togglable_slot");
	private static final int SLOT_HOVER_SIZE = 19;
	private static final int SLOT_HOVER_OFFSET = 2;
	private static final int REDSTONE_ICON_SIZE = 16;
	private static final float SOUND_PITCH_ENABLED = 1.0F;
	private static final float SOUND_PITCH_DISABLED = 0.75F;
	private static final float SOUND_VOLUME = 0.4F;

	private final PlayerEntity player;

	public CrafterScreen(CrafterScreenHandler handler, PlayerInventory playerInventory, Text title) {
		super(handler, playerInventory, title);
		player = playerInventory.player;
	}

	@Override
	protected void init() {
		super.init();
		titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
	}

	@Override
	protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
		if (slot instanceof CrafterInputSlot && !slot.hasStack() && !player.isSpectator()) {
			switch (actionType) {
				case PICKUP -> {
					if (handler.isSlotDisabled(slotId)) {
						enableSlot(slotId);
					} else if (handler.getCursorStack().isEmpty()) {
						disableSlot(slotId);
					}
				}
				case SWAP -> {
					ItemStack hotbarStack = player.getInventory().getStack(button);

					if (handler.isSlotDisabled(slotId) && !hotbarStack.isEmpty()) {
						enableSlot(slotId);
					}
				}
				default -> {
				}
			}
		}

		super.onMouseClick(slot, slotId, button, actionType);
	}

	private void enableSlot(int slotId) {
		setSlotEnabled(slotId, true);
	}

	private void disableSlot(int slotId) {
		setSlotEnabled(slotId, false);
	}

	private void setSlotEnabled(int slotId, boolean enabled) {
		handler.setSlotEnabled(slotId, enabled);
		super.onSlotChangedState(slotId, handler.syncId, enabled);
		float pitch = enabled ? SOUND_PITCH_ENABLED : SOUND_PITCH_DISABLED;
		player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), SOUND_VOLUME, pitch);
	}

	@Override
	public void drawSlot(DrawContext context, Slot slot, int mouseX, int mouseY) {
		if (slot instanceof CrafterInputSlot crafterInputSlot) {
			if (handler.isSlotDisabled(slot.id)) {
				drawDisabledSlot(context, crafterInputSlot);
			} else {
				super.drawSlot(context, slot, mouseX, mouseY);
			}

			int slotScreenX = x + crafterInputSlot.x - SLOT_HOVER_OFFSET;
			int slotScreenY = y + crafterInputSlot.y - SLOT_HOVER_OFFSET;

			if (mouseX > slotScreenX && mouseY > slotScreenY
				&& mouseX < slotScreenX + SLOT_HOVER_SIZE
				&& mouseY < slotScreenY + SLOT_HOVER_SIZE
			) {
				context.setCursor(StandardCursors.POINTING_HAND);
			}
		} else {
			super.drawSlot(context, slot, mouseX, mouseY);
		}
	}

	private void drawDisabledSlot(DrawContext context, CrafterInputSlot slot) {
		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, DISABLED_SLOT_TEXTURE, slot.x - 1, slot.y - 1, 18, 18);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		drawArrowTexture(context);
		drawMouseoverTooltip(context, mouseX, mouseY);

		if (focusedSlot instanceof CrafterInputSlot
			&& !handler.isSlotDisabled(focusedSlot.id)
			&& handler.getCursorStack().isEmpty()
			&& !focusedSlot.hasStack()
			&& !player.isSpectator()
		) {
			context.drawTooltip(textRenderer, TOGGLEABLE_SLOT_TEXT, mouseX, mouseY);
		}
	}

	private void drawArrowTexture(DrawContext context) {
		int arrowX = width / 2 + 9;
		int arrowY = height / 2 - 48;
		Identifier arrowTexture = handler.isTriggered() ? POWERED_REDSTONE_TEXTURE : UNPOWERED_REDSTONE_TEXTURE;
		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, arrowTexture, arrowX, arrowY, REDSTONE_ICON_SIZE, REDSTONE_ICON_SIZE);
	}

	@Override
	protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
		int bgX = (width - backgroundWidth) / 2;
		int bgY = (height - backgroundHeight) / 2;
		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			TEXTURE,
			bgX,
			bgY,
			0.0F,
			0.0F,
			backgroundWidth,
			backgroundHeight,
			256,
			256
		);
	}
}

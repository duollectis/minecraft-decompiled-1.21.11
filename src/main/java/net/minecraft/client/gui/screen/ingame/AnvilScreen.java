package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Экран наковальни. Позволяет переименовывать предметы и объединять их для ремонта/зачарования.
 * Отображает стоимость операции в уровнях опыта.
 */
@Environment(EnvType.CLIENT)
public class AnvilScreen extends ForgingScreen<AnvilScreenHandler> {

	private static final Identifier TEXT_FIELD_TEXTURE = Identifier.ofVanilla("container/anvil/text_field");
	private static final Identifier TEXT_FIELD_DISABLED_TEXTURE = Identifier.ofVanilla("container/anvil/text_field_disabled");
	private static final Identifier ERROR_TEXTURE = Identifier.ofVanilla("container/anvil/error");
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/anvil.png");
	private static final Text TOO_EXPENSIVE_TEXT = Text.translatable("container.repair.expensive");
	private static final int COLOR_NORMAL = -8323296;
	private static final int COLOR_TOO_EXPENSIVE = -40864;
	private static final int MAX_LEVEL_COST_CREATIVE = 40;
	private static final int COST_LABEL_Y = 69;
	private static final int COST_BACKGROUND_Y1 = 67;
	private static final int COST_BACKGROUND_Y2 = 79;

	private TextFieldWidget nameField;
	private final PlayerEntity player;

	public AnvilScreen(AnvilScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title, TEXTURE);
		player = inventory.player;
		titleX = 60;
	}

	@Override
	protected void setup() {
		int bgX = (width - backgroundWidth) / 2;
		int bgY = (height - backgroundHeight) / 2;
		nameField = new TextFieldWidget(textRenderer, bgX + 62, bgY + 24, 103, 12, Text.translatable("container.repair"));
		nameField.setFocusUnlocked(false);
		nameField.setEditableColor(-1);
		nameField.setUneditableColor(-1);
		nameField.setInvertSelectionBackground(false);
		nameField.setDrawsBackground(false);
		nameField.setMaxLength(AnvilScreenHandler.MAX_NAME_LENGTH);
		nameField.setChangedListener(this::onRenamed);
		nameField.setText("");
		addDrawableChild(nameField);
		nameField.setEditable(handler.getSlot(0).hasStack());
	}

	@Override
	protected void handledScreenTick() {
		super.handledScreenTick();
		client.player.experienceBarDisplayStartTime = client.player.age;
	}

	@Override
	protected void setInitialFocus() {
		setInitialFocus(nameField);
	}

	@Override
	public void resize(int width, int height) {
		String currentText = nameField.getText();
		init(width, height);
		nameField.setText(currentText);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.isEscape()) {
			client.player.closeHandledScreen();
			return true;
		}

		return nameField.keyPressed(input) || nameField.isActive()
			? true
			: super.keyPressed(input);
	}

	private void onRenamed(String name) {
		Slot inputSlot = handler.getSlot(0);
		if (!inputSlot.hasStack()) {
			return;
		}

		String nameToSend = name;
		if (!inputSlot.getStack().contains(DataComponentTypes.CUSTOM_NAME)
			&& name.equals(inputSlot.getStack().getName().getString())
		) {
			nameToSend = "";
		}

		if (handler.setNewItemName(nameToSend)) {
			client.player.networkHandler.sendPacket(new RenameItemC2SPacket(nameToSend));
		}
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		super.drawForeground(context, mouseX, mouseY);
		int levelCost = handler.getLevelCost();
		if (levelCost <= 0) {
			return;
		}

		int textColor = COLOR_NORMAL;
		Text costText;
		if (levelCost >= MAX_LEVEL_COST_CREATIVE && !client.player.isInCreativeMode()) {
			costText = TOO_EXPENSIVE_TEXT;
			textColor = COLOR_TOO_EXPENSIVE;
		} else if (!handler.getSlot(2).hasStack()) {
			costText = null;
		} else {
			costText = Text.translatable("container.repair.cost", levelCost);
			if (!handler.getSlot(2).canTakeItems(player)) {
				textColor = COLOR_TOO_EXPENSIVE;
			}
		}

		if (costText == null) {
			return;
		}

		int labelX = backgroundWidth - 8 - textRenderer.getWidth(costText) - 2;
		context.fill(labelX - 2, COST_BACKGROUND_Y1, backgroundWidth - 8, COST_BACKGROUND_Y2, 1325400064);
		context.drawTextWithShadow(textRenderer, costText, labelX, COST_LABEL_Y, textColor);
	}

	@Override
	protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
		super.drawBackground(context, deltaTicks, mouseX, mouseY);
		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			handler.getSlot(0).hasStack() ? TEXT_FIELD_TEXTURE : TEXT_FIELD_DISABLED_TEXTURE,
			x + 59,
			y + 20,
			110,
			16
		);
	}

	@Override
	protected void drawInvalidRecipeArrow(DrawContext context, int x, int y) {
		if ((handler.getSlot(0).hasStack() || handler.getSlot(1).hasStack())
			&& !handler.getSlot(handler.getResultSlotIndex()).hasStack()
		) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ERROR_TEXTURE, x + 99, y + 45, 28, 21);
		}
	}

	@Override
	public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
		if (slotId == 0) {
			nameField.setText(stack.isEmpty() ? "" : stack.getName().getString());
			nameField.setEditable(!stack.isEmpty());
			setFocused(nameField);
		}
	}
}

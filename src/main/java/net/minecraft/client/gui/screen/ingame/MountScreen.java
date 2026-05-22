package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.MountScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Базовый экран инвентаря верхового существа (лошадь, наутилус и т.д.).
 * Отрисовывает текстуру фона, слоты седла/брони и 3D-модель существа.
 */
@Environment(EnvType.CLIENT)
public abstract class MountScreen<T extends MountScreenHandler> extends HandledScreen<T> {

	private static final int CHEST_SLOTS_TEXTURE_WIDTH = 90;
	private static final int CHEST_SLOTS_TEXTURE_HEIGHT = 54;
	private static final int CHEST_SLOTS_OFFSET_X = 79;
	private static final int CHEST_SLOTS_OFFSET_Y = 17;
	private static final int SADDLE_SLOT_OFFSET_X = 7;
	private static final int SADDLE_SLOT_OFFSET_Y = 35;
	private static final int ARMOR_SLOT_OFFSET_Y = 35;
	private static final int ENTITY_LEFT = 26;
	private static final int ENTITY_TOP = 18;
	private static final int ENTITY_RIGHT = 78;
	private static final int ENTITY_BOTTOM = 70;
	private static final int ENTITY_SCALE = 17;
	private static final float ENTITY_Y_OFFSET = 0.25F;
	private static final int SLOT_SIZE = 18;

	protected final int slotColumnCount;
	protected float mouseX;
	protected float mouseY;
	protected LivingEntity mount;

	public MountScreen(T handler, PlayerInventory inventory, Text title, int slotColumnCount, LivingEntity mount) {
		super(handler, inventory, title);
		this.slotColumnCount = slotColumnCount;
		this.mount = mount;
	}

	@Override
	protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
		int bgX = (width - backgroundWidth) / 2;
		int bgY = (height - backgroundHeight) / 2;

		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			getTexture(),
			bgX,
			bgY,
			0.0F,
			0.0F,
			backgroundWidth,
			backgroundHeight,
			256,
			256
		);

		if (slotColumnCount > 0 && getChestSlotsTexture() != null) {
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				getChestSlotsTexture(),
				CHEST_SLOTS_TEXTURE_WIDTH,
				CHEST_SLOTS_TEXTURE_HEIGHT,
				0,
				0,
				bgX + CHEST_SLOTS_OFFSET_X,
				bgY + CHEST_SLOTS_OFFSET_Y,
				slotColumnCount * SLOT_SIZE,
				CHEST_SLOTS_TEXTURE_HEIGHT
			);
		}

		if (canEquipSaddle()) {
			drawSlot(context, bgX + SADDLE_SLOT_OFFSET_X, bgY + SADDLE_SLOT_OFFSET_Y - SLOT_SIZE);
		}

		if (canEquipArmor()) {
			drawSlot(context, bgX + SADDLE_SLOT_OFFSET_X, bgY + ARMOR_SLOT_OFFSET_Y);
		}

		InventoryScreen.drawEntity(
			context,
			bgX + ENTITY_LEFT,
			bgY + ENTITY_TOP,
			bgX + ENTITY_RIGHT,
			bgY + ENTITY_BOTTOM,
			ENTITY_SCALE,
			ENTITY_Y_OFFSET,
			this.mouseX,
			this.mouseY,
			mount
		);
	}

	protected void drawSlot(DrawContext context, int x, int y) {
		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, getSlotTexture(), x, y, SLOT_SIZE, SLOT_SIZE);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		super.render(context, mouseX, mouseY, deltaTicks);
		drawMouseoverTooltip(context, mouseX, mouseY);
	}

	protected abstract Identifier getTexture();

	protected abstract Identifier getSlotTexture();

	protected abstract @Nullable Identifier getChestSlotsTexture();

	protected abstract boolean canEquipSaddle();

	protected abstract boolean canEquipArmor();
}

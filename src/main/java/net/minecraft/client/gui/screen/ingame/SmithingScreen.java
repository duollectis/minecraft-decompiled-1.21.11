package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SmithingTemplateItem;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Экран кузнечного стола. Отображает слоты шаблона/базы/добавки,
 * 3D-превью стойки для брони с надетым результатом и тултипы ошибок.
 */
@Environment(EnvType.CLIENT)
public class SmithingScreen extends ForgingScreen<SmithingScreenHandler> {

	private static final Identifier ERROR_TEXTURE = Identifier.ofVanilla("container/smithing/error");
	private static final Identifier EMPTY_SLOT_SMITHING_TEMPLATE_ARMOR_TRIM_TEXTURE = Identifier.ofVanilla("container/slot/smithing_template_armor_trim");
	private static final Identifier EMPTY_SLOT_SMITHING_TEMPLATE_NETHERITE_UPGRADE_TEXTURE = Identifier.ofVanilla("container/slot/smithing_template_netherite_upgrade");

	private static final Text MISSING_TEMPLATE_TOOLTIP = Text.translatable("container.upgrade.missing_template_tooltip");
	private static final Text ERROR_TOOLTIP = Text.translatable("container.upgrade.error_tooltip");

	private static final List<Identifier> EMPTY_SLOT_TEXTURES = List.of(
		EMPTY_SLOT_SMITHING_TEMPLATE_ARMOR_TRIM_TEXTURE,
		EMPTY_SLOT_SMITHING_TEMPLATE_NETHERITE_UPGRADE_TEXTURE
	);

	private static final int TITLE_X = 44;
	private static final int TITLE_Y = 15;
	private static final int ERROR_ARROW_WIDTH = 28;
	private static final int ERROR_ARROW_HEIGHT = 21;
	private static final int ERROR_ARROW_X = 65;
	private static final int ERROR_ARROW_Y = 46;
	private static final int TOOLTIP_MAX_WIDTH = 115;
	private static final int ARMOR_STAND_SCALE = 25;
	private static final int ARMOR_STAND_X1 = 121;
	private static final int ARMOR_STAND_Y1 = 20;
	private static final int ARMOR_STAND_X2 = 161;
	private static final int ARMOR_STAND_Y2 = 80;
	private static final float ARMOR_STAND_PITCH = 25.0F;
	private static final float ARMOR_STAND_BODY_YAW = 210.0F;
	private static final Vector3f ARMOR_STAND_TRANSLATION = new Vector3f(0.0F, 1.0F, 0.0F);
	private static final Quaternionf ARMOR_STAND_ROTATION = new Quaternionf().rotationXYZ(0.43633232F, 0.0F, (float) Math.PI);

	private static final int SLOT_TEMPLATE = 0;
	private static final int SLOT_BASE = 1;
	private static final int SLOT_ADDITIONS = 2;
	private static final int SLOT_RESULT = 3;

	private final CyclingSlotIcon templateSlotIcon = new CyclingSlotIcon(SLOT_TEMPLATE);
	private final CyclingSlotIcon baseSlotIcon = new CyclingSlotIcon(SLOT_BASE);
	private final CyclingSlotIcon additionsSlotIcon = new CyclingSlotIcon(SLOT_ADDITIONS);
	private final ArmorStandEntityRenderState armorStand = new ArmorStandEntityRenderState();

	public SmithingScreen(SmithingScreenHandler handler, PlayerInventory playerInventory, Text title) {
		super(handler, playerInventory, title, Identifier.ofVanilla("textures/gui/container/smithing.png"));
		titleX = TITLE_X;
		titleY = TITLE_Y;
		armorStand.entityType = EntityType.ARMOR_STAND;
		armorStand.showBasePlate = false;
		armorStand.showArms = true;
		armorStand.pitch = ARMOR_STAND_PITCH;
		armorStand.bodyYaw = ARMOR_STAND_BODY_YAW;
	}

	@Override
	protected void setup() {
		equipArmorStand(handler.getSlot(SLOT_RESULT).getStack());
	}

	@Override
	public void handledScreenTick() {
		super.handledScreenTick();
		Optional<SmithingTemplateItem> template = getSmithingTemplate();
		templateSlotIcon.updateTexture(EMPTY_SLOT_TEXTURES);
		baseSlotIcon.updateTexture(template.map(SmithingTemplateItem::getEmptyBaseSlotTextures).orElse(List.of()));
		additionsSlotIcon.updateTexture(template.map(SmithingTemplateItem::getEmptyAdditionsSlotTextures).orElse(List.of()));
	}

	private Optional<SmithingTemplateItem> getSmithingTemplate() {
		ItemStack templateStack = handler.getSlot(SLOT_TEMPLATE).getStack();
		return !templateStack.isEmpty() && templateStack.getItem() instanceof SmithingTemplateItem smithingTemplate
			? Optional.of(smithingTemplate)
			: Optional.empty();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		renderSlotTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
		super.drawBackground(context, deltaTicks, mouseX, mouseY);
		templateSlotIcon.render(handler, context, deltaTicks, x, y);
		baseSlotIcon.render(handler, context, deltaTicks, x, y);
		additionsSlotIcon.render(handler, context, deltaTicks, x, y);

		context.addEntity(
			armorStand,
			(float) ARMOR_STAND_SCALE,
			ARMOR_STAND_TRANSLATION,
			ARMOR_STAND_ROTATION,
			null,
			x + ARMOR_STAND_X1,
			y + ARMOR_STAND_Y1,
			x + ARMOR_STAND_X2,
			y + ARMOR_STAND_Y2
		);
	}

	@Override
	public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
		if (slotId == SLOT_RESULT) {
			equipArmorStand(stack);
		}
	}

	/**
	 * Экипирует стойку для брони предметом из слота результата для превью.
	 * Определяет слот экипировки через компонент {@link EquippableComponent}.
	 */
	private void equipArmorStand(ItemStack stack) {
		armorStand.leftHandItem = ItemStack.EMPTY;
		armorStand.leftHandItemState.clear();
		armorStand.equippedHeadStack = ItemStack.EMPTY;
		armorStand.headItemRenderState.clear();
		armorStand.equippedChestStack = ItemStack.EMPTY;
		armorStand.equippedLegsStack = ItemStack.EMPTY;
		armorStand.equippedFeetStack = ItemStack.EMPTY;

		if (stack.isEmpty()) {
			return;
		}

		EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
		EquipmentSlot equipmentSlot = equippable != null ? equippable.slot() : null;
		ItemModelManager itemModelManager = client.getItemModelManager();

		switch (equipmentSlot) {
			case HEAD -> {
				if (ArmorFeatureRenderer.hasModel(stack, EquipmentSlot.HEAD)) {
					armorStand.equippedHeadStack = stack.copy();
				} else {
					itemModelManager.clearAndUpdate(armorStand.headItemRenderState, stack, ItemDisplayContext.HEAD, null, null, 0);
				}
			}
			case CHEST -> armorStand.equippedChestStack = stack.copy();
			case LEGS -> armorStand.equippedLegsStack = stack.copy();
			case FEET -> armorStand.equippedFeetStack = stack.copy();
			default -> {
				armorStand.leftHandItem = stack.copy();
				itemModelManager.clearAndUpdate(armorStand.leftHandItemState, stack, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, null, null, 0);
			}
		}
	}

	@Override
	protected void drawInvalidRecipeArrow(DrawContext context, int x, int y) {
		if (hasInvalidRecipe()) {
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				ERROR_TEXTURE,
				x + ERROR_ARROW_X,
				y + ERROR_ARROW_Y,
				ERROR_ARROW_WIDTH,
				ERROR_ARROW_HEIGHT
			);
		}
	}

	private void renderSlotTooltip(DrawContext context, int mouseX, int mouseY) {
		Optional<Text> tooltip = Optional.empty();

		if (hasInvalidRecipe() && isPointWithinBounds(ERROR_ARROW_X, ERROR_ARROW_Y, ERROR_ARROW_WIDTH, ERROR_ARROW_HEIGHT, mouseX, mouseY)) {
			tooltip = Optional.of(ERROR_TOOLTIP);
		}

		if (focusedSlot != null) {
			ItemStack templateStack = handler.getSlot(SLOT_TEMPLATE).getStack();
			ItemStack focusedStack = focusedSlot.getStack();

			if (templateStack.isEmpty()) {
				if (focusedSlot.id == SLOT_TEMPLATE) {
					tooltip = Optional.of(MISSING_TEMPLATE_TOOLTIP);
				}
			} else if (templateStack.getItem() instanceof SmithingTemplateItem smithingTemplate && focusedStack.isEmpty()) {
				if (focusedSlot.id == SLOT_BASE) {
					tooltip = Optional.of(smithingTemplate.getBaseSlotDescription());
				} else if (focusedSlot.id == SLOT_ADDITIONS) {
					tooltip = Optional.of(smithingTemplate.getAdditionsSlotDescription());
				}
			}
		}

		tooltip.ifPresent(text -> context.drawOrderedTooltip(
			textRenderer,
			textRenderer.wrapLines(text, TOOLTIP_MAX_WIDTH),
			mouseX,
			mouseY
		));
	}

	private boolean hasInvalidRecipe() {
		return handler.hasInvalidRecipe();
	}
}

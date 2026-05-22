package net.minecraft.client.gui.screen.ingame;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateBeaconC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.BeaconScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Экран управления маяком. Позволяет выбрать первичный и вторичный эффекты статуса
 * в зависимости от уровня пирамиды маяка.
 */
@Environment(EnvType.CLIENT)
public class BeaconScreen extends HandledScreen<BeaconScreenHandler> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/beacon.png");
	static final Identifier BUTTON_DISABLED_TEXTURE = Identifier.ofVanilla("container/beacon/button_disabled");
	static final Identifier BUTTON_SELECTED_TEXTURE = Identifier.ofVanilla("container/beacon/button_selected");
	static final Identifier BUTTON_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("container/beacon/button_highlighted");
	static final Identifier BUTTON_TEXTURE = Identifier.ofVanilla("container/beacon/button");
	static final Identifier CONFIRM_TEXTURE = Identifier.ofVanilla("container/beacon/confirm");
	static final Identifier CANCEL_TEXTURE = Identifier.ofVanilla("container/beacon/cancel");
	private static final Text PRIMARY_POWER_TEXT = Text.translatable("block.minecraft.beacon.primary");
	private static final Text SECONDARY_POWER_TEXT = Text.translatable("block.minecraft.beacon.secondary");
	private static final int BACKGROUND_WIDTH_BEACON = 230;
	private static final int BACKGROUND_HEIGHT_BEACON = 219;
	private static final int PRIMARY_LEVELS = 3;
	private static final int SECONDARY_LEVEL = 3;

	private final List<BeaconScreen.BeaconButtonWidget> buttons = Lists.newArrayList();
	@Nullable RegistryEntry<StatusEffect> primaryEffect;
	@Nullable RegistryEntry<StatusEffect> secondaryEffect;

	public BeaconScreen(BeaconScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		backgroundWidth = BACKGROUND_WIDTH_BEACON;
		backgroundHeight = BACKGROUND_HEIGHT_BEACON;
		handler.addListener(new ScreenHandlerListener() {
			@Override
			public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
			}

			@Override
			public void onPropertyUpdate(ScreenHandler handler, int property, int value) {
				BeaconScreenHandler beaconHandler = (BeaconScreenHandler) handler;
				primaryEffect = beaconHandler.getPrimaryEffect();
				secondaryEffect = beaconHandler.getSecondaryEffect();
			}
		});
	}

	private <T extends ClickableWidget & BeaconScreen.BeaconButtonWidget> void addButton(T button) {
		addDrawableChild(button);
		buttons.add(button);
	}

	@Override
	protected void init() {
		super.init();
		buttons.clear();

		for (int level = 0; level <= PRIMARY_LEVELS - 1; level++) {
			int effectCount = BeaconBlockEntity.EFFECTS_BY_LEVEL.get(level).size();
			int rowWidth = effectCount * 22 + (effectCount - 1) * 2;

			for (int effectIndex = 0; effectIndex < effectCount; effectIndex++) {
				RegistryEntry<StatusEffect> effect = BeaconBlockEntity.EFFECTS_BY_LEVEL.get(level).get(effectIndex);
				BeaconScreen.EffectButtonWidget button = new BeaconScreen.EffectButtonWidget(
					x + 76 + effectIndex * 24 - rowWidth / 2,
					y + 22 + level * 25,
					effect,
					true,
					level
				);
				button.active = false;
				addButton(button);
			}
		}

		int secondaryEffectCount = BeaconBlockEntity.EFFECTS_BY_LEVEL.get(SECONDARY_LEVEL).size() + 1;
		int secondaryRowWidth = secondaryEffectCount * 22 + (secondaryEffectCount - 1) * 2;

		for (int effectIndex = 0; effectIndex < secondaryEffectCount - 1; effectIndex++) {
			RegistryEntry<StatusEffect> effect = BeaconBlockEntity.EFFECTS_BY_LEVEL.get(SECONDARY_LEVEL).get(effectIndex);
			BeaconScreen.EffectButtonWidget button = new BeaconScreen.EffectButtonWidget(
				x + 167 + effectIndex * 24 - secondaryRowWidth / 2,
				y + 47,
				effect,
				false,
				SECONDARY_LEVEL
			);
			button.active = false;
			addButton(button);
		}

		RegistryEntry<StatusEffect> firstEffect = BeaconBlockEntity.EFFECTS_BY_LEVEL.get(0).get(0);
		BeaconScreen.EffectButtonWidget levelTwoButton = new BeaconScreen.LevelTwoEffectButtonWidget(
			x + 167 + (secondaryEffectCount - 1) * 24 - secondaryRowWidth / 2,
			y + 47,
			firstEffect
		);
		levelTwoButton.visible = false;
		addButton(levelTwoButton);
		addButton(new BeaconScreen.DoneButtonWidget(x + 164, y + 107));
		addButton(new BeaconScreen.CancelButtonWidget(x + 190, y + 107));
	}

	@Override
	public void handledScreenTick() {
		super.handledScreenTick();
		tickButtons();
	}

	void tickButtons() {
		int level = handler.getProperties();
		buttons.forEach(button -> button.tick(level));
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawCenteredTextWithShadow(textRenderer, PRIMARY_POWER_TEXT, 62, 10, -2039584);
		context.drawCenteredTextWithShadow(textRenderer, SECONDARY_POWER_TEXT, 169, 10, -2039584);
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
		context.drawItem(new ItemStack(Items.NETHERITE_INGOT), bgX + 20, bgY + 109);
		context.drawItem(new ItemStack(Items.EMERALD), bgX + 41, bgY + 109);
		context.drawItem(new ItemStack(Items.DIAMOND), bgX + 41 + 22, bgY + 109);
		context.drawItem(new ItemStack(Items.GOLD_INGOT), bgX + 42 + 44, bgY + 109);
		context.drawItem(new ItemStack(Items.IRON_INGOT), bgX + 42 + 66, bgY + 109);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		drawMouseoverTooltip(context, mouseX, mouseY);
	}

	/**
	 * {@code BaseButtonWidget} — базовая кнопка маяка с поддержкой состояний disabled/selected/highlighted.
	 */
	@Environment(EnvType.CLIENT)
	abstract static class BaseButtonWidget extends PressableWidget implements BeaconScreen.BeaconButtonWidget {

		private boolean disabled;

		protected BaseButtonWidget(int x, int y) {
			super(x, y, 22, 22, ScreenTexts.EMPTY);
		}

		protected BaseButtonWidget(int x, int y, Text message) {
			super(x, y, 22, 22, message);
		}

		@Override
		public void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
			Identifier buttonTexture;
			if (!active) {
				buttonTexture = BeaconScreen.BUTTON_DISABLED_TEXTURE;
			} else if (disabled) {
				buttonTexture = BeaconScreen.BUTTON_SELECTED_TEXTURE;
			} else if (isSelected()) {
				buttonTexture = BeaconScreen.BUTTON_HIGHLIGHTED_TEXTURE;
			} else {
				buttonTexture = BeaconScreen.BUTTON_TEXTURE;
			}

			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, buttonTexture, getX(), getY(), width, height);
			renderExtra(context);
		}

		protected abstract void renderExtra(DrawContext context);

		public boolean isDisabled() {
			return disabled;
		}

		public void setDisabled(boolean disabled) {
			this.disabled = disabled;
		}

		@Override
		public void appendClickableNarrations(NarrationMessageBuilder builder) {
			appendDefaultNarrations(builder);
		}
	}

	/**
	 * {@code BeaconButtonWidget} — маркерный интерфейс для кнопок маяка с поддержкой тика.
	 */
	@Environment(EnvType.CLIENT)
	interface BeaconButtonWidget {

		void tick(int level);
	}

	/**
	 * {@code CancelButtonWidget} — кнопка отмены, закрывает экран без сохранения.
	 */
	@Environment(EnvType.CLIENT)
	class CancelButtonWidget extends BeaconScreen.IconButtonWidget {

		public CancelButtonWidget(final int x, final int y) {
			super(x, y, BeaconScreen.CANCEL_TEXTURE, ScreenTexts.CANCEL);
		}

		@Override
		public void onPress(AbstractInput input) {
			BeaconScreen.this.client.player.closeHandledScreen();
		}

		@Override
		public void tick(int level) {
		}
	}

	/**
	 * {@code DoneButtonWidget} — кнопка подтверждения, отправляет выбранные эффекты на сервер.
	 */
	@Environment(EnvType.CLIENT)
	class DoneButtonWidget extends BeaconScreen.IconButtonWidget {

		public DoneButtonWidget(final int x, final int y) {
			super(x, y, BeaconScreen.CONFIRM_TEXTURE, ScreenTexts.DONE);
		}

		@Override
		public void onPress(AbstractInput input) {
			BeaconScreen.this.client
				.getNetworkHandler()
				.sendPacket(new UpdateBeaconC2SPacket(
					Optional.ofNullable(BeaconScreen.this.primaryEffect),
					Optional.ofNullable(BeaconScreen.this.secondaryEffect)
				));
			BeaconScreen.this.client.player.closeHandledScreen();
		}

		@Override
		public void tick(int level) {
			active = BeaconScreen.this.handler.hasPayment() && BeaconScreen.this.primaryEffect != null;
		}
	}

	/**
	 * {@code EffectButtonWidget} — кнопка выбора эффекта статуса для маяка.
	 */
	@Environment(EnvType.CLIENT)
	class EffectButtonWidget extends BeaconScreen.BaseButtonWidget {

		private final boolean primary;
		protected final int level;
		private RegistryEntry<StatusEffect> effect;
		private Identifier sprite;

		public EffectButtonWidget(
			final int x,
			final int y,
			final RegistryEntry<StatusEffect> effect,
			final boolean primary,
			final int level
		) {
			super(x, y);
			this.primary = primary;
			this.level = level;
			init(effect);
		}

		protected void init(RegistryEntry<StatusEffect> effect) {
			this.effect = effect;
			sprite = InGameHud.getEffectTexture(effect);
			setTooltip(Tooltip.of(getEffectName(effect), null));
		}

		protected MutableText getEffectName(RegistryEntry<StatusEffect> effect) {
			return Text.translatable(effect.value().getTranslationKey());
		}

		@Override
		public void onPress(AbstractInput input) {
			if (isDisabled()) {
				return;
			}

			if (primary) {
				BeaconScreen.this.primaryEffect = effect;
			} else {
				BeaconScreen.this.secondaryEffect = effect;
			}

			BeaconScreen.this.tickButtons();
		}

		@Override
		protected void renderExtra(DrawContext context) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, sprite, getX() + 2, getY() + 2, 18, 18);
		}

		@Override
		public void tick(int level) {
			active = this.level < level;
			setDisabled(effect.equals(primary ? BeaconScreen.this.primaryEffect : BeaconScreen.this.secondaryEffect));
		}

		@Override
		protected MutableText getNarrationMessage() {
			return getEffectName(effect);
		}
	}

	/**
	 * {@code IconButtonWidget} — кнопка маяка с иконкой-текстурой.
	 */
	@Environment(EnvType.CLIENT)
	abstract static class IconButtonWidget extends BeaconScreen.BaseButtonWidget {

		private final Identifier texture;

		protected IconButtonWidget(int x, int y, Identifier identifier, Text message) {
			super(x, y, message);
			setTooltip(Tooltip.of(message));
			texture = identifier;
		}

		@Override
		protected void renderExtra(DrawContext context) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, texture, getX() + 2, getY() + 2, 18, 18);
		}
	}

	/**
	 * {@code LevelTwoEffectButtonWidget} — кнопка усиленного (II уровня) вторичного эффекта маяка.
	 * Отображается только если выбран первичный эффект.
	 */
	@Environment(EnvType.CLIENT)
	class LevelTwoEffectButtonWidget extends BeaconScreen.EffectButtonWidget {

		public LevelTwoEffectButtonWidget(final int x, final int y, final RegistryEntry<StatusEffect> effect) {
			super(x, y, effect, false, SECONDARY_LEVEL);
		}

		@Override
		protected MutableText getEffectName(RegistryEntry<StatusEffect> effect) {
			return Text.translatable(effect.value().getTranslationKey()).append(" II");
		}

		@Override
		public void tick(int level) {
			if (BeaconScreen.this.primaryEffect != null) {
				visible = true;
				init(BeaconScreen.this.primaryEffect);
				super.tick(level);
			} else {
				visible = false;
			}
		}
	}
}

package net.minecraft.client.realms.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.client.realms.dto.RealmsSlot;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.realms.util.RealmsTextureManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.jspecify.annotations.Nullable;

/**
 * Кнопка слота мира на экране настройки Realms-сервера.
 * Отображает превью мира (текстуру, имя, версию) и управляет переключением активного слота.
 * Слот с индексом {@code 4} считается слотом мини-игры.
 */
@Environment(EnvType.CLIENT)
public class RealmsWorldSlotButton extends ButtonWidget {

	private static final Identifier SLOT_FRAME = Identifier.ofVanilla("widget/slot_frame");
	public static final Identifier EMPTY_FRAME = Identifier.ofVanilla("textures/gui/realms/empty_frame.png");
	public static final Identifier PANORAMA_0 = Identifier.ofVanilla("textures/gui/title/background/panorama_0.png");
	public static final Identifier PANORAMA_2 = Identifier.ofVanilla("textures/gui/title/background/panorama_2.png");
	public static final Identifier PANORAMA_3 = Identifier.ofVanilla("textures/gui/title/background/panorama_3.png");
	private static final net.minecraft.text.Text MINIGAME_TOOLTIP = net.minecraft.text.Text.translatable("mco.configure.world.slot.tooltip.minigame");
	private static final net.minecraft.text.Text TOOLTIP = net.minecraft.text.Text.translatable("mco.configure.world.slot.tooltip");
	static final net.minecraft.text.Text MINIGAME_SLOT_NAME = net.minecraft.text.Text.translatable("mco.worldSlot.minigame");
	private static final int MAX_DISPLAYED_SLOT_NAME_LENGTH = 64;
	private static final String ELLIPSIS = "...";
	private static final int MINIGAME_SLOT_INDEX = 4;
	private static final long NO_IMAGE_ID = -1L;
	private static final int COLOR_INACTIVE = ColorHelper.fromFloats(1.0F, 0.56F, 0.56F, 0.56F);
	private static final int COLOR_ACTIVE_DIM = ColorHelper.fromFloats(1.0F, 0.8F, 0.8F, 0.8F);
	private static final int COLOR_WHITE = -1;

	private final int slotIndex;
	private RealmsWorldSlotButton.State state;

	public RealmsWorldSlotButton(
		int x,
		int y,
		int width,
		int height,
		int slotIndex,
		RealmsServer server,
		ButtonWidget.PressAction onPress
	) {
		super(x, y, width, height, ScreenTexts.EMPTY, onPress, DEFAULT_NARRATION_SUPPLIER);
		this.slotIndex = slotIndex;
		state = setServer(server);
	}

	public RealmsWorldSlotButton.State getState() {
		return state;
	}

	public RealmsWorldSlotButton.State setServer(RealmsServer server) {
		state = new RealmsWorldSlotButton.State(server, slotIndex);
		updateTooltip(state, server.minigameName);
		return state;
	}

	private void updateTooltip(RealmsWorldSlotButton.State buttonState, @Nullable String minigameName) {
		net.minecraft.text.Text tooltipText = switch (buttonState.action) {
			case SWITCH_SLOT -> buttonState.minigame ? MINIGAME_TOOLTIP : TOOLTIP;
			default -> null;
		};

		if (tooltipText != null) {
			setTooltip(Tooltip.of(tooltipText));
		}

		net.minecraft.text.MutableText label = net.minecraft.text.Text.literal(buttonState.slotName);

		if (buttonState.minigame && minigameName != null) {
			label = label.append(ScreenTexts.SPACE).append(minigameName);
		}

		setMessage(label);
	}

	static RealmsWorldSlotButton.Action getAction(boolean isActive, boolean isEmpty, boolean isExpired) {
		return isActive || isEmpty && isExpired
			? RealmsWorldSlotButton.Action.NOTHING
			: RealmsWorldSlotButton.Action.SWITCH_SLOT;
	}

	@Override
	public boolean isInteractable() {
		return state.action != RealmsWorldSlotButton.Action.NOTHING && super.isInteractable();
	}

	@Override
	public void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		int x = getX();
		int y = getY();
		boolean hovered = isSelected();

		Identifier texture = resolveTexture();
		int tintColor = state.active ? COLOR_WHITE : COLOR_INACTIVE;

		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			texture,
			x + 1,
			y + 1,
			0.0F,
			0.0F,
			width - 2,
			height - 2,
			74,
			74,
			74,
			74,
			tintColor
		);

		drawFrame(context, x, y, hovered);
		drawHardcoreIcon(context, x, y);
		drawSlotName(context, x, y);
		drawVersionText(context, x, y);
	}

	private Identifier resolveTexture() {
		if (state.minigame) {
			return RealmsTextureManager.getTextureId(String.valueOf(state.imageId), state.image);
		}

		if (state.empty) {
			return EMPTY_FRAME;
		}

		if (state.image != null && state.imageId != NO_IMAGE_ID) {
			return RealmsTextureManager.getTextureId(String.valueOf(state.imageId), state.image);
		}

		return switch (slotIndex) {
			case 1 -> PANORAMA_0;
			case 2 -> PANORAMA_2;
			case 3 -> PANORAMA_3;
			default -> EMPTY_FRAME;
		};
	}

	private void drawFrame(DrawContext context, int x, int y, boolean hovered) {
		if (hovered && state.action != RealmsWorldSlotButton.Action.NOTHING) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SLOT_FRAME, x, y, width, height);
		} else if (state.active) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SLOT_FRAME, x, y, width, height, COLOR_ACTIVE_DIM);
		} else {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SLOT_FRAME, x, y, width, height, COLOR_INACTIVE);
		}
	}

	private void drawHardcoreIcon(DrawContext context, int x, int y) {
		if (!state.hardcore) {
			return;
		}

		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, RealmsMainScreen.HARDCORE_ICON_TEXTURE, x + 3, y + 4, 9, 8);
	}

	private void drawSlotName(DrawContext context, int x, int y) {
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		String name = state.slotName;

		if (textRenderer.getWidth(name) > MAX_DISPLAYED_SLOT_NAME_LENGTH) {
			int maxWidth = MAX_DISPLAYED_SLOT_NAME_LENGTH - textRenderer.getWidth(ELLIPSIS);
			name = textRenderer.trimToWidth(name, maxWidth) + ELLIPSIS;
		}

		context.drawCenteredTextWithShadow(textRenderer, name, x + width / 2, y + height - 14, COLOR_WHITE);
	}

	private void drawVersionText(DrawContext context, int x, int y) {
		if (!state.active) {
			return;
		}

		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		net.minecraft.text.Text versionText = RealmsMainScreen.getVersionText(state.version, state.compatibility.isCompatible());
		context.drawCenteredTextWithShadow(textRenderer, versionText, x + width / 2, y + height + 2, COLOR_WHITE);
	}

	@Environment(EnvType.CLIENT)
	public enum Action {
		NOTHING,
		SWITCH_SLOT
	}

	/**
	 * Снимок состояния слота мира для одного кадра отрисовки.
	 * Вычисляется из {@link RealmsServer} и индекса слота при каждом обновлении сервера.
	 */
	@Environment(EnvType.CLIENT)
	public static class State {

		final String slotName;
		final String version;
		final RealmsServer.Compatibility compatibility;
		final long imageId;
		final @Nullable String image;
		public final boolean empty;
		public final boolean minigame;
		public final RealmsWorldSlotButton.Action action;
		public final boolean hardcore;
		public final boolean active;

		public State(RealmsServer server, int slot) {
			minigame = slot == MINIGAME_SLOT_INDEX;

			if (minigame) {
				slotName = RealmsWorldSlotButton.MINIGAME_SLOT_NAME.getString();
				imageId = server.minigameId;
				image = server.minigameImage;
				empty = server.minigameId == NO_IMAGE_ID;
				version = "";
				compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
				hardcore = false;
				active = server.isMinigame();
			} else {
				RealmsSlot realmsSlot = server.slots.get(slot);
				slotName = realmsSlot.options.getSlotName(slot);
				imageId = realmsSlot.options.templateId;
				image = realmsSlot.options.templateImage;
				empty = realmsSlot.options.empty;
				version = realmsSlot.options.version;
				compatibility = realmsSlot.options.compatibility;
				hardcore = realmsSlot.isHardcore();
				active = server.activeSlot == slot && !server.isMinigame();
			}

			action = RealmsWorldSlotButton.getAction(active, empty, server.expired);
		}
	}
}

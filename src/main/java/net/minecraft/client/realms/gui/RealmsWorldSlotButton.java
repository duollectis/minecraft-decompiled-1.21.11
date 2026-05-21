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
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsWorldSlotButton}.
 */
public class RealmsWorldSlotButton extends ButtonWidget {

	private static final Identifier SLOT_FRAME = Identifier.ofVanilla("widget/slot_frame");
	public static final Identifier EMPTY_FRAME = Identifier.ofVanilla("textures/gui/realms/empty_frame.png");
	public static final Identifier PANORAMA_0 = Identifier.ofVanilla("textures/gui/title/background/panorama_0.png");
	public static final Identifier PANORAMA_2 = Identifier.ofVanilla("textures/gui/title/background/panorama_2.png");
	public static final Identifier PANORAMA_3 = Identifier.ofVanilla("textures/gui/title/background/panorama_3.png");
	private static final net.minecraft.text.Text
			MINIGAME_TOOLTIP =
			net.minecraft.text.Text.translatable("mco.configure.world.slot.tooltip.minigame");
	private static final net.minecraft.text.Text
			TOOLTIP =
			net.minecraft.text.Text.translatable("mco.configure.world.slot.tooltip");
	static final net.minecraft.text.Text
			MINIGAME_SLOT_NAME =
			net.minecraft.text.Text.translatable("mco.worldSlot.minigame");
	private static final int MAX_DISPLAYED_SLOT_NAME_LENGTH = 64;
	private static final String ELLIPSIS = "...";
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
		this.state = this.setServer(server);
	}

	public RealmsWorldSlotButton.State getState() {
		return this.state;
	}

	public RealmsWorldSlotButton.State setServer(RealmsServer server) {
		this.state = new RealmsWorldSlotButton.State(server, this.slotIndex);
		this.updateTooltip(this.state, server.minigameName);
		return this.state;
	}

	private void updateTooltip(RealmsWorldSlotButton.State state, @Nullable String minigameName) {
		net.minecraft.text.Text text = switch (state.action) {
			case SWITCH_SLOT -> state.minigame ? MINIGAME_TOOLTIP : TOOLTIP;
			default -> null;
		};
		if (text != null) {
			this.setTooltip(Tooltip.of(text));
		}

		MutableText mutableText = net.minecraft.text.Text.literal(state.slotName);
		if (state.minigame && minigameName != null) {
			mutableText = mutableText.append(ScreenTexts.SPACE).append(minigameName);
		}

		this.setMessage(mutableText);
	}

	static RealmsWorldSlotButton.Action getAction(boolean bl, boolean active, boolean bl2) {
		return bl || active && bl2 ? RealmsWorldSlotButton.Action.NOTHING : RealmsWorldSlotButton.Action.SWITCH_SLOT;
	}

	@Override
	public boolean isInteractable() {
		return this.state.action != RealmsWorldSlotButton.Action.NOTHING && super.isInteractable();
	}

	@Override
	public void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		int i = this.getX();
		int j = this.getY();
		boolean bl = this.isSelected();
		Identifier identifier;
		if (this.state.minigame) {
			identifier = RealmsTextureManager.getTextureId(String.valueOf(this.state.imageId), this.state.image);
		}
		else if (this.state.empty) {
			identifier = EMPTY_FRAME;
		}
		else if (this.state.image != null && this.state.imageId != -1L) {
			identifier = RealmsTextureManager.getTextureId(String.valueOf(this.state.imageId), this.state.image);
		}
		else if (this.slotIndex == 1) {
			identifier = PANORAMA_0;
		}
		else if (this.slotIndex == 2) {
			identifier = PANORAMA_2;
		}
		else if (this.slotIndex == 3) {
			identifier = PANORAMA_3;
		}
		else {
			identifier = EMPTY_FRAME;
		}

		int k = -1;
		if (!this.state.active) {
			k = ColorHelper.fromFloats(1.0F, 0.56F, 0.56F, 0.56F);
		}

		context.drawTexture(
				RenderPipelines.GUI_TEXTURED,
				identifier,
				i + 1,
				j + 1,
				0.0F,
				0.0F,
				this.width - 2,
				this.height - 2,
				74,
				74,
				74,
				74,
				k
		);
		if (bl && this.state.action != RealmsWorldSlotButton.Action.NOTHING) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SLOT_FRAME, i, j, this.width, this.height);
		}
		else if (this.state.active) {
			context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					SLOT_FRAME,
					i,
					j,
					this.width,
					this.height,
					ColorHelper.fromFloats(1.0F, 0.8F, 0.8F, 0.8F)
			);
		}
		else {
			context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					SLOT_FRAME,
					i,
					j,
					this.width,
					this.height,
					ColorHelper.fromFloats(1.0F, 0.56F, 0.56F, 0.56F)
			);
		}

		if (this.state.hardcore) {
			context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					RealmsMainScreen.HARDCORE_ICON_TEXTURE,
					i + 3,
					j + 4,
					9,
					8
			);
		}

		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		String string = this.state.slotName;
		if (textRenderer.getWidth(string) > 64) {
			string = textRenderer.trimToWidth(string, 64 - textRenderer.getWidth("...")) + "...";
		}

		context.drawCenteredTextWithShadow(textRenderer, string, i + this.width / 2, j + this.height - 14, -1);
		if (this.state.active) {
			context.drawCenteredTextWithShadow(
					textRenderer,
					RealmsMainScreen.getVersionText(this.state.version, this.state.compatibility.isCompatible()),
					i + this.width / 2,
					j + this.height + 2,
					-1
			);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Action}.
	 */
	public static enum Action {
		NOTHING,
		SWITCH_SLOT;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code State}.
	 */
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
			this.minigame = slot == 4;
			if (this.minigame) {
				this.slotName = RealmsWorldSlotButton.MINIGAME_SLOT_NAME.getString();
				this.imageId = server.minigameId;
				this.image = server.minigameImage;
				this.empty = server.minigameId == -1;
				this.version = "";
				this.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
				this.hardcore = false;
				this.active = server.isMinigame();
			}
			else {
				RealmsSlot realmsSlot = server.slots.get(slot);
				this.slotName = realmsSlot.options.getSlotName(slot);
				this.imageId = realmsSlot.options.templateId;
				this.image = realmsSlot.options.templateImage;
				this.empty = realmsSlot.options.empty;
				this.version = realmsSlot.options.version;
				this.compatibility = realmsSlot.options.compatibility;
				this.hardcore = realmsSlot.isHardcore();
				this.active = server.activeSlot == slot && !server.isMinigame();
			}

			this.action = RealmsWorldSlotButton.getAction(this.active, this.empty, server.expired);
		}
	}
}

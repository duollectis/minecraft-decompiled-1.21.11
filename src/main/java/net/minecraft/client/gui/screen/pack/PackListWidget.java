package net.minecraft.client.gui.screen.pack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.client.gui.widget.SquareWidgetEntry;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.resource.ResourcePackCompatibility;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Виджет списка пакетов ресурсов — отображает доступные или выбранные паки
 * с иконками и кнопками управления (включить, отключить, переместить).
 */
@Environment(EnvType.CLIENT)
public class PackListWidget extends AlwaysSelectedEntryListWidget<PackListWidget.Entry> {

	static final Identifier SELECT_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("transferable_list/select_highlighted");
	static final Identifier SELECT_TEXTURE = Identifier.ofVanilla("transferable_list/select");
	static final Identifier UNSELECT_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("transferable_list/unselect_highlighted");
	static final Identifier UNSELECT_TEXTURE = Identifier.ofVanilla("transferable_list/unselect");
	static final Identifier MOVE_UP_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("transferable_list/move_up_highlighted");
	static final Identifier MOVE_UP_TEXTURE = Identifier.ofVanilla("transferable_list/move_up");
	static final Identifier MOVE_DOWN_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("transferable_list/move_down_highlighted");
	static final Identifier MOVE_DOWN_TEXTURE = Identifier.ofVanilla("transferable_list/move_down");
	static final Text INCOMPATIBLE = Text.translatable("pack.incompatible");
	static final Text INCOMPATIBLE_CONFIRM = Text.translatable("pack.incompatible.confirm.title");
	private static final int SCROLLBAR_WIDTH = 2;

	private final Text title;
	final PackScreen screen;

	public PackListWidget(MinecraftClient client, PackScreen screen, int width, int height, Text title) {
		super(client, width, height, 33, 36);
		this.screen = screen;
		this.title = title;
		centerListVertically = false;
	}

	@Override
	public int getRowWidth() {
		return width - 4;
	}

	@Override
	protected int getScrollbarX() {
		return getRight() - 6;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		Entry selected = getSelectedOrNull();
		return selected != null ? selected.keyPressed(input) : super.keyPressed(input);
	}

	/**
	 * Обновляет содержимое списка — устанавливает заголовок и добавляет записи паков.
	 * Если передан {@code focused}, фокусирует соответствующую запись.
	 */
	public void set(Stream<ResourcePackOrganizer.Pack> packs, ResourcePackOrganizer.@Nullable AbstractPack focused) {
		clearEntries();
		Text headerText = Text.empty().append(title).formatted(Formatting.UNDERLINE, Formatting.BOLD);
		addEntry(new PackListWidget.HeaderEntry(client.textRenderer, headerText), (int) (9.0F * 1.5F));
		setSelected(null);
		packs.forEach(pack -> {
			PackListWidget.ResourcePackEntry entry = new PackListWidget.ResourcePackEntry(client, this, pack);
			addEntry(entry);
			if (focused != null && focused.getName().equals(pack.getName())) {
				screen.setFocused(this);
				setFocused(entry);
			}
		});
		refreshScroll();
	}

	/**
	 * Базовый тип записи списка паков.
	 */
	@Environment(EnvType.CLIENT)
	public abstract class Entry extends AlwaysSelectedEntryListWidget.Entry<PackListWidget.Entry> {

		@Override
		public int getWidth() {
			return super.getWidth() - (PackListWidget.this.overflows() ? 6 : 0);
		}

		public abstract String getName();
	}

	/**
	 * Запись-заголовок списка паков.
	 */
	@Environment(EnvType.CLIENT)
	public class HeaderEntry extends PackListWidget.Entry {

		private final TextRenderer textRenderer;
		private final Text text;

		public HeaderEntry(final TextRenderer textRenderer, final Text text) {
			this.textRenderer = textRenderer;
			this.text = text;
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			context.drawCenteredTextWithShadow(
					textRenderer,
					text,
					getX() + getWidth() / 2,
					getContentMiddleY() - 9 / 2,
					-1
			);
		}

		@Override
		public Text getNarration() {
			return text;
		}

		@Override
		public String getName() {
			return "";
		}
	}

	/**
	 * Запись одного пакета ресурсов — отображает иконку, название, описание
	 * и кнопки управления при наведении.
	 */
	@Environment(EnvType.CLIENT)
	public class ResourcePackEntry extends PackListWidget.Entry implements SquareWidgetEntry {

		private static final int ENTRY_WIDTH = 157;
		public static final int ICON_SIZE = 32;
		private static final int COLOR_INCOMPATIBLE_BG = -8978432;
		private static final int COLOR_HOVER_OVERLAY = -1601138544;
		private static final int COLOR_DESCRIPTION_TEXT = -8355712;

		private final PackListWidget widget;
		protected final MinecraftClient client;
		private final ResourcePackOrganizer.Pack pack;
		private final TextWidget nameWidget;
		private final MultilineTextWidget descriptionWidget;

		public ResourcePackEntry(
				final MinecraftClient client,
				final PackListWidget widget,
				final ResourcePackOrganizer.Pack pack
		) {
			this.client = client;
			this.pack = pack;
			this.widget = widget;
			nameWidget = new TextWidget(pack.getDisplayName(), client.textRenderer);
			descriptionWidget = new MultilineTextWidget(
					Texts.withStyle(pack.getDecoratedDescription(), Style.EMPTY.withColor(COLOR_DESCRIPTION_TEXT)),
					client.textRenderer
			);
			descriptionWidget.setMaxRows(2);
		}

		@Override
		public Text getNarration() {
			return Text.translatable("narrator.select", pack.getDisplayName());
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			ResourcePackCompatibility compatibility = pack.getCompatibility();
			if (!compatibility.isCompatible()) {
				context.fill(
						getContentX() - 1,
						getContentY() - 1,
						getContentRightEnd() + 1,
						getContentBottomEnd() + 1,
						COLOR_INCOMPATIBLE_BG
				);
			}

			context.drawTexture(
					RenderPipelines.GUI_TEXTURED,
					pack.getIconId(),
					getContentX(),
					getContentY(),
					0.0F,
					0.0F,
					ICON_SIZE,
					ICON_SIZE,
					ICON_SIZE,
					ICON_SIZE
			);
			if (!nameWidget.getMessage().equals(pack.getDisplayName())) {
				nameWidget.setMessage(pack.getDisplayName());
			}

			if (!descriptionWidget.getMessage().getContent().equals(pack.getDecoratedDescription().getContent())) {
				descriptionWidget.setMessage(
						Texts.withStyle(pack.getDecoratedDescription(), Style.EMPTY.withColor(COLOR_DESCRIPTION_TEXT))
				);
			}

			if (isSelectable()
					&& (client.options.getTouchscreen().getValue()
					|| hovered
					|| widget.getSelectedOrNull() == this && widget.isFocused())
			) {
				context.fill(
						getContentX(),
						getContentY(),
						getContentX() + ICON_SIZE,
						getContentY() + ICON_SIZE,
						COLOR_HOVER_OVERLAY
				);
				int relX = mouseX - getContentX();
				int relY = mouseY - getContentY();

				if (!pack.getCompatibility().isCompatible()) {
					nameWidget.setMessage(PackListWidget.INCOMPATIBLE);
					descriptionWidget.setMessage(pack.getCompatibility().getNotification());
				}

				if (pack.canBeEnabled()) {
					if (isInside(relX, relY, ICON_SIZE)) {
						context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, PackListWidget.SELECT_HIGHLIGHTED_TEXTURE, getContentX(), getContentY(), ICON_SIZE, ICON_SIZE);
						PackListWidget.this.setCursor(context);
					} else {
						context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, PackListWidget.SELECT_TEXTURE, getContentX(), getContentY(), ICON_SIZE, ICON_SIZE);
					}
				} else {
					if (pack.canBeDisabled()) {
						if (isLeft(relX, relY, ICON_SIZE)) {
							context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, PackListWidget.UNSELECT_HIGHLIGHTED_TEXTURE, getContentX(), getContentY(), ICON_SIZE, ICON_SIZE);
							PackListWidget.this.setCursor(context);
						} else {
							context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, PackListWidget.UNSELECT_TEXTURE, getContentX(), getContentY(), ICON_SIZE, ICON_SIZE);
						}
					}

					if (pack.canMoveTowardStart()) {
						if (isBottomRight(relX, relY, ICON_SIZE)) {
							context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, PackListWidget.MOVE_UP_HIGHLIGHTED_TEXTURE, getContentX(), getContentY(), ICON_SIZE, ICON_SIZE);
							PackListWidget.this.setCursor(context);
						} else {
							context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, PackListWidget.MOVE_UP_TEXTURE, getContentX(), getContentY(), ICON_SIZE, ICON_SIZE);
						}
					}

					if (pack.canMoveTowardEnd()) {
						if (isTopRight(relX, relY, ICON_SIZE)) {
							context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, PackListWidget.MOVE_DOWN_HIGHLIGHTED_TEXTURE, getContentX(), getContentY(), ICON_SIZE, ICON_SIZE);
							PackListWidget.this.setCursor(context);
						} else {
							context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, PackListWidget.MOVE_DOWN_TEXTURE, getContentX(), getContentY(), ICON_SIZE, ICON_SIZE);
						}
					}
				}
			}

			int overflowOffset = PackListWidget.this.overflows() ? 6 : 0;
			nameWidget.setMaxWidth(ENTRY_WIDTH - overflowOffset);
			nameWidget.setPosition(getContentX() + ICON_SIZE + 2, getContentY() + 1);
			nameWidget.render(context, mouseX, mouseY, deltaTicks);
			descriptionWidget.setMaxWidth(ENTRY_WIDTH - overflowOffset);
			descriptionWidget.setPosition(getContentX() + ICON_SIZE + 2, getContentY() + 12);
			descriptionWidget.render(context, mouseX, mouseY, deltaTicks);
		}

		@Override
		public boolean mouseClicked(Click click, boolean doubled) {
			if (!isSelectable()) {
				return super.mouseClicked(click, doubled);
			}

			int relX = (int) click.x() - getContentX();
			int relY = (int) click.y() - getContentY();

			if (pack.canBeEnabled() && isInside(relX, relY, ICON_SIZE)) {
				enable();
				return true;
			}

			if (pack.canBeDisabled() && isLeft(relX, relY, ICON_SIZE)) {
				pack.disable();
				return true;
			}

			if (pack.canMoveTowardStart() && isBottomRight(relX, relY, ICON_SIZE)) {
				pack.moveTowardStart();
				return true;
			}

			if (pack.canMoveTowardEnd() && isTopRight(relX, relY, ICON_SIZE)) {
				pack.moveTowardEnd();
				return true;
			}

			return super.mouseClicked(click, doubled);
		}

		@Override
		public boolean keyPressed(KeyInput input) {
			if (input.isEnter()) {
				toggle();
				return true;
			}

			if (input.hasShift()) {
				if (input.isUp()) {
					moveTowardStart();
					return true;
				}

				if (input.isDown()) {
					moveTowardEnd();
					return true;
				}
			}

			return super.keyPressed(input);
		}

		private boolean isSelectable() {
			return !pack.isPinned() || !pack.isAlwaysEnabled();
		}

		public void toggle() {
			if (pack.canBeEnabled()) {
				enable();
			} else if (pack.canBeDisabled()) {
				pack.disable();
			}
		}

		private void moveTowardStart() {
			if (pack.canMoveTowardStart()) {
				pack.moveTowardStart();
			}
		}

		private void moveTowardEnd() {
			if (pack.canMoveTowardEnd()) {
				pack.moveTowardEnd();
			}
		}

		private void enable() {
			if (pack.getCompatibility().isCompatible()) {
				pack.enable();
				return;
			}

			Text confirmMessage = pack.getCompatibility().getConfirmMessage();
			client.setScreen(new ConfirmScreen(
					confirmed -> {
						client.setScreen(widget.screen);
						if (confirmed) {
							pack.enable();
						}
					},
					PackListWidget.INCOMPATIBLE_CONFIRM,
					confirmMessage
			));
		}

		@Override
		public String getName() {
			return pack.getName();
		}

		@Override
		public boolean isClickable() {
			return PackListWidget.this.children().stream().anyMatch(entry -> entry.getName().equals(getName()));
		}
	}
}

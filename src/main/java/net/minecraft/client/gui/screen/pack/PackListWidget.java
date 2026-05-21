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

@Environment(EnvType.CLIENT)
/**
 * {@code PackListWidget}.
 */
public class PackListWidget extends AlwaysSelectedEntryListWidget<PackListWidget.Entry> {

	static final Identifier SELECT_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("transferable_list/select_highlighted");
	static final Identifier SELECT_TEXTURE = Identifier.ofVanilla("transferable_list/select");
	static final Identifier
			UNSELECT_HIGHLIGHTED_TEXTURE =
			Identifier.ofVanilla("transferable_list/unselect_highlighted");
	static final Identifier UNSELECT_TEXTURE = Identifier.ofVanilla("transferable_list/unselect");
	static final Identifier MOVE_UP_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("transferable_list/move_up_highlighted");
	static final Identifier MOVE_UP_TEXTURE = Identifier.ofVanilla("transferable_list/move_up");
	static final Identifier
			MOVE_DOWN_HIGHLIGHTED_TEXTURE =
			Identifier.ofVanilla("transferable_list/move_down_highlighted");
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
		this.centerListVertically = false;
	}

	@Override
	public int getRowWidth() {
		return this.width - 4;
	}

	@Override
	protected int getScrollbarX() {
		return this.getRight() - 6;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		return this.getSelectedOrNull() != null ? this.getSelectedOrNull().keyPressed(input) : super.keyPressed(input);
	}

	/**
	 * Set.
	 *
	 * @param packs packs
	 * @param focused focused
	 */
	public void set(Stream<ResourcePackOrganizer.Pack> packs, ResourcePackOrganizer.@Nullable AbstractPack focused) {
		this.clearEntries();
		Text text = Text.empty().append(this.title).formatted(Formatting.UNDERLINE, Formatting.BOLD);
		this.addEntry(new PackListWidget.HeaderEntry(this.client.textRenderer, text), (int) (9.0F * 1.5F));
		this.setSelected(null);
		packs.forEach(pack -> {
			PackListWidget.ResourcePackEntry
					resourcePackEntry =
					new PackListWidget.ResourcePackEntry(this.client, this, pack);
			this.addEntry(resourcePackEntry);
			if (focused != null && focused.getName().equals(pack.getName())) {
				this.screen.setFocused(this);
				this.setFocused(resourcePackEntry);
			}
		});
		this.refreshScroll();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Entry}.
	 */
	public abstract class Entry extends AlwaysSelectedEntryListWidget.Entry<PackListWidget.Entry> {

		@Override
		public int getWidth() {
			return super.getWidth() - (PackListWidget.this.overflows() ? 6 : 0);
		}

		public abstract String getName();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code HeaderEntry}.
	 */
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
					this.textRenderer,
					this.text,
					this.getX() + this.getWidth() / 2,
					this.getContentMiddleY() - 9 / 2,
					-1
			);
		}

		@Override
		public Text getNarration() {
			return this.text;
		}

		@Override
		public String getName() {
			return "";
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ResourcePackEntry}.
	 */
	public class ResourcePackEntry extends PackListWidget.Entry implements SquareWidgetEntry {

		private static final int ENTRY_WIDTH = 157;
		public static final int ICON_SIZE = 32;
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
			this.nameWidget = new TextWidget(pack.getDisplayName(), client.textRenderer);
			this.descriptionWidget =
					new MultilineTextWidget(
							Texts.withStyle(
									pack.getDecoratedDescription(),
									Style.EMPTY.withColor(-8355712)
							), client.textRenderer
					);
			this.descriptionWidget.setMaxRows(2);
		}

		@Override
		public Text getNarration() {
			return Text.translatable("narrator.select", this.pack.getDisplayName());
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			ResourcePackCompatibility resourcePackCompatibility = this.pack.getCompatibility();
			if (!resourcePackCompatibility.isCompatible()) {
				int i = this.getContentX() - 1;
				int j = this.getContentY() - 1;
				int k = this.getContentRightEnd() + 1;
				int l = this.getContentBottomEnd() + 1;
				context.fill(i, j, k, l, -8978432);
			}

			context.drawTexture(
					RenderPipelines.GUI_TEXTURED,
					this.pack.getIconId(),
					this.getContentX(),
					this.getContentY(),
					0.0F,
					0.0F,
					32,
					32,
					32,
					32
			);
			if (!this.nameWidget.getMessage().equals(this.pack.getDisplayName())) {
				this.nameWidget.setMessage(this.pack.getDisplayName());
			}

			if (!this.descriptionWidget
					.getMessage()
					.getContent()
					.equals(this.pack.getDecoratedDescription().getContent())) {
				this.descriptionWidget.setMessage(Texts.withStyle(
						this.pack.getDecoratedDescription(),
						Style.EMPTY.withColor(-8355712)
				));
			}

			if (this.isSelectable()
					&& (this.client.options.getTouchscreen().getValue() || hovered
					|| this.widget.getSelectedOrNull() == this && this.widget.isFocused()
			)) {
				context.fill(
						this.getContentX(),
						this.getContentY(),
						this.getContentX() + 32,
						this.getContentY() + 32,
						-1601138544
				);
				int i = mouseX - this.getContentX();
				int j = mouseY - this.getContentY();
				if (!this.pack.getCompatibility().isCompatible()) {
					this.nameWidget.setMessage(PackListWidget.INCOMPATIBLE);
					this.descriptionWidget.setMessage(this.pack.getCompatibility().getNotification());
				}

				if (this.pack.canBeEnabled()) {
					if (this.isInside(i, j, 32)) {
						context.drawGuiTexture(
								RenderPipelines.GUI_TEXTURED,
								PackListWidget.SELECT_HIGHLIGHTED_TEXTURE,
								this.getContentX(),
								this.getContentY(),
								32,
								32
						);
						PackListWidget.this.setCursor(context);
					}
					else {
						context.drawGuiTexture(
								RenderPipelines.GUI_TEXTURED,
								PackListWidget.SELECT_TEXTURE,
								this.getContentX(),
								this.getContentY(),
								32,
								32
						);
					}
				}
				else {
					if (this.pack.canBeDisabled()) {
						if (this.isLeft(i, j, 32)) {
							context.drawGuiTexture(
									RenderPipelines.GUI_TEXTURED,
									PackListWidget.UNSELECT_HIGHLIGHTED_TEXTURE,
									this.getContentX(),
									this.getContentY(),
									32,
									32
							);
							PackListWidget.this.setCursor(context);
						}
						else {
							context.drawGuiTexture(
									RenderPipelines.GUI_TEXTURED,
									PackListWidget.UNSELECT_TEXTURE,
									this.getContentX(),
									this.getContentY(),
									32,
									32
							);
						}
					}

					if (this.pack.canMoveTowardStart()) {
						if (this.isBottomRight(i, j, 32)) {
							context.drawGuiTexture(
									RenderPipelines.GUI_TEXTURED,
									PackListWidget.MOVE_UP_HIGHLIGHTED_TEXTURE,
									this.getContentX(),
									this.getContentY(),
									32,
									32
							);
							PackListWidget.this.setCursor(context);
						}
						else {
							context.drawGuiTexture(
									RenderPipelines.GUI_TEXTURED,
									PackListWidget.MOVE_UP_TEXTURE,
									this.getContentX(),
									this.getContentY(),
									32,
									32
							);
						}
					}

					if (this.pack.canMoveTowardEnd()) {
						if (this.isTopRight(i, j, 32)) {
							context.drawGuiTexture(
									RenderPipelines.GUI_TEXTURED,
									PackListWidget.MOVE_DOWN_HIGHLIGHTED_TEXTURE,
									this.getContentX(),
									this.getContentY(),
									32,
									32
							);
							PackListWidget.this.setCursor(context);
						}
						else {
							context.drawGuiTexture(
									RenderPipelines.GUI_TEXTURED,
									PackListWidget.MOVE_DOWN_TEXTURE,
									this.getContentX(),
									this.getContentY(),
									32,
									32
							);
						}
					}
				}
			}

			this.nameWidget.setMaxWidth(157 - (PackListWidget.this.overflows() ? 6 : 0));
			this.nameWidget.setPosition(this.getContentX() + 32 + 2, this.getContentY() + 1);
			this.nameWidget.render(context, mouseX, mouseY, deltaTicks);
			this.descriptionWidget.setMaxWidth(157 - (PackListWidget.this.overflows() ? 6 : 0));
			this.descriptionWidget.setPosition(this.getContentX() + 32 + 2, this.getContentY() + 12);
			this.descriptionWidget.render(context, mouseX, mouseY, deltaTicks);
		}

		@Override
		public boolean mouseClicked(Click click, boolean doubled) {
			if (this.isSelectable()) {
				int i = (int) click.x() - this.getContentX();
				int j = (int) click.y() - this.getContentY();
				if (this.pack.canBeEnabled() && this.isInside(i, j, 32)) {
					this.enable();
					return true;
				}

				if (this.pack.canBeDisabled() && this.isLeft(i, j, 32)) {
					this.pack.disable();
					return true;
				}

				if (this.pack.canMoveTowardStart() && this.isBottomRight(i, j, 32)) {
					this.pack.moveTowardStart();
					return true;
				}

				if (this.pack.canMoveTowardEnd() && this.isTopRight(i, j, 32)) {
					this.pack.moveTowardEnd();
					return true;
				}
			}

			return super.mouseClicked(click, doubled);
		}

		@Override
		public boolean keyPressed(KeyInput input) {
			if (input.isEnter()) {
				this.toggle();
				return true;
			}
			else {
				if (input.hasShift()) {
					if (input.isUp()) {
						this.moveTowardStart();
						return true;
					}

					if (input.isDown()) {
						this.moveTowardEnd();
						return true;
					}
				}

				return super.keyPressed(input);
			}
		}

		private boolean isSelectable() {
			return !this.pack.isPinned() || !this.pack.isAlwaysEnabled();
		}

		/**
		 * Toggle.
		 */
		public void toggle() {
			if (this.pack.canBeEnabled()) {
				this.enable();
			}
			else if (this.pack.canBeDisabled()) {
				this.pack.disable();
			}
		}

		private void moveTowardStart() {
			if (this.pack.canMoveTowardStart()) {
				this.pack.moveTowardStart();
			}
		}

		private void moveTowardEnd() {
			if (this.pack.canMoveTowardEnd()) {
				this.pack.moveTowardEnd();
			}
		}

		private void enable() {
			if (this.pack.getCompatibility().isCompatible()) {
				this.pack.enable();
			}
			else {
				Text text = this.pack.getCompatibility().getConfirmMessage();
				this.client.setScreen(new ConfirmScreen(
						confirmed -> {
							this.client.setScreen(this.widget.screen);
							if (confirmed) {
								this.pack.enable();
							}
						}, PackListWidget.INCOMPATIBLE_CONFIRM, text
				));
			}
		}

		@Override
		public String getName() {
			return this.pack.getName();
		}

		@Override
		public boolean isClickable() {
			return PackListWidget.this.children().stream().anyMatch(entry -> entry.getName().equals(this.getName()));
		}
	}
}

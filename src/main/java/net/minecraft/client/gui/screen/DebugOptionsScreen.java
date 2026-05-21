package net.minecraft.client.gui.screen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.floats.FloatComparators;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.hud.debug.*;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.*;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
/**
 * {@code DebugOptionsScreen}.
 */
public class DebugOptionsScreen extends Screen {

	private static final Text TITLE = Text.translatable("debug.options.title");
	private static final Text WARNING_TEXT = Text.translatable("debug.options.warning").withColor(-2142128);
	static final Text ALWAYS_ON_TEXT = Text.translatable("debug.entry.always");
	static final Text IN_F3_TEXT = Text.translatable("debug.entry.overlay");
	static final Text NEVER_TEXT = ScreenTexts.OFF;
	static final Text NOT_ALLOWED_TEXT = Text.translatable("debug.options.notAllowed.tooltip");
	private static final Text
			SEARCH_TEXT =
			Text.translatable("debug.options.search").fillStyle(TextFieldWidget.SEARCH_STYLE);
	final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this, 61, 33);
	private DebugOptionsScreen.@Nullable OptionsListWidget optionsListWidget;
	private TextFieldWidget searchStringWidget;
	final List<ButtonWidget> profileButtons = new ArrayList<>();

	public DebugOptionsScreen() {
		super(TITLE);
	}

	@Override
	protected void init() {
		DirectionalLayoutWidget
				directionalLayoutWidget =
				this.layout.addHeader(DirectionalLayoutWidget.vertical().spacing(8));
		this.optionsListWidget = new DebugOptionsScreen.OptionsListWidget();
		int i = this.optionsListWidget.getRowWidth();
		DirectionalLayoutWidget directionalLayoutWidget2 = DirectionalLayoutWidget.horizontal().spacing(8);
		directionalLayoutWidget2.add(new EmptyWidget(i / 3, 1));
		directionalLayoutWidget2.add(
				new TextWidget(TITLE, this.textRenderer),
				directionalLayoutWidget2.copyPositioner().alignVerticalCenter()
		);
		this.searchStringWidget =
				new TextFieldWidget(this.textRenderer, 0, 0, i / 3, 20, this.searchStringWidget, SEARCH_TEXT);
		this.searchStringWidget.setChangedListener(searchString -> this.optionsListWidget.fillEntries(searchString));
		this.searchStringWidget.setPlaceholder(SEARCH_TEXT);
		directionalLayoutWidget2.add(this.searchStringWidget);
		directionalLayoutWidget.add(directionalLayoutWidget2, Positioner::alignHorizontalCenter);
		directionalLayoutWidget.add(
				new MultilineTextWidget(WARNING_TEXT, this.textRenderer)
						.setMaxWidth(i)
						.setCentered(true), Positioner::alignHorizontalCenter
		);
		this.layout.addBody(this.optionsListWidget);
		DirectionalLayoutWidget
				directionalLayoutWidget3 =
				this.layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
		this.addProfile(DebugProfileType.DEFAULT, directionalLayoutWidget3);
		this.addProfile(DebugProfileType.PERFORMANCE, directionalLayoutWidget3);
		directionalLayoutWidget3.add(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close()).width(60).build());
		this.layout.forEachChild(widget -> {
			ClickableWidget var10000 = this.addDrawableChild(widget);
		});
		this.refreshWidgetPositions();
	}

	@Override
	public void applyBlur(DrawContext context) {
		this.client.inGameHud.renderDebugHud(context);
		super.applyBlur(context);
	}

	@Override
	protected void setInitialFocus() {
		this.setInitialFocus(this.searchStringWidget);
	}

	private void addProfile(DebugProfileType profileType, DirectionalLayoutWidget widget) {
		ButtonWidget buttonWidget = ButtonWidget.builder(
				Text.translatable(profileType.getTranslationKey()), button -> {
					this.client.debugHudEntryList.setProfileType(profileType);
					this.client.debugHudEntryList.saveProfileFile();
					this.optionsListWidget.init();

					for (ButtonWidget buttonWidgetx : this.profileButtons) {
						buttonWidgetx.active = true;
					}

					button.active = false;
				}
		).width(120).build();
		buttonWidget.active = !this.client.debugHudEntryList.profileTypeMatches(profileType);
		this.profileButtons.add(buttonWidget);
		widget.add(buttonWidget);
	}

	@Override
	protected void refreshWidgetPositions() {
		this.layout.refreshPositions();
		if (this.optionsListWidget != null) {
			this.optionsListWidget.position(this.width, this.layout);
		}
	}

	public DebugOptionsScreen.@Nullable OptionsListWidget getOptionsListWidget() {
		return this.optionsListWidget;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code AbstractEntry}.
	 */
	public abstract static class AbstractEntry extends ElementListWidget.Entry<DebugOptionsScreen.AbstractEntry> {

		/**
		 * Init.
		 */
		public abstract void init();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Category}.
	 */
	class Category extends DebugOptionsScreen.AbstractEntry {

		final Text label;

		public Category(final Text label) {
			this.label = label;
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			context.drawCenteredTextWithShadow(
					DebugOptionsScreen.this.client.textRenderer,
					this.label,
					this.getContentX() + this.getContentWidth() / 2,
					this.getContentY() + 5,
					-1
			);
		}

		@Override
		public List<? extends Element> children() {
			return ImmutableList.of();
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return ImmutableList.of(new Selectable() {
				@Override
				public Selectable.SelectionType getType() {
					return Selectable.SelectionType.HOVERED;
				}

				@Override
				public void appendNarrations(NarrationMessageBuilder builder) {
					builder.put(NarrationPart.TITLE, Category.this.label);
				}
			});
		}

		@Override
		public void init() {
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Entry}.
	 */
	class Entry extends DebugOptionsScreen.AbstractEntry {

		private static final int BUTTON_WIDTH = 60;
		private final Identifier label;
		protected final List<ClickableWidget> widgets = Lists.newArrayList();
		private final CyclingButtonWidget<Boolean> alwaysOnButton;
		private final CyclingButtonWidget<Boolean> inF3Button;
		private final CyclingButtonWidget<Boolean> neverButton;
		private final String renderedLabel;
		private final boolean canShow;

		public Entry(final Identifier label) {
			this.label = label;
			DebugHudEntry debugHudEntry = DebugHudEntries.get(label);
			this.canShow =
					debugHudEntry != null
							&& debugHudEntry.canShow(DebugOptionsScreen.this.client.hasReducedDebugInfo());
			String string = label.getPath();
			if (this.canShow) {
				this.renderedLabel = string;
			}
			else {
				this.renderedLabel = Formatting.ITALIC + string;
			}

			this.alwaysOnButton = CyclingButtonWidget.onOffBuilder(
					                                         DebugOptionsScreen.ALWAYS_ON_TEXT.copy().withColor(-2142128),
					                                         DebugOptionsScreen.ALWAYS_ON_TEXT.copy().withColor(-4539718),
					                                         false
			                                         )
			                                         .omitKeyText()
			                                         .narration(this::getNarrationMessage)
			                                         .build(
					                                         10,
					                                         5,
					                                         60,
					                                         16,
					                                         Text.literal(string),
					                                         (button, value) -> this.setEntryVisibility(
							                                         label,
							                                         DebugHudEntryVisibility.ALWAYS_ON
					                                         )
			                                         );
			this.inF3Button = CyclingButtonWidget.onOffBuilder(
					                                     DebugOptionsScreen.IN_F3_TEXT.copy().withColor(-171),
					                                     DebugOptionsScreen.IN_F3_TEXT.copy().withColor(-4539718),
					                                     false
			                                     )
			                                     .omitKeyText()
			                                     .narration(this::getNarrationMessage)
			                                     .build(
					                                     10,
					                                     5,
					                                     60,
					                                     16,
					                                     Text.literal(string),
					                                     (button, value) -> this.setEntryVisibility(
							                                     label,
							                                     DebugHudEntryVisibility.IN_OVERLAY
					                                     )
			                                     );
			this.neverButton = CyclingButtonWidget.onOffBuilder(
					                                      DebugOptionsScreen.NEVER_TEXT.copy().withColor(-1),
					                                      DebugOptionsScreen.NEVER_TEXT.copy().withColor(-4539718),
					                                      false
			                                      )
			                                      .omitKeyText()
			                                      .narration(this::getNarrationMessage)
			                                      .build(
					                                      10,
					                                      5,
					                                      60,
					                                      16,
					                                      Text.literal(string),
					                                      (button, value) -> this.setEntryVisibility(
							                                      label,
							                                      DebugHudEntryVisibility.NEVER
					                                      )
			                                      );
			this.widgets.add(this.neverButton);
			this.widgets.add(this.inF3Button);
			this.widgets.add(this.alwaysOnButton);
			this.init();
		}

		private MutableText getNarrationMessage(CyclingButtonWidget<Boolean> widget) {
			DebugHudEntryVisibility
					debugHudEntryVisibility =
					DebugOptionsScreen.this.client.debugHudEntryList.getVisibility(this.label);
			MutableText
					mutableText =
					Text.translatable(
							"debug.entry.currently." + debugHudEntryVisibility.asString(),
							this.renderedLabel
					);
			return ScreenTexts.composeGenericOptionText(mutableText, widget.getMessage());
		}

		private void setEntryVisibility(Identifier label, DebugHudEntryVisibility visibility) {
			DebugOptionsScreen.this.client.debugHudEntryList.setEntryVisibility(label, visibility);

			for (ButtonWidget buttonWidget : DebugOptionsScreen.this.profileButtons) {
				buttonWidget.active = true;
			}

			this.init();
		}

		@Override
		public List<? extends Element> children() {
			return this.widgets;
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return this.widgets;
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int i = this.getContentX();
			int j = this.getContentY();
			context.drawTextWithShadow(
					DebugOptionsScreen.this.client.textRenderer,
					this.renderedLabel,
					i,
					j + 5,
					this.canShow ? -1 : -8355712
			);
			int
					k =
					i + this.getContentWidth() - this.neverButton.getWidth() - this.inF3Button.getWidth()
							- this.alwaysOnButton.getWidth();
			if (!this.canShow && hovered && mouseX < k) {
				context.drawTooltip(DebugOptionsScreen.NOT_ALLOWED_TEXT, mouseX, mouseY);
			}

			this.neverButton.setX(k);
			this.inF3Button.setX(this.neverButton.getX() + this.neverButton.getWidth());
			this.alwaysOnButton.setX(this.inF3Button.getX() + this.inF3Button.getWidth());
			this.alwaysOnButton.setY(j);
			this.inF3Button.setY(j);
			this.neverButton.setY(j);
			this.alwaysOnButton.render(context, mouseX, mouseY, deltaTicks);
			this.inF3Button.render(context, mouseX, mouseY, deltaTicks);
			this.neverButton.render(context, mouseX, mouseY, deltaTicks);
		}

		@Override
		public void init() {
			DebugHudEntryVisibility
					debugHudEntryVisibility =
					DebugOptionsScreen.this.client.debugHudEntryList.getVisibility(this.label);
			this.alwaysOnButton.setValue(debugHudEntryVisibility == DebugHudEntryVisibility.ALWAYS_ON);
			this.inF3Button.setValue(debugHudEntryVisibility == DebugHudEntryVisibility.IN_OVERLAY);
			this.neverButton.setValue(debugHudEntryVisibility == DebugHudEntryVisibility.NEVER);
			this.alwaysOnButton.active = !this.alwaysOnButton.getValue();
			this.inF3Button.active = !this.inF3Button.getValue();
			this.neverButton.active = !this.neverButton.getValue();
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code OptionsListWidget}.
	 */
	public class OptionsListWidget extends ElementListWidget<DebugOptionsScreen.AbstractEntry> {

		private static final Comparator<Map.Entry<Identifier, DebugHudEntry>> ENTRY_COMPARATOR = (a, b) -> {
			int
					i =
					FloatComparators.NATURAL_COMPARATOR.compare(
							a.getValue().getCategory().sortKey(),
							b.getValue().getCategory().sortKey()
					);
			return i != 0 ? i : a.getKey().compareTo(b.getKey());
		};
		private static final int ITEM_HEIGHT = 20;

		public OptionsListWidget() {
			super(
					MinecraftClient.getInstance(),
					DebugOptionsScreen.this.width,
					DebugOptionsScreen.this.layout.getContentHeight(),
					DebugOptionsScreen.this.layout.getHeaderHeight(),
					20
			);
			this.fillEntries("");
		}

		@Override
		public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
			super.renderWidget(context, mouseX, mouseY, deltaTicks);
		}

		@Override
		public int getRowWidth() {
			return 350;
		}

		/**
		 * Init.
		 */
		public void init() {
			this.children().forEach(DebugOptionsScreen.AbstractEntry::init);
		}

		/**
		 * Fill entries.
		 *
		 * @param searchString search string
		 */
		public void fillEntries(String searchString) {
			this.clearEntries();
			List<Map.Entry<Identifier, DebugHudEntry>> list = new ArrayList<>(DebugHudEntries.getEntries().entrySet());
			list.sort(ENTRY_COMPARATOR);
			DebugHudEntryCategory debugHudEntryCategory = null;

			for (Map.Entry<Identifier, DebugHudEntry> entry : list) {
				if (entry.getKey().getPath().contains(searchString)) {
					DebugHudEntryCategory debugHudEntryCategory2 = entry.getValue().getCategory();
					if (!debugHudEntryCategory2.equals(debugHudEntryCategory)) {
						this.addEntry(DebugOptionsScreen.this.new Category(debugHudEntryCategory2.label()));
						debugHudEntryCategory = debugHudEntryCategory2;
					}

					this.addEntry(DebugOptionsScreen.this.new Entry(entry.getKey()));
				}
			}

			this.refreshScreen();
		}

		private void refreshScreen() {
			this.refreshScroll();
			DebugOptionsScreen.this.narrateScreenIfNarrationEnabled(true);
		}
	}
}

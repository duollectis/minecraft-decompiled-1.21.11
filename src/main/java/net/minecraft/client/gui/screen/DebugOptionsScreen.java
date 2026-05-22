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

/**
 * Экран настройки видимости элементов отладочного HUD (F3).
 * Позволяет задать для каждого элемента режим: всегда, только в F3-оверлее или никогда.
 */
@Environment(EnvType.CLIENT)
public class DebugOptionsScreen extends Screen {

	private static final Text TITLE = Text.translatable("debug.options.title");
	private static final Text WARNING_TEXT = Text.translatable("debug.options.warning").withColor(-2142128);
	static final Text ALWAYS_ON_TEXT = Text.translatable("debug.entry.always");
	static final Text IN_F3_TEXT = Text.translatable("debug.entry.overlay");
	static final Text NEVER_TEXT = ScreenTexts.OFF;
	static final Text NOT_ALLOWED_TEXT = Text.translatable("debug.options.notAllowed.tooltip");
	private static final Text SEARCH_TEXT = Text.translatable("debug.options.search").fillStyle(TextFieldWidget.SEARCH_STYLE);

	final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this, 61, 33);
	final List<ButtonWidget> profileButtons = new ArrayList<>();
	private @Nullable OptionsListWidget optionsListWidget;
	private TextFieldWidget searchStringWidget;

	public DebugOptionsScreen() {
		super(TITLE);
	}

	@Override
	protected void init() {
		DirectionalLayoutWidget headerLayout = layout.addHeader(DirectionalLayoutWidget.vertical().spacing(8));
		optionsListWidget = new OptionsListWidget();

		int rowWidth = optionsListWidget.getRowWidth();
		DirectionalLayoutWidget titleRow = DirectionalLayoutWidget.horizontal().spacing(8);
		titleRow.add(new EmptyWidget(rowWidth / 3, 1));
		titleRow.add(new TextWidget(TITLE, textRenderer), titleRow.copyPositioner().alignVerticalCenter());

		searchStringWidget = new TextFieldWidget(textRenderer, 0, 0, rowWidth / 3, 20, searchStringWidget, SEARCH_TEXT);
		searchStringWidget.setChangedListener(searchString -> optionsListWidget.fillEntries(searchString));
		searchStringWidget.setPlaceholder(SEARCH_TEXT);
		titleRow.add(searchStringWidget);

		headerLayout.add(titleRow, Positioner::alignHorizontalCenter);
		headerLayout.add(
			new MultilineTextWidget(WARNING_TEXT, textRenderer).setMaxWidth(rowWidth).setCentered(true),
			Positioner::alignHorizontalCenter
		);

		layout.addBody(optionsListWidget);

		DirectionalLayoutWidget footerRow = layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
		addProfile(DebugProfileType.DEFAULT, footerRow);
		addProfile(DebugProfileType.PERFORMANCE, footerRow);
		footerRow.add(ButtonWidget.builder(ScreenTexts.DONE, button -> close()).width(200).build());

		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	@Override
	public void applyBlur(DrawContext context) {
		client.inGameHud.renderDebugHud(context);
		super.applyBlur(context);
	}

	@Override
	protected void setInitialFocus() {
		setInitialFocus(searchStringWidget);
	}

	private void addProfile(DebugProfileType profileType, DirectionalLayoutWidget widget) {
		ButtonWidget button = ButtonWidget.builder(
			Text.translatable(profileType.getTranslationKey()), pressed -> {
				client.debugHudEntryList.setProfileType(profileType);
				client.debugHudEntryList.saveProfileFile();
				optionsListWidget.init();

				for (ButtonWidget profileButton : profileButtons) {
					profileButton.active = true;
				}

				pressed.active = false;
			}
		).width(120).build();

		button.active = !client.debugHudEntryList.profileTypeMatches(profileType);
		profileButtons.add(button);
		widget.add(button);
	}

	@Override
	protected void refreshWidgetPositions() {
		layout.refreshPositions();

		if (optionsListWidget != null) {
			optionsListWidget.position(width, layout);
		}
	}

	public @Nullable OptionsListWidget getOptionsListWidget() {
		return optionsListWidget;
	}

	@Environment(EnvType.CLIENT)
	public abstract static class AbstractEntry extends ElementListWidget.Entry<AbstractEntry> {

		public abstract void init();
	}

	@Environment(EnvType.CLIENT)
	class Category extends AbstractEntry {

		final Text label;

		public Category(Text label) {
			this.label = label;
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			context.drawCenteredTextWithShadow(
				DebugOptionsScreen.this.client.textRenderer,
				label,
				getContentX() + getContentWidth() / 2,
				getContentY() + 5,
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
	class Entry extends AbstractEntry {

		private static final int ENTRY_BUTTON_WIDTH = 60;
		private static final int ENTRY_BUTTON_HEIGHT = 16;
		private static final int ENTRY_BUTTON_PADDING = 5;

		private final Identifier label;
		protected final List<ClickableWidget> widgets = Lists.newArrayList();
		private final CyclingButtonWidget<Boolean> alwaysOnButton;
		private final CyclingButtonWidget<Boolean> inF3Button;
		private final CyclingButtonWidget<Boolean> neverButton;
		private final String renderedLabel;
		private final boolean canShow;

		public Entry(Identifier label) {
			this.label = label;
			DebugHudEntry debugHudEntry = DebugHudEntries.get(label);
			canShow = debugHudEntry != null && debugHudEntry.canShow(DebugOptionsScreen.this.client.hasReducedDebugInfo());

			String path = label.getPath();
			renderedLabel = canShow ? path : Formatting.ITALIC + path;

			alwaysOnButton = CyclingButtonWidget.onOffBuilder(
				DebugOptionsScreen.ALWAYS_ON_TEXT.copy().withColor(-2142128),
				DebugOptionsScreen.ALWAYS_ON_TEXT.copy().withColor(-4539718),
				false
			)
				.omitKeyText()
				.narration(this::getNarrationMessage)
				.build(10, ENTRY_BUTTON_PADDING, ENTRY_BUTTON_WIDTH, ENTRY_BUTTON_HEIGHT, Text.literal(path),
					(button, value) -> setEntryVisibility(label, DebugHudEntryVisibility.ALWAYS_ON));

			inF3Button = CyclingButtonWidget.onOffBuilder(
				DebugOptionsScreen.IN_F3_TEXT.copy().withColor(-171),
				DebugOptionsScreen.IN_F3_TEXT.copy().withColor(-4539718),
				false
			)
				.omitKeyText()
				.narration(this::getNarrationMessage)
				.build(10, ENTRY_BUTTON_PADDING, ENTRY_BUTTON_WIDTH, ENTRY_BUTTON_HEIGHT, Text.literal(path),
					(button, value) -> setEntryVisibility(label, DebugHudEntryVisibility.IN_OVERLAY));

			neverButton = CyclingButtonWidget.onOffBuilder(
				DebugOptionsScreen.NEVER_TEXT.copy().withColor(-1),
				DebugOptionsScreen.NEVER_TEXT.copy().withColor(-4539718),
				false
			)
				.omitKeyText()
				.narration(this::getNarrationMessage)
				.build(10, ENTRY_BUTTON_PADDING, ENTRY_BUTTON_WIDTH, ENTRY_BUTTON_HEIGHT, Text.literal(path),
					(button, value) -> setEntryVisibility(label, DebugHudEntryVisibility.NEVER));

			widgets.add(neverButton);
			widgets.add(inF3Button);
			widgets.add(alwaysOnButton);
			init();
		}

		private MutableText getNarrationMessage(CyclingButtonWidget<Boolean> widget) {
			DebugHudEntryVisibility visibility = DebugOptionsScreen.this.client.debugHudEntryList.getVisibility(label);
			MutableText current = Text.translatable("debug.entry.currently." + visibility.asString(), renderedLabel);
			return ScreenTexts.composeGenericOptionText(current, widget.getMessage());
		}

		private void setEntryVisibility(Identifier entryLabel, DebugHudEntryVisibility visibility) {
			DebugOptionsScreen.this.client.debugHudEntryList.setEntryVisibility(entryLabel, visibility);

			for (ButtonWidget profileButton : DebugOptionsScreen.this.profileButtons) {
				profileButton.active = true;
			}

			init();
		}

		@Override
		public List<? extends Element> children() {
			return widgets;
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return widgets;
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int contentX = getContentX();
			int contentY = getContentY();

			context.drawTextWithShadow(
				DebugOptionsScreen.this.client.textRenderer,
				renderedLabel,
				contentX,
				contentY + 5,
				canShow ? -1 : -8355712
			);

			int buttonsX = contentX + getContentWidth() - neverButton.getWidth() - inF3Button.getWidth() - alwaysOnButton.getWidth();

			if (!canShow && hovered && mouseX < buttonsX) {
				context.drawTooltip(DebugOptionsScreen.NOT_ALLOWED_TEXT, mouseX, mouseY);
			}

			neverButton.setX(buttonsX);
			inF3Button.setX(neverButton.getX() + neverButton.getWidth());
			alwaysOnButton.setX(inF3Button.getX() + inF3Button.getWidth());
			alwaysOnButton.setY(contentY);
			inF3Button.setY(contentY);
			neverButton.setY(contentY);
			alwaysOnButton.render(context, mouseX, mouseY, deltaTicks);
			inF3Button.render(context, mouseX, mouseY, deltaTicks);
			neverButton.render(context, mouseX, mouseY, deltaTicks);
		}

		@Override
		public void init() {
			DebugHudEntryVisibility visibility = DebugOptionsScreen.this.client.debugHudEntryList.getVisibility(label);
			alwaysOnButton.setValue(visibility == DebugHudEntryVisibility.ALWAYS_ON);
			inF3Button.setValue(visibility == DebugHudEntryVisibility.IN_OVERLAY);
			neverButton.setValue(visibility == DebugHudEntryVisibility.NEVER);
			alwaysOnButton.active = !alwaysOnButton.getValue();
			inF3Button.active = !inF3Button.getValue();
			neverButton.active = !neverButton.getValue();
		}
	}

	@Environment(EnvType.CLIENT)
	public class OptionsListWidget extends ElementListWidget<AbstractEntry> {

		private static final Comparator<Map.Entry<Identifier, DebugHudEntry>> ENTRY_COMPARATOR = (a, b) -> {
			int categoryOrder = FloatComparators.NATURAL_COMPARATOR.compare(
				a.getValue().getCategory().sortKey(),
				b.getValue().getCategory().sortKey()
			);
			return categoryOrder != 0 ? categoryOrder : a.getKey().compareTo(b.getKey());
		};
		private static final int ITEM_HEIGHT = 20;

		public OptionsListWidget() {
			super(
				MinecraftClient.getInstance(),
				DebugOptionsScreen.this.width,
				DebugOptionsScreen.this.layout.getContentHeight(),
				DebugOptionsScreen.this.layout.getHeaderHeight(),
				ITEM_HEIGHT
			);
			fillEntries("");
		}

		@Override
		public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
			super.renderWidget(context, mouseX, mouseY, deltaTicks);
		}

		@Override
		public int getRowWidth() {
			return 350;
		}

		public void init() {
			children().forEach(AbstractEntry::init);
		}

		public void fillEntries(String searchString) {
			clearEntries();
			List<Map.Entry<Identifier, DebugHudEntry>> entries = new ArrayList<>(DebugHudEntries.getEntries().entrySet());
			entries.sort(ENTRY_COMPARATOR);
			DebugHudEntryCategory currentCategory = null;

			for (Map.Entry<Identifier, DebugHudEntry> entry : entries) {
				if (!entry.getKey().getPath().contains(searchString)) {
					continue;
				}

				DebugHudEntryCategory entryCategory = entry.getValue().getCategory();

				if (!entryCategory.equals(currentCategory)) {
					addEntry(DebugOptionsScreen.this.new Category(entryCategory.label()));
					currentCategory = entryCategory;
				}

				addEntry(DebugOptionsScreen.this.new Entry(entry.getKey()));
			}

			refreshScreen();
		}

		private void refreshScreen() {
			refreshScroll();
			DebugOptionsScreen.this.narrateScreenIfNarrationEnabled(true);
		}
	}
}

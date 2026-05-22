package net.minecraft.client.gui.screen.option;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.SortedMap;

/**
 * Экран выбора языка — отображает список доступных языков с поиском
 * и позволяет переключить язык интерфейса.
 */
@Environment(EnvType.CLIENT)
public class LanguageOptionsScreen extends GameOptionsScreen {

	private static final Text LANGUAGE_WARNING_TEXT =
			Text.translatable("options.languageAccuracyWarning").withColor(-4539718);
	private static final int LIST_HEIGHT = 53;
	private static final Text SEARCH_TEXT =
			Text.translatable("gui.language.search").fillStyle(TextFieldWidget.SEARCH_STYLE);
	private static final int SEARCH_FIELD_HEIGHT = 15;
	private static final int HEADER_HEIGHT = (int) (12.0 + 9.0 + 15.0);

	final LanguageManager languageManager;
	private LanguageOptionsScreen.@Nullable LanguageSelectionListWidget languageSelectionList;
	private @Nullable TextFieldWidget searchBox;

	public LanguageOptionsScreen(Screen parent, GameOptions options, LanguageManager languageManager) {
		super(parent, options, Text.translatable("options.language.title"));
		this.languageManager = languageManager;
		layout.setFooterHeight(LIST_HEIGHT);
	}

	@Override
	protected void initHeader() {
		DirectionalLayoutWidget headerLayout = layout.addHeader(DirectionalLayoutWidget.vertical().spacing(4));
		headerLayout.getMainPositioner().alignHorizontalCenter();
		headerLayout.add(new TextWidget(title, textRenderer));
		searchBox = headerLayout.add(new TextFieldWidget(textRenderer, 0, 0, 200, SEARCH_FIELD_HEIGHT, Text.empty()));
		searchBox.setPlaceholder(SEARCH_TEXT);
		searchBox.setChangedListener(search -> {
			if (languageSelectionList != null) {
				languageSelectionList.setSearch(search);
			}
		});
		layout.setHeaderHeight(HEADER_HEIGHT);
	}

	@Override
	protected void setInitialFocus() {
		if (searchBox != null) {
			setInitialFocus(searchBox);
		} else {
			super.setInitialFocus();
		}
	}

	@Override
	protected void initBody() {
		languageSelectionList = layout.addBody(new LanguageOptionsScreen.LanguageSelectionListWidget(client));
	}

	@Override
	protected void addOptions() {
	}

	@Override
	protected void initFooter() {
		DirectionalLayoutWidget footerLayout = layout.addFooter(DirectionalLayoutWidget.vertical()).spacing(8);
		footerLayout.getMainPositioner().alignHorizontalCenter();
		footerLayout.add(new TextWidget(LANGUAGE_WARNING_TEXT, textRenderer));
		DirectionalLayoutWidget buttonRow = footerLayout.add(DirectionalLayoutWidget.horizontal().spacing(8));
		buttonRow.add(ButtonWidget
				.builder(
						Text.translatable("options.font"),
						button -> client.setScreen(new FontOptionsScreen(this, gameOptions))
				)
				.build()
		);
		buttonRow.add(ButtonWidget.builder(ScreenTexts.DONE, button -> onDone()).build());
	}

	@Override
	protected void refreshWidgetPositions() {
		super.refreshWidgetPositions();
		if (languageSelectionList != null) {
			languageSelectionList.position(width, layout);
		}
	}

	void onDone() {
		if (languageSelectionList != null
				&& languageSelectionList.getSelectedOrNull() instanceof LanguageOptionsScreen.LanguageSelectionListWidget.LanguageEntry languageEntry
				&& !languageEntry.languageCode.equals(languageManager.getLanguage())
		) {
			languageManager.setLanguage(languageEntry.languageCode);
			gameOptions.language = languageEntry.languageCode;
			client.reloadResources();
		}

		client.setScreen(parent);
	}

	@Override
	protected boolean allowRotatingPanorama() {
		return !(parent instanceof AccessibilityOnboardingScreen);
	}

	/**
	 * Виджет списка языков с поддержкой поиска по названию и региону.
	 */
	@Environment(EnvType.CLIENT)
	class LanguageSelectionListWidget extends AlwaysSelectedEntryListWidget<LanguageOptionsScreen.LanguageSelectionListWidget.LanguageEntry> {

		public LanguageSelectionListWidget(final MinecraftClient client) {
			super(client, LanguageOptionsScreen.this.width, LanguageOptionsScreen.this.height - 33 - LIST_HEIGHT, 33, 18);
			String currentLanguage = LanguageOptionsScreen.this.languageManager.getLanguage();
			LanguageOptionsScreen.this.languageManager
					.getAllLanguages()
					.forEach(
							(languageCode, languageDefinition) -> {
								LanguageEntry languageEntry = new LanguageEntry(languageCode, languageDefinition);
								addEntry(languageEntry);
								if (currentLanguage.equals(languageCode)) {
									setSelected(languageEntry);
								}
							}
					);
			if (getSelectedOrNull() != null) {
				centerScrollOn(getSelectedOrNull());
			}
		}

		/**
		 * Фильтрует список языков по строке поиска (по названию и региону, без учёта регистра).
		 */
		void setSearch(String search) {
			SortedMap<String, LanguageDefinition> allLanguages = LanguageOptionsScreen.this.languageManager.getAllLanguages();
			List<LanguageEntry> filtered = allLanguages.entrySet()
					.stream()
					.filter(entry -> {
						if (search.isEmpty()) {
							return true;
						}

						String lowerSearch = search.toLowerCase(Locale.ROOT);
						boolean nameMatches = entry.getValue().name().toLowerCase(Locale.ROOT).contains(lowerSearch);
						boolean regionMatches = entry.getValue().region().toLowerCase(Locale.ROOT).contains(lowerSearch);
						return nameMatches || regionMatches;
					})
					.map(entry -> new LanguageEntry(entry.getKey(), entry.getValue()))
					.toList();
			replaceEntries(filtered);
			refreshScroll();
		}

		@Override
		public int getRowWidth() {
			return super.getRowWidth() + 50;
		}

		/**
		 * Запись одного языка в списке.
		 */
		@Environment(EnvType.CLIENT)
		public class LanguageEntry extends AlwaysSelectedEntryListWidget.Entry<LanguageOptionsScreen.LanguageSelectionListWidget.LanguageEntry> {

			final String languageCode;
			private final Text languageDisplayText;

			public LanguageEntry(final String languageCode, final LanguageDefinition languageDefinition) {
				this.languageCode = languageCode;
				languageDisplayText = languageDefinition.getDisplayText();
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
				context.drawCenteredTextWithShadow(
						LanguageOptionsScreen.this.textRenderer,
						languageDisplayText,
						LanguageSelectionListWidget.this.width / 2,
						getContentMiddleY() - 9 / 2,
						-1
				);
			}

			@Override
			public boolean keyPressed(KeyInput input) {
				if (input.isEnterOrSpace()) {
					onPressed();
					LanguageOptionsScreen.this.onDone();
					return true;
				}

				return super.keyPressed(input);
			}

			@Override
			public boolean mouseClicked(Click click, boolean doubled) {
				onPressed();
				if (doubled) {
					LanguageOptionsScreen.this.onDone();
				}

				return super.mouseClicked(click, doubled);
			}

			private void onPressed() {
				LanguageSelectionListWidget.this.setSelected(this);
			}

			@Override
			public Text getNarration() {
				return Text.translatable("narrator.select", languageDisplayText);
			}
		}
	}
}

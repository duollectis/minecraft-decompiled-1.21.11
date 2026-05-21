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

@Environment(EnvType.CLIENT)
/**
 * {@code LanguageOptionsScreen}.
 */
public class LanguageOptionsScreen extends GameOptionsScreen {

	private static final Text
			LANGUAGE_WARNING_TEXT =
			Text.translatable("options.languageAccuracyWarning").withColor(-4539718);
	private static final int LIST_HEIGHT = 53;
	private static final Text
			SEARCH_TEXT =
			Text.translatable("gui.language.search").fillStyle(TextFieldWidget.SEARCH_STYLE);
	private static final int SEARCH_FIELD_HEIGHT = 15;
	final LanguageManager languageManager;
	private LanguageOptionsScreen.@Nullable LanguageSelectionListWidget languageSelectionList;
	private @Nullable TextFieldWidget searchBox;

	public LanguageOptionsScreen(Screen parent, GameOptions options, LanguageManager languageManager) {
		super(parent, options, Text.translatable("options.language.title"));
		this.languageManager = languageManager;
		this.layout.setFooterHeight(53);
	}

	@Override
	protected void initHeader() {
		DirectionalLayoutWidget
				directionalLayoutWidget =
				this.layout.addHeader(DirectionalLayoutWidget.vertical().spacing(4));
		directionalLayoutWidget.getMainPositioner().alignHorizontalCenter();
		directionalLayoutWidget.add(new TextWidget(this.title, this.textRenderer));
		this.searchBox =
				directionalLayoutWidget.add(new TextFieldWidget(this.textRenderer, 0, 0, 200, 15, Text.empty()));
		this.searchBox.setPlaceholder(SEARCH_TEXT);
		this.searchBox.setChangedListener(search -> {
			if (this.languageSelectionList != null) {
				this.languageSelectionList.setSearch(search);
			}
		});
		this.layout.setHeaderHeight((int) (12.0 + 9.0 + 15.0));
	}

	@Override
	protected void setInitialFocus() {
		if (this.searchBox != null) {
			this.setInitialFocus(this.searchBox);
		}
		else {
			super.setInitialFocus();
		}
	}

	@Override
	protected void initBody() {
		this.languageSelectionList =
				this.layout.addBody(new LanguageOptionsScreen.LanguageSelectionListWidget(this.client));
	}

	@Override
	protected void addOptions() {
	}

	@Override
	protected void initFooter() {
		DirectionalLayoutWidget
				directionalLayoutWidget =
				this.layout.addFooter(DirectionalLayoutWidget.vertical()).spacing(8);
		directionalLayoutWidget.getMainPositioner().alignHorizontalCenter();
		directionalLayoutWidget.add(new TextWidget(LANGUAGE_WARNING_TEXT, this.textRenderer));
		DirectionalLayoutWidget
				directionalLayoutWidget2 =
				directionalLayoutWidget.add(DirectionalLayoutWidget.horizontal().spacing(8));
		directionalLayoutWidget2.add(
				ButtonWidget
						.builder(
								Text.translatable("options.font"),
								button -> this.client.setScreen(new FontOptionsScreen(this, this.gameOptions))
						)
						.build()
		);
		directionalLayoutWidget2.add(ButtonWidget.builder(ScreenTexts.DONE, button -> this.onDone()).build());
	}

	@Override
	protected void refreshWidgetPositions() {
		super.refreshWidgetPositions();
		if (this.languageSelectionList != null) {
			this.languageSelectionList.position(this.width, this.layout);
		}
	}

	void onDone() {
		if (this.languageSelectionList != null
				&& this.languageSelectionList.getSelectedOrNull() instanceof LanguageOptionsScreen.LanguageSelectionListWidget.LanguageEntry languageEntry
				&& !languageEntry.languageCode.equals(this.languageManager.getLanguage())) {
			this.languageManager.setLanguage(languageEntry.languageCode);
			this.gameOptions.language = languageEntry.languageCode;
			this.client.reloadResources();
		}

		this.client.setScreen(this.parent);
	}

	@Override
	protected boolean allowRotatingPanorama() {
		return !(this.parent instanceof AccessibilityOnboardingScreen);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code LanguageSelectionListWidget}.
	 */
	class LanguageSelectionListWidget extends AlwaysSelectedEntryListWidget<LanguageOptionsScreen.LanguageSelectionListWidget.LanguageEntry> {

		public LanguageSelectionListWidget(final MinecraftClient client) {
			super(client, LanguageOptionsScreen.this.width, LanguageOptionsScreen.this.height - 33 - 53, 33, 18);
			String string = LanguageOptionsScreen.this.languageManager.getLanguage();
			LanguageOptionsScreen.this.languageManager
					.getAllLanguages()
					.forEach(
							(languageCode, languageDefinition) -> {
								LanguageOptionsScreen.LanguageSelectionListWidget.LanguageEntry
										languageEntry =
										new LanguageOptionsScreen.LanguageSelectionListWidget.LanguageEntry(
												languageCode, languageDefinition
										);
								this.addEntry(languageEntry);
								if (string.equals(languageCode)) {
									this.setSelected(languageEntry);
								}
							}
					);
			if (this.getSelectedOrNull() != null) {
				this.centerScrollOn(this.getSelectedOrNull());
			}
		}

		void setSearch(String search) {
			SortedMap<String, LanguageDefinition>
					sortedMap =
					LanguageOptionsScreen.this.languageManager.getAllLanguages();
			List<LanguageOptionsScreen.LanguageSelectionListWidget.LanguageEntry> list = sortedMap.entrySet()
			                                                                                      .stream()
			                                                                                      .filter(
					                                                                                      entry ->
							                                                                                      search.isEmpty()
									                                                                                      || entry
									                                                                                      .getValue()
									                                                                                      .name()
									                                                                                      .toLowerCase(
											                                                                                      Locale.ROOT)
									                                                                                      .contains(
											                                                                                      search.toLowerCase(
													                                                                                      Locale.ROOT))
									                                                                                      || entry
									                                                                                      .getValue()
									                                                                                      .region()
									                                                                                      .toLowerCase(
											                                                                                      Locale.ROOT)
									                                                                                      .contains(
											                                                                                      search.toLowerCase(
													                                                                                      Locale.ROOT))
			                                                                                      )
			                                                                                      .map(entry -> new LanguageOptionsScreen.LanguageSelectionListWidget.LanguageEntry(
					                                                                                      entry.getKey(),
					                                                                                      entry.getValue()
			                                                                                      ))
			                                                                                      .toList();
			this.replaceEntries(list);
			this.refreshScroll();
		}

		@Override
		public int getRowWidth() {
			return super.getRowWidth() + 50;
		}

		@Environment(EnvType.CLIENT)
		/**
		 * {@code LanguageEntry}.
		 */
		public class LanguageEntry extends AlwaysSelectedEntryListWidget.Entry<LanguageOptionsScreen.LanguageSelectionListWidget.LanguageEntry> {

			final String languageCode;
			private final Text languageDefinition;

			public LanguageEntry(final String languageCode, final LanguageDefinition languageDefinition) {
				this.languageCode = languageCode;
				this.languageDefinition = languageDefinition.getDisplayText();
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
				context.drawCenteredTextWithShadow(
						LanguageOptionsScreen.this.textRenderer,
						this.languageDefinition,
						LanguageSelectionListWidget.this.width / 2,
						this.getContentMiddleY() - 9 / 2,
						-1
				);
			}

			@Override
			public boolean keyPressed(KeyInput input) {
				if (input.isEnterOrSpace()) {
					this.onPressed();
					LanguageOptionsScreen.this.onDone();
					return true;
				}
				else {
					return super.keyPressed(input);
				}
			}

			@Override
			public boolean mouseClicked(Click click, boolean doubled) {
				this.onPressed();
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
				return Text.translatable("narrator.select", this.languageDefinition);
			}
		}
	}
}

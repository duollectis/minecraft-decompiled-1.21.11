package net.minecraft.client.gui.screen.recipebook;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.navigation.NavigationAxis;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.RecipeCategoryOptionsC2SPacket;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.book.RecipeBookGroup;
import net.minecraft.recipe.book.RecipeBookType;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Виджет книги рецептов. Управляет отображением, поиском и фильтрацией рецептов,
 * а также вкладками по категориям и призрачными слотами крафта.
 */
@Environment(EnvType.CLIENT)
public abstract class RecipeBookWidget<T extends AbstractRecipeScreenHandler> implements Drawable, Element, Selectable {

	public static final ButtonTextures BUTTON_TEXTURES = new ButtonTextures(
			Identifier.ofVanilla("recipe_book/button"),
			Identifier.ofVanilla("recipe_book/button_highlighted")
	);
	protected static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/recipe_book.png");
	private static final int TEXTURE_WIDTH = 256;
	private static final int TEXTURE_HEIGHT = 256;
	private static final Text SEARCH_HINT_TEXT =
			Text.translatable("gui.recipebook.search_hint").fillStyle(TextFieldWidget.SEARCH_STYLE);
	private static final String PIRATE_SPEAK_LANGUAGE = "en_pt";
	private static final String PIRATE_SPEAK_EASTER_EGG_QUERY = "excitedze";
	public static final int WIDGET_WIDTH = 147;
	public static final int WIDGET_HEIGHT = 166;
	private static final int NARROW_WIDGET_WIDTH = 86;
	private static final int PADDING = 8;
	private static final Text TOGGLE_ALL_RECIPES_TEXT = Text.translatable("gui.recipebook.toggleRecipes.all");
	private static final int BUTTON_HEIGHT = 30;
	private static final int TAB_SPACING = 27;
	private static final int TAB_OFFSET_Y = 3;
	private static final int SEARCH_FIELD_WIDTH = 81;
	private static final int SEARCH_FIELD_HEIGHT = 14;
	private static final int SEARCH_FIELD_OFFSET_X = 25;
	private static final int SEARCH_FIELD_OFFSET_Y = 13;
	private static final int TOGGLE_BUTTON_OFFSET_X = 110;
	private static final int TOGGLE_BUTTON_OFFSET_Y = 12;
	private static final int TOGGLE_BUTTON_WIDTH = 26;
	private static final int TOGGLE_BUTTON_HEIGHT = 16;
	private static final int MAX_SEARCH_LENGTH = 50;

	private int leftOffset;
	private int parentWidth;
	private int parentHeight;
	private float displayTime;
	private @Nullable NetworkRecipeId selectedRecipeId;
	private final GhostRecipe ghostRecipe;
	private final List<RecipeGroupButtonWidget> tabButtons = Lists.newArrayList();
	private @Nullable RecipeGroupButtonWidget currentTab;
	protected CyclingButtonWidget<Boolean> toggleCraftableButton;
	protected final T craftingScreenHandler;
	protected MinecraftClient client;
	private @Nullable TextFieldWidget searchField;
	private String searchText = "";
	private final List<RecipeBookWidget.Tab> tabs;
	private ClientRecipeBook recipeBook;
	private final RecipeBookResults recipesArea;
	private @Nullable NetworkRecipeId selectedRecipe;
	private @Nullable RecipeResultCollection selectedRecipeResults;
	private final RecipeFinder recipeFinder = new RecipeFinder();
	private int cachedInvChangeCount;
	private boolean searching;
	private boolean open;
	private boolean narrow;
	private @Nullable ScreenRect searchFieldRect;

	public RecipeBookWidget(T craftingScreenHandler, List<RecipeBookWidget.Tab> tabs) {
		this.craftingScreenHandler = craftingScreenHandler;
		this.tabs = tabs;
		CurrentIndexProvider currentIndexProvider = () -> MathHelper.floor(displayTime / 30.0F);
		ghostRecipe = new GhostRecipe(currentIndexProvider);
		recipesArea = new RecipeBookResults(
				this,
				currentIndexProvider,
				craftingScreenHandler instanceof AbstractFurnaceScreenHandler
		);
	}

	public void initialize(int parentWidth, int parentHeight, MinecraftClient client, boolean narrow) {
		this.client = client;
		this.parentWidth = parentWidth;
		this.parentHeight = parentHeight;
		this.narrow = narrow;
		recipeBook = client.player.getRecipeBook();
		cachedInvChangeCount = client.player.getInventory().getChangeCount();
		open = isGuiOpen();

		if (open) {
			reset();
		}
	}

	private void reset() {
		boolean filteringCraftable = isFilteringCraftable();
		leftOffset = narrow ? 0 : NARROW_WIDGET_WIDTH;

		int left = getLeft();
		int top = getTop();

		recipeFinder.clear();
		client.player.getInventory().populateRecipeFinder(recipeFinder);
		craftingScreenHandler.populateRecipeFinder(recipeFinder);

		String previousSearch = searchField != null ? searchField.getText() : "";
		searchField = new TextFieldWidget(
				client.textRenderer,
				left + SEARCH_FIELD_OFFSET_X,
				top + SEARCH_FIELD_OFFSET_Y,
				SEARCH_FIELD_WIDTH,
				SEARCH_FIELD_HEIGHT - 1,
				Text.translatable("itemGroup.search")
		);
		searchField.setMaxLength(MAX_SEARCH_LENGTH);
		searchField.setVisible(true);
		searchField.setEditableColor(-1);
		searchField.setText(previousSearch);
		searchField.setPlaceholder(SEARCH_HINT_TEXT);

		searchFieldRect = ScreenRect.of(
				NavigationAxis.HORIZONTAL,
				left + PADDING,
				searchField.getY(),
				searchField.getX() - getLeft(),
				searchField.getHeight()
		);

		recipesArea.initialize(client, left, top);

		toggleCraftableButton = CyclingButtonWidget
				.onOffBuilder(getToggleCraftableButtonText(), TOGGLE_ALL_RECIPES_TEXT, filteringCraftable)
				.tooltip(value -> value ? Tooltip.of(getToggleCraftableButtonText()) : Tooltip.of(TOGGLE_ALL_RECIPES_TEXT))
				.icon((button, value) -> getBookButtonTextures().get(value, button.isSelected()))
				.labelType(CyclingButtonWidget.LabelType.HIDE)
				.build(
						left + TOGGLE_BUTTON_OFFSET_X,
						top + TOGGLE_BUTTON_OFFSET_Y,
						TOGGLE_BUTTON_WIDTH,
						TOGGLE_BUTTON_HEIGHT,
						ScreenTexts.EMPTY,
						(button, value) -> {
							toggleFilteringCraftable();
							sendBookDataPacket();
							refreshResults(false, value);
						}
				);

		tabButtons.clear();

		for (RecipeBookWidget.Tab tab : tabs) {
			tabButtons.add(new RecipeGroupButtonWidget(0, 0, tab, this::onTabSelected));
		}

		if (currentTab != null) {
			currentTab = tabButtons
					.stream()
					.filter(button -> button.getCategory().equals(currentTab.getCategory()))
					.findFirst()
					.orElse(null);
		}

		if (currentTab == null) {
			currentTab = tabButtons.get(0);
		}

		currentTab.focus();
		populateAllRecipes();
		refreshTabButtons(filteringCraftable);
		refreshResults(false, filteringCraftable);
	}

	private int getTop() {
		return (parentHeight - WIDGET_HEIGHT) / 2;
	}

	private int getLeft() {
		return (parentWidth - WIDGET_WIDTH) / 2 - leftOffset;
	}

	protected abstract ButtonTextures getBookButtonTextures();

	public int findLeftEdge(int width, int backgroundWidth) {
		return isOpen() && !narrow
				? 177 + (width - backgroundWidth - 200) / 2
				: (width - backgroundWidth) / 2;
	}

	public void toggleOpen() {
		setOpen(!isOpen());
	}

	public boolean isOpen() {
		return open;
	}

	private boolean isGuiOpen() {
		return recipeBook.isGuiOpen(craftingScreenHandler.getCategory());
	}

	protected void setOpen(boolean opened) {
		if (opened) {
			reset();
		}

		open = opened;
		recipeBook.setGuiOpen(craftingScreenHandler.getCategory(), opened);

		if (!opened) {
			recipesArea.hideAlternates();
		}

		sendBookDataPacket();
	}

	protected abstract boolean isCraftingSlot(Slot slot);

	public void onMouseClick(@Nullable Slot slot) {
		if (slot == null || !isCraftingSlot(slot)) {
			return;
		}

		selectedRecipeId = null;
		ghostRecipe.clear();

		if (isOpen()) {
			refreshInputs();
		}
	}

	private void populateAllRecipes() {
		for (RecipeBookWidget.Tab tab : tabs) {
			for (RecipeResultCollection collection : recipeBook.getResultsForCategory(tab.category())) {
				populateRecipes(collection, recipeFinder);
			}
		}
	}

	protected abstract void populateRecipes(RecipeResultCollection recipeResultCollection, RecipeFinder recipeFinder);

	private void refreshResults(boolean resetCurrentPage, boolean filteringCraftable) {
		List<RecipeResultCollection> allResults = recipeBook.getResultsForCategory(currentTab.getCategory());
		List<RecipeResultCollection> filteredResults = Lists.newArrayList(allResults);
		filteredResults.removeIf(collection -> !collection.hasDisplayableRecipes());

		String query = searchField.getText();

		if (!query.isEmpty()) {
			ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();

			if (networkHandler != null) {
				ObjectSet<RecipeResultCollection> matchingResults = new ObjectLinkedOpenHashSet(
						networkHandler
								.getSearchManager()
								.getRecipeOutputReloadFuture()
								.findAll(query.toLowerCase(Locale.ROOT))
				);
				filteredResults.removeIf(collection -> !matchingResults.contains(collection));
			}
		}

		if (filteringCraftable) {
			filteredResults.removeIf(collection -> !collection.hasCraftableRecipes());
		}

		recipesArea.setResults(filteredResults, resetCurrentPage, filteringCraftable);
	}

	private void refreshTabButtons(boolean filteringCraftable) {
		int tabX = (parentWidth - WIDGET_WIDTH) / 2 - leftOffset - BUTTON_HEIGHT;
		int tabY = (parentHeight - WIDGET_HEIGHT) / 2 + TAB_OFFSET_Y;
		int tabIndex = 0;

		for (RecipeGroupButtonWidget tabButton : tabButtons) {
			RecipeBookGroup category = tabButton.getCategory();

			if (category instanceof net.minecraft.client.recipebook.RecipeBookType) {
				tabButton.visible = true;
				tabButton.setPosition(tabX, tabY + TAB_SPACING * tabIndex++);
			} else if (tabButton.hasKnownRecipes(recipeBook)) {
				tabButton.setPosition(tabX, tabY + TAB_SPACING * tabIndex++);
				tabButton.checkForNewRecipes(recipeBook, filteringCraftable);
			}
		}
	}

	public void update() {
		boolean guiOpen = isGuiOpen();

		if (isOpen() != guiOpen) {
			setOpen(guiOpen);
		}

		if (!isOpen()) {
			return;
		}

		if (cachedInvChangeCount != client.player.getInventory().getChangeCount()) {
			refreshInputs();
			cachedInvChangeCount = client.player.getInventory().getChangeCount();
		}
	}

	private void refreshInputs() {
		recipeFinder.clear();
		client.player.getInventory().populateRecipeFinder(recipeFinder);
		craftingScreenHandler.populateRecipeFinder(recipeFinder);
		populateAllRecipes();
		refreshResults(false, isFilteringCraftable());
	}

	private boolean isFilteringCraftable() {
		return recipeBook.isFilteringCraftable(craftingScreenHandler.getCategory());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (!isOpen()) {
			return;
		}

		if (!client.isCtrlPressed()) {
			displayTime += deltaTicks;
		}

		int left = getLeft();
		int top = getTop();
		context.drawTexture(
				RenderPipelines.GUI_TEXTURED,
				TEXTURE,
				left,
				top,
				1.0F,
				1.0F,
				WIDGET_WIDTH,
				WIDGET_HEIGHT,
				TEXTURE_WIDTH,
				TEXTURE_HEIGHT
		);
		searchField.render(context, mouseX, mouseY, deltaTicks);

		for (RecipeGroupButtonWidget tabButton : tabButtons) {
			tabButton.render(context, mouseX, mouseY, deltaTicks);
		}

		toggleCraftableButton.render(context, mouseX, mouseY, deltaTicks);
		recipesArea.draw(context, left, top, mouseX, mouseY, deltaTicks);
	}

	public void drawTooltip(DrawContext context, int x, int y, @Nullable Slot slot) {
		if (!isOpen()) {
			return;
		}

		recipesArea.drawTooltip(context, x, y);
		ghostRecipe.drawTooltip(context, client, x, y, slot);
	}

	protected abstract Text getToggleCraftableButtonText();

	public void drawGhostSlots(DrawContext context, boolean resultHasPadding) {
		ghostRecipe.draw(context, client, resultHasPadding);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (!isOpen() || client.player.isSpectator()) {
			return false;
		}

		if (recipesArea.mouseClicked(click, getLeft(), getTop(), WIDGET_WIDTH, WIDGET_HEIGHT, doubled)) {
			NetworkRecipeId clickedRecipeId = recipesArea.getLastClickedRecipe();
			RecipeResultCollection clickedResults = recipesArea.getLastClickedResults();

			if (clickedRecipeId != null && clickedResults != null) {
				if (!select(clickedResults, clickedRecipeId, click.hasShift())) {
					return false;
				}

				selectedRecipeResults = clickedResults;
				selectedRecipe = clickedRecipeId;

				if (!isWide()) {
					setOpen(false);
				}
			}

			return true;
		}

		if (searchField != null) {
			boolean clickedSearchArea = searchFieldRect != null
					&& searchFieldRect.contains(MathHelper.floor(click.x()), MathHelper.floor(click.y()));

			if (clickedSearchArea || searchField.mouseClicked(click, doubled)) {
				searchField.setFocused(true);
				return true;
			}

			searchField.setFocused(false);
		}

		if (toggleCraftableButton.mouseClicked(click, doubled)) {
			return true;
		}

		for (RecipeGroupButtonWidget tabButton : tabButtons) {
			if (tabButton.mouseClicked(click, doubled)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		return searchField != null
				&& searchField.isFocused()
				&& searchField.mouseDragged(click, offsetX, offsetY);
	}

	/**
	 * Выбирает рецепт для крафта. Возвращает {@code false}, если рецепт уже выбран
	 * и при этом не является крафтабельным (нет ингредиентов).
	 */
	private boolean select(RecipeResultCollection results, NetworkRecipeId recipeId, boolean craftAll) {
		if (!results.isCraftable(recipeId) && recipeId.equals(selectedRecipeId)) {
			return false;
		}

		selectedRecipeId = recipeId;
		ghostRecipe.clear();
		client.interactionManager.clickRecipe(client.player.currentScreenHandler.syncId, recipeId, craftAll);
		return true;
	}

	private void onTabSelected(ButtonWidget button) {
		if (currentTab == button || !(button instanceof RecipeGroupButtonWidget tabButton)) {
			return;
		}

		setCurrentTab(tabButton);
		refreshResults(true, isFilteringCraftable());
	}

	private void setCurrentTab(RecipeGroupButtonWidget newTab) {
		if (currentTab != null) {
			currentTab.unfocus();
		}

		newTab.focus();
		currentTab = newTab;
	}

	private void toggleFilteringCraftable() {
		RecipeBookType category = craftingScreenHandler.getCategory();
		boolean newValue = !recipeBook.isFilteringCraftable(category);
		recipeBook.setFilteringCraftable(category, newValue);
	}

	public boolean isClickOutsideBounds(
			double mouseX,
			double mouseY,
			int x,
			int y,
			int backgroundWidth,
			int backgroundHeight
	) {
		if (!isOpen()) {
			return true;
		}

		boolean isOutsideBackground = mouseX < x
				|| mouseY < y
				|| mouseX >= x + backgroundWidth
				|| mouseY >= y + backgroundHeight;
		boolean isInsideBookArea = x - WIDGET_WIDTH < mouseX
				&& mouseX < x
				&& y < mouseY
				&& mouseY < y + backgroundHeight;

		return isOutsideBackground && !isInsideBookArea && !currentTab.isSelected();
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		searching = false;

		if (!isOpen() || client.player.isSpectator()) {
			return false;
		}

		if (input.isEscape() && !isWide()) {
			setOpen(false);
			return true;
		}

		if (searchField.keyPressed(input)) {
			refreshSearchResults();
			return true;
		}

		if (searchField.isFocused() && searchField.isVisible() && !input.isEscape()) {
			return true;
		}

		if (client.options.chatKey.matchesKey(input) && !searchField.isFocused()) {
			searching = true;
			searchField.setFocused(true);
			return true;
		}

		if (input.isEnterOrSpace() && selectedRecipeResults != null && selectedRecipe != null) {
			ClickableWidget.playClickSound(MinecraftClient.getInstance().getSoundManager());
			return select(selectedRecipeResults, selectedRecipe, input.hasShift());
		}

		return false;
	}

	@Override
	public boolean keyReleased(KeyInput input) {
		searching = false;
		return Element.super.keyReleased(input);
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (searching) {
			return false;
		}

		if (!isOpen() || client.player.isSpectator()) {
			return false;
		}

		if (searchField.charTyped(input)) {
			refreshSearchResults();
			return true;
		}

		return Element.super.charTyped(input);
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return false;
	}

	@Override
	public void setFocused(boolean focused) {
	}

	@Override
	public boolean isFocused() {
		return false;
	}

	private void refreshSearchResults() {
		String query = searchField.getText().toLowerCase(Locale.ROOT);
		triggerPirateSpeakEasterEgg(query);

		if (!query.equals(searchText)) {
			refreshResults(false, isFilteringCraftable());
			searchText = query;
		}
	}

	private void triggerPirateSpeakEasterEgg(String search) {
		if (!PIRATE_SPEAK_EASTER_EGG_QUERY.equals(search)) {
			return;
		}

		LanguageManager languageManager = client.getLanguageManager();
		LanguageDefinition pirateLanguage = languageManager.getLanguage(PIRATE_SPEAK_LANGUAGE);

		if (pirateLanguage == null || languageManager.getLanguage().equals(PIRATE_SPEAK_LANGUAGE)) {
			return;
		}

		languageManager.setLanguage(PIRATE_SPEAK_LANGUAGE);
		client.options.language = PIRATE_SPEAK_LANGUAGE;
		client.reloadResources();
		client.options.write();
	}

	private boolean isWide() {
		return leftOffset == NARROW_WIDGET_WIDTH;
	}

	public void refresh() {
		populateAllRecipes();
		refreshTabButtons(isFilteringCraftable());

		if (isOpen()) {
			refreshResults(false, isFilteringCraftable());
		}
	}

	public void onRecipeDisplayed(NetworkRecipeId recipeId) {
		client.player.onRecipeDisplayed(recipeId);
	}

	public void onCraftFailed(RecipeDisplay display) {
		ghostRecipe.clear();
		ContextParameterMap context = SlotDisplayContexts.createParameters(Objects.requireNonNull(client.world));
		showGhostRecipe(ghostRecipe, display, context);
	}

	protected abstract void showGhostRecipe(
			GhostRecipe ghostRecipe,
			RecipeDisplay display,
			ContextParameterMap context
	);

	protected void sendBookDataPacket() {
		ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();

		if (networkHandler == null) {
			return;
		}

		RecipeBookType category = craftingScreenHandler.getCategory();
		boolean isGuiOpen = recipeBook.getOptions().isGuiOpen(category);
		boolean isFiltering = recipeBook.getOptions().isFilteringCraftable(category);
		networkHandler.sendPacket(new RecipeCategoryOptionsC2SPacket(category, isGuiOpen, isFiltering));
	}

	@Override
	public Selectable.SelectionType getType() {
		return open ? Selectable.SelectionType.HOVERED : Selectable.SelectionType.NONE;
	}

	@Override
	public void appendNarrations(NarrationMessageBuilder builder) {
		List<Selectable> selectables = Lists.newArrayList();
		recipesArea.forEachButton(button -> {
			if (button.isInteractable()) {
				selectables.add(button);
			}
		});
		selectables.add(searchField);
		selectables.add(toggleCraftableButton);
		selectables.addAll(tabButtons);

		Screen.SelectedElementNarrationData narrationData = Screen.findSelectedElementData(selectables, null);

		if (narrationData != null) {
			narrationData.selectable().appendNarrations(builder.nextMessage());
		}
	}

	/**
	 * Описывает вкладку книги рецептов: иконки и категорию рецептов.
	 */
	@Environment(EnvType.CLIENT)
	public record Tab(ItemStack primaryIcon, Optional<ItemStack> secondaryIcon, RecipeBookGroup category) {

		public Tab(net.minecraft.client.recipebook.RecipeBookType type) {
			this(new ItemStack(Items.COMPASS), Optional.empty(), type);
		}

		public Tab(Item primaryIcon, RecipeBookCategory category) {
			this(new ItemStack(primaryIcon), Optional.empty(), category);
		}

		public Tab(Item primaryIcon, Item secondaryIcon, RecipeBookCategory category) {
			this(new ItemStack(primaryIcon), Optional.of(new ItemStack(secondaryIcon)), category);
		}
	}
}

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
import net.minecraft.client.recipebook.RecipeBookType;
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

@Environment(EnvType.CLIENT)
/**
 * {@code RecipeBookWidget}.
 */
public abstract class RecipeBookWidget<T extends AbstractRecipeScreenHandler> implements Drawable, Element, Selectable {

	public static final ButtonTextures BUTTON_TEXTURES = new ButtonTextures(
			Identifier.ofVanilla("recipe_book/button"), Identifier.ofVanilla("recipe_book/button_highlighted")
	);
	protected static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/recipe_book.png");
	private static final int TEXTURE_WIDTH = 256;
	private static final int TEXTURE_HEIGHT = 256;
	private static final Text
			SEARCH_HINT_TEXT =
			Text.translatable("gui.recipebook.search_hint").fillStyle(TextFieldWidget.SEARCH_STYLE);
	public static final int WIDGET_WIDTH = 147;
	public static final int WIDGET_HEIGHT = 166;
	private static final int NARROW_WIDGET_WIDTH = 86;
	private static final int PADDING = 8;
	private static final Text TOGGLE_ALL_RECIPES_TEXT = Text.translatable("gui.recipebook.toggleRecipes.all");
	private static final int BUTTON_HEIGHT = 30;
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
		CurrentIndexProvider currentIndexProvider = () -> MathHelper.floor(this.displayTime / 30.0F);
		this.ghostRecipe = new GhostRecipe(currentIndexProvider);
		this.recipesArea =
				new RecipeBookResults(
						this,
						currentIndexProvider,
						craftingScreenHandler instanceof AbstractFurnaceScreenHandler
				);
	}

	/**
	 * Инициализирует ialize.
	 *
	 * @param parentWidth parent width
	 * @param parentHeight parent height
	 * @param client client
	 * @param narrow narrow
	 */
	public void initialize(int parentWidth, int parentHeight, MinecraftClient client, boolean narrow) {
		this.client = client;
		this.parentWidth = parentWidth;
		this.parentHeight = parentHeight;
		this.narrow = narrow;
		this.recipeBook = client.player.getRecipeBook();
		this.cachedInvChangeCount = client.player.getInventory().getChangeCount();
		this.open = this.isGuiOpen();
		if (this.open) {
			this.reset();
		}
	}

	private void reset() {
		boolean bl = this.isFilteringCraftable();
		this.leftOffset = this.narrow ? 0 : 86;
		int i = this.getLeft();
		int j = this.getTop();
		this.recipeFinder.clear();
		this.client.player.getInventory().populateRecipeFinder(this.recipeFinder);
		this.craftingScreenHandler.populateRecipeFinder(this.recipeFinder);
		String string = this.searchField != null ? this.searchField.getText() : "";
		this.searchField =
				new TextFieldWidget(
						this.client.textRenderer,
						i + 25,
						j + 13,
						81,
						9 + 5,
						Text.translatable("itemGroup.search")
				);
		this.searchField.setMaxLength(50);
		this.searchField.setVisible(true);
		this.searchField.setEditableColor(-1);
		this.searchField.setText(string);
		this.searchField.setPlaceholder(SEARCH_HINT_TEXT);
		this.searchFieldRect = ScreenRect.of(
				NavigationAxis.HORIZONTAL,
				i + 8,
				this.searchField.getY(),
				this.searchField.getX() - this.getLeft(),
				this.searchField.getHeight()
		);
		this.recipesArea.initialize(this.client, i, j);
		this.toggleCraftableButton =
				CyclingButtonWidget.onOffBuilder(this.getToggleCraftableButtonText(), TOGGLE_ALL_RECIPES_TEXT, bl)
				                   .tooltip(value -> value ? Tooltip.of(this.getToggleCraftableButtonText())
				                                           : Tooltip.of(TOGGLE_ALL_RECIPES_TEXT))
				                   .icon((button, value) -> this
						                   .getBookButtonTextures()
						                   .get(value, button.isSelected()))
				                   .labelType(CyclingButtonWidget.LabelType.HIDE)
				                   .build(
						                   i + 110, j + 12, 26, 16, ScreenTexts.EMPTY, (button, value) -> {
							                   this.toggleFilteringCraftable();
							                   this.sendBookDataPacket();
							                   this.refreshResults(false, value);
						                   }
				                   );
		this.tabButtons.clear();

		for (RecipeBookWidget.Tab tab : this.tabs) {
			this.tabButtons.add(new RecipeGroupButtonWidget(0, 0, tab, this::onTabSelected));
		}

		if (this.currentTab != null) {
			this.currentTab =
					this.tabButtons
							.stream()
							.filter(button -> button.getCategory().equals(this.currentTab.getCategory()))
							.findFirst()
							.orElse(null);
		}

		if (this.currentTab == null) {
			this.currentTab = this.tabButtons.get(0);
		}

		this.currentTab.focus();
		this.populateAllRecipes();
		this.refreshTabButtons(bl);
		this.refreshResults(false, bl);
	}

	private int getTop() {
		return (this.parentHeight - 166) / 2;
	}

	private int getLeft() {
		return (this.parentWidth - 147) / 2 - this.leftOffset;
	}

	protected abstract ButtonTextures getBookButtonTextures();

	/**
	 * Ищет left edge.
	 *
	 * @param width width
	 * @param backgroundWidth background width
	 *
	 * @return int — left edge
	 */
	public int findLeftEdge(int width, int backgroundWidth) {
		int i;
		if (this.isOpen() && !this.narrow) {
			i = 177 + (width - backgroundWidth - 200) / 2;
		}
		else {
			i = (width - backgroundWidth) / 2;
		}

		return i;
	}

	/**
	 * Toggle open.
	 */
	public void toggleOpen() {
		this.setOpen(!this.isOpen());
	}

	public boolean isOpen() {
		return this.open;
	}

	private boolean isGuiOpen() {
		return this.recipeBook.isGuiOpen(this.craftingScreenHandler.getCategory());
	}

	protected void setOpen(boolean opened) {
		if (opened) {
			this.reset();
		}

		this.open = opened;
		this.recipeBook.setGuiOpen(this.craftingScreenHandler.getCategory(), opened);
		if (!opened) {
			this.recipesArea.hideAlternates();
		}

		this.sendBookDataPacket();
	}

	protected abstract boolean isCraftingSlot(Slot slot);

	/**
	 * Обрабатывает событие mouse click.
	 *
	 * @param slot slot
	 */
	public void onMouseClick(@Nullable Slot slot) {
		if (slot != null && this.isCraftingSlot(slot)) {
			this.selectedRecipeId = null;
			this.ghostRecipe.clear();
			if (this.isOpen()) {
				this.refreshInputs();
			}
		}
	}

	private void populateAllRecipes() {
		for (RecipeBookWidget.Tab tab : this.tabs) {
			for (RecipeResultCollection recipeResultCollection : this.recipeBook.getResultsForCategory(tab.category())) {
				this.populateRecipes(recipeResultCollection, this.recipeFinder);
			}
		}
	}

	/**
	 * Populate recipes.
	 *
	 * @param recipeResultCollection recipe result collection
	 * @param recipeFinder recipe finder
	 */
	protected abstract void populateRecipes(RecipeResultCollection recipeResultCollection, RecipeFinder recipeFinder);

	private void refreshResults(boolean resetCurrentPage, boolean filteringCraftable) {
		List<RecipeResultCollection> list = this.recipeBook.getResultsForCategory(this.currentTab.getCategory());
		List<RecipeResultCollection> list2 = Lists.newArrayList(list);
		list2.removeIf(resultCollection -> !resultCollection.hasDisplayableRecipes());
		String string = this.searchField.getText();
		if (!string.isEmpty()) {
			ClientPlayNetworkHandler clientPlayNetworkHandler = this.client.getNetworkHandler();
			if (clientPlayNetworkHandler != null) {
				ObjectSet<RecipeResultCollection> objectSet = new ObjectLinkedOpenHashSet(
						clientPlayNetworkHandler
								.getSearchManager()
								.getRecipeOutputReloadFuture()
								.findAll(string.toLowerCase(Locale.ROOT))
				);
				list2.removeIf(resultCollection -> !objectSet.contains(resultCollection));
			}
		}

		if (filteringCraftable) {
			list2.removeIf(resultCollection -> !resultCollection.hasCraftableRecipes());
		}

		this.recipesArea.setResults(list2, resetCurrentPage, filteringCraftable);
	}

	private void refreshTabButtons(boolean filteringCraftable) {
		int i = (this.parentWidth - 147) / 2 - this.leftOffset - 30;
		int j = (this.parentHeight - 166) / 2 + 3;
		int k = 27;
		int l = 0;

		for (RecipeGroupButtonWidget recipeGroupButtonWidget : this.tabButtons) {
			RecipeBookGroup recipeBookGroup = recipeGroupButtonWidget.getCategory();
			if (recipeBookGroup instanceof RecipeBookType) {
				recipeGroupButtonWidget.visible = true;
				recipeGroupButtonWidget.setPosition(i, j + 27 * l++);
			}
			else if (recipeGroupButtonWidget.hasKnownRecipes(this.recipeBook)) {
				recipeGroupButtonWidget.setPosition(i, j + 27 * l++);
				recipeGroupButtonWidget.checkForNewRecipes(this.recipeBook, filteringCraftable);
			}
		}
	}

	/**
	 * Update.
	 */
	public void update() {
		boolean bl = this.isGuiOpen();
		if (this.isOpen() != bl) {
			this.setOpen(bl);
		}

		if (this.isOpen()) {
			if (this.cachedInvChangeCount != this.client.player.getInventory().getChangeCount()) {
				this.refreshInputs();
				this.cachedInvChangeCount = this.client.player.getInventory().getChangeCount();
			}
		}
	}

	private void refreshInputs() {
		this.recipeFinder.clear();
		this.client.player.getInventory().populateRecipeFinder(this.recipeFinder);
		this.craftingScreenHandler.populateRecipeFinder(this.recipeFinder);
		this.populateAllRecipes();
		this.refreshResults(false, this.isFilteringCraftable());
	}

	private boolean isFilteringCraftable() {
		return this.recipeBook.isFilteringCraftable(this.craftingScreenHandler.getCategory());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (this.isOpen()) {
			if (!this.client.isCtrlPressed()) {
				this.displayTime += deltaTicks;
			}

			int i = this.getLeft();
			int j = this.getTop();
			context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 1.0F, 1.0F, 147, 166, 256, 256);
			this.searchField.render(context, mouseX, mouseY, deltaTicks);

			for (RecipeGroupButtonWidget recipeGroupButtonWidget : this.tabButtons) {
				recipeGroupButtonWidget.render(context, mouseX, mouseY, deltaTicks);
			}

			this.toggleCraftableButton.render(context, mouseX, mouseY, deltaTicks);
			this.recipesArea.draw(context, i, j, mouseX, mouseY, deltaTicks);
		}
	}

	/**
	 * Draw tooltip.
	 *
	 * @param context context
	 * @param x x
	 * @param y y
	 * @param slot slot
	 */
	public void drawTooltip(DrawContext context, int x, int y, @Nullable Slot slot) {
		if (this.isOpen()) {
			this.recipesArea.drawTooltip(context, x, y);
			this.ghostRecipe.drawTooltip(context, this.client, x, y, slot);
		}
	}

	protected abstract Text getToggleCraftableButtonText();

	/**
	 * Draw ghost slots.
	 *
	 * @param context context
	 * @param resultHasPadding result has padding
	 */
	public void drawGhostSlots(DrawContext context, boolean resultHasPadding) {
		this.ghostRecipe.draw(context, this.client, resultHasPadding);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (this.isOpen() && !this.client.player.isSpectator()) {
			if (this.recipesArea.mouseClicked(click, this.getLeft(), this.getTop(), 147, 166, doubled)) {
				NetworkRecipeId networkRecipeId = this.recipesArea.getLastClickedRecipe();
				RecipeResultCollection recipeResultCollection = this.recipesArea.getLastClickedResults();
				if (networkRecipeId != null && recipeResultCollection != null) {
					if (!this.select(recipeResultCollection, networkRecipeId, click.hasShift())) {
						return false;
					}

					this.selectedRecipeResults = recipeResultCollection;
					this.selectedRecipe = networkRecipeId;
					if (!this.isWide()) {
						this.setOpen(false);
					}
				}

				return true;
			}
			else {
				if (this.searchField != null) {
					boolean
							bl =
							this.searchFieldRect != null && this.searchFieldRect.contains(
									MathHelper.floor(click.x()),
									MathHelper.floor(click.y())
							);
					if (bl || this.searchField.mouseClicked(click, doubled)) {
						this.searchField.setFocused(true);
						return true;
					}

					this.searchField.setFocused(false);
				}

				if (this.toggleCraftableButton.mouseClicked(click, doubled)) {
					return true;
				}
				else {
					for (RecipeGroupButtonWidget recipeGroupButtonWidget : this.tabButtons) {
						if (recipeGroupButtonWidget.mouseClicked(click, doubled)) {
							return true;
						}
					}

					return false;
				}
			}
		}
		else {
			return false;
		}
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		return this.searchField != null && this.searchField.isFocused() ? this.searchField.mouseDragged(
				click,
				offsetX,
				offsetY
		) : false;
	}

	private boolean select(RecipeResultCollection results, NetworkRecipeId recipeId, boolean craftAll) {
		if (!results.isCraftable(recipeId) && recipeId.equals(this.selectedRecipeId)) {
			return false;
		}
		else {
			this.selectedRecipeId = recipeId;
			this.ghostRecipe.clear();
			this.client.interactionManager.clickRecipe(
					this.client.player.currentScreenHandler.syncId,
					recipeId,
					craftAll
			);
			return true;
		}
	}

	private void onTabSelected(ButtonWidget button) {
		if (this.currentTab != button && button instanceof RecipeGroupButtonWidget recipeGroupButtonWidget) {
			this.setCurrentTab(recipeGroupButtonWidget);
			this.refreshResults(true, this.isFilteringCraftable());
		}
	}

	private void setCurrentTab(RecipeGroupButtonWidget currentTab) {
		if (this.currentTab != null) {
			this.currentTab.unfocus();
		}

		currentTab.focus();
		this.currentTab = currentTab;
	}

	private void toggleFilteringCraftable() {
		net.minecraft.recipe.book.RecipeBookType recipeBookType = this.craftingScreenHandler.getCategory();
		boolean bl = !this.recipeBook.isFilteringCraftable(recipeBookType);
		this.recipeBook.setFilteringCraftable(recipeBookType, bl);
	}

	public boolean isClickOutsideBounds(
			double mouseX,
			double mouseY,
			int x,
			int y,
			int backgroundWidth,
			int backgroundHeight
	) {
		if (!this.isOpen()) {
			return true;
		}
		else {
			boolean bl = mouseX < x || mouseY < y || mouseX >= x + backgroundWidth || mouseY >= y + backgroundHeight;
			boolean bl2 = x - 147 < mouseX && mouseX < x && y < mouseY && mouseY < y + backgroundHeight;
			return bl && !bl2 && !this.currentTab.isSelected();
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		this.searching = false;
		if (!this.isOpen() || this.client.player.isSpectator()) {
			return false;
		}
		else if (input.isEscape() && !this.isWide()) {
			this.setOpen(false);
			return true;
		}
		else if (this.searchField.keyPressed(input)) {
			this.refreshSearchResults();
			return true;
		}
		else if (this.searchField.isFocused() && this.searchField.isVisible() && !input.isEscape()) {
			return true;
		}
		else if (this.client.options.chatKey.matchesKey(input) && !this.searchField.isFocused()) {
			this.searching = true;
			this.searchField.setFocused(true);
			return true;
		}
		else if (input.isEnterOrSpace() && this.selectedRecipeResults != null && this.selectedRecipe != null) {
			ClickableWidget.playClickSound(MinecraftClient.getInstance().getSoundManager());
			return this.select(this.selectedRecipeResults, this.selectedRecipe, input.hasShift());
		}
		else {
			return false;
		}
	}

	@Override
	public boolean keyReleased(KeyInput input) {
		this.searching = false;
		return Element.super.keyReleased(input);
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (this.searching) {
			return false;
		}
		else if (!this.isOpen() || this.client.player.isSpectator()) {
			return false;
		}
		else if (this.searchField.charTyped(input)) {
			this.refreshSearchResults();
			return true;
		}
		else {
			return Element.super.charTyped(input);
		}
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
		String string = this.searchField.getText().toLowerCase(Locale.ROOT);
		this.triggerPirateSpeakEasterEgg(string);
		if (!string.equals(this.searchText)) {
			this.refreshResults(false, this.isFilteringCraftable());
			this.searchText = string;
		}
	}

	private void triggerPirateSpeakEasterEgg(String search) {
		if ("excitedze".equals(search)) {
			LanguageManager languageManager = this.client.getLanguageManager();
			String string = "en_pt";
			LanguageDefinition languageDefinition = languageManager.getLanguage("en_pt");
			if (languageDefinition == null || languageManager.getLanguage().equals("en_pt")) {
				return;
			}

			languageManager.setLanguage("en_pt");
			this.client.options.language = "en_pt";
			this.client.reloadResources();
			this.client.options.write();
		}
	}

	private boolean isWide() {
		return this.leftOffset == 86;
	}

	/**
	 * Refresh.
	 */
	public void refresh() {
		this.populateAllRecipes();
		this.refreshTabButtons(this.isFilteringCraftable());
		if (this.isOpen()) {
			this.refreshResults(false, this.isFilteringCraftable());
		}
	}

	/**
	 * Обрабатывает событие recipe displayed.
	 *
	 * @param recipeId recipe id
	 */
	public void onRecipeDisplayed(NetworkRecipeId recipeId) {
		this.client.player.onRecipeDisplayed(recipeId);
	}

	/**
	 * Обрабатывает событие craft failed.
	 *
	 * @param display display
	 */
	public void onCraftFailed(RecipeDisplay display) {
		this.ghostRecipe.clear();
		ContextParameterMap
				contextParameterMap =
				SlotDisplayContexts.createParameters(Objects.requireNonNull(this.client.world));
		this.showGhostRecipe(this.ghostRecipe, display, contextParameterMap);
	}

	protected abstract void showGhostRecipe(
			GhostRecipe ghostRecipe,
			RecipeDisplay display,
			ContextParameterMap context
	);

	/**
	 * Отправляет book data packet.
	 */
	protected void sendBookDataPacket() {
		if (this.client.getNetworkHandler() != null) {
			net.minecraft.recipe.book.RecipeBookType recipeBookType = this.craftingScreenHandler.getCategory();
			boolean bl = this.recipeBook.getOptions().isGuiOpen(recipeBookType);
			boolean bl2 = this.recipeBook.getOptions().isFilteringCraftable(recipeBookType);
			this.client.getNetworkHandler().sendPacket(new RecipeCategoryOptionsC2SPacket(recipeBookType, bl, bl2));
		}
	}

	@Override
	public Selectable.SelectionType getType() {
		return this.open ? Selectable.SelectionType.HOVERED : Selectable.SelectionType.NONE;
	}

	@Override
	public void appendNarrations(NarrationMessageBuilder builder) {
		List<Selectable> list = Lists.newArrayList();
		this.recipesArea.forEachButton(button -> {
			if (button.isInteractable()) {
				list.add(button);
			}
		});
		list.add(this.searchField);
		list.add(this.toggleCraftableButton);
		list.addAll(this.tabButtons);
		Screen.SelectedElementNarrationData selectedElementNarrationData = Screen.findSelectedElementData(list, null);
		if (selectedElementNarrationData != null) {
			selectedElementNarrationData.selectable().appendNarrations(builder.nextMessage());
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Tab}.
	 */
	public record Tab(ItemStack primaryIcon, Optional<ItemStack> secondaryIcon, RecipeBookGroup category) {

		public Tab(RecipeBookType type) {
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

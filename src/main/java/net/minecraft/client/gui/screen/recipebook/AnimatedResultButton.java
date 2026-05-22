package net.minecraft.client.gui.screen.recipebook;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.MouseInput;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Кнопка результата рецепта с анимацией «подпрыгивания» при появлении нового рецепта.
 * Отображает один или несколько результатов крафта, циклически переключая их иконки.
 */
@Environment(EnvType.CLIENT)
public class AnimatedResultButton extends ClickableWidget {

	private static final Identifier SLOT_MANY_CRAFTABLE_TEXTURE =
			Identifier.ofVanilla("recipe_book/slot_many_craftable");
	private static final Identifier SLOT_CRAFTABLE_TEXTURE =
			Identifier.ofVanilla("recipe_book/slot_craftable");
	private static final Identifier SLOT_MANY_UNCRAFTABLE_TEXTURE =
			Identifier.ofVanilla("recipe_book/slot_many_uncraftable");
	private static final Identifier SLOT_UNCRAFTABLE_TEXTURE =
			Identifier.ofVanilla("recipe_book/slot_uncraftable");
	private static final float ANIMATION_DURATION_TICKS = 15.0F;
	private static final int ANIMATION_INTERVAL_TICKS = 25;
	private static final int ITEM_OFFSET_SHADOW = 4;
	private static final Text MORE_RECIPES_TEXT = Text.translatable("gui.recipebook.moreRecipes");

	private RecipeResultCollection resultCollection = RecipeResultCollection.EMPTY;
	private List<AnimatedResultButton.Result> results = List.of();
	private boolean allResultsEqual;
	private final CurrentIndexProvider currentIndexProvider;
	private float bounce;

	public AnimatedResultButton(CurrentIndexProvider currentIndexProvider) {
		super(0, 0, ANIMATION_INTERVAL_TICKS, ANIMATION_INTERVAL_TICKS, ScreenTexts.EMPTY);
		this.currentIndexProvider = currentIndexProvider;
	}

	/**
	 * Привязывает кнопку к коллекции результатов рецепта и запускает анимацию,
	 * если среди рецептов есть новые (подсвеченные).
	 */
	public void showResultCollection(
			RecipeResultCollection resultCollection,
			boolean filteringCraftable,
			RecipeBookResults results,
			ContextParameterMap context
	) {
		this.resultCollection = resultCollection;
		RecipeResultCollection.RecipeFilterMode filterMode = filteringCraftable
				? RecipeResultCollection.RecipeFilterMode.CRAFTABLE
				: RecipeResultCollection.RecipeFilterMode.ANY;
		List<RecipeDisplayEntry> entries = resultCollection.filter(filterMode);

		this.results = entries
				.stream()
				.map(entry -> new AnimatedResultButton.Result(entry.id(), entry.getStacks(context)))
				.toList();
		allResultsEqual = areAllResultsEqual(this.results);

		List<NetworkRecipeId> highlighted = entries
				.stream()
				.map(RecipeDisplayEntry::id)
				.filter(results.getRecipeBook()::isHighlighted)
				.toList();

		if (highlighted.isEmpty()) {
			return;
		}

		highlighted.forEach(results::onRecipeDisplayed);
		bounce = ANIMATION_DURATION_TICKS;
	}

	private static boolean areAllResultsEqual(List<AnimatedResultButton.Result> results) {
		Iterator<ItemStack> iterator = results
				.stream()
				.flatMap(result -> result.displayItems().stream())
				.iterator();

		if (!iterator.hasNext()) {
			return true;
		}

		ItemStack first = iterator.next();

		while (iterator.hasNext()) {
			if (!ItemStack.areItemsAndComponentsEqual(first, iterator.next())) {
				return false;
			}
		}

		return true;
	}

	public RecipeResultCollection getResultCollection() {
		return resultCollection;
	}

	@Override
	public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		Identifier slotTexture = resolveSlotTexture();
		boolean bouncing = bounce > 0.0F;

		if (bouncing) {
			float scale = 1.0F + 0.1F * (float) Math.sin(bounce / ANIMATION_DURATION_TICKS * (float) Math.PI);
			context.getMatrices().pushMatrix();
			context.getMatrices().translate(getX() + 8, getY() + 12);
			context.getMatrices().scale(scale, scale);
			context.getMatrices().translate(-(getX() + 8), -(getY() + 12));
			bounce -= deltaTicks;
		}

		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, slotTexture, getX(), getY(), width, height);

		ItemStack displayStack = getDisplayStack();
		int itemOffset = ITEM_OFFSET_SHADOW;

		if (hasMultipleResults() && allResultsEqual) {
			context.drawItem(displayStack, getX() + itemOffset + 1, getY() + itemOffset + 1, 0);
			itemOffset--;
		}

		context.drawItemWithoutEntity(displayStack, getX() + itemOffset, getY() + itemOffset);

		if (bouncing) {
			context.getMatrices().popMatrix();
		}
	}

	private Identifier resolveSlotTexture() {
		if (resultCollection.hasCraftableRecipes()) {
			return hasMultipleResults() ? SLOT_MANY_CRAFTABLE_TEXTURE : SLOT_CRAFTABLE_TEXTURE;
		}

		return hasMultipleResults() ? SLOT_MANY_UNCRAFTABLE_TEXTURE : SLOT_UNCRAFTABLE_TEXTURE;
	}

	private boolean hasMultipleResults() {
		return results.size() > 1;
	}

	public boolean hasSingleResult() {
		return results.size() == 1;
	}

	public NetworkRecipeId getCurrentId() {
		int index = currentIndexProvider.currentIndex() % results.size();
		return results.get(index).id;
	}

	public ItemStack getDisplayStack() {
		int globalIndex = currentIndexProvider.currentIndex();
		int count = results.size();
		int cycleIndex = globalIndex - count * (globalIndex / count);
		return results.get(cycleIndex).getDisplayStack(globalIndex / count);
	}

	public List<Text> getTooltip(ItemStack stack) {
		List<Text> tooltip = new ArrayList<>(Screen.getTooltipFromItem(MinecraftClient.getInstance(), stack));

		if (hasMultipleResults()) {
			tooltip.add(MORE_RECIPES_TEXT);
		}

		return tooltip;
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		builder.put(NarrationPart.TITLE, Text.translatable("narration.recipe", getDisplayStack().getName()));

		if (hasMultipleResults()) {
			builder.put(
					NarrationPart.USAGE,
					Text.translatable("narration.button.usage.hovered"),
					Text.translatable("narration.recipe.usage.more")
			);
		} else {
			builder.put(NarrationPart.USAGE, Text.translatable("narration.button.usage.hovered"));
		}
	}

	@Override
	public int getWidth() {
		return ANIMATION_INTERVAL_TICKS;
	}

	@Override
	protected boolean isValidClickButton(MouseInput input) {
		return input.button() == 0 || input.button() == 1;
	}

	@Environment(EnvType.CLIENT)
	record Result(NetworkRecipeId id, List<ItemStack> displayItems) {

		public ItemStack getDisplayStack(int currentIndex) {
			if (displayItems.isEmpty()) {
				return ItemStack.EMPTY;
			}

			return displayItems.get(currentIndex % displayItems.size());
		}
	}
}

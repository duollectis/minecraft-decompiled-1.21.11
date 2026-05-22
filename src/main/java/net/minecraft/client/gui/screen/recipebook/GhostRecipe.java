package net.minecraft.client.gui.screen.recipebook;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.context.ContextParameterMap;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Призрачный рецепт — полупрозрачное отображение ингредиентов и результата крафта
 * поверх слотов инвентаря, подсказывающее игроку, что нужно положить в сетку.
 */
@Environment(EnvType.CLIENT)
public class GhostRecipe {

	private static final int COLOR_GHOST_FILL = 822018048;
	private static final int COLOR_GHOST_OVERLAY = 822083583;
	private static final int RESULT_PADDING = 4;

	private final Reference2ObjectMap<Slot, GhostRecipe.CyclingItem> items = new Reference2ObjectArrayMap();
	private final CurrentIndexProvider currentIndexProvider;

	public GhostRecipe(CurrentIndexProvider currentIndexProvider) {
		this.currentIndexProvider = currentIndexProvider;
	}

	public void clear() {
		items.clear();
	}

	private void addItems(Slot slot, ContextParameterMap context, SlotDisplay display, boolean resultSlot) {
		List<ItemStack> stacks = display.getStacks(context);

		if (stacks.isEmpty()) {
			return;
		}

		items.put(slot, new GhostRecipe.CyclingItem(stacks, resultSlot));
	}

	protected void addInputs(Slot slot, ContextParameterMap context, SlotDisplay display) {
		addItems(slot, context, display, false);
	}

	protected void addResults(Slot slot, ContextParameterMap context, SlotDisplay display) {
		addItems(slot, context, display, true);
	}

	/**
	 * Отрисовывает призрачные предметы поверх слотов крафта.
	 * Для слота результата добавляет расширенную подложку, если {@code resultHasPadding} равен {@code true}.
	 */
	public void draw(DrawContext context, MinecraftClient client, boolean resultHasPadding) {
		items.forEach((slot, item) -> {
			int slotX = slot.x;
			int slotY = slot.y;

			if (item.isResultSlot && resultHasPadding) {
				context.fill(slotX - RESULT_PADDING, slotY - RESULT_PADDING, slotX + 20, slotY + 20, COLOR_GHOST_FILL);
			} else {
				context.fill(slotX, slotY, slotX + 16, slotY + 16, COLOR_GHOST_FILL);
			}

			ItemStack displayStack = item.get(currentIndexProvider.currentIndex());
			context.drawItemWithoutEntity(displayStack, slotX, slotY);
			context.fill(slotX, slotY, slotX + 16, slotY + 16, COLOR_GHOST_OVERLAY);

			if (item.isResultSlot) {
				context.drawStackOverlay(client.textRenderer, displayStack, slotX, slotY);
			}
		});
	}

	/**
	 * Отрисовывает подсказку для призрачного предмета в указанном слоте.
	 */
	public void drawTooltip(DrawContext context, MinecraftClient client, int x, int y, @Nullable Slot slot) {
		if (slot == null) {
			return;
		}

		GhostRecipe.CyclingItem cyclingItem = items.get(slot);

		if (cyclingItem == null) {
			return;
		}

		ItemStack displayStack = cyclingItem.get(currentIndexProvider.currentIndex());
		context.drawTooltip(
				client.textRenderer,
				Screen.getTooltipFromItem(client, displayStack),
				x,
				y,
				displayStack.get(DataComponentTypes.TOOLTIP_STYLE)
		);
	}

	@Environment(EnvType.CLIENT)
	record CyclingItem(List<ItemStack> items, boolean isResultSlot) {

		public ItemStack get(int index) {
			int count = items.size();
			return count == 0 ? ItemStack.EMPTY : items.get(index % count);
		}
	}
}

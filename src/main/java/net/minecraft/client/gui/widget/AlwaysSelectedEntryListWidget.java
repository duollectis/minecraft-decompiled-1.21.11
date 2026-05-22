package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.Narratable;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

/**
 * Список с записями, в котором всегда должна быть выбрана хотя бы одна запись.
 * Расширяет {@link EntryListWidget}, добавляя логику навигации, которая не позволяет
 * снять выделение при перемещении стрелками — вместо этого выбор переходит к соседней записи.
 *
 * @param <E> тип записи списка
 */
@Environment(EnvType.CLIENT)
public abstract class AlwaysSelectedEntryListWidget<E extends AlwaysSelectedEntryListWidget.Entry<E>> extends EntryListWidget<E> {

	private static final Text SELECTION_USAGE_TEXT = Text.translatable("narration.selection.usage");

	public AlwaysSelectedEntryListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) {
		super(client, width, height, y, itemHeight);
	}

	@Override
	public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
		if (getEntryCount() == 0) {
			return null;
		}

		if (isFocused() && navigation instanceof GuiNavigation.Arrow arrow) {
			E neighbor = getNeighboringEntry(arrow.direction());
			if (neighbor != null) {
				return GuiNavigationPath.of(this, GuiNavigationPath.of(neighbor));
			}

			setFocused(null);
			setSelected(null);
			return null;
		}

		if (isFocused()) {
			return null;
		}

		E selected = getSelectedOrNull();
		if (selected == null) {
			selected = getNeighboringEntry(navigation.getDirection());
		}

		return selected == null ? null : GuiNavigationPath.of(this, GuiNavigationPath.of(selected));
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		E hovered = getHoveredEntry();
		if (hovered != null) {
			appendNarrations(builder.nextMessage(), hovered);
			hovered.appendNarrations(builder);
		} else {
			E selected = getSelectedOrNull();
			if (selected != null) {
				appendNarrations(builder.nextMessage(), selected);
				selected.appendNarrations(builder);
			}
		}

		if (isFocused()) {
			builder.put(NarrationPart.USAGE, SELECTION_USAGE_TEXT);
		}
	}

	/**
	 * Базовая запись для {@link AlwaysSelectedEntryListWidget}.
	 * Реализует {@link Narratable} и всегда возвращает {@code true} при клике,
	 * чтобы гарантировать выбор записи.
	 *
	 * @param <E> тип записи
	 */
	@Environment(EnvType.CLIENT)
	public abstract static class Entry<E extends AlwaysSelectedEntryListWidget.Entry<E>> extends EntryListWidget.Entry<E> implements Narratable {

		public abstract Text getNarration();

		@Override
		public boolean mouseClicked(Click click, boolean doubled) {
			return true;
		}

		@Override
		public void appendNarrations(NarrationMessageBuilder builder) {
			builder.put(NarrationPart.TITLE, getNarration());
		}
	}
}

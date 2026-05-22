package net.minecraft.client.gui.screen.option;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.NarratedMultilineTextWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Виджет списка привязок клавиш — отображает категории и отдельные записи клавиш
 * с кнопками редактирования и сброса.
 */
@Environment(EnvType.CLIENT)
public class ControlsListWidget extends ElementListWidget<ControlsListWidget.Entry> {

	private static final int ENTRY_HEIGHT = 20;

	final KeybindsScreen parent;
	private int maxKeyNameLength;

	public ControlsListWidget(KeybindsScreen parent, MinecraftClient client) {
		super(client, parent.width, parent.layout.getContentHeight(), parent.layout.getHeaderHeight(), ENTRY_HEIGHT);
		this.parent = parent;
		KeyBinding[] keyBindings = (KeyBinding[]) ArrayUtils.clone(client.options.allKeys);
		Arrays.sort((Object[]) keyBindings);
		KeyBinding.Category currentCategory = null;

		for (KeyBinding keyBinding : keyBindings) {
			KeyBinding.Category bindingCategory = keyBinding.getCategory();

			if (bindingCategory != currentCategory) {
				currentCategory = bindingCategory;
				addEntry(new ControlsListWidget.CategoryEntry(bindingCategory));
			}

			Text bindingName = Text.translatable(keyBinding.getId());
			int nameWidth = client.textRenderer.getWidth(bindingName);

			if (nameWidth > maxKeyNameLength) {
				maxKeyNameLength = nameWidth;
			}

			addEntry(new ControlsListWidget.KeyBindingEntry(keyBinding, bindingName));
		}
	}

	public void update() {
		KeyBinding.updateKeysByCode();
		updateChildren();
	}

	public void updateChildren() {
		children().forEach(ControlsListWidget.Entry::update);
	}

	@Override
	public int getRowWidth() {
		return 340;
	}

	/**
	 * Запись-заголовок категории клавиш.
	 */
	@Environment(EnvType.CLIENT)
	public class CategoryEntry extends ControlsListWidget.Entry {

		private final NarratedMultilineTextWidget categoryLabel;

		public CategoryEntry(final KeyBinding.Category category) {
			categoryLabel =
					NarratedMultilineTextWidget
							.builder(category.getLabel(), ControlsListWidget.this.client.textRenderer)
							.alwaysShowBorders(false)
							.backgroundRendering(NarratedMultilineTextWidget.BackgroundRendering.ON_FOCUS)
							.build();
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			categoryLabel.setPosition(
					ControlsListWidget.this.width / 2 - categoryLabel.getWidth() / 2,
					getContentBottomEnd() - categoryLabel.getHeight()
			);
			categoryLabel.render(context, mouseX, mouseY, deltaTicks);
		}

		@Override
		public List<? extends Element> children() {
			return List.of(categoryLabel);
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return List.of(categoryLabel);
		}

		@Override
		protected void update() {
		}
	}

	/**
	 * Базовый абстрактный тип записи списка.
	 */
	@Environment(EnvType.CLIENT)
	public abstract static class Entry extends ElementListWidget.Entry<ControlsListWidget.Entry> {

		abstract void update();
	}

	/**
	 * Запись привязки клавиши — отображает название действия, кнопку редактирования
	 * и кнопку сброса, а также маркер дублирования при конфликте клавиш.
	 */
	@Environment(EnvType.CLIENT)
	public class KeyBindingEntry extends ControlsListWidget.Entry {

		private static final Text RESET_TEXT = Text.translatable("controls.reset");
		private static final int RESET_BUTTON_MARGIN = 10;
		private static final int DUPLICATE_MARKER_WIDTH = 3;
		private static final int DUPLICATE_MARKER_OFFSET_X = 6;
		private static final int COLOR_DUPLICATE_MARKER = -256;

		private final KeyBinding binding;
		private final Text bindingName;
		private final ButtonWidget editButton;
		private final ButtonWidget resetButton;
		private boolean duplicate = false;

		KeyBindingEntry(final KeyBinding binding, final Text bindingName) {
			this.binding = binding;
			this.bindingName = bindingName;
			editButton = ButtonWidget
					.builder(
							bindingName, button -> {
								ControlsListWidget.this.parent.selectedKeyBinding = binding;
								ControlsListWidget.this.update();
							}
					)
					.dimensions(0, 0, 75, ENTRY_HEIGHT)
					.narrationSupplier(
							textSupplier -> binding.isUnbound()
									? Text.translatable("narrator.controls.unbound", bindingName)
									: Text.translatable("narrator.controls.bound", bindingName, textSupplier.get())
					)
					.build();
			resetButton = ButtonWidget
					.builder(
							RESET_TEXT, button -> {
								binding.setBoundKey(binding.getDefaultKey());
								ControlsListWidget.this.update();
							}
					)
					.dimensions(0, 0, 50, ENTRY_HEIGHT)
					.narrationSupplier(textSupplier -> Text.translatable("narrator.controls.reset", bindingName))
					.build();
			update();
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int resetX = ControlsListWidget.this.getScrollbarX() - resetButton.getWidth() - RESET_BUTTON_MARGIN;
			int entryY = getContentY() - 2;

			resetButton.setPosition(resetX, entryY);
			resetButton.render(context, mouseX, mouseY, deltaTicks);

			int editX = resetX - 5 - editButton.getWidth();

			editButton.setPosition(editX, entryY);
			editButton.render(context, mouseX, mouseY, deltaTicks);

			context.drawTextWithShadow(
					ControlsListWidget.this.client.textRenderer,
					bindingName,
					getContentX(),
					getContentMiddleY() - 9 / 2,
					-1
			);

			if (duplicate) {
				int markerX = editButton.getX() - DUPLICATE_MARKER_OFFSET_X;
				context.fill(markerX, getContentY() - 1, markerX + DUPLICATE_MARKER_WIDTH, getContentBottomEnd(), COLOR_DUPLICATE_MARKER);
			}
		}

		@Override
		public List<? extends Element> children() {
			return ImmutableList.of(editButton, resetButton);
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return ImmutableList.of(editButton, resetButton);
		}

		@Override
		protected void update() {
			editButton.setMessage(binding.getBoundKeyLocalizedText());
			resetButton.active = !binding.isDefault();
			duplicate = false;
			MutableText conflictNames = Text.empty();

			if (binding.isUnbound()) {
				return;
			}

			for (KeyBinding keyBinding : ControlsListWidget.this.client.options.allKeys) {
				if (keyBinding != binding
						&& binding.equals(keyBinding)
						&& (!keyBinding.isDefault() || !binding.isDefault())
				) {
					if (duplicate) {
						conflictNames.append(", ");
					}

					duplicate = true;
					conflictNames.append(Text.translatable(keyBinding.getId()));
				}
			}

			if (duplicate) {
				editButton.setMessage(
						Text.literal("[ ")
								.append(editButton.getMessage().copy().formatted(Formatting.WHITE))
								.append(" ]")
								.formatted(Formatting.YELLOW)
				);
				editButton.setTooltip(Tooltip.of(Text.translatable("controls.keybinds.duplicateKeybinds", conflictNames)));
			} else {
				editButton.setTooltip(null);
			}

			if (ControlsListWidget.this.parent.selectedKeyBinding == binding) {
				editButton.setMessage(
						Text.literal("> ")
								.append(editButton.getMessage().copy().formatted(Formatting.WHITE, Formatting.UNDERLINE))
								.append(" <")
								.formatted(Formatting.YELLOW)
				);
			}
		}
	}
}

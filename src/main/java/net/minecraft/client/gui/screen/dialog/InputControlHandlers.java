package net.minecraft.client.gui.screen.dialog;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.dialog.action.DialogAction;
import net.minecraft.dialog.input.*;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtString;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Реестр обработчиков элементов управления диалоговых форм.
 * Каждый обработчик создаёт виджет для конкретного типа {@link InputControl}.
 */
@Environment(EnvType.CLIENT)
public class InputControlHandlers {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Map<MapCodec<? extends InputControl>, InputControlHandler<?>>
			INPUT_CONTROL_HANDLERS =
			new HashMap<>();

	private static <T extends InputControl> void register(
			MapCodec<T> inputControlCodec,
			InputControlHandler<? super T> inputControlHandler
	) {
		INPUT_CONTROL_HANDLERS.put(inputControlCodec, inputControlHandler);
	}

	private static <T extends InputControl> @Nullable InputControlHandler<T> getHandler(T inputControl) {
		return (InputControlHandler<T>) INPUT_CONTROL_HANDLERS.get(inputControl.getCodec());
	}

	public static <T extends InputControl> void addControl(
			T inputControl,
			Screen screen,
			InputControlHandler.Output output
	) {
		InputControlHandler<T> handler = getHandler(inputControl);

		if (handler == null) {
			LOGGER.warn("Unrecognized input control {}", inputControl);
			return;
		}

		handler.addControl(inputControl, screen, output);
	}

	/**
	 * Bootstrap.
	 */
	public static void bootstrap() {
		register(TextInputControl.CODEC, new InputControlHandlers.TextInputControlHandler());
		register(SingleOptionInputControl.CODEC, new InputControlHandlers.SimpleOptionInputControlHandler());
		register(BooleanInputControl.CODEC, new InputControlHandlers.BooleanInputControlHandler());
		register(NumberRangeInputControl.CODEC, new InputControlHandlers.NumberRangeInputControlHandler());
	}

	/**
	 * Обработчик булевого элемента управления (чекбокс).
	 */
	@Environment(EnvType.CLIENT)
	static class BooleanInputControlHandler implements InputControlHandler<BooleanInputControl> {

		public void addControl(BooleanInputControl control, Screen screen, InputControlHandler.Output output) {
			CheckboxWidget checkbox = CheckboxWidget
					.builder(control.label(), screen.getTextRenderer())
					.checked(control.initial())
					.build();
			output.accept(checkbox, new DialogAction.ValueGetter() {
				@Override
				public String get() {
					return checkbox.isChecked() ? control.onTrue() : control.onFalse();
				}

				@Override
				public NbtElement getAsNbt() {
					return NbtByte.of(checkbox.isChecked());
				}
			});
		}
	}

	/**
	 * Обработчик числового диапазона (слайдер).
	 */
	@Environment(EnvType.CLIENT)
	static class NumberRangeInputControlHandler implements InputControlHandler<NumberRangeInputControl> {

		public void addControl(NumberRangeInputControl control, Screen screen, InputControlHandler.Output output) {
			float initialProgress = control.rangeInfo().getInitialSliderProgress();
			RangeSliderWidget slider = new RangeSliderWidget(control, initialProgress);
			output.accept(slider, new DialogAction.ValueGetter() {
				@Override
				public String get() {
					return slider.getLabel();
				}

				@Override
				public NbtElement getAsNbt() {
					return NbtFloat.of(slider.getActualValue());
				}
			});
		}

		/**
		 * Слайдер для выбора числового значения из диапазона.
		 */
		@Environment(EnvType.CLIENT)
		static class RangeSliderWidget extends SliderWidget {

			private final NumberRangeInputControl inputControl;

			RangeSliderWidget(NumberRangeInputControl inputControl, double value) {
				super(0, 0, inputControl.width(), 20, getFormattedLabel(inputControl, value), value);
				this.inputControl = inputControl;
			}

			@Override
			protected void updateMessage() {
				setMessage(getFormattedLabel(inputControl, value));
			}

			@Override
			protected void applyValue() {
			}

			public String getLabel() {
				return getLabel(inputControl, value);
			}

			public float getActualValue() {
				return getActualValue(inputControl, value);
			}

			private static float getActualValue(NumberRangeInputControl control, double sliderProgress) {
				return control.rangeInfo().sliderProgressToValue((float) sliderProgress);
			}

			private static String getLabel(NumberRangeInputControl control, double sliderProgress) {
				return valueToString(getActualValue(control, sliderProgress));
			}

			private static Text getFormattedLabel(NumberRangeInputControl control, double sliderProgress) {
				return control.getFormattedLabel(getLabel(control, sliderProgress));
			}

			private static String valueToString(float value) {
				int intValue = (int) value;
				return intValue == value ? Integer.toString(intValue) : Float.toString(value);
			}
		}
	}

	/**
	 * Обработчик элемента с единственным выбором из списка (выпадающий список).
	 */
	@Environment(EnvType.CLIENT)
	static class SimpleOptionInputControlHandler implements InputControlHandler<SingleOptionInputControl> {

		public void addControl(SingleOptionInputControl control, Screen screen, InputControlHandler.Output output) {
			SingleOptionInputControl.Entry initialEntry = control.getInitialEntry().orElse(control.entries().getFirst());
			CyclingButtonWidget.LabelType labelType = control.labelVisible()
					? CyclingButtonWidget.LabelType.NAME_AND_VALUE
					: CyclingButtonWidget.LabelType.VALUE;
			CyclingButtonWidget<SingleOptionInputControl.Entry> cyclingButton = CyclingButtonWidget
					.builder(SingleOptionInputControl.Entry::getDisplay, initialEntry)
					.values(control.entries())
					.labelType(labelType)
					.build(0, 0, control.width(), 20, control.label());
			output.accept(cyclingButton, DialogAction.ValueGetter.of(() -> cyclingButton.getValue().id()));
		}
	}

	/**
	 * Обработчик текстового поля ввода (однострочного или многострочного).
	 */
	@Environment(EnvType.CLIENT)
	static class TextInputControlHandler implements InputControlHandler<TextInputControl> {

		public void addControl(TextInputControl control, Screen screen, InputControlHandler.Output output) {
			TextRenderer textRenderer = screen.getTextRenderer();
			Widget inputWidget;
			Supplier<String> valueSupplier;

			if (control.multiline().isPresent()) {
				TextInputControl.Multiline multiline = control.multiline().get();
				int boxHeight = multiline.height().orElseGet(() -> {
					int maxLines = multiline.maxLines().orElse(4);
					return Math.min(9 * maxLines + 8, 512);
				});
				EditBoxWidget editBox = EditBoxWidget.builder().build(textRenderer, control.width(), boxHeight, ScreenTexts.EMPTY);
				editBox.setMaxLength(control.maxLength());
				multiline.maxLines().ifPresent(editBox::setMaxLines);
				editBox.setText(control.initial());
				inputWidget = editBox;
				valueSupplier = editBox::getText;
			} else {
				TextFieldWidget textField = new TextFieldWidget(textRenderer, control.width(), 20, control.label());
				textField.setMaxLength(control.maxLength());
				textField.setText(control.initial());
				inputWidget = textField;
				valueSupplier = textField::getText;
			}

			Widget labeledWidget = control.labelVisible()
					? LayoutWidgets.createLabeledWidget(textRenderer, inputWidget, control.label())
					: inputWidget;
			output.accept(labeledWidget, new DialogAction.ValueGetter() {
				@Override
				public String get() {
					return NbtString.escapeUnquoted(valueSupplier.get());
				}

				@Override
				public NbtElement getAsNbt() {
					return NbtString.of(valueSupplier.get());
				}
			});
		}
	}
}

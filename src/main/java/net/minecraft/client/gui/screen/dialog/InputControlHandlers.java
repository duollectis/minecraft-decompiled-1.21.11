package net.minecraft.client.gui.screen.dialog;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.LayoutWidgets;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.dialog.action.DialogAction;
import net.minecraft.dialog.input.BooleanInputControl;
import net.minecraft.dialog.input.InputControl;
import net.minecraft.dialog.input.NumberRangeInputControl;
import net.minecraft.dialog.input.SingleOptionInputControl;
import net.minecraft.dialog.input.TextInputControl;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtString;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class InputControlHandlers {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<MapCodec<? extends InputControl>, InputControlHandler<?>> INPUT_CONTROL_HANDLERS = new HashMap<>();

   private static <T extends InputControl> void register(MapCodec<T> inputControlCodec, InputControlHandler<? super T> inputControlHandler) {
      INPUT_CONTROL_HANDLERS.put(inputControlCodec, inputControlHandler);
   }

   private static <T extends InputControl> @Nullable InputControlHandler<T> getHandler(T inputControl) {
      return (InputControlHandler<T>)INPUT_CONTROL_HANDLERS.get(inputControl.getCodec());
   }

   public static <T extends InputControl> void addControl(T inputControl, Screen screen, InputControlHandler.Output output) {
      InputControlHandler<T> inputControlHandler = getHandler(inputControl);
      if (inputControlHandler == null) {
         LOGGER.warn("Unrecognized input control {}", inputControl);
      } else {
         inputControlHandler.addControl(inputControl, screen, output);
      }
   }

   public static void bootstrap() {
      register(TextInputControl.CODEC, new InputControlHandlers.TextInputControlHandler());
      register(SingleOptionInputControl.CODEC, new InputControlHandlers.SimpleOptionInputControlHandler());
      register(BooleanInputControl.CODEC, new InputControlHandlers.BooleanInputControlHandler());
      register(NumberRangeInputControl.CODEC, new InputControlHandlers.NumberRangeInputControlHandler());
   }

   @Environment(EnvType.CLIENT)
   static class BooleanInputControlHandler implements InputControlHandler<BooleanInputControl> {
      public void addControl(BooleanInputControl booleanInputControl, Screen screen, InputControlHandler.Output output) {
         TextRenderer textRenderer = screen.getTextRenderer();
         final CheckboxWidget checkboxWidget = CheckboxWidget.builder(booleanInputControl.label(), textRenderer).checked(booleanInputControl.initial()).build();
         output.accept(checkboxWidget, new DialogAction.ValueGetter() {
            @Override
            public String get() {
               return checkboxWidget.isChecked() ? booleanInputControl.onTrue() : booleanInputControl.onFalse();
            }

            @Override
            public NbtElement getAsNbt() {
               return NbtByte.of(checkboxWidget.isChecked());
            }
         });
      }
   }

   @Environment(EnvType.CLIENT)
   static class NumberRangeInputControlHandler implements InputControlHandler<NumberRangeInputControl> {
      public void addControl(NumberRangeInputControl numberRangeInputControl, Screen screen, InputControlHandler.Output output) {
         float f = numberRangeInputControl.rangeInfo().getInitialSliderProgress();
         final InputControlHandlers.NumberRangeInputControlHandler.RangeSliderWidget rangeSliderWidget = new InputControlHandlers.NumberRangeInputControlHandler.RangeSliderWidget(
            numberRangeInputControl, f
         );
         output.accept(rangeSliderWidget, new DialogAction.ValueGetter() {
            @Override
            public String get() {
               return rangeSliderWidget.getLabel();
            }

            @Override
            public NbtElement getAsNbt() {
               return NbtFloat.of(rangeSliderWidget.getActualValue());
            }
         });
      }

      @Environment(EnvType.CLIENT)
      static class RangeSliderWidget extends SliderWidget {
         private final NumberRangeInputControl inputControl;

         RangeSliderWidget(NumberRangeInputControl inputControl, double value) {
            super(0, 0, inputControl.width(), 20, getFormattedLabel(inputControl, value), value);
            this.inputControl = inputControl;
         }

         @Override
         protected void updateMessage() {
            this.setMessage(getFormattedLabel(this.inputControl, this.value));
         }

         @Override
         protected void applyValue() {
         }

         public String getLabel() {
            return getLabel(this.inputControl, this.value);
         }

         public float getActualValue() {
            return getActualValue(this.inputControl, this.value);
         }

         private static float getActualValue(NumberRangeInputControl inputControl, double sliderProgress) {
            return inputControl.rangeInfo().sliderProgressToValue((float)sliderProgress);
         }

         private static String getLabel(NumberRangeInputControl inputControl, double sliderProgress) {
            return valueToString(getActualValue(inputControl, sliderProgress));
         }

         private static Text getFormattedLabel(NumberRangeInputControl inputControl, double sliderProgress) {
            return inputControl.getFormattedLabel(getLabel(inputControl, sliderProgress));
         }

         private static String valueToString(float value) {
            int i = (int)value;
            return i == value ? Integer.toString(i) : Float.toString(value);
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static class SimpleOptionInputControlHandler implements InputControlHandler<SingleOptionInputControl> {
      public void addControl(SingleOptionInputControl singleOptionInputControl, Screen screen, InputControlHandler.Output output) {
         SingleOptionInputControl.Entry entry = singleOptionInputControl.getInitialEntry().orElse(singleOptionInputControl.entries().getFirst());
         CyclingButtonWidget.Builder<SingleOptionInputControl.Entry> builder = CyclingButtonWidget.builder(SingleOptionInputControl.Entry::getDisplay, entry)
            .values(singleOptionInputControl.entries())
            .labelType(!singleOptionInputControl.labelVisible() ? CyclingButtonWidget.LabelType.VALUE : CyclingButtonWidget.LabelType.NAME_AND_VALUE);
         CyclingButtonWidget<SingleOptionInputControl.Entry> cyclingButtonWidget = builder.build(
            0, 0, singleOptionInputControl.width(), 20, singleOptionInputControl.label()
         );
         output.accept(cyclingButtonWidget, DialogAction.ValueGetter.of(() -> cyclingButtonWidget.getValue().id()));
      }
   }

   @Environment(EnvType.CLIENT)
   static class TextInputControlHandler implements InputControlHandler<TextInputControl> {
      public void addControl(TextInputControl textInputControl, Screen screen, InputControlHandler.Output output) {
         TextRenderer textRenderer = screen.getTextRenderer();
         Widget widget;
         final Supplier<String> supplier;
         if (textInputControl.multiline().isPresent()) {
            TextInputControl.Multiline multiline = textInputControl.multiline().get();
            int i = multiline.height().orElseGet(() -> {
               int ix = multiline.maxLines().orElse(4);
               return Math.min(9 * ix + 8, 512);
            });
            EditBoxWidget editBoxWidget = EditBoxWidget.builder().build(textRenderer, textInputControl.width(), i, ScreenTexts.EMPTY);
            editBoxWidget.setMaxLength(textInputControl.maxLength());
            multiline.maxLines().ifPresent(editBoxWidget::setMaxLines);
            editBoxWidget.setText(textInputControl.initial());
            widget = editBoxWidget;
            supplier = editBoxWidget::getText;
         } else {
            TextFieldWidget textFieldWidget = new TextFieldWidget(textRenderer, textInputControl.width(), 20, textInputControl.label());
            textFieldWidget.setMaxLength(textInputControl.maxLength());
            textFieldWidget.setText(textInputControl.initial());
            widget = textFieldWidget;
            supplier = textFieldWidget::getText;
         }

         Widget widget2 = (Widget)(textInputControl.labelVisible() ? LayoutWidgets.createLabeledWidget(textRenderer, widget, textInputControl.label()) : widget);
         output.accept(widget2, new DialogAction.ValueGetter() {
            @Override
            public String get() {
               return NbtString.escapeUnquoted(supplier.get());
            }

            @Override
            public NbtElement getAsNbt() {
               return NbtString.of(supplier.get());
            }
         });
      }
   }
}

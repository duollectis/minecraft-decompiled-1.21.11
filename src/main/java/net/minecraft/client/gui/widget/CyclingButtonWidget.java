package net.minecraft.client.gui.widget;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Updatable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Кнопка с циклическим переключением значений из списка {@link Values}.
 * Поддерживает альтернативные списки значений (при зажатом Alt), тултипы и иконки.
 */
@Environment(EnvType.CLIENT)
public class CyclingButtonWidget<T> extends PressableWidget implements Updatable {

	public static final BooleanSupplier HAS_ALT_DOWN = () -> MinecraftClient.getInstance().isAltPressed();
	private static final List<Boolean> BOOLEAN_VALUES = ImmutableList.of(Boolean.TRUE, Boolean.FALSE);
	private final Supplier<T> valueSupplier;
	private final Text optionText;
	private int index;
	private T value;
	private final CyclingButtonWidget.Values<T> values;
	private final Function<T, Text> valueToText;
	private final Function<CyclingButtonWidget<T>, MutableText> narrationMessageFactory;
	private final CyclingButtonWidget.UpdateCallback<T> callback;
	private final CyclingButtonWidget.LabelType labelType;
	private final SimpleOption.TooltipFactory<T> tooltipFactory;
	private final CyclingButtonWidget.IconGetter<T> icon;

	CyclingButtonWidget(
			int x,
			int y,
			int width,
			int height,
			Text message,
			Text optionText,
			int index,
			T value,
			Supplier<T> valueSupplier,
			CyclingButtonWidget.Values<T> values,
			Function<T, Text> valueToText,
			Function<CyclingButtonWidget<T>, MutableText> narrationMessageFactory,
			CyclingButtonWidget.UpdateCallback<T> callback,
			SimpleOption.TooltipFactory<T> tooltipFactory,
			CyclingButtonWidget.LabelType labelType,
			CyclingButtonWidget.IconGetter<T> icon
	) {
		super(x, y, width, height, message);
		this.optionText = optionText;
		this.index = index;
		this.valueSupplier = valueSupplier;
		this.value = value;
		this.values = values;
		this.valueToText = valueToText;
		this.narrationMessageFactory = narrationMessageFactory;
		this.callback = callback;
		this.labelType = labelType;
		this.tooltipFactory = tooltipFactory;
		this.icon = icon;
		refreshTooltip();
	}

	@Override
	protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		Identifier iconTexture = icon.apply(this, getValue());
		if (iconTexture != null) {
			context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					iconTexture,
					getX(),
					getY(),
					getWidth(),
					getHeight()
			);
		}
		else {
			drawButton(context);
		}

		if (labelType != CyclingButtonWidget.LabelType.HIDE) {
			drawLabel(context.getHoverListener(this, DrawContext.HoverType.NONE));
		}
	}

	private void refreshTooltip() {
		setTooltip(tooltipFactory.apply(value));
	}

	@Override
	public void onPress(AbstractInput input) {
		cycle(input.hasShift() ? -1 : 1);
	}

	private void cycle(int amount) {
		List<T> current = values.getCurrent();
		index = MathHelper.floorMod(index + amount, current.size());
		T next = current.get(index);
		internalSetValue(next);
		callback.onValueChange(this, next);
	}

	private T getValue(int offset) {
		List<T> current = values.getCurrent();
		return current.get(MathHelper.floorMod(index + offset, current.size()));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (verticalAmount > 0.0) {
			cycle(-1);
		}
		else if (verticalAmount < 0.0) {
			cycle(1);
		}

		return true;
	}

	public void setValue(T value) {
		List<T> current = values.getCurrent();
		int foundIndex = current.indexOf(value);
		if (foundIndex != -1) {
			index = foundIndex;
		}

		internalSetValue(value);
	}

	@Override
	public void update() {
		setValue(valueSupplier.get());
	}

	private void internalSetValue(T value) {
		setMessage(composeText(value));
		this.value = value;
		refreshTooltip();
	}

	private Text composeText(T value) {
		return labelType == CyclingButtonWidget.LabelType.VALUE
				? valueToText.apply(value)
				: composeGenericOptionText(value);
	}

	private MutableText composeGenericOptionText(T value) {
		return ScreenTexts.composeGenericOptionText(optionText, valueToText.apply(value));
	}

	public T getValue() {
		return value;
	}

	@Override
	protected MutableText getNarrationMessage() {
		return narrationMessageFactory.apply(this);
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		builder.put(NarrationPart.TITLE, getNarrationMessage());

		if (active) {
			Text nextValueText = composeText(getValue(1));
			String usageKey = isFocused()
					? "narration.cycle_button.usage.focused"
					: "narration.cycle_button.usage.hovered";
			builder.put(NarrationPart.USAGE, Text.translatable(usageKey, nextValueText));
		}
	}

	public MutableText getGenericNarrationMessage() {
		return getNarrationMessage(
				labelType == CyclingButtonWidget.LabelType.VALUE
				? composeGenericOptionText(value)
				: getMessage()
		);
	}

	public static <T> CyclingButtonWidget.Builder<T> builder(Function<T, Text> valueToText, Supplier<T> valueSupplier) {
		return new CyclingButtonWidget.Builder<>(valueToText, valueSupplier);
	}

	public static <T> CyclingButtonWidget.Builder<T> builder(Function<T, Text> valueToText, T value) {
		return new CyclingButtonWidget.Builder<>(valueToText, () -> value);
	}

	public static CyclingButtonWidget.Builder<Boolean> onOffBuilder(Text on, Text off, boolean defaultValue) {
		return new CyclingButtonWidget.Builder<>(value -> value == Boolean.TRUE ? on : off, () -> defaultValue).values(
				BOOLEAN_VALUES);
	}

	public static CyclingButtonWidget.Builder<Boolean> onOffBuilder(boolean defaultValue) {
		return new CyclingButtonWidget.Builder<>(
				value -> value == Boolean.TRUE ? ScreenTexts.ON : ScreenTexts.OFF,
				() -> defaultValue
		).values(BOOLEAN_VALUES);
	}

	/**
	 * Строитель для создания экземпляров {@link CyclingButtonWidget}.
	 */
	@Environment(EnvType.CLIENT)
	public static class Builder<T> {

		private final Supplier<T> valueSupplier;
		private final Function<T, Text> valueToText;
		private SimpleOption.TooltipFactory<T> tooltipFactory = value -> null;
		private CyclingButtonWidget.IconGetter<T> icon = (button, value) -> null;
		private Function<CyclingButtonWidget<T>, MutableText>
				narrationMessageFactory =
				CyclingButtonWidget::getGenericNarrationMessage;
		private CyclingButtonWidget.Values<T> values = CyclingButtonWidget.Values.of(ImmutableList.of());
		private CyclingButtonWidget.LabelType labelType = CyclingButtonWidget.LabelType.NAME_AND_VALUE;

		public Builder(Function<T, Text> valueToText, Supplier<T> valueSupplier) {
			this.valueToText = valueToText;
			this.valueSupplier = valueSupplier;
		}

		public CyclingButtonWidget.Builder<T> values(Collection<T> values) {
			return this.values(CyclingButtonWidget.Values.of(values));
		}

		@SafeVarargs
		public final CyclingButtonWidget.Builder<T> values(T... values) {
			return this.values(ImmutableList.copyOf(values));
		}

		public CyclingButtonWidget.Builder<T> values(List<T> defaults, List<T> alternatives) {
			return this.values(CyclingButtonWidget.Values.of(CyclingButtonWidget.HAS_ALT_DOWN, defaults, alternatives));
		}

		public CyclingButtonWidget.Builder<T> values(
				BooleanSupplier alternativeToggle,
				List<T> defaults,
				List<T> alternatives
		) {
			return this.values(CyclingButtonWidget.Values.of(alternativeToggle, defaults, alternatives));
		}

		public CyclingButtonWidget.Builder<T> values(CyclingButtonWidget.Values<T> values) {
			this.values = values;
			return this;
		}

		public CyclingButtonWidget.Builder<T> tooltip(SimpleOption.TooltipFactory<T> tooltipFactory) {
			this.tooltipFactory = tooltipFactory;
			return this;
		}

		public CyclingButtonWidget.Builder<T> narration(Function<CyclingButtonWidget<T>, MutableText> narrationMessageFactory) {
			this.narrationMessageFactory = narrationMessageFactory;
			return this;
		}

		public CyclingButtonWidget.Builder<T> icon(CyclingButtonWidget.IconGetter<T> icon) {
			this.icon = icon;
			return this;
		}

		public CyclingButtonWidget.Builder<T> labelType(CyclingButtonWidget.LabelType labelType) {
			this.labelType = labelType;
			return this;
		}

		public CyclingButtonWidget.Builder<T> omitKeyText() {
			return this.labelType(CyclingButtonWidget.LabelType.VALUE);
		}

		public CyclingButtonWidget<T> build(Text optionText, CyclingButtonWidget.UpdateCallback<T> callback) {
			return build(0, 0, 150, 20, optionText, callback);
		}

		public CyclingButtonWidget<T> build(int x, int y, int width, int height, Text optionText) {
			return build(x, y, width, height, optionText, (button, value) -> {});
		}

		public CyclingButtonWidget<T> build(
				int x,
				int y,
				int width,
				int height,
				Text optionText,
				CyclingButtonWidget.UpdateCallback<T> callback
		) {
			List<T> defaults = values.getDefaults();
			if (defaults.isEmpty()) {
				throw new IllegalStateException("No values for cycle button");
			}

			T currentValue = valueSupplier.get();
			int currentIndex = defaults.indexOf(currentValue);
			Text valueText = valueToText.apply(currentValue);
			Text displayText = labelType == CyclingButtonWidget.LabelType.VALUE
					? valueText
					: ScreenTexts.composeGenericOptionText(optionText, valueText);

			return new CyclingButtonWidget<>(
					x,
					y,
					width,
					height,
					displayText,
					optionText,
					currentIndex,
					currentValue,
					valueSupplier,
					values,
					valueToText,
					narrationMessageFactory,
					callback,
					tooltipFactory,
					labelType,
					icon
			);
		}
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	public interface IconGetter<T> {

		@Nullable Identifier apply(CyclingButtonWidget<T> button, T value);
	}

	@Environment(EnvType.CLIENT)
	public enum LabelType {
		NAME_AND_VALUE,
		VALUE,
		HIDE
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	public interface UpdateCallback<T> {

		void onValueChange(CyclingButtonWidget<T> button, T value);
	}

	@Environment(EnvType.CLIENT)
	public interface Values<T> {

		List<T> getCurrent();

		List<T> getDefaults();

		static <T> CyclingButtonWidget.Values<T> of(Collection<T> values) {
			final List<T> list = ImmutableList.copyOf(values);
			return new CyclingButtonWidget.Values<T>() {
				@Override
				public List<T> getCurrent() {
					return list;
				}

				@Override
				public List<T> getDefaults() {
					return list;
				}
			};
		}

		static <T> CyclingButtonWidget.Values<T> of(
				BooleanSupplier alternativeToggle,
				List<T> defaults,
				List<T> alternatives
		) {
			final List<T> list = ImmutableList.copyOf(defaults);
			final List<T> list2 = ImmutableList.copyOf(alternatives);
			return new CyclingButtonWidget.Values<T>() {
				@Override
				public List<T> getCurrent() {
					return alternativeToggle.getAsBoolean() ? list2 : list;
				}

				@Override
				public List<T> getDefaults() {
					return list;
				}
			};
		}
	}
}

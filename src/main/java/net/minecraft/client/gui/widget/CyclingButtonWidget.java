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

@Environment(EnvType.CLIENT)
/**
 * {@code CyclingButtonWidget}.
 */
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
		this.refreshTooltip();
	}

	@Override
	protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		Identifier identifier = this.icon.apply(this, this.getValue());
		if (identifier != null) {
			context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					identifier,
					this.getX(),
					this.getY(),
					this.getWidth(),
					this.getHeight()
			);
		}
		else {
			this.drawButton(context);
		}

		if (this.labelType != CyclingButtonWidget.LabelType.HIDE) {
			this.drawLabel(context.getHoverListener(this, DrawContext.HoverType.NONE));
		}
	}

	private void refreshTooltip() {
		this.setTooltip(this.tooltipFactory.apply(this.value));
	}

	@Override
	public void onPress(AbstractInput input) {
		if (input.hasShift()) {
			this.cycle(-1);
		}
		else {
			this.cycle(1);
		}
	}

	private void cycle(int amount) {
		List<T> list = this.values.getCurrent();
		this.index = MathHelper.floorMod(this.index + amount, list.size());
		T object = list.get(this.index);
		this.internalSetValue(object);
		this.callback.onValueChange(this, object);
	}

	private T getValue(int offset) {
		List<T> list = this.values.getCurrent();
		return list.get(MathHelper.floorMod(this.index + offset, list.size()));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (verticalAmount > 0.0) {
			this.cycle(-1);
		}
		else if (verticalAmount < 0.0) {
			this.cycle(1);
		}

		return true;
	}

	public void setValue(T value) {
		List<T> list = this.values.getCurrent();
		int i = list.indexOf(value);
		if (i != -1) {
			this.index = i;
		}

		this.internalSetValue(value);
	}

	@Override
	public void update() {
		this.setValue(this.valueSupplier.get());
	}

	private void internalSetValue(T value) {
		Text text = this.composeText(value);
		this.setMessage(text);
		this.value = value;
		this.refreshTooltip();
	}

	private Text composeText(T value) {
		return (Text) (this.labelType == CyclingButtonWidget.LabelType.VALUE ? this.valueToText.apply(value)
		                                                                     : this.composeGenericOptionText(value)
		);
	}

	private MutableText composeGenericOptionText(T value) {
		return ScreenTexts.composeGenericOptionText(this.optionText, this.valueToText.apply(value));
	}

	public T getValue() {
		return this.value;
	}

	@Override
	protected MutableText getNarrationMessage() {
		return this.narrationMessageFactory.apply(this);
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		builder.put(NarrationPart.TITLE, this.getNarrationMessage());
		if (this.active) {
			T object = this.getValue(1);
			Text text = this.composeText(object);
			if (this.isFocused()) {
				builder.put(NarrationPart.USAGE, Text.translatable("narration.cycle_button.usage.focused", text));
			}
			else {
				builder.put(NarrationPart.USAGE, Text.translatable("narration.cycle_button.usage.hovered", text));
			}
		}
	}

	public MutableText getGenericNarrationMessage() {
		return getNarrationMessage((Text) (this.labelType == CyclingButtonWidget.LabelType.VALUE
		                                   ? this.composeGenericOptionText(this.value) : this.getMessage()
		));
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

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Builder}.
	 */
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
			return this.build(0, 0, 150, 20, optionText, callback);
		}

		public CyclingButtonWidget<T> build(int x, int y, int width, int height, Text optionText) {
			return this.build(x, y, width, height, optionText, (button, value) -> {});
		}

		public CyclingButtonWidget<T> build(
				int x,
				int y,
				int width,
				int height,
				Text optionText,
				CyclingButtonWidget.UpdateCallback<T> callback
		) {
			List<T> list = this.values.getDefaults();
			if (list.isEmpty()) {
				throw new IllegalStateException("No values for cycle button");
			}
			else {
				T object = this.valueSupplier.get();
				int i = list.indexOf(object);
				Text text = this.valueToText.apply(object);
				Text
						text2 =
						(Text) (this.labelType == CyclingButtonWidget.LabelType.VALUE ? text
						                                                              : ScreenTexts.composeGenericOptionText(
								                                                              optionText,
								                                                              text
						                                                              )
						);
				return new CyclingButtonWidget<>(
						x,
						y,
						width,
						height,
						text2,
						optionText,
						i,
						object,
						this.valueSupplier,
						this.values,
						this.valueToText,
						this.narrationMessageFactory,
						callback,
						this.tooltipFactory,
						this.labelType,
						this.icon
				);
			}
		}
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	/**
	 * {@code IconGetter}.
	 */
	public interface IconGetter<T> {

		@Nullable Identifier apply(CyclingButtonWidget<T> button, T value);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code LabelType}.
	 */
	public static enum LabelType {
		NAME_AND_VALUE,
		VALUE,
		HIDE;
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	/**
	 * {@code UpdateCallback}.
	 */
	public interface UpdateCallback<T> {

		void onValueChange(CyclingButtonWidget<T> button, T value);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Values}.
	 */
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

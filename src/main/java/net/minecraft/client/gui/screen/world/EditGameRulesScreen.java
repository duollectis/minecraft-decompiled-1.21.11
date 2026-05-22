package net.minecraft.client.gui.screen.world;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.serialization.DataResult;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRuleCategory;
import net.minecraft.world.rule.GameRuleVisitor;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * Экран редактирования правил игры (gamerules) перед созданием мира.
 * Поддерживает булевые и целочисленные правила с валидацией ввода.
 */
@Environment(EnvType.CLIENT)
public class EditGameRulesScreen extends Screen {

	private static final Text TITLE = Text.translatable("editGamerule.title");
	private static final int RULE_PADDING = 8;
	final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);
	private final Consumer<Optional<GameRules>> ruleSaver;
	private final Set<EditGameRulesScreen.AbstractRuleWidget> invalidRuleWidgets = Sets.newHashSet();
	final GameRules gameRules;
	private EditGameRulesScreen.@Nullable RuleListWidget ruleListWidget;
	private @Nullable ButtonWidget doneButton;

	public EditGameRulesScreen(GameRules gameRules, Consumer<Optional<GameRules>> ruleSaveConsumer) {
		super(TITLE);
		this.gameRules = gameRules;
		this.ruleSaver = ruleSaveConsumer;
	}

	@Override
	protected void init() {
		layout.addHeader(TITLE, textRenderer);
		ruleListWidget = layout.addBody(new EditGameRulesScreen.RuleListWidget(gameRules));
		DirectionalLayoutWidget footer = layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
		doneButton = footer.add(
				ButtonWidget
						.builder(ScreenTexts.DONE, button -> ruleSaver.accept(Optional.of(gameRules)))
						.build()
		);
		footer.add(ButtonWidget.builder(ScreenTexts.CANCEL, button -> close()).build());
		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		layout.refreshPositions();
		if (ruleListWidget != null) {
			ruleListWidget.position(width, layout);
		}
	}

	@Override
	public void close() {
		ruleSaver.accept(Optional.empty());
	}

	private void updateDoneButton() {
		if (doneButton != null) {
			doneButton.active = invalidRuleWidgets.isEmpty();
		}
	}

	void markInvalid(EditGameRulesScreen.AbstractRuleWidget ruleWidget) {
		invalidRuleWidgets.add(ruleWidget);
		updateDoneButton();
	}

	void markValid(EditGameRulesScreen.AbstractRuleWidget ruleWidget) {
		invalidRuleWidgets.remove(ruleWidget);
		updateDoneButton();
	}

	@Environment(EnvType.CLIENT)
	public abstract static class AbstractRuleWidget extends ElementListWidget.Entry<EditGameRulesScreen.AbstractRuleWidget> {

		final @Nullable List<OrderedText> description;

		public AbstractRuleWidget(@Nullable List<OrderedText> description) {
			this.description = description;
		}
	}

	@Environment(EnvType.CLIENT)
	public class BooleanRuleWidget extends EditGameRulesScreen.NamedRuleWidget {

		private final CyclingButtonWidget<Boolean> toggleButton;

		public BooleanRuleWidget(
				final Text name,
				final List<OrderedText> description,
				final String ruleName,
				final GameRule<Boolean> rule
		) {
			super(description, name);
			this.toggleButton = CyclingButtonWidget.onOffBuilder(EditGameRulesScreen.this.gameRules.getValue(rule))
			                                       .omitKeyText()
			                                       .narration(button -> button
					                                       .getGenericNarrationMessage()
					                                       .append("\n")
					                                       .append(ruleName))
			                                       .build(
					                                       10,
					                                       5,
					                                       44,
					                                       20,
					                                       name,
					                                       (button, value) -> EditGameRulesScreen.this.gameRules.setValue(
							                                       rule,
							                                       value,
							                                       null
					                                       )
			                                       );
			this.children.add(this.toggleButton);
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			this.drawName(context, this.getContentY(), this.getContentX());
			this.toggleButton.setX(this.getContentRightEnd() - 45);
			this.toggleButton.setY(this.getContentY());
			this.toggleButton.render(context, mouseX, mouseY, deltaTicks);
		}
	}

	@Environment(EnvType.CLIENT)
	public class IntRuleWidget extends EditGameRulesScreen.NamedRuleWidget {

		private final TextFieldWidget valueWidget;

		public IntRuleWidget(
				final Text name,
				final List<OrderedText> description,
				final String ruleName,
				final GameRule<Integer> rule
		) {
			super(description, name);
			this.valueWidget = new TextFieldWidget(
					EditGameRulesScreen.this.client.textRenderer,
					10,
					5,
					44,
					20,
					name.copy().append("\n").append(ruleName).append("\n")
			);
			this.valueWidget.setText(EditGameRulesScreen.this.gameRules.getRuleValueName(rule));
			this.valueWidget.setChangedListener(value -> {
				DataResult<Integer> dataResult = rule.deserialize(value);
				if (dataResult.isSuccess()) {
					this.valueWidget.setEditableColor(-2039584);
					EditGameRulesScreen.this.markValid(this);
					EditGameRulesScreen.this.gameRules.setValue(rule, (Integer) dataResult.getOrThrow(), null);
				}
				else {
					this.valueWidget.setEditableColor(-65536);
					EditGameRulesScreen.this.markInvalid(this);
				}
			});
			this.children.add(this.valueWidget);
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			this.drawName(context, this.getContentY(), this.getContentX());
			this.valueWidget.setX(this.getContentRightEnd() - 45);
			this.valueWidget.setY(this.getContentY());
			this.valueWidget.render(context, mouseX, mouseY, deltaTicks);
		}
	}

	@Environment(EnvType.CLIENT)
	public abstract class NamedRuleWidget extends EditGameRulesScreen.AbstractRuleWidget {

		private final List<OrderedText> name;
		protected final List<ClickableWidget> children = Lists.newArrayList();

		public NamedRuleWidget(final List<OrderedText> description, final Text name) {
			super(description);
			this.name = EditGameRulesScreen.this.client.textRenderer.wrapLines(name, 175);
		}

		@Override
		public List<? extends Element> children() {
			return this.children;
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return this.children;
		}

		protected void drawName(DrawContext context, int x, int y) {
			if (this.name.size() == 1) {
				context.drawTextWithShadow(
						EditGameRulesScreen.this.client.textRenderer,
						this.name.get(0),
						y,
						x + 5,
						-1
				);
			}
			else if (this.name.size() >= 2) {
				context.drawTextWithShadow(EditGameRulesScreen.this.client.textRenderer, this.name.get(0), y, x, -1);
				context.drawTextWithShadow(
						EditGameRulesScreen.this.client.textRenderer,
						this.name.get(1),
						y,
						x + 10,
						-1
				);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public class RuleCategoryWidget extends EditGameRulesScreen.AbstractRuleWidget {

		final Text name;

		public RuleCategoryWidget(final Text text) {
			super(null);
			this.name = text;
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			context.drawCenteredTextWithShadow(
					EditGameRulesScreen.this.client.textRenderer,
					this.name,
					this.getContentMiddleX(),
					this.getContentY() + 5,
					-1
			);
		}

		@Override
		public List<? extends Element> children() {
			return ImmutableList.of();
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return ImmutableList.of(new Selectable() {
				@Override
				public Selectable.SelectionType getType() {
					return Selectable.SelectionType.HOVERED;
				}

				@Override
				public void appendNarrations(NarrationMessageBuilder builder) {
					builder.put(NarrationPart.TITLE, RuleCategoryWidget.this.name);
				}
			});
		}
	}

	@Environment(EnvType.CLIENT)
	public class RuleListWidget extends ElementListWidget<EditGameRulesScreen.AbstractRuleWidget> {

		private static final int RULE_ENTRY_HEIGHT = 24;

		public RuleListWidget(final GameRules gameRules) {
			super(
					MinecraftClient.getInstance(),
					EditGameRulesScreen.this.width,
					EditGameRulesScreen.this.layout.getContentHeight(),
					EditGameRulesScreen.this.layout.getHeaderHeight(),
					RULE_ENTRY_HEIGHT
			);
			final Map<GameRuleCategory, Map<GameRule<?>, EditGameRulesScreen.AbstractRuleWidget>>
					map =
					Maps.newHashMap();
			gameRules.accept(
					new GameRuleVisitor() {
						@Override
						public void visitBoolean(GameRule<Boolean> rule) {
							this.createRuleWidget(
									rule,
									(name, description, ruleName, rulex) -> EditGameRulesScreen.this.new BooleanRuleWidget(
											name,
											description,
											ruleName,
											rulex
									)
							);
						}

						@Override
						public void visitInt(GameRule<Integer> rule) {
							this.createRuleWidget(
									rule,
									(name, description, ruleName, rulex) -> EditGameRulesScreen.this.new IntRuleWidget(
											name,
											description,
											ruleName,
											rulex
									)
							);
						}

						private <T> void createRuleWidget(
								GameRule<T> key,
								EditGameRulesScreen.RuleWidgetFactory<T> widgetFactory
						) {
							Text ruleName = Text.translatable(key.getTranslationKey());
							Text ruleShortName = Text.literal(key.toShortString()).formatted(Formatting.YELLOW);
							Text defaultValueText = Text
									.translatable(
											"editGamerule.default",
											Text.literal(key.getValueName(key.getDefaultValue()))
									)
									.formatted(Formatting.GRAY);
							String descriptionKey = key.getTranslationKey() + ".description";
							List<OrderedText> description;
							String narrationText;

							if (I18n.hasTranslation(descriptionKey)) {
								ImmutableList.Builder<OrderedText> builder =
										ImmutableList.<OrderedText>builder().add(ruleShortName.asOrderedText());
								Text descriptionText = Text.translatable(descriptionKey);
								EditGameRulesScreen.this.textRenderer.wrapLines(descriptionText, 150).forEach(builder::add);
								description = builder.add(defaultValueText.asOrderedText()).build();
								narrationText = descriptionText.getString() + "\n" + defaultValueText.getString();
							}
							else {
								description = ImmutableList.of(ruleShortName.asOrderedText(), defaultValueText.asOrderedText());
								narrationText = defaultValueText.getString();
							}

							map
									.computeIfAbsent(key.getCategory(), category -> Maps.newHashMap())
									.put(key, widgetFactory.create(ruleName, description, narrationText, key));
						}
					}
			);
			map.entrySet()
			   .stream()
			   .sorted(Map.Entry.comparingByKey(Comparator.comparing(GameRuleCategory::getCategory)))
			   .forEach(
					   entry -> {
						   this.addEntry(EditGameRulesScreen.this.new RuleCategoryWidget(entry
								   .getKey()
								   .getText()
								   .formatted(Formatting.BOLD, Formatting.YELLOW)));
						   entry.getValue()
						        .entrySet()
						        .stream()
						        .sorted(Map.Entry.comparingByKey(Comparator.comparing(GameRule::getTranslationKey)))
						        .forEach(e -> this.addEntry(e.getValue()));
					   }
			   );
		}

		@Override
		public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
			super.renderWidget(context, mouseX, mouseY, deltaTicks);
			EditGameRulesScreen.AbstractRuleWidget abstractRuleWidget = this.getHoveredEntry();
			if (abstractRuleWidget != null && abstractRuleWidget.description != null) {
				context.drawTooltip(abstractRuleWidget.description, mouseX, mouseY);
			}
		}
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	interface RuleWidgetFactory<T> {

		EditGameRulesScreen.AbstractRuleWidget create(
				Text name,
				List<OrderedText> description,
				String ruleName,
				GameRule<T> rule
		);
	}
}

package net.minecraft.client.gui.screen.world;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.DataResult;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
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

@Environment(EnvType.CLIENT)
public class EditGameRulesScreen extends Screen {
   private static final Text TITLE = Text.translatable("editGamerule.title");
   private static final int field_49559 = 8;
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
      this.layout.addHeader(TITLE, this.textRenderer);
      this.ruleListWidget = this.layout.addBody(new EditGameRulesScreen.RuleListWidget(this.gameRules));
      DirectionalLayoutWidget directionalLayoutWidget = this.layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
      this.doneButton = directionalLayoutWidget.add(
         ButtonWidget.builder(ScreenTexts.DONE, button -> this.ruleSaver.accept(Optional.of(this.gameRules))).build()
      );
      directionalLayoutWidget.add(ButtonWidget.builder(ScreenTexts.CANCEL, button -> this.close()).build());
      this.layout.forEachChild(child -> {
         ClickableWidget var10000 = this.addDrawableChild(child);
      });
      this.refreshWidgetPositions();
   }

   @Override
   protected void refreshWidgetPositions() {
      this.layout.refreshPositions();
      if (this.ruleListWidget != null) {
         this.ruleListWidget.position(this.width, this.layout);
      }
   }

   @Override
   public void close() {
      this.ruleSaver.accept(Optional.empty());
   }

   private void updateDoneButton() {
      if (this.doneButton != null) {
         this.doneButton.active = this.invalidRuleWidgets.isEmpty();
      }
   }

   void markInvalid(EditGameRulesScreen.AbstractRuleWidget ruleWidget) {
      this.invalidRuleWidgets.add(ruleWidget);
      this.updateDoneButton();
   }

   void markValid(EditGameRulesScreen.AbstractRuleWidget ruleWidget) {
      this.invalidRuleWidgets.remove(ruleWidget);
      this.updateDoneButton();
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

      public BooleanRuleWidget(final Text name, final List<OrderedText> description, final String ruleName, final GameRule<Boolean> rule) {
         super(description, name);
         this.toggleButton = CyclingButtonWidget.onOffBuilder(EditGameRulesScreen.this.gameRules.getValue(rule))
            .omitKeyText()
            .narration(button -> button.getGenericNarrationMessage().append("\n").append(ruleName))
            .build(10, 5, 44, 20, name, (button, value) -> EditGameRulesScreen.this.gameRules.setValue(rule, value, null));
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

      public IntRuleWidget(final Text name, final List<OrderedText> description, final String ruleName, final GameRule<Integer> rule) {
         super(description, name);
         this.valueWidget = new TextFieldWidget(
            EditGameRulesScreen.this.client.textRenderer, 10, 5, 44, 20, name.copy().append("\n").append(ruleName).append("\n")
         );
         this.valueWidget.setText(EditGameRulesScreen.this.gameRules.getRuleValueName(rule));
         this.valueWidget.setChangedListener(value -> {
            DataResult<Integer> dataResult = rule.deserialize(value);
            if (dataResult.isSuccess()) {
               this.valueWidget.setEditableColor(-2039584);
               EditGameRulesScreen.this.markValid(this);
               EditGameRulesScreen.this.gameRules.setValue(rule, (Integer)dataResult.getOrThrow(), null);
            } else {
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
            context.drawTextWithShadow(EditGameRulesScreen.this.client.textRenderer, this.name.get(0), y, x + 5, -1);
         } else if (this.name.size() >= 2) {
            context.drawTextWithShadow(EditGameRulesScreen.this.client.textRenderer, this.name.get(0), y, x, -1);
            context.drawTextWithShadow(EditGameRulesScreen.this.client.textRenderer, this.name.get(1), y, x + 10, -1);
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
         context.drawCenteredTextWithShadow(EditGameRulesScreen.this.client.textRenderer, this.name, this.getContentMiddleX(), this.getContentY() + 5, -1);
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
      private static final int field_49561 = 24;

      public RuleListWidget(final GameRules gameRules) {
         super(
            MinecraftClient.getInstance(),
            EditGameRulesScreen.this.width,
            EditGameRulesScreen.this.layout.getContentHeight(),
            EditGameRulesScreen.this.layout.getHeaderHeight(),
            24
         );
         final Map<GameRuleCategory, Map<GameRule<?>, EditGameRulesScreen.AbstractRuleWidget>> map = Maps.newHashMap();
         gameRules.accept(
            new GameRuleVisitor() {
               @Override
               public void visitBoolean(GameRule<Boolean> rule) {
                  this.createRuleWidget(
                     rule, (name, description, ruleName, rulex) -> EditGameRulesScreen.this.new BooleanRuleWidget(name, description, ruleName, rulex)
                  );
               }

               @Override
               public void visitInt(GameRule<Integer> rule) {
                  this.createRuleWidget(
                     rule, (name, description, ruleName, rulex) -> EditGameRulesScreen.this.new IntRuleWidget(name, description, ruleName, rulex)
                  );
               }

               private <T> void createRuleWidget(GameRule<T> key, EditGameRulesScreen.RuleWidgetFactory<T> widgetFactory) {
                  Text text = Text.translatable(key.getTranslationKey());
                  Text text2 = Text.literal(key.toShortString()).formatted(Formatting.YELLOW);
                  Text text3 = Text.translatable("editGamerule.default", Text.literal(key.getValueName(key.getDefaultValue()))).formatted(Formatting.GRAY);
                  String string = key.getTranslationKey() + ".description";
                  List<OrderedText> list;
                  String string2;
                  if (I18n.hasTranslation(string)) {
                     ImmutableList.Builder<OrderedText> builder = ImmutableList.<OrderedText>builder().add(text2.asOrderedText());
                     Text text4 = Text.translatable(string);
                     EditGameRulesScreen.this.textRenderer.wrapLines(text4, 150).forEach(builder::add);
                     list = builder.add(text3.asOrderedText()).build();
                     string2 = text4.getString() + "\n" + text3.getString();
                  } else {
                     list = ImmutableList.of(text2.asOrderedText(), text3.asOrderedText());
                     string2 = text3.getString();
                  }

                  map.computeIfAbsent(key.getCategory(), category -> Maps.newHashMap()).put(key, widgetFactory.create(text, list, string2, key));
               }
            }
         );
         map.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparing(GameRuleCategory::getCategory)))
            .forEach(
               entry -> {
                  this.addEntry(EditGameRulesScreen.this.new RuleCategoryWidget(entry.getKey().getText().formatted(Formatting.BOLD, Formatting.YELLOW)));
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
      EditGameRulesScreen.AbstractRuleWidget create(Text name, List<OrderedText> description, String ruleName, GameRule<T> rule);
   }
}

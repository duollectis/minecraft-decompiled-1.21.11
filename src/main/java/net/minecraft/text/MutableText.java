package net.minecraft.text;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import org.jspecify.annotations.Nullable;

public final class MutableText implements Text {
   private final TextContent content;
   private final List<Text> siblings;
   private Style style;
   private OrderedText ordered = OrderedText.EMPTY;
   private @Nullable Language language;

   MutableText(TextContent content, List<Text> siblings, Style style) {
      this.content = content;
      this.siblings = siblings;
      this.style = style;
   }

   public static MutableText of(TextContent content) {
      return new MutableText(content, Lists.newArrayList(), Style.EMPTY);
   }

   @Override
   public TextContent getContent() {
      return this.content;
   }

   @Override
   public List<Text> getSiblings() {
      return this.siblings;
   }

   public MutableText setStyle(Style style) {
      this.style = style;
      return this;
   }

   @Override
   public Style getStyle() {
      return this.style;
   }

   public MutableText append(String text) {
      return text.isEmpty() ? this : this.append(Text.literal(text));
   }

   public MutableText append(Text text) {
      this.siblings.add(text);
      return this;
   }

   public MutableText styled(UnaryOperator<Style> styleUpdater) {
      this.setStyle(styleUpdater.apply(this.getStyle()));
      return this;
   }

   public MutableText fillStyle(Style styleOverride) {
      this.setStyle(styleOverride.withParent(this.getStyle()));
      return this;
   }

   public MutableText formatted(Formatting... formattings) {
      this.setStyle(this.getStyle().withFormatting(formattings));
      return this;
   }

   public MutableText formatted(Formatting formatting) {
      this.setStyle(this.getStyle().withFormatting(formatting));
      return this;
   }

   public MutableText withColor(int color) {
      this.setStyle(this.getStyle().withColor(color));
      return this;
   }

   public MutableText withoutShadow() {
      this.setStyle(this.getStyle().withoutShadow());
      return this;
   }

   @Override
   public OrderedText asOrderedText() {
      Language language = Language.getInstance();
      if (this.language != language) {
         this.ordered = language.reorder(this);
         this.language = language;
      }

      return this.ordered;
   }

   @Override
   public boolean equals(Object o) {
      return this == o
         ? true
         : o instanceof MutableText mutableText
            && this.content.equals(mutableText.content)
            && this.style.equals(mutableText.style)
            && this.siblings.equals(mutableText.siblings);
   }

   @Override
   public int hashCode() {
      int i = 1;
      i = 31 * i + this.content.hashCode();
      i = 31 * i + this.style.hashCode();
      return 31 * i + this.siblings.hashCode();
   }

   @Override
   public String toString() {
      StringBuilder stringBuilder = new StringBuilder(this.content.toString());
      boolean bl = !this.style.isEmpty();
      boolean bl2 = !this.siblings.isEmpty();
      if (bl || bl2) {
         stringBuilder.append('[');
         if (bl) {
            stringBuilder.append("style=");
            stringBuilder.append(this.style);
         }

         if (bl && bl2) {
            stringBuilder.append(", ");
         }

         if (bl2) {
            stringBuilder.append("siblings=");
            stringBuilder.append(this.siblings);
         }

         stringBuilder.append(']');
      }

      return stringBuilder.toString();
   }
}

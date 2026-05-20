package net.minecraft.client.font;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Language;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface MultilineText {
   MultilineText EMPTY = new MultilineText() {
      @Override
      public int draw(Alignment alignment, int x, int y, int lineHeight, DrawnTextConsumer consumer) {
         return y;
      }

      @Override
      public int getLineCount() {
         return 0;
      }

      @Override
      public int getMaxWidth() {
         return 0;
      }
   };

   static MultilineText create(TextRenderer renderer, Text... texts) {
      return create(renderer, Integer.MAX_VALUE, Integer.MAX_VALUE, texts);
   }

   static MultilineText create(TextRenderer renderer, int maxWidth, Text... texts) {
      return create(renderer, maxWidth, Integer.MAX_VALUE, texts);
   }

   static MultilineText create(TextRenderer renderer, Text text, int maxWidth) {
      return create(renderer, maxWidth, Integer.MAX_VALUE, text);
   }

   static MultilineText create(TextRenderer textRenderer, int maxWidth, int maxLines, Text... texts) {
      return texts.length == 0
         ? EMPTY
         : new MultilineText() {
            private @Nullable List<MultilineText.Line> lines;
            private @Nullable Language language;

            @Override
            public int draw(Alignment alignment, int x, int y, int lineHeight, DrawnTextConsumer consumer) {
               int i = y;

               for (MultilineText.Line line : this.getLines()) {
                  int j = alignment.getAdjustedX(x, line.width);
                  consumer.text(j, i, line.text);
                  i += lineHeight;
               }

               return i;
            }

            private List<MultilineText.Line> getLines() {
               Language language = Language.getInstance();
               if (this.lines != null && language == this.language) {
                  return this.lines;
               } else {
                  this.language = language;
                  List<StringVisitable> list = new ArrayList<>();

                  for (Text text : texts) {
                     list.addAll(textRenderer.wrapLinesWithoutLanguage(text, maxWidth));
                  }

                  this.lines = new ArrayList<>();
                  int i = Math.min(list.size(), maxLines);
                  List<StringVisitable> list2 = list.subList(0, i);

                  for (int j = 0; j < list2.size(); j++) {
                     StringVisitable stringVisitable = list2.get(j);
                     OrderedText orderedText = Language.getInstance().reorder(stringVisitable);
                     if (j == list2.size() - 1 && i == maxLines && i != list.size()) {
                        StringVisitable stringVisitable2 = textRenderer.trimToWidth(
                           stringVisitable, textRenderer.getWidth(stringVisitable) - textRenderer.getWidth(ScreenTexts.ELLIPSIS)
                        );
                        StringVisitable stringVisitable3 = StringVisitable.concat(
                           stringVisitable2, ScreenTexts.ELLIPSIS.copy().fillStyle(texts[texts.length - 1].getStyle())
                        );
                        this.lines.add(new MultilineText.Line(Language.getInstance().reorder(stringVisitable3), textRenderer.getWidth(stringVisitable3)));
                     } else {
                        this.lines.add(new MultilineText.Line(orderedText, textRenderer.getWidth(orderedText)));
                     }
                  }

                  return this.lines;
               }
            }

            @Override
            public int getLineCount() {
               return this.getLines().size();
            }

            @Override
            public int getMaxWidth() {
               return Math.min(maxWidth, this.getLines().stream().mapToInt(MultilineText.Line::width).max().orElse(0));
            }
         };
   }

   int draw(Alignment alignment, int x, int y, int lineHeight, DrawnTextConsumer consumer);

   int getLineCount();

   int getMaxWidth();

   @Environment(EnvType.CLIENT)
   public record Line(OrderedText text, int width) {
   }
}

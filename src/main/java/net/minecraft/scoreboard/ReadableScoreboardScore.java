package net.minecraft.scoreboard;

import java.util.Objects;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.text.MutableText;
import org.jspecify.annotations.Nullable;

public interface ReadableScoreboardScore {
   int getScore();

   boolean isLocked();

   @Nullable NumberFormat getNumberFormat();

   default MutableText getFormattedScore(NumberFormat fallbackFormat) {
      return Objects.requireNonNullElse(this.getNumberFormat(), fallbackFormat).format(this.getScore());
   }

   static MutableText getFormattedScore(@Nullable ReadableScoreboardScore score, NumberFormat fallbackFormat) {
      return score != null ? score.getFormattedScore(fallbackFormat) : fallbackFormat.format(0);
   }
}

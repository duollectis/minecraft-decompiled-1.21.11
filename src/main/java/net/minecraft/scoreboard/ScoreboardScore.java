package net.minecraft.scoreboard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.NumberFormatTypes;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * {@code ScoreboardScore}.
 */
public class ScoreboardScore implements ReadableScoreboardScore {

	private int score;
	private boolean locked = true;
	private @Nullable Text displayText;
	private @Nullable NumberFormat numberFormat;

	public ScoreboardScore() {
	}

	public ScoreboardScore(ScoreboardScore.Packed packed) {
		this.score = packed.value;
		this.locked = packed.locked;
		this.displayText = packed.display.orElse(null);
		this.numberFormat = packed.numberFormat.orElse(null);
	}

	public ScoreboardScore.Packed toPacked() {
		return new ScoreboardScore.Packed(
				this.score,
				this.locked,
				Optional.ofNullable(this.displayText),
				Optional.ofNullable(this.numberFormat)
		);
	}

	@Override
	public int getScore() {
		return this.score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	@Override
	public boolean isLocked() {
		return this.locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public @Nullable Text getDisplayText() {
		return this.displayText;
	}

	public void setDisplayText(@Nullable Text text) {
		this.displayText = text;
	}

	@Override
	public @Nullable NumberFormat getNumberFormat() {
		return this.numberFormat;
	}

	public void setNumberFormat(@Nullable NumberFormat numberFormat) {
		this.numberFormat = numberFormat;
	}

	/**
	 * {@code Packed}.
	 */
	public record Packed(int value, boolean locked, Optional<Text> display, Optional<NumberFormat> numberFormat) {

		public static final MapCodec<ScoreboardScore.Packed> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    Codec.INT.optionalFieldOf("Score", 0).forGetter(ScoreboardScore.Packed::value),
						                    Codec.BOOL.optionalFieldOf("Locked", false).forGetter(ScoreboardScore.Packed::locked),
						                    TextCodecs.CODEC.optionalFieldOf("display").forGetter(ScoreboardScore.Packed::display),
						                    NumberFormatTypes.CODEC
								                    .optionalFieldOf("format")
								                    .forGetter(ScoreboardScore.Packed::numberFormat)
				                    )
				                    .apply(instance, ScoreboardScore.Packed::new)
		);
	}
}

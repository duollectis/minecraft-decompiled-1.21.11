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
 * Хранит данные одного очка держателя по конкретной цели скорборда.
 * <p>
 * По умолчанию очко создаётся заблокированным ({@code locked = true}),
 * что предотвращает его изменение через триггерный критерий до явной разблокировки.
 */
public class ScoreboardScore implements ReadableScoreboardScore {

	private int score;
	private boolean locked = true;
	private @Nullable Text displayText;
	private @Nullable NumberFormat numberFormat;

	public ScoreboardScore() {
	}

	public ScoreboardScore(Packed packed) {
		this.score = packed.value;
		this.locked = packed.locked;
		this.displayText = packed.display.orElse(null);
		this.numberFormat = packed.numberFormat.orElse(null);
	}

	public Packed toPacked() {
		return new Packed(
				score,
				locked,
				Optional.ofNullable(displayText),
				Optional.ofNullable(numberFormat)
		);
	}

	@Override
	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	@Override
	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public @Nullable Text getDisplayText() {
		return displayText;
	}

	public void setDisplayText(@Nullable Text displayText) {
		this.displayText = displayText;
	}

	@Override
	public @Nullable NumberFormat getNumberFormat() {
		return numberFormat;
	}

	public void setNumberFormat(@Nullable NumberFormat numberFormat) {
		this.numberFormat = numberFormat;
	}

	/**
	 * Упакованное представление очка для сериализации в NBT/JSON.
	 * Значение по умолчанию — 0, состояние блокировки — {@code false}.
	 */
	public record Packed(int value, boolean locked, Optional<Text> display, Optional<NumberFormat> numberFormat) {

		public static final MapCodec<Packed> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						Codec.INT.optionalFieldOf("Score", 0).forGetter(Packed::value),
						Codec.BOOL.optionalFieldOf("Locked", false).forGetter(Packed::locked),
						TextCodecs.CODEC.optionalFieldOf("display").forGetter(Packed::display),
						NumberFormatTypes.CODEC.optionalFieldOf("format").forGetter(Packed::numberFormat)
				).apply(instance, Packed::new)
		);
	}
}

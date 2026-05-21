package net.minecraft.world.gen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.world.dimension.DimensionType;

import java.util.function.Function;

/**
 * {@code YOffset}.
 */
public interface YOffset {

	Codec<YOffset>
			OFFSET_CODEC =
			Codec.xor(YOffset.Fixed.CODEC, Codec.xor(YOffset.AboveBottom.CODEC, YOffset.BelowTop.CODEC))
			     .xmap(YOffset::fromEither, YOffset::map);

	YOffset BOTTOM = aboveBottom(0);

	YOffset TOP = belowTop(0);

	static YOffset fixed(int offset) {
		return new YOffset.Fixed(offset);
	}

	static YOffset aboveBottom(int offset) {
		return new YOffset.AboveBottom(offset);
	}

	static YOffset belowTop(int offset) {
		return new YOffset.BelowTop(offset);
	}

	static YOffset getBottom() {
		return BOTTOM;
	}

	static YOffset getTop() {
		return TOP;
	}

	private static YOffset fromEither(Either<YOffset.Fixed, Either<YOffset.AboveBottom, YOffset.BelowTop>> either) {
		return (YOffset) either.map(Function.identity(), Either::unwrap);
	}

	private static Either<YOffset.Fixed, Either<YOffset.AboveBottom, YOffset.BelowTop>> map(YOffset yOffset) {
		return yOffset instanceof YOffset.Fixed
		       ? Either.left((YOffset.Fixed) yOffset)
		       : Either.right(yOffset instanceof YOffset.AboveBottom ? Either.left((YOffset.AboveBottom) yOffset)
		                                                             : Either.right((YOffset.BelowTop) yOffset));
	}

	int getY(HeightContext context);

	/**
	 * {@code AboveBottom}.
	 */
	public record AboveBottom(int offset) implements YOffset {

		public static final Codec<YOffset.AboveBottom>
				CODEC =
				Codec.intRange(DimensionType.MIN_HEIGHT, DimensionType.MAX_COLUMN_HEIGHT)
				     .fieldOf("above_bottom")
				     .xmap(YOffset.AboveBottom::new, YOffset.AboveBottom::offset)
				     .codec();

		@Override
		public int getY(HeightContext context) {
			return context.getMinY() + this.offset;
		}

		@Override
		public String toString() {
			return this.offset + " above bottom";
		}
	}

	/**
	 * {@code BelowTop}.
	 */
	public record BelowTop(int offset) implements YOffset {

		public static final Codec<YOffset.BelowTop>
				CODEC =
				Codec.intRange(DimensionType.MIN_HEIGHT, DimensionType.MAX_COLUMN_HEIGHT)
				     .fieldOf("below_top")
				     .xmap(YOffset.BelowTop::new, YOffset.BelowTop::offset)
				     .codec();

		@Override
		public int getY(HeightContext context) {
			return context.getHeight() - 1 + context.getMinY() - this.offset;
		}

		@Override
		public String toString() {
			return this.offset + " below top";
		}
	}

	/**
	 * {@code Fixed}.
	 */
	public record Fixed(int y) implements YOffset {

		public static final Codec<YOffset.Fixed>
				CODEC =
				Codec.intRange(DimensionType.MIN_HEIGHT, DimensionType.MAX_COLUMN_HEIGHT)
				     .fieldOf("absolute")
				     .xmap(YOffset.Fixed::new, YOffset.Fixed::y)
				     .codec();

		@Override
		public int getY(HeightContext context) {
			return this.y;
		}

		@Override
		public String toString() {
			return this.y + " absolute";
		}
	}
}

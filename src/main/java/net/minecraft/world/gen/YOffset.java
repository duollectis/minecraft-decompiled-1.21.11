package net.minecraft.world.gen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.world.dimension.DimensionType;

import java.util.function.Function;

/**
 * Смещение по оси Y для генерации фич и структур.
 * Поддерживает три режима: абсолютная координата, смещение от дна и смещение от верха мира.
 */
public interface YOffset {

	Codec<YOffset> OFFSET_CODEC =
		Codec.xor(Fixed.CODEC, Codec.xor(AboveBottom.CODEC, BelowTop.CODEC))
			.xmap(YOffset::fromEither, YOffset::toEither);

	YOffset BOTTOM = aboveBottom(0);
	YOffset TOP = belowTop(0);

	static YOffset fixed(int offset) {
		return new Fixed(offset);
	}

	static YOffset aboveBottom(int offset) {
		return new AboveBottom(offset);
	}

	static YOffset belowTop(int offset) {
		return new BelowTop(offset);
	}

	static YOffset getBottom() {
		return BOTTOM;
	}

	static YOffset getTop() {
		return TOP;
	}

	private static YOffset fromEither(Either<Fixed, Either<AboveBottom, BelowTop>> either) {
		return (YOffset) either.map(Function.identity(), Either::unwrap);
	}

	private static Either<Fixed, Either<AboveBottom, BelowTop>> toEither(YOffset yOffset) {
		return yOffset instanceof Fixed fixed
			? Either.left(fixed)
			: Either.right(yOffset instanceof AboveBottom above
				? Either.left(above)
				: Either.right((BelowTop) yOffset));
	}

	int getY(HeightContext context);

	/**
	 * Смещение выше дна мира.
	 */
	record AboveBottom(int offset) implements YOffset {

		public static final Codec<AboveBottom> CODEC =
			Codec.intRange(DimensionType.MIN_HEIGHT, DimensionType.MAX_COLUMN_HEIGHT)
				.fieldOf("above_bottom")
				.xmap(AboveBottom::new, AboveBottom::offset)
				.codec();

		@Override
		public int getY(HeightContext context) {
			return context.getMinY() + offset;
		}

		@Override
		public String toString() {
			return offset + " above bottom";
		}
	}

	/**
	 * Смещение ниже верха мира.
	 */
	record BelowTop(int offset) implements YOffset {

		public static final Codec<BelowTop> CODEC =
			Codec.intRange(DimensionType.MIN_HEIGHT, DimensionType.MAX_COLUMN_HEIGHT)
				.fieldOf("below_top")
				.xmap(BelowTop::new, BelowTop::offset)
				.codec();

		@Override
		public int getY(HeightContext context) {
			return context.getHeight() - 1 + context.getMinY() - offset;
		}

		@Override
		public String toString() {
			return offset + " below top";
		}
	}

	/**
	 * Абсолютная координата Y.
	 */
	record Fixed(int y) implements YOffset {

		public static final Codec<Fixed> CODEC =
			Codec.intRange(DimensionType.MIN_HEIGHT, DimensionType.MAX_COLUMN_HEIGHT)
				.fieldOf("absolute")
				.xmap(Fixed::new, Fixed::y)
				.codec();

		@Override
		public int getY(HeightContext context) {
			return y;
		}

		@Override
		public String toString() {
			return y + " absolute";
		}
	}
}

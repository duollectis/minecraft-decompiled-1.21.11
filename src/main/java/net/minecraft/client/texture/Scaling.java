package net.minecraft.client.texture;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.dynamic.Codecs;

import java.util.OptionalInt;

/**
 * Стратегия масштабирования текстуры GUI-элемента.
 *
 * <p>Три реализации: {@link Stretch} — растягивает текстуру целиком,
 * {@link Tile} — повторяет текстуру плиткой, {@link NineSlice} — делит
 * текстуру на 9 частей (углы, рёбра, центр) для корректного масштабирования рамок.
 */
@Environment(EnvType.CLIENT)
public interface Scaling {

	Codec<Scaling> CODEC = Type.CODEC.dispatch(Scaling::getType, Type::getCodec);

	Scaling STRETCH = new Stretch();

	Scaling.Type getType();

	/**
	 * Масштабирование по принципу «9-slice»: текстура делится на 9 частей
	 * границами {@link Border}. Углы не масштабируются, рёбра тянутся вдоль
	 * одной оси, центр масштабируется (или повторяется, если {@code stretchInner = false}).
	 */
	@Environment(EnvType.CLIENT)
	record NineSlice(
		int width,
		int height,
		Border border,
		boolean stretchInner
	) implements Scaling {

		public static final MapCodec<NineSlice> CODEC = RecordCodecBuilder.<NineSlice>mapCodec(
			instance -> instance.group(
				Codecs.POSITIVE_INT.fieldOf("width").forGetter(NineSlice::width),
				Codecs.POSITIVE_INT.fieldOf("height").forGetter(NineSlice::height),
				Border.CODEC.fieldOf("border").forGetter(NineSlice::border),
				Codec.BOOL.optionalFieldOf("stretch_inner", false).forGetter(NineSlice::stretchInner)
			)
			.apply(instance, NineSlice::new)
		)
		.validate(NineSlice::validate);

		private static DataResult<NineSlice> validate(NineSlice nineSlice) {
			Border border = nineSlice.border();

			if (border.left() + border.right() >= nineSlice.width()) {
				return DataResult.error(
					() -> "Nine-sliced texture has no horizontal center slice: "
						+ border.left() + " + " + border.right() + " >= " + nineSlice.width()
				);
			}

			return border.top() + border.bottom() >= nineSlice.height()
				? DataResult.error(
					() -> "Nine-sliced texture has no vertical center slice: "
						+ border.top() + " + " + border.bottom() + " >= " + nineSlice.height()
				)
				: DataResult.success(nineSlice);
		}

		@Override
		public Type getType() {
			return Type.NINE_SLICE;
		}

		/** Размеры границ 9-slice текстуры по четырём сторонам. */
		@Environment(EnvType.CLIENT)
		public record Border(int left, int top, int right, int bottom) {

			private static final Codec<Border> UNIFORM_SIDE_SIZES_CODEC = Codecs.POSITIVE_INT
				.flatComapMap(
					size -> new Border(size, size, size, size),
					border -> {
						OptionalInt uniform = border.getUniformSideSize();
						return uniform.isPresent()
							? DataResult.success(uniform.getAsInt())
							: DataResult.error(() -> "Border has different side sizes");
					}
				);

			private static final Codec<Border> DIFFERENT_SIDE_SIZES_CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
					Codecs.NON_NEGATIVE_INT.fieldOf("left").forGetter(Border::left),
					Codecs.NON_NEGATIVE_INT.fieldOf("top").forGetter(Border::top),
					Codecs.NON_NEGATIVE_INT.fieldOf("right").forGetter(Border::right),
					Codecs.NON_NEGATIVE_INT.fieldOf("bottom").forGetter(Border::bottom)
				)
				.apply(instance, Border::new)
			);

			static final Codec<Border> CODEC = Codec.either(UNIFORM_SIDE_SIZES_CODEC, DIFFERENT_SIDE_SIZES_CODEC)
				.xmap(
					Either::unwrap,
					border -> border.getUniformSideSize().isPresent()
						? Either.left(border)
						: Either.right(border)
				);

			private OptionalInt getUniformSideSize() {
				return left() == top() && top() == right() && right() == bottom()
					? OptionalInt.of(left())
					: OptionalInt.empty();
			}
		}
	}

	/** Растягивает текстуру на весь доступный размер элемента. */
	@Environment(EnvType.CLIENT)
	record Stretch() implements Scaling {

		public static final MapCodec<Stretch> CODEC = MapCodec.unit(Stretch::new);

		@Override
		public Type getType() {
			return Type.STRETCH;
		}
	}

	/** Повторяет текстуру плиткой заданного размера {@code width × height}. */
	@Environment(EnvType.CLIENT)
	record Tile(int width, int height) implements Scaling {

		public static final MapCodec<Tile> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				Codecs.POSITIVE_INT.fieldOf("width").forGetter(Tile::width),
				Codecs.POSITIVE_INT.fieldOf("height").forGetter(Tile::height)
			)
			.apply(instance, Tile::new)
		);

		@Override
		public Type getType() {
			return Type.TILE;
		}
	}

	/** Перечисление доступных стратегий масштабирования, используется для диспетчеризации кодека. */
	@Environment(EnvType.CLIENT)
	enum Type implements StringIdentifiable {
		STRETCH("stretch", Stretch.CODEC),
		TILE("tile", Tile.CODEC),
		NINE_SLICE("nine_slice", NineSlice.CODEC);

		public static final Codec<Type> CODEC = StringIdentifiable.createCodec(Type::values);

		private final String name;
		private final MapCodec<? extends Scaling> codec;

		Type(final String name, final MapCodec<? extends Scaling> codec) {
			this.name = name;
			this.codec = codec;
		}

		@Override
		public String asString() {
			return name;
		}

		public MapCodec<? extends Scaling> getCodec() {
			return codec;
		}
	}
}

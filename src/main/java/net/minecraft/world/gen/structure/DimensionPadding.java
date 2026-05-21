package net.minecraft.world.gen.structure;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;

import java.util.function.Function;

/**
 * {@code DimensionPadding}.
 */
public record DimensionPadding(int bottom, int top) {

	private static final Codec<DimensionPadding> OBJECT_CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    Codecs.NON_NEGATIVE_INT.lenientOptionalFieldOf("bottom", 0).forGetter(padding -> padding.bottom),
					                    Codecs.NON_NEGATIVE_INT.lenientOptionalFieldOf("top", 0).forGetter(padding -> padding.top)
			                    )
			                    .apply(instance, DimensionPadding::new)
	);
	public static final Codec<DimensionPadding> CODEC = Codec.either(Codecs.NON_NEGATIVE_INT, OBJECT_CODEC)
	                                                         .xmap(
			                                                         either -> (DimensionPadding) either.map(
					                                                         DimensionPadding::new,
					                                                         Function.identity()
			                                                         ),
			                                                         padding -> padding.paddedBySameDistance()
			                                                                    ? Either.left(padding.bottom)
			                                                                    : Either.right(padding)
	                                                         );
	public static final DimensionPadding NONE = new DimensionPadding(0);

	public DimensionPadding(int value) {
		this(value, value);
	}

	public boolean paddedBySameDistance() {
		return this.top == this.bottom;
	}
}

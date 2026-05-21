package net.minecraft.world.gen.feature.size;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.Registries;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * {@code FeatureSize}.
 */
public abstract class FeatureSize {

	public static final Codec<FeatureSize>
			TYPE_CODEC =
			Registries.FEATURE_SIZE_TYPE.getCodec().dispatch(FeatureSize::getType, FeatureSizeType::getCodec);
	protected static final int MAX_HEIGHT = 16;
	protected final OptionalInt minClippedHeight;

	/**
	 * Создаёт codec.
	 *
	 * @return RecordCodecBuilder — результат операции
	 */
	protected static <S extends FeatureSize> RecordCodecBuilder<S, OptionalInt> createCodec() {
		return Codec.intRange(0, 80)
		            .optionalFieldOf("min_clipped_height")
		            .xmap(
				            minClippedHeight -> minClippedHeight.map(OptionalInt::of).orElse(OptionalInt.empty()),
				            minClippedHeight -> minClippedHeight.isPresent() ? Optional.of(minClippedHeight.getAsInt())
				                                                             : Optional.empty()
		            )
		            .forGetter(featureSize -> featureSize.minClippedHeight);
	}

	public FeatureSize(OptionalInt minClippedHeight) {
		this.minClippedHeight = minClippedHeight;
	}

	protected abstract FeatureSizeType<?> getType();

	public abstract int getRadius(int height, int y);

	public OptionalInt getMinClippedHeight() {
		return this.minClippedHeight;
	}
}

package net.minecraft.world.attribute;

import com.mojang.serialization.DataResult;
import net.minecraft.util.math.MathHelper;

/**
 * {@code AttributeValidator}.
 */
public interface AttributeValidator<Value> {

	AttributeValidator<Float> PROBABILITY = ranged(0.0F, 1.0F);

	AttributeValidator<Float> NON_NEGATIVE_FLOAT = ranged(0.0F, Float.POSITIVE_INFINITY);

	static <Value> AttributeValidator<Value> all() {
		return new AttributeValidator<Value>() {
			@Override
			public DataResult<Value> validate(Value value) {
				return DataResult.success(value);
			}

			@Override
			public Value clamp(Value value) {
				return value;
			}
		};
	}

	static AttributeValidator<Float> ranged(float min, float max) {
		return new AttributeValidator<Float>() {
			public DataResult<Float> validate(Float float_) {
				return float_ >= min && float_ <= max ? DataResult.success(float_)
				                                      : DataResult.error(() -> float_ + " is not in range [" + min
				                                                               + "; " + max + "]");
			}

			public Float clamp(Float float_) {
				return float_ >= min && float_ <= max ? float_ : MathHelper.clamp(float_, min, max);
			}
		};
	}

	DataResult<Value> validate(Value value);

	Value clamp(Value value);
}

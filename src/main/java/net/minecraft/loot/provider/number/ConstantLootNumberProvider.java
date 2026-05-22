package net.minecraft.loot.provider.number;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.context.LootContext;

/** Провайдер числа, всегда возвращающий фиксированное значение. */
public record ConstantLootNumberProvider(float value) implements LootNumberProvider {

	public static final MapCodec<ConstantLootNumberProvider> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(Codec.FLOAT.fieldOf("value").forGetter(ConstantLootNumberProvider::value))
			.apply(instance, ConstantLootNumberProvider::new)
	);

	public static final Codec<ConstantLootNumberProvider> INLINE_CODEC =
		Codec.FLOAT.xmap(ConstantLootNumberProvider::new, ConstantLootNumberProvider::value);

	@Override
	public LootNumberProviderType getType() {
		return LootNumberProviderTypes.CONSTANT;
	}

	@Override
	public float nextFloat(LootContext context) {
		return value;
	}

	public static ConstantLootNumberProvider create(float value) {
		return new ConstantLootNumberProvider(value);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		return other != null && getClass() == other.getClass()
			? Float.compare(((ConstantLootNumberProvider) other).value, value) == 0
			: false;
	}

	@Override
	public int hashCode() {
		return value != 0.0F ? Float.floatToIntBits(value) : 0;
	}
}

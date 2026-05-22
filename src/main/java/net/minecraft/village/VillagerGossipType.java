package net.minecraft.village;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

/**
 * Тип слуха, которым жители деревни обмениваются между собой.
 * <p>
 * Каждый тип определяет знак и силу влияния на репутацию игрока ({@code multiplier}),
 * максимально накапливаемое значение ({@code maxValue}), скорость затухания ({@code decay})
 * и величину потери при передаче другому жителю ({@code shareDecrement}).
 */
public enum VillagerGossipType implements StringIdentifiable {

	MAJOR_NEGATIVE("major_negative", -5, 100, 10, 10),
	MINOR_NEGATIVE("minor_negative", -1, 200, 20, 20),
	MINOR_POSITIVE("minor_positive", 1, 25, 1, 5),
	MAJOR_POSITIVE("major_positive", 5, 20, 0, 20),
	TRADING("trading", 1, 25, 2, 20);

	public static final int MAX_TRADING_REPUTATION = 25;
	public static final int MIN_GOSSIP_VALUE = 20;
	public static final int TRADING_GOSSIP_DECAY = 2;

	public static final Codec<VillagerGossipType> CODEC = StringIdentifiable.createCodec(VillagerGossipType::values);

	public final String id;
	public final int multiplier;
	public final int maxValue;
	public final int decay;
	public final int shareDecrement;

	VillagerGossipType(
			final String id,
			final int multiplier,
			final int maxReputation,
			final int decay,
			final int shareDecrement
	) {
		this.id = id;
		this.multiplier = multiplier;
		this.maxValue = maxReputation;
		this.decay = decay;
		this.shareDecrement = shareDecrement;
	}

	@Override
	public String asString() {
		return id;
	}
}

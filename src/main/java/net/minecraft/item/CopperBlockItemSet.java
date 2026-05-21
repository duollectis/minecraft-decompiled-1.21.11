package net.minecraft.item;

import com.google.common.collect.ImmutableBiMap;
import net.minecraft.block.Block;
import net.minecraft.block.CopperBlockSet;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@code CopperBlockItemSet}.
 */
public record CopperBlockItemSet(
		Item unaffected,
		Item exposed,
		Item weathered,
		Item oxidized,
		Item waxed,
		Item waxedExposed,
		Item waxedWeathered,
		Item waxedOxidized
) {

	public static CopperBlockItemSet create(CopperBlockSet blockSet, Function<Block, Item> registerFunction) {
		return new CopperBlockItemSet(
				registerFunction.apply(blockSet.unaffected()),
				registerFunction.apply(blockSet.exposed()),
				registerFunction.apply(blockSet.weathered()),
				registerFunction.apply(blockSet.oxidized()),
				registerFunction.apply(blockSet.waxed()),
				registerFunction.apply(blockSet.waxedExposed()),
				registerFunction.apply(blockSet.waxedWeathered()),
				registerFunction.apply(blockSet.waxedOxidized())
		);
	}

	public ImmutableBiMap<Item, Item> getWaxingMap() {
		return ImmutableBiMap.of(
				this.unaffected,
				this.waxed,
				this.exposed,
				this.waxedExposed,
				this.weathered,
				this.waxedWeathered,
				this.oxidized,
				this.waxedOxidized
		);
	}

	public void forEach(Consumer<Item> consumer) {
		consumer.accept(this.unaffected);
		consumer.accept(this.exposed);
		consumer.accept(this.weathered);
		consumer.accept(this.oxidized);
		consumer.accept(this.waxed);
		consumer.accept(this.waxedExposed);
		consumer.accept(this.waxedWeathered);
		consumer.accept(this.waxedOxidized);
	}
}

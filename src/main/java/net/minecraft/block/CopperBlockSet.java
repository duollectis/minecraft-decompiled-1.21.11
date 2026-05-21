package net.minecraft.block;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.function.TriFunction;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@code CopperBlockSet}.
 */
public record CopperBlockSet(
		Block unaffected,
		Block exposed,
		Block weathered,
		Block oxidized,
		Block waxed,
		Block waxedExposed,
		Block waxedWeathered,
		Block waxedOxidized
) {

	public static <WaxedBlock extends Block, WeatheringBlock extends Block & Oxidizable> CopperBlockSet create(
			String baseId,
			TriFunction<String, Function<AbstractBlock.Settings, Block>, AbstractBlock.Settings, Block> registerFunction,
			Function<AbstractBlock.Settings, WaxedBlock> waxedBlockFactory,
			BiFunction<Oxidizable.OxidationLevel, AbstractBlock.Settings, WeatheringBlock> unwaxedBlockFactory,
			Function<Oxidizable.OxidationLevel, AbstractBlock.Settings> settingsFromOxidationLevel
	) {
		return new CopperBlockSet(
				(Block) registerFunction.apply(
						baseId,
						(Function<AbstractBlock.Settings, Block>) settings -> unwaxedBlockFactory.apply(
								Oxidizable.OxidationLevel.UNAFFECTED,
								settings
						),
						settingsFromOxidationLevel.apply(Oxidizable.OxidationLevel.UNAFFECTED)
				),
				(Block) registerFunction.apply(
						"exposed_" + baseId,
						(Function<AbstractBlock.Settings, Block>) settings -> unwaxedBlockFactory.apply(
								Oxidizable.OxidationLevel.EXPOSED,
								settings
						),
						settingsFromOxidationLevel.apply(Oxidizable.OxidationLevel.EXPOSED)
				),
				(Block) registerFunction.apply(
						"weathered_" + baseId,
						(Function<AbstractBlock.Settings, Block>) settings -> unwaxedBlockFactory.apply(
								Oxidizable.OxidationLevel.WEATHERED,
								settings
						),
						settingsFromOxidationLevel.apply(Oxidizable.OxidationLevel.WEATHERED)
				),
				(Block) registerFunction.apply(
						"oxidized_" + baseId,
						(Function<AbstractBlock.Settings, Block>) settings -> unwaxedBlockFactory.apply(
								Oxidizable.OxidationLevel.OXIDIZED,
								settings
						),
						settingsFromOxidationLevel.apply(Oxidizable.OxidationLevel.OXIDIZED)
				),
				(Block) registerFunction.apply(
						"waxed_" + baseId,
						waxedBlockFactory::apply,
						settingsFromOxidationLevel.apply(Oxidizable.OxidationLevel.UNAFFECTED)
				),
				(Block) registerFunction.apply(
						"waxed_exposed_" + baseId,
						waxedBlockFactory::apply,
						settingsFromOxidationLevel.apply(Oxidizable.OxidationLevel.EXPOSED)
				),
				(Block) registerFunction.apply(
						"waxed_weathered_" + baseId,
						waxedBlockFactory::apply,
						settingsFromOxidationLevel.apply(Oxidizable.OxidationLevel.WEATHERED)
				),
				(Block) registerFunction.apply(
						"waxed_oxidized_" + baseId,
						waxedBlockFactory::apply,
						settingsFromOxidationLevel.apply(Oxidizable.OxidationLevel.OXIDIZED)
				)
		);
	}

	public ImmutableBiMap<Block, Block> getOxidizingMap() {
		return ImmutableBiMap.of(
				this.unaffected,
				this.exposed,
				this.exposed,
				this.weathered,
				this.weathered,
				this.oxidized
		);
	}

	public ImmutableBiMap<Block, Block> getWaxingMap() {
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

	public ImmutableList<Block> getAll() {
		return ImmutableList.of(
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

	public void forEach(Consumer<Block> consumer) {
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

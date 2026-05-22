package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;
import net.minecraft.world.gen.treedecorator.TreeDecorator;

import java.util.ArrayList;
import java.util.List;

/**
 * Конфигурация фичи упавшего дерева. Задаёт провайдер блоков ствола,
 * длину бревна и декораторы для пня и бревна. Строится через {@link Builder}.
 */
public class FallenTreeFeatureConfig implements FeatureConfig {

	public static final Codec<FallenTreeFeatureConfig> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    BlockStateProvider.TYPE_CODEC
							                    .fieldOf("trunk_provider")
							                    .forGetter(featureConfig -> featureConfig.trunkProvider),
					                    IntProvider
							                    .createValidatingCodec(0, 16)
							                    .fieldOf("log_length")
							                    .forGetter(featureConfig -> featureConfig.logLength),
					                    TreeDecorator.TYPE_CODEC
							                    .listOf()
							                    .fieldOf("stump_decorators")
							                    .forGetter(featureConfig -> featureConfig.stumpDecorators),
					                    TreeDecorator.TYPE_CODEC
							                    .listOf()
							                    .fieldOf("log_decorators")
							                    .forGetter(featureConfig -> featureConfig.logDecorators)
			                    )
			                    .apply(instance, FallenTreeFeatureConfig::new)
	);
	public final BlockStateProvider trunkProvider;
	public final IntProvider logLength;
	public final List<TreeDecorator> stumpDecorators;
	public final List<TreeDecorator> logDecorators;

	protected FallenTreeFeatureConfig(
			BlockStateProvider trunkProvider,
			IntProvider logLength,
			List<TreeDecorator> stumpDecorators,
			List<TreeDecorator> logDecorators
	) {
		this.trunkProvider = trunkProvider;
		this.logLength = logLength;
		this.stumpDecorators = stumpDecorators;
		this.logDecorators = logDecorators;
	}

	/** Строитель конфигурации упавшего дерева с опциональными декораторами пня и бревна. */
	public static class Builder {

		private final BlockStateProvider trunkProvider;
		private final IntProvider logLength;
		private List<TreeDecorator> stumpDecorators = new ArrayList<>();
		private List<TreeDecorator> logDecorators = new ArrayList<>();

		public Builder(BlockStateProvider trunkProvider, IntProvider logLength) {
			this.trunkProvider = trunkProvider;
			this.logLength = logLength;
		}

		public FallenTreeFeatureConfig.Builder stumpDecorators(List<TreeDecorator> stumpDecorators) {
			this.stumpDecorators = stumpDecorators;
			return this;
		}

		public FallenTreeFeatureConfig.Builder logDecorators(List<TreeDecorator> logDecorators) {
			this.logDecorators = logDecorators;
			return this;
		}

		public FallenTreeFeatureConfig build() {
			return new FallenTreeFeatureConfig(
					this.trunkProvider,
					this.logLength,
					this.stumpDecorators,
					this.logDecorators
			);
		}
	}
}

package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;

/**
 * {@code SingleStateFeatureConfig}.
 */
public class SingleStateFeatureConfig implements FeatureConfig {

	public static final Codec<SingleStateFeatureConfig> CODEC = BlockState.CODEC
			.fieldOf("state")
			.xmap(SingleStateFeatureConfig::new, config -> config.state)
			.codec();
	public final BlockState state;

	public SingleStateFeatureConfig(BlockState state) {
		this.state = state;
	}
}

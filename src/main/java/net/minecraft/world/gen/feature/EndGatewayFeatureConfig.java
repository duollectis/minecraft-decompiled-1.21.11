package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * {@code EndGatewayFeatureConfig}.
 */
public class EndGatewayFeatureConfig implements FeatureConfig {

	public static final Codec<EndGatewayFeatureConfig> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    BlockPos.CODEC.optionalFieldOf("exit").forGetter(config -> config.exitPos),
					                    Codec.BOOL.fieldOf("exact").forGetter(config -> config.exact)
			                    )
			                    .apply(instance, EndGatewayFeatureConfig::new)
	);
	private final Optional<BlockPos> exitPos;
	private final boolean exact;

	private EndGatewayFeatureConfig(Optional<BlockPos> exitPos, boolean exact) {
		this.exitPos = exitPos;
		this.exact = exact;
	}

	/**
	 * Создаёт config.
	 *
	 * @param exitPortalPosition exit portal position
	 * @param exitsAtSpawn exits at spawn
	 *
	 * @return EndGatewayFeatureConfig — результат операции
	 */
	public static EndGatewayFeatureConfig createConfig(BlockPos exitPortalPosition, boolean exitsAtSpawn) {
		return new EndGatewayFeatureConfig(Optional.of(exitPortalPosition), exitsAtSpawn);
	}

	/**
	 * Создаёт config.
	 *
	 * @return EndGatewayFeatureConfig — результат операции
	 */
	public static EndGatewayFeatureConfig createConfig() {
		return new EndGatewayFeatureConfig(Optional.empty(), false);
	}

	public Optional<BlockPos> getExitPos() {
		return this.exitPos;
	}

	public boolean isExact() {
		return this.exact;
	}
}

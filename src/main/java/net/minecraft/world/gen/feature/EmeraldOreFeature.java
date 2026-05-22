package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Генерирует одиночный блок изумрудной руды, заменяя первый подходящий целевой блок в точке происхождения. */
public class EmeraldOreFeature extends Feature<EmeraldOreFeatureConfig> {

	public EmeraldOreFeature(Codec<EmeraldOreFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<EmeraldOreFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		EmeraldOreFeatureConfig config = context.getConfig();

		for (OreFeatureConfig.Target target : config.targets) {
			if (target.target.test(world.getBlockState(origin), context.getRandom())) {
				world.setBlockState(origin, target.state, 2);
				break;
			}
		}

		return true;
	}
}

package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.EndGatewayBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Генерирует портал Края: блок ворот в центре, обёрнутый в бедрок с воздушными боковыми гранями. */
public class EndGatewayFeature extends Feature<EndGatewayFeatureConfig> {

	public EndGatewayFeature(Codec<EndGatewayFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<EndGatewayFeatureConfig> context) {
		BlockPos origin = context.getOrigin();
		StructureWorldAccess world = context.getWorld();
		EndGatewayFeatureConfig config = context.getConfig();

		for (BlockPos pos : BlockPos.iterate(origin.add(-1, -2, -1), origin.add(1, 2, 1))) {
			boolean onCenterX = pos.getX() == origin.getX();
			boolean onCenterY = pos.getY() == origin.getY();
			boolean onCenterZ = pos.getZ() == origin.getZ();
			boolean onTopOrBottom = Math.abs(pos.getY() - origin.getY()) == 2;

			if (onCenterX && onCenterY && onCenterZ) {
				BlockPos gatewayPos = pos.toImmutable();
				setBlockState(world, gatewayPos, Blocks.END_GATEWAY.getDefaultState());
				config.getExitPos().ifPresent(exitPos -> {
					if (world.getBlockEntity(gatewayPos) instanceof EndGatewayBlockEntity gateway) {
						gateway.setExitPortalPos(exitPos, config.isExact());
					}
				});
			} else if (onCenterY) {
				setBlockState(world, pos, Blocks.AIR.getDefaultState());
			} else if (onTopOrBottom && onCenterX && onCenterZ) {
				setBlockState(world, pos, Blocks.BEDROCK.getDefaultState());
			} else if ((onCenterX || onCenterZ) && !onTopOrBottom) {
				setBlockState(world, pos, Blocks.BEDROCK.getDefaultState());
			} else {
				setBlockState(world, pos, Blocks.AIR.getDefaultState());
			}
		}

		return true;
	}
}

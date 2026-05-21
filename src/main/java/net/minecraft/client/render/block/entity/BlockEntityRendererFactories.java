package net.minecraft.client.render.block.entity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.registry.Registries;

import java.util.Map;

@Environment(EnvType.CLIENT)
/**
 * {@code BlockEntityRendererFactories}.
 */
public class BlockEntityRendererFactories {

	private static final Map<BlockEntityType<?>, BlockEntityRendererFactory<?, ?>> FACTORIES = Maps.newHashMap();

	public static <T extends BlockEntity, S extends BlockEntityRenderState> void register(
			BlockEntityType<? extends T> type, BlockEntityRendererFactory<T, S> factory
	) {
		FACTORIES.put(type, factory);
	}

	public static Map<BlockEntityType<?>, BlockEntityRenderer<?, ?>> reload(BlockEntityRendererFactory.Context args) {
		Builder<BlockEntityType<?>, BlockEntityRenderer<?, ?>> builder = ImmutableMap.builder();
		FACTORIES.forEach((type, factory) -> {
			try {
				builder.put(type, factory.create(args));
			}
			catch (Exception var5) {
				throw new IllegalStateException(
						"Failed to create model for " + Registries.BLOCK_ENTITY_TYPE.getId((BlockEntityType<?>) type),
						var5
				);
			}
		});
		return builder.build();
	}

	static {
		register(BlockEntityType.SIGN, SignBlockEntityRenderer::new);
		register(BlockEntityType.HANGING_SIGN, HangingSignBlockEntityRenderer::new);
		register(BlockEntityType.MOB_SPAWNER, MobSpawnerBlockEntityRenderer::new);
		register(BlockEntityType.PISTON, context -> new PistonBlockEntityRenderer());
		register(BlockEntityType.CHEST, ChestBlockEntityRenderer::new);
		register(BlockEntityType.ENDER_CHEST, ChestBlockEntityRenderer::new);
		register(BlockEntityType.TRAPPED_CHEST, ChestBlockEntityRenderer::new);
		register(BlockEntityType.ENCHANTING_TABLE, EnchantingTableBlockEntityRenderer::new);
		register(BlockEntityType.LECTERN, LecternBlockEntityRenderer::new);
		register(BlockEntityType.END_PORTAL, context -> new EndPortalBlockEntityRenderer());
		register(BlockEntityType.END_GATEWAY, context -> new EndGatewayBlockEntityRenderer());
		register(BlockEntityType.BEACON, context -> new BeaconBlockEntityRenderer());
		register(BlockEntityType.SKULL, SkullBlockEntityRenderer::new);
		register(BlockEntityType.BANNER, BannerBlockEntityRenderer::new);
		register(BlockEntityType.STRUCTURE_BLOCK, context -> new StructureBlockBlockEntityRenderer());
		register(BlockEntityType.TEST_INSTANCE_BLOCK, context -> new TestInstanceBlockEntityRenderer());
		register(BlockEntityType.SHULKER_BOX, ShulkerBoxBlockEntityRenderer::new);
		register(BlockEntityType.BED, BedBlockEntityRenderer::new);
		register(BlockEntityType.CONDUIT, ConduitBlockEntityRenderer::new);
		register(BlockEntityType.BELL, BellBlockEntityRenderer::new);
		register(BlockEntityType.CAMPFIRE, CampfireBlockEntityRenderer::new);
		register(BlockEntityType.BRUSHABLE_BLOCK, BrushableBlockEntityRenderer::new);
		register(BlockEntityType.DECORATED_POT, DecoratedPotBlockEntityRenderer::new);
		register(BlockEntityType.TRIAL_SPAWNER, TrialSpawnerBlockEntityRenderer::new);
		register(BlockEntityType.VAULT, VaultBlockEntityRenderer::new);
		register(BlockEntityType.COPPER_GOLEM_STATUE, CopperGolemStatueBlockEntityRenderer::new);
		register(BlockEntityType.SHELF, ShelfBlockEntityRenderer::new);
	}
}

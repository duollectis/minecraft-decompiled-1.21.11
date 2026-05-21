package net.minecraft.client.render;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.chunk.BlockBufferAllocatorStorage;
import net.minecraft.client.render.chunk.BlockBufferBuilderPool;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Util;

import java.util.SequencedMap;

@Environment(EnvType.CLIENT)
/**
 * {@code BufferBuilderStorage}.
 */
public class BufferBuilderStorage {

	private final BlockBufferAllocatorStorage blockBufferBuilders = new BlockBufferAllocatorStorage();
	private final BlockBufferBuilderPool blockBufferBuildersPool;
	private final VertexConsumerProvider.Immediate entityVertexConsumers;
	private final VertexConsumerProvider.Immediate effectVertexConsumers;
	private final OutlineVertexConsumerProvider outlineVertexConsumers;

	public BufferBuilderStorage(int maxBlockBuildersPoolSize) {
		this.blockBufferBuildersPool = BlockBufferBuilderPool.allocate(maxBlockBuildersPoolSize);
		SequencedMap<RenderLayer, BufferAllocator> sequencedMap = Util.make(
				new Object2ObjectLinkedOpenHashMap(), map -> {
					map.put(
							TexturedRenderLayers.getEntitySolid(),
							this.blockBufferBuilders.get(BlockRenderLayer.SOLID)
					);
					map.put(
							TexturedRenderLayers.getEntityCutout(),
							this.blockBufferBuilders.get(BlockRenderLayer.CUTOUT)
					);
					map.put(
							TexturedRenderLayers.getItemTranslucentCull(),
							this.blockBufferBuilders.get(BlockRenderLayer.TRANSLUCENT)
					);
					assignBufferBuilder(map, TexturedRenderLayers.getBlockTranslucentCull());
					assignBufferBuilder(map, TexturedRenderLayers.getShieldPatterns());
					assignBufferBuilder(map, TexturedRenderLayers.getBeds());
					assignBufferBuilder(map, TexturedRenderLayers.getShulkerBoxes());
					assignBufferBuilder(map, TexturedRenderLayers.getSign());
					assignBufferBuilder(map, TexturedRenderLayers.getHangingSign());
					map.put(TexturedRenderLayers.getChest(), new BufferAllocator(786432));
					assignBufferBuilder(map, RenderLayers.armorEntityGlint());
					assignBufferBuilder(map, RenderLayers.glint());
					assignBufferBuilder(map, RenderLayers.glintTranslucent());
					assignBufferBuilder(map, RenderLayers.entityGlint());
					assignBufferBuilder(map, RenderLayers.waterMask());
				}
		);
		this.entityVertexConsumers = VertexConsumerProvider.immediate(sequencedMap, new BufferAllocator(786432));
		this.outlineVertexConsumers = new OutlineVertexConsumerProvider();
		SequencedMap<RenderLayer, BufferAllocator> sequencedMap2 = Util.make(
				new Object2ObjectLinkedOpenHashMap(),
				objectMap -> ModelBaker.BLOCK_DESTRUCTION_RENDER_LAYERS.forEach(renderLayer -> assignBufferBuilder(
						objectMap,
						renderLayer
				))
		);
		this.effectVertexConsumers = VertexConsumerProvider.immediate(sequencedMap2, new BufferAllocator(0));
	}

	private static void assignBufferBuilder(
			Object2ObjectLinkedOpenHashMap<RenderLayer, BufferAllocator> builderStorage,
			RenderLayer layer
	) {
		builderStorage.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
	}

	public BlockBufferAllocatorStorage getBlockBufferBuilders() {
		return this.blockBufferBuilders;
	}

	public BlockBufferBuilderPool getBlockBufferBuildersPool() {
		return this.blockBufferBuildersPool;
	}

	public VertexConsumerProvider.Immediate getEntityVertexConsumers() {
		return this.entityVertexConsumers;
	}

	public VertexConsumerProvider.Immediate getEffectVertexConsumers() {
		return this.effectVertexConsumers;
	}

	public OutlineVertexConsumerProvider getOutlineVertexConsumers() {
		return this.outlineVertexConsumers;
	}
}

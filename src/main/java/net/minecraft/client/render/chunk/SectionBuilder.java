package net.minecraft.client.render.chunk;

import com.mojang.blaze3d.systems.VertexSorter;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Компилирует одну секцию чанка (16×16×16 блоков) в набор {@link BuiltBuffer} по слоям рендеринга.
 * Итерирует все блоки секции, собирает геометрию блоков и жидкостей, строит данные окклюзии
 * и сортирует прозрачные вершины. Результат — {@link RenderData}, готовый к загрузке на GPU.
 */
@Environment(EnvType.CLIENT)
public class SectionBuilder {

	private final BlockRenderManager blockRenderManager;
	private final BlockEntityRenderManager blockEntityRenderDispatcher;

	public SectionBuilder(BlockRenderManager blockRenderManager, BlockEntityRenderManager blockEntityRenderDispatcher) {
		this.blockRenderManager = blockRenderManager;
		this.blockEntityRenderDispatcher = blockEntityRenderDispatcher;
	}

	/**
	 * Компилирует геометрию секции чанка по всем слоям рендеринга.
	 * Итерирует каждый блок в диапазоне секции, собирает модели блоков, жидкостей и блок-энтити,
	 * строит данные окклюзии и выполняет сортировку прозрачных вершин.
	 *
	 * @param sectionPos позиция секции чанка в мировых координатах
	 * @param renderRegion регион блоков, доступный для чтения во время компиляции
	 * @param vertexSorter сортировщик вершин для прозрачного слоя
	 * @param allocatorStorage хранилище аллокаторов буферов по слоям
	 * @return скомпилированные данные секции, готовые к загрузке на GPU
	 */
	public SectionBuilder.RenderData build(
			ChunkSectionPos sectionPos,
			ChunkRendererRegion renderRegion,
			VertexSorter vertexSorter,
			BlockBufferAllocatorStorage allocatorStorage
	) {
		SectionBuilder.RenderData renderData = new SectionBuilder.RenderData();
		BlockPos minPos = sectionPos.getMinPos();
		// 15 = CHUNK_SIZE - 1, максимальный локальный координат внутри секции
		BlockPos maxPos = minPos.add(15, 15, 15);
		ChunkOcclusionDataBuilder occlusionBuilder = new ChunkOcclusionDataBuilder();
		MatrixStack matrixStack = new MatrixStack();
		BlockModelRenderer.enableBrightnessCache();
		Map<BlockRenderLayer, BufferBuilder> buildersByLayer = new EnumMap<>(BlockRenderLayer.class);
		Random random = Random.create();
		List<BlockModelPart> modelParts = new ObjectArrayList();

		for (BlockPos pos : BlockPos.iterate(minPos, maxPos)) {
			BlockState blockState = renderRegion.getBlockState(pos);

			if (blockState.isOpaqueFullCube()) {
				occlusionBuilder.markClosed(pos);
			}

			if (blockState.hasBlockEntity()) {
				BlockEntity blockEntity = renderRegion.getBlockEntity(pos);
				if (blockEntity != null) {
					addBlockEntity(renderData, blockEntity);
				}
			}

			FluidState fluidState = blockState.getFluidState();
			if (!fluidState.isEmpty()) {
				BlockRenderLayer fluidLayer = BlockRenderLayers.getFluidLayer(fluidState);
				BufferBuilder fluidBuffer = beginBufferBuilding(buildersByLayer, allocatorStorage, fluidLayer);
				blockRenderManager.renderFluid(pos, renderRegion, fluidBuffer, blockState, fluidState);
			}

			if (blockState.getRenderType() == BlockRenderType.MODEL) {
				BlockRenderLayer blockLayer = BlockRenderLayers.getBlockLayer(blockState);
				BufferBuilder blockBuffer = beginBufferBuilding(buildersByLayer, allocatorStorage, blockLayer);
				random.setSeed(blockState.getRenderingSeed(pos));
				blockRenderManager.getModel(blockState).addParts(random, modelParts);
				matrixStack.push();
				matrixStack.translate(
						(float) ChunkSectionPos.getLocalCoord(pos.getX()),
						(float) ChunkSectionPos.getLocalCoord(pos.getY()),
						(float) ChunkSectionPos.getLocalCoord(pos.getZ())
				);
				blockRenderManager.renderBlock(blockState, pos, renderRegion, matrixStack, blockBuffer, true, modelParts);
				matrixStack.pop();
				modelParts.clear();
			}
		}

		for (Entry<BlockRenderLayer, BufferBuilder> entry : buildersByLayer.entrySet()) {
			BlockRenderLayer layer = entry.getKey();
			BuiltBuffer builtBuffer = entry.getValue().endNullable();

			if (builtBuffer == null) {
				continue;
			}

			if (layer == BlockRenderLayer.TRANSLUCENT) {
				renderData.translucencySortingData = builtBuffer.sortQuads(allocatorStorage.get(layer), vertexSorter);
			}

			renderData.buffers.put(layer, builtBuffer);
		}

		BlockModelRenderer.disableBrightnessCache();
		renderData.chunkOcclusionData = occlusionBuilder.build();
		return renderData;
	}

	private BufferBuilder beginBufferBuilding(
			Map<BlockRenderLayer, BufferBuilder> builders,
			BlockBufferAllocatorStorage allocatorStorage,
			BlockRenderLayer layer
	) {
		BufferBuilder bufferBuilder = builders.get(layer);
		if (bufferBuilder == null) {
			BufferAllocator bufferAllocator = allocatorStorage.get(layer);
			bufferBuilder =
					new BufferBuilder(
							bufferAllocator,
							VertexFormat.DrawMode.QUADS,
							VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL
					);
			builders.put(layer, bufferBuilder);
		}

		return bufferBuilder;
	}

	private <E extends BlockEntity> void addBlockEntity(SectionBuilder.RenderData data, E blockEntity) {
		BlockEntityRenderer<E, ?> blockEntityRenderer = this.blockEntityRenderDispatcher.get(blockEntity);
		if (blockEntityRenderer != null && !blockEntityRenderer.rendersOutsideBoundingBox()) {
			data.blockEntities.add(blockEntity);
		}
	}

	/**
	 * Результат компиляции секции чанка: набор скомпилированных буферов по слоям,
	 * данные окклюзии граней и состояние сортировки прозрачных вершин.
	 * Должен быть закрыт после загрузки данных на GPU.
	 */
	@Environment(EnvType.CLIENT)
	public static final class RenderData {

		public final List<BlockEntity> blockEntities = new ArrayList<>();
		public final Map<BlockRenderLayer, BuiltBuffer> buffers = new EnumMap<>(BlockRenderLayer.class);
		public ChunkOcclusionData chunkOcclusionData = new ChunkOcclusionData();
		public BuiltBuffer.@Nullable SortState translucencySortingData;

		public void close() {
			buffers.values().forEach(BuiltBuffer::close);
		}
	}
}

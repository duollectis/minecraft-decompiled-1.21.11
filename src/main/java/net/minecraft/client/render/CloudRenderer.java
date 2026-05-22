package net.minecraft.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Рендерер облаков, загружающий текстуру облаков как ресурс и строящий GPU-буфер
 * из ячеек облачного поля. Поддерживает два режима: быстрый (flat) и детальный (fancy),
 * а также три точки зрения: снизу, внутри и сверху облаков.
 * Реализует {@link SinglePreparationResourceReloader} для перезагрузки при смене ресурс-пака.
 */
@Environment(EnvType.CLIENT)
public class CloudRenderer extends SinglePreparationResourceReloader<Optional<CloudRenderer.CloudCells>> implements AutoCloseable {

	private static final int CLOUD_CELL_SIZE = 16;
	private static final int CLOUD_THICKNESS = 32;
	private static final float CLOUD_BLOCK_SIZE = 12.0F;
	/** Период прокрутки облаков в тиках (20 сек при 20 TPS). */
	private static final int CLOUD_SCROLL_PERIOD = 400;
	private static final float CLOUD_ALPHA_THRESHOLD = 0.6F;
	/** Горизонтальное смещение облаков относительно позиции игрока по оси Z. */
	private static final float CLOUD_Z_OFFSET = 3.96F;
	/** Скорость горизонтального движения облаков (блоков/тик). */
	private static final float CLOUD_SCROLL_SPEED = 0.030000001F;
	private static final int UBO_SIZE = new Std140SizeCalculator().putVec4().putVec3().putVec3().get();
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Identifier CLOUD_TEXTURE = Identifier.ofVanilla("textures/environment/clouds.png");

	private boolean rebuild = true;
	private int centerX = Integer.MIN_VALUE;
	private int centerZ = Integer.MIN_VALUE;
	private CloudRenderer.ViewMode viewMode = CloudRenderer.ViewMode.INSIDE_CLOUDS;
	private @Nullable CloudRenderMode renderMode;
	private CloudRenderer.@Nullable CloudCells cells;
	private int instanceCount = 0;
	private final MappableRingBuffer cloudInfoBuffer = new MappableRingBuffer(() -> "Cloud UBO", 130, UBO_SIZE);
	private @Nullable MappableRingBuffer cloudFacesBuffer;

	@Override
	protected Optional<CloudRenderer.CloudCells> prepare(ResourceManager resourceManager, Profiler profiler) {
		try {
			Optional<CloudRenderer.CloudCells> result;
			try (
					InputStream inputStream = resourceManager.open(CLOUD_TEXTURE);
					NativeImage nativeImage = NativeImage.read(inputStream)
			) {
				int width = nativeImage.getWidth();
				int height = nativeImage.getHeight();
				long[] packedCells = new long[width * height];

				for (int row = 0; row < height; row++) {
					for (int col = 0; col < width; col++) {
						int color = nativeImage.getColorArgb(col, row);
						if (isEmpty(color)) {
							packedCells[col + row * width] = 0L;
						}
						else {
							boolean borderNorth = isEmpty(nativeImage.getColorArgb(col, Math.floorMod(row - 1, height)));
							boolean borderEast = isEmpty(nativeImage.getColorArgb(Math.floorMod(col + 1, width), row));
							boolean borderSouth = isEmpty(nativeImage.getColorArgb(col, Math.floorMod(row + 1, height)));
							boolean borderWest = isEmpty(nativeImage.getColorArgb(Math.floorMod(col - 1, width), row));
							packedCells[col + row * width] = packCloudCell(color, borderNorth, borderEast, borderSouth, borderWest);
						}
					}
				}

				result = Optional.of(new CloudRenderer.CloudCells(packedCells, width, height));
			}

			return result;
		}
		catch (IOException exception) {
			LOGGER.error("Failed to load cloud texture", exception);
			return Optional.empty();
		}
	}

	/**
	 * Вычисляет размер GPU-буфера граней облаков для заданного радиуса отрисовки.
	 * Каждая грань кодируется 3 байтами; максимальное число граней — 6 на ячейку.
	 *
	 * @param cloudRange радиус отрисовки облаков в ячейках
	 * @return размер буфера в байтах
	 */
	private static int calcCloudBufferSize(int cloudRange) {
		int facesPerCell = 4;
		int maxCells = (cloudRange + 1) * 2 * (cloudRange + 1) * 2 / 2;
		int facesTotal = maxCells * facesPerCell + 54;
		return facesTotal * 3;
	}

	@Override
	protected void apply(
			Optional<CloudRenderer.CloudCells> optional,
			ResourceManager resourceManager,
			Profiler profiler
	) {
		cells = optional.orElse(null);
		rebuild = true;
	}

	private static boolean isEmpty(int color) {
		return ColorHelper.getAlpha(color) < 10;
	}

	private static long packCloudCell(
			int color,
			boolean borderNorth,
			boolean borderEast,
			boolean borderSouth,
			boolean borderWest
	) {
		return (long) color << 4
				| (borderNorth ? 1 : 0) << 3
				| (borderEast ? 1 : 0) << 2
				| (borderSouth ? 1 : 0) << 1
				| (borderWest ? 1 : 0);
	}

	private static boolean hasBorderNorth(long packed) {
		return (packed >> 3 & 1L) != 0L;
	}

	private static boolean hasBorderEast(long packed) {
		return (packed >> 2 & 1L) != 0L;
	}

	private static boolean hasBorderSouth(long packed) {
		return (packed >> 1 & 1L) != 0L;
	}

	private static boolean hasBorderWest(long packed) {
		return (packed & 1L) != 0L;
	}

	/**
	 * Выполняет отрисовку облаков в текущем кадре.
	 * При необходимости перестраивает GPU-буфер ячеек (при смене позиции, режима или ресурса).
	 * Загружает UBO с цветом, смещением и размером облачного блока, затем выполняет draw call.
	 *
	 * @param color       ARGB-цвет облаков (с учётом освещения неба)
	 * @param mode        режим отрисовки (FAST или FANCY)
	 * @param cloudHeight высота облаков в мировых координатах
	 * @param cameraPos   позиция камеры в мировом пространстве
	 * @param worldTick   текущий тик мира для анимации прокрутки
	 * @param tickDelta   интерполяционный прогресс между тиками [0..1]
	 */
	public void renderClouds(int color, CloudRenderMode mode, float cloudHeight, Vec3d cameraPos, long worldTick, float tickDelta) {
		if (cells == null) {
			return;
		}

		int renderDistanceCells = MinecraftClient.getInstance().options.getCloudRenderDistance().getValue() * CLOUD_CELL_SIZE;
		int cloudRange = MathHelper.ceil(renderDistanceCells / CLOUD_BLOCK_SIZE);
		int requiredBufferSize = calcCloudBufferSize(cloudRange);

		if (cloudFacesBuffer == null || cloudFacesBuffer.getBlocking().size() != requiredBufferSize) {
			if (cloudFacesBuffer != null) {
				cloudFacesBuffer.close();
			}

			cloudFacesBuffer = new MappableRingBuffer(() -> "Cloud UTB", 258, requiredBufferSize);
		}

		float relativeHeight = (float) (cloudHeight - cameraPos.y);
		float topRelativeHeight = relativeHeight + CLOUD_THICKNESS / 8.0F;
		CloudRenderer.ViewMode currentViewMode;

		if (topRelativeHeight < 0.0F) {
			currentViewMode = CloudRenderer.ViewMode.ABOVE_CLOUDS;
		}
		else if (relativeHeight > 0.0F) {
			currentViewMode = CloudRenderer.ViewMode.BELOW_CLOUDS;
		}
		else {
			currentViewMode = CloudRenderer.ViewMode.INSIDE_CLOUDS;
		}

		float scrollOffset = (float) (worldTick % (cells.width * (long) CLOUD_SCROLL_PERIOD)) + tickDelta;
		double scrolledX = cameraPos.x + scrollOffset * CLOUD_SCROLL_SPEED;
		double scrolledZ = cameraPos.z + CLOUD_Z_OFFSET;
		double gridWidth = cells.width * CLOUD_BLOCK_SIZE;
		double gridHeight = cells.height * CLOUD_BLOCK_SIZE;
		scrolledX -= MathHelper.floor(scrolledX / gridWidth) * gridWidth;
		scrolledZ -= MathHelper.floor(scrolledZ / gridHeight) * gridHeight;
		int gridX = MathHelper.floor(scrolledX / CLOUD_BLOCK_SIZE);
		int gridZ = MathHelper.floor(scrolledZ / CLOUD_BLOCK_SIZE);
		float subCellOffsetX = (float) (scrolledX - gridX * CLOUD_BLOCK_SIZE);
		float subCellOffsetZ = (float) (scrolledZ - gridZ * CLOUD_BLOCK_SIZE);
		boolean isFancy = mode == CloudRenderMode.FANCY;
		RenderPipeline renderPipeline = isFancy ? RenderPipelines.CLOUDS : RenderPipelines.FLAT_CLOUDS;

		if (rebuild || gridX != centerX || gridZ != centerZ || currentViewMode != viewMode || mode != renderMode) {
			rebuild = false;
			centerX = gridX;
			centerZ = gridZ;
			viewMode = currentViewMode;
			renderMode = mode;
			cloudFacesBuffer.rotate();

			try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice()
					.createCommandEncoder()
					.mapBuffer(cloudFacesBuffer.getBlocking(), false, true)
			) {
				buildCloudCells(currentViewMode, mappedView.data(), gridX, gridZ, isFancy, cloudRange);
				instanceCount = mappedView.data().position() / 3;
			}
		}

		if (instanceCount == 0) {
			return;
		}

		try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice()
				.createCommandEncoder()
				.mapBuffer(cloudInfoBuffer.getBlocking(), false, true)
		) {
			Std140Builder.intoBuffer(mappedView.data())
					.putVec4(ColorHelper.toRgbaVector(color))
					.putVec3(-subCellOffsetX, relativeHeight, -subCellOffsetZ)
					.putVec3(CLOUD_BLOCK_SIZE, CLOUD_THICKNESS / 8.0F, CLOUD_BLOCK_SIZE);
		}

		GpuBufferSlice dynamicUniforms = RenderSystem.getDynamicUniforms().write(
				RenderSystem.getModelViewMatrix(),
				new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
				new Vector3f(),
				new Matrix4f()
		);
		Framebuffer mainFramebuffer = MinecraftClient.getInstance().getFramebuffer();
		Framebuffer cloudsFramebuffer = MinecraftClient.getInstance().worldRenderer.getCloudsFramebuffer();
		RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
		GpuBuffer indexBuffer = shapeIndexBuffer.getIndexBuffer(6 * instanceCount);
		GpuTextureView colorView;
		GpuTextureView depthView;

		if (cloudsFramebuffer != null) {
			colorView = cloudsFramebuffer.getColorAttachmentView();
			depthView = cloudsFramebuffer.getDepthAttachmentView();
		}
		else {
			colorView = mainFramebuffer.getColorAttachmentView();
			depthView = mainFramebuffer.getDepthAttachmentView();
		}

		try (RenderPass renderPass = RenderSystem.getDevice()
				.createCommandEncoder()
				.createRenderPass(
						() -> "Clouds",
						colorView,
						OptionalInt.empty(),
						depthView,
						OptionalDouble.empty()
				)
		) {
			renderPass.setPipeline(renderPipeline);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", dynamicUniforms);
			renderPass.setIndexBuffer(indexBuffer, shapeIndexBuffer.getIndexType());
			renderPass.setUniform("CloudInfo", cloudInfoBuffer.getBlocking());
			renderPass.setUniform("CloudFaces", cloudFacesBuffer.getBlocking());
			renderPass.drawIndexed(0, 0, 6 * instanceCount, 1);
		}
	}

	private void buildCloudCells(
			CloudRenderer.ViewMode viewMode,
			ByteBuffer byteBuffer,
			int centerX,
			int centerZ,
			boolean fancy,
			int cloudRange
	) {
		if (cells == null) {
			return;
		}

		long[] cellData = cells.cells;
		int gridWidth = cells.width;
		int gridHeight = cells.height;

		for (int ring = 0; ring <= 2 * cloudRange; ring++) {
			for (int dx = -ring; dx <= ring; dx++) {
				int dz = ring - Math.abs(dx);
				if (dz >= 0 && dz <= cloudRange && dx * dx + dz * dz <= cloudRange * cloudRange) {
					if (dz != 0) {
						buildCloudCellAt(viewMode, byteBuffer, centerX, centerZ, fancy, dx, gridWidth, -dz, gridHeight, cellData);
					}

					buildCloudCellAt(viewMode, byteBuffer, centerX, centerZ, fancy, dx, gridWidth, dz, gridHeight, cellData);
				}
			}
		}
	}

	private void buildCloudCellAt(
			CloudRenderer.ViewMode viewMode,
			ByteBuffer byteBuffer,
			int centerX,
			int centerZ,
			boolean fancy,
			int dx,
			int gridWidth,
			int dz,
			int gridHeight,
			long[] cellData
	) {
		int cellX = Math.floorMod(centerX + dx, gridWidth);
		int cellZ = Math.floorMod(centerZ + dz, gridHeight);
		long packed = cellData[cellX + cellZ * gridWidth];

		if (packed == 0L) {
			return;
		}

		if (fancy) {
			buildCloudCellFancy(viewMode, byteBuffer, dx, dz, packed);
		}
		else {
			buildCloudCellFast(byteBuffer, dx, dz);
		}
	}

	private void buildCloudCellFast(ByteBuffer byteBuffer, int x, int z) {
		writeCloudFaceData(byteBuffer, x, z, Direction.DOWN, CLOUD_THICKNESS);
	}

	private void writeCloudFaceData(ByteBuffer byteBuffer, int x, int z, Direction direction, int flags) {
		int packed = direction.getIndex() | flags;
		packed |= (x & 1) << 7;
		packed |= (z & 1) << 6;
		byteBuffer.put((byte) (x >> 1)).put((byte) (z >> 1)).put((byte) packed);
	}

	private void buildCloudCellFancy(CloudRenderer.ViewMode viewMode, ByteBuffer byteBuffer, int x, int z, long packed) {
		if (viewMode != CloudRenderer.ViewMode.BELOW_CLOUDS) {
			writeCloudFaceData(byteBuffer, x, z, Direction.UP, 0);
		}

		if (viewMode != CloudRenderer.ViewMode.ABOVE_CLOUDS) {
			writeCloudFaceData(byteBuffer, x, z, Direction.DOWN, 0);
		}

		if (hasBorderNorth(packed) && z > 0) {
			writeCloudFaceData(byteBuffer, x, z, Direction.NORTH, 0);
		}

		if (hasBorderSouth(packed) && z < 0) {
			writeCloudFaceData(byteBuffer, x, z, Direction.SOUTH, 0);
		}

		if (hasBorderWest(packed) && x > 0) {
			writeCloudFaceData(byteBuffer, x, z, Direction.WEST, 0);
		}

		if (hasBorderEast(packed) && x < 0) {
			writeCloudFaceData(byteBuffer, x, z, Direction.EAST, 0);
		}

		boolean isNearCenter = Math.abs(x) <= 1 && Math.abs(z) <= 1;
		if (isNearCenter) {
			for (Direction direction : Direction.values()) {
				writeCloudFaceData(byteBuffer, x, z, direction, CLOUD_CELL_SIZE);
			}
		}
	}

	public void scheduleTerrainUpdate() {
		rebuild = true;
	}

	public void rotate() {
		cloudInfoBuffer.rotate();
	}

	@Override
	public void close() {
		cloudInfoBuffer.close();
		if (cloudFacesBuffer != null) {
			cloudFacesBuffer.close();
		}
	}

	/**
	 * Упакованные данные облачного поля, загруженные из текстуры.
	 * Каждая ячейка хранит цвет и флаги видимых границ в одном {@code long}.
	 */
	@Environment(EnvType.CLIENT)
	public record CloudCells(long[] cells, int width, int height) {
	}

	/** Позиция камеры относительно слоя облаков: выше, внутри или ниже. */
	@Environment(EnvType.CLIENT)
	enum ViewMode {
		ABOVE_CLOUDS,
		INSIDE_CLOUDS,
		BELOW_CLOUDS
	}
}

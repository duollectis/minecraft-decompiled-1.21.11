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

@Environment(EnvType.CLIENT)
/**
 * {@code CloudRenderer}.
 */
public class CloudRenderer extends SinglePreparationResourceReloader<Optional<CloudRenderer.CloudCells>> implements AutoCloseable {

	private static final int CLOUD_CELL_SIZE = 16;
	private static final int CLOUD_THICKNESS = 32;
	private static final float CLOUD_BLOCK_SIZE = 12.0F;
	private static final int CLOUD_SCROLL_PERIOD = 400;
	private static final float CLOUD_ALPHA_THRESHOLD = 0.6F;
	private static final int UBO_SIZE = new Std140SizeCalculator().putVec4().putVec3().putVec3().get();
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Identifier CLOUD_TEXTURE = Identifier.ofVanilla("textures/environment/clouds.png");
	private static final long CLOUD_INITIAL_OFFSET = 0L;
	private static final int FACE_FLAG_NORTH = 4;
	private static final int FACE_FLAG_EAST = 3;
	private static final int FACE_FLAG_SOUTH = 2;
	private static final int FACE_FLAG_WEST = 1;
	private static final int FACE_FLAG_NONE = 0;
	private boolean rebuild = true;
	private int centerX = Integer.MIN_VALUE;
	private int centerZ = Integer.MIN_VALUE;
	private CloudRenderer.ViewMode viewMode = CloudRenderer.ViewMode.INSIDE_CLOUDS;
	private @Nullable CloudRenderMode renderMode;
	private CloudRenderer.@Nullable CloudCells cells;
	private int instanceCount = 0;
	private final MappableRingBuffer cloudInfoBuffer = new MappableRingBuffer(() -> "Cloud UBO", 130, UBO_SIZE);
	private @Nullable MappableRingBuffer cloudFacesBuffer;

	protected Optional<CloudRenderer.CloudCells> prepare(ResourceManager resourceManager, Profiler profiler) {
		try {
			Optional var20;
			try (
					InputStream inputStream = resourceManager.open(CLOUD_TEXTURE);
					NativeImage nativeImage = NativeImage.read(inputStream);
			) {
				int i = nativeImage.getWidth();
				int j = nativeImage.getHeight();
				long[] ls = new long[i * j];

				for (int k = 0; k < j; k++) {
					for (int l = 0; l < i; l++) {
						int m = nativeImage.getColorArgb(l, k);
						if (isEmpty(m)) {
							ls[l + k * i] = 0L;
						}
						else {
							boolean bl = isEmpty(nativeImage.getColorArgb(l, Math.floorMod(k - 1, j)));
							boolean bl2 = isEmpty(nativeImage.getColorArgb(Math.floorMod(l + 1, j), k));
							boolean bl3 = isEmpty(nativeImage.getColorArgb(l, Math.floorMod(k + 1, j)));
							boolean bl4 = isEmpty(nativeImage.getColorArgb(Math.floorMod(l - 1, j), k));
							ls[l + k * i] = packCloudCell(m, bl, bl2, bl3, bl4);
						}
					}
				}

				var20 = Optional.of(new CloudRenderer.CloudCells(ls, i, j));
			}

			return var20;
		}
		catch (IOException var19) {
			LOGGER.error("Failed to load cloud texture", var19);
			return Optional.empty();
		}
	}

	private static int calcCloudBufferSize(int cloudRange) {
		int i = 4;
		int j = (cloudRange + 1) * 2 * (cloudRange + 1) * 2 / 2;
		int k = j * 4 + 54;
		return k * 3;
	}

	protected void apply(
			Optional<CloudRenderer.CloudCells> optional,
			ResourceManager resourceManager,
			Profiler profiler
	) {
		this.cells = optional.orElse(null);
		this.rebuild = true;
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
		return (long) color << 4 | (borderNorth ? 1 : 0) << 3 | (borderEast ? 1 : 0) << 2 | (borderSouth ? 1 : 0) << 1
				| (borderWest ? 1 : 0) << 0;
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
		return (packed >> 0 & 1L) != 0L;
	}

	public void renderClouds(int color, CloudRenderMode mode, float cloudHeight, Vec3d vec3d, long l, float f) {
		if (this.cells != null) {
			int i = MinecraftClient.getInstance().options.getCloudRenderDistance().getValue() * 16;
			int j = MathHelper.ceil(i / 12.0F);
			int k = calcCloudBufferSize(j);
			if (this.cloudFacesBuffer == null || this.cloudFacesBuffer.getBlocking().size() != k) {
				if (this.cloudFacesBuffer != null) {
					this.cloudFacesBuffer.close();
				}

				this.cloudFacesBuffer = new MappableRingBuffer(() -> "Cloud UTB", 258, k);
			}

			float g = (float) (cloudHeight - vec3d.y);
			float h = g + 4.0F;
			CloudRenderer.ViewMode viewMode;
			if (h < 0.0F) {
				viewMode = CloudRenderer.ViewMode.ABOVE_CLOUDS;
			}
			else if (g > 0.0F) {
				viewMode = CloudRenderer.ViewMode.BELOW_CLOUDS;
			}
			else {
				viewMode = CloudRenderer.ViewMode.INSIDE_CLOUDS;
			}

			float m = (float) (l % (this.cells.width * 400L)) + f;
			double d = vec3d.x + m * 0.030000001F;
			double e = vec3d.z + 3.96F;
			double n = this.cells.width * 12.0;
			double o = this.cells.height * 12.0;
			d -= MathHelper.floor(d / n) * n;
			e -= MathHelper.floor(e / o) * o;
			int p = MathHelper.floor(d / 12.0);
			int q = MathHelper.floor(e / 12.0);
			float r = (float) (d - p * 12.0F);
			float s = (float) (e - q * 12.0F);
			boolean bl = mode == CloudRenderMode.FANCY;
			RenderPipeline renderPipeline = bl ? RenderPipelines.CLOUDS : RenderPipelines.FLAT_CLOUDS;
			if (this.rebuild || p != this.centerX || q != this.centerZ || viewMode != this.viewMode
					|| mode != this.renderMode) {
				this.rebuild = false;
				this.centerX = p;
				this.centerZ = q;
				this.viewMode = viewMode;
				this.renderMode = mode;
				this.cloudFacesBuffer.rotate();

				try (GpuBuffer.MappedView mappedView = RenderSystem
						.getDevice()
						.createCommandEncoder()
						.mapBuffer(this.cloudFacesBuffer.getBlocking(), false, true)
				) {
					this.buildCloudCells(viewMode, mappedView.data(), p, q, bl, j);
					this.instanceCount = mappedView.data().position() / 3;
				}
			}

			if (this.instanceCount != 0) {
				try (GpuBuffer.MappedView mappedView = RenderSystem
						.getDevice()
						.createCommandEncoder()
						.mapBuffer(this.cloudInfoBuffer.getBlocking(), false, true)
				) {
					Std140Builder
							.intoBuffer(mappedView.data())
							.putVec4(ColorHelper.toRgbaVector(color))
							.putVec3(-r, g, -s)
							.putVec3(12.0F, 4.0F, 12.0F);
				}

				GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
				                                            .write(
						                                            RenderSystem.getModelViewMatrix(),
						                                            new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
						                                            new Vector3f(),
						                                            new Matrix4f()
				                                            );
				Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
				Framebuffer framebuffer2 = MinecraftClient.getInstance().worldRenderer.getCloudsFramebuffer();
				RenderSystem.ShapeIndexBuffer
						shapeIndexBuffer =
						RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
				GpuBuffer gpuBuffer = shapeIndexBuffer.getIndexBuffer(6 * this.instanceCount);
				GpuTextureView gpuTextureView;
				GpuTextureView gpuTextureView2;
				if (framebuffer2 != null) {
					gpuTextureView = framebuffer2.getColorAttachmentView();
					gpuTextureView2 = framebuffer2.getDepthAttachmentView();
				}
				else {
					gpuTextureView = framebuffer.getColorAttachmentView();
					gpuTextureView2 = framebuffer.getDepthAttachmentView();
				}

				try (RenderPass renderPass = RenderSystem.getDevice()
				                                         .createCommandEncoder()
				                                         .createRenderPass(
						                                         () -> "Clouds",
						                                         gpuTextureView,
						                                         OptionalInt.empty(),
						                                         gpuTextureView2,
						                                         OptionalDouble.empty()
				                                         )
				) {
					renderPass.setPipeline(renderPipeline);
					RenderSystem.bindDefaultUniforms(renderPass);
					renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
					renderPass.setIndexBuffer(gpuBuffer, shapeIndexBuffer.getIndexType());
					renderPass.setUniform("CloudInfo", this.cloudInfoBuffer.getBlocking());
					renderPass.setUniform("CloudFaces", this.cloudFacesBuffer.getBlocking());
					renderPass.drawIndexed(0, 0, 6 * this.instanceCount, 1);
				}
			}
		}
	}

	private void buildCloudCells(
			CloudRenderer.ViewMode viewMode,
			ByteBuffer byteBuffer,
			int x,
			int z,
			boolean bl,
			int i
	) {
		if (this.cells != null) {
			long[] ls = this.cells.cells;
			int j = this.cells.width;
			int k = this.cells.height;

			for (int l = 0; l <= 2 * i; l++) {
				for (int m = -l; m <= l; m++) {
					int n = l - Math.abs(m);
					if (n >= 0 && n <= i && m * m + n * n <= i * i) {
						if (n != 0) {
							this.buildCloudCellAt(viewMode, byteBuffer, x, z, bl, m, j, -n, k, ls);
						}

						this.buildCloudCellAt(viewMode, byteBuffer, x, z, bl, m, j, n, k, ls);
					}
				}
			}
		}
	}

	private void buildCloudCellAt(
			CloudRenderer.ViewMode viewMode,
			ByteBuffer byteBuffer,
			int i,
			int j,
			boolean bl,
			int k,
			int l,
			int m,
			int n,
			long[] ls
	) {
		int o = Math.floorMod(i + k, l);
		int p = Math.floorMod(j + m, n);
		long q = ls[o + p * l];
		if (q != 0L) {
			if (bl) {
				this.buildCloudCellFancy(viewMode, byteBuffer, k, m, q);
			}
			else {
				this.buildCloudCellFast(byteBuffer, k, m);
			}
		}
	}

	private void buildCloudCellFast(ByteBuffer byteBuffer, int color, int x) {
		this.writeCloudFaceData(byteBuffer, color, x, Direction.DOWN, 32);
	}

	private void writeCloudFaceData(ByteBuffer byteBuffer, int i, int j, Direction direction, int k) {
		int l = direction.getIndex() | k;
		l |= (i & 1) << 7;
		l |= (j & 1) << 6;
		byteBuffer.put((byte) (i >> 1)).put((byte) (j >> 1)).put((byte) l);
	}

	private void buildCloudCellFancy(CloudRenderer.ViewMode viewMode, ByteBuffer byteBuffer, int i, int j, long l) {
		if (viewMode != CloudRenderer.ViewMode.BELOW_CLOUDS) {
			this.writeCloudFaceData(byteBuffer, i, j, Direction.UP, 0);
		}

		if (viewMode != CloudRenderer.ViewMode.ABOVE_CLOUDS) {
			this.writeCloudFaceData(byteBuffer, i, j, Direction.DOWN, 0);
		}

		if (hasBorderNorth(l) && j > 0) {
			this.writeCloudFaceData(byteBuffer, i, j, Direction.NORTH, 0);
		}

		if (hasBorderSouth(l) && j < 0) {
			this.writeCloudFaceData(byteBuffer, i, j, Direction.SOUTH, 0);
		}

		if (hasBorderWest(l) && i > 0) {
			this.writeCloudFaceData(byteBuffer, i, j, Direction.WEST, 0);
		}

		if (hasBorderEast(l) && i < 0) {
			this.writeCloudFaceData(byteBuffer, i, j, Direction.EAST, 0);
		}

		boolean bl = Math.abs(i) <= 1 && Math.abs(j) <= 1;
		if (bl) {
			for (Direction direction : Direction.values()) {
				this.writeCloudFaceData(byteBuffer, i, j, direction, 16);
			}
		}
	}

	public void scheduleTerrainUpdate() {
		this.rebuild = true;
	}

	public void rotate() {
		this.cloudInfoBuffer.rotate();
	}

	@Override
	public void close() {
		this.cloudInfoBuffer.close();
		if (this.cloudFacesBuffer != null) {
			this.cloudFacesBuffer.close();
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code CloudCells}.
	 */
	public record CloudCells(long[] cells, int width, int height) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ViewMode}.
	 */
	static enum ViewMode {
		ABOVE_CLOUDS,
		INSIDE_CLOUDS,
		BELOW_CLOUDS;
	}
}

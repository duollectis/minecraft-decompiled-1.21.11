package net.minecraft.client.texture;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.resource.metadata.AnimationFrameResourceMetadata;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.resource.metadata.TextureResourceMetadata;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.ColorHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.IntStream;

@Environment(EnvType.CLIENT)
/**
 * {@code SpriteContents}.
 */
public class SpriteContents implements TextureStitcher.Stitchable, AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final int
			SPRITE_INFO_SIZE =
			new Std140SizeCalculator().putMat4f().putMat4f().putFloat().putFloat().putInt().get();
	final Identifier id;
	final int width;
	final int height;
	private final NativeImage image;
	NativeImage[] mipmapLevelsImages;
	private final SpriteContents.@Nullable Animation animation;
	private final List<ResourceMetadataSerializer.Value<?>> additionalMetadata;
	private final MipmapStrategy strategy;
	private final float cutoffBias;

	public SpriteContents(Identifier id, SpriteDimensions dimensions, NativeImage image) {
		this(id, dimensions, image, Optional.empty(), List.of(), Optional.empty());
	}

	public SpriteContents(
			Identifier id,
			SpriteDimensions dimensions,
			NativeImage image,
			Optional<AnimationResourceMetadata> animationResourceMetadata,
			List<ResourceMetadataSerializer.Value<?>> additionalMetadata,
			Optional<TextureResourceMetadata> metadata
	) {
		this.id = id;
		this.width = dimensions.width();
		this.height = dimensions.height();
		this.additionalMetadata = additionalMetadata;
		this.animation = animationResourceMetadata.<SpriteContents.Animation>map(
				                                          animationMetadata -> this.createAnimation(
						                                          dimensions,
						                                          image.getWidth(),
						                                          image.getHeight(),
						                                          animationMetadata
				                                          )
		                                          )
		                                          .orElse(null);
		this.image = image;
		this.mipmapLevelsImages = new NativeImage[]{this.image};
		this.strategy = metadata.map(TextureResourceMetadata::mipmapStrategy).orElse(MipmapStrategy.AUTO);
		this.cutoffBias = metadata.map(TextureResourceMetadata::alphaCutoffBias).orElse(0.0F);
	}

	public void generateMipmaps(int mipmapLevels) {
		try {
			this.mipmapLevelsImages =
					MipmapHelper.getMipmapLevelsImages(
							this.id,
							this.mipmapLevelsImages,
							mipmapLevels,
							this.strategy,
							this.cutoffBias
					);
		}
		catch (Throwable var5) {
			CrashReport crashReport = CrashReport.create(var5, "Generating mipmaps for frame");
			CrashReportSection crashReportSection = crashReport.addElement("Frame being iterated");
			crashReportSection.add("Sprite name", this.id);
			crashReportSection.add("Sprite size", () -> this.width + " x " + this.height);
			crashReportSection.add("Sprite frames", () -> this.getFrameCount() + " frames");
			crashReportSection.add("Mipmap levels", mipmapLevels);
			crashReportSection.add("Original image size", () -> this.image.getWidth() + "x" + this.image.getHeight());
			throw new CrashException(crashReport);
		}
	}

	private int getFrameCount() {
		return this.animation != null ? this.animation.frames.size() : 1;
	}

	public boolean isAnimated() {
		return this.getFrameCount() > 1;
	}

	private SpriteContents.@Nullable Animation createAnimation(
			SpriteDimensions dimensions,
			int imageWidth,
			int imageHeight,
			AnimationResourceMetadata metadata
	) {
		int i = imageWidth / dimensions.width();
		int j = imageHeight / dimensions.height();
		int k = i * j;
		int l = metadata.defaultFrameTime();
		List<SpriteContents.AnimationFrame> list;
		if (metadata.frames().isEmpty()) {
			list = new ArrayList<>(k);

			for (int m = 0; m < k; m++) {
				list.add(new SpriteContents.AnimationFrame(m, l));
			}
		}
		else {
			List<AnimationFrameResourceMetadata> list2 = metadata.frames().get();
			list = new ArrayList<>(list2.size());

			for (AnimationFrameResourceMetadata animationFrameResourceMetadata : list2) {
				list.add(new SpriteContents.AnimationFrame(
						animationFrameResourceMetadata.index(),
						animationFrameResourceMetadata.getTime(l)
				));
			}

			int n = 0;
			IntSet intSet = new IntOpenHashSet();

			for (Iterator<SpriteContents.AnimationFrame> iterator = list.iterator(); iterator.hasNext(); n++) {
				SpriteContents.AnimationFrame animationFrame = iterator.next();
				boolean bl = true;
				if (animationFrame.time <= 0) {
					LOGGER.warn(
							"Invalid frame duration on sprite {} frame {}: {}",
							new Object[]{this.id, n, animationFrame.time}
					);
					bl = false;
				}

				if (animationFrame.index < 0 || animationFrame.index >= k) {
					LOGGER.warn(
							"Invalid frame index on sprite {} frame {}: {}",
							new Object[]{this.id, n, animationFrame.index}
					);
					bl = false;
				}

				if (bl) {
					intSet.add(animationFrame.index);
				}
				else {
					iterator.remove();
				}
			}

			int[] is = IntStream.range(0, k).filter(ix -> !intSet.contains(ix)).toArray();
			if (is.length > 0) {
				LOGGER.warn("Unused frames in sprite {}: {}", this.id, Arrays.toString(is));
			}
		}

		return list.size() <= 1 ? null : new SpriteContents.Animation(List.copyOf(list), i, metadata.interpolate());
	}

	@Override
	public int getWidth() {
		return this.width;
	}

	@Override
	public int getHeight() {
		return this.height;
	}

	@Override
	public Identifier getId() {
		return this.id;
	}

	public IntStream getDistinctFrameCount() {
		return this.animation != null ? this.animation.getDistinctFrameCount() : IntStream.of(1);
	}

	public SpriteContents.@Nullable Animator createAnimator(GpuBufferSlice bufferSlice, int animationInfoSize) {
		return this.animation != null ? this.animation.createAnimator(bufferSlice, animationInfoSize) : null;
	}

	public <T> Optional<T> getAdditionalMetadataValue(ResourceMetadataSerializer<T> serializer) {
		for (ResourceMetadataSerializer.Value<?> value : this.additionalMetadata) {
			Optional<T> optional = value.getValueIfMatching(serializer);
			if (optional.isPresent()) {
				return optional;
			}
		}

		return Optional.empty();
	}

	@Override
	public void close() {
		for (NativeImage nativeImage : this.mipmapLevelsImages) {
			nativeImage.close();
		}
	}

	@Override
	public String toString() {
		return "SpriteContents{name=" + this.id + ", frameCount=" + this.getFrameCount() + ", height=" + this.height
				+ ", width=" + this.width + "}";
	}

	public boolean isPixelTransparent(int frame, int x, int y) {
		int i = x;
		int j = y;
		if (this.animation != null) {
			i = x + this.animation.getFrameX(frame) * this.width;
			j = y + this.animation.getFrameY(frame) * this.height;
		}

		return ColorHelper.getAlpha(this.image.getColorArgb(i, j)) == 0;
	}

	public void upload(GpuTexture texture, int mipmap) {
		RenderSystem.getDevice()
		            .createCommandEncoder()
		            .writeToTexture(
				            texture,
				            this.mipmapLevelsImages[mipmap],
				            mipmap,
				            0,
				            0,
				            0,
				            this.width >> mipmap,
				            this.height >> mipmap,
				            0,
				            0
		            );
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Animation}.
	 */
	class Animation {

		final List<SpriteContents.AnimationFrame> frames;
		private final int frameCount;
		final boolean interpolated;

		Animation(final List<SpriteContents.AnimationFrame> frames, final int frameCount, final boolean interpolated) {
			this.frames = frames;
			this.frameCount = frameCount;
			this.interpolated = interpolated;
		}

		int getFrameX(int frame) {
			return frame % this.frameCount;
		}

		int getFrameY(int frame) {
			return frame / this.frameCount;
		}

		public SpriteContents.Animator createAnimator(GpuBufferSlice bufferSlice, int animationInfoSize) {
			GpuDevice gpuDevice = RenderSystem.getDevice();
			Int2ObjectMap<GpuTextureView> int2ObjectMap = new Int2ObjectOpenHashMap();
			GpuBufferSlice[] gpuBufferSlices = new GpuBufferSlice[SpriteContents.this.mipmapLevelsImages.length];

			for (int i : this.getDistinctFrameCount().toArray()) {
				GpuTexture gpuTexture = gpuDevice.createTexture(
						() -> SpriteContents.this.id + " animation frame " + i,
						5,
						TextureFormat.RGBA8,
						SpriteContents.this.width,
						SpriteContents.this.height,
						1,
						SpriteContents.this.mipmapLevelsImages.length + 1
				);
				int j = this.getFrameX(i) * SpriteContents.this.width;
				int k = this.getFrameY(i) * SpriteContents.this.height;

				for (int l = 0; l < SpriteContents.this.mipmapLevelsImages.length; l++) {
					RenderSystem.getDevice()
					            .createCommandEncoder()
					            .writeToTexture(
							            gpuTexture,
							            SpriteContents.this.mipmapLevelsImages[l],
							            l,
							            0,
							            0,
							            0,
							            SpriteContents.this.width >> l,
							            SpriteContents.this.height >> l,
							            j >> l,
							            k >> l
					            );
				}

				int2ObjectMap.put(i, RenderSystem.getDevice().createTextureView(gpuTexture));
			}

			for (int m = 0; m < SpriteContents.this.mipmapLevelsImages.length; m++) {
				gpuBufferSlices[m] = bufferSlice.slice(m * animationInfoSize, animationInfoSize);
			}

			return SpriteContents.this.new Animator(this, int2ObjectMap, gpuBufferSlices);
		}

		public IntStream getDistinctFrameCount() {
			return this.frames.stream().mapToInt(frame -> frame.index).distinct();
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code AnimationFrame}.
	 */
	record AnimationFrame(int index, int time) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Animator}.
	 */
	public class Animator implements AutoCloseable {

		private int frame;
		private int elapsedTimeInFrame;
		private final SpriteContents.Animation animation;
		private final Int2ObjectMap<GpuTextureView> textureViewsByFrame;
		private final GpuBufferSlice[] animationInfosByFrame;
		private boolean changedFrame = true;

		Animator(
				final SpriteContents.Animation animation,
				final Int2ObjectMap<GpuTextureView> textureViewsByFrame,
				final GpuBufferSlice[] bufferSlices
		) {
			this.animation = animation;
			this.textureViewsByFrame = textureViewsByFrame;
			this.animationInfosByFrame = bufferSlices;
		}

		public void tick() {
			this.elapsedTimeInFrame++;
			this.changedFrame = false;
			SpriteContents.AnimationFrame animationFrame = this.animation.frames.get(this.frame);
			if (this.elapsedTimeInFrame >= animationFrame.time) {
				int i = animationFrame.index;
				this.frame = (this.frame + 1) % this.animation.frames.size();
				this.elapsedTimeInFrame = 0;
				int j = this.animation.frames.get(this.frame).index;
				if (i != j) {
					this.changedFrame = true;
				}
			}
		}

		public GpuBufferSlice getBufferSlice(int frame) {
			return this.animationInfosByFrame[frame];
		}

		public boolean isDirty() {
			return this.animation.interpolated || this.changedFrame;
		}

		public void upload(RenderPass renderPass, GpuBufferSlice bufferSlice) {
			GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST, true);
			List<SpriteContents.AnimationFrame> list = this.animation.frames;
			int i = list.get(this.frame).index;
			float f = (float) this.elapsedTimeInFrame / this.animation.frames.get(this.frame).time;
			int j = (int) (f * 1000.0F);
			if (this.animation.interpolated) {
				int k = list.get((this.frame + 1) % list.size()).index;
				renderPass.setPipeline(RenderPipelines.ANIMATE_SPRITE_INTERPOLATE);
				renderPass.bindTexture("CurrentSprite", (GpuTextureView) this.textureViewsByFrame.get(i), gpuSampler);
				renderPass.bindTexture("NextSprite", (GpuTextureView) this.textureViewsByFrame.get(k), gpuSampler);
			}
			else if (this.changedFrame) {
				renderPass.setPipeline(RenderPipelines.ANIMATE_SPRITE_BLIT);
				renderPass.bindTexture("Sprite", (GpuTextureView) this.textureViewsByFrame.get(i), gpuSampler);
			}

			renderPass.setUniform("SpriteAnimationInfo", bufferSlice);
			renderPass.draw(j << 3, 6);
		}

		@Override
		public void close() {
			ObjectIterator var1 = this.textureViewsByFrame.values().iterator();

			while (var1.hasNext()) {
				GpuTextureView gpuTextureView = (GpuTextureView) var1.next();
				gpuTextureView.texture().close();
				gpuTextureView.close();
			}
		}
	}
}

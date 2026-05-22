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

/**
 * Содержимое спрайта: пиксельные данные всех уровней mipmap и опциональная анимация.
 *
 * <p>Хранит {@link NativeImage} для каждого уровня mipmap. Если спрайт анимирован,
 * содержит {@link Animation} с набором кадров. Класс {@link Animator} управляет
 * воспроизведением анимации и загрузкой кадров на GPU.
 */
@Environment(EnvType.CLIENT)
public class SpriteContents implements TextureStitcher.Stitchable, AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final int SPRITE_INFO_SIZE =
		new Std140SizeCalculator().putMat4f().putMat4f().putFloat().putFloat().putInt().get();

	final Identifier id;
	final int width;
	final int height;
	private final NativeImage image;
	NativeImage[] mipmapLevelsImages;
	private final @Nullable Animation animation;
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
		width = dimensions.width();
		height = dimensions.height();
		this.additionalMetadata = additionalMetadata;
		animation = animationResourceMetadata
			.<Animation>map(
				animationMetadata -> createAnimation(
					dimensions,
					image.getWidth(),
					image.getHeight(),
					animationMetadata
				)
			)
			.orElse(null);
		this.image = image;
		mipmapLevelsImages = new NativeImage[]{this.image};
		strategy = metadata.map(TextureResourceMetadata::mipmapStrategy).orElse(MipmapStrategy.AUTO);
		cutoffBias = metadata.map(TextureResourceMetadata::alphaCutoffBias).orElse(0.0F);
	}

	/**
	 * Генерирует mipmap-уровни для данного спрайта.
	 * При ошибке формирует подробный {@link CrashReport} с именем спрайта и его размерами.
	 */
	public void generateMipmaps(int mipmapLevels) {
		try {
			mipmapLevelsImages = MipmapHelper.getMipmapLevelsImages(
				id,
				mipmapLevelsImages,
				mipmapLevels,
				strategy,
				cutoffBias
			);
		} catch (Throwable cause) {
			CrashReport crashReport = CrashReport.create(cause, "Generating mipmaps for frame");
			CrashReportSection section = crashReport.addElement("Frame being iterated");
			section.add("Sprite name", id);
			section.add("Sprite size", () -> width + " x " + height);
			section.add("Sprite frames", () -> getFrameCount() + " frames");
			section.add("Mipmap levels", mipmapLevels);
			section.add("Original image size", () -> image.getWidth() + "x" + image.getHeight());
			throw new CrashException(crashReport);
		}
	}

	private int getFrameCount() {
		return animation != null ? animation.frames.size() : 1;
	}

	public boolean isAnimated() {
		return getFrameCount() > 1;
	}

	/**
	 * Разбирает метаданные анимации и строит список {@link AnimationFrame}.
	 * Кадры с некорректным индексом или нулевым временем отбрасываются с предупреждением.
	 * Возвращает {@code null}, если кадров меньше двух (статичный спрайт).
	 */
	private @Nullable Animation createAnimation(
		SpriteDimensions dimensions,
		int imageWidth,
		int imageHeight,
		AnimationResourceMetadata metadata
	) {
		int cols = imageWidth / dimensions.width();
		int rows = imageHeight / dimensions.height();
		int totalFrames = cols * rows;
		int defaultFrameTime = metadata.defaultFrameTime();
		List<AnimationFrame> frames;

		if (metadata.frames().isEmpty()) {
			frames = new ArrayList<>(totalFrames);
			for (int frameIndex = 0; frameIndex < totalFrames; frameIndex++) {
				frames.add(new AnimationFrame(frameIndex, defaultFrameTime));
			}
		} else {
			List<AnimationFrameResourceMetadata> rawFrames = metadata.frames().get();
			frames = new ArrayList<>(rawFrames.size());
			for (AnimationFrameResourceMetadata rawFrame : rawFrames) {
				frames.add(new AnimationFrame(rawFrame.index(), rawFrame.getTime(defaultFrameTime)));
			}

			int frameNumber = 0;
			IntSet usedIndices = new IntOpenHashSet();

			for (Iterator<AnimationFrame> iterator = frames.iterator(); iterator.hasNext(); frameNumber++) {
				AnimationFrame frame = iterator.next();
				boolean valid = true;

				if (frame.time <= 0) {
					LOGGER.warn(
						"Invalid frame duration on sprite {} frame {}: {}",
						new Object[]{id, frameNumber, frame.time}
					);
					valid = false;
				}

				if (frame.index < 0 || frame.index >= totalFrames) {
					LOGGER.warn(
						"Invalid frame index on sprite {} frame {}: {}",
						new Object[]{id, frameNumber, frame.index}
					);
					valid = false;
				}

				if (valid) {
					usedIndices.add(frame.index);
				} else {
					iterator.remove();
				}
			}

			int[] unusedFrames = IntStream.range(0, totalFrames)
				.filter(idx -> !usedIndices.contains(idx))
				.toArray();

			if (unusedFrames.length > 0) {
				LOGGER.warn("Unused frames in sprite {}: {}", id, Arrays.toString(unusedFrames));
			}
		}

		return frames.size() <= 1
			? null
			: new Animation(List.copyOf(frames), cols, metadata.interpolate());
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public Identifier getId() {
		return id;
	}

	public IntStream getDistinctFrameCount() {
		return animation != null ? animation.getDistinctFrameCount() : IntStream.of(1);
	}

	public @Nullable Animator createAnimator(GpuBufferSlice bufferSlice, int animationInfoSize) {
		return animation != null ? animation.createAnimator(bufferSlice, animationInfoSize) : null;
	}

	public <T> Optional<T> getAdditionalMetadataValue(ResourceMetadataSerializer<T> serializer) {
		for (ResourceMetadataSerializer.Value<?> value : additionalMetadata) {
			Optional<T> optional = value.getValueIfMatching(serializer);
			if (optional.isPresent()) {
				return optional;
			}
		}

		return Optional.empty();
	}

	public boolean isPixelTransparent(int frame, int x, int y) {
		int pixelX = x;
		int pixelY = y;

		if (animation != null) {
			pixelX = x + animation.getFrameX(frame) * width;
			pixelY = y + animation.getFrameY(frame) * height;
		}

		return ColorHelper.getAlpha(image.getColorArgb(pixelX, pixelY)) == 0;
	}

	public void upload(GpuTexture texture, int mipmap) {
		RenderSystem.getDevice()
			.createCommandEncoder()
			.writeToTexture(
				texture,
				mipmapLevelsImages[mipmap],
				mipmap,
				0,
				0,
				0,
				width >> mipmap,
				height >> mipmap,
				0,
				0
			);
	}

	@Override
	public void close() {
		for (NativeImage nativeImage : mipmapLevelsImages) {
			nativeImage.close();
		}
	}

	@Override
	public String toString() {
		return "SpriteContents{name=" + id + ", frameCount=" + getFrameCount()
			+ ", height=" + height + ", width=" + width + "}";
	}

	/** Данные анимации спрайта: список кадров, количество колонок и флаг интерполяции. */
	@Environment(EnvType.CLIENT)
	class Animation {

		final List<AnimationFrame> frames;
		private final int frameCount;
		final boolean interpolated;

		Animation(
			final List<AnimationFrame> frames,
			final int frameCount,
			final boolean interpolated
		) {
			this.frames = frames;
			this.frameCount = frameCount;
			this.interpolated = interpolated;
		}

		int getFrameX(int frame) {
			return frame % frameCount;
		}

		int getFrameY(int frame) {
			return frame / frameCount;
		}

		public IntStream getDistinctFrameCount() {
			return frames.stream().mapToInt(frame -> frame.index).distinct();
		}

		/**
		 * Создаёт {@link Animator} для данной анимации.
		 * Загружает каждый уникальный кадр в отдельную GPU-текстуру и нарезает
		 * {@code bufferSlice} на слайсы по одному на уровень mipmap.
		 */
		public Animator createAnimator(GpuBufferSlice bufferSlice, int animationInfoSize) {
			GpuDevice gpuDevice = RenderSystem.getDevice();
			Int2ObjectMap<GpuTextureView> textureViewsByFrame = new Int2ObjectOpenHashMap<>();
			GpuBufferSlice[] bufferSlices = new GpuBufferSlice[mipmapLevelsImages.length];

			for (int frameIndex : getDistinctFrameCount().toArray()) {
				GpuTexture gpuTexture = gpuDevice.createTexture(
					() -> id + " animation frame " + frameIndex,
					5,
					TextureFormat.RGBA8,
					width,
					height,
					1,
					mipmapLevelsImages.length + 1
				);
				int srcX = getFrameX(frameIndex) * width;
				int srcY = getFrameY(frameIndex) * height;

				for (int level = 0; level < mipmapLevelsImages.length; level++) {
					RenderSystem.getDevice()
						.createCommandEncoder()
						.writeToTexture(
							gpuTexture,
							mipmapLevelsImages[level],
							level,
							0,
							0,
							0,
							width >> level,
							height >> level,
							srcX >> level,
							srcY >> level
						);
				}

				textureViewsByFrame.put(frameIndex, RenderSystem.getDevice().createTextureView(gpuTexture));
			}

			for (int level = 0; level < mipmapLevelsImages.length; level++) {
				bufferSlices[level] = bufferSlice.slice(level * animationInfoSize, animationInfoSize);
			}

			return new Animator(this, textureViewsByFrame, bufferSlices);
		}
	}

	@Environment(EnvType.CLIENT)
	record AnimationFrame(int index, int time) {
	}

	/**
	 * Управляет воспроизведением анимации спрайта на GPU.
	 * Отслеживает текущий кадр и время, загружает нужные текстуры в render pass.
	 */
	@Environment(EnvType.CLIENT)
	public class Animator implements AutoCloseable {

		private int frame;
		private int elapsedTimeInFrame;
		private final Animation animation;
		private final Int2ObjectMap<GpuTextureView> textureViewsByFrame;
		private final GpuBufferSlice[] animationInfosByFrame;
		private boolean changedFrame = true;

		Animator(
			final Animation animation,
			final Int2ObjectMap<GpuTextureView> textureViewsByFrame,
			final GpuBufferSlice[] bufferSlices
		) {
			this.animation = animation;
			this.textureViewsByFrame = textureViewsByFrame;
			animationInfosByFrame = bufferSlices;
		}

		public void tick() {
			elapsedTimeInFrame++;
			changedFrame = false;
			AnimationFrame currentFrame = animation.frames.get(frame);

			if (elapsedTimeInFrame >= currentFrame.time) {
				int prevIndex = currentFrame.index;
				frame = (frame + 1) % animation.frames.size();
				elapsedTimeInFrame = 0;
				int nextIndex = animation.frames.get(frame).index;

				if (prevIndex != nextIndex) {
					changedFrame = true;
				}
			}
		}

		public GpuBufferSlice getBufferSlice(int level) {
			return animationInfosByFrame[level];
		}

		public boolean isDirty() {
			return animation.interpolated || changedFrame;
		}

		/**
		 * Загружает текущий (и следующий при интерполяции) кадр анимации в render pass.
		 * Выбирает пайплайн {@code ANIMATE_SPRITE_INTERPOLATE} или {@code ANIMATE_SPRITE_BLIT}
		 * в зависимости от режима анимации.
		 */
		public void upload(RenderPass renderPass, GpuBufferSlice bufferSlice) {
			GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST, true);
			List<AnimationFrame> frames = animation.frames;
			int currentIndex = frames.get(frame).index;
			float progress = (float) elapsedTimeInFrame / animation.frames.get(frame).time;
			int progressMillis = (int) (progress * 1000.0F);

			if (animation.interpolated) {
				int nextIndex = frames.get((frame + 1) % frames.size()).index;
				renderPass.setPipeline(RenderPipelines.ANIMATE_SPRITE_INTERPOLATE);
				renderPass.bindTexture("CurrentSprite", (GpuTextureView) textureViewsByFrame.get(currentIndex), sampler);
				renderPass.bindTexture("NextSprite", (GpuTextureView) textureViewsByFrame.get(nextIndex), sampler);
			} else if (changedFrame) {
				renderPass.setPipeline(RenderPipelines.ANIMATE_SPRITE_BLIT);
				renderPass.bindTexture("Sprite", (GpuTextureView) textureViewsByFrame.get(currentIndex), sampler);
			}

			renderPass.setUniform("SpriteAnimationInfo", bufferSlice);
			renderPass.draw(progressMillis << 3, 6);
		}

		@Override
		public void close() {
			for (GpuTextureView view : textureViewsByFrame.values()) {
				view.texture().close();
				view.close();
			}
		}
	}
}

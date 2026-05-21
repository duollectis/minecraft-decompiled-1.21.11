package net.minecraft.client.texture;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.sprite.FabricSpriteAtlasTexture;
import net.minecraft.SharedConstants;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

@Environment(EnvType.CLIENT)
/**
 * {@code SpriteAtlasTexture}.
 */
public class SpriteAtlasTexture extends AbstractTexture implements DynamicTexture, TextureTickListener, FabricSpriteAtlasTexture {

	private static final Logger LOGGER = LogUtils.getLogger();
	@Deprecated
	public static final Identifier BLOCK_ATLAS_TEXTURE = Identifier.ofVanilla("textures/atlas/blocks.png");
	@Deprecated
	public static final Identifier ITEMS_ATLAS_TEXTURE = Identifier.ofVanilla("textures/atlas/items.png");
	@Deprecated
	public static final Identifier PARTICLE_ATLAS_TEXTURE = Identifier.ofVanilla("textures/atlas/particles.png");
	private List<Sprite> spritesToLoad = List.of();
	private List<SpriteContents.Animator> animators = List.of();
	private Map<Identifier, Sprite> sprites = Map.of();
	private @Nullable Sprite missingSprite;
	private final Identifier id;
	private final int maxTextureSize;
	private int width;
	private int height;
	private int mipLevel;
	private int numMipLevels;
	private GpuTextureView[] mipTextures = new GpuTextureView[0];
	private @Nullable GpuBuffer uniformBuffer;

	public SpriteAtlasTexture(Identifier id) {
		this.id = id;
		this.maxTextureSize = RenderSystem.getDevice().getMaxTextureSize();
	}

	private void createTexture(int width, int height, int mipLevel) {
		LOGGER.info("Created: {}x{}x{} {}-atlas", new Object[]{width, height, mipLevel, this.id});
		GpuDevice gpuDevice = RenderSystem.getDevice();
		this.close();
		this.glTexture =
				gpuDevice.createTexture(this.id::toString, 15, TextureFormat.RGBA8, width, height, 1, mipLevel + 1);
		this.glTextureView = gpuDevice.createTextureView(this.glTexture);
		this.width = width;
		this.height = height;
		this.mipLevel = mipLevel;
		this.numMipLevels = mipLevel + 1;
		this.mipTextures = new GpuTextureView[this.numMipLevels];

		for (int i = 0; i <= this.mipLevel; i++) {
			this.mipTextures[i] = gpuDevice.createTextureView(this.glTexture, i, 1);
		}
	}

	/**
	 * Create.
	 *
	 * @param stitchResult stitch result
	 */
	public void create(SpriteLoader.StitchResult stitchResult) {
		this.createTexture(stitchResult.width(), stitchResult.height(), stitchResult.mipLevel());
		this.clear();
		this.sampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
		this.sprites = Map.copyOf(stitchResult.sprites());
		this.missingSprite = this.sprites.get(MissingSprite.getMissingSpriteId());
		if (this.missingSprite == null) {
			throw new IllegalStateException(
					"Atlas '" + this.id + "' (" + this.sprites.size() + " sprites) has no missing texture sprite");
		}
		else {
			List<Sprite> list = new ArrayList<>();
			List<SpriteContents.Animator> list2 = new ArrayList<>();
			int i = (int) stitchResult.sprites().values().stream().filter(Sprite::isAnimated).count();
			int
					j =
					MathHelper.roundUpToMultiple(
							SpriteContents.SPRITE_INFO_SIZE,
							RenderSystem.getDevice().getUniformOffsetAlignment()
					);
			int k = j * this.numMipLevels;
			ByteBuffer byteBuffer = MemoryUtil.memAlloc(i * k);
			int l = 0;

			for (Sprite sprite : stitchResult.sprites().values()) {
				if (sprite.isAnimated()) {
					sprite.putSpriteInfo(byteBuffer, l * k, this.mipLevel, this.width, this.height, j);
					l++;
				}
			}

			GpuBuffer
					gpuBuffer =
					l > 0 ? RenderSystem.getDevice().createBuffer(() -> this.id + " sprite UBOs", 128, byteBuffer)
					      : null;
			l = 0;

			for (Sprite sprite2 : stitchResult.sprites().values()) {
				list.add(sprite2);
				if (sprite2.isAnimated() && gpuBuffer != null) {
					SpriteContents.Animator animator = sprite2.createAnimator(gpuBuffer.slice(l * k, k), j);
					l++;
					if (animator != null) {
						list2.add(animator);
					}
				}
			}

			this.uniformBuffer = gpuBuffer;
			this.spritesToLoad = list;
			this.animators = List.copyOf(list2);
			this.upload();
			if (SharedConstants.DUMP_TEXTURE_ATLAS) {
				Path path = TextureUtil.getDebugTexturePath();

				try {
					Files.createDirectories(path);
					this.save(this.id, path);
				}
				catch (Exception var13) {
					LOGGER.warn("Failed to dump atlas contents to {}", path);
				}
			}
		}
	}

	private void upload() {
		GpuDevice gpuDevice = RenderSystem.getDevice();
		int
				i =
				MathHelper.roundUpToMultiple(
						SpriteContents.SPRITE_INFO_SIZE,
						RenderSystem.getDevice().getUniformOffsetAlignment()
				);
		int j = i * this.numMipLevels;
		GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST, true);
		List<Sprite> list = this.spritesToLoad.stream().filter(sprite -> !sprite.isAnimated()).toList();
		List<GpuTextureView[]> list2 = new ArrayList<>();
		ByteBuffer byteBuffer = MemoryUtil.memAlloc(list.size() * j);

		for (int k = 0; k < list.size(); k++) {
			Sprite sprite = list.get(k);
			sprite.putSpriteInfo(byteBuffer, k * j, this.mipLevel, this.width, this.height, i);
			GpuTexture gpuTexture = gpuDevice.createTexture(
					() -> sprite.getContents().getId().toString(),
					5,
					TextureFormat.RGBA8,
					sprite.getContents().getWidth(),
					sprite.getContents().getHeight(),
					1,
					this.numMipLevels
			);
			GpuTextureView[] gpuTextureViews = new GpuTextureView[this.numMipLevels];

			for (int l = 0; l <= this.mipLevel; l++) {
				sprite.upload(gpuTexture, l);
				gpuTextureViews[l] = gpuDevice.createTextureView(gpuTexture);
			}

			list2.add(gpuTextureViews);
		}

		try (GpuBuffer gpuBuffer = gpuDevice.createBuffer(() -> "SpriteAnimationInfo", 128, byteBuffer)) {
			for (int m = 0; m < this.numMipLevels; m++) {
				try (RenderPass renderPass = RenderSystem.getDevice()
				                                         .createCommandEncoder()
				                                         .createRenderPass(
						                                         () -> "Animate " + this.id,
						                                         this.mipTextures[m],
						                                         OptionalInt.empty()
				                                         )
				) {
					renderPass.setPipeline(RenderPipelines.ANIMATE_SPRITE_BLIT);

					for (int n = 0; n < list.size(); n++) {
						renderPass.bindTexture("Sprite", list2.get(n)[m], gpuSampler);
						renderPass.setUniform(
								"SpriteAnimationInfo",
								gpuBuffer.slice(n * j + m * i, SpriteContents.SPRITE_INFO_SIZE)
						);
						renderPass.draw(0, 6);
					}
				}
			}
		}

		for (GpuTextureView[] gpuTextureViews2 : list2) {
			for (GpuTextureView gpuTextureView : gpuTextureViews2) {
				gpuTextureView.close();
				gpuTextureView.texture().close();
			}
		}

		MemoryUtil.memFree(byteBuffer);
		this.uploadAnimations();
	}

	@Override
	public void save(Identifier id, Path path) throws IOException {
		String string = id.toUnderscoreSeparatedString();
		TextureUtil.writeAsPNG(path, string, this.getGlTexture(), this.mipLevel, color -> color);
		dumpAtlasInfos(path, string, this.sprites);
	}

	private static void dumpAtlasInfos(Path path, String id, Map<Identifier, Sprite> sprites) {
		Path path2 = path.resolve(id + ".txt");

		try (Writer writer = Files.newBufferedWriter(path2)) {
			for (Entry<Identifier, Sprite> entry : sprites
					.entrySet()
					.stream()
					.sorted(Entry.comparingByKey())
					.toList()) {
				Sprite sprite = entry.getValue();
				writer.write(
						String.format(
								Locale.ROOT,
								"%s\tx=%d\ty=%d\tw=%d\th=%d%n",
								entry.getKey(),
								sprite.getX(),
								sprite.getY(),
								sprite.getContents().getWidth(),
								sprite.getContents().getHeight()
						)
				);
			}
		}
		catch (IOException var10) {
			LOGGER.warn("Failed to write file {}", path2, var10);
		}
	}

	/**
	 * Выполняет тик обновления для animated sprites.
	 */
	public void tickAnimatedSprites() {
		if (this.glTexture != null) {
			for (SpriteContents.Animator animator : this.animators) {
				animator.tick();
			}

			this.uploadAnimations();
		}
	}

	private void uploadAnimations() {
		if (this.animators.stream().anyMatch(SpriteContents.Animator::isDirty)) {
			for (int i = 0; i <= this.mipLevel; i++) {
				try (RenderPass renderPass = RenderSystem.getDevice()
				                                         .createCommandEncoder()
				                                         .createRenderPass(
						                                         () -> "Animate " + this.id,
						                                         this.mipTextures[i],
						                                         OptionalInt.empty()
				                                         )
				) {
					for (SpriteContents.Animator animator : this.animators) {
						if (animator.isDirty()) {
							animator.upload(renderPass, animator.getBufferSlice(i));
						}
					}
				}
			}
		}
	}

	@Override
	public void tick() {
		this.tickAnimatedSprites();
	}

	public Sprite getSprite(Identifier id) {
		Sprite sprite = this.sprites.getOrDefault(id, this.missingSprite);
		if (sprite == null) {
			throw new IllegalStateException("Tried to lookup sprite, but atlas is not initialized");
		}
		else {
			return sprite;
		}
	}

	public Sprite getMissingSprite() {
		return Objects.requireNonNull(this.missingSprite, "Atlas not initialized");
	}

	/**
	 * Clear.
	 */
	public void clear() {
		this.spritesToLoad.forEach(Sprite::close);
		this.spritesToLoad = List.of();
		this.animators = List.of();
		this.sprites = Map.of();
		this.missingSprite = null;
	}

	@Override
	public void close() {
		super.close();

		for (GpuTextureView gpuTextureView : this.mipTextures) {
			gpuTextureView.close();
		}

		for (SpriteContents.Animator animator : this.animators) {
			animator.close();
		}

		if (this.uniformBuffer != null) {
			this.uniformBuffer.close();
			this.uniformBuffer = null;
		}
	}

	public Identifier getId() {
		return this.id;
	}

	public int getMaxTextureSize() {
		return this.maxTextureSize;
	}

	int getWidth() {
		return this.width;
	}

	int getHeight() {
		return this.height;
	}
}

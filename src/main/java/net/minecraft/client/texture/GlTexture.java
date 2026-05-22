package net.minecraft.client.texture;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.BufferManager;
import org.jspecify.annotations.Nullable;

/**
 * Реализация {@link GpuTexture} поверх OpenGL. Управляет временем жизни GL-текстуры
 * через счётчик ссылок ({@code refCount}): текстура физически удаляется только тогда,
 * когда она закрыта ({@code closed == true}) и счётчик ссылок достиг нуля.
 * Также кэширует framebuffer-объекты, связанные с этой текстурой как color attachment.
 */
@Environment(EnvType.CLIENT)
public class GlTexture extends GpuTexture {

	private static final int UNINITIALIZED = -1;

	public final int glId;
	private int framebufferId = UNINITIALIZED;
	private int depthGlId = UNINITIALIZED;
	private @Nullable Int2IntMap depthTexToFramebufferIdCache;
	protected boolean closed;
	private int refCount;

	public GlTexture(
		@GpuTexture.Usage int usage,
		String label,
		TextureFormat format,
		int width,
		int height,
		int depthOrLayers,
		int mipLevels,
		int glId
	) {
		super(usage, label, format, width, height, depthOrLayers, mipLevels);
		this.glId = glId;
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}

		closed = true;
		if (refCount == 0) {
			free();
		}
	}

	private void free() {
		GlStateManager._deleteTexture(glId);
		if (framebufferId != UNINITIALIZED) {
			GlStateManager._glDeleteFramebuffers(framebufferId);
		}

		if (depthTexToFramebufferIdCache == null) {
			return;
		}

		for (int framebuffer : depthTexToFramebufferIdCache.values()) {
			GlStateManager._glDeleteFramebuffers(framebuffer);
		}
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	/**
	 * Возвращает или создаёт framebuffer для этой текстуры как color attachment.
	 * Если {@code depthTexture} изменился по сравнению с предыдущим вызовом,
	 * создаётся новый framebuffer и кэшируется в {@code depthTexToFramebufferIdCache}.
	 */
	public int getOrCreateFramebuffer(BufferManager bufferManager, @Nullable GpuTexture depthTexture) {
		int depthId = depthTexture == null ? 0 : ((GlTexture) depthTexture).glId;
		if (depthGlId == depthId) {
			return framebufferId;
		}

		if (framebufferId == UNINITIALIZED) {
			framebufferId = createFramebuffer(bufferManager, depthId);
			depthGlId = depthId;
			return framebufferId;
		}

		if (depthTexToFramebufferIdCache == null) {
			depthTexToFramebufferIdCache = new Int2IntArrayMap();
		}

		return depthTexToFramebufferIdCache.computeIfAbsent(
			depthId,
			id -> createFramebuffer(bufferManager, id)
		);
	}

	private int createFramebuffer(BufferManager bufferManager, int depthGlId) {
		int id = bufferManager.createFramebuffer();
		bufferManager.setupFramebuffer(id, glId, depthGlId, 0, 0);
		return id;
	}

	public int getGlId() {
		return glId;
	}

	public void incrementRefCount() {
		refCount++;
	}

	public void decrementRefCount() {
		refCount--;
		if (closed && refCount == 0) {
			free();
		}
	}
}

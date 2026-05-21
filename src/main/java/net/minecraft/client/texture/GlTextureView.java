package net.minecraft.client.texture;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.BufferManager;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code GlTextureView}.
 */
public class GlTextureView extends GpuTextureView {

	private static final int UNINITIALIZED = -1;
	private boolean closed;
	private int framebufferId = -1;
	private int depthGlId = -1;
	private @Nullable Int2IntMap depthTexToFramebufferIdCache;

	public GlTextureView(GlTexture texture, int baseMipLevel, int mipLevels) {
		super(texture, baseMipLevel, mipLevels);
		texture.incrementRefCount();
	}

	@Override
	public boolean isClosed() {
		return this.closed;
	}

	@Override
	public void close() {
		if (!this.closed) {
			this.closed = true;
			this.texture().decrementRefCount();
			if (this.framebufferId != -1) {
				GlStateManager._glDeleteFramebuffers(this.framebufferId);
			}

			if (this.depthTexToFramebufferIdCache != null) {
				IntIterator var1 = this.depthTexToFramebufferIdCache.values().iterator();

				while (var1.hasNext()) {
					int i = (Integer) var1.next();
					GlStateManager._glDeleteFramebuffers(i);
				}
			}
		}
	}

	public int getOrCreateFramebuffer(BufferManager bufferManager, @Nullable GpuTexture depthTexture) {
		int i = depthTexture == null ? 0 : ((GlTexture) depthTexture).glId;
		if (this.depthGlId == i) {
			return this.framebufferId;
		}
		else if (this.framebufferId == -1) {
			this.framebufferId = this.createFramebuffer(bufferManager, i);
			this.depthGlId = i;
			return this.framebufferId;
		}
		else {
			if (this.depthTexToFramebufferIdCache == null) {
				this.depthTexToFramebufferIdCache = new Int2IntArrayMap();
			}

			return this.depthTexToFramebufferIdCache.computeIfAbsent(
					i,
					depthGlId -> this.createFramebuffer(bufferManager, depthGlId)
			);
		}
	}

	private int createFramebuffer(BufferManager bufferManager, int depthGlId) {
		int i = bufferManager.createFramebuffer();
		bufferManager.setupFramebuffer(i, this.texture().glId, depthGlId, this.baseMipLevel(), 0);
		return i;
	}

	public GlTexture texture() {
		return (GlTexture) super.texture();
	}
}

package net.minecraft.client.texture;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.textures.GpuTexture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.SpriteTexturedVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

@Environment(EnvType.CLIENT)
/**
 * {@code Sprite}.
 */
public class Sprite implements AutoCloseable {

	private final Identifier atlasId;
	private final SpriteContents contents;
	private final int x;
	private final int y;
	private final float minU;
	private final float maxU;
	private final float minV;
	private final float maxV;
	private final int padding;

	protected Sprite(
			Identifier atlasId,
			SpriteContents contents,
			int atlasWidth,
			int atlasHeight,
			int x,
			int y,
			int padding
	) {
		this.atlasId = atlasId;
		this.contents = contents;
		this.padding = padding;
		this.x = x;
		this.y = y;
		this.minU = (float) (x + padding) / atlasWidth;
		this.maxU = (float) (x + padding + contents.getWidth()) / atlasWidth;
		this.minV = (float) (y + padding) / atlasHeight;
		this.maxV = (float) (y + padding + contents.getHeight()) / atlasHeight;
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public float getMinU() {
		return this.minU;
	}

	public float getMaxU() {
		return this.maxU;
	}

	public SpriteContents getContents() {
		return this.contents;
	}

	public SpriteContents.@Nullable Animator createAnimator(GpuBufferSlice bufferSlice, int animationInfoSize) {
		return this.contents.createAnimator(bufferSlice, animationInfoSize);
	}

	public float getFrameU(float frame) {
		float f = this.maxU - this.minU;
		return this.minU + f * frame;
	}

	public float getMinV() {
		return this.minV;
	}

	public float getMaxV() {
		return this.maxV;
	}

	public float getFrameV(float frame) {
		float f = this.maxV - this.minV;
		return this.minV + f * frame;
	}

	public Identifier getAtlasId() {
		return this.atlasId;
	}

	@Override
	public String toString() {
		return "TextureAtlasSprite{contents='" + this.contents + "', u0=" + this.minU + ", u1=" + this.maxU + ", v0="
				+ this.minV + ", v1=" + this.maxV + "}";
	}

	public void upload(GpuTexture texture, int mipmap) {
		this.contents.upload(texture, mipmap);
	}

	public VertexConsumer getTextureSpecificVertexConsumer(VertexConsumer consumer) {
		return new SpriteTexturedVertexConsumer(consumer, this);
	}

	boolean isAnimated() {
		return this.contents.isAnimated();
	}

	public void putSpriteInfo(ByteBuffer buffer, int offset, int maxLevel, int width, int height, int stride) {
		for (int i = 0; i <= maxLevel; i++) {
			Std140Builder.intoBuffer(MemoryUtil.memSlice(buffer, offset + i * stride, stride))
			             .putMat4f(new Matrix4f().ortho2D(0.0F, width >> i, 0.0F, height >> i))
			             .putMat4f(
					             new Matrix4f()
							             .translate(this.x >> i, this.y >> i, 0.0F)
							             .scale(
									             this.contents.getWidth() + this.padding * 2 >> i,
									             this.contents.getHeight() + this.padding * 2 >> i,
									             1.0F
							             )
			             )
			             .putFloat((float) this.padding / this.contents.getWidth())
			             .putFloat((float) this.padding / this.contents.getHeight())
			             .putInt(i);
		}
	}

	@Override
	public void close() {
		this.contents.close();
	}
}

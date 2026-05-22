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

/**
 * Спрайт — прямоугольная область внутри текстурного атласа.
 *
 * <p>Хранит нормализованные UV-координаты ({@code minU/maxU/minV/maxV}) и
 * пиксельные координаты ({@code x/y}) своей позиции в атласе.
 * Делегирует загрузку пикселей и анимацию в {@link SpriteContents}.
 */
@Environment(EnvType.CLIENT)
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
		minU = (float) (x + padding) / atlasWidth;
		maxU = (float) (x + padding + contents.getWidth()) / atlasWidth;
		minV = (float) (y + padding) / atlasHeight;
		maxV = (float) (y + padding + contents.getHeight()) / atlasHeight;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public float getMinU() {
		return minU;
	}

	public float getMaxU() {
		return maxU;
	}

	public float getMinV() {
		return minV;
	}

	public float getMaxV() {
		return maxV;
	}

	public SpriteContents getContents() {
		return contents;
	}

	public Identifier getAtlasId() {
		return atlasId;
	}

	public SpriteContents.@Nullable Animator createAnimator(GpuBufferSlice bufferSlice, int animationInfoSize) {
		return contents.createAnimator(bufferSlice, animationInfoSize);
	}

	/** Возвращает U-координату для заданной доли {@code frame} внутри диапазона спрайта. */
	public float getFrameU(float frame) {
		float range = maxU - minU;
		return minU + range * frame;
	}

	/** Возвращает V-координату для заданной доли {@code frame} внутри диапазона спрайта. */
	public float getFrameV(float frame) {
		float range = maxV - minV;
		return minV + range * frame;
	}

	public VertexConsumer getTextureSpecificVertexConsumer(VertexConsumer consumer) {
		return new SpriteTexturedVertexConsumer(consumer, this);
	}

	boolean isAnimated() {
		return contents.isAnimated();
	}

	/**
	 * Записывает данные спрайта в GPU-буфер для каждого уровня mipmap.
	 *
	 * <p>Для каждого уровня {@code i} от 0 до {@code maxLevel} включительно записывает
	 * две матрицы (ортографическая проекция атласа и трансформация спрайта) и
	 * два float-значения отступа. Используется шейдером анимации спрайтов.
	 */
	public void putSpriteInfo(ByteBuffer buffer, int offset, int maxLevel, int width, int height, int stride) {
		for (int level = 0; level <= maxLevel; level++) {
			Std140Builder.intoBuffer(MemoryUtil.memSlice(buffer, offset + level * stride, stride))
				.putMat4f(new Matrix4f().ortho2D(0.0F, width >> level, 0.0F, height >> level))
				.putMat4f(
					new Matrix4f()
						.translate(x >> level, y >> level, 0.0F)
						.scale(
							contents.getWidth() + padding * 2 >> level,
							contents.getHeight() + padding * 2 >> level,
							1.0F
						)
				)
				.putFloat((float) padding / contents.getWidth())
				.putFloat((float) padding / contents.getHeight())
				.putInt(level);
		}
	}

	public void upload(GpuTexture texture, int mipmap) {
		contents.upload(texture, mipmap);
	}

	@Override
	public String toString() {
		return "TextureAtlasSprite{contents='" + contents + "', u0=" + minU + ", u1=" + maxU
			+ ", v0=" + minV + ", v1=" + maxV + "}";
	}

	@Override
	public void close() {
		contents.close();
	}
}

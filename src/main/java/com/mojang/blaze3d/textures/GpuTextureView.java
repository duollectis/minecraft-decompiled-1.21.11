package com.mojang.blaze3d.textures;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

/**
 * Представление (view) GPU-текстуры, описывающее диапазон уровней мипмапов.
 * Позволяет использовать подмножество мипмапов текстуры без создания нового GPU-объекта.
 * Обязательно закрывать после использования.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public abstract class GpuTextureView implements AutoCloseable {

	private final GpuTexture texture;
	private final int baseMipLevel;
	private final int mipLevels;

	public GpuTextureView(GpuTexture texture, int baseMipLevel, int mipLevels) {
		this.texture = texture;
		this.baseMipLevel = baseMipLevel;
		this.mipLevels = mipLevels;
	}

	@Override
	public abstract void close();

	public GpuTexture texture() {
		return texture;
	}

	public int baseMipLevel() {
		return baseMipLevel;
	}

	public int mipLevels() {
		return mipLevels;
	}

	/**
	 * Возвращает ширину текстуры на заданном уровне мипмапа относительно базового уровня этого view.
	 *
	 * @param mipLevel уровень мипмапа относительно {@link #baseMipLevel()}
	 */
	public int getWidth(int mipLevel) {
		return texture.getWidth(mipLevel + baseMipLevel);
	}

	/**
	 * Возвращает высоту текстуры на заданном уровне мипмапа относительно базового уровня этого view.
	 *
	 * @param mipLevel уровень мипмапа относительно {@link #baseMipLevel()}
	 */
	public int getHeight(int mipLevel) {
		return texture.getHeight(mipLevel + baseMipLevel);
	}

	public abstract boolean isClosed();
}

package com.mojang.blaze3d.textures;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Абстрактная GPU-текстура. Хранит изображение в памяти видеокарты и предоставляет
 * метаданные о формате, размерах и уровнях мипмапов.
 * Реализует {@link AutoCloseable} — текстуру обязательно закрывать после использования.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public abstract class GpuTexture implements AutoCloseable {

	/** Флаг: текстура является целью операций копирования. */
	public static final int USAGE_COPY_DST = 1;
	/** Флаг: текстура является источником операций копирования. */
	public static final int USAGE_COPY_SRC = 2;
	/** Флаг: текстура может быть привязана как сэмплер в шейдере. */
	public static final int USAGE_TEXTURE_BINDING = 4;
	/** Флаг: текстура используется как цель рендеринга (color/depth attachment). */
	public static final int USAGE_RENDER_ATTACHMENT = 8;
	/** Флаг: текстура совместима с кубической картой (cubemap). */
	public static final int USAGE_CUBEMAP_COMPATIBLE = 16;

	private final TextureFormat format;
	private final int width;
	private final int height;
	private final int depthOrLayers;
	private final int mipLevels;
	@Usage
	private final int usage;
	private final String label;

	public GpuTexture(
		@Usage int usage,
		String label,
		TextureFormat format,
		int width,
		int height,
		int depthOrLayers,
		int mipLevels
	) {
		this.usage = usage;
		this.label = label;
		this.format = format;
		this.width = width;
		this.height = height;
		this.depthOrLayers = depthOrLayers;
		this.mipLevels = mipLevels;
	}

	/**
	 * Возвращает ширину текстуры на заданном уровне мипмапа.
	 * Каждый уровень вдвое меньше предыдущего: {@code width >> mipLevel}.
	 */
	public int getWidth(int mipLevel) {
		return width >> mipLevel;
	}

	/**
	 * Возвращает высоту текстуры на заданном уровне мипмапа.
	 * Каждый уровень вдвое меньше предыдущего: {@code height >> mipLevel}.
	 */
	public int getHeight(int mipLevel) {
		return height >> mipLevel;
	}

	public int getDepthOrLayers() {
		return depthOrLayers;
	}

	public int getMipLevels() {
		return mipLevels;
	}

	public TextureFormat getFormat() {
		return format;
	}

	@Usage
	public int usage() {
		return usage;
	}

	public String getLabel() {
		return label;
	}

	@Override
	public abstract void close();

	public abstract boolean isClosed();

	/**
	 * Аннотация-маркер для параметров, полей и возвращаемых значений,
	 * обозначающая битовую маску флагов использования GPU-текстуры.
	 */
	@Retention(RetentionPolicy.CLASS)
	@Target({
		ElementType.FIELD,
		ElementType.PARAMETER,
		ElementType.LOCAL_VARIABLE,
		ElementType.METHOD,
		ElementType.TYPE_USE
	})
	@Environment(EnvType.CLIENT)
	public @interface Usage {
	}
}

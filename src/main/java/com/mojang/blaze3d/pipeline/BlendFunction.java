package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

/**
 * Описание функции смешивания цветов (alpha blending) для рендер-конвейера.
 * Хранит отдельные факторы источника и назначения для RGB и альфа-каналов.
 *
 * <p>Предоставляет набор готовых пресетов для наиболее распространённых режимов смешивания.
 *
 * @param sourceColor  фактор источника для RGB-каналов
 * @param destColor    фактор назначения для RGB-каналов
 * @param sourceAlpha  фактор источника для альфа-канала
 * @param destAlpha    фактор назначения для альфа-канала
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public record BlendFunction(
	SourceFactor sourceColor,
	DestFactor destColor,
	SourceFactor sourceAlpha,
	DestFactor destAlpha
) {

	/** Смешивание для эффектов молнии: {@code src_alpha * src + 1 * dst}. */
	public static final BlendFunction LIGHTNING = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE);

	/** Смешивание для блеска (glint): аддитивное по цвету, без изменения альфы. */
	public static final BlendFunction GLINT = new BlendFunction(
		SourceFactor.SRC_COLOR, DestFactor.ONE, SourceFactor.ZERO, DestFactor.ONE
	);

	/** Смешивание для наложения (overlay): полупрозрачный источник поверх непрозрачного фона. */
	public static final BlendFunction OVERLAY = new BlendFunction(
		SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO
	);

	/** Стандартное полупрозрачное смешивание с корректной альфой. */
	public static final BlendFunction TRANSLUCENT = new BlendFunction(
		SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
		SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA
	);

	/** Полупрозрачное смешивание с предумноженной альфой. */
	public static final BlendFunction TRANSLUCENT_PREMULTIPLIED_ALPHA = new BlendFunction(
		SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA,
		SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA
	);

	/** Аддитивное смешивание: {@code src + dst}. */
	public static final BlendFunction ADDITIVE = new BlendFunction(SourceFactor.ONE, DestFactor.ONE);

	/** Смешивание для блита контура сущности: прозрачный фон, непрозрачный контур. */
	public static final BlendFunction ENTITY_OUTLINE_BLIT = new BlendFunction(
		SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
		SourceFactor.ZERO, DestFactor.ONE
	);

	/** Инверсия цвета: {@code (1 - dst) * src + (1 - src) * dst}. */
	public static final BlendFunction INVERT = new BlendFunction(
		SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ONE_MINUS_SRC_COLOR,
		SourceFactor.ONE, DestFactor.ZERO
	);

	/** Создаёт функцию смешивания с одинаковыми факторами для RGB и альфа-каналов. */
	public BlendFunction(SourceFactor source, DestFactor dest) {
		this(source, dest, source, dest);
	}
}

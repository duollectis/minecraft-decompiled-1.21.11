package net.minecraft.client.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

/**
 * Интерфейс для сущностей, визуально похожих на игрока: локальный игрок,
 * другие игроки и манекены. Предоставляет доступ к скину, состоянию движения
 * и дополнительным визуальным параметрам (попугаи на плечах, уши deadmau5).
 */
@Environment(EnvType.CLIENT)
public interface ClientPlayerLikeEntity {

	/**
	 * Возвращает состояние движения и интерполяции позиции сущности.
	 *
	 * @return объект состояния
	 */
	ClientPlayerLikeState getState();

	/**
	 * Возвращает текстуры скина сущности.
	 *
	 * @return текстуры скина
	 */
	SkinTextures getSkin();

	/**
	 * Возвращает имя, отображаемое под манекеном (из скорборда или описания).
	 *
	 * @return текст имени или {@code null}
	 */
	@Nullable Text getMannequinName();

	/**
	 * Возвращает вариант попугая на плече.
	 *
	 * @param leftShoulder {@code true} для левого плеча, {@code false} для правого
	 * @return вариант попугая или {@code null} если попугая нет
	 */
	ParrotEntity.@Nullable Variant getShoulderParrotVariant(boolean leftShoulder);

	/**
	 * Проверяет, нужно ли рендерить дополнительные уши (пасхалка deadmau5).
	 *
	 * @return {@code true} если нужно рендерить уши
	 */
	boolean hasExtraEars();
}

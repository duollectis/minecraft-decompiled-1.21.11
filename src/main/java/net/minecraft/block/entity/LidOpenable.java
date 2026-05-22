package net.minecraft.block.entity;

/**
 * Интерфейс для блок-сущностей с анимируемой крышкой (сундук, шалкер-бокс и т.д.).
 * Возвращает прогресс анимации открытия крышки для рендерера.
 */
public interface LidOpenable {

	float getAnimationProgress(float tickProgress);
}

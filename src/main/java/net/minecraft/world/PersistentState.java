package net.minecraft.world;

/**
 * Базовый класс для всех персистентных состояний мира, сохраняемых на диск.
 * <p>
 * Подклассы реализуют сериализацию через {@code Codec} и регистрируются
 * в {@link PersistentStateManager}. Флаг {@code dirty} сигнализирует менеджеру
 * о необходимости записи состояния при следующем сохранении мира.
 */
public abstract class PersistentState {

	private boolean dirty;

	public void markDirty() {
		setDirty(true);
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public boolean isDirty() {
		return dirty;
	}
}

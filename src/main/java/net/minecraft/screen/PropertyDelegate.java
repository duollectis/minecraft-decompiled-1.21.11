package net.minecraft.screen;

/**
 * Контракт для хранилища целочисленных свойств экрана.
 * <p>
 * Абстрагирует доступ к массиву синхронизируемых числовых значений (прогресс, заряд и т.д.),
 * позволяя использовать как простые массивы ({@link ArrayPropertyDelegate}),
 * так и делегаты, привязанные к полям блок-сущностей.
 */
public interface PropertyDelegate {

	int get(int index);

	void set(int index, int value);

	int size();
}

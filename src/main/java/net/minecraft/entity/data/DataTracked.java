package net.minecraft.entity.data;

import java.util.List;

/**
 * Маркерный интерфейс для сущностей, поддерживающих синхронизацию данных через {@link DataTracker}.
 * Реализующий класс получает уведомления об изменении отслеживаемых полей.
 */
public interface DataTracked {

	void onTrackedDataSet(TrackedData<?> data);

	void onDataTrackerUpdate(List<DataTracker.SerializedEntry<?>> entries);
}

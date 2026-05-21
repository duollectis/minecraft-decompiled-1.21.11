package net.minecraft.entity.data;

import java.util.List;

/**
 * {@code DataTracked}.
 */
public interface DataTracked {

	void onTrackedDataSet(TrackedData<?> data);

	void onDataTrackerUpdate(List<DataTracker.SerializedEntry<?>> entries);
}

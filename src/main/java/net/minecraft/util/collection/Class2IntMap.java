package net.minecraft.util.collection;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.util.Util;

/**
 * {@code Class2IntMap}.
 */
public class Class2IntMap {

	public static final int MISSING = -1;
	private final Object2IntMap<Class<?>> backingMap = Util.make(
			new Object2IntOpenHashMap(), object2IntOpenHashMap -> object2IntOpenHashMap.defaultReturnValue(-1)
	);

	public int get(Class<?> clazz) {
		int i = this.backingMap.getInt(clazz);
		if (i != -1) {
			return i;
		}
		else {
			Class<?> class_ = clazz;

			while ((class_ = class_.getSuperclass()) != Object.class) {
				int j = this.backingMap.getInt(class_);
				if (j != -1) {
					return j;
				}
			}

			return -1;
		}
	}

	public int getNext(Class<?> clazz) {
		return this.get(clazz) + 1;
	}

	public int put(Class<?> clazz) {
		int i = this.get(clazz);
		int j = i == -1 ? 0 : i + 1;
		this.backingMap.put(clazz, j);
		return j;
	}
}

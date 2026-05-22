package net.minecraft.world;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Потокобезопасный (для одного потока) список сущностей с поддержкой
 * безопасной модификации во время итерации.
 * При попытке изменить коллекцию во время итерации создаётся копия,
 * чтобы избежать ConcurrentModificationException.
 */
public class EntityList {

	private Int2ObjectMap<Entity> entities = new Int2ObjectLinkedOpenHashMap<>();
	private Int2ObjectMap<Entity> temp = new Int2ObjectLinkedOpenHashMap<>();
	private @Nullable Int2ObjectMap<Entity> iterating;

	/**
	 * Если сейчас идёт итерация по основной карте, копирует её содержимое
	 * во временную и переключается на неё, чтобы модификации не ломали итератор.
	 */
	private void ensureSafe() {
		if (iterating != entities) {
			return;
		}

		temp.clear();

		for (Int2ObjectMap.Entry<Entity> entry : Int2ObjectMaps.fastIterable(entities)) {
			temp.put(entry.getIntKey(), entry.getValue());
		}

		Int2ObjectMap<Entity> old = entities;
		entities = temp;
		temp = old;
	}

	public void add(Entity entity) {
		ensureSafe();
		entities.put(entity.getId(), entity);
	}

	public void remove(Entity entity) {
		ensureSafe();
		entities.remove(entity.getId());
	}

	public boolean has(Entity entity) {
		return entities.containsKey(entity.getId());
	}

	/**
	 * Выполняет действие для каждой сущности в списке.
	 * Не допускает вложенных итераций — бросает исключение при попытке.
	 *
	 * @param action действие, применяемое к каждой сущности
	 * @throws UnsupportedOperationException при попытке вложенной итерации
	 */
	public void forEach(Consumer<Entity> action) {
		if (iterating != null) {
			throw new UnsupportedOperationException("Only one concurrent iteration supported");
		}

		iterating = entities;

		try {
			for (Entity entity : entities.values()) {
				action.accept(entity);
			}
		} finally {
			iterating = null;
		}
	}
}

package net.minecraft.stat;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Базовый обработчик статистики игрока.
 *
 * <p>Хранит потокобезопасную карту {@link Stat} → целое значение.
 * Серверная реализация {@link ServerStatHandler} расширяет этот класс,
 * добавляя персистентность и синхронизацию с клиентом.
 */
public class StatHandler {

	protected final Object2IntMap<Stat<?>> statMap = Object2IntMaps.synchronize(new Object2IntOpenHashMap<>());

	public StatHandler() {
		statMap.defaultReturnValue(0);
	}

	/**
	 * Увеличивает значение статистики на указанную величину.
	 * Результат ограничен сверху значением {@link Integer#MAX_VALUE}.
	 *
	 * @param player игрок, которому принадлежит статистика
	 * @param stat   статистика для увеличения
	 * @param delta  величина прироста
	 */
	public void increaseStat(PlayerEntity player, Stat<?> stat, int delta) {
		int newValue = (int) Math.min((long) getStat(stat) + delta, Integer.MAX_VALUE);
		setStat(player, stat, newValue);
	}

	/**
	 * Устанавливает точное значение статистики.
	 *
	 * @param player игрок, которому принадлежит статистика
	 * @param stat   статистика для изменения
	 * @param value  новое значение
	 */
	public void setStat(PlayerEntity player, Stat<?> stat, int value) {
		statMap.put(stat, value);
	}

	/**
	 * Возвращает значение статистики по типу и ключу реестра.
	 * Если статистика ещё не создана, возвращает 0.
	 *
	 * @param type тип статистики
	 * @param key  ключ значения в реестре типа
	 * @param <T>  тип значения
	 * @return текущее значение или 0
	 */
	public <T> int getStat(StatType<T> type, T key) {
		return type.hasStat(key) ? getStat(type.getOrCreateStat(key)) : 0;
	}

	public int getStat(Stat<?> stat) {
		return statMap.getInt(stat);
	}
}

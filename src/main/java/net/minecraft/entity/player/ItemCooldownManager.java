package net.minecraft.entity.player;

import com.google.common.collect.Maps;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.UseCooldownComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.Iterator;
import java.util.Map;

/**
 * Управляет кулдаунами предметов игрока, сгруппированными по идентификатору группы.
 * Кулдаун задаётся в тиках и отслеживается относительно внутреннего счётчика тиков.
 */
public class ItemCooldownManager {

	private final Map<Identifier, Entry> entries = Maps.newHashMap();
	private int tick;

	/**
	 * Проверяет, находится ли предмет в состоянии кулдауна.
	 *
	 * @param stack предмет для проверки
	 * @return {@code true}, если кулдаун ещё не истёк
	 */
	public boolean isCoolingDown(ItemStack stack) {
		return getCooldownProgress(stack, 0.0F) > 0.0F;
	}

	/**
	 * Возвращает прогресс кулдауна от 0.0 (кулдаун истёк) до 1.0 (только что установлен).
	 *
	 * @param stack        предмет
	 * @param tickProgress интерполяционный прогресс текущего тика (0.0–1.0)
	 * @return прогресс кулдауна в диапазоне [0.0, 1.0]
	 */
	public float getCooldownProgress(ItemStack stack, float tickProgress) {
		Identifier groupId = getGroup(stack);
		Entry entry = entries.get(groupId);

		if (entry == null) {
			return 0.0F;
		}

		float totalDuration = entry.endTick - entry.startTick;
		float remaining = entry.endTick - (tick + tickProgress);
		return MathHelper.clamp(remaining / totalDuration, 0.0F, 1.0F);
	}

	/**
	 * Продвигает внутренний счётчик тиков и удаляет истёкшие кулдауны.
	 */
	public void update() {
		tick++;

		if (entries.isEmpty()) {
			return;
		}

		Iterator<Map.Entry<Identifier, Entry>> iterator = entries.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<Identifier, Entry> entry = iterator.next();

			if (entry.getValue().endTick <= tick) {
				iterator.remove();
				onCooldownUpdate(entry.getKey());
			}
		}
	}

	/**
	 * Определяет группу кулдауна для предмета.
	 * Если у предмета есть компонент {@link UseCooldownComponent} с явной группой — используется она,
	 * иначе группой является идентификатор самого предмета.
	 *
	 * @param stack предмет
	 * @return идентификатор группы кулдауна
	 */
	public Identifier getGroup(ItemStack stack) {
		UseCooldownComponent cooldownComponent = stack.get(DataComponentTypes.USE_COOLDOWN);
		Identifier itemId = Registries.ITEM.getId(stack.getItem());
		return cooldownComponent == null
			? itemId
			: cooldownComponent.cooldownGroup().orElse(itemId);
	}

	/**
	 * Устанавливает кулдаун для группы предмета.
	 *
	 * @param stack    предмет
	 * @param duration длительность кулдауна в тиках
	 */
	public void set(ItemStack stack, int duration) {
		set(getGroup(stack), duration);
	}

	/**
	 * Устанавливает кулдаун для конкретной группы.
	 *
	 * @param groupId  идентификатор группы
	 * @param duration длительность кулдауна в тиках
	 */
	public void set(Identifier groupId, int duration) {
		entries.put(groupId, new Entry(tick, tick + duration));
		onCooldownUpdate(groupId, duration);
	}

	/**
	 * Принудительно снимает кулдаун с группы.
	 *
	 * @param groupId идентификатор группы
	 */
	public void remove(Identifier groupId) {
		entries.remove(groupId);
		onCooldownUpdate(groupId);
	}

	/**
	 * Вызывается при установке нового кулдауна. Переопределяется в подклассах
	 * для отправки пакетов клиенту.
	 *
	 * @param groupId  идентификатор группы
	 * @param duration длительность кулдауна в тиках
	 */
	protected void onCooldownUpdate(Identifier groupId, int duration) {
	}

	/**
	 * Вызывается при истечении или снятии кулдауна. Переопределяется в подклассах
	 * для отправки пакетов клиенту.
	 *
	 * @param groupId идентификатор группы
	 */
	protected void onCooldownUpdate(Identifier groupId) {
	}

	record Entry(int startTick, int endTick) {
	}
}

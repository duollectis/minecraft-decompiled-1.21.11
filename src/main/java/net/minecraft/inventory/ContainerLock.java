package net.minecraft.inventory;

import com.mojang.serialization.Codec;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;

/**
 * Замок контейнера, основанный на предикате предмета.
 * Контейнер считается открытым, если предмет в главной руке игрока
 * удовлетворяет предикату, либо если игрок находится в режиме наблюдателя.
 * Пустой замок ({@link #EMPTY}) пропускает всех игроков без ограничений.
 */
public record ContainerLock(ItemPredicate predicate) {

	public static final ContainerLock EMPTY = new ContainerLock(ItemPredicate.Builder.create().build());
	public static final Codec<ContainerLock> CODEC = ItemPredicate.CODEC.xmap(ContainerLock::new, ContainerLock::predicate);
	public static final String LOCK_KEY = "lock";

	public boolean canOpen(ItemStack stack) {
		return predicate.test(stack);
	}

	/**
	 * Проверяет, может ли игрок открыть контейнер.
	 * Наблюдатели всегда проходят проверку вне зависимости от предиката.
	 *
	 * @param player игрок, пытающийся открыть контейнер
	 * @return {@code true}, если доступ разрешён
	 */
	public boolean checkUnlocked(PlayerEntity player) {
		return player.isSpectator() || canOpen(player.getMainHandStack());
	}

	public void write(WriteView view) {
		if (this != EMPTY) {
			view.put(LOCK_KEY, CODEC, this);
		}
	}

	public static ContainerLock read(ReadView view) {
		return view.read(LOCK_KEY, CODEC).orElse(EMPTY);
	}
}

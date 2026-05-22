package net.minecraft.block.dispenser;

import net.minecraft.block.DispenserBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

/**
 * Поведение диспенсера для надеваемых предметов: экипирует предмет на живое существо
 * перед диспенсером. Если подходящего существа нет — выбрасывает предмет стандартно.
 */
public class EquippableDispenserBehavior extends ItemDispenserBehavior {

	public static final EquippableDispenserBehavior INSTANCE = new EquippableDispenserBehavior();

	@Override
	protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
		return tryEquip(pointer, stack) ? stack : super.dispenseSilently(pointer, stack);
	}

	/**
	 * Пытается надеть предмет на живое существо в блоке перед диспенсером.
	 * Выбирает первое подходящее существо, определяет слот экипировки и надевает предмет.
	 * Для моба дополнительно помечает слот как гарантированный дроп и делает его постоянным.
	 *
	 * @return {@code true}, если предмет успешно надет на существо
	 */
	public static boolean tryEquip(BlockPointer pointer, ItemStack stack) {
		BlockPos targetPos = pointer.pos().offset(pointer.state().get(DispenserBlock.FACING));
		List<LivingEntity> candidates = pointer.world().getEntitiesByClass(
				LivingEntity.class,
				new Box(targetPos),
				entity -> entity.canEquipFromDispenser(stack)
		);

		if (candidates.isEmpty()) {
			return false;
		}

		LivingEntity target = candidates.getFirst();
		EquipmentSlot slot = target.getPreferredEquipmentSlot(stack);
		target.equipStack(slot, stack.split(1));

		if (target instanceof MobEntity mob) {
			mob.setDropGuaranteed(slot);
			mob.setPersistent();
		}

		return true;
	}
}

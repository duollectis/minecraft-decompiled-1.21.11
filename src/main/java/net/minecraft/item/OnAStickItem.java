package net.minecraft.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemSteerable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Предмет «Морковка/грибок на удочке». Управляет верховым животным типа {@code T},
 * нанося урон удочке при каждом использовании ускорения.
 *
 * @param <T> тип управляемой сущности, реализующей {@link ItemSteerable}
 */
public class OnAStickItem<T extends Entity & ItemSteerable> extends Item {

	private final EntityType<T> target;
	private final int damagePerUse;

	public OnAStickItem(EntityType<T> target, int damagePerUse, Item.Settings settings) {
		super(settings);
		this.target = target;
		this.damagePerUse = damagePerUse;
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		if (world.isClient()) {
			return ActionResult.PASS;
		}

		ItemStack stack = user.getStackInHand(hand);
		Entity vehicle = user.getControllingVehicle();

		if (user.hasVehicle()
			&& vehicle instanceof ItemSteerable steerable
			&& vehicle.getType() == target
			&& steerable.consumeOnAStickItem()
		) {
			EquipmentSlot slot = hand.getEquipmentSlot();
			ItemStack damagedStack = stack.damage(damagePerUse, Items.FISHING_ROD, user, slot);
			return ActionResult.SUCCESS_SERVER.withNewHandStack(damagedStack);
		}

		user.incrementStat(Stats.USED.getOrCreateStat(this));

		return ActionResult.PASS;
	}
}

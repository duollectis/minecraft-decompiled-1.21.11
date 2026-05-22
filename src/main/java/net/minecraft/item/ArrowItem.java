package net.minecraft.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Предмет обычной стрелы. Реализует {@link ProjectileItem} для поддержки
 * выстрела из диспенсера, а также предоставляет фабричный метод для луков и арбалетов.
 */
public class ArrowItem extends Item implements ProjectileItem {

	public ArrowItem(Item.Settings settings) {
		super(settings);
	}

	/**
	 * Создаёт сущность стрелы для выстрела из лука или арбалета.
	 *
	 * @param world    мир, в котором создаётся стрела
	 * @param stack    стек предмета стрелы (копируется с количеством 1)
	 * @param shooter  сущность, производящая выстрел
	 * @param shotFrom стек оружия, из которого произведён выстрел (может быть {@code null})
	 * @return новая сущность {@link PersistentProjectileEntity}
	 */
	public PersistentProjectileEntity createArrow(
		World world,
		ItemStack stack,
		LivingEntity shooter,
		@Nullable ItemStack shotFrom
	) {
		return new ArrowEntity(world, shooter, stack.copyWithCount(1), shotFrom);
	}

	@Override
	public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
		ArrowEntity arrow = new ArrowEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack.copyWithCount(1), null);
		arrow.pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
		return arrow;
	}
}

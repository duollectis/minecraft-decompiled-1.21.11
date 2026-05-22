package net.minecraft.entity.projectile.thrown;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.world.World;

/**
 * Базовый класс для бросаемых снарядов, имеющих визуальное представление предмета.
 * <p>
 * Хранит {@link ItemStack} в {@link DataTracker} для синхронизации с клиентом.
 * Подклассы обязаны реализовать {@link #getDefaultItem()} для инициализации
 * дефолтного стека при создании через конструктор типа.
 */
public abstract class ThrownItemEntity extends ThrownEntity implements FlyingItemEntity {

	private static final TrackedData<ItemStack> ITEM =
			DataTracker.registerData(ThrownItemEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);

	public ThrownItemEntity(EntityType<? extends ThrownItemEntity> entityType, World world) {
		super(entityType, world);
	}

	public ThrownItemEntity(
			EntityType<? extends ThrownItemEntity> type,
			double x,
			double y,
			double z,
			World world,
			ItemStack stack
	) {
		super(type, x, y, z, world);
		setItem(stack);
	}

	public ThrownItemEntity(
			EntityType<? extends ThrownItemEntity> type,
			LivingEntity owner,
			World world,
			ItemStack stack
	) {
		this(type, owner.getX(), owner.getEyeY() - 0.1F, owner.getZ(), world, stack);
		setOwner(owner);
	}

	public void setItem(ItemStack stack) {
		getDataTracker().set(ITEM, stack.copyWithCount(1));
	}

	protected abstract Item getDefaultItem();

	@Override
	public ItemStack getStack() {
		return getDataTracker().get(ITEM);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(ITEM, new ItemStack(getDefaultItem()));
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.put("Item", ItemStack.CODEC, getStack());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setItem(view.<ItemStack>read("Item", ItemStack.CODEC)
				.orElseGet(() -> new ItemStack(getDefaultItem())));
	}
}

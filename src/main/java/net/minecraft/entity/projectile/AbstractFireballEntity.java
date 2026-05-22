package net.minecraft.entity.projectile;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Базовый класс для огненных шаров, хранящих визуальный предмет.
 * <p>
 * Синхронизирует стек предмета через {@link DataTracker} для отображения на клиенте.
 * По умолчанию использует {@link Items#FIRE_CHARGE} как визуальный предмет.
 * Подавляет звук тушения огня — огненные шары не тушатся водой визуально.
 */
public abstract class AbstractFireballEntity extends ExplosiveProjectileEntity implements FlyingItemEntity {

	/** Квадрат минимального расстояния рендера для только что заспавненных шаров (3.5 блока). */
	private static final float MIN_RENDER_DISTANCE_SQUARED_NEW = 12.25F;

	private static final TrackedData<ItemStack> ITEM =
		DataTracker.registerData(AbstractFireballEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);

	public AbstractFireballEntity(EntityType<? extends AbstractFireballEntity> entityType, World world) {
		super(entityType, world);
	}

	public AbstractFireballEntity(
		EntityType<? extends AbstractFireballEntity> entityType,
		double x,
		double y,
		double z,
		Vec3d velocity,
		World world
	) {
		super(entityType, x, y, z, velocity, world);
	}

	public AbstractFireballEntity(
		EntityType<? extends AbstractFireballEntity> entityType,
		LivingEntity owner,
		Vec3d velocity,
		World world
	) {
		super(entityType, owner, velocity, world);
	}

	/**
	 * Устанавливает визуальный предмет огненного шара.
	 * Если стек пустой — восстанавливает предмет по умолчанию.
	 *
	 * @param stack новый стек предмета
	 */
	public void setItem(ItemStack stack) {
		if (stack.isEmpty()) {
			getDataTracker().set(ITEM, getDefaultFireballItem());
		} else {
			getDataTracker().set(ITEM, stack.copyWithCount(1));
		}
	}

	@Override
	protected void playExtinguishSound() {
	}

	@Override
	public ItemStack getStack() {
		return getDataTracker().get(ITEM);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(ITEM, getDefaultFireballItem());
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.put("Item", ItemStack.CODEC, getStack());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setItem(view.<ItemStack>read("Item", ItemStack.CODEC).orElse(getDefaultFireballItem()));
	}

	private ItemStack getDefaultFireballItem() {
		return new ItemStack(Items.FIRE_CHARGE);
	}

	@Override
	public @Nullable StackReference getStackReference(int slot) {
		return slot == 0 ? StackReference.of(this::getStack, this::setItem) : super.getStackReference(slot);
	}

	/**
	 * Скрывает огненный шар в первые 2 тика, если камера слишком близко.
	 * Предотвращает мерцание при спавне прямо перед игроком.
	 */
	@Override
	public boolean shouldRender(double distance) {
		return age < 2 && distance < MIN_RENDER_DISTANCE_SQUARED_NEW
			? false
			: super.shouldRender(distance);
	}
}

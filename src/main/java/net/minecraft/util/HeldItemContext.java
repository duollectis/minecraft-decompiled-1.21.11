package net.minecraft.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Контекст предмета, удерживаемого в руке.
 * Предоставляет информацию о мире, позиции и ориентации держателя предмета.
 * Используется в системе рендеринга предметов и эффектов.
 */
public interface HeldItemContext {

	World getEntityWorld();

	Vec3d getEntityPos();

	float getBodyYaw();

	default @Nullable LivingEntity getEntity() {
		return null;
	}

	/**
	 * Создаёт контекст со смещённой позицией относительно исходного контекста.
	 *
	 * @param context исходный контекст
	 * @param offset  вектор смещения позиции
	 * @return новый контекст с применённым смещением
	 */
	static HeldItemContext offseted(HeldItemContext context, Vec3d offset) {
		return new HeldItemContext.Offset(context, offset);
	}

	/**
	 * Контекст со смещённой позицией.
	 *
	 * @param owner  исходный контекст-владелец
	 * @param offset вектор смещения
	 */
	record Offset(HeldItemContext owner, Vec3d offset) implements HeldItemContext {

		@Override
		public World getEntityWorld() {
			return owner.getEntityWorld();
		}

		@Override
		public Vec3d getEntityPos() {
			return owner.getEntityPos().add(offset);
		}

		@Override
		public float getBodyYaw() {
			return owner.getBodyYaw();
		}

		@Override
		public @Nullable LivingEntity getEntity() {
			return owner.getEntity();
		}
	}
}

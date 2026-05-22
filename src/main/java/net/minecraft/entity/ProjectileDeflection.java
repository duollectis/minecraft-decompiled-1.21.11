package net.minecraft.entity;

import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

/**
 * Стратегия отклонения снаряда при попадании в сущность.
 * Определяет, как изменяется скорость и направление снаряда после столкновения.
 */
@FunctionalInterface
public interface ProjectileDeflection {

	/** Снаряд не отклоняется. */
	ProjectileDeflection NONE = (projectile, hitEntity, random) -> {};

	/** Снаряд отражается назад с небольшим случайным поворотом по оси Y. */
	ProjectileDeflection SIMPLE = (projectile, hitEntity, random) -> {
		float deflectionAngle = 170.0F + random.nextFloat() * 20.0F;
		projectile.setVelocity(projectile.getVelocity().multiply(-0.5));
		projectile.setYaw(projectile.getYaw() + deflectionAngle);
		projectile.lastYaw += deflectionAngle;
		projectile.velocityDirty = true;
	};

	/** Снаряд перенаправляется в сторону взгляда сущности, которая его отклонила. */
	ProjectileDeflection REDIRECTED = (projectile, hitEntity, random) -> {
		if (hitEntity == null) {
			return;
		}

		projectile.setVelocity(hitEntity.getRotationVector());
		projectile.velocityDirty = true;
	};

	/** Снаряд получает направление скорости сущности, которая его отклонила. */
	ProjectileDeflection TRANSFER_VELOCITY_DIRECTION = (projectile, hitEntity, random) -> {
		if (hitEntity == null) {
			return;
		}

		projectile.setVelocity(hitEntity.getVelocity().normalize());
		projectile.velocityDirty = true;
	};

	void deflect(ProjectileEntity projectile, @Nullable Entity hitEntity, Random random);
}

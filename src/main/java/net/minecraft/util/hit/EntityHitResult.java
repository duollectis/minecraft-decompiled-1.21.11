package net.minecraft.util.hit;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/** Результат трассировки луча, попавшего в сущность. */
public class EntityHitResult extends HitResult {

	private final Entity entity;

	public EntityHitResult(Entity entity) {
		this(entity, entity.getEntityPos());
	}

	public EntityHitResult(Entity entity, Vec3d pos) {
		super(pos);
		this.entity = entity;
	}

	public Entity getEntity() {
		return entity;
	}

	@Override
	public Type getType() {
		return Type.ENTITY;
	}
}

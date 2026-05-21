package net.minecraft.entity;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * {@code Tameable}.
 */
public interface Tameable {

	@Nullable LazyEntityReference<LivingEntity> getOwnerReference();

	World getEntityWorld();

	default @Nullable LivingEntity getOwner() {
		return LazyEntityReference.getLivingEntity(this.getOwnerReference(), this.getEntityWorld());
	}

	default @Nullable LivingEntity getTopLevelOwner() {
		Set<Object> set = new ObjectArraySet();
		LivingEntity livingEntity = this.getOwner();
		set.add(this);

		while (livingEntity instanceof Tameable) {
			Tameable tameable = (Tameable) livingEntity;
			LivingEntity livingEntity2 = tameable.getOwner();
			if (set.contains(livingEntity2)) {
				return null;
			}

			set.add(livingEntity);
			livingEntity = tameable.getOwner();
		}

		return livingEntity;
	}
}

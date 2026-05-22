package net.minecraft.entity;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Интерфейс для прирученных сущностей, имеющих владельца.
 */
public interface Tameable {

	@Nullable LazyEntityReference<LivingEntity> getOwnerReference();

	World getEntityWorld();

	default @Nullable LivingEntity getOwner() {
		return LazyEntityReference.getLivingEntity(getOwnerReference(), getEntityWorld());
	}

	/**
	 * Возвращает самого верхнего владельца в цепочке приручения.
	 * Защищает от циклических ссылок: если цепочка зацикливается — возвращает {@code null}.
	 */
	default @Nullable LivingEntity getTopLevelOwner() {
		Set<Object> visited = new ObjectArraySet<>();
		LivingEntity owner = getOwner();
		visited.add(this);

		while (owner instanceof Tameable tameable) {
			LivingEntity nextOwner = tameable.getOwner();

			if (visited.contains(nextOwner)) {
				return null;
			}

			visited.add(owner);
			owner = nextOwner;
		}

		return owner;
	}
}

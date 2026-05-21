package net.minecraft.predicate.component;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.Map;
import java.util.function.Predicate;

/**
 * {@code ComponentsPredicate}.
 */
public record ComponentsPredicate(
		ComponentMapPredicate exact,
		Map<ComponentPredicate.Type<?>, ComponentPredicate> partial
)
		implements Predicate<ComponentsAccess> {

	public static final ComponentsPredicate EMPTY = new ComponentsPredicate(ComponentMapPredicate.EMPTY, Map.of());
	public static final MapCodec<ComponentsPredicate> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    ComponentMapPredicate.CODEC
							                    .optionalFieldOf("components", ComponentMapPredicate.EMPTY)
							                    .forGetter(ComponentsPredicate::exact),
					                    ComponentPredicate.PREDICATES_MAP_CODEC
							                    .optionalFieldOf("predicates", Map.of())
							                    .forGetter(ComponentsPredicate::partial)
			                    )
			                    .apply(instance, ComponentsPredicate::new)
	);
	public static final PacketCodec<RegistryByteBuf, ComponentsPredicate> PACKET_CODEC = PacketCodec.tuple(
			ComponentMapPredicate.PACKET_CODEC,
			ComponentsPredicate::exact,
			ComponentPredicate.PREDICATES_MAP_PACKET_CODEC,
			ComponentsPredicate::partial,
			ComponentsPredicate::new
	);

	public boolean test(ComponentsAccess componentsAccess) {
		if (!this.exact.test(componentsAccess)) {
			return false;
		}
		else {
			for (ComponentPredicate componentPredicate : this.partial.values()) {
				if (!componentPredicate.test(componentsAccess)) {
					return false;
				}
			}

			return true;
		}
	}

	public boolean isEmpty() {
		return this.exact.isEmpty() && this.partial.isEmpty();
	}

	/**
	 * {@code Builder}.
	 */
	public static class Builder {

		private ComponentMapPredicate exact = ComponentMapPredicate.EMPTY;
		private final com.google.common.collect.ImmutableMap.Builder<ComponentPredicate.Type<?>, ComponentPredicate>
				partial =
				ImmutableMap.builder();

		private Builder() {
		}

		public static ComponentsPredicate.Builder create() {
			return new ComponentsPredicate.Builder();
		}

		public <T extends ComponentType<?>> ComponentsPredicate.Builder has(ComponentType<?> type) {
			ComponentPredicate.OfExistence ofExistence = ComponentPredicate.OfExistence.toPredicateType(type);
			this.partial.put(ofExistence, ofExistence.getPredicate());
			return this;
		}

		public <T extends ComponentPredicate> ComponentsPredicate.Builder partial(
				ComponentPredicate.Type<T> type,
				T predicate
		) {
			this.partial.put(type, predicate);
			return this;
		}

		public ComponentsPredicate.Builder exact(ComponentMapPredicate exact) {
			this.exact = exact;
			return this;
		}

		public ComponentsPredicate build() {
			return new ComponentsPredicate(this.exact, this.partial.buildOrThrow());
		}
	}
}

package net.minecraft.client.render.model.json;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.state.State;
import net.minecraft.state.StateManager;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Util;

import java.util.List;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
/**
 * {@code MultipartModelCombinedCondition}.
 */
public record MultipartModelCombinedCondition(
		MultipartModelCombinedCondition.LogicalOperator operation,
		List<MultipartModelCondition> terms
)
		implements MultipartModelCondition {

	@Override
	public <O, S extends State<O, S>> Predicate<S> instantiate(StateManager<O, S> stateManager) {
		return this.operation.apply(Lists.transform(this.terms, condition -> condition.instantiate(stateManager)));
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code LogicalOperator}.
	 */
	public static enum LogicalOperator implements StringIdentifiable {
		AND("AND") {
			@Override
			public <V> Predicate<V> apply(List<Predicate<V>> conditions) {
				return Util.allOf(conditions);
			}
		},
		OR("OR") {
			@Override
			public <V> Predicate<V> apply(List<Predicate<V>> conditions) {
				return Util.anyOf(conditions);
			}
		};

		public static final Codec<MultipartModelCombinedCondition.LogicalOperator>
				CODEC =
				StringIdentifiable.createCodec(
						MultipartModelCombinedCondition.LogicalOperator::values
				);
		private final String name;

		LogicalOperator(final String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return this.name;
		}

		public abstract <V> Predicate<V> apply(List<Predicate<V>> conditions);
	}
}

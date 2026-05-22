package net.minecraft.loot.context;

import com.google.common.collect.Maps;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameter;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.context.ContextType;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

/** Контекст мира для таблиц лута: параметры, динамические дропы и удача. */
public class LootWorldContext {

	private final ServerWorld world;
	private final ContextParameterMap parameters;
	private final Map<Identifier, LootWorldContext.DynamicDrop> dynamicDrops;
	private final float luck;

	public LootWorldContext(
		ServerWorld world,
		ContextParameterMap parameters,
		Map<Identifier, LootWorldContext.DynamicDrop> dynamicDrops,
		float luck
	) {
		this.world = world;
		this.parameters = parameters;
		this.dynamicDrops = dynamicDrops;
		this.luck = luck;
	}

	public ServerWorld getWorld() {
		return world;
	}

	public ContextParameterMap getParameters() {
		return parameters;
	}

	public void addDynamicDrops(Identifier id, Consumer<ItemStack> lootConsumer) {
		LootWorldContext.DynamicDrop dynamicDrop = dynamicDrops.get(id);

		if (dynamicDrop != null) {
			dynamicDrop.add(lootConsumer);
		}
	}

	public float getLuck() {
		return luck;
	}

	/** Строитель контекста мира с поддержкой параметров и динамических дропов. */
	public static class Builder {

		private final ServerWorld world;
		private final ContextParameterMap.Builder parameters = new ContextParameterMap.Builder();
		private final Map<Identifier, LootWorldContext.DynamicDrop> dynamicDrops = Maps.newHashMap();
		private float luck;

		public Builder(ServerWorld world) {
			this.world = world;
		}

		public ServerWorld getWorld() {
			return world;
		}

		public <T> LootWorldContext.Builder add(ContextParameter<T> parameter, T value) {
			parameters.add(parameter, value);
			return this;
		}

		public <T> LootWorldContext.Builder addOptional(ContextParameter<T> parameter, @Nullable T value) {
			parameters.addNullable(parameter, value);
			return this;
		}

		public <T> T get(ContextParameter<T> parameter) {
			return parameters.getOrThrow(parameter);
		}

		public <T> @Nullable T getOptional(ContextParameter<T> parameter) {
			return parameters.getNullable(parameter);
		}

		public LootWorldContext.Builder addDynamicDrop(Identifier id, LootWorldContext.DynamicDrop dynamicDrop) {
			LootWorldContext.DynamicDrop existing = dynamicDrops.put(id, dynamicDrop);

			if (existing != null) {
				throw new IllegalStateException("Duplicated dynamic drop '" + dynamicDrops + "'");
			}

			return this;
		}

		public LootWorldContext.Builder luck(float luck) {
			this.luck = luck;
			return this;
		}

		public LootWorldContext build(ContextType contextType) {
			ContextParameterMap builtParameters = parameters.build(contextType);
			return new LootWorldContext(world, builtParameters, dynamicDrops, luck);
		}
	}

	/** Функциональный интерфейс для динамических дропов, добавляемых в контекст. */
	@FunctionalInterface
	public interface DynamicDrop {

		void add(Consumer<ItemStack> lootConsumer);
	}
}

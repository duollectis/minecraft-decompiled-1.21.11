package net.minecraft.advancement;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.LazyContainer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * Описывает награды за выполнение достижения: опыт, лут-таблицы, рецепты и функцию.
 */
public record AdvancementRewards(
	int experience,
	List<RegistryKey<LootTable>> loot,
	List<RegistryKey<Recipe<?>>> recipes,
	Optional<LazyContainer> function
) {

	public static final Codec<AdvancementRewards> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			Codec.INT.optionalFieldOf("experience", 0).forGetter(AdvancementRewards::experience),
			LootTable.TABLE_KEY.listOf().optionalFieldOf("loot", List.of()).forGetter(AdvancementRewards::loot),
			Recipe.KEY_CODEC.listOf().optionalFieldOf("recipes", List.of()).forGetter(AdvancementRewards::recipes),
			LazyContainer.CODEC.optionalFieldOf("function").forGetter(AdvancementRewards::function)
		).apply(instance, AdvancementRewards::new)
	);

	public static final AdvancementRewards NONE = new AdvancementRewards(0, List.of(), List.of(), Optional.empty());

	/**
	 * Выдаёт все награды игроку: опыт, предметы из лут-таблиц, рецепты и выполняет функцию.
	 */
	public void apply(ServerPlayerEntity player) {
		player.addExperience(experience);

		ServerWorld world = player.getEntityWorld();
		MinecraftServer server = world.getServer();

		LootWorldContext lootContext = new LootWorldContext.Builder(world)
			.add(LootContextParameters.THIS_ENTITY, player)
			.add(LootContextParameters.ORIGIN, player.getEntityPos())
			.build(LootContextTypes.ADVANCEMENT_REWARD);

		boolean inventoryUpdated = false;

		for (RegistryKey<LootTable> lootKey : loot) {
			List<ItemStack> drops = server.getReloadableRegistries()
			                             .getLootTable(lootKey)
			                             .generateLoot(lootContext);

			for (ItemStack stack : drops) {
				if (player.giveItemStack(stack)) {
					world.playSound(
						null,
						player.getX(), player.getY(), player.getZ(),
						SoundEvents.ENTITY_ITEM_PICKUP,
						SoundCategory.PLAYERS,
						0.2F,
						((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
					);
					inventoryUpdated = true;
				} else {
					ItemEntity dropped = player.dropItem(stack, false);
					if (dropped != null) {
						dropped.resetPickupDelay();
						dropped.setOwner(player.getUuid());
					}
				}
			}
		}

		if (inventoryUpdated) {
			player.currentScreenHandler.sendContentUpdates();
		}

		if (!recipes.isEmpty()) {
			player.unlockRecipes(recipes);
		}

		function
			.flatMap(fn -> fn.get(server.getCommandFunctionManager()))
			.ifPresent(fn -> server.getCommandFunctionManager().execute(
				(CommandFunction<ServerCommandSource>) fn,
				player.getCommandSource()
				      .withSilent()
				      .withPermissions(LeveledPermissionPredicate.GAMEMASTERS)
			));
	}

	/**
	 * Строитель для создания наград достижения с fluent API.
	 */
	public static class Builder {

		private int experience;
		private final ImmutableList.Builder<RegistryKey<LootTable>> loot = ImmutableList.builder();
		private final ImmutableList.Builder<RegistryKey<Recipe<?>>> recipes = ImmutableList.builder();
		private Optional<Identifier> function = Optional.empty();

		public static Builder experience(int experience) {
			return new Builder().setExperience(experience);
		}

		public Builder setExperience(int experience) {
			this.experience += experience;
			return this;
		}

		public static Builder loot(RegistryKey<LootTable> loot) {
			return new Builder().addLoot(loot);
		}

		public Builder addLoot(RegistryKey<LootTable> loot) {
			this.loot.add(loot);
			return this;
		}

		public static Builder recipe(RegistryKey<Recipe<?>> recipeKey) {
			return new Builder().addRecipe(recipeKey);
		}

		public Builder addRecipe(RegistryKey<Recipe<?>> recipeKey) {
			recipes.add(recipeKey);
			return this;
		}

		public static Builder function(Identifier function) {
			return new Builder().setFunction(function);
		}

		public Builder setFunction(Identifier function) {
			this.function = Optional.of(function);
			return this;
		}

		public AdvancementRewards build() {
			return new AdvancementRewards(
				experience,
				loot.build(),
				recipes.build(),
				function.map(LazyContainer::new)
			);
		}
	}
}

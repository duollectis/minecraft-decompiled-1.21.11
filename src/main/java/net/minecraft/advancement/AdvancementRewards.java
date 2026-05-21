package net.minecraft.advancement;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
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
 * {@code AdvancementRewards}.
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
					                    Recipe.KEY_CODEC
							                    .listOf()
							                    .optionalFieldOf("recipes", List.of())
							                    .forGetter(AdvancementRewards::recipes),
					                    LazyContainer.CODEC.optionalFieldOf("function").forGetter(AdvancementRewards::function)
			                    )
			                    .apply(instance, AdvancementRewards::new)
	);
	public static final AdvancementRewards NONE = new AdvancementRewards(0, List.of(), List.of(), Optional.empty());

	public void apply(ServerPlayerEntity player) {
		player.addExperience(this.experience);
		ServerWorld serverWorld = player.getEntityWorld();
		MinecraftServer minecraftServer = serverWorld.getServer();
		LootWorldContext lootWorldContext = new LootWorldContext.Builder(serverWorld)
				.add(LootContextParameters.THIS_ENTITY, player)
				.add(LootContextParameters.ORIGIN, player.getEntityPos())
				.build(LootContextTypes.ADVANCEMENT_REWARD);
		boolean bl = false;

		for (RegistryKey<LootTable> registryKey : this.loot) {
			ObjectListIterator
					var8 =
					minecraftServer
							.getReloadableRegistries()
							.getLootTable(registryKey)
							.generateLoot(lootWorldContext)
							.iterator();

			while (var8.hasNext()) {
				ItemStack itemStack = (ItemStack) var8.next();
				if (player.giveItemStack(itemStack)) {
					serverWorld.playSound(
							null,
							player.getX(),
							player.getY(),
							player.getZ(),
							SoundEvents.ENTITY_ITEM_PICKUP,
							SoundCategory.PLAYERS,
							0.2F,
							((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
					);
					bl = true;
				}
				else {
					ItemEntity itemEntity = player.dropItem(itemStack, false);
					if (itemEntity != null) {
						itemEntity.resetPickupDelay();
						itemEntity.setOwner(player.getUuid());
					}
				}
			}
		}

		if (bl) {
			player.currentScreenHandler.sendContentUpdates();
		}

		if (!this.recipes.isEmpty()) {
			player.unlockRecipes(this.recipes);
		}

		this.function
				.flatMap(function -> function.get(minecraftServer.getCommandFunctionManager()))
				.ifPresent(
						function -> minecraftServer.getCommandFunctionManager()
						                           .execute(
								                           (CommandFunction<ServerCommandSource>) function,
								                           player
										                           .getCommandSource()
										                           .withSilent()
										                           .withPermissions(LeveledPermissionPredicate.GAMEMASTERS)
						                           )
				);
	}

	/**
	 * {@code Builder}.
	 */
	public static class Builder {

		private int experience;
		private final com.google.common.collect.ImmutableList.Builder<RegistryKey<LootTable>>
				loot =
				ImmutableList.builder();
		private final com.google.common.collect.ImmutableList.Builder<RegistryKey<Recipe<?>>>
				recipes =
				ImmutableList.builder();
		private Optional<Identifier> function = Optional.empty();

		public static AdvancementRewards.Builder experience(int experience) {
			return new AdvancementRewards.Builder().setExperience(experience);
		}

		public AdvancementRewards.Builder setExperience(int experience) {
			this.experience += experience;
			return this;
		}

		public static AdvancementRewards.Builder loot(RegistryKey<LootTable> loot) {
			return new AdvancementRewards.Builder().addLoot(loot);
		}

		public AdvancementRewards.Builder addLoot(RegistryKey<LootTable> loot) {
			this.loot.add(loot);
			return this;
		}

		public static AdvancementRewards.Builder recipe(RegistryKey<Recipe<?>> recipeKey) {
			return new AdvancementRewards.Builder().addRecipe(recipeKey);
		}

		public AdvancementRewards.Builder addRecipe(RegistryKey<Recipe<?>> recipeKey) {
			this.recipes.add(recipeKey);
			return this;
		}

		public static AdvancementRewards.Builder function(Identifier function) {
			return new AdvancementRewards.Builder().setFunction(function);
		}

		public AdvancementRewards.Builder setFunction(Identifier function) {
			this.function = Optional.of(function);
			return this;
		}

		public AdvancementRewards build() {
			return new AdvancementRewards(
					this.experience,
					this.loot.build(),
					this.recipes.build(),
					this.function.map(LazyContainer::new)
			);
		}
	}
}

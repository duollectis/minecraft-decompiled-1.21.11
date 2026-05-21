package net.minecraft.server.network;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.network.packet.s2c.play.RecipeBookAddS2CPacket;
import net.minecraft.network.packet.s2c.play.RecipeBookRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.RecipeBookSettingsS2CPacket;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.book.RecipeBook;
import net.minecraft.recipe.book.RecipeBookOptions;
import net.minecraft.registry.RegistryKey;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * {@code ServerRecipeBook}.
 */
public class ServerRecipeBook extends RecipeBook {

	public static final String RECIPE_BOOK_KEY = "recipeBook";
	private static final Logger LOGGER = LogUtils.getLogger();
	private final ServerRecipeBook.DisplayCollector collector;
	@VisibleForTesting
	protected final Set<RegistryKey<Recipe<?>>> unlocked = Sets.newIdentityHashSet();
	@VisibleForTesting
	protected final Set<RegistryKey<Recipe<?>>> highlighted = Sets.newIdentityHashSet();

	public ServerRecipeBook(ServerRecipeBook.DisplayCollector collector) {
		this.collector = collector;
	}

	public void unlock(RegistryKey<Recipe<?>> recipeKey) {
		this.unlocked.add(recipeKey);
	}

	public boolean isUnlocked(RegistryKey<Recipe<?>> recipeKey) {
		return this.unlocked.contains(recipeKey);
	}

	public void lock(RegistryKey<Recipe<?>> recipeKey) {
		this.unlocked.remove(recipeKey);
		this.highlighted.remove(recipeKey);
	}

	public void unmarkHighlighted(RegistryKey<Recipe<?>> recipeKey) {
		this.highlighted.remove(recipeKey);
	}

	private void markHighlighted(RegistryKey<Recipe<?>> recipeKey) {
		this.highlighted.add(recipeKey);
	}

	public int unlockRecipes(Collection<RecipeEntry<?>> recipes, ServerPlayerEntity player) {
		List<RecipeBookAddS2CPacket.Entry> list = new ArrayList<>();

		for (RecipeEntry<?> recipeEntry : recipes) {
			RegistryKey<Recipe<?>> registryKey = recipeEntry.id();
			if (!this.unlocked.contains(registryKey) && !recipeEntry.value().isIgnoredInRecipeBook()) {
				this.unlock(registryKey);
				this.markHighlighted(registryKey);
				this.collector
						.displaysForRecipe(
								registryKey,
								display -> list.add(new RecipeBookAddS2CPacket.Entry(
										display,
										recipeEntry.value().showNotification(),
										true
								))
						);
				Criteria.RECIPE_UNLOCKED.trigger(player, recipeEntry);
			}
		}

		if (!list.isEmpty()) {
			player.networkHandler.sendPacket(new RecipeBookAddS2CPacket(list, false));
		}

		return list.size();
	}

	public int lockRecipes(Collection<RecipeEntry<?>> recipes, ServerPlayerEntity player) {
		List<NetworkRecipeId> list = Lists.newArrayList();

		for (RecipeEntry<?> recipeEntry : recipes) {
			RegistryKey<Recipe<?>> registryKey = recipeEntry.id();
			if (this.unlocked.contains(registryKey)) {
				this.lock(registryKey);
				this.collector.displaysForRecipe(registryKey, display -> list.add(display.id()));
			}
		}

		if (!list.isEmpty()) {
			player.networkHandler.sendPacket(new RecipeBookRemoveS2CPacket(list));
		}

		return list.size();
	}

	private void handleList(
			List<RegistryKey<Recipe<?>>> recipes,
			Consumer<RegistryKey<Recipe<?>>> handler,
			Predicate<RegistryKey<Recipe<?>>> validPredicate
	) {
		for (RegistryKey<Recipe<?>> registryKey : recipes) {
			if (!validPredicate.test(registryKey)) {
				LOGGER.error("Tried to load unrecognized recipe: {} removed now.", registryKey);
			}
			else {
				handler.accept(registryKey);
			}
		}
	}

	public void sendInitRecipesPacket(ServerPlayerEntity player) {
		player.networkHandler.sendPacket(new RecipeBookSettingsS2CPacket(this.getOptions().copy()));
		List<RecipeBookAddS2CPacket.Entry> list = new ArrayList<>(this.unlocked.size());

		for (RegistryKey<Recipe<?>> registryKey : this.unlocked) {
			this.collector
					.displaysForRecipe(
							registryKey,
							display -> list.add(new RecipeBookAddS2CPacket.Entry(
									display,
									false,
									this.highlighted.contains(registryKey)
							))
					);
		}

		player.networkHandler.sendPacket(new RecipeBookAddS2CPacket(list, true));
	}

	public void copyFrom(ServerRecipeBook recipeBook) {
		this.unpack(recipeBook.pack());
	}

	public ServerRecipeBook.Packed pack() {
		return new ServerRecipeBook.Packed(
				this.options.copy(),
				List.copyOf(this.unlocked),
				List.copyOf(this.highlighted)
		);
	}

	private void unpack(ServerRecipeBook.Packed packed) {
		this.unlocked.clear();
		this.highlighted.clear();
		this.options.copyFrom(packed.settings);
		this.unlocked.addAll(packed.known);
		this.highlighted.addAll(packed.highlight);
	}

	public void unpack(ServerRecipeBook.Packed packed, Predicate<RegistryKey<Recipe<?>>> validPredicate) {
		this.options.copyFrom(packed.settings);
		this.handleList(packed.known, this.unlocked::add, validPredicate);
		this.handleList(packed.highlight, this.highlighted::add, validPredicate);
	}

	@FunctionalInterface
	/**
	 * {@code DisplayCollector}.
	 */
	public interface DisplayCollector {

		void displaysForRecipe(RegistryKey<Recipe<?>> recipeKey, Consumer<RecipeDisplayEntry> adder);
	}

	/**
	 * {@code Packed}.
	 */
	public record Packed(
			RecipeBookOptions settings,
			List<RegistryKey<Recipe<?>>> known,
			List<RegistryKey<Recipe<?>>> highlight
	) {

		public static final Codec<ServerRecipeBook.Packed> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    RecipeBookOptions.CODEC.forGetter(ServerRecipeBook.Packed::settings),
						                    Recipe.KEY_CODEC.listOf().fieldOf("recipes").forGetter(ServerRecipeBook.Packed::known),
						                    Recipe.KEY_CODEC.listOf().fieldOf("toBeDisplayed").forGetter(ServerRecipeBook.Packed::highlight)
				                    )
				                    .apply(instance, ServerRecipeBook.Packed::new)
		);
	}
}

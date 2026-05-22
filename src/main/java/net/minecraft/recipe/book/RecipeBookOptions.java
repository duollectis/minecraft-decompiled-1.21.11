package net.minecraft.recipe.book;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.function.UnaryOperator;

/**
 * Настройки книги рецептов для всех четырёх типов станков.
 * Хранит состояние открытости UI и фильтрации крафтабельных рецептов
 * для каждого {@link RecipeBookType}.
 */
public final class RecipeBookOptions {

	public static final PacketCodec<PacketByteBuf, RecipeBookOptions> PACKET_CODEC = PacketCodec.tuple(
			CategoryOption.PACKET_CODEC, options -> options.crafting,
			CategoryOption.PACKET_CODEC, options -> options.furnace,
			CategoryOption.PACKET_CODEC, options -> options.blastFurnace,
			CategoryOption.PACKET_CODEC, options -> options.smoker,
			RecipeBookOptions::new
	);
	public static final MapCodec<RecipeBookOptions> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					CategoryOption.CRAFTING.forGetter(options -> options.crafting),
					CategoryOption.FURNACE.forGetter(options -> options.furnace),
					CategoryOption.BLAST_FURNACE.forGetter(options -> options.blastFurnace),
					CategoryOption.SMOKER.forGetter(options -> options.smoker)
			).apply(instance, RecipeBookOptions::new)
	);

	private CategoryOption crafting;
	private CategoryOption furnace;
	private CategoryOption blastFurnace;
	private CategoryOption smoker;

	public RecipeBookOptions() {
		this(
				CategoryOption.DEFAULT,
				CategoryOption.DEFAULT,
				CategoryOption.DEFAULT,
				CategoryOption.DEFAULT
		);
	}

	private RecipeBookOptions(
			CategoryOption crafting,
			CategoryOption furnace,
			CategoryOption blastFurnace,
			CategoryOption smoker
	) {
		this.crafting = crafting;
		this.furnace = furnace;
		this.blastFurnace = blastFurnace;
		this.smoker = smoker;
	}

	@VisibleForTesting
	public CategoryOption getOption(RecipeBookType type) {
		return switch (type) {
			case CRAFTING -> crafting;
			case FURNACE -> furnace;
			case BLAST_FURNACE -> blastFurnace;
			case SMOKER -> smoker;
		};
	}

	private void apply(RecipeBookType type, UnaryOperator<CategoryOption> modifier) {
		switch (type) {
			case CRAFTING -> crafting = modifier.apply(crafting);
			case FURNACE -> furnace = modifier.apply(furnace);
			case BLAST_FURNACE -> blastFurnace = modifier.apply(blastFurnace);
			case SMOKER -> smoker = modifier.apply(smoker);
		}
	}

	public boolean isGuiOpen(RecipeBookType category) {
		return getOption(category).guiOpen;
	}

	public void setGuiOpen(RecipeBookType category, boolean open) {
		apply(category, option -> option.withGuiOpen(open));
	}

	public boolean isFilteringCraftable(RecipeBookType category) {
		return getOption(category).filteringCraftable;
	}

	public void setFilteringCraftable(RecipeBookType category, boolean filtering) {
		apply(category, option -> option.withFilteringCraftable(filtering));
	}

	public RecipeBookOptions copy() {
		return new RecipeBookOptions(crafting, furnace, blastFurnace, smoker);
	}

	public void copyFrom(RecipeBookOptions other) {
		crafting = other.crafting;
		furnace = other.furnace;
		blastFurnace = other.blastFurnace;
		smoker = other.smoker;
	}

	/**
	 * Иммутабельное состояние одной категории книги рецептов:
	 * открыт ли UI и включён ли фильтр «только крафтабельные».
	 */
	public record CategoryOption(boolean guiOpen, boolean filteringCraftable) {

		public static final CategoryOption DEFAULT = new CategoryOption(false, false);
		public static final MapCodec<CategoryOption> CRAFTING = createCodec("isGuiOpen", "isFilteringCraftable");
		public static final MapCodec<CategoryOption> FURNACE = createCodec(
				"isFurnaceGuiOpen", "isFurnaceFilteringCraftable"
		);
		public static final MapCodec<CategoryOption> BLAST_FURNACE = createCodec(
				"isBlastingFurnaceGuiOpen", "isBlastingFurnaceFilteringCraftable"
		);
		public static final MapCodec<CategoryOption> SMOKER = createCodec(
				"isSmokerGuiOpen", "isSmokerFilteringCraftable"
		);
		public static final PacketCodec<ByteBuf, CategoryOption> PACKET_CODEC = PacketCodec.tuple(
				PacketCodecs.BOOLEAN, CategoryOption::guiOpen,
				PacketCodecs.BOOLEAN, CategoryOption::filteringCraftable,
				CategoryOption::new
		);

		@Override
		public String toString() {
			return "[open=" + guiOpen + ", filtering=" + filteringCraftable + "]";
		}

		public CategoryOption withGuiOpen(boolean open) {
			return new CategoryOption(open, filteringCraftable);
		}

		public CategoryOption withFilteringCraftable(boolean filtering) {
			return new CategoryOption(guiOpen, filtering);
		}

		private static MapCodec<CategoryOption> createCodec(String guiOpenField, String filteringCraftableField) {
			return RecordCodecBuilder.mapCodec(
					instance -> instance.group(
							Codec.BOOL.optionalFieldOf(guiOpenField, false).forGetter(CategoryOption::guiOpen),
							Codec.BOOL.optionalFieldOf(filteringCraftableField, false).forGetter(CategoryOption::filteringCraftable)
					).apply(instance, CategoryOption::new)
			);
		}
	}
}

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
 * {@code RecipeBookOptions}.
 */
public final class RecipeBookOptions {

	public static final PacketCodec<PacketByteBuf, RecipeBookOptions> PACKET_CODEC = PacketCodec.tuple(
			RecipeBookOptions.CategoryOption.PACKET_CODEC,
			options -> options.crafting,
			RecipeBookOptions.CategoryOption.PACKET_CODEC,
			options -> options.furnace,
			RecipeBookOptions.CategoryOption.PACKET_CODEC,
			options -> options.blastFurnace,
			RecipeBookOptions.CategoryOption.PACKET_CODEC,
			options -> options.smoker,
			RecipeBookOptions::new
	);
	public static final MapCodec<RecipeBookOptions> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    RecipeBookOptions.CategoryOption.CRAFTING.forGetter(options -> options.crafting),
					                    RecipeBookOptions.CategoryOption.FURNACE.forGetter(options -> options.furnace),
					                    RecipeBookOptions.CategoryOption.BLAST_FURNACE.forGetter(options -> options.blastFurnace),
					                    RecipeBookOptions.CategoryOption.SMOKER.forGetter(options -> options.smoker)
			                    )
			                    .apply(instance, RecipeBookOptions::new)
	);
	private RecipeBookOptions.CategoryOption crafting;
	private RecipeBookOptions.CategoryOption furnace;
	private RecipeBookOptions.CategoryOption blastFurnace;
	private RecipeBookOptions.CategoryOption smoker;

	public RecipeBookOptions() {
		this(
				RecipeBookOptions.CategoryOption.DEFAULT,
				RecipeBookOptions.CategoryOption.DEFAULT,
				RecipeBookOptions.CategoryOption.DEFAULT,
				RecipeBookOptions.CategoryOption.DEFAULT
		);
	}

	private RecipeBookOptions(
			RecipeBookOptions.CategoryOption crafting,
			RecipeBookOptions.CategoryOption furnace,
			RecipeBookOptions.CategoryOption blastFurnace,
			RecipeBookOptions.CategoryOption smoker
	) {
		this.crafting = crafting;
		this.furnace = furnace;
		this.blastFurnace = blastFurnace;
		this.smoker = smoker;
	}

	@VisibleForTesting
	public RecipeBookOptions.CategoryOption getOption(RecipeBookType type) {
		return switch (type) {
			case CRAFTING -> this.crafting;
			case FURNACE -> this.furnace;
			case BLAST_FURNACE -> this.blastFurnace;
			case SMOKER -> this.smoker;
		};
	}

	private void apply(RecipeBookType type, UnaryOperator<RecipeBookOptions.CategoryOption> modifier) {
		switch (type) {
			case CRAFTING:
				this.crafting = modifier.apply(this.crafting);
				break;
			case FURNACE:
				this.furnace = modifier.apply(this.furnace);
				break;
			case BLAST_FURNACE:
				this.blastFurnace = modifier.apply(this.blastFurnace);
				break;
			case SMOKER:
				this.smoker = modifier.apply(this.smoker);
		}
	}

	public boolean isGuiOpen(RecipeBookType category) {
		return this.getOption(category).guiOpen;
	}

	public void setGuiOpen(RecipeBookType category, boolean open) {
		this.apply(category, option -> option.withGuiOpen(open));
	}

	public boolean isFilteringCraftable(RecipeBookType category) {
		return this.getOption(category).filteringCraftable;
	}

	public void setFilteringCraftable(RecipeBookType category, boolean filtering) {
		this.apply(category, option -> option.withFilteringCraftable(filtering));
	}

	/**
	 * Copy.
	 *
	 * @return RecipeBookOptions — результат операции
	 */
	public RecipeBookOptions copy() {
		return new RecipeBookOptions(this.crafting, this.furnace, this.blastFurnace, this.smoker);
	}

	/**
	 * Создаёт копию from.
	 *
	 * @param other other
	 */
	public void copyFrom(RecipeBookOptions other) {
		this.crafting = other.crafting;
		this.furnace = other.furnace;
		this.blastFurnace = other.blastFurnace;
		this.smoker = other.smoker;
	}

	/**
	 * {@code CategoryOption}.
	 */
	public record CategoryOption(boolean guiOpen, boolean filteringCraftable) {

		public static final RecipeBookOptions.CategoryOption
				DEFAULT =
				new RecipeBookOptions.CategoryOption(false, false);
		public static final MapCodec<RecipeBookOptions.CategoryOption>
				CRAFTING =
				createCodec("isGuiOpen", "isFilteringCraftable");
		public static final MapCodec<RecipeBookOptions.CategoryOption>
				FURNACE =
				createCodec("isFurnaceGuiOpen", "isFurnaceFilteringCraftable");
		public static final MapCodec<RecipeBookOptions.CategoryOption> BLAST_FURNACE = createCodec(
				"isBlastingFurnaceGuiOpen", "isBlastingFurnaceFilteringCraftable"
		);
		public static final MapCodec<RecipeBookOptions.CategoryOption>
				SMOKER =
				createCodec("isSmokerGuiOpen", "isSmokerFilteringCraftable");
		public static final PacketCodec<ByteBuf, RecipeBookOptions.CategoryOption> PACKET_CODEC = PacketCodec.tuple(
				PacketCodecs.BOOLEAN,
				RecipeBookOptions.CategoryOption::guiOpen,
				PacketCodecs.BOOLEAN,
				RecipeBookOptions.CategoryOption::filteringCraftable,
				RecipeBookOptions.CategoryOption::new
		);

		@Override
		public String toString() {
			return "[open=" + this.guiOpen + ", filtering=" + this.filteringCraftable + "]";
		}

		public RecipeBookOptions.CategoryOption withGuiOpen(boolean guiOpen) {
			return new RecipeBookOptions.CategoryOption(guiOpen, this.filteringCraftable);
		}

		public RecipeBookOptions.CategoryOption withFilteringCraftable(boolean filteringCraftable) {
			return new RecipeBookOptions.CategoryOption(this.guiOpen, filteringCraftable);
		}

		private static MapCodec<RecipeBookOptions.CategoryOption> createCodec(
				String guiOpenField,
				String filteringCraftableField
		) {
			return RecordCodecBuilder.mapCodec(
					instance -> instance.group(
							                    Codec.BOOL
									                    .optionalFieldOf(guiOpenField, false)
									                    .forGetter(RecipeBookOptions.CategoryOption::guiOpen),
							                    Codec.BOOL
									                    .optionalFieldOf(filteringCraftableField, false)
									                    .forGetter(RecipeBookOptions.CategoryOption::filteringCraftable)
					                    )
					                    .apply(instance, RecipeBookOptions.CategoryOption::new)
			);
		}
	}
}

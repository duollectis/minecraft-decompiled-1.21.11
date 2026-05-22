package net.minecraft.data.family;

import com.google.common.collect.Maps;
import net.minecraft.block.Block;
import net.minecraft.util.StringHelper;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Описывает семейство блоков: базовый блок и его варианты (ступени, плиты, двери и т.д.).
 * Используется генераторами рецептов и моделей для автоматического создания
 * всех стандартных рецептов семейства.
 */
public class BlockFamily {

	private final Block baseBlock;
	final Map<Variant, Block> variants = Maps.newHashMap();
	boolean generateModels = true;
	boolean generateRecipes = true;
	@Nullable String group;
	@Nullable String unlockCriterionName;

	BlockFamily(Block baseBlock) {
		this.baseBlock = baseBlock;
	}

	public Block getBaseBlock() {
		return baseBlock;
	}

	public Map<Variant, Block> getVariants() {
		return variants;
	}

	public Block getVariant(Variant variant) {
		return variants.get(variant);
	}

	public boolean shouldGenerateModels() {
		return generateModels;
	}

	public boolean shouldGenerateRecipes() {
		return generateRecipes;
	}

	public Optional<String> getGroup() {
		return StringHelper.isBlank(group) ? Optional.empty() : Optional.of(group);
	}

	public Optional<String> getUnlockCriterionName() {
		return StringHelper.isBlank(unlockCriterionName)
				? Optional.empty()
				: Optional.of(unlockCriterionName);
	}

	/**
	 * Строитель семейства блоков. Позволяет декларативно задать все варианты
	 * базового блока и параметры генерации.
	 */
	public static class Builder {

		private final BlockFamily family;

		public Builder(Block baseBlock) {
			this.family = new BlockFamily(baseBlock);
		}

		public BlockFamily build() {
			return family;
		}

		public Builder button(Block block) {
			family.variants.put(Variant.BUTTON, block);
			return this;
		}

		public Builder chiseled(Block block) {
			family.variants.put(Variant.CHISELED, block);
			return this;
		}

		public Builder mosaic(Block block) {
			family.variants.put(Variant.MOSAIC, block);
			return this;
		}

		public Builder cracked(Block block) {
			family.variants.put(Variant.CRACKED, block);
			return this;
		}

		public Builder cut(Block block) {
			family.variants.put(Variant.CUT, block);
			return this;
		}

		public Builder door(Block block) {
			family.variants.put(Variant.DOOR, block);
			return this;
		}

		public Builder customFence(Block block) {
			family.variants.put(Variant.CUSTOM_FENCE, block);
			return this;
		}

		public Builder fence(Block block) {
			family.variants.put(Variant.FENCE, block);
			return this;
		}

		public Builder customFenceGate(Block block) {
			family.variants.put(Variant.CUSTOM_FENCE_GATE, block);
			return this;
		}

		public Builder fenceGate(Block block) {
			family.variants.put(Variant.FENCE_GATE, block);
			return this;
		}

		public Builder sign(Block block, Block wallBlock) {
			family.variants.put(Variant.SIGN, block);
			family.variants.put(Variant.WALL_SIGN, wallBlock);
			return this;
		}

		public Builder slab(Block block) {
			family.variants.put(Variant.SLAB, block);
			return this;
		}

		public Builder stairs(Block block) {
			family.variants.put(Variant.STAIRS, block);
			return this;
		}

		public Builder pressurePlate(Block block) {
			family.variants.put(Variant.PRESSURE_PLATE, block);
			return this;
		}

		public Builder polished(Block block) {
			family.variants.put(Variant.POLISHED, block);
			return this;
		}

		public Builder trapdoor(Block block) {
			family.variants.put(Variant.TRAPDOOR, block);
			return this;
		}

		public Builder wall(Block block) {
			family.variants.put(Variant.WALL, block);
			return this;
		}

		public Builder noGenerateModels() {
			family.generateModels = false;
			return this;
		}

		public Builder noGenerateRecipes() {
			family.generateRecipes = false;
			return this;
		}

		public Builder group(String group) {
			family.group = group;
			return this;
		}

		public Builder unlockCriterionName(String unlockCriterionName) {
			family.unlockCriterionName = unlockCriterionName;
			return this;
		}
	}

	/**
	 * Перечисление всех возможных вариантов блока в семействе.
	 * Имя варианта используется при построении идентификаторов рецептов и моделей.
	 */
	public enum Variant {
		BUTTON("button"),
		CHISELED("chiseled"),
		CRACKED("cracked"),
		CUT("cut"),
		DOOR("door"),
		CUSTOM_FENCE("fence"),
		FENCE("fence"),
		CUSTOM_FENCE_GATE("fence_gate"),
		FENCE_GATE("fence_gate"),
		MOSAIC("mosaic"),
		SIGN("sign"),
		SLAB("slab"),
		STAIRS("stairs"),
		PRESSURE_PLATE("pressure_plate"),
		POLISHED("polished"),
		TRAPDOOR("trapdoor"),
		WALL("wall"),
		WALL_SIGN("wall_sign");

		private final String name;

		Variant(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}

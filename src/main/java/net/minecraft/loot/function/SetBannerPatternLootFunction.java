package net.minecraft.loot.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.DyeColor;

import java.util.List;

/** Функция лута, устанавливающая или добавляющая узоры баннера к предмету. */
public class SetBannerPatternLootFunction extends ConditionalLootFunction {

	public static final MapCodec<SetBannerPatternLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(instance.group(
				BannerPatternsComponent.CODEC
					.fieldOf("patterns")
					.forGetter(function -> function.patterns),
				Codec.BOOL.fieldOf("append").forGetter(function -> function.append)
			))
			.apply(instance, SetBannerPatternLootFunction::new)
	);

	private final BannerPatternsComponent patterns;
	private final boolean append;

	SetBannerPatternLootFunction(List<LootCondition> conditions, BannerPatternsComponent patterns, boolean append) {
		super(conditions);
		this.patterns = patterns;
		this.append = append;
	}

	@Override
	protected ItemStack process(ItemStack stack, LootContext context) {
		if (append) {
			stack.apply(
				DataComponentTypes.BANNER_PATTERNS,
				BannerPatternsComponent.DEFAULT,
				patterns,
				(current, newPatterns) -> new BannerPatternsComponent.Builder()
					.addAll(current)
					.addAll(newPatterns)
					.build()
			);
		} else {
			stack.set(DataComponentTypes.BANNER_PATTERNS, patterns);
		}

		return stack;
	}

	@Override
	public LootFunctionType<SetBannerPatternLootFunction> getType() {
		return LootFunctionTypes.SET_BANNER_PATTERN;
	}

	public static Builder builder(boolean append) {
		return new Builder(append);
	}

	/** Строитель функции установки узоров баннера. */
	public static class Builder extends ConditionalLootFunction.Builder<Builder> {

		private final BannerPatternsComponent.Builder patterns = new BannerPatternsComponent.Builder();
		private final boolean append;

		Builder(boolean append) {
			this.append = append;
		}

		@Override
		protected Builder getThisBuilder() {
			return this;
		}

		public Builder pattern(RegistryEntry<BannerPattern> pattern, DyeColor color) {
			patterns.add(pattern, color);
			return this;
		}

		@Override
		public LootFunction build() {
			return new SetBannerPatternLootFunction(getConditions(), patterns.build(), append);
		}
	}
}

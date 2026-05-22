package net.minecraft.loot.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;

import java.util.List;
import java.util.Optional;

/**
 * Функция лута, устанавливающая параметры взрыва фейерверка.
 * Все поля опциональны — если не указаны, сохраняются текущие значения компонента.
 */
public class SetFireworkExplosionLootFunction extends ConditionalLootFunction {

	public static final FireworkExplosionComponent DEFAULT_EXPLOSION = new FireworkExplosionComponent(
		FireworkExplosionComponent.Type.SMALL_BALL, IntList.of(), IntList.of(), false, false
	);

	public static final MapCodec<SetFireworkExplosionLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(
				instance.group(
					FireworkExplosionComponent.Type.CODEC
						.optionalFieldOf("shape")
						.forGetter(function -> function.shape),
					FireworkExplosionComponent.COLORS_CODEC
						.optionalFieldOf("colors")
						.forGetter(function -> function.colors),
					FireworkExplosionComponent.COLORS_CODEC
						.optionalFieldOf("fade_colors")
						.forGetter(function -> function.fadeColors),
					Codec.BOOL.optionalFieldOf("trail").forGetter(function -> function.trail),
					Codec.BOOL.optionalFieldOf("twinkle").forGetter(function -> function.twinkle)
				)
			)
			.apply(instance, SetFireworkExplosionLootFunction::new)
	);

	final Optional<FireworkExplosionComponent.Type> shape;
	final Optional<IntList> colors;
	final Optional<IntList> fadeColors;
	final Optional<Boolean> trail;
	final Optional<Boolean> twinkle;

	public SetFireworkExplosionLootFunction(
		List<LootCondition> conditions,
		Optional<FireworkExplosionComponent.Type> shape,
		Optional<IntList> colors,
		Optional<IntList> fadeColors,
		Optional<Boolean> trail,
		Optional<Boolean> twinkle
	) {
		super(conditions);
		this.shape = shape;
		this.colors = colors;
		this.fadeColors = fadeColors;
		this.trail = trail;
		this.twinkle = twinkle;
	}

	@Override
	protected ItemStack process(ItemStack stack, LootContext context) {
		stack.apply(DataComponentTypes.FIREWORK_EXPLOSION, DEFAULT_EXPLOSION, this::applyToExplosion);
		return stack;
	}

	private FireworkExplosionComponent applyToExplosion(FireworkExplosionComponent current) {
		return new FireworkExplosionComponent(
			shape.orElseGet(current::shape),
			colors.orElseGet(current::colors),
			fadeColors.orElseGet(current::fadeColors),
			trail.orElseGet(current::hasTrail),
			twinkle.orElseGet(current::hasTwinkle)
		);
	}

	@Override
	public LootFunctionType<SetFireworkExplosionLootFunction> getType() {
		return LootFunctionTypes.SET_FIREWORK_EXPLOSION;
	}
}

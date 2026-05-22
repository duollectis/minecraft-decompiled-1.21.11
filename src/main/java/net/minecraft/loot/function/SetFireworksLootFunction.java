package net.minecraft.loot.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.collection.ListOperation;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;
import java.util.Optional;

/**
 * Функция лута, устанавливающая компонент фейерверка предмета:
 * список взрывов и длительность полёта.
 */
public class SetFireworksLootFunction extends ConditionalLootFunction {

	public static final FireworksComponent DEFAULT_FIREWORKS = new FireworksComponent(0, List.of());

	public static final MapCodec<SetFireworksLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(
				instance.group(
					ListOperation.Values
						.createCodec(FireworkExplosionComponent.CODEC, 256)
						.optionalFieldOf("explosions")
						.forGetter(function -> function.explosions),
					Codecs.UNSIGNED_BYTE
						.optionalFieldOf("flight_duration")
						.forGetter(function -> function.flightDuration)
				)
			)
			.apply(instance, SetFireworksLootFunction::new)
	);

	private final Optional<ListOperation.Values<FireworkExplosionComponent>> explosions;
	private final Optional<Integer> flightDuration;

	protected SetFireworksLootFunction(
		List<LootCondition> conditions,
		Optional<ListOperation.Values<FireworkExplosionComponent>> explosions,
		Optional<Integer> flightDuration
	) {
		super(conditions);
		this.explosions = explosions;
		this.flightDuration = flightDuration;
	}

	@Override
	protected ItemStack process(ItemStack stack, LootContext context) {
		stack.apply(DataComponentTypes.FIREWORKS, DEFAULT_FIREWORKS, this::applyToFireworks);
		return stack;
	}

	private FireworksComponent applyToFireworks(FireworksComponent current) {
		return new FireworksComponent(
			flightDuration.orElseGet(current::flightDuration),
			explosions
				.<List<FireworkExplosionComponent>>map(values -> values.apply(current.explosions()))
				.orElse(current.explosions())
		);
	}

	@Override
	public LootFunctionType<SetFireworksLootFunction> getType() {
		return LootFunctionTypes.SET_FIREWORKS;
	}
}

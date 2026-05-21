package net.minecraft.loot.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.context.ContextParameter;

import java.util.List;
import java.util.Set;

/**
 * {@code FillPlayerHeadLootFunction}.
 */
public class FillPlayerHeadLootFunction extends ConditionalLootFunction {

	public static final MapCodec<FillPlayerHeadLootFunction> CODEC = RecordCodecBuilder.mapCodec(
			instance -> addConditionsField(instance)
					.and(LootContext.EntityReference.CODEC.fieldOf("entity").forGetter(function -> function.entity))
					.apply(instance, FillPlayerHeadLootFunction::new)
	);
	private final LootContext.EntityReference entity;

	public FillPlayerHeadLootFunction(List<LootCondition> conditions, LootContext.EntityReference entity) {
		super(conditions);
		this.entity = entity;
	}

	@Override
	public LootFunctionType<FillPlayerHeadLootFunction> getType() {
		return LootFunctionTypes.FILL_PLAYER_HEAD;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return Set.of(this.entity.contextParam());
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		if (stack.isOf(Items.PLAYER_HEAD)
				&& context.get(this.entity.contextParam()) instanceof PlayerEntity playerEntity) {
			stack.set(DataComponentTypes.PROFILE, ProfileComponent.ofStatic(playerEntity.getGameProfile()));
		}

		return stack;
	}

	public static ConditionalLootFunction.Builder<?> builder(LootContext.EntityReference target) {
		return builder(conditions -> new FillPlayerHeadLootFunction(conditions, target));
	}
}

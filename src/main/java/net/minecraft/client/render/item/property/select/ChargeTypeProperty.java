package net.minecraft.client.render.item.property.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code ChargeTypeProperty}.
 */
public record ChargeTypeProperty() implements SelectProperty<CrossbowItem.ChargeType> {

	public static final Codec<CrossbowItem.ChargeType> VALUE_CODEC = CrossbowItem.ChargeType.CODEC;
	public static final SelectProperty.Type<ChargeTypeProperty, CrossbowItem.ChargeType>
			TYPE =
			SelectProperty.Type.create(
					MapCodec.unit(new ChargeTypeProperty()), VALUE_CODEC
			);

	public CrossbowItem.ChargeType getValue(
			ItemStack itemStack,
			@Nullable ClientWorld clientWorld,
			@Nullable LivingEntity livingEntity,
			int i,
			ItemDisplayContext itemDisplayContext
	) {
		ChargedProjectilesComponent chargedProjectilesComponent = itemStack.get(DataComponentTypes.CHARGED_PROJECTILES);
		if (chargedProjectilesComponent == null || chargedProjectilesComponent.isEmpty()) {
			return CrossbowItem.ChargeType.NONE;
		}
		else {
			return chargedProjectilesComponent.contains(Items.FIREWORK_ROCKET) ? CrossbowItem.ChargeType.ROCKET
			                                                                   : CrossbowItem.ChargeType.ARROW;
		}
	}

	@Override
	public SelectProperty.Type<ChargeTypeProperty, CrossbowItem.ChargeType> getType() {
		return TYPE;
	}

	@Override
	public Codec<CrossbowItem.ChargeType> valueCodec() {
		return VALUE_CODEC;
	}
}

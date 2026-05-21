package net.minecraft.client.render.item.property.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code DisplayContextProperty}.
 */
public record DisplayContextProperty() implements SelectProperty<ItemDisplayContext> {

	public static final Codec<ItemDisplayContext> VALUE_CODEC = ItemDisplayContext.CODEC;
	public static final SelectProperty.Type<DisplayContextProperty, ItemDisplayContext>
			TYPE =
			SelectProperty.Type.create(
					MapCodec.unit(new DisplayContextProperty()), VALUE_CODEC
			);

	public ItemDisplayContext getValue(
			ItemStack itemStack,
			@Nullable ClientWorld clientWorld,
			@Nullable LivingEntity livingEntity,
			int i,
			ItemDisplayContext itemDisplayContext
	) {
		return itemDisplayContext;
	}

	@Override
	public SelectProperty.Type<DisplayContextProperty, ItemDisplayContext> getType() {
		return TYPE;
	}

	@Override
	public Codec<ItemDisplayContext> valueCodec() {
		return VALUE_CODEC;
	}
}

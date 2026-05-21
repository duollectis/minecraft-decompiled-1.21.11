package net.minecraft.client.render.item.property.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code ContextDimensionProperty}.
 */
public record ContextDimensionProperty() implements SelectProperty<RegistryKey<World>> {

	public static final Codec<RegistryKey<World>> VALUE_CODEC = RegistryKey.createCodec(RegistryKeys.WORLD);
	public static final SelectProperty.Type<ContextDimensionProperty, RegistryKey<World>>
			TYPE =
			SelectProperty.Type.create(
					MapCodec.unit(new ContextDimensionProperty()), VALUE_CODEC
			);

	public @Nullable RegistryKey<World> getValue(
			ItemStack itemStack,
			@Nullable ClientWorld clientWorld,
			@Nullable LivingEntity livingEntity,
			int i,
			ItemDisplayContext itemDisplayContext
	) {
		return clientWorld != null ? clientWorld.getRegistryKey() : null;
	}

	@Override
	public SelectProperty.Type<ContextDimensionProperty, RegistryKey<World>> getType() {
		return TYPE;
	}

	@Override
	public Codec<RegistryKey<World>> valueCodec() {
		return VALUE_CODEC;
	}
}

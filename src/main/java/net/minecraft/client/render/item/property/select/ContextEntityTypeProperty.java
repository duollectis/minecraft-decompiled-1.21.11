package net.minecraft.client.render.item.property.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code ContextEntityTypeProperty}.
 */
public record ContextEntityTypeProperty() implements SelectProperty<RegistryKey<EntityType<?>>> {

	public static final Codec<RegistryKey<EntityType<?>>>
			VALUE_CODEC =
			RegistryKey.createCodec(RegistryKeys.ENTITY_TYPE);
	public static final SelectProperty.Type<ContextEntityTypeProperty, RegistryKey<EntityType<?>>>
			TYPE =
			SelectProperty.Type.create(
					MapCodec.unit(new ContextEntityTypeProperty()), VALUE_CODEC
			);

	public @Nullable RegistryKey<EntityType<?>> getValue(
			ItemStack itemStack,
			@Nullable ClientWorld clientWorld,
			@Nullable LivingEntity livingEntity,
			int i,
			ItemDisplayContext itemDisplayContext
	) {
		return livingEntity == null ? null : livingEntity.getType().getRegistryEntry().registryKey();
	}

	@Override
	public SelectProperty.Type<ContextEntityTypeProperty, RegistryKey<EntityType<?>>> getType() {
		return TYPE;
	}

	@Override
	public Codec<RegistryKey<EntityType<?>>> valueCodec() {
		return VALUE_CODEC;
	}
}

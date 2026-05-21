package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code CustomModelDataFlagProperty}.
 */
public record CustomModelDataFlagProperty(int index) implements BooleanProperty {

	public static final MapCodec<CustomModelDataFlagProperty> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(Codecs.NON_NEGATIVE_INT
							.optionalFieldOf("index", 0)
							.forGetter(CustomModelDataFlagProperty::index))
					.apply(instance, CustomModelDataFlagProperty::new)
	);

	@Override
	public boolean test(
			ItemStack stack,
			@Nullable ClientWorld world,
			@Nullable LivingEntity entity,
			int seed,
			ItemDisplayContext displayContext
	) {
		CustomModelDataComponent customModelDataComponent = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
		return customModelDataComponent != null ? customModelDataComponent.getFlag(this.index) == Boolean.TRUE : false;
	}

	@Override
	public MapCodec<CustomModelDataFlagProperty> getCodec() {
		return CODEC;
	}
}

package net.minecraft.client.render.item.property.numeric;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code CustomModelDataFloatProperty}.
 */
public record CustomModelDataFloatProperty(int index) implements NumericProperty {

	public static final MapCodec<CustomModelDataFloatProperty> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(Codecs.NON_NEGATIVE_INT
							.optionalFieldOf("index", 0)
							.forGetter(CustomModelDataFloatProperty::index))
					.apply(instance, CustomModelDataFloatProperty::new)
	);

	@Override
	public float getValue(ItemStack stack, @Nullable ClientWorld world, @Nullable HeldItemContext context, int seed) {
		CustomModelDataComponent customModelDataComponent = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
		if (customModelDataComponent != null) {
			Float float_ = customModelDataComponent.getFloat(this.index);
			if (float_ != null) {
				return float_;
			}
		}

		return 0.0F;
	}

	@Override
	public MapCodec<CustomModelDataFloatProperty> getCodec() {
		return CODEC;
	}
}

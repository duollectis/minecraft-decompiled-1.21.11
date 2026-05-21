package net.minecraft.client.render.item.property.numeric;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code NumericProperty}.
 */
public interface NumericProperty {

	float getValue(ItemStack stack, @Nullable ClientWorld world, @Nullable HeldItemContext context, int seed);

	MapCodec<? extends NumericProperty> getCodec();
}

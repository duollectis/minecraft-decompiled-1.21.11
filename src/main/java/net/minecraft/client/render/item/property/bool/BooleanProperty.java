package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.property.PropertyTester;

@Environment(EnvType.CLIENT)
/**
 * {@code BooleanProperty}.
 */
public interface BooleanProperty extends PropertyTester {

	MapCodec<? extends BooleanProperty> getCodec();
}

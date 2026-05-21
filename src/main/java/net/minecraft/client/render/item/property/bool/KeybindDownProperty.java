package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code KeybindDownProperty}.
 */
public record KeybindDownProperty(KeyBinding keybind) implements BooleanProperty {

	private static final Codec<KeyBinding> KEY_BINDING_CODEC = Codec.STRING.comapFlatMap(
			id -> {
				KeyBinding keyBinding = KeyBinding.byId(id);
				return keyBinding != null ? DataResult.success(keyBinding)
				                          : DataResult.error(() -> "Invalid keybind: " + id);
			}, KeyBinding::getId
	);
	public static final MapCodec<KeybindDownProperty> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(KEY_BINDING_CODEC.fieldOf("keybind").forGetter(KeybindDownProperty::keybind))
					.apply(instance, KeybindDownProperty::new)
	);

	@Override
	public boolean test(
			ItemStack stack,
			@Nullable ClientWorld world,
			@Nullable LivingEntity entity,
			int seed,
			ItemDisplayContext displayContext
	) {
		return this.keybind.isPressed();
	}

	@Override
	public MapCodec<KeybindDownProperty> getCodec() {
		return CODEC;
	}
}

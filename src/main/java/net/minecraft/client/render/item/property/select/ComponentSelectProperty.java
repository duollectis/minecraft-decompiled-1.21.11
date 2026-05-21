package net.minecraft.client.render.item.property.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.model.SelectItemModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code ComponentSelectProperty}.
 */
public record ComponentSelectProperty<T>(ComponentType<T> componentType) implements SelectProperty<T> {

	private static final SelectProperty.Type<? extends ComponentSelectProperty<?>, ?> TYPE = createType();

	@SuppressWarnings("unchecked")
	private static <T> SelectProperty.Type<ComponentSelectProperty<T>, T> createType() {
		Codec<? extends ComponentType<?>> codec = Registries.DATA_COMPONENT_TYPE
				.getCodec()
				.validate(
						componentType -> componentType.shouldSkipSerialization()
						                 ? DataResult.error(() -> "Component can't be serialized")
						                 : DataResult.success(componentType)
				);
		MapCodec<SelectItemModel.UnbakedSwitch<ComponentSelectProperty<T>, T>>
				mapCodec =
				((Codec<ComponentType<T>>) codec).dispatchMap(
						"component",
						unbakedSwitch -> unbakedSwitch.property().componentType,
						componentType -> SelectProperty.Type.createCaseListCodec(componentType.getCodecOrThrow())
						                                    .xmap(
								                                    cases -> new SelectItemModel.UnbakedSwitch<>(
										                                    new ComponentSelectProperty<>(componentType),
										                                    cases
								                                    ), SelectItemModel.UnbakedSwitch::cases
						                                    )
				);
		return new SelectProperty.Type<>(mapCodec);
	}

	public static <T> SelectProperty.Type<ComponentSelectProperty<T>, T> getTypeInstance() {
		return (SelectProperty.Type<ComponentSelectProperty<T>, T>) TYPE;
	}

	@Override
	public @Nullable T getValue(
			ItemStack stack,
			@Nullable ClientWorld world,
			@Nullable LivingEntity user,
			int seed,
			ItemDisplayContext displayContext
	) {
		return stack.get(this.componentType);
	}

	@Override
	public SelectProperty.Type<ComponentSelectProperty<T>, T> getType() {
		return getTypeInstance();
	}

	@Override
	public Codec<T> valueCodec() {
		return this.componentType.getCodecOrThrow();
	}
}

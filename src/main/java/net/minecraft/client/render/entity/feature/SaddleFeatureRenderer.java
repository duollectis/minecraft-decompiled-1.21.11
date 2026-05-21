package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
/**
 * {@code SaddleFeatureRenderer}.
 */
public class SaddleFeatureRenderer<S extends LivingEntityRenderState, RM extends EntityModel<? super S>, EM extends EntityModel<? super S>>
		extends FeatureRenderer<S, RM> {

	private final EquipmentRenderer equipmentRenderer;
	private final EquipmentModel.LayerType layerType;
	private final Function<S, ItemStack> saddleStackGetter;
	private final EM adultModel;
	private final @Nullable EM babyModel;
	private final int saddleTextureIndex;

	public SaddleFeatureRenderer(
			FeatureRendererContext<S, RM> context,
			EquipmentRenderer equipmentRenderer,
			EquipmentModel.LayerType layerType,
			Function<S, ItemStack> saddleStackGetter,
			EM adultModel,
			@Nullable EM babyModel,
			int i
	) {
		super(context);
		this.equipmentRenderer = equipmentRenderer;
		this.layerType = layerType;
		this.saddleStackGetter = saddleStackGetter;
		this.adultModel = adultModel;
		this.babyModel = babyModel;
		this.saddleTextureIndex = i;
	}

	public SaddleFeatureRenderer(
			FeatureRendererContext<S, RM> context,
			EquipmentRenderer equipmentRenderer,
			EquipmentModel.LayerType layerType,
			Function<S, ItemStack> function,
			EM entityModel,
			@Nullable EM entityModel2
	) {
		this(context, equipmentRenderer, layerType, function, entityModel, entityModel2, 0);
	}

	public void render(
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int i,
			S livingEntityRenderState,
			float f,
			float g
	) {
		ItemStack itemStack = this.saddleStackGetter.apply(livingEntityRenderState);
		EquippableComponent equippableComponent = itemStack.get(DataComponentTypes.EQUIPPABLE);
		if (equippableComponent != null && !equippableComponent.assetId().isEmpty() && (!livingEntityRenderState.baby
				|| this.babyModel != null
		)) {
			EM entityModel = livingEntityRenderState.baby ? this.babyModel : this.adultModel;
			this.equipmentRenderer
					.render(
							this.layerType,
							equippableComponent.assetId().get(),
							entityModel,
							livingEntityRenderState,
							itemStack,
							matrixStack,
							orderedRenderCommandQueue,
							i,
							null,
							livingEntityRenderState.outlineColor,
							this.saddleTextureIndex
					);
		}
	}
}

package net.minecraft.client.render.entity.feature;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.HorseEntityModel;
import net.minecraft.client.render.entity.state.HorseEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.HorseMarking;
import net.minecraft.util.Identifier;

import java.util.Map;

@Environment(EnvType.CLIENT)
/**
 * {@code HorseMarkingFeatureRenderer}.
 */
public class HorseMarkingFeatureRenderer extends FeatureRenderer<HorseEntityRenderState, HorseEntityModel> {

	private static final Identifier INVISIBLE_ID = Identifier.ofVanilla("invisible");
	private static final Map<HorseMarking, Identifier> TEXTURES = Maps.newEnumMap(
			Map.of(
					HorseMarking.NONE,
					INVISIBLE_ID,
					HorseMarking.WHITE,
					Identifier.ofVanilla("textures/entity/horse/horse_markings_white.png"),
					HorseMarking.WHITE_FIELD,
					Identifier.ofVanilla("textures/entity/horse/horse_markings_whitefield.png"),
					HorseMarking.WHITE_DOTS,
					Identifier.ofVanilla("textures/entity/horse/horse_markings_whitedots.png"),
					HorseMarking.BLACK_DOTS,
					Identifier.ofVanilla("textures/entity/horse/horse_markings_blackdots.png")
			)
	);

	public HorseMarkingFeatureRenderer(FeatureRendererContext<HorseEntityRenderState, HorseEntityModel> featureRendererContext) {
		super(featureRendererContext);
	}

	public void render(
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int i,
			HorseEntityRenderState horseEntityRenderState,
			float f,
			float g
	) {
		Identifier identifier = TEXTURES.get(horseEntityRenderState.marking);
		if (identifier != INVISIBLE_ID && !horseEntityRenderState.invisible) {
			orderedRenderCommandQueue.getBatchingQueue(1)
			                         .submitModel(
					                         this.getContextModel(),
					                         horseEntityRenderState,
					                         matrixStack,
					                         RenderLayers.entityTranslucent(identifier),
					                         i,
					                         LivingEntityRenderer.getOverlay(horseEntityRenderState, 0.0F),
					                         -1,
					                         null,
					                         horseEntityRenderState.outlineColor,
					                         null
			                         );
		}
	}
}

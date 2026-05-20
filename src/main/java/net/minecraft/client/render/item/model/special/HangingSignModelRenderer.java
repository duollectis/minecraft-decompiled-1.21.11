package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.WoodType;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.block.entity.HangingSignBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.Identifier;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public class HangingSignModelRenderer implements SimpleSpecialModelRenderer {
   private final SpriteHolder spriteHolder;
   private final Model.SinglePartModel model;
   private final SpriteIdentifier texture;

   public HangingSignModelRenderer(SpriteHolder spriteHolder, Model.SinglePartModel model, SpriteIdentifier texture) {
      this.spriteHolder = spriteHolder;
      this.model = model;
      this.texture = texture;
   }

   @Override
   public void render(ItemDisplayContext displayContext, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, boolean glint, int i) {
      HangingSignBlockEntityRenderer.renderAsItem(this.spriteHolder, matrices, queue, light, overlay, this.model, this.texture);
   }

   @Override
   public void collectVertices(Consumer<Vector3fc> consumer) {
      MatrixStack matrixStack = new MatrixStack();
      HangingSignBlockEntityRenderer.setAngles(matrixStack, 0.0F);
      matrixStack.scale(1.0F, -1.0F, -1.0F);
      this.model.getRootPart().collectVertices(matrixStack, consumer);
   }

   @Environment(EnvType.CLIENT)
   public record Unbaked(WoodType woodType, Optional<Identifier> texture) implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<HangingSignModelRenderer.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               WoodType.CODEC.fieldOf("wood_type").forGetter(HangingSignModelRenderer.Unbaked::woodType),
               Identifier.CODEC.optionalFieldOf("texture").forGetter(HangingSignModelRenderer.Unbaked::texture)
            )
            .apply(instance, HangingSignModelRenderer.Unbaked::new)
      );

      public Unbaked(WoodType woodType) {
         this(woodType, Optional.empty());
      }

      @Override
      public MapCodec<HangingSignModelRenderer.Unbaked> getCodec() {
         return CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) {
         Model.SinglePartModel singlePartModel = HangingSignBlockEntityRenderer.createModel(
            context.entityModelSet(), this.woodType, HangingSignBlockEntityRenderer.AttachmentType.CEILING_MIDDLE
         );
         SpriteIdentifier spriteIdentifier = this.texture
            .map(TexturedRenderLayers.HANGING_SIGN_SPRITE_MAPPER::map)
            .orElseGet(() -> TexturedRenderLayers.getHangingSignTextureId(this.woodType));
         return new HangingSignModelRenderer(context.spriteHolder(), singlePartModel, spriteIdentifier);
      }
   }
}

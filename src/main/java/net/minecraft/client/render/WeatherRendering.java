package net.minecraft.client.render;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.state.WeatherRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

@Environment(EnvType.CLIENT)
public class WeatherRendering {
   private static final float field_63581 = 0.225F;
   private static final int field_53148 = 10;
   private static final Identifier RAIN_TEXTURE = Identifier.ofVanilla("textures/environment/rain.png");
   private static final Identifier SNOW_TEXTURE = Identifier.ofVanilla("textures/environment/snow.png");
   private static final int field_53152 = 32;
   private static final int field_53153 = 16;
   private int soundChance;
   private final float[] NORMAL_LINE_DX = new float[1024];
   private final float[] NORMAL_LINE_DZ = new float[1024];

   public WeatherRendering() {
      for (int i = 0; i < 32; i++) {
         for (int j = 0; j < 32; j++) {
            float f = j - 16;
            float g = i - 16;
            float h = MathHelper.hypot(f, g);
            this.NORMAL_LINE_DX[i * 32 + j] = -g / h;
            this.NORMAL_LINE_DZ[i * 32 + j] = f / h;
         }
      }
   }

   public void buildPrecipitationPieces(World world, int ticks, float tickProgress, Vec3d cameraPos, WeatherRenderState state) {
      state.intensity = world.getRainGradient(tickProgress);
      if (!(state.intensity <= 0.0F)) {
         state.radius = MinecraftClient.getInstance().options.getWeatherRadius().getValue();
         int i = MathHelper.floor(cameraPos.x);
         int j = MathHelper.floor(cameraPos.y);
         int k = MathHelper.floor(cameraPos.z);
         BlockPos.Mutable mutable = new BlockPos.Mutable();
         Random random = Random.create();

         for (int l = k - state.radius; l <= k + state.radius; l++) {
            for (int m = i - state.radius; m <= i + state.radius; m++) {
               int n = world.getTopY(Heightmap.Type.MOTION_BLOCKING, m, l);
               int o = Math.max(j - state.radius, n);
               int p = Math.max(j + state.radius, n);
               if (p - o != 0) {
                  Biome.Precipitation precipitation = this.getPrecipitationAt(world, mutable.set(m, j, l));
                  if (precipitation != Biome.Precipitation.NONE) {
                     int q = m * m * 3121 + m * 45238971 ^ l * l * 418711 + l * 13761;
                     random.setSeed(q);
                     int r = Math.max(j, n);
                     int s = WorldRenderer.getLightmapCoordinates(world, mutable.set(m, r, l));
                     if (precipitation == Biome.Precipitation.RAIN) {
                        state.rainPieces.add(this.createRainPiece(random, ticks, m, o, p, l, s, tickProgress));
                     } else if (precipitation == Biome.Precipitation.SNOW) {
                        state.snowPieces.add(this.createSnowPiece(random, ticks, m, o, p, l, s, tickProgress));
                     }
                  }
               }
            }
         }
      }
   }

   public void renderPrecipitation(VertexConsumerProvider vertexConsumers, Vec3d pos, WeatherRenderState state) {
      if (!state.rainPieces.isEmpty()) {
         RenderLayer renderLayer = RenderLayers.weather(RAIN_TEXTURE, MinecraftClient.usesImprovedTransparency());
         this.renderPieces(vertexConsumers.getBuffer(renderLayer), state.rainPieces, pos, 1.0F, state.radius, state.intensity);
      }

      if (!state.snowPieces.isEmpty()) {
         RenderLayer renderLayer = RenderLayers.weather(SNOW_TEXTURE, MinecraftClient.usesImprovedTransparency());
         this.renderPieces(vertexConsumers.getBuffer(renderLayer), state.snowPieces, pos, 0.8F, state.radius, state.intensity);
      }
   }

   private WeatherRendering.Piece createRainPiece(Random random, int ticks, int x, int yMin, int yMax, int z, int light, float tickProgress) {
      int i = ticks & 131071;
      int j = x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761 & 0xFF;
      float f = 3.0F + random.nextFloat();
      float g = -(i + j + tickProgress) / 32.0F * f;
      float h = g % 32.0F;
      return new WeatherRendering.Piece(x, z, yMin, yMax, 0.0F, h, light);
   }

   private WeatherRendering.Piece createSnowPiece(Random random, int ticks, int x, int yMin, int yMax, int z, int light, float tickProgress) {
      float f = ticks + tickProgress;
      float g = (float)(random.nextDouble() + f * 0.01F * (float)random.nextGaussian());
      float h = (float)(random.nextDouble() + f * (float)random.nextGaussian() * 0.001F);
      float i = -((ticks & 511) + tickProgress) / 512.0F;
      int j = LightmapTextureManager.pack(
         (LightmapTextureManager.getBlockLightCoordinates(light) * 3 + 15) / 4, (LightmapTextureManager.getSkyLightCoordinates(light) * 3 + 15) / 4
      );
      return new WeatherRendering.Piece(x, z, yMin, yMax, g, i + h, j);
   }

   private void renderPieces(VertexConsumer vertexConsumer, List<WeatherRendering.Piece> pieces, Vec3d pos, float intensity, int range, float gradient) {
      float f = range * range;

      for (WeatherRendering.Piece piece : pieces) {
         float g = (float)(piece.x + 0.5 - pos.x);
         float h = (float)(piece.z + 0.5 - pos.z);
         float i = (float)MathHelper.squaredHypot(g, h);
         float j = MathHelper.lerp(Math.min(i / f, 1.0F), intensity, 0.5F) * gradient;
         int k = ColorHelper.getWhite(j);
         int l = (piece.z - MathHelper.floor(pos.z) + 16) * 32 + piece.x - MathHelper.floor(pos.x) + 16;
         float m = this.NORMAL_LINE_DX[l] / 2.0F;
         float n = this.NORMAL_LINE_DZ[l] / 2.0F;
         float o = g - m;
         float p = g + m;
         float q = (float)(piece.topY - pos.y);
         float r = (float)(piece.bottomY - pos.y);
         float s = h - n;
         float t = h + n;
         float u = piece.uOffset + 0.0F;
         float v = piece.uOffset + 1.0F;
         float w = piece.bottomY * 0.25F + piece.vOffset;
         float x = piece.topY * 0.25F + piece.vOffset;
         vertexConsumer.vertex(o, q, s).texture(u, w).color(k).light(piece.lightCoords);
         vertexConsumer.vertex(p, q, t).texture(v, w).color(k).light(piece.lightCoords);
         vertexConsumer.vertex(p, r, t).texture(v, x).color(k).light(piece.lightCoords);
         vertexConsumer.vertex(o, r, s).texture(u, x).color(k).light(piece.lightCoords);
      }
   }

   public void addParticlesAndSound(ClientWorld world, Camera camera, int ticks, ParticlesMode particlesMode, int weatherRadius) {
      float f = world.getRainGradient(1.0F);
      if (!(f <= 0.0F)) {
         Random random = Random.create(ticks * 312987231L);
         BlockPos blockPos = BlockPos.ofFloored(camera.getCameraPos());
         BlockPos blockPos2 = null;
         int i = 2 * weatherRadius + 1;
         int j = i * i;
         int k = (int)(0.225F * j * f * f) / (particlesMode == ParticlesMode.DECREASED ? 2 : 1);

         for (int l = 0; l < k; l++) {
            int m = random.nextInt(i) - weatherRadius;
            int n = random.nextInt(i) - weatherRadius;
            BlockPos blockPos3 = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, blockPos.add(m, 0, n));
            if (blockPos3.getY() > world.getBottomY()
               && blockPos3.getY() <= blockPos.getY() + 10
               && blockPos3.getY() >= blockPos.getY() - 10
               && this.getPrecipitationAt(world, blockPos3) == Biome.Precipitation.RAIN) {
               blockPos2 = blockPos3.down();
               if (particlesMode == ParticlesMode.MINIMAL) {
                  break;
               }

               double d = random.nextDouble();
               double e = random.nextDouble();
               BlockState blockState = world.getBlockState(blockPos2);
               FluidState fluidState = world.getFluidState(blockPos2);
               VoxelShape voxelShape = blockState.getCollisionShape(world, blockPos2);
               double g = voxelShape.getEndingCoord(Direction.Axis.Y, d, e);
               double h = fluidState.getHeight(world, blockPos2);
               double o = Math.max(g, h);
               ParticleEffect particleEffect = !fluidState.isIn(FluidTags.LAVA)
                     && !blockState.isOf(Blocks.MAGMA_BLOCK)
                     && !CampfireBlock.isLitCampfire(blockState)
                  ? ParticleTypes.RAIN
                  : ParticleTypes.SMOKE;
               world.addParticleClient(particleEffect, blockPos2.getX() + d, blockPos2.getY() + o, blockPos2.getZ() + e, 0.0, 0.0, 0.0);
            }
         }

         if (blockPos2 != null && random.nextInt(3) < this.soundChance++) {
            this.soundChance = 0;
            if (blockPos2.getY() > blockPos.getY() + 1
               && world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, blockPos).getY() > MathHelper.floor((float)blockPos.getY())) {
               world.playSoundAtBlockCenterClient(blockPos2, SoundEvents.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, 0.1F, 0.5F, false);
            } else {
               world.playSoundAtBlockCenterClient(blockPos2, SoundEvents.WEATHER_RAIN, SoundCategory.WEATHER, 0.2F, 1.0F, false);
            }
         }
      }
   }

   private Biome.Precipitation getPrecipitationAt(World world, BlockPos pos) {
      if (!world.getChunkManager().isChunkLoaded(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()))) {
         return Biome.Precipitation.NONE;
      } else {
         Biome biome = world.getBiome(pos).value();
         return biome.getPrecipitation(pos, world.getSeaLevel());
      }
   }

   @Environment(EnvType.CLIENT)
   public record Piece(int x, int z, int bottomY, int topY, float uOffset, float vOffset, int lightCoords) {
   }
}

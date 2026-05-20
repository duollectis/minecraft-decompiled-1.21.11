package net.minecraft.client.particle;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.SubmittableBatch;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleGroup;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profilers;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ParticleManager {
   private static final List<ParticleTextureSheet> PARTICLE_TEXTURE_SHEETS = List.of(
      ParticleTextureSheet.SINGLE_QUADS, ParticleTextureSheet.ITEM_PICKUP, ParticleTextureSheet.ELDER_GUARDIANS
   );
   protected ClientWorld world;
   private final Map<ParticleTextureSheet, ParticleRenderer<?>> particles = Maps.newIdentityHashMap();
   private final Queue<EmitterParticle> newEmitterParticles = Queues.newArrayDeque();
   private final Queue<Particle> newParticles = Queues.newArrayDeque();
   private final Object2IntOpenHashMap<ParticleGroup> groupCounts = new Object2IntOpenHashMap();
   private final ParticleSpriteManager spriteManager;
   private final Random random = Random.create();

   public ParticleManager(ClientWorld world, ParticleSpriteManager spriteManager) {
      this.world = world;
      this.spriteManager = spriteManager;
   }

   public void addEmitter(Entity entity, ParticleEffect parameters) {
      this.newEmitterParticles.add(new EmitterParticle(this.world, entity, parameters));
   }

   public void addEmitter(Entity entity, ParticleEffect parameters, int maxAge) {
      this.newEmitterParticles.add(new EmitterParticle(this.world, entity, parameters, maxAge));
   }

   public @Nullable Particle addParticle(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
      Particle particle = this.createParticle(parameters, x, y, z, velocityX, velocityY, velocityZ);
      if (particle != null) {
         this.addParticle(particle);
         return particle;
      } else {
         return null;
      }
   }

   private <T extends ParticleEffect> @Nullable Particle createParticle(
      T parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ
   ) {
      ParticleFactory<T> particleFactory = (ParticleFactory<T>)this.spriteManager
         .getParticleFactories()
         .get(Registries.PARTICLE_TYPE.getRawId(parameters.getType()));
      return particleFactory == null ? null : particleFactory.createParticle(parameters, this.world, x, y, z, velocityX, velocityY, velocityZ, this.random);
   }

   public void addParticle(Particle particle) {
      Optional<ParticleGroup> optional = particle.getGroup();
      if (optional.isPresent()) {
         if (this.canAdd(optional.get())) {
            this.newParticles.add(particle);
            this.addTo(optional.get(), 1);
         }
      } else {
         this.newParticles.add(particle);
      }
   }

   public void tick() {
      this.particles.forEach((textureSheet, particlex) -> {
         Profilers.get().push(textureSheet.name());
         particlex.tick();
         Profilers.get().pop();
      });
      if (!this.newEmitterParticles.isEmpty()) {
         List<EmitterParticle> list = Lists.newArrayList();

         for (EmitterParticle emitterParticle : this.newEmitterParticles) {
            emitterParticle.tick();
            if (!emitterParticle.isAlive()) {
               list.add(emitterParticle);
            }
         }

         this.newEmitterParticles.removeAll(list);
      }

      Particle particle;
      if (!this.newParticles.isEmpty()) {
         while ((particle = this.newParticles.poll()) != null) {
            this.particles.computeIfAbsent(particle.textureSheet(), this::createParticleRenderer).add(particle);
         }
      }
   }

   private ParticleRenderer<?> createParticleRenderer(ParticleTextureSheet textureSheet) {
      if (textureSheet == ParticleTextureSheet.ITEM_PICKUP) {
         return new ItemPickupParticleRenderer(this);
      } else if (textureSheet == ParticleTextureSheet.ELDER_GUARDIANS) {
         return new ElderGuardianParticleRenderer(this);
      } else {
         return (ParticleRenderer<?>)(textureSheet == ParticleTextureSheet.NO_RENDER
            ? new NoRenderParticleRenderer(this)
            : new BillboardParticleRenderer(this, textureSheet));
      }
   }

   protected void addTo(ParticleGroup group, int count) {
      this.groupCounts.addTo(group, count);
   }

   public void addToBatch(SubmittableBatch batch, Frustum frustum, Camera camera, float tickProgress) {
      for (ParticleTextureSheet particleTextureSheet : PARTICLE_TEXTURE_SHEETS) {
         ParticleRenderer<?> particleRenderer = this.particles.get(particleTextureSheet);
         if (particleRenderer != null && !particleRenderer.isEmpty()) {
            batch.add(particleRenderer.render(frustum, camera, tickProgress));
         }
      }
   }

   public void setWorld(@Nullable ClientWorld world) {
      this.world = world;
      this.clearParticles();
      this.newEmitterParticles.clear();
   }

   public String getDebugString() {
      return String.valueOf(this.particles.values().stream().mapToInt(ParticleRenderer::size).sum());
   }

   private boolean canAdd(ParticleGroup group) {
      return this.groupCounts.getInt(group) < group.maxCount();
   }

   public void clearParticles() {
      this.particles.clear();
      this.newParticles.clear();
      this.newEmitterParticles.clear();
      this.groupCounts.clear();
   }
}

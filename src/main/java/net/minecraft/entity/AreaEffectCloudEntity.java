package net.minecraft.entity;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

public class AreaEffectCloudEntity extends Entity implements Ownable {
   private static final int field_29972 = 5;
   private static final TrackedData<Float> RADIUS = DataTracker.registerData(AreaEffectCloudEntity.class, TrackedDataHandlerRegistry.FLOAT);
   private static final TrackedData<Boolean> WAITING = DataTracker.registerData(AreaEffectCloudEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
   private static final TrackedData<ParticleEffect> PARTICLE = DataTracker.registerData(AreaEffectCloudEntity.class, TrackedDataHandlerRegistry.PARTICLE);
   private static final float MAX_RADIUS = 32.0F;
   private static final int field_57566 = 0;
   private static final int field_57567 = 0;
   private static final float DEFAULT_RADIUS_ON_USE = 0.0F;
   private static final float DEFAULT_RADIUS_GROWTH = 0.0F;
   private static final float field_57570 = 1.0F;
   private static final float field_40730 = 0.5F;
   private static final float DEFAULT_RADIUS = 3.0F;
   public static final float field_40732 = 6.0F;
   public static final float field_40733 = 0.5F;
   public static final int DEFAULT_DURATION = -1;
   public static final int field_57565 = 600;
   private static final int DEFAULT_WAIT_TIME = 20;
   private static final int DEFAULT_REAPPLICATION_DELAY = 20;
   private static final TintedParticleEffect DEFAULT_PARTICLE_EFFECT = TintedParticleEffect.create(ParticleTypes.ENTITY_EFFECT, -1);
   private @Nullable ParticleEffect customParticle;
   private PotionContentsComponent potionContentsComponent = PotionContentsComponent.DEFAULT;
   private float potionDurationScale = 1.0F;
   private final Map<Entity, Integer> affectedEntities = Maps.newHashMap();
   private int duration = -1;
   private int waitTime = 20;
   private int reapplicationDelay = 20;
   private int durationOnUse = 0;
   private float radiusOnUse = 0.0F;
   private float radiusGrowth = 0.0F;
   private @Nullable LazyEntityReference<LivingEntity> owner;

   public AreaEffectCloudEntity(EntityType<? extends AreaEffectCloudEntity> entityType, World world) {
      super(entityType, world);
      this.noClip = true;
   }

   public AreaEffectCloudEntity(World world, double x, double y, double z) {
      this(EntityType.AREA_EFFECT_CLOUD, world);
      this.setPosition(x, y, z);
   }

   @Override
   protected void initDataTracker(DataTracker.Builder builder) {
      builder.add(RADIUS, 3.0F);
      builder.add(WAITING, false);
      builder.add(PARTICLE, DEFAULT_PARTICLE_EFFECT);
   }

   public void setRadius(float radius) {
      if (!this.getEntityWorld().isClient()) {
         this.getDataTracker().set(RADIUS, MathHelper.clamp(radius, 0.0F, 32.0F));
      }
   }

   @Override
   public void calculateDimensions() {
      double d = this.getX();
      double e = this.getY();
      double f = this.getZ();
      super.calculateDimensions();
      this.setPosition(d, e, f);
   }

   public float getRadius() {
      return this.getDataTracker().get(RADIUS);
   }

   public void setPotionContents(PotionContentsComponent potionContentsComponent) {
      this.potionContentsComponent = potionContentsComponent;
      this.updateParticle();
   }

   public void setParticleType(@Nullable ParticleEffect customParticle) {
      this.customParticle = customParticle;
      this.updateParticle();
   }

   public void setPotionDurationScale(float potionDurationScale) {
      this.potionDurationScale = potionDurationScale;
   }

   private void updateParticle() {
      if (this.customParticle != null) {
         this.dataTracker.set(PARTICLE, this.customParticle);
      } else {
         int i = ColorHelper.fullAlpha(this.potionContentsComponent.getColor());
         this.dataTracker.set(PARTICLE, TintedParticleEffect.create(DEFAULT_PARTICLE_EFFECT.getType(), i));
      }
   }

   public void addEffect(StatusEffectInstance effect) {
      this.setPotionContents(this.potionContentsComponent.with(effect));
   }

   public ParticleEffect getParticleType() {
      return this.getDataTracker().get(PARTICLE);
   }

   protected void setWaiting(boolean waiting) {
      this.getDataTracker().set(WAITING, waiting);
   }

   public boolean isWaiting() {
      return this.getDataTracker().get(WAITING);
   }

   public int getDuration() {
      return this.duration;
   }

   public void setDuration(int duration) {
      this.duration = duration;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
         this.serverTick(serverWorld);
      } else {
         this.clientTick();
      }
   }

   private void clientTick() {
      boolean bl = this.isWaiting();
      float f = this.getRadius();
      if (!bl || !this.random.nextBoolean()) {
         ParticleEffect particleEffect = this.getParticleType();
         int i;
         float g;
         if (bl) {
            i = 2;
            g = 0.2F;
         } else {
            i = MathHelper.ceil((float) Math.PI * f * f);
            g = f;
         }

         for (int j = 0; j < i; j++) {
            float h = this.random.nextFloat() * (float) (Math.PI * 2);
            float k = MathHelper.sqrt(this.random.nextFloat()) * g;
            double d = this.getX() + MathHelper.cos(h) * k;
            double e = this.getY();
            double l = this.getZ() + MathHelper.sin(h) * k;
            if (particleEffect.getType() == ParticleTypes.ENTITY_EFFECT) {
               if (bl && this.random.nextBoolean()) {
                  this.getEntityWorld().addImportantParticleClient(DEFAULT_PARTICLE_EFFECT, d, e, l, 0.0, 0.0, 0.0);
               } else {
                  this.getEntityWorld().addImportantParticleClient(particleEffect, d, e, l, 0.0, 0.0, 0.0);
               }
            } else if (bl) {
               this.getEntityWorld().addImportantParticleClient(particleEffect, d, e, l, 0.0, 0.0, 0.0);
            } else {
               this.getEntityWorld()
                  .addImportantParticleClient(particleEffect, d, e, l, (0.5 - this.random.nextDouble()) * 0.15, 0.01F, (0.5 - this.random.nextDouble()) * 0.15);
            }
         }
      }
   }

   private void serverTick(ServerWorld world) {
      if (this.duration != -1 && this.age - this.waitTime >= this.duration) {
         this.discard();
      } else {
         boolean bl = this.isWaiting();
         boolean bl2 = this.age < this.waitTime;
         if (bl != bl2) {
            this.setWaiting(bl2);
         }

         if (!bl2) {
            float f = this.getRadius();
            if (this.radiusGrowth != 0.0F) {
               f += this.radiusGrowth;
               if (f < 0.5F) {
                  this.discard();
                  return;
               }

               this.setRadius(f);
            }

            if (this.age % 5 == 0) {
               this.affectedEntities.entrySet().removeIf(entity -> this.age >= entity.getValue());
               if (!this.potionContentsComponent.hasEffects()) {
                  this.affectedEntities.clear();
               } else {
                  List<StatusEffectInstance> list = new ArrayList<>();
                  this.potionContentsComponent.forEachEffect(list::add, this.potionDurationScale);
                  List<LivingEntity> list2 = this.getEntityWorld().getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox());
                  if (!list2.isEmpty()) {
                     for (LivingEntity livingEntity : list2) {
                        if (!this.affectedEntities.containsKey(livingEntity)
                           && livingEntity.isAffectedBySplashPotions()
                           && !list.stream().noneMatch(livingEntity::canHaveStatusEffect)) {
                           double d = livingEntity.getX() - this.getX();
                           double e = livingEntity.getZ() - this.getZ();
                           double g = d * d + e * e;
                           if (g <= f * f) {
                              this.affectedEntities.put(livingEntity, this.age + this.reapplicationDelay);

                              for (StatusEffectInstance statusEffectInstance : list) {
                                 if (statusEffectInstance.getEffectType().value().isInstant()) {
                                    statusEffectInstance.getEffectType()
                                       .value()
                                       .applyInstantEffect(world, this, this.getOwner(), livingEntity, statusEffectInstance.getAmplifier(), 0.5);
                                 } else {
                                    livingEntity.addStatusEffect(new StatusEffectInstance(statusEffectInstance), this);
                                 }
                              }

                              if (this.radiusOnUse != 0.0F) {
                                 f += this.radiusOnUse;
                                 if (f < 0.5F) {
                                    this.discard();
                                    return;
                                 }

                                 this.setRadius(f);
                              }

                              if (this.durationOnUse != 0 && this.duration != -1) {
                                 this.duration = this.duration + this.durationOnUse;
                                 if (this.duration <= 0) {
                                    this.discard();
                                    return;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public float getRadiusOnUse() {
      return this.radiusOnUse;
   }

   public void setRadiusOnUse(float radiusOnUse) {
      this.radiusOnUse = radiusOnUse;
   }

   public float getRadiusGrowth() {
      return this.radiusGrowth;
   }

   public void setRadiusGrowth(float radiusGrowth) {
      this.radiusGrowth = radiusGrowth;
   }

   public int getDurationOnUse() {
      return this.durationOnUse;
   }

   public void setDurationOnUse(int durationOnUse) {
      this.durationOnUse = durationOnUse;
   }

   public int getWaitTime() {
      return this.waitTime;
   }

   public void setWaitTime(int waitTime) {
      this.waitTime = waitTime;
   }

   public void setOwner(@Nullable LivingEntity owner) {
      this.owner = LazyEntityReference.of(owner);
   }

   public @Nullable LivingEntity getOwner() {
      return LazyEntityReference.getLivingEntity(this.owner, this.getEntityWorld());
   }

   @Override
   protected void readCustomData(ReadView view) {
      this.age = view.getInt("Age", 0);
      this.duration = view.getInt("Duration", -1);
      this.waitTime = view.getInt("WaitTime", 20);
      this.reapplicationDelay = view.getInt("ReapplicationDelay", 20);
      this.durationOnUse = view.getInt("DurationOnUse", 0);
      this.radiusOnUse = view.getFloat("RadiusOnUse", 0.0F);
      this.radiusGrowth = view.getFloat("RadiusPerTick", 0.0F);
      this.setRadius(view.getFloat("Radius", 3.0F));
      this.owner = LazyEntityReference.fromData(view, "Owner");
      this.setParticleType(view.<ParticleEffect>read("custom_particle", ParticleTypes.TYPE_CODEC).orElse(null));
      this.setPotionContents(view.<PotionContentsComponent>read("potion_contents", PotionContentsComponent.CODEC).orElse(PotionContentsComponent.DEFAULT));
      this.potionDurationScale = view.getFloat("potion_duration_scale", 1.0F);
   }

   @Override
   protected void writeCustomData(WriteView view) {
      view.putInt("Age", this.age);
      view.putInt("Duration", this.duration);
      view.putInt("WaitTime", this.waitTime);
      view.putInt("ReapplicationDelay", this.reapplicationDelay);
      view.putInt("DurationOnUse", this.durationOnUse);
      view.putFloat("RadiusOnUse", this.radiusOnUse);
      view.putFloat("RadiusPerTick", this.radiusGrowth);
      view.putFloat("Radius", this.getRadius());
      view.putNullable("custom_particle", ParticleTypes.TYPE_CODEC, this.customParticle);
      LazyEntityReference.writeData(this.owner, view, "Owner");
      if (!this.potionContentsComponent.equals(PotionContentsComponent.DEFAULT)) {
         view.put("potion_contents", PotionContentsComponent.CODEC, this.potionContentsComponent);
      }

      if (this.potionDurationScale != 1.0F) {
         view.putFloat("potion_duration_scale", this.potionDurationScale);
      }
   }

   @Override
   public void onTrackedDataSet(TrackedData<?> data) {
      if (RADIUS.equals(data)) {
         this.calculateDimensions();
      }

      super.onTrackedDataSet(data);
   }

   @Override
   public PistonBehavior getPistonBehavior() {
      return PistonBehavior.IGNORE;
   }

   @Override
   public EntityDimensions getDimensions(EntityPose pose) {
      return EntityDimensions.changing(this.getRadius() * 2.0F, 0.5F);
   }

   @Override
   public final boolean damage(ServerWorld world, DamageSource source, float amount) {
      return false;
   }

   @Override
   public <T> @Nullable T get(ComponentType<? extends T> type) {
      if (type == DataComponentTypes.POTION_CONTENTS) {
         return castComponentValue((ComponentType<T>)type, this.potionContentsComponent);
      } else {
         return type == DataComponentTypes.POTION_DURATION_SCALE ? castComponentValue((ComponentType<T>)type, this.potionDurationScale) : super.get(type);
      }
   }

   @Override
   protected void copyComponentsFrom(ComponentsAccess from) {
      this.copyComponentFrom(from, DataComponentTypes.POTION_CONTENTS);
      this.copyComponentFrom(from, DataComponentTypes.POTION_DURATION_SCALE);
      super.copyComponentsFrom(from);
   }

   @Override
   protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
      if (type == DataComponentTypes.POTION_CONTENTS) {
         this.setPotionContents(castComponentValue(DataComponentTypes.POTION_CONTENTS, value));
         return true;
      } else if (type == DataComponentTypes.POTION_DURATION_SCALE) {
         this.setPotionDurationScale(castComponentValue(DataComponentTypes.POTION_DURATION_SCALE, value));
         return true;
      } else {
         return super.setApplicableComponent(type, value);
      }
   }
}

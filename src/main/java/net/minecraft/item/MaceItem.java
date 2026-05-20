package net.minecraft.item;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

public class MaceItem extends Item {
   private static final int ATTACK_DAMAGE_MODIFIER_VALUE = 5;
   private static final float ATTACK_SPEED_MODIFIER_VALUE = -3.4F;
   public static final float MINING_SPEED_MULTIPLIER = 1.5F;
   private static final float HEAVY_SMASH_SOUND_FALL_DISTANCE_THRESHOLD = 5.0F;
   public static final float KNOCKBACK_RANGE = 3.5F;
   private static final float KNOCKBACK_POWER = 0.7F;

   public MaceItem(Item.Settings settings) {
      super(settings);
   }

   public static AttributeModifiersComponent createAttributeModifiers() {
      return AttributeModifiersComponent.builder()
         .add(
            EntityAttributes.ATTACK_DAMAGE,
            new EntityAttributeModifier(BASE_ATTACK_DAMAGE_MODIFIER_ID, 5.0, EntityAttributeModifier.Operation.ADD_VALUE),
            AttributeModifierSlot.MAINHAND
         )
         .add(
            EntityAttributes.ATTACK_SPEED,
            new EntityAttributeModifier(BASE_ATTACK_SPEED_MODIFIER_ID, -3.4F, EntityAttributeModifier.Operation.ADD_VALUE),
            AttributeModifierSlot.MAINHAND
         )
         .build();
   }

   public static ToolComponent createToolComponent() {
      return new ToolComponent(List.of(), 1.0F, 2, false);
   }

   @Override
   public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      if (shouldDealAdditionalDamage(attacker)) {
         ServerWorld serverWorld = (ServerWorld)attacker.getEntityWorld();
         attacker.setVelocity(attacker.getVelocity().withAxis(Direction.Axis.Y, 0.01F));
         if (attacker instanceof ServerPlayerEntity serverPlayerEntity) {
            serverPlayerEntity.currentExplosionImpactPos = this.getCurrentExplosionImpactPos(serverPlayerEntity);
            serverPlayerEntity.setIgnoreFallDamageFromCurrentExplosion(true);
            serverPlayerEntity.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverPlayerEntity));
         }

         if (target.isOnGround()) {
            if (attacker instanceof ServerPlayerEntity serverPlayerEntity) {
               serverPlayerEntity.setSpawnExtraParticlesOnFall(true);
            }

            SoundEvent soundEvent = attacker.fallDistance > 5.0 ? SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY : SoundEvents.ITEM_MACE_SMASH_GROUND;
            serverWorld.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), soundEvent, attacker.getSoundCategory(), 1.0F, 1.0F);
         } else {
            serverWorld.playSound(
               null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.ITEM_MACE_SMASH_AIR, attacker.getSoundCategory(), 1.0F, 1.0F
            );
         }

         knockbackNearbyEntities(serverWorld, attacker, target);
      }
   }

   private Vec3d getCurrentExplosionImpactPos(ServerPlayerEntity player) {
      return player.shouldIgnoreFallDamageFromCurrentExplosion()
            && player.currentExplosionImpactPos != null
            && player.currentExplosionImpactPos.y <= player.getEntityPos().y
         ? player.currentExplosionImpactPos
         : player.getEntityPos();
   }

   @Override
   public void postDamageEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      if (shouldDealAdditionalDamage(attacker)) {
         attacker.onLanding();
      }
   }

   @Override
   public float getBonusAttackDamage(Entity target, float baseAttackDamage, DamageSource damageSource) {
      if (damageSource.getSource() instanceof LivingEntity livingEntity) {
         if (!shouldDealAdditionalDamage(livingEntity)) {
            return 0.0F;
         } else {
            double d = 3.0;
            double e = 8.0;
            double f = livingEntity.fallDistance;
            double g;
            if (f <= 3.0) {
               g = 4.0 * f;
            } else if (f <= 8.0) {
               g = 12.0 + 2.0 * (f - 3.0);
            } else {
               g = 22.0 + f - 8.0;
            }

            return livingEntity.getEntityWorld() instanceof ServerWorld serverWorld
               ? (float)(g + EnchantmentHelper.getSmashDamagePerFallenBlock(serverWorld, livingEntity.getWeaponStack(), target, damageSource, 0.0F) * f)
               : (float)g;
         }
      } else {
         return 0.0F;
      }
   }

   private static void knockbackNearbyEntities(World world, Entity attacker, Entity attacked) {
      world.syncWorldEvent(2013, attacked.getSteppingPos(), 750);
      world.getEntitiesByClass(LivingEntity.class, attacked.getBoundingBox().expand(3.5), getKnockbackPredicate(attacker, attacked)).forEach(entity -> {
         Vec3d vec3d = entity.getEntityPos().subtract(attacked.getEntityPos());
         double d = getKnockback(attacker, entity, vec3d);
         Vec3d vec3d2 = vec3d.normalize().multiply(d);
         if (d > 0.0) {
            entity.addVelocity(vec3d2.x, 0.7F, vec3d2.z);
            if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
               serverPlayerEntity.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverPlayerEntity));
            }
         }
      });
   }

   private static Predicate<LivingEntity> getKnockbackPredicate(Entity attacker, Entity attacked) {
      return entity -> {
         boolean bl = !entity.isSpectator();
         boolean bl2 = entity != attacker && entity != attacked;
         boolean bl3 = !attacker.isTeammate(entity);
         boolean bl4 = !(
            entity instanceof TameableEntity tameableEntity
               && attacked instanceof LivingEntity livingEntity
               && tameableEntity.isTamed()
               && tameableEntity.isOwner(livingEntity)
         );
         boolean bl5 = !(entity instanceof ArmorStandEntity armorStandEntity && armorStandEntity.isMarker());
         boolean bl6 = attacked.squaredDistanceTo(entity) <= Math.pow(3.5, 2.0);
         boolean bl7 = !(entity instanceof PlayerEntity playerEntity && playerEntity.isCreative() && playerEntity.getAbilities().flying);
         return bl && bl2 && bl3 && bl4 && bl5 && bl6 && bl7;
      };
   }

   private static double getKnockback(Entity attacker, LivingEntity attacked, Vec3d distance) {
      return (3.5 - distance.length())
         * 0.7F
         * (attacker.fallDistance > 5.0 ? 2 : 1)
         * (1.0 - attacked.getAttributeValue(EntityAttributes.KNOCKBACK_RESISTANCE));
   }

   public static boolean shouldDealAdditionalDamage(LivingEntity attacker) {
      return attacker.fallDistance > 1.5 && !attacker.isGliding();
   }

   @Override
   public @Nullable DamageSource getDamageSource(LivingEntity user) {
      return shouldDealAdditionalDamage(user) ? user.getDamageSources().maceSmash(user) : super.getDamageSource(user);
   }
}

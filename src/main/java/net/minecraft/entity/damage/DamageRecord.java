package net.minecraft.entity.damage;

import org.jspecify.annotations.Nullable;

public record DamageRecord(DamageSource damageSource, float damage, @Nullable FallLocation fallLocation, float fallDistance) {
}

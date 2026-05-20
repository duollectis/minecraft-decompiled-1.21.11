package net.minecraft.world.entity;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public interface EntityQueriable<IdentifiedType extends UniquelyIdentifiable> {
   @Nullable IdentifiedType lookup(UUID uUID);
}

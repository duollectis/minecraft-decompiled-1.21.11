package net.minecraft.world.entity;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * {@code EntityQueriable}.
 */
public interface EntityQueriable<IdentifiedType extends UniquelyIdentifiable> {

	@Nullable IdentifiedType lookup(UUID uUID);
}

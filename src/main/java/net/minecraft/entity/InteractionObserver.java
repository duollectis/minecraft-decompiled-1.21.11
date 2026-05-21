package net.minecraft.entity;

/**
 * {@code InteractionObserver}.
 */
public interface InteractionObserver {

	void onInteractionWith(EntityInteraction interaction, Entity entity);
}

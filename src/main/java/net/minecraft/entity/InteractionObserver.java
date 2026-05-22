package net.minecraft.entity;

/**
 * Интерфейс для сущностей, способных наблюдать за взаимодействиями с другими сущностями.
 */
public interface InteractionObserver {

	void onInteractionWith(EntityInteraction interaction, Entity entity);
}

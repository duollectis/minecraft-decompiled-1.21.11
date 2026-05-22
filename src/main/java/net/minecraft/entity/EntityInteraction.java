package net.minecraft.entity;

/**
 * Тип взаимодействия между сущностями, используемый для обновления репутации
 * и отношений в системе деревенских жителей.
 * Каждый экземпляр создаётся через {@link #create(String)} и имеет строковое представление
 * для отладки и логирования.
 */
public interface EntityInteraction {

	EntityInteraction ZOMBIE_VILLAGER_CURED = create("zombie_villager_cured");
	EntityInteraction GOLEM_KILLED = create("golem_killed");
	EntityInteraction VILLAGER_HURT = create("villager_hurt");
	EntityInteraction VILLAGER_KILLED = create("villager_killed");
	EntityInteraction TRADE = create("trade");

	static EntityInteraction create(String key) {
		return new EntityInteraction() {
			@Override
			public String toString() {
				return key;
			}
		};
	}
}

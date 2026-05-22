package net.minecraft.village;

/**
 * Контракт для сущностей, хранящих данные жителя деревни.
 * <p>
 * Реализуется жителями и зомби-жителями для унифицированного доступа
 * к типу, профессии и уровню через общий интерфейс.
 */
public interface VillagerDataContainer {

	VillagerData getVillagerData();

	void setVillagerData(VillagerData villagerData);
}

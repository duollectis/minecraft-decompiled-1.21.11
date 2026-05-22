package net.minecraft.entity.conversion;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.scoreboard.Team;
import org.jspecify.annotations.Nullable;

/**
 * Контекст преобразования сущности: хранит параметры, управляющие тем,
 * как именно старая сущность конвертируется в новую (тип конверсии,
 * сохранение снаряжения, подбор лута, команда скорборда).
 */
public record EntityConversionContext(
	EntityConversionType type,
	boolean keepEquipment,
	boolean preserveCanPickUpLoot,
	@Nullable Team team
) {

	/**
	 * Создаёт контекст для одиночной конверсии {@link EntityConversionType#SINGLE},
	 * автоматически подтягивая команду скорборда из исходной сущности.
	 *
	 * @param entity исходная сущность, из которой берётся команда
	 * @param keepEquipment сохранять ли снаряжение при конверсии
	 * @param preserveCanPickUpLoot сохранять ли флаг подбора лута
	 * @return готовый контекст конверсии
	 */
	public static EntityConversionContext create(
		MobEntity entity,
		boolean keepEquipment,
		boolean preserveCanPickUpLoot
	) {
		return new EntityConversionContext(
			EntityConversionType.SINGLE,
			keepEquipment,
			preserveCanPickUpLoot,
			entity.getScoreboardTeam()
		);
	}

	/**
	 * Колбэк, вызываемый после завершения конверсии для дополнительной
	 * настройки новой сущности (например, установки специфичных данных).
	 *
	 * @param <T> тип конвертированной сущности
	 */
	@FunctionalInterface
	public interface Finalizer<T extends MobEntity> {

		void finalizeConversion(T convertedEntity);
	}
}

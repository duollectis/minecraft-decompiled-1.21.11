package net.minecraft.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;

import java.util.UUID;

/**
 * Генератор псевдослучайных имён для сущностей на основе их UUID.
 * Имена составляются из фиксированных массивов префиксов и суффиксов,
 * что обеспечивает детерминированность: одинаковый UUID всегда даёт одинаковое имя.
 */
public class NameGenerator {

	private static final String[] PREFIX = {
		"Slim", "Far", "River", "Silly", "Fat", "Thin", "Fish", "Bat",
		"Dark", "Oak", "Sly", "Bush", "Zen", "Bark", "Cry", "Slack",
		"Soup", "Grim", "Hook", "Dirt", "Mud", "Sad", "Hard", "Crook",
		"Sneak", "Stink", "Weird", "Fire", "Soot", "Soft", "Rough", "Cling", "Scar"
	};

	private static final String[] SUFFIX = {
		"Fox", "Tail", "Jaw", "Whisper", "Twig", "Root", "Finder", "Nose",
		"Brow", "Blade", "Fry", "Seek", "Wart", "Tooth", "Foot", "Leaf",
		"Stone", "Fall", "Face", "Tongue", "Voice", "Lip", "Mouth", "Snail",
		"Toe", "Ear", "Hair", "Beard", "Shirt", "Fist"
	};

	/**
	 * Возвращает имя сущности: для игроков — их игровое имя,
	 * для остальных — пользовательское имя или сгенерированное по UUID.
	 *
	 * @param entity сущность
	 * @return строковое имя сущности
	 */
	public static String name(Entity entity) {
		if (entity instanceof PlayerEntity) {
			return entity.getStringifiedName();
		}

		Text customName = entity.getCustomName();
		return customName != null ? customName.getString() : name(entity.getUuid());
	}

	/**
	 * Генерирует детерминированное имя по UUID.
	 * Использует хэш UUID как зерно генератора случайных чисел.
	 *
	 * @param uuid идентификатор сущности
	 * @return сгенерированное имя вида «ПрефиксСуффикс»
	 */
	public static String name(UUID uuid) {
		Random random = randomFromUuid(uuid);
		return Util.getRandom(PREFIX, random) + Util.getRandom(SUFFIX, random);
	}

	private static Random randomFromUuid(UUID uuid) {
		return Random.create(uuid.hashCode() >> 2);
	}
}

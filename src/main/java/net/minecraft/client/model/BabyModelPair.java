package net.minecraft.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code BabyModelPair}.
 */
public record BabyModelPair<T extends Model>(T adultModel, T babyModel) {

	/**
	 * Get.
	 *
	 * @param baby baby
	 *
	 * @return T — 
	 */
	public T get(boolean baby) {
		return baby ? this.babyModel : this.adultModel;
	}
}

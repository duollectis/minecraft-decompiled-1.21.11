package net.minecraft.structure;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.structure.pool.StructurePool;

/**
 * Описывает точку соединения (стык) между двумя jigsaw-фрагментами структуры.
 * Хранит координаты источника стыка, вертикальное смещение и тип проекции
 * целевого пула, что необходимо для корректного расчёта плотности шума при генерации.
 */
public class JigsawJunction {

	private final int sourceX;
	private final int sourceGroundY;
	private final int sourceZ;
	private final int deltaY;
	private final StructurePool.Projection destProjection;

	public JigsawJunction(
		int sourceX,
		int sourceGroundY,
		int sourceZ,
		int deltaY,
		StructurePool.Projection destProjection
	) {
		this.sourceX = sourceX;
		this.sourceGroundY = sourceGroundY;
		this.sourceZ = sourceZ;
		this.deltaY = deltaY;
		this.destProjection = destProjection;
	}

	public int getSourceX() {
		return sourceX;
	}

	public int getSourceGroundY() {
		return sourceGroundY;
	}

	public int getSourceZ() {
		return sourceZ;
	}

	public int getDeltaY() {
		return deltaY;
	}

	public StructurePool.Projection getDestProjection() {
		return destProjection;
	}

	/**
	 * Сериализует стык в динамическую структуру данных для записи в NBT или JSON.
	 *
	 * @param ops операции над целевым форматом данных
	 * @param <T> тип целевого формата
	 * @return динамическое представление стыка
	 */
	public <T> Dynamic<T> serialize(DynamicOps<T> ops) {
		ImmutableMap.Builder<T, T> builder = ImmutableMap.builder();
		builder
			.put(ops.createString("source_x"), ops.createInt(sourceX))
			.put(ops.createString("source_ground_y"), ops.createInt(sourceGroundY))
			.put(ops.createString("source_z"), ops.createInt(sourceZ))
			.put(ops.createString("delta_y"), ops.createInt(deltaY))
			.put(ops.createString("dest_proj"), ops.createString(destProjection.getId()));
		return new Dynamic<>(ops, ops.createMap(builder.build()));
	}

	/**
	 * Десериализует стык из динамической структуры данных.
	 *
	 * @param dynamic динамическое представление стыка
	 * @param <T> тип исходного формата
	 * @return восстановленный объект {@code JigsawJunction}
	 */
	public static <T> JigsawJunction deserialize(Dynamic<T> dynamic) {
		return new JigsawJunction(
			dynamic.get("source_x").asInt(0),
			dynamic.get("source_ground_y").asInt(0),
			dynamic.get("source_z").asInt(0),
			dynamic.get("delta_y").asInt(0),
			StructurePool.Projection.getById(dynamic.get("dest_proj").asString(""))
		);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		JigsawJunction other = (JigsawJunction) o;
		return sourceX == other.sourceX
			&& sourceGroundY == other.sourceGroundY
			&& sourceZ == other.sourceZ
			&& deltaY == other.deltaY
			&& destProjection == other.destProjection;
	}

	@Override
	public int hashCode() {
		int result = sourceX;
		result = 31 * result + sourceGroundY;
		result = 31 * result + sourceZ;
		result = 31 * result + deltaY;
		return 31 * result + destProjection.hashCode();
	}

	@Override
	public String toString() {
		return "JigsawJunction{"
			+ "sourceX=" + sourceX
			+ ", sourceGroundY=" + sourceGroundY
			+ ", sourceZ=" + sourceZ
			+ ", deltaY=" + deltaY
			+ ", destProjection=" + destProjection
			+ "}";
	}
}

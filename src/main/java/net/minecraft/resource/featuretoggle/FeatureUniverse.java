package net.minecraft.resource.featuretoggle;

/**
 * Именованная вселенная флагов функций.
 *
 * <p>Служит идентификатором пространства имён для {@link FeatureFlag} и {@link FeatureSet}.
 * Флаги из разных вселенных несовместимы — попытка объединить их вызовет исключение.
 * Сравнение вселенных выполняется по ссылке ({@code ==}), а не по имени.</p>
 */
public class FeatureUniverse {

	private final String name;

	public FeatureUniverse(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}

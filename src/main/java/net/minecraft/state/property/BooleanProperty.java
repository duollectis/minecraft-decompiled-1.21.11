package net.minecraft.state.property;

import java.util.List;
import java.util.Optional;

/**
 * Свойство с булевым значением ({@code true} / {@code false}).
 *
 * <p>Порядок значений фиксирован: {@code true} имеет порядковый номер {@value TRUE_ORDINAL},
 * {@code false} — {@value FALSE_ORDINAL}. Это соответствует порядку в {@link #VALUES}
 * и используется для O(1)-индексации в кэше переходов состояний.
 */
public final class BooleanProperty extends Property<Boolean> {

	private static final List<Boolean> VALUES = List.of(true, false);
	private static final int TRUE_ORDINAL = 0;
	private static final int FALSE_ORDINAL = 1;

	private BooleanProperty(String name) {
		super(name, Boolean.class);
	}

	public static BooleanProperty of(String name) {
		return new BooleanProperty(name);
	}

	@Override
	public List<Boolean> getValues() {
		return VALUES;
	}

	@Override
	public Optional<Boolean> parse(String name) {
		return switch (name) {
			case "true" -> Optional.of(true);
			case "false" -> Optional.of(false);
			default -> Optional.empty();
		};
	}

	@Override
	public String name(Boolean value) {
		return value.toString();
	}

	@Override
	public int ordinal(Boolean value) {
		return value ? TRUE_ORDINAL : FALSE_ORDINAL;
	}
}

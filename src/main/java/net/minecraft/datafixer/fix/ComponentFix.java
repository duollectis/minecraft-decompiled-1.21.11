package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Базовый класс для фиксов, трансформирующих отдельный компонент предмета.
 * Находит компонент по {@code oldComponentId}, применяет {@link #fixComponent} и
 * записывает результат под {@code newComponentId} (может совпадать со старым).
 */
public abstract class ComponentFix extends DataFix {

	private final String name;
	private final String oldComponentId;
	private final String newComponentId;

	public ComponentFix(Schema outputSchema, String name, String componentId) {
		this(outputSchema, name, componentId, componentId);
	}

	public ComponentFix(Schema outputSchema, String name, String oldComponentId, String newComponentId) {
		super(outputSchema, false);
		this.name = name;
		this.oldComponentId = oldComponentId;
		this.newComponentId = newComponentId;
	}

	public final TypeRewriteRule makeRule() {
		Type<?> dataComponentsType = getInputSchema().getType(TypeReferences.DATA_COMPONENTS);

		return fixTypeEverywhereTyped(
				name,
				dataComponentsType,
				typed -> typed.update(
						DSL.remainderFinder(),
						dynamic -> {
							Optional<? extends Dynamic<?>> componentValue = dynamic.get(oldComponentId).result();

							if (componentValue.isEmpty()) {
								return dynamic;
							}

							Dynamic<?> fixed = fixComponent(componentValue.get());
							return dynamic
									.remove(oldComponentId)
									.setFieldIfPresent(newComponentId, Optional.ofNullable(fixed));
						}
				)
		);
	}

	protected abstract <T> @Nullable Dynamic<T> fixComponent(Dynamic<T> component);
}

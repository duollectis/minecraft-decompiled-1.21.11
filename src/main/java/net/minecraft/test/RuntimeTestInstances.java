package net.minecraft.test;

import com.google.common.collect.Sets;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Реестр тестовых инстансов, зарегистрированных в рантайме (не через датапаки).
 * Используется для динамической регистрации тестов во время работы сервера.
 */
public class RuntimeTestInstances {

	private static final Set<RegistryEntry.Reference<TestInstance>> INSTANCES = Sets.newHashSet();

	public static Stream<RegistryEntry.Reference<TestInstance>> stream() {
		return INSTANCES.stream();
	}

	public static void add(RegistryEntry.Reference<TestInstance> instance) {
		INSTANCES.add(instance);
	}

	public static void clear() {
		INSTANCES.clear();
	}
}

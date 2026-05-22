package net.minecraft.client.util;

import com.mojang.blaze3d.platform.GLX;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.Pointer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Утилита для снятия отслеживания нативной памяти в debug-аллокаторе LWJGL.
 * Используется при передаче владения памятью из LWJGL в сторонний код,
 * чтобы избежать ложных предупреждений об утечках.
 */
@Environment(EnvType.CLIENT)
public class Untracker {

	// Рефлексивный доступ к DebugAllocator.untrack — метод существует только в debug-сборках LWJGL
	private static final @Nullable MethodHandle ALLOCATOR_UNTRACK = GLX.make(() -> {
		try {
			Lookup lookup = MethodHandles.lookup();
			Class<?> debugAllocatorClass = Class.forName("org.lwjgl.system.MemoryManage$DebugAllocator");
			Method untrackMethod = debugAllocatorClass.getDeclaredMethod("untrack", long.class);
			untrackMethod.setAccessible(true);
			Field allocatorField = Class.forName("org.lwjgl.system.MemoryUtil$LazyInit").getDeclaredField("ALLOCATOR");
			allocatorField.setAccessible(true);
			Object allocator = allocatorField.get(null);
			return debugAllocatorClass.isInstance(allocator) ? lookup.unreflect(untrackMethod) : null;
		} catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException | ClassNotFoundException exception) {
			throw new RuntimeException(exception);
		}
	});

	public static void untrack(long address) {
		if (ALLOCATOR_UNTRACK == null) {
			return;
		}

		try {
			ALLOCATOR_UNTRACK.invoke(address);
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

	public static void untrack(Pointer pointer) {
		untrack(pointer.address());
	}
}

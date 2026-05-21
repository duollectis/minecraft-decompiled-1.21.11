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

@Environment(EnvType.CLIENT)
/**
 * {@code Untracker}.
 */
public class Untracker {

	private static final @Nullable MethodHandle ALLOCATOR_UNTRACK = GLX.make(() -> {
		try {
			Lookup lookup = MethodHandles.lookup();
			Class<?> class_ = Class.forName("org.lwjgl.system.MemoryManage$DebugAllocator");
			Method method = class_.getDeclaredMethod("untrack", long.class);
			method.setAccessible(true);
			Field field = Class.forName("org.lwjgl.system.MemoryUtil$LazyInit").getDeclaredField("ALLOCATOR");
			field.setAccessible(true);
			Object object = field.get(null);
			return class_.isInstance(object) ? lookup.unreflect(method) : null;
		}
		catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException | ClassNotFoundException var5) {
			throw new RuntimeException(var5);
		}
	});

	/**
	 * Untrack.
	 *
	 * @param address address
	 */
	public static void untrack(long address) {
		if (ALLOCATOR_UNTRACK != null) {
			try {
				ALLOCATOR_UNTRACK.invoke((long) address);
			}
			catch (Throwable var3) {
				throw new RuntimeException(var3);
			}
		}
	}

	/**
	 * Untrack.
	 *
	 * @param pointer pointer
	 */
	public static void untrack(Pointer pointer) {
		untrack(pointer.address());
	}
}

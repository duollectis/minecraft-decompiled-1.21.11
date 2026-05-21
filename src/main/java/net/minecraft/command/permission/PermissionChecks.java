package net.minecraft.command.permission;

import com.mojang.serialization.MapCodec;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * {@code PermissionChecks}.
 */
public class PermissionChecks {

	public static MapCodec<? extends PermissionCheck> registerAndGetDefault(Registry<MapCodec<? extends PermissionCheck>> registry) {
		Registry.register(registry, Identifier.ofVanilla("always_pass"), PermissionCheck.AlwaysPass.CODEC);
		return Registry.register(registry, Identifier.ofVanilla("require"), PermissionCheck.Require.CODEC);
	}
}

package net.minecraft.command.permission;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.Registries;

/**
 * {@code PermissionCheck}.
 */
public interface PermissionCheck {

	Codec<PermissionCheck>
			CODEC =
			Registries.PERMISSION_CHECK_TYPE.getCodec().dispatch(PermissionCheck::getCodec, codec -> codec);

	boolean allows(PermissionPredicate permissions);

	MapCodec<? extends PermissionCheck> getCodec();

	/**
	 * {@code AlwaysPass}.
	 */
	public static class AlwaysPass implements PermissionCheck {

		public static final PermissionCheck.AlwaysPass INSTANCE = new PermissionCheck.AlwaysPass();
		public static final MapCodec<PermissionCheck.AlwaysPass> CODEC = MapCodec.unit(INSTANCE);

		private AlwaysPass() {
		}

		@Override
		public boolean allows(PermissionPredicate permissions) {
			return true;
		}

		@Override
		public MapCodec<PermissionCheck.AlwaysPass> getCodec() {
			return CODEC;
		}
	}

	/**
	 * {@code Require}.
	 */
	public record Require(Permission permission) implements PermissionCheck {

		public static final MapCodec<PermissionCheck.Require> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance
						.group(Permission.CODEC.fieldOf("permission").forGetter(PermissionCheck.Require::permission))
						.apply(instance, PermissionCheck.Require::new)
		);

		@Override
		public MapCodec<PermissionCheck.Require> getCodec() {
			return CODEC;
		}

		@Override
		public boolean allows(PermissionPredicate permissions) {
			return permissions.hasPermission(this.permission);
		}
	}
}

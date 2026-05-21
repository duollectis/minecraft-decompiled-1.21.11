package net.minecraft.world.debug.gizmo;

import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * {@code ArrowGizmo}.
 */
public record ArrowGizmo(Vec3d start, Vec3d end, int color, float width) implements Gizmo {

	public static final float ARROW_SIZE = 2.5F;

	@Override
	public void draw(GizmoDrawer consumer, float opacity) {
		int i = ColorHelper.scaleAlpha(this.color, opacity);
		consumer.addLine(this.start, this.end, i, this.width);
		Quaternionf
				quaternionf =
				new Quaternionf().rotationTo(
						new Vector3f(1.0F, 0.0F, 0.0F),
						this.end.subtract(this.start).toVector3f().normalize()
				);
		float f = (float) MathHelper.clamp(this.end.distanceTo(this.start) * 0.1F, 0.1F, 1.0);
		Vector3f[] vector3fs = new Vector3f[]{
				quaternionf.transform(-f, f, 0.0F, new Vector3f()),
				quaternionf.transform(-f, 0.0F, f, new Vector3f()),
				quaternionf.transform(-f, -f, 0.0F, new Vector3f()),
				quaternionf.transform(-f, 0.0F, -f, new Vector3f())
		};

		for (Vector3f vector3f : vector3fs) {
			consumer.addLine(this.end.add(vector3f.x, vector3f.y, vector3f.z), this.end, i, this.width);
		}
	}
}

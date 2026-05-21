package net.minecraft.client.render.chunk;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
/**
 * {@code NormalizedRelativePos}.
 */
public final class NormalizedRelativePos {

	private int x;
	private int y;
	private int z;

	public static NormalizedRelativePos of(Vec3d cameraPos, long sectionPos) {
		return new NormalizedRelativePos().with(cameraPos, sectionPos);
	}

	public NormalizedRelativePos with(Vec3d cameraPos, long sectionPos) {
		this.x = normalize(cameraPos.getX(), ChunkSectionPos.unpackX(sectionPos));
		this.y = normalize(cameraPos.getY(), ChunkSectionPos.unpackY(sectionPos));
		this.z = normalize(cameraPos.getZ(), ChunkSectionPos.unpackZ(sectionPos));
		return this;
	}

	private static int normalize(double cameraCoord, int sectionCoord) {
		int i = ChunkSectionPos.getSectionCoordFloored(cameraCoord) - sectionCoord;
		return MathHelper.clamp(i, -1, 1);
	}

	public boolean isOnCameraAxis() {
		return this.x == 0 || this.y == 0 || this.z == 0;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		else {
			return !(o instanceof NormalizedRelativePos normalizedRelativePos)
			       ? false
			       : this.x == normalizedRelativePos.x && this.y == normalizedRelativePos.y
			         && this.z == normalizedRelativePos.z;
		}
	}
}

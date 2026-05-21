package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.Submittable;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;

@Environment(EnvType.CLIENT)
/**
 * {@code BillboardParticleRenderer}.
 */
public class BillboardParticleRenderer extends ParticleRenderer<BillboardParticle> {

	private final ParticleTextureSheet textureSheet;
	final BillboardParticleSubmittable submittable = new BillboardParticleSubmittable();

	public BillboardParticleRenderer(ParticleManager manager, ParticleTextureSheet textureSheet) {
		super(manager);
		this.textureSheet = textureSheet;
	}

	@Override
	public Submittable render(Frustum frustum, Camera camera, float tickProgress) {
		for (BillboardParticle billboardParticle : this.particles) {
			if (frustum.intersectPoint(billboardParticle.x, billboardParticle.y, billboardParticle.z)) {
				try {
					billboardParticle.render(this.submittable, camera, tickProgress);
				}
				catch (Throwable var9) {
					CrashReport crashReport = CrashReport.create(var9, "Rendering Particle");
					CrashReportSection crashReportSection = crashReport.addElement("Particle being rendered");
					crashReportSection.add("Particle", billboardParticle::toString);
					crashReportSection.add("Particle Type", this.textureSheet::toString);
					throw new CrashException(crashReport);
				}
			}
		}

		return this.submittable;
	}
}

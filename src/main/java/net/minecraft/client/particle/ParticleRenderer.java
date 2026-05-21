package net.minecraft.client.particle;

import com.google.common.collect.EvictingQueue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.Submittable;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;

import java.util.Iterator;
import java.util.Queue;

@Environment(EnvType.CLIENT)
/**
 * {@code ParticleRenderer}.
 */
public abstract class ParticleRenderer<P extends Particle> {

	private static final int QUEUE_SIZE = 16384;
	protected final ParticleManager particleManager;
	protected final Queue<P> particles = EvictingQueue.create(16384);

	public ParticleRenderer(ParticleManager particleManager) {
		this.particleManager = particleManager;
	}

	public boolean isEmpty() {
		return this.particles.isEmpty();
	}

	/**
	 * Tick.
	 */
	public void tick() {
		if (!this.particles.isEmpty()) {
			Iterator<P> iterator = this.particles.iterator();

			while (iterator.hasNext()) {
				P particle = iterator.next();
				this.tickParticle(particle);
				if (!particle.isAlive()) {
					particle.getGroup().ifPresent(group -> this.particleManager.addTo(group, -1));
					iterator.remove();
				}
			}
		}
	}

	private void tickParticle(Particle particle) {
		try {
			particle.tick();
		}
		catch (Throwable var5) {
			CrashReport crashReport = CrashReport.create(var5, "Ticking Particle");
			CrashReportSection crashReportSection = crashReport.addElement("Particle being ticked");
			crashReportSection.add("Particle", particle::toString);
			crashReportSection.add("Particle Type", particle.textureSheet()::toString);
			throw new CrashException(crashReport);
		}
	}

	/**
	 * Add.
	 *
	 * @param particle particle
	 */
	public void add(Particle particle) {
		this.particles.add((P) particle);
	}

	/**
	 * Size.
	 *
	 * @return int — результат операции
	 */
	public int size() {
		return this.particles.size();
	}

	/**
	 * Render.
	 *
	 * @param frustum frustum
	 * @param camera camera
	 * @param tickProgress tick progress
	 *
	 * @return Submittable — результат операции
	 */
	public abstract Submittable render(Frustum frustum, Camera camera, float tickProgress);

	public Queue<P> getParticles() {
		return this.particles;
	}
}

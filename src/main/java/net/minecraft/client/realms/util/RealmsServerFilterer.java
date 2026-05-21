package net.minecraft.client.realms.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.realms.dto.RealmsServer;

import java.util.*;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsServerFilterer}.
 */
public class RealmsServerFilterer implements Iterable<RealmsServer> {

	private final MinecraftClient client;
	private final Set<RealmsServer> removedServers = new HashSet<>();
	private List<RealmsServer> sortedServers = List.of();

	public RealmsServerFilterer(MinecraftClient client) {
		this.client = client;
	}

	public void filterAndSort(List<RealmsServer> servers) {
		List<RealmsServer> list = new ArrayList<>(servers);
		list.sort(new RealmsServer.McoServerComparator(this.client.getSession().getUsername()));
		boolean bl = list.removeAll(this.removedServers);
		if (!bl) {
			this.removedServers.clear();
		}

		this.sortedServers = list;
	}

	public void remove(RealmsServer server) {
		this.sortedServers.remove(server);
		this.removedServers.add(server);
	}

	@Override
	public Iterator<RealmsServer> iterator() {
		return this.sortedServers.iterator();
	}

	public boolean isEmpty() {
		return this.sortedServers.isEmpty();
	}
}

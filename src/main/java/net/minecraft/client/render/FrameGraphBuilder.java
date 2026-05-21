package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.ClosableFactory;
import net.minecraft.client.util.ObjectAllocator;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
/**
 * {@code FrameGraphBuilder}.
 */
public class FrameGraphBuilder {

	private final List<FrameGraphBuilder.ResourceNode<?>> resourceNodes = new ArrayList<>();
	private final List<FrameGraphBuilder.ObjectNode<?>> objectNodes = new ArrayList<>();
	private final List<FrameGraphBuilder.FramePassImpl> passes = new ArrayList<>();

	public FramePass createPass(String name) {
		FrameGraphBuilder.FramePassImpl framePassImpl = new FrameGraphBuilder.FramePassImpl(this.passes.size(), name);
		this.passes.add(framePassImpl);
		return framePassImpl;
	}

	public <T> net.minecraft.client.util.Handle<T> createObjectNode(String name, T object) {
		FrameGraphBuilder.ObjectNode<T> objectNode = new FrameGraphBuilder.ObjectNode<>(name, null, object);
		this.objectNodes.add(objectNode);
		return objectNode.handle;
	}

	public <T> net.minecraft.client.util.Handle<T> createResourceHandle(String name, ClosableFactory<T> factory) {
		return this.createResourceNode(name, factory, null).handle;
	}

	<T> FrameGraphBuilder.ResourceNode<T> createResourceNode(
			String name,
			ClosableFactory<T> factory,
			FrameGraphBuilder.@Nullable FramePassImpl stageNode
	) {
		int i = this.resourceNodes.size();
		FrameGraphBuilder.ResourceNode<T>
				resourceNode =
				new FrameGraphBuilder.ResourceNode<>(i, name, stageNode, factory);
		this.resourceNodes.add(resourceNode);
		return resourceNode;
	}

	public void run(ObjectAllocator allocator) {
		this.run(allocator, FrameGraphBuilder.Profiler.NONE);
	}

	public void run(ObjectAllocator allocator, FrameGraphBuilder.Profiler profiler) {
		BitSet bitSet = this.collectPassesToVisit();
		List<FrameGraphBuilder.FramePassImpl> list = new ArrayList<>(bitSet.cardinality());
		BitSet bitSet2 = new BitSet(this.passes.size());

		for (FrameGraphBuilder.FramePassImpl framePassImpl : this.passes) {
			this.visit(framePassImpl, bitSet, bitSet2, list);
		}

		this.checkResources(list);

		for (FrameGraphBuilder.FramePassImpl framePassImpl : list) {
			for (FrameGraphBuilder.ResourceNode<?> resourceNode : framePassImpl.resourcesToAcquire) {
				profiler.acquire(resourceNode.name);
				resourceNode.acquire(allocator);
			}

			profiler.push(framePassImpl.name);
			framePassImpl.renderer.run();
			profiler.pop(framePassImpl.name);

			for (int i = framePassImpl.resourcesToRelease.nextSetBit(0);
			     i >= 0;
			     i = framePassImpl.resourcesToRelease.nextSetBit(i + 1)) {
				FrameGraphBuilder.ResourceNode<?> resourceNode = this.resourceNodes.get(i);
				profiler.release(resourceNode.name);
				resourceNode.release(allocator);
			}
		}
	}

	private BitSet collectPassesToVisit() {
		Deque<FrameGraphBuilder.FramePassImpl> deque = new ArrayDeque<>(this.passes.size());
		BitSet bitSet = new BitSet(this.passes.size());

		for (FrameGraphBuilder.Node<?> node : this.objectNodes) {
			FrameGraphBuilder.FramePassImpl framePassImpl = node.handle.from;
			if (framePassImpl != null) {
				this.markForVisit(framePassImpl, bitSet, deque);
			}
		}

		for (FrameGraphBuilder.FramePassImpl framePassImpl2 : this.passes) {
			if (framePassImpl2.toBeVisited) {
				this.markForVisit(framePassImpl2, bitSet, deque);
			}
		}

		return bitSet;
	}

	private void markForVisit(
			FrameGraphBuilder.FramePassImpl pass,
			BitSet result,
			Deque<FrameGraphBuilder.FramePassImpl> deque
	) {
		deque.add(pass);

		while (!deque.isEmpty()) {
			FrameGraphBuilder.FramePassImpl framePassImpl = deque.poll();
			if (!result.get(framePassImpl.id)) {
				result.set(framePassImpl.id);

				for (int i = framePassImpl.requiredPassIds.nextSetBit(0);
				     i >= 0;
				     i = framePassImpl.requiredPassIds.nextSetBit(i + 1)) {
					deque.add(this.passes.get(i));
				}
			}
		}
	}

	private void visit(
			FrameGraphBuilder.FramePassImpl node,
			BitSet unvisited,
			BitSet visiting,
			List<FrameGraphBuilder.FramePassImpl> topologicalOrderOut
	) {
		if (visiting.get(node.id)) {
			String
					string =
					visiting.stream().mapToObj(id -> this.passes.get(id).name).collect(Collectors.joining(", "));
			throw new IllegalStateException("Frame graph cycle detected between " + string);
		}
		else if (unvisited.get(node.id)) {
			visiting.set(node.id);
			unvisited.clear(node.id);

			for (int i = node.requiredPassIds.nextSetBit(0); i >= 0; i = node.requiredPassIds.nextSetBit(i + 1)) {
				this.visit(this.passes.get(i), unvisited, visiting, topologicalOrderOut);
			}

			for (FrameGraphBuilder.Handle<?> handle : node.transferredHandles) {
				for (int j = handle.dependents.nextSetBit(0); j >= 0; j = handle.dependents.nextSetBit(j + 1)) {
					if (j != node.id) {
						this.visit(this.passes.get(j), unvisited, visiting, topologicalOrderOut);
					}
				}
			}

			topologicalOrderOut.add(node);
			visiting.clear(node.id);
		}
	}

	private void checkResources(Collection<FrameGraphBuilder.FramePassImpl> passes) {
		FrameGraphBuilder.FramePassImpl[]
				framePassImpls =
				new FrameGraphBuilder.FramePassImpl[this.resourceNodes.size()];

		for (FrameGraphBuilder.FramePassImpl framePassImpl : passes) {
			for (int i = framePassImpl.requiredResourceIds.nextSetBit(0);
			     i >= 0;
			     i = framePassImpl.requiredResourceIds.nextSetBit(i + 1)) {
				FrameGraphBuilder.ResourceNode<?> resourceNode = this.resourceNodes.get(i);
				FrameGraphBuilder.FramePassImpl framePassImpl2 = framePassImpls[i];
				framePassImpls[i] = framePassImpl;
				if (framePassImpl2 == null) {
					framePassImpl.resourcesToAcquire.add(resourceNode);
				}
				else {
					framePassImpl2.resourcesToRelease.clear(i);
				}

				framePassImpl.resourcesToRelease.set(i);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code FramePassImpl}.
	 */
	class FramePassImpl implements FramePass {

		final int id;
		final String name;
		final List<FrameGraphBuilder.Handle<?>> transferredHandles = new ArrayList<>();
		final BitSet requiredResourceIds = new BitSet();
		final BitSet requiredPassIds = new BitSet();
		Runnable renderer = () -> {};
		final List<FrameGraphBuilder.ResourceNode<?>> resourcesToAcquire = new ArrayList<>();
		final BitSet resourcesToRelease = new BitSet();
		boolean toBeVisited;

		public FramePassImpl(final int id, final String name) {
			this.id = id;
			this.name = name;
		}

		private <T> void addRequired(FrameGraphBuilder.Handle<T> handle) {
			if (handle.parent instanceof FrameGraphBuilder.ResourceNode<?> resourceNode) {
				this.requiredResourceIds.set(resourceNode.id);
			}
		}

		private void addRequired(FrameGraphBuilder.FramePassImpl child) {
			this.requiredPassIds.set(child.id);
		}

		@Override
		public <T> net.minecraft.client.util.Handle<T> addRequiredResource(String name, ClosableFactory<T> factory) {
			FrameGraphBuilder.ResourceNode<T>
					resourceNode =
					FrameGraphBuilder.this.createResourceNode(name, factory, this);
			this.requiredResourceIds.set(resourceNode.id);
			return resourceNode.handle;
		}

		@Override
		public <T> void dependsOn(net.minecraft.client.util.Handle<T> handle) {
			this.dependsOn((FrameGraphBuilder.Handle<T>) handle);
		}

		private <T> void dependsOn(FrameGraphBuilder.Handle<T> handle) {
			this.addRequired(handle);
			if (handle.from != null) {
				this.addRequired(handle.from);
			}

			handle.dependents.set(this.id);
		}

		@Override
		public <T> net.minecraft.client.util.Handle<T> transfer(net.minecraft.client.util.Handle<T> handle) {
			return this.transfer((FrameGraphBuilder.Handle<T>) handle);
		}

		@Override
		public void addRequired(FramePass pass) {
			this.requiredPassIds.set(((FrameGraphBuilder.FramePassImpl) pass).id);
		}

		@Override
		public void markToBeVisited() {
			this.toBeVisited = true;
		}

		private <T> FrameGraphBuilder.Handle<T> transfer(FrameGraphBuilder.Handle<T> handle) {
			this.transferredHandles.add(handle);
			this.dependsOn(handle);
			return handle.moveTo(this);
		}

		@Override
		public void setRenderer(Runnable renderer) {
			this.renderer = renderer;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Handle}.
	 */
	static class Handle<T> implements net.minecraft.client.util.Handle<T> {

		final FrameGraphBuilder.Node<T> parent;
		private final int id;
		final FrameGraphBuilder.@Nullable FramePassImpl from;
		final BitSet dependents = new BitSet();
		private FrameGraphBuilder.@Nullable Handle<T> movedTo;

		Handle(FrameGraphBuilder.Node<T> parent, int id, FrameGraphBuilder.@Nullable FramePassImpl from) {
			this.parent = parent;
			this.id = id;
			this.from = from;
		}

		@Override
		public T get() {
			return this.parent.get();
		}

		FrameGraphBuilder.Handle<T> moveTo(FrameGraphBuilder.FramePassImpl pass) {
			if (this.parent.handle != this) {
				throw new IllegalStateException(
						"Handle " + this + " is no longer valid, as its contents were moved into " + this.movedTo);
			}
			else {
				FrameGraphBuilder.Handle<T> handle = new FrameGraphBuilder.Handle<>(this.parent, this.id + 1, pass);
				this.parent.handle = handle;
				this.movedTo = handle;
				return handle;
			}
		}

		@Override
		public String toString() {
			return this.from != null ? this.parent + "#" + this.id + " (from " + this.from + ")"
			                         : this.parent + "#" + this.id;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Node}.
	 */
	abstract static class Node<T> {

		public final String name;
		public FrameGraphBuilder.Handle<T> handle;

		public Node(String name, FrameGraphBuilder.@Nullable FramePassImpl from) {
			this.name = name;
			this.handle = new FrameGraphBuilder.Handle<>(this, 0, from);
		}

		public abstract T get();

		@Override
		public String toString() {
			return this.name;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ObjectNode}.
	 */
	static class ObjectNode<T> extends FrameGraphBuilder.Node<T> {

		private final T value;

		public ObjectNode(String name, FrameGraphBuilder.@Nullable FramePassImpl parent, T value) {
			super(name, parent);
			this.value = value;
		}

		@Override
		public T get() {
			return this.value;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Profiler}.
	 */
	public interface Profiler {

		FrameGraphBuilder.Profiler NONE = new FrameGraphBuilder.Profiler() {};

		default void acquire(String name) {
		}

		default void release(String name) {
		}

		default void push(String location) {
		}

		default void pop(String location) {
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ResourceNode}.
	 */
	static class ResourceNode<T> extends FrameGraphBuilder.Node<T> {

		final int id;
		private final ClosableFactory<T> factory;
		private @Nullable T resource;

		public ResourceNode(
				int id,
				String name,
				FrameGraphBuilder.@Nullable FramePassImpl from,
				ClosableFactory<T> factory
		) {
			super(name, from);
			this.id = id;
			this.factory = factory;
		}

		@Override
		public T get() {
			return Objects.requireNonNull(this.resource, "Resource is not currently available");
		}

		public void acquire(ObjectAllocator allocator) {
			if (this.resource != null) {
				throw new IllegalStateException("Tried to acquire physical resource, but it was already assigned");
			}
			else {
				this.resource = allocator.acquire(this.factory);
			}
		}

		public void release(ObjectAllocator allocator) {
			if (this.resource == null) {
				throw new IllegalStateException("Tried to release physical resource that was not allocated");
			}
			else {
				allocator.release(this.factory, this.resource);
				this.resource = null;
			}
		}
	}
}

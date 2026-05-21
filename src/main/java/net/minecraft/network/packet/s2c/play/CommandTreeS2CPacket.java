package net.minecraft.network.packet.s2c.play;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.function.BiPredicate;

/**
 * Класс command tree s2 c packet.
 */
public class CommandTreeS2CPacket implements Packet<ClientPlayPacketListener> {

	public static final PacketCodec<PacketByteBuf, CommandTreeS2CPacket>
			CODEC =
			Packet.createCodec(CommandTreeS2CPacket::write, CommandTreeS2CPacket::new);
	private static final byte NODE_TYPE_MASK = 3;
	private static final byte EXECUTABLE = 4;
	private static final byte HAS_REDIRECT = 8;
	private static final byte HAS_SUGGESTION_PROVIDER = 16;
	private static final byte REQUIRES_LEVEL = 32;
	private static final byte NODE_TYPE_ROOT = 0;
	private static final byte NODE_TYPE_LITERAL = 1;
	private static final byte NODE_TYPE_ARGUMENT = 2;
	private final int rootSize;
	private final List<CommandTreeS2CPacket.CommandNodeData> nodes;

	public <S> CommandTreeS2CPacket(
			RootCommandNode<S> rootIndex,
			CommandTreeS2CPacket.CommandNodeInspector<S> inspector
	) {
		Object2IntMap<CommandNode<S>> object2IntMap = traverse(rootIndex);
		this.nodes = collectNodes(object2IntMap, inspector);
		this.rootSize = object2IntMap.getInt(rootIndex);
	}

	private CommandTreeS2CPacket(PacketByteBuf buf) {
		this.nodes = buf.readList(CommandTreeS2CPacket::readCommandNode);
		this.rootSize = buf.readVarInt();
		validate(this.nodes);
	}

	private void write(PacketByteBuf buf) {
		buf.writeCollection(this.nodes, (buf2, node) -> node.write(buf2));
		buf.writeVarInt(this.rootSize);
	}

	private static void validate(
			List<CommandTreeS2CPacket.CommandNodeData> nodeDatas,
			BiPredicate<CommandTreeS2CPacket.CommandNodeData, IntSet> validator
	) {
		IntSet intSet = new IntOpenHashSet(IntSets.fromTo(0, nodeDatas.size()));

		while (!intSet.isEmpty()) {
			boolean bl = intSet.removeIf(i -> validator.test(nodeDatas.get(i), intSet));
			if (!bl) {
				throw new IllegalStateException("Server sent an impossible command tree");
			}
		}
	}

	private static void validate(List<CommandTreeS2CPacket.CommandNodeData> nodeDatas) {
		validate(nodeDatas, CommandTreeS2CPacket.CommandNodeData::validateRedirectNodeIndex);
		validate(nodeDatas, CommandTreeS2CPacket.CommandNodeData::validateChildNodeIndices);
	}

	private static <S> Object2IntMap<CommandNode<S>> traverse(RootCommandNode<S> commandTree) {
		Object2IntMap<CommandNode<S>> object2IntMap = new Object2IntOpenHashMap();
		Queue<CommandNode<S>> queue = new ArrayDeque<>();
		queue.add(commandTree);

		CommandNode<S> commandNode;
		while ((commandNode = queue.poll()) != null) {
			if (!object2IntMap.containsKey(commandNode)) {
				int i = object2IntMap.size();
				object2IntMap.put(commandNode, i);
				queue.addAll(commandNode.getChildren());
				if (commandNode.getRedirect() != null) {
					queue.add(commandNode.getRedirect());
				}
			}
		}

		return object2IntMap;
	}

	private static <S> List<CommandTreeS2CPacket.CommandNodeData> collectNodes(
			Object2IntMap<CommandNode<S>> nodeOrdinals, CommandTreeS2CPacket.CommandNodeInspector<S> inspector
	) {
		ObjectArrayList<CommandTreeS2CPacket.CommandNodeData>
				objectArrayList =
				new ObjectArrayList(nodeOrdinals.size());
		objectArrayList.size(nodeOrdinals.size());
		ObjectIterator var3 = Object2IntMaps.fastIterable(nodeOrdinals).iterator();

		while (var3.hasNext()) {
			Entry<CommandNode<S>> entry = (Entry<CommandNode<S>>) var3.next();
			objectArrayList.set(
					entry.getIntValue(),
					createNodeData((CommandNode<S>) entry.getKey(), inspector, nodeOrdinals)
			);
		}

		return objectArrayList;
	}

	private static CommandTreeS2CPacket.CommandNodeData readCommandNode(PacketByteBuf buf) {
		byte b = buf.readByte();
		int[] is = buf.readIntArray();
		int i = (b & 8) != 0 ? buf.readVarInt() : 0;
		CommandTreeS2CPacket.SuggestableNode suggestableNode = readArgumentBuilder(buf, b);
		return new CommandTreeS2CPacket.CommandNodeData(suggestableNode, b, i, is);
	}

	private static CommandTreeS2CPacket.@Nullable SuggestableNode readArgumentBuilder(PacketByteBuf buf, byte flags) {
		int i = flags & 3;
		if (i == 2) {
			String string = buf.readString();
			int j = buf.readVarInt();
			ArgumentSerializer<?, ?> argumentSerializer = Registries.COMMAND_ARGUMENT_TYPE.get(j);
			if (argumentSerializer == null) {
				return null;
			}
			else {
				ArgumentSerializer.ArgumentTypeProperties<?>
						argumentTypeProperties =
						argumentSerializer.fromPacket(buf);
				Identifier identifier = (flags & 16) != 0 ? buf.readIdentifier() : null;
				return new CommandTreeS2CPacket.ArgumentNode(string, argumentTypeProperties, identifier);
			}
		}
		else if (i == 1) {
			String string = buf.readString();
			return new CommandTreeS2CPacket.LiteralNode(string);
		}
		else {
			return null;
		}
	}

	private static <S> CommandTreeS2CPacket.CommandNodeData createNodeData(
			CommandNode<S> node,
			CommandTreeS2CPacket.CommandNodeInspector<S> inspector,
			Object2IntMap<CommandNode<S>> nodeOrdinals
	) {
		int i = 0;
		int j;
		if (node.getRedirect() != null) {
			i |= 8;
			j = nodeOrdinals.getInt(node.getRedirect());
		}
		else {
			j = 0;
		}

		if (inspector.isExecutable(node)) {
			i |= 4;
		}

		if (inspector.hasRequiredLevel(node)) {
			i |= 32;
		}

		CommandTreeS2CPacket.SuggestableNode suggestableNode;
		switch (node) {
			case RootCommandNode<S> rootCommandNode:
				i |= 0;
				suggestableNode = null;
				break;
			case ArgumentCommandNode<S, ?> argumentCommandNode:
				Identifier identifier = inspector.getSuggestionProviderId(argumentCommandNode);
				suggestableNode = new CommandTreeS2CPacket.ArgumentNode(
						argumentCommandNode.getName(),
						ArgumentTypes.getArgumentTypeProperties(argumentCommandNode.getType()),
						identifier
				);
				i |= 2;
				if (identifier != null) {
					i |= 16;
				}
				break;
			case LiteralCommandNode<S> literalCommandNode:
				suggestableNode = new CommandTreeS2CPacket.LiteralNode(literalCommandNode.getLiteral());
				i |= 1;
				break;
			default:
				throw new UnsupportedOperationException("Unknown node type " + node);
		}

		int[] is = node.getChildren().stream().mapToInt(nodeOrdinals::getInt).toArray();
		return new CommandTreeS2CPacket.CommandNodeData(suggestableNode, i, j, is);
	}

	@Override
	public PacketType<CommandTreeS2CPacket> getPacketType() {
		return PlayPackets.COMMANDS;
	}

	/**
	 * Apply.
	 *
	 * @param clientPlayPacketListener client play packet listener
	 */
	public void apply(ClientPlayPacketListener clientPlayPacketListener) {
		clientPlayPacketListener.onCommandTree(this);
	}

	public <S> RootCommandNode<S> getCommandTree(
			CommandRegistryAccess commandRegistryAccess,
			CommandTreeS2CPacket.NodeFactory<S> nodeFactory
	) {
		return (RootCommandNode<S>) new CommandTreeS2CPacket.CommandTree<>(
				commandRegistryAccess,
				nodeFactory,
				this.nodes
		).getNode(this.rootSize);
	}

	record ArgumentNode(String name, ArgumentSerializer.ArgumentTypeProperties<?> properties, @Nullable Identifier id)
			implements CommandTreeS2CPacket.SuggestableNode {

		@Override
		public <S> ArgumentBuilder<S, ?> createArgumentBuilder(
				CommandRegistryAccess commandRegistryAccess,
				CommandTreeS2CPacket.NodeFactory<S> nodeFactory
		) {
			ArgumentType<?> argumentType = this.properties.createType(commandRegistryAccess);
			return nodeFactory.argument(this.name, argumentType, this.id);
		}

		@Override
		public void write(PacketByteBuf buf) {
			buf.writeString(this.name);
			write(buf, this.properties);
			if (this.id != null) {
				buf.writeIdentifier(this.id);
			}
		}

		private static <A extends ArgumentType<?>> void write(
				PacketByteBuf buf,
				ArgumentSerializer.ArgumentTypeProperties<A> properties
		) {
			write(buf, properties.getSerializer(), properties);
		}

		private static <A extends ArgumentType<?>, T extends ArgumentSerializer.ArgumentTypeProperties<A>> void write(
				PacketByteBuf buf,
				ArgumentSerializer<A, T> serializer,
				ArgumentSerializer.ArgumentTypeProperties<A> properties
		) {
			buf.writeVarInt(Registries.COMMAND_ARGUMENT_TYPE.getRawId(serializer));
			serializer.writePacket((T) properties, buf);
		}
	}

	record CommandNodeData(
			CommandTreeS2CPacket.@Nullable SuggestableNode suggestableNode,
			int flags,
			int redirectNodeIndex,
			int[] childNodeIndices
	) {

		/**
		 * Write.
		 *
		 * @param buf buf
		 */
		public void write(PacketByteBuf buf) {
			buf.writeByte(this.flags);
			buf.writeIntArray(this.childNodeIndices);
			if ((this.flags & 8) != 0) {
				buf.writeVarInt(this.redirectNodeIndex);
			}

			if (this.suggestableNode != null) {
				this.suggestableNode.write(buf);
			}
		}

		/**
		 * Валидирует redirect node index.
		 *
		 * @param indices indices
		 *
		 * @return boolean — результат операции
		 */
		public boolean validateRedirectNodeIndex(IntSet indices) {
			return (this.flags & 8) != 0 ? !indices.contains(this.redirectNodeIndex) : true;
		}

		/**
		 * Валидирует child node indices.
		 *
		 * @param indices indices
		 *
		 * @return boolean — результат операции
		 */
		public boolean validateChildNodeIndices(IntSet indices) {
			for (int i : this.childNodeIndices) {
				if (indices.contains(i)) {
					return false;
				}
			}

			return true;
		}
	}

	/**
	 * Интерфейс command node inspector.
	 */
	public interface CommandNodeInspector<S> {

		@Nullable Identifier getSuggestionProviderId(ArgumentCommandNode<S, ?> node);

		boolean isExecutable(CommandNode<S> node);

		boolean hasRequiredLevel(CommandNode<S> node);
	}

	static class CommandTree<S> {

		private final CommandRegistryAccess commandRegistryAccess;
		private final CommandTreeS2CPacket.NodeFactory<S> nodeFactory;
		private final List<CommandTreeS2CPacket.CommandNodeData> nodeDatas;
		private final List<CommandNode<S>> nodes;

		CommandTree(
				CommandRegistryAccess commandRegistryAccess,
				CommandTreeS2CPacket.NodeFactory<S> nodeFactory,
				List<CommandTreeS2CPacket.CommandNodeData> nodeDatas
		) {
			this.commandRegistryAccess = commandRegistryAccess;
			this.nodeFactory = nodeFactory;
			this.nodeDatas = nodeDatas;
			ObjectArrayList<CommandNode<S>> objectArrayList = new ObjectArrayList();
			objectArrayList.size(nodeDatas.size());
			this.nodes = objectArrayList;
		}

		public CommandNode<S> getNode(int index) {
			CommandNode<S> commandNode = this.nodes.get(index);
			if (commandNode != null) {
				return commandNode;
			}
			else {
				CommandTreeS2CPacket.CommandNodeData commandNodeData = this.nodeDatas.get(index);
				CommandNode<S> commandNode2;
				if (commandNodeData.suggestableNode == null) {
					commandNode2 = new RootCommandNode();
				}
				else {
					ArgumentBuilder<S, ?>
							argumentBuilder =
							commandNodeData.suggestableNode.createArgumentBuilder(
									this.commandRegistryAccess,
									this.nodeFactory
							);
					if ((commandNodeData.flags & 8) != 0) {
						argumentBuilder.redirect(this.getNode(commandNodeData.redirectNodeIndex));
					}

					boolean bl = (commandNodeData.flags & 4) != 0;
					boolean bl2 = (commandNodeData.flags & 32) != 0;
					commandNode2 = this.nodeFactory.modifyNode(argumentBuilder, bl, bl2).build();
				}

				this.nodes.set(index, commandNode2);

				for (int i : commandNodeData.childNodeIndices) {
					CommandNode<S> commandNode3 = this.getNode(i);
					if (!(commandNode3 instanceof RootCommandNode)) {
						commandNode2.addChild(commandNode3);
					}
				}

				return commandNode2;
			}
		}
	}

	record LiteralNode(String literal) implements CommandTreeS2CPacket.SuggestableNode {

		@Override
		public <S> ArgumentBuilder<S, ?> createArgumentBuilder(
				CommandRegistryAccess commandRegistryAccess,
				CommandTreeS2CPacket.NodeFactory<S> nodeFactory
		) {
			return nodeFactory.literal(this.literal);
		}

		@Override
		public void write(PacketByteBuf buf) {
			buf.writeString(this.literal);
		}
	}

	/**
	 * Интерфейс node factory.
	 */
	public interface NodeFactory<S> {

		ArgumentBuilder<S, ?> literal(String name);

		ArgumentBuilder<S, ?> argument(String name, ArgumentType<?> type, @Nullable Identifier suggestionProviderId);

		ArgumentBuilder<S, ?> modifyNode(ArgumentBuilder<S, ?> arg, boolean disableExecution, boolean requireTrusted);
	}

	interface SuggestableNode {

		<S> ArgumentBuilder<S, ?> createArgumentBuilder(
				CommandRegistryAccess commandRegistryAccess,
				CommandTreeS2CPacket.NodeFactory<S> nodeFactory
		);

		void write(PacketByteBuf buf);
	}
}

package dev.jorel.commandapi.nms;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.CommandRegistrationStrategy;
import dev.jorel.commandapi.PaperCommandRegistration;
import dev.jorel.commandapi.SpigotCommandRegistration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_20_R4.CraftServer;
import org.bukkit.craftbukkit.v1_20_R4.command.BukkitCommandWrapper;
import org.bukkit.craftbukkit.v1_20_R4.command.VanillaCommandWrapper;

public class PaperNMS_1_20_R4 extends PaperNMS_Common {

	private static final CommandBuildContext COMMAND_BUILD_CONTEXT;
	private static final boolean vanillaCommandDispatcherFieldExists;

	static {
		if (Bukkit.getServer() instanceof CraftServer server) {
			COMMAND_BUILD_CONTEXT = CommandBuildContext.simple(server.getServer().registryAccess(),
				server.getServer().getWorldData().enabledFeatures());
		} else {
			COMMAND_BUILD_CONTEXT = null;
		}

		boolean fieldExists;
		try {
			MinecraftServer.class.getDeclaredField("vanillaCommandDispatcher");
			fieldExists = true;
		} catch (NoSuchFieldException | SecurityException e) {
			// Expected on Paper-1.20.6-65 or later due to https://github.com/PaperMC/Paper/pull/8235
			fieldExists = false;
		}
		vanillaCommandDispatcherFieldExists = fieldExists;
	}

	@Override
	public NamedTextColor getChatColor(CommandContext<CommandSourceStack> cmdCtx, String key) {
		final Integer color = ColorArgument.getColor(cmdCtx, key).getColor();
		return color == null ? NamedTextColor.WHITE : NamedTextColor.namedColor(color);
	}

	@Override
	public Component getChatComponent(CommandContext<CommandSourceStack> cmdCtx, String key) {
		return GsonComponentSerializer.gson().deserialize(net.minecraft.network.chat.Component.Serializer.toJson(ComponentArgument.getComponent(cmdCtx, key), COMMAND_BUILD_CONTEXT));
	}

	@Override
	public NMS<?> bukkitNMS() {
		return new NMS_1_20_R4();
	}

	@Override
	public CommandRegistrationStrategy<CommandSourceStack> createCommandRegistrationStrategy() {
		if (vanillaCommandDispatcherFieldExists) {
			return new SpigotCommandRegistration<>(
				((CommandAPIBukkit<?>) bukkitNMS()).<MinecraftServer>getMinecraftServer().vanillaCommandDispatcher.getDispatcher(),
				(SimpleCommandMap) getCommandMap(),
				() -> ((CommandAPIBukkit<?>) bukkitNMS()).<MinecraftServer>getMinecraftServer().getCommands().getDispatcher(),
				command -> command instanceof VanillaCommandWrapper,
				node -> new VanillaCommandWrapper(((CommandAPIBukkit<?>) bukkitNMS()).<MinecraftServer>getMinecraftServer().vanillaCommandDispatcher, node),
				node -> node.getCommand() instanceof BukkitCommandWrapper
			);
		} else {
			// This class is Paper-server specific, so we need to use paper's userdev plugin to
			//  access it directly. That might need gradle, but there might also be a maven version?
			//  https://discord.com/channels/289587909051416579/1121227200277004398/1246910745761812480
			Class<?> bukkitCommandNode_bukkitBrigCommand;
			try {
				bukkitCommandNode_bukkitBrigCommand = Class.forName("io.papermc.paper.command.brigadier.bukkit.BukkitCommandNode$BukkitBrigCommand");
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException("Expected to find class", e);
			}
			return new PaperCommandRegistration<>(
				() -> ((CommandAPIBukkit<?>) bukkitNMS()).<MinecraftServer>getMinecraftServer().getCommands().getDispatcher(),
				node -> bukkitCommandNode_bukkitBrigCommand.isInstance(node.getCommand())
			);
		}
	}

}

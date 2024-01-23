package dev.jorel.commandapi.nms;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.CommandRegistrationStrategy;
import dev.jorel.commandapi.PaperCommandRegistration;
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
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.bukkit.craftbukkit.v1_21_R1.command.VanillaCommandWrapper;

public class PaperNMS_1_21_R1 extends PaperNMS_Common {

	private static final CommandBuildContext COMMAND_BUILD_CONTEXT;

	static {
		if (Bukkit.getServer() instanceof CraftServer server) {
			COMMAND_BUILD_CONTEXT = CommandBuildContext.simple(server.getServer().registryAccess(),
				server.getServer().getWorldData().enabledFeatures());
		} else {
			COMMAND_BUILD_CONTEXT = null;
		}
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
		return new NMS_1_21_R1();
	}

	@Override
	public CommandRegistrationStrategy<CommandSourceStack> createCommandRegistrationStrategy() {
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

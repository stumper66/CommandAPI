package dev.jorel.commandapi.nms;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;

public interface SpigotNMS<CommandListenerWrapper> {

	BaseComponent[] getChat(CommandContext<CommandListenerWrapper> cmdCtx, String key) throws CommandSyntaxException;

	ChatColor getChatColor(CommandContext<CommandListenerWrapper> cmdCtx, String key);

	BaseComponent[] getChatComponent(CommandContext<CommandListenerWrapper> cmdCtx, String key);

	NMS<?> bukkitNMS();

	/**
	 * Wraps a Brigadier command node as Bukkit's VanillaCommandWrapper
	 *
	 * @param node The LiteralCommandNode to wrap
	 * @return A VanillaCommandWrapper representing the given node
	 */
	Command wrapToVanillaCommandWrapper(CommandNode<CommandListenerWrapper> node);

}

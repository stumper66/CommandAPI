package dev.jorel.commandapi.test;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jorel.commandapi.CommandAPISpigot;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import org.bukkit.ChatColor;

public abstract class ArgumentNMS extends MockSpigotPlatform<CommandListenerWrapper> {

	CommandAPISpigot<CommandListenerWrapper> baseNMS;

	@SuppressWarnings("unchecked")
	protected ArgumentNMS(CommandAPISpigot<?> baseNMS) {
		this.baseNMS = (CommandAPISpigot<CommandListenerWrapper>) baseNMS;
	}

	@Override
	public BaseComponent[] getChat(CommandContext<CommandListenerWrapper> cmdCtx, String key) throws CommandSyntaxException {
		return baseNMS.getChat(cmdCtx, key);
	}

	@Override
	public ChatColor getChatColor(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		return baseNMS.getChatColor(cmdCtx, key);
	}

	@Override
	public BaseComponent[] getChatComponent(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		return baseNMS.getChatComponent(cmdCtx, key);
	}
}

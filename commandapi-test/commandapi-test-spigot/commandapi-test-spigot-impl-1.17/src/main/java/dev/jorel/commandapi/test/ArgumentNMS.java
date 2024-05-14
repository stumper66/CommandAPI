package dev.jorel.commandapi.test;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jorel.commandapi.CommandAPISpigot;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.ChatColor;

public abstract class ArgumentNMS extends MockSpigotPlatform<CommandSourceStack> {

	CommandAPISpigot<CommandSourceStack> baseNMS;

	@SuppressWarnings("unchecked")
	protected ArgumentNMS(CommandAPISpigot<?> baseNMS) {
		this.baseNMS = (CommandAPISpigot<CommandSourceStack>) baseNMS;
	}

	@Override
	public BaseComponent[] getChat(CommandContext<CommandSourceStack> cmdCtx, String key) throws CommandSyntaxException {
		return baseNMS.getChat(cmdCtx, key);
	}

	@Override
	public ChatColor getChatColor(CommandContext<CommandSourceStack> cmdCtx, String key) {
		return baseNMS.getChatColor(cmdCtx, key);
	}

	@Override
	public BaseComponent[] getChatComponent(CommandContext<CommandSourceStack> cmdCtx, String key) {
		return baseNMS.getChatComponent(cmdCtx, key);
	}

}

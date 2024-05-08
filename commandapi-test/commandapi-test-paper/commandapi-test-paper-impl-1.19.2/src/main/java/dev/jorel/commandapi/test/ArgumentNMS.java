package dev.jorel.commandapi.test;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jorel.commandapi.CommandAPIPaper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.commands.CommandSourceStack;

public abstract class ArgumentNMS extends MockPaperPlatform<CommandSourceStack> {

	CommandAPIPaper<CommandSourceStack> baseNMS;

	@SuppressWarnings("unchecked")
	protected ArgumentNMS(CommandAPIPaper<?> baseNMS) {
		this.baseNMS = (CommandAPIPaper<CommandSourceStack>) baseNMS;
	}

	@Override
	public Component getChatComponent(CommandContext<CommandSourceStack> cmdCtx, String key) {
		return baseNMS.getChatComponent(cmdCtx, key);
	}

	@Override
	public Component getChat(CommandContext<CommandSourceStack> cmdCtx, String key) throws CommandSyntaxException {
		return baseNMS.getChat(cmdCtx, key);
	}

	@Override
	public NamedTextColor getChatColor(CommandContext<CommandSourceStack> cmdCtx, String key) {
		return baseNMS.getChatColor(cmdCtx, key);
	}
}

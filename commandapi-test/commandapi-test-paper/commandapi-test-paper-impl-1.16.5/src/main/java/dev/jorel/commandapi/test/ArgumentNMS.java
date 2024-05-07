package dev.jorel.commandapi.test;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jorel.commandapi.CommandAPIPaper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;

public abstract class ArgumentNMS extends MockPaperPlatform<CommandListenerWrapper> {

	CommandAPIPaper<CommandListenerWrapper> baseNMS;

	@SuppressWarnings("unchecked")
	protected ArgumentNMS(CommandAPIPaper<?> baseNMS) {
		this.baseNMS = (CommandAPIPaper<CommandListenerWrapper>) baseNMS;
	}

	@Override
	public Component getChat(CommandContext<CommandListenerWrapper> cmdCtx, String key) throws CommandSyntaxException {
		return baseNMS.getChat(cmdCtx, key);
	}

	@Override
	public NamedTextColor getChatColor(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		return baseNMS.getChatColor(cmdCtx, key);
	}

	@Override
	public Component getChatComponent(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		return baseNMS.getChatComponent(cmdCtx, key);
	}
}

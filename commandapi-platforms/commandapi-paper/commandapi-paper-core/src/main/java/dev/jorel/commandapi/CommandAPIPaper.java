package dev.jorel.commandapi;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.SuggestionProviders;
import dev.jorel.commandapi.commandsenders.AbstractCommandSender;
import dev.jorel.commandapi.commandsenders.AbstractPlayer;
import dev.jorel.commandapi.commandsenders.BukkitCommandSender;
import dev.jorel.commandapi.nms.PaperNMS;
import dev.jorel.commandapi.preprocessor.Unimplemented;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static dev.jorel.commandapi.preprocessor.Unimplemented.REASON.REQUIRES_CRAFTBUKKIT;

public abstract class CommandAPIPaper<Source> implements CommandAPIPlatform<Argument<?>, CommandSender, Source>, PaperNMS<Source> {

	private static CommandAPIBukkit<?> bukkit;
	private static CommandAPIPaper<?> paper;

	protected CommandAPIPaper() {
		CommandAPIPaper.bukkit = ((CommandAPIBukkit<?>) bukkitNMS());
		CommandAPIPaper.paper = this;
	}

	@SuppressWarnings("unchecked")
	public static <Source> CommandAPIBukkit<Source> getBukkit() {
		return (CommandAPIBukkit<Source>) bukkit;
	}

	@SuppressWarnings("unchecked")
	public static <Source> CommandAPIPaper<Source> getPaper() {
		return (CommandAPIPaper<Source>) paper;
	}

	public static InternalBukkitConfig getConfiguration() {
		return CommandAPIBukkit.getConfiguration();
	}

	@Override
	public void onLoad(CommandAPIConfig<?> config) {
		bukkit.onLoad(config);
	}

	@Override
	public void onEnable() {
		bukkit.onEnable();
	}

	@Override
	public void onDisable() {
		bukkit.onDisable();
	}

	@SuppressWarnings("unchecked")
	@Override
	public BukkitCommandSender<? extends CommandSender> getSenderForCommand(CommandContext<Source> cmdCtx, boolean forceNative) {
		return getBukkit().getSenderForCommand((CommandContext<Object>) cmdCtx, forceNative);
	}

	@Override
	public BukkitCommandSender<? extends CommandSender> getCommandSenderFromCommandSource(Source source) {
		return getBukkit().getCommandSenderFromCommandSource(source);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Source getBrigadierSourceFromCommandSender(AbstractCommandSender<? extends CommandSender> sender) {
		return (Source) bukkit.getBrigadierSourceFromCommandSender(sender);
	}

	@Override
	public BukkitCommandSender<? extends CommandSender> wrapCommandSender(CommandSender commandSender) {
		return bukkit.wrapCommandSender(commandSender);
	}

	@Override
	public void registerPermission(String string) {
		bukkit.registerPermission(string);
	}

	@SuppressWarnings("unchecked")
	@Override
	public SuggestionProvider<Source> getSuggestionProvider(SuggestionProviders suggestionProvider) {
		return (SuggestionProvider<Source>) bukkit.getSuggestionProvider(suggestionProvider);
	}

	@Override
	public void preCommandRegistration(String commandName) {
		bukkit.preCommandRegistration(commandName);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void postCommandRegistration(RegisteredCommand registeredCommand, LiteralCommandNode<Source> resultantNode, List<LiteralCommandNode<Source>> aliasNodes) {
		List<LiteralCommandNode<Object>> aliases = new ArrayList<>();
		for (LiteralCommandNode<?> commandNode : aliasNodes) {
			aliases.add((LiteralCommandNode<Object>) commandNode);
		}
		getBukkit().postCommandRegistration(registeredCommand, (LiteralCommandNode<Object>) resultantNode, aliases);
	}

	@SuppressWarnings("unchecked")
	@Override
	public LiteralCommandNode<Source> registerCommandNode(LiteralArgumentBuilder<Source> node, String namespace) {
		return (LiteralCommandNode<Source>) getBukkit().registerCommandNode((LiteralArgumentBuilder<Object>) node, namespace);
	}

	@Override
	public void unregister(String commandName, boolean unregisterNamespaces) {
		bukkit.unregister(commandName, unregisterNamespaces);
	}

	@SuppressWarnings("unchecked")
	@Override
	public CommandDispatcher<Source> getBrigadierDispatcher() {
		return (CommandDispatcher<Source>) bukkit.getBrigadierDispatcher();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void createDispatcherFile(File file, CommandDispatcher<Source> dispatcher) throws IOException {
		getBukkit().createDispatcherFile(file, (CommandDispatcher<Object>) dispatcher);
	}

	@Override
	public void reloadDataPacks() {
		bukkit.reloadDataPacks();
	}

	@Override
	public void updateRequirements(AbstractPlayer<?> player) {
		bukkit.updateRequirements(player);
	}

	@Override
	public CommandAPICommand newConcreteCommandAPICommand(CommandMetaData<CommandSender> meta) {
		return bukkit.newConcreteCommandAPICommand(meta);
	}

	@Override
	public MultiLiteralArgument newConcreteMultiLiteralArgument(String nodeName, String[] literals) {
		return (MultiLiteralArgument) bukkit.newConcreteMultiLiteralArgument(nodeName, literals);
	}

	@Override
	public LiteralArgument newConcreteLiteralArgument(String nodeName, String literal) {
		return (LiteralArgument) bukkit.newConcreteLiteralArgument(nodeName, literal);
	}

	@Override
	@Unimplemented(because = REQUIRES_CRAFTBUKKIT, classNamed = "VanillaCommandWrapper")
	public abstract Command wrapToVanillaCommandWrapper(CommandNode<Source> node);
}

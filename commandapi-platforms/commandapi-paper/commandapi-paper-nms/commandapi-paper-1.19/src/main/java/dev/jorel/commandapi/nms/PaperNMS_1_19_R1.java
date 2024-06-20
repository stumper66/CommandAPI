package dev.jorel.commandapi.nms;

import com.mojang.brigadier.tree.CommandNode;
import dev.jorel.commandapi.CommandAPIBukkit;
import io.netty.channel.Channel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.bukkit.command.Command;
import org.bukkit.craftbukkit.v1_19_R1.command.VanillaCommandWrapper;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PaperNMS_1_19_R1 extends PaperNMS_1_19_Common {

	@Override
	protected void hookChatPreview(Plugin plugin, Player player) {
		final Channel playerChannel = ((CraftPlayer) player).getHandle().connection.connection.channel;
		if (playerChannel.pipeline().get("CommandAPI_" + player.getName()) == null) {
			playerChannel.pipeline().addBefore("packet_handler", "CommandAPI_" + player.getName(), new PaperNMS_1_19_R1_ChatPreviewHandler(this, plugin, player));
		}
	}

	@Override
	public NMS<?> bukkitNMS() {
		return new NMS_1_19_R1();
	}

	@Override
	public Command wrapToVanillaCommandWrapper(CommandNode<CommandSourceStack> node) {
		return new VanillaCommandWrapper(((CommandAPIBukkit<?>) bukkitNMS()).<MinecraftServer>getMinecraftServer().getCommands(), node);
	}

}

package dev.jorel.commandapi.nms;

import com.mojang.brigadier.tree.CommandNode;
import dev.jorel.commandapi.CommandAPIBukkit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.bukkit.command.Command;
import org.bukkit.craftbukkit.v1_17_R1.command.VanillaCommandWrapper;

public class PaperNMS_1_17 extends PaperNMS_1_17_Common {

	@Override
	public NMS<?> bukkitNMS() {
		return new NMS_1_17();
	}

	@Override
	public Command wrapToVanillaCommandWrapper(CommandNode<CommandSourceStack> node) {
		return new VanillaCommandWrapper(((CommandAPIBukkit<?>) bukkitNMS()).<MinecraftServer>getMinecraftServer().getCommands(), node);
	}

}

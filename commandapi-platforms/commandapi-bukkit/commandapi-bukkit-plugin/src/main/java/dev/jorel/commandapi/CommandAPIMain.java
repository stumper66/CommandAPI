/*******************************************************************************
 * Copyright 2018, 2021 Jorel Ali (Skepter) - MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package dev.jorel.commandapi;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import dev.jorel.commandapi.arguments.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main CommandAPI plugin entrypoint
 */
public class CommandAPIMain extends JavaPlugin {

	private static final String PLUGINS_TO_CONVERT = "plugins-to-convert";

	@Override
	public void onLoad() {
		// Read config file
		saveDefaultConfig();
		FileConfiguration fileConfig = getConfig();
		CommandAPIBukkitConfig config = new CommandAPIBukkitConfig(this)
			.verboseOutput(fileConfig.getBoolean("verbose-outputs"))
			.silentLogs(fileConfig.getBoolean("silent-logs"))
			.useLatestNMSVersion(fileConfig.getBoolean("use-latest-nms-version"))
			.missingExecutorImplementationMessage(fileConfig.getString("messages.missing-executor-implementation"))
			.dispatcherFile(fileConfig.getBoolean("create-dispatcher-json") ? new File(getDataFolder(), "command_registration.json") : null)
			.shouldHookPaperReload(fileConfig.getBoolean("hook-paper-reload"))
			.skipReloadDatapacks(fileConfig.getBoolean("skip-initial-datapack-reload"));

		for (String pluginName : fileConfig.getStringList("skip-sender-proxy")) {
			if (Bukkit.getPluginManager().getPlugin(pluginName) != null) {
				config.addSkipSenderProxy(pluginName);
			} else {
				new InvalidPluginException("Could not find a plugin " + pluginName + "! Has it been loaded properly?")
					.printStackTrace();
			}
		}

		// Main CommandAPI loading
		CommandAPI.setLogger(CommandAPILogger.fromJavaLogger(getLogger()));
		CommandAPI.onLoad(config);

		// Convert all plugins to be converted
		if (!fileConfig.getList(PLUGINS_TO_CONVERT).isEmpty()
			&& fileConfig.getMapList(PLUGINS_TO_CONVERT).isEmpty()) {
			CommandAPI.logError("plugins-to-convert has an invalid type. Did you miss a colon (:) after a plugin name?");
		}

		convertCommands(fileConfig);
	}

	private void convertCommands(FileConfiguration fileConfig) {
		// Load all plugins at the same time
		Map<JavaPlugin, String[]> pluginsToConvert = new HashMap<>();
		for (Map<?, ?> map : fileConfig.getMapList(PLUGINS_TO_CONVERT)) {
			String[] pluginCommands;
			if (map.values().size() == 1 && map.values().iterator().next() == null) {
				pluginCommands = new String[0];
			} else {
				@SuppressWarnings("unchecked")
				List<String> commands = (List<String>) map.values().iterator().next();
				pluginCommands = commands.toArray(new String[0]);
			}

			// Get the plugin, if it doesn't exist, scream in the console (but
			// don't crash, we want to continue!)
			final JavaPlugin plugin = getAndValidatePlugin((String) map.keySet().iterator().next());
			if (plugin != null) {
				pluginsToConvert.put(plugin, pluginCommands);
			}
		}

		// Convert plugin commands
		for (Entry<JavaPlugin, String[]> pluginToConvert : pluginsToConvert.entrySet()) {
			if (pluginToConvert.getValue().length == 0) {
				Converter.convert(pluginToConvert.getKey());
			} else {
				for (String command : pluginToConvert.getValue()) {
					new AdvancedConverter(pluginToConvert.getKey(), command).convert();
				}
			}
		}

		// Convert all arbitrary commands
		for (String commandName : fileConfig.getStringList("other-commands-to-convert")) {
			new AdvancedConverter(commandName).convertCommand();
		}
	}

	private JavaPlugin getAndValidatePlugin(String pluginName) {
		Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
		if (plugin != null) {
			if (plugin instanceof JavaPlugin javaPlugin) {
				return javaPlugin;
			} else {
				new InvalidPluginException("Plugin " + pluginName + " is not a JavaPlugin!").printStackTrace();
			}
		} else {
			new InvalidPluginException("Could not find a plugin " + pluginName + "! Has it been loaded properly?")
				.printStackTrace();
		}
		return null;
	}

	@Override
	public void onEnable() {
		CommandAPI.onEnable();

		List<String> tags = new ArrayList<>();
		tags.add("hello");
		tags.add("world");

		new CommandTree("server")
			.then(
				new LiteralArgument("add").then(
					new StringArgument("tag").executes(info -> {
						String tag = info.args().getUnchecked("tag");

						tags.add(tag);
					})
				)
			)
			.then(
				new LiteralArgument("tag").then(
					new DynamicMultiLiteralArgument("tag", sender -> {
						if (sender instanceof Player player) {
							List<String> addPlayer = new ArrayList<>(tags);
							addPlayer.add(player.getName());
							return addPlayer;
						} else {
							return tags;
						}
					}).then(new IntegerArgument("extra").replaceSafeSuggestions(SafeSuggestions.suggest(1, 2, 3))
						.executes(info -> {
								String tag = info.args().getUnchecked("tag");
								int extra = info.args().getUnchecked("extra");

								info.sender().sendMessage(tag + " " + extra);
							}
						)
					))
			)
			.register();

		new CommandAPICommand("updateCommands")
			.executes(info -> {
				for (Player player : Bukkit.getOnlinePlayers()) {
					CommandAPI.updateRequirements(player);
				}
			})
			.register();
	}
}

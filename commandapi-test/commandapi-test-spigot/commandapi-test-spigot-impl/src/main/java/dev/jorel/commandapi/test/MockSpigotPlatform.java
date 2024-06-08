package dev.jorel.commandapi.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import dev.jorel.commandapi.CommandAPISpigot;
import dev.jorel.commandapi.SafeVarHandle;
import dev.jorel.commandapi.commandsenders.BukkitCommandSender;
import org.bukkit.command.CommandSender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public abstract class MockSpigotPlatform<CLW> extends CommandAPISpigot<CLW> implements MockBukkitPlatform {

	/*****************
	 * Instantiation *
	 *****************/

	private static MockPlatform<?> mockPlatform = null;
	private static MockSpigotPlatform<?> spigotInstance = null;

	@SuppressWarnings("unchecked")
	public static <CLW> MockPlatform<CLW> getMockPlatform() {
		return (MockPlatform<CLW>) mockPlatform;
	}

	@SuppressWarnings("unchecked")
	public static <CLW> MockSpigotPlatform<CLW> getSpigotInstance() {
		return (MockSpigotPlatform<CLW>) spigotInstance;
	}

	protected MockSpigotPlatform() {
		if (MockSpigotPlatform.spigotInstance == null) {
			MockSpigotPlatform.spigotInstance = this;
			MockSpigotPlatform.mockPlatform = (MockPlatform<?>) bukkitNMS();
			MockPlatform.bukkitPlatform = this;
		} else {
			// wtf why was this called twice?
		}
	}

	public static void unload() {
		MockSpigotPlatform.spigotInstance = null;
		MockSpigotPlatform.mockPlatform = null;
	}

	/************************************
	 * CommandAPIPaper implementations *
	 ************************************/

	private final CommandDispatcher<CLW> brigadierDispatcher = new CommandDispatcher<>();
	private final CommandDispatcher<CLW> resourcesDispatcher = new CommandDispatcher<>();

	public final CommandDispatcher<CLW> getMockBrigadierDispatcher() {
		return this.brigadierDispatcher;
	}

	public final CommandDispatcher<CLW> getMockResourcesDispatcher() {
		return this.resourcesDispatcher;
	}

	@Override
	public final BukkitCommandSender<? extends CommandSender> getSenderForCommand(CommandContext<CLW> cmdCtx, boolean forceNative) {
		return getCommandSenderFromCommandSource(cmdCtx.getSource());
	}

	@Override
	public final void reloadDataPacks() {
		assert true; // Nothing to do here
	}

	/******************
	 * Helper methods *
	 ******************/

	public static Object getField(Class<?> className, String fieldName, Object instance) {
		return getField(className, fieldName, fieldName, instance);
	}

	public static Object getField(Class<?> className, String fieldName, String mojangMappedName, Object instance) {
		try {
			Field field = className.getDeclaredField(SafeVarHandle.USING_MOJANG_MAPPINGS ? mojangMappedName : fieldName);
			field.setAccessible(true);
			return field.get(instance);
		} catch (ReflectiveOperationException e) {
			return null;
		}
	}

	public static void setField(Class<?> className, String fieldName, Object instance, Object value) {
		setField(className, fieldName, fieldName, instance, value);
	}

	public static void setField(Class<?> className, String fieldName, String mojangMappedName, Object instance, Object value) {
		try {
			Field field = className.getDeclaredField(SafeVarHandle.USING_MOJANG_MAPPINGS ? mojangMappedName : fieldName);
			field.setAccessible(true);
			field.set(instance, value);
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}
	}

	public static <T> T getFieldAs(Class<?> className, String fieldName, Object instance, Class<T> asType) {
		return getFieldAs(className, fieldName, fieldName, instance, asType);
	}

	public static <T> T getFieldAs(Class<?> className, String fieldName, String mojangMappedName, Object instance, Class<T> asType) {
		try {
			Field field = className.getDeclaredField(SafeVarHandle.USING_MOJANG_MAPPINGS ? mojangMappedName : fieldName);
			field.setAccessible(true);
			return asType.cast(field.get(instance));
		} catch (ReflectiveOperationException e) {
			return null;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> T forceGetArgument(CommandContext cmdCtx, String key) {
		ParsedArgument result = (ParsedArgument) getFieldAs(CommandContext.class, "arguments", cmdCtx, Map.class).get(key);
		return (T) result.getResult();
	}

	/***************
	 * Other stuff *
	 ***************/

	static record Pair<A, B>(A first, B second) {}

	/**
	 * Gets recipes from {@code data/minecraft/recipes/<file>.json}. Parses them and
	 * returns a list of {@code {name, json}}, where {@code name} is the name of the
	 * file without the {@code .json} extension, and {@code json} is the parsed JSON
	 * result from the file
	 *
	 * @param minecraftServerClass an instance of MinecraftServer.class
	 * @return A list of pairs of resource locations (with no namespace) and JSON objects
	 */
	public final List<Pair<String, JsonObject>> getRecipes(Class<?> minecraftServerClass) {
		List<Pair<String, JsonObject>> list = new ArrayList<>();
		// Get the spigot-x.x.x-Rx.x-SNAPSHOT.jar file
		try(JarFile jar = new JarFile(minecraftServerClass.getProtectionDomain().getCodeSource().getLocation().getPath())) {
			// Iterate over everything in the jar
			jar.entries().asIterator().forEachRemaining(entry -> {
				if(entry.getName().startsWith("data/minecraft/recipes/") && entry.getName().endsWith(".json")) {
					// If it's what we want, read everything
					InputStream is = minecraftServerClass.getClassLoader().getResourceAsStream(entry.getName());
					String jsonStr = new BufferedReader(new InputStreamReader(is))
						.lines()
						.map(line -> {
							// We can't load tags in the testing environment. If we have any recipes that
							// use tags as ingredients (e.g. wooden_axe or charcoal), we'll get an illegal
							// state exception from TagUtil complaining that a tag has been used before it
							// was bound. To mitigate this, we simply remove all tags and put in a dummy
							// item (in this case, stick)
							if(line.contains("\"tag\": ")) {
								return "\"item\": \"minecraft:stick\"";
							}
							return line;
						})
						.collect(Collectors.joining("\n"));
					// Get the resource location (file name, no extension, no path) and parse the JSON.
					// Using deprecated method as the alternative doesn't exist in 1.17
					@SuppressWarnings("deprecation")
					JsonObject parsedJson = new JsonParser().parse(jsonStr).getAsJsonObject();
					list.add(new Pair<>(entry.getName().substring("data/minecraft/recipes/".length(), entry.getName().lastIndexOf(".")), parsedJson));
				}
			});
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load any recipes for testing!", e);
		}

		return list;
	}

	@SuppressWarnings("serial")
	private static class UnimplementedError extends Error {
		public UnimplementedError() {
			super("Unimplemented");
		}
	}

}

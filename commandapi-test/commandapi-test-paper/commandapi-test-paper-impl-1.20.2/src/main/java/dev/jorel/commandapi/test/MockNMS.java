package dev.jorel.commandapi.test;

import be.seeseemelk.mockbukkit.enchantments.EnchantmentMock;
import be.seeseemelk.mockbukkit.potion.MockPotionEffectType;
import com.google.common.collect.Streams;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import dev.jorel.commandapi.Brigadier;
import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.CommandAPIPaper;
import dev.jorel.commandapi.CommandRegistrationStrategy;
import dev.jorel.commandapi.nms.NMS;
import dev.jorel.commandapi.nms.NMS_1_20_R2;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.ServerFunctionLibrary;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.profiling.metrics.profiling.InactiveMetricsRecorder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;

import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

public class MockNMS extends ArgumentNMS {

	static {
		CodeSource src = PotionEffectType.class.getProtectionDomain().getCodeSource();
		if (src != null) {
			System.err.println("Loading PotionEffectType sources from " + src.getLocation());
		}
	}

	static ServerAdvancementManager advancementDataWorld = new ServerAdvancementManager(null);

	MinecraftServer minecraftServerMock = null;
	List<ServerPlayer> players = new ArrayList<>();
	PlayerList playerListMock;
	final RecipeManager recipeManager;
	Map<ResourceLocation, CommandFunction> functions = new HashMap<>();
	Map<ResourceLocation, Collection<CommandFunction>> tags = new HashMap<>();

	private NMS_1_20_R2 bukkitNMS;

	public MockNMS(CommandAPIPaper<?> baseNMS) {
		super(baseNMS);

		// Stub in our getMinecraftServer implementation
		CommandAPIPaper<CommandSourceStack> nms = Mockito.spy(super.baseNMS);
		Mockito.when(((CommandAPIBukkit<?>) bukkitNMS()).getMinecraftServer()).thenAnswer(i -> getMinecraftServer());
		super.baseNMS = nms;

//		initializeArgumentsInArgumentTypeInfos();

		// Initialize WorldVersion (game version)
		SharedConstants.tryDetectVersion();

		// MockBukkit is very helpful and registers all of the potion
		// effects and enchantments for us. We need to not do this (because
		// we call Bootstrap.bootStrap() below which does the same thing)
		unregisterAllEnchantments();
		unregisterAllPotionEffects();

		// Invoke Minecraft's registry
		Bootstrap.bootStrap();

		// Don't use EnchantmentMock.registerDefaultEnchantments because we want
		// to specify what enchantments to mock (i.e. only 1.18 ones, and not any
		// 1.19 ones!)
		registerDefaultPotionEffects();
		registerDefaultEnchantments();

		this.recipeManager = new RecipeManager();
		this.functions = new HashMap<>();
		registerDefaultRecipes();

		// Setup playerListMock
		playerListMock = Mockito.mock(PlayerList.class);
		Mockito.when(playerListMock.getPlayerByName(anyString())).thenAnswer(invocation -> {
			String playerName = invocation.getArgument(0);
			for (ServerPlayer onlinePlayer : players) {
				if (onlinePlayer.getBukkitEntity().getName().equals(playerName)) {
					return onlinePlayer;
				}
			}
			return null;
		});
	}

	/*************************
	 * Registry manipulation *
	 *************************/

	private void unregisterAllPotionEffects() {
		PotionEffectType[] byId = getFieldAs(PotionEffectType.class, "byId", null, PotionEffectType[].class);
		for (int i = 0; i < byId.length; i++) {
			byId[i] = null;
		}

		getFieldAs(PotionEffectType.class, "byName", null, Map.class).clear();
		getFieldAs(PotionEffectType.class, "byKey", null, Map.class).clear();
		setField(PotionEffectType.class, "acceptingNew", null, true);
	}

	private void registerDefaultPotionEffects() {
		for (PotionEffectType type : PotionEffectType.values()) {
			if (type != null) {
				return;
			}
		}

		registerPotionEffectType(1, "SPEED", false, 8171462);
		registerPotionEffectType(2, "SLOWNESS", false, 5926017);
		registerPotionEffectType(3, "HASTE", false, 14270531);
		registerPotionEffectType(4, "MINING_FATIGUE", false, 4866583);
		registerPotionEffectType(5, "STRENGTH", false, 9643043);
		registerPotionEffectType(6, "INSTANT_HEALTH", true, 16262179);
		registerPotionEffectType(7, "INSTANT_DAMAGE", true, 4393481);
		registerPotionEffectType(8, "JUMP_BOOST", false, 2293580);
		registerPotionEffectType(9, "NAUSEA", false, 5578058);
		registerPotionEffectType(10, "REGENERATION", false, 13458603);
		registerPotionEffectType(11, "RESISTANCE", false, 10044730);
		registerPotionEffectType(12, "FIRE_RESISTANCE", false, 14981690);
		registerPotionEffectType(13, "WATER_BREATHING", false, 3035801);
		registerPotionEffectType(14, "INVISIBILITY", false, 8356754);
		registerPotionEffectType(15, "BLINDNESS", false, 2039587);
		registerPotionEffectType(16, "NIGHT_VISION", false, 2039713);
		registerPotionEffectType(17, "HUNGER", false, 5797459);
		registerPotionEffectType(18, "WEAKNESS", false, 4738376);
		registerPotionEffectType(19, "POISON", false, 5149489);
		registerPotionEffectType(20, "WITHER", false, 3484199);
		registerPotionEffectType(21, "HEALTH_BOOST", false, 16284963);
		registerPotionEffectType(22, "ABSORPTION", false, 2445989);
		registerPotionEffectType(23, "SATURATION", true, 16262179);
		registerPotionEffectType(24, "GLOWING", false, 9740385);
		registerPotionEffectType(25, "LEVITATION", false, 13565951);
		registerPotionEffectType(26, "LUCK", false, 3381504);
		registerPotionEffectType(27, "UNLUCK", false, 12624973);
		registerPotionEffectType(28, "SLOW_FALLING", false, 16773073);
		registerPotionEffectType(29, "CONDUIT_POWER", false, 1950417);
		registerPotionEffectType(30, "DOLPHINS_GRACE", false, 8954814);
		registerPotionEffectType(31, "BAD_OMEN", false, 745784);
		registerPotionEffectType(32, "HERO_OF_THE_VILLAGE", false, 4521796);
		registerPotionEffectType(33, "DARKNESS", false, 2696993);
		PotionEffectType.stopAcceptingRegistrations();
	}

	private void registerPotionEffectType(int id, @NotNull String name, boolean instant, int rgb) {
		final NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT));
		PotionEffectType.registerPotionEffectType(new MockPotionEffectType(key, id, name, instant, Color.fromRGB(rgb)));
	}

	private void unregisterAllEnchantments() {
		getFieldAs(Enchantment.class, "byName", null, Map.class).clear();
		getFieldAs(Enchantment.class, "byKey", null, Map.class).clear();
		setField(Enchantment.class, "acceptingNew", null, true);
	}

	private void registerDefaultEnchantments() {
		for (Enchantment enchantment : MockPlatform.getInstance().getEnchantments()) {
			if (Enchantment.getByKey(enchantment.getKey()) == null) {
				Enchantment.registerEnchantment(new EnchantmentMock(enchantment.getKey(), enchantment.getKey().getKey()));
			}
		}
	}

	private void registerDefaultRecipes() {
		// TODO: Come back to this in a bit
//		@SuppressWarnings({ "unchecked", "rawtypes" })
//		List<Recipe<?>> recipes = (List) getRecipes(MinecraftServer.class)
//			.stream()
//			.map(p -> RecipeManager.fromJson(new ResourceLocation(p.first()), p.second()))
//			.toList();
//		recipeManager.replaceRecipes(recipes);
	}

	@Override
	public NMS<?> bukkitNMS() {
		if (bukkitNMS == null) {
			this.bukkitNMS = new NMS_1_20_R2();
		}
		return bukkitNMS;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getMinecraftServer() {
		if (minecraftServerMock != null) {
			return (T) minecraftServerMock;
		}
		minecraftServerMock = Mockito.mock(MinecraftServer.class);

		// LootTableArgument
		Mockito.when(minecraftServerMock.getLootData()).thenAnswer(invocation -> {
			//.getKeys(LootDataType.TABLE)
			LootDataManager lootDataManager = Mockito.mock(LootDataManager.class);

			Mockito.when(lootDataManager.getLootTable(any(ResourceLocation.class))).thenAnswer(i -> {
				if (BuiltInLootTables.all().contains(i.getArgument(0))) {
					return net.minecraft.world.level.storage.loot.LootTable.EMPTY;
				} else {
					return null;
				}
			});

			Mockito.when(lootDataManager.getKeys(any())).thenAnswer(i -> {
				return Streams
					.concat(
						Arrays.stream(MockPlatform.getInstance().getEntityTypes())
							.filter(e -> !e.equals(EntityType.UNKNOWN))
							// TODO? These entity types don't have corresponding
							// loot table entries! Did Spigot miss them out?
							.filter(e -> !e.equals(EntityType.ALLAY))
							.filter(e -> !e.equals(EntityType.FROG))
							.filter(e -> !e.equals(EntityType.TADPOLE))
							.filter(e -> !e.equals(EntityType.WARDEN))
							.filter(e -> e.isAlive())
							.map(EntityType::getKey)
							.map(k -> new ResourceLocation("minecraft", "entities/" + k.getKey())),
						BuiltInLootTables.all().stream())
					.collect(Collectors.toSet());
			});
			return lootDataManager;
		});

		// AdvancementArgument
		Mockito.when(minecraftServerMock.getAdvancements()).thenAnswer(i -> advancementDataWorld);

		// TeamArgument
		ServerScoreboard scoreboardServerMock = Mockito.mock(ServerScoreboard.class);
		Mockito.when(scoreboardServerMock.getPlayerTeam(anyString())).thenAnswer(invocation -> { // Scoreboard#getPlayerTeam
			String teamName = invocation.getArgument(0);
			Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamName);
			if (team == null) {
				return null;
			} else {
				return new PlayerTeam(scoreboardServerMock, teamName);
			}
		});
		Mockito.when(scoreboardServerMock.getObjective(anyString())).thenAnswer(invocation -> { // Scoreboard#getObjective
			String objectiveName = invocation.getArgument(0);
			org.bukkit.scoreboard.Objective bukkitObjective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objectiveName);
			if (bukkitObjective == null) {
				return null;
			} else {
				return new Objective(scoreboardServerMock, objectiveName, ObjectiveCriteria.byName(bukkitObjective.getCriteria()).get(), Component.literal(bukkitObjective.getDisplayName()), switch(bukkitObjective.getRenderType()) {
					case HEARTS:
						yield ObjectiveCriteria.RenderType.HEARTS;
					case INTEGER:
						yield ObjectiveCriteria.RenderType.INTEGER;
				});
			}
		});
		Mockito.when(minecraftServerMock.getScoreboard()).thenReturn(scoreboardServerMock); // MinecraftServer#getScoreboard

		// WorldArgument (Dimension)
		Mockito.when(minecraftServerMock.getLevel(any(ResourceKey.class))).thenAnswer(invocation -> {
			// Get the ResourceKey<World> and extract the world name from it
			ResourceKey<Level> resourceKey = invocation.getArgument(0);
			String worldName = resourceKey.location().getPath();

			// Get the world via Bukkit (returns a WorldMock) and create a
			// CraftWorld clone of it for WorldServer.getWorld()
			World world = Bukkit.getServer().getWorld(worldName);
			if (world == null) {
				return null;
			} else {
				CraftWorld craftWorldMock = Mockito.mock(CraftWorld.class);
				Mockito.when(craftWorldMock.getName()).thenReturn(world.getName());
				Mockito.when(craftWorldMock.getUID()).thenReturn(world.getUID());

				// Create our return WorldServer object
				ServerLevel bukkitWorldServerMock = Mockito.mock(ServerLevel.class);
				Mockito.when(bukkitWorldServerMock.getWorld()).thenReturn(craftWorldMock);
				return bukkitWorldServerMock;
			}
		});

		// Player lists
		Mockito.when(minecraftServerMock.getPlayerList()).thenAnswer(i -> playerListMock);
		Mockito.when(minecraftServerMock.getPlayerList().getPlayers()).thenAnswer(i -> players);

		// PlayerArgument
		GameProfileCache userCacheMock = Mockito.mock(GameProfileCache.class);
		Mockito.when(userCacheMock.get(anyString())).thenAnswer(invocation -> {
			String playerName = invocation.getArgument(0);
			for (ServerPlayer onlinePlayer : players) {
				if (onlinePlayer.getBukkitEntity().getName().equals(playerName)) {
					return Optional.of(new GameProfile(onlinePlayer.getBukkitEntity().getUniqueId(), playerName));
				}
			}
			return Optional.empty();
		});
		Mockito.when(minecraftServerMock.getProfileCache()).thenReturn(userCacheMock);

		// RecipeArgument
		Mockito.when(minecraftServerMock.getRecipeManager()).thenAnswer(i -> this.recipeManager);

		// FunctionArgument
		// We're using 2 as the function compilation level.
		Mockito.when(minecraftServerMock.getFunctionCompilationLevel()).thenReturn(2);
		Mockito.when(minecraftServerMock.getFunctions()).thenAnswer(i -> {
			ServerFunctionLibrary serverFunctionLibrary = Mockito.mock(ServerFunctionLibrary.class);

			// Functions
			Mockito.when(serverFunctionLibrary.getFunction(any())).thenAnswer(invocation -> Optional.ofNullable(functions.get(invocation.getArgument(0))));
			Mockito.when(serverFunctionLibrary.getFunctions()).thenAnswer(invocation -> functions);

			// Tags
			Mockito.when(serverFunctionLibrary.getTag(any())).thenAnswer(invocation -> tags.getOrDefault(invocation.getArgument(0), List.of()));
			Mockito.when(serverFunctionLibrary.getAvailableTags()).thenAnswer(invocation -> tags.keySet());

			return new ServerFunctionManager(minecraftServerMock, serverFunctionLibrary) {

				// Make sure we don't use ServerFunctionManager#getDispatcher!
				// That method accesses MinecraftServer.vanillaCommandDispatcher
				// directly (boo) and that causes all sorts of nonsense.
				@Override
				public CommandDispatcher<CommandSourceStack> getDispatcher() {
					return Brigadier.getCommandDispatcher();
				}
			};
		});

		Mockito.when(minecraftServerMock.getGameRules()).thenAnswer(i -> new GameRules());
		Mockito.when(minecraftServerMock.getProfiler()).thenAnswer(i -> InactiveMetricsRecorder.INSTANCE.getProfiler());

		// Brigadier and resources dispatcher, used in `NMS#createCommandRegistrationStrategy`
		Commands brigadierCommands = new Commands();
		MockPlatform.setField(brigadierCommands.getClass(), "g", "dispatcher",
			brigadierCommands, getMockBrigadierDispatcher());
		minecraftServerMock.vanillaCommandDispatcher = brigadierCommands;

		Commands resourcesCommands = new Commands();
		MockPlatform.setField(resourcesCommands.getClass(), "g", "dispatcher",
			resourcesCommands, getMockResourcesDispatcher());
		Mockito.when(minecraftServerMock.getCommands()).thenReturn(resourcesCommands);

		return (T) minecraftServerMock;
	}

	@Override
	public CommandRegistrationStrategy<CommandSourceStack> createCommandRegistrationStrategy() {
		return baseNMS.createCommandRegistrationStrategy();
	}

}

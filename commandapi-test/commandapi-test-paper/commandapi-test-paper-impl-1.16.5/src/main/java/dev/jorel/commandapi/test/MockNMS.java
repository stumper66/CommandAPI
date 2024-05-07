package dev.jorel.commandapi.test;

import com.google.common.collect.Streams;
import com.mojang.authlib.GameProfile;
import dev.jorel.commandapi.Brigadier;
import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.CommandAPIPaper;
import dev.jorel.commandapi.nms.NMS;
import dev.jorel.commandapi.nms.NMS_1_16_R3;
import net.minecraft.server.v1_16_R3.AdvancementDataWorld;
import net.minecraft.server.v1_16_R3.ChatComponentText;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import net.minecraft.server.v1_16_R3.CraftingManager;
import net.minecraft.server.v1_16_R3.CustomFunction;
import net.minecraft.server.v1_16_R3.CustomFunctionData;
import net.minecraft.server.v1_16_R3.DispenserRegistry;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.GameProfilerDisabled;
import net.minecraft.server.v1_16_R3.GameRules;
import net.minecraft.server.v1_16_R3.IScoreboardCriteria;
import net.minecraft.server.v1_16_R3.LootTableRegistry;
import net.minecraft.server.v1_16_R3.LootTables;
import net.minecraft.server.v1_16_R3.MinecraftKey;
import net.minecraft.server.v1_16_R3.MinecraftServer;
import net.minecraft.server.v1_16_R3.PlayerList;
import net.minecraft.server.v1_16_R3.ResourceKey;
import net.minecraft.server.v1_16_R3.ScoreboardObjective;
import net.minecraft.server.v1_16_R3.ScoreboardServer;
import net.minecraft.server.v1_16_R3.ScoreboardTeam;
import net.minecraft.server.v1_16_R3.SharedConstants;
import net.minecraft.server.v1_16_R3.Tag;
import net.minecraft.server.v1_16_R3.UserCache;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.entity.EntityType;
import org.bukkit.scoreboard.Team;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

public class MockNMS extends ArgumentNMS {

	static AdvancementDataWorld advancementDataWorld = new AdvancementDataWorld(null);
	MinecraftServer minecraftServerMock = null;
	List<EntityPlayer> players = new ArrayList<>();
	PlayerList playerListMock;
	final CraftingManager recipeManager;
	Map<MinecraftKey, CustomFunction> functions = new HashMap<>();
	Map<MinecraftKey, Collection<CustomFunction>> tags = new HashMap<>();

	@SuppressWarnings("unchecked")
	public MockNMS(CommandAPIPaper<?> baseNMS) {
		super(baseNMS);

		// Stub in our getMinecraftServer implementation
		CommandAPIPaper<CommandListenerWrapper> nms = Mockito.spy(super.baseNMS);
		Mockito.when(((CommandAPIBukkit<CommandListenerWrapper>) nms.bukkitNMS()).getMinecraftServer()).thenAnswer(i -> getMinecraftServer());
		super.baseNMS = nms;

		// Initialize WorldVersion (game version)
		SharedConstants.b();

		// MockBukkit is very helpful and registers all of the potion
		// effects and enchantments for us. We need to not do this (because
		// we call Bootstrap.bootStrap() below which does the same thing)
		unregisterAllEnchantments();
		unregisterAllPotionEffects();

		// Invoke Minecraft's registry. This also initializes all argument types.
		// How convenient!
		DispenserRegistry.init();

		// Don't use EnchantmentMock.registerDefaultEnchantments because we want
		// to specify what enchantments to mock (i.e. only 1.18 ones, and not any
		// 1.19 ones!)
		registerDefaultPotionEffects();
		registerDefaultEnchantments();

		this.recipeManager = new CraftingManager();
		this.functions = new HashMap<>();
		registerDefaultRecipes();

		// Set up playerListMock
		playerListMock = Mockito.mock(PlayerList.class);
		Mockito.when(playerListMock.getPlayer(anyString())).thenAnswer(invocation -> {
			String playerName = invocation.getArgument(0);
			for (EntityPlayer onlinePlayer : players) {
				if (onlinePlayer.getBukkitEntity().getName().equals(playerName)) {
					return onlinePlayer;
				}
			}
			return null;
		});
	}

	@Override
	public NMS<?> bukkitNMS() {
		return new NMS_1_16_R3();
	}

	@Override
	public <T> T getMinecraftServer() {
		if (minecraftServerMock != null) {
			return (T) minecraftServerMock;
		}
		minecraftServerMock = Mockito.mock(MinecraftServer.class);

		// LootTableArgument
		Mockito.when(minecraftServerMock.getLootTableRegistry()).thenAnswer(invocation -> {
			LootTableRegistry lootTables = Mockito.mock(LootTableRegistry.class);
			Mockito.when(lootTables.getLootTable(any(MinecraftKey.class))).thenAnswer(i -> {
				if (LootTables.a().contains(i.getArgument(0))) {
					return net.minecraft.server.v1_16_R3.LootTable.EMPTY;
				} else {
					return null;
				}
			});
			Mockito.when(lootTables.a()).thenAnswer(i -> {
				return Streams
					.concat(
						Arrays.stream(MockPaperPlatform.getMockPlatform().getEntityTypes())
							.filter(e -> !e.equals(EntityType.UNKNOWN))
							.filter(e -> e.isAlive())
							.map(EntityType::getKey)
							.map(k -> new MinecraftKey("minecraft", "entities/" + k.getKey())),
						LootTables.a().stream())
					.collect(Collectors.toSet());
			});
			return lootTables;
		});

		// AdvancementArgument
		Mockito.when(minecraftServerMock.getAdvancementData()).thenAnswer(i -> advancementDataWorld);

		// TeamArgument
		ScoreboardServer scoreboardServerMock = Mockito.mock(ScoreboardServer.class);
		Mockito.when(scoreboardServerMock.getTeam(anyString())).thenAnswer(invocation -> { // Scoreboard#getTeam is used for 1.16.5 instead of Scoreboard#getPlayerTeam
			String teamName = invocation.getArgument(0);
			Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamName);
			if (team == null) {
				return null;
			} else {
				return new ScoreboardTeam(scoreboardServerMock, teamName);
			}
		});
		Mockito.when(scoreboardServerMock.getObjective(anyString())).thenAnswer(invocation -> { // Scoreboard#getObjective
			String objectiveName = invocation.getArgument(0);
			org.bukkit.scoreboard.Objective bukkitObjective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objectiveName);
			if (bukkitObjective == null) {
				return null;
			} else {
				return new ScoreboardObjective(scoreboardServerMock, objectiveName, IScoreboardCriteria.a(bukkitObjective.getCriteria()).get(), new ChatComponentText(bukkitObjective.getDisplayName()), switch(bukkitObjective.getRenderType()) {
					case HEARTS:
						yield IScoreboardCriteria.EnumScoreboardHealthDisplay.HEARTS;
					case INTEGER:
						yield IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER;
				});
			}
		});
		Mockito.when(minecraftServerMock.getScoreboard()).thenReturn(scoreboardServerMock); // MinecraftServer#getScoreboard

		// WorldArgument (Dimension)
		Mockito.when(minecraftServerMock.getWorldServer(any(ResourceKey.class))).thenAnswer(invocation -> {
			// Get the ResourceKey<World> and extract the world name from it
			ResourceKey<net.minecraft.server.v1_16_R3.World> resourceKey = invocation.getArgument(0);
			String worldName = resourceKey.a().getKey();

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
				WorldServer bukkitWorldServerMock = Mockito.mock(WorldServer.class);
				Mockito.when(bukkitWorldServerMock.getWorld()).thenReturn(craftWorldMock);
				return bukkitWorldServerMock;
			}
		});

		// Player lists
		Mockito.when(minecraftServerMock.getPlayerList()).thenAnswer(i -> playerListMock);
		Mockito.when(minecraftServerMock.getPlayerList().getPlayers()).thenAnswer(i -> players);

		// PlayerArgument
		UserCache userCacheMock = Mockito.mock(UserCache.class);
		Mockito.when(userCacheMock.getProfile(anyString())).thenAnswer(invocation -> {
			String playerName = invocation.getArgument(0);
			for (EntityPlayer onlinePlayer : players) {
				if (onlinePlayer.getBukkitEntity().getName().equals(playerName)) {
					return new GameProfile(onlinePlayer.getBukkitEntity().getUniqueId(), playerName);
				}
			}
			return null;
		});
		Mockito.when(minecraftServerMock.getUserCache()).thenReturn(userCacheMock);

		// RecipeArgument
		Mockito.when(minecraftServerMock.getCraftingManager()).thenAnswer(i -> this.recipeManager);

		// FunctionArgument
		// We're using 2 as the function compilation level.
		// Mockito.when(minecraftServerMock.??()).thenReturn(2);
		Mockito.when(minecraftServerMock.getFunctionData()).thenAnswer(i -> {
			CustomFunctionData customFunctionData = Mockito.mock(CustomFunctionData.class);

			// Functions
			Mockito.when(customFunctionData.a(any(MinecraftKey.class))).thenAnswer(invocation -> Optional.ofNullable(functions.get(invocation.getArgument(0))));
			Mockito.when(customFunctionData.f()).thenAnswer(invocation -> functions.keySet());

			// Tags
			Mockito.when(customFunctionData.b(any())).thenAnswer(invocation -> {
				Collection<CustomFunction> tagsFromResourceLocation = tags.getOrDefault(invocation.getArgument(0), List.of());
				return Tag.b(Set.copyOf(tagsFromResourceLocation));
			});
			Mockito.when(customFunctionData.g()).thenAnswer(invocation -> tags.keySet());

			// Command dispatcher
			Mockito.when(customFunctionData.getCommandDispatcher()).thenAnswer(invocation -> Brigadier.getCommandDispatcher());

			// Command chain length
			Mockito.when(customFunctionData.b()).thenReturn(65536);

			return customFunctionData;
		});

		Mockito.when(minecraftServerMock.getGameRules()).thenAnswer(i -> new GameRules());
		Mockito.when(minecraftServerMock.getMethodProfiler()).thenAnswer(i -> GameProfilerDisabled.a);

		// Commands object, used when creating VanillaCommandWrappers in NMS#wrapToVanillaCommandWrapper
		net.minecraft.server.v1_16_R3.CommandDispatcher commands = new net.minecraft.server.v1_16_R3.CommandDispatcher();
		MockPlatform.setField(commands.getClass(), "b", commands, getBrigadierDispatcher());
		minecraftServerMock.vanillaCommandDispatcher = commands;

		return (T) minecraftServerMock;
	}
}

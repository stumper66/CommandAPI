package dev.jorel.commandapi.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import be.seeseemelk.mockbukkit.help.HelpMapMock;
import dev.jorel.commandapi.*;
import com.mojang.brigadier.tree.CommandNode;
import dev.jorel.commandapi.SafeVarHandle;
import dev.jorel.commandapi.test.exception.UnimplementedMethodException;
import net.minecraft.commands.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_17_R1.CraftParticle;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemFactory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.help.HelpTopic;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.mockito.Mockito;

import com.google.common.collect.Streams;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.enchantments.EnchantmentMock;
import be.seeseemelk.mockbukkit.potion.MockPotionEffectType;
import dev.jorel.commandapi.commandsenders.AbstractCommandSender;
import dev.jorel.commandapi.commandsenders.BukkitCommandSender;
import dev.jorel.commandapi.commandsenders.BukkitPlayer;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.Advancement;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TextComponent;
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
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagCollection;
import net.minecraft.util.profiling.metrics.profiling.InactiveMetricsRecorder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType;

public class MockNMS extends Enums {
	private static final SafeVarHandle<HelpMapMock, Map<String, HelpTopic>> helpMapTopics =
		SafeVarHandle.ofOrNull(HelpMapMock.class, "topics", "topics", Map.class);

	static ServerAdvancementManager advancementDataWorld = new ServerAdvancementManager(null);

	MinecraftServer minecraftServerMock = null;
	List<ServerPlayer> players = new ArrayList<>();
	PlayerList playerListMock;
	final RecipeManager recipeManager;
	Map<ResourceLocation, CommandFunction> functions = new HashMap<>();
	Map<ResourceLocation, Collection<CommandFunction>> tags = new HashMap<>();

	public MockNMS(CommandAPIBukkit<?> baseNMS) {
		super(baseNMS);

		CommandAPIBukkit<CommandSourceStack> nms = Mockito.spy(super.baseNMS);
		// Stub in our getMinecraftServer implementation
		Mockito.when(nms.getMinecraftServer()).thenAnswer(i -> getMinecraftServer());
		// Stub in our getSimpleCommandMap implementation
		//  Note that calling `nms.getSimpleCommandMap()` throws a
		//  class cast exception  (`CraftServer` vs `CommandAPIServerMock`),
		//  so we have to mock with `doAnswer` instead of `when`
		Mockito.doAnswer(i -> getSimpleCommandMap()).when(nms).getSimpleCommandMap();
		super.baseNMS = nms;

		// Initialize WorldVersion (game version)
		SharedConstants.tryDetectVersion();

		// MockBukkit is very helpful and registers all of the potion
		// effects and enchantments for us. We need to not do this (because
		// we call Bootstrap.bootStrap() below which does the same thing)
		unregisterAllEnchantments();
		unregisterAllPotionEffects();

		// Invoke Minecraft's registry. This also initializes all argument types.
		// How convenient!
		Bootstrap.bootStrap();

		// Don't use EnchantmentMock.registerDefaultEnchantments because we want
		// to specify what enchantments to mock (i.e. only 1.18 ones, and not any
		// 1.19 ones!)
		registerDefaultPotionEffects();
		registerDefaultEnchantments();

		this.recipeManager = new RecipeManager();
		this.functions = new HashMap<>();
		registerDefaultRecipes();

		// Set up playerListMock
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
		PotionEffectType.stopAcceptingRegistrations();
	}

	private static void registerPotionEffectType(int id, String name, boolean instant, int rgb) {
		PotionEffectType.registerPotionEffectType(new MockPotionEffectType(id, name, instant, Color.fromRGB(rgb)));
	}

	private void unregisterAllEnchantments() {
		getFieldAs(Enchantment.class, "byName", null, Map.class).clear();
		getFieldAs(Enchantment.class, "byKey", null, Map.class).clear();
		setField(Enchantment.class, "acceptingNew", null, true);
	}

	private void registerDefaultEnchantments() {
		for (Enchantment enchantment : getEnchantments()) {
			if (Enchantment.getByKey(enchantment.getKey()) == null) {
				Enchantment.registerEnchantment(new EnchantmentMock(enchantment.getKey(), enchantment.getKey().getKey()));
			}
		}
	}
	
	private void registerDefaultRecipes() {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<Recipe<?>> recipes = (List) getRecipes(MinecraftServer.class)
			.stream()
			.map(p -> RecipeManager.fromJson(new ResourceLocation(p.first()), p.second()))
			.toList();
		recipeManager.replaceRecipes(recipes);
	}

	/**************************
	 * MockPlatform overrides *
	 **************************/

	@Override
	public ItemFactory getItemFactory() {
		return CraftItemFactory.instance();
	}

	@Override
	public List<String> getAllItemNames() {
		return StreamSupport.stream(Registry.ITEM.spliterator(), false)
			.map(Object::toString)
			.map(s -> "minecraft:" + s)
			.sorted()
			.toList();
	}

	@Override
	public String[] compatibleVersions() {
		return baseNMS.compatibleVersions();
	}

	@Override
	public SimpleCommandMap getSimpleCommandMap() {
		return ((ServerMock) Bukkit.getServer()).getCommandMap();
	}

	@Override
	public CommandSourceStack getBrigadierSourceFromCommandSender(AbstractCommandSender<? extends CommandSender> senderWrapper) {
		CommandSender sender = senderWrapper.getSource();
		CommandSourceStack css = Mockito.mock(CommandSourceStack.class);
		Mockito.when(css.getBukkitSender()).thenReturn(sender);

		if (sender instanceof Entity entity) {
			// LocationArgument
			Location loc = entity.getLocation();
			Mockito.when(css.getPosition()).thenReturn(new Vec3(loc.getX(), loc.getY(), loc.getZ()));

			// If entity gives us a ServerLevel, use it, otherwise mock it
			ServerLevel worldServerLevel;
			if(entity.getWorld() instanceof CraftWorld cw) worldServerLevel = cw.getHandle();
			else worldServerLevel = Mockito.mock(ServerLevel.class);

			Mockito.when(css.getLevel()).thenReturn(worldServerLevel);
			Mockito.when(css.getLevel().hasChunkAt(any(BlockPos.class))).thenReturn(true);
			Mockito.when(css.getLevel().isInWorldBounds(any(BlockPos.class))).thenReturn(true);
			Mockito.when(css.getAnchor()).thenReturn(Anchor.EYES);

			// Get mocked MinecraftServer
			Mockito.when(css.getServer()).thenAnswer(s -> getMinecraftServer());

			// EntitySelectorArgument
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				ServerPlayer entityPlayerMock = Mockito.mock(ServerPlayer.class);
				CraftPlayer craftPlayerMock = Mockito.mock(CraftPlayer.class);

				// Extract these variables first in case the onlinePlayer is a Mockito object itself
				String name = onlinePlayer.getName();
				UUID uuid = onlinePlayer.getUniqueId();

				Mockito.when(craftPlayerMock.getName()).thenReturn(name);
				Mockito.when(craftPlayerMock.getUniqueId()).thenReturn(uuid);
				Mockito.when(entityPlayerMock.getBukkitEntity()).thenReturn(craftPlayerMock);
				Mockito.when(entityPlayerMock.getDisplayName()).thenReturn(new TextComponent(name)); // ChatArgument, AdventureChatArgument
				players.add(entityPlayerMock);
			}

			// CommandSourceStack#levels
			Mockito.when(css.levels()).thenAnswer(invocation -> {
				Set<ResourceKey<Level>> set = new HashSet<>();
				// We only need to implement resourceKey.a()

				for (World world : Bukkit.getWorlds()) {
					@SuppressWarnings("unchecked")
					ResourceKey<Level> key = Mockito.mock(ResourceKey.class);
					Mockito.when(key.location()).thenReturn(new ResourceLocation(world.getName()));
					set.add(key);
				}

				return set;
			});

			// RotationArgument
			Mockito.when(css.getRotation()).thenReturn(new Vec2(loc.getPitch(), loc.getYaw()));

			// CommandSourceStack#getAllTeams
			Mockito.when(css.getAllTeams()).thenAnswer(invocation -> {
				return Bukkit.getScoreboardManager().getMainScoreboard().getTeams().stream().map(Team::getName).toList();
			});

			// SoundArgument
			Mockito.when(css.getAvailableSoundEvents()).thenAnswer(invocation -> Registry.SOUND_EVENT.keySet());
			
			// RecipeArgument
			Mockito.when(css.getRecipeNames()).thenAnswer(invocation -> recipeManager.getRecipeIds());

			// ChatArgument, AdventureChatArgument
			Mockito.when(css.hasPermission(anyInt())).thenAnswer(invocation -> sender.isOp());
			Mockito.when(css.hasPermission(anyInt(), anyString())).thenAnswer(invocation -> sender.isOp());

			// FunctionArgument
			// We don't really need to do anything funky here, we'll just return the same CSS
			Mockito.when(css.withSuppressedOutput()).thenReturn(css);
			Mockito.when(css.withMaximumPermission(anyInt())).thenReturn(css);
		} else {
			// `getPosition` and `getRotation` are always accessed when `NMS#getSenderForCommand` is called
			//  If sender is an entity then we can give a physical location, but here we'll just give some defaults
			Mockito.when(css.getPosition()).thenReturn(new Vec3(0, 0, 0));
			Mockito.when(css.getRotation()).thenReturn(new Vec2(0, 0));
		}
		return css;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void createDispatcherFile(File file, CommandDispatcher dispatcher)
		throws IOException {
		baseNMS.createDispatcherFile(file, dispatcher);
	}

	@Override
	public World getWorldForCSS(CommandSourceStack clw) {
		return baseNMS.getWorldForCSS(clw);
	}

	@SuppressWarnings("deprecation")
	@Override
	public String getBukkitPotionEffectTypeName(PotionEffectType potionEffectType) {
		return MobEffect.byId(potionEffectType.getId()).getDescriptionId().replace("effect.minecraft.", "minecraft:");
	}
	
	@Override
	public String getNMSParticleNameFromBukkit(Particle particle) {
		CraftParticle craftParticle = CraftParticle.valueOf(particle.name());
		return MockPlatform.getFieldAs(CraftParticle.class, "minecraftKey", craftParticle, ResourceLocation.class).toString();
	}

	@SuppressWarnings("deprecation")
	@Override
	public List<NamespacedKey> getAllRecipes() {
		return recipeManager.getRecipeIds().map(k -> new NamespacedKey(k.getNamespace(), k.getPath())).toList();
	}

	@Override
	public <T> T getMinecraftServer() {
		throw new UnimplementedMethodException("This method should not be accessed here. Please use a platform-specific version of this method.");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addFunction(NamespacedKey key, List<String> commands) {
		if(Bukkit.getOnlinePlayers().isEmpty()) {
			throw new IllegalStateException("You need to have at least one player on the server to add a function");
		}

		ResourceLocation resourceLocation = new ResourceLocation(key.toString());
		CommandSourceStack css = getBrigadierSourceFromCommandSender(new BukkitPlayer(Bukkit.getOnlinePlayers().iterator().next()));

		// So for very interesting reasons, Brigadier.getCommandDispatcher()
		// gives a different result in this method than using getBrigadierDispatcher()
		this.functions.put(resourceLocation, CommandFunction.fromLines(resourceLocation, Brigadier.getCommandDispatcher(), css, commands));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addTag(NamespacedKey key, List<List<String>> commands) {
		if(Bukkit.getOnlinePlayers().isEmpty()) {
			throw new IllegalStateException("You need to have at least one player on the server to add a function");
		}

		ResourceLocation resourceLocation = new ResourceLocation(key.toString());
		CommandSourceStack css = getBrigadierSourceFromCommandSender(new BukkitPlayer(Bukkit.getOnlinePlayers().iterator().next()));

		List<CommandFunction> tagFunctions = new ArrayList<>();
		for(List<String> functionCommands : commands) {
			tagFunctions.add(CommandFunction.fromLines(resourceLocation, Brigadier.getCommandDispatcher(), css, functionCommands));
		}
		this.tags.put(resourceLocation, tagFunctions);
	}

	@Override
	public Player setupMockedCraftPlayer(String name) {
		CraftPlayer player = Mockito.mock(CraftPlayer.class);

		// getLocation and getWorld is used when creating the CommandSourceStack in MockNMS
		ServerLevel serverLevel = Mockito.mock(ServerLevel.class);
		CraftWorld world = Mockito.mock(CraftWorld.class);
		Mockito.when(world.getHandle()).thenReturn(serverLevel);
		Mockito.when(serverLevel.getWorld()).thenReturn(world);

		Mockito.when(player.getLocation()).thenReturn(new Location(world, 0, 0, 0));
		Mockito.when(player.getWorld()).thenReturn(world);

		// Provide proper handle as VanillaCommandWrapper expects
		CommandSourceStack css = getBrigadierSourceFromCommandSender(wrapCommandSender(player));

		ServerPlayer handle = Mockito.mock(ServerPlayer.class);
		Mockito.when(handle.createCommandSourceStack()).thenReturn(css);

		Mockito.when(player.getHandle()).thenReturn(handle);


		// getName and getDisplayName are used when CommandSourceStack#withEntity is called
		net.minecraft.network.chat.Component nameComponent = new TextComponent(name);
		Mockito.when(handle.getName()).thenReturn(nameComponent);
		Mockito.when(handle.getDisplayName()).thenReturn(nameComponent);

		return player;
	}

	@Override
	public org.bukkit.advancement.Advancement addAdvancement(NamespacedKey key) {
		advancementDataWorld.advancements.advancements.put(new ResourceLocation(key.toString()),
			new Advancement(new ResourceLocation(key.toString()), null, null, null, new HashMap<>(), null));
		return new org.bukkit.advancement.Advancement() {

			@Override
			public NamespacedKey getKey() {
				return key;
			}

			@Override
			public Collection<String> getCriteria() {
				return List.of();
			}
		};
	}

	@Override
	public BukkitCommandSender<? extends CommandSender> getSenderForCommand(CommandContext<CommandSourceStack> cmdCtx, boolean forceNative) {
		return baseNMS.getSenderForCommand(cmdCtx, forceNative);
	}

	@Override
	public BukkitCommandSender<? extends CommandSender> getCommandSenderFromCommandSource(CommandSourceStack clw) {
		try {
			return wrapCommandSender(clw.getBukkitSender());
		} catch (UnsupportedOperationException e) {
			return null;
		}
	}
	
	@Override
	public HelpTopic generateHelpTopic(String commandName, String shortDescription, String fullDescription, String permission) {
		return baseNMS.generateHelpTopic(commandName, shortDescription, fullDescription, permission);
	}

	@Override
	public Map<String, HelpTopic> getHelpMap() {
		return helpMapTopics.get((HelpMapMock) Bukkit.getHelpMap());
	}
}

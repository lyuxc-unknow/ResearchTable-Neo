package snownee.researchtable.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.core.team.TeamHelper;
import snownee.researchtable.network.PacketSyncClient;

@EventBusSubscriber(modid = ResearchTable.MODID)
public class DataStorage {
	private static DataStorage INSTANCE;
	private final ServerLevel world;
	private static final Map<UUID, Object2IntMap<String>> records = new HashMap<>();
	private static final Map<String, Object2IntMap<String>> players = new HashMap<>();
	private static boolean changed = false;
	public static Object2IntMap<String> clientData;
	public static int clientVersion;

	public DataStorage(ServerLevel world) {
		this.world = world;
		load();
	}

	private void load() {
		records.clear();
		players.clear();
		changed = false;
		clientData = null;

		File folder = new File(world.getServer().getWorldPath(LevelResource.ROOT).toFile(), "data/");
		File file = new File(folder, ResearchTable.MODID + ".dat");
		CompoundTag data = null;

		if (file.exists() && file.isFile()) {
			try (InputStream stream = new FileInputStream(file)) {
				data = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
			} catch (Exception compressedReadFailure) {
				try {
					data = NbtIo.read(file.toPath());
				} catch (Exception plainReadFailure) {
					compressedReadFailure.addSuppressed(plainReadFailure);
					ResearchTable.LOGGER.warn("Failed to load research data from {}", file, compressedReadFailure);
				}
			}
		}
		if (data == null) {
			return;
		}

		int format = data.getInt("__v");
		if (format == 0) {
			readNamedPlayerRecords(data);
		} else if (format == 1) {
			readLegacyPlayerRecords(data);

			ListTag recordsData = data.getList("records", Tag.TAG_COMPOUND);
			for (Tag raw : recordsData) {
				CompoundTag recordData = (CompoundTag) raw;
				UUID k = recordData.getUUID("k");
				Object2IntMap<String> v = readPlayerData(recordData.getCompound("v"));
				if (!v.isEmpty()) {
					records.put(k, v);
				}
			}
		} else {
			throw new IllegalStateException("Unsupported research data format version: " + format);
		}
	}

	private static void readLegacyPlayerRecords(CompoundTag data) {
		if (data.contains("oldRecords", Tag.TAG_COMPOUND)) {
			readNamedPlayerRecords(data.getCompound("oldRecords"));
			return;
		}
		if (data.contains("oldRecords", Tag.TAG_LIST)) {
			ListTag oldRecords = data.getList("oldRecords", Tag.TAG_COMPOUND);
			for (Tag raw : oldRecords) {
				readNamedPlayerRecords((CompoundTag) raw);
			}
		}
	}

	private static void readNamedPlayerRecords(CompoundTag playersData) {
		for (String player : playersData.getAllKeys()) {
			if (playersData.contains(player, Tag.TAG_COMPOUND)) {
				Object2IntMap<String> researches = readPlayerData(playersData.getCompound(player));
				if (!researches.isEmpty()) {
					players.put(player, researches);
				}
			}
		}
	}

	private void save() {
		if (!changed) {
			return;
		}
		CompoundTag data = new CompoundTag();
		data.putInt("__v", 1);

		CompoundTag playersData = new CompoundTag();
		players.forEach((player, researches) -> {
			if (!researches.isEmpty()) {
				CompoundTag playerData = writePlayerData(researches);
				if (!playerData.isEmpty()) {
					playersData.put(player, playerData);
				}
			}
		});
		if (!playersData.isEmpty()) {
			data.put("oldRecords", playersData);
		}

		ListTag recordsDataList = new ListTag();
		records.forEach((k, v) -> {
			if (!v.isEmpty()) {
				CompoundTag progressData = writePlayerData(v);
				if (progressData.isEmpty()) {
					return;
				}
				CompoundTag recordsData = new CompoundTag();
				recordsData.putUUID("k", k);
				recordsData.put("v", progressData);
				recordsDataList.add(recordsData);
			}
		});
		if (!recordsDataList.isEmpty()) {
			data.put("records", recordsDataList);
		}

		File folder = new File(world.getServer().getWorldPath(LevelResource.ROOT).toFile(), "data/");
		File file = new File(folder, ResearchTable.MODID + ".dat");
		try {
			if (!folder.exists() && !folder.mkdirs()) {
				throw new IllegalStateException("Could not create data directory: " + folder);
			}
			if (!file.exists()) {
				file.createNewFile();
			}
			try (OutputStream stream = new FileOutputStream(file)) {
				NbtIo.writeCompressed(data, stream);
			}
			changed = false;
		} catch (Exception e) {
			ResearchTable.LOGGER.error("Failed to save research data to {}", file, e);
		}
	}

	public static boolean loaded() {
		return INSTANCE != null;
	}

	public static int complete(UUID uuid, Research research) {
		return setCount(uuid, research, count(uuid, research) + 1);
	}

	public static int setCount(UUID uuid, Research research, int count) {
		if (!loaded()) {
			return -1;
		}
		Object2IntMap<String> researches = getRecords(uuid);
		if (count > 0) {
			researches.put(research.getName(), count);
		} else {
			researches.removeInt(research.getName());
		}
		changed = true;
		syncClientAllMembers(uuid);
		return count;
	}

	public static Object2IntMap<String> getRecords(UUID uuid) {
		if (!loaded()) {
			return Object2IntMaps.emptyMap();
		}
		UUID owner = TeamHelper.provider.getOwner(uuid);
		if (owner != null) {
			return records.computeIfAbsent(owner, $ -> new Object2IntOpenHashMap<>());
		} else {
			return records.computeIfAbsent(uuid, $ -> new Object2IntOpenHashMap<>());
		}
	}

	public static int count(UUID uuid, String research) {
		if (loaded()) {
			return getRecords(uuid).getInt(research);
		} else if (clientData != null) {
			return clientData.getInt(research);
		}
		return 0;
	}

	public static int count(UUID uuid, Research research) {
		return count(uuid, research.getName());
	}

	public static boolean hasAllOf(UUID uuid, Collection<String> researches) {
		for (String research : researches) {
			if (count(uuid, research) == 0) {
				return false;
			}
		}
		return true;
	}

	@SubscribeEvent
	public static void onWorldLoaded(LevelEvent.Load event) {
		Level level = (Level) event.getLevel();
		if (!level.isClientSide && level instanceof ServerLevel && level.dimension() == Level.OVERWORLD) {
			INSTANCE = new DataStorage((ServerLevel) level);
		}
	}

	@SubscribeEvent
	public static void onWorldSaved(LevelEvent.Save event) {
		if (loaded() && event.getLevel() == INSTANCE.world) {
			INSTANCE.save();
		}
	}

	@SubscribeEvent
	public static void onWorldUnloaded(LevelEvent.Unload event) {
		if (loaded() && event.getLevel() == INSTANCE.world) {
			INSTANCE.save();
			INSTANCE = null;
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
		if (!loaded() || event.getEntity() instanceof FakePlayer) {
			return;
		}
		String name = event.getEntity().getName().getString();
		UUID uuid = event.getEntity().getGameProfile().getId();
		if (players.containsKey(name)) {
			Object2IntMap<String> data = players.get(name);
			players.remove(name);
			mergeProgress(data, uuid);
			syncClientAllMembers(uuid);
		} else {
			syncClient(uuid);
		}
	}

	public static void mergeProgress(Object2IntMap<String> from, UUID uuid) {
		Object2IntMap<String> to = getRecords(uuid);
		for (Entry<String> entry : from.object2IntEntrySet()) {
			int fromInt = entry.getIntValue();
			int toInt = to.getInt(entry.getKey());
			if (fromInt > toInt) {
				to.put(entry.getKey(), fromInt);
			}
		}
		changed = true;
	}

	private static void syncClientAllMembers(UUID uuid) {
		TeamHelper.provider.getMembers(uuid).forEach(DataStorage::syncClient);
	}

	private static void syncClient(UUID uuid) {
		Player player = getPlayer(uuid);
		if (player instanceof ServerPlayer sp && !(player instanceof FakePlayer)) {
			syncClient(sp);
		}
	}

	public static void syncClient(ServerPlayer player) {
		if (player instanceof FakePlayer) {
			return;
		}
		Object2IntMap<String> data = getRecords(player.getGameProfile().getId());
		PacketDistributor.sendToPlayer(player, new PacketSyncClient(data));
	}

	public static void handleClientSync(Object2IntMap<String> data) {
		clientData = data;
		++clientVersion;
	}

	public static Object2IntMap<String> readPlayerData(CompoundTag data) {
		Set<String> keySet = data.getAllKeys();
		Object2IntMap<String> researches = new Object2IntOpenHashMap<>(keySet.size());
		for (String research : keySet) {
			if (data.contains(research, Tag.TAG_INT)) {
				int count = data.getInt(research);
				if (count > 0) {
					researches.put(research, count);
				}
			}
		}
		return researches;
	}

	public static CompoundTag writePlayerData(Object2IntMap<String> map) {
		CompoundTag data = new CompoundTag();
		map.forEach((research, count) -> {
			if (count > 0) {
				data.putInt(research, count);
			}
		});
		return data;
	}

	@Nullable
	public static ServerPlayer getPlayer(UUID uuid) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) {
			return null;
		}
		return server.getPlayerList().getPlayer(uuid);
	}

	public static void onPlayerAdd(UUID uuid, UUID owner) {
		mergeProgress(records.getOrDefault(uuid, Object2IntMaps.emptyMap()), owner);
		records.remove(uuid);
		syncClientAllMembers(owner);
	}
}

package snownee.researchtable;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import snownee.researchtable.client.ClientInit;
import snownee.researchtable.command.CommandResearch;
import snownee.researchtable.core.CriterionResearchCount;
import snownee.researchtable.core.CriterionResearches;
import snownee.researchtable.core.CriterionScore;
import snownee.researchtable.core.OpenTableEvent;
import snownee.researchtable.network.NetworkChannel;
import snownee.researchtable.plugin.astage.CriterionStages;
import snownee.researchtable.plugin.crafttweaker.ConditionCrTItem;
import snownee.researchtable.plugin.crafttweaker.ConditionCrTLiquid;
import snownee.researchtable.plugin.forge.ConditionForgeEnergy;
import snownee.researchtable.plugin.ftbteam.FTBTeamProvider;
import snownee.researchtable.plugin.kubejs.ConditionKubeJSFluid;
import snownee.researchtable.plugin.kubejs.ConditionKubeJSItem;
import snownee.researchtable.plugin.minecraft.ConditionExperience;
import snownee.researchtable.plugin.minecraft.ConditionFluid;
import snownee.researchtable.plugin.minecraft.ConditionItem;
import snownee.researchtable.plugin.minecraft.CriterionBiome;
import snownee.researchtable.plugin.minecraft.CriterionDimension;
import snownee.researchtable.plugin.minecraft.CriterionTime;
import snownee.researchtable.plugin.minecraft.CriterionWeather;

@Mod(ResearchTable.MODID)
@EventBusSubscriber(modid = ResearchTable.MODID)
public class ResearchTable {
	public static final String MODID = "researchtable";
	public static final String NAME = "ResearchTable";

	public static String scoreFormattingText;
	public static String[] scores;

	public static final Logger LOGGER = LogManager.getLogger(NAME);

	public ResearchTable(IEventBus modBus, ModContainer container) {
		Registration.register(modBus);
		modBus.addListener(this::commonSetup);
		modBus.addListener(NetworkChannel::register);
		ModConfig.register(container);
		NeoForge.EVENT_BUS.register(CommandResearch.class);
		if (FMLEnvironment.dist == Dist.CLIENT) {
			ClientInit.register(container);
		}
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		// Force class-init so ConditionType / CriterionType registries are populated before any sync
		// packet is decoded on either side. Without this, the type IDs would be unresolvable when a
		// client receives PacketSyncResearchList because the classes are only touched lazily by CrT.
		registerTypes();
	}

	private static void registerTypes() {
		Objects.requireNonNull(CriterionResearches.TYPE);
		Objects.requireNonNull(CriterionResearchCount.TYPE);
		Objects.requireNonNull(CriterionScore.TYPE);
		Objects.requireNonNull(CriterionBiome.TYPE);
		Objects.requireNonNull(CriterionDimension.TYPE);
		Objects.requireNonNull(CriterionWeather.TYPE);
		Objects.requireNonNull(CriterionTime.TYPE);
		Objects.requireNonNull(ConditionItem.TYPE);
		Objects.requireNonNull(ConditionFluid.TYPE);
		Objects.requireNonNull(ConditionForgeEnergy.TYPE);
		Objects.requireNonNull(ConditionExperience.TYPE);
		if (ModList.get().isLoaded("crafttweaker")) {
			touchCrTTypes();
		}
		if (ModList.get().isLoaded("kubejs")) {
			touchKubeJSTypes();
		}
		if (ModList.get().isLoaded("astages")) {
			touchAStagesTypes();
		}
		if (ModList.get().isLoaded("ftbteams")) {
			initFTBTeams();
		}
	}

	private static void touchCrTTypes() {
		Objects.requireNonNull(ConditionCrTItem.TYPE);
		Objects.requireNonNull(ConditionCrTLiquid.TYPE);
	}

	private static void touchKubeJSTypes() {
		Objects.requireNonNull(ConditionKubeJSItem.TYPE);
		Objects.requireNonNull(ConditionKubeJSFluid.TYPE);
	}

	private static void touchAStagesTypes() {
		Objects.requireNonNull(CriterionStages.TYPE);
	}

	private static void initFTBTeams() {
		FTBTeamProvider.init();
	}

	@SubscribeEvent
	public static void onOpenTable(OpenTableEvent event) {
		Player player = event.getEntity();
		if (scores == null || scores.length == 0 || player.level().isClientSide) {
			return;
		}

		Scoreboard scoreboard = player.level().getScoreboard();
		CompoundTag helper = event.getTable().getData();

		boolean changed = false;
		for (String s : scores) {
			String key = "score." + s;
			int i = 0;
			Objective objective = scoreboard.getObjective(s);
			if (objective != null) {
				ReadOnlyScoreInfo info = scoreboard.getPlayerScoreInfo(player, objective);
				if (info != null) {
					i = info.value();
				}
			}
			if (!helper.contains(key) || helper.getInt(key) != i) {
				helper.putInt(key, i);
				changed = true;
			}
		}
		if (changed) {
			event.getTable().hasChanged = true;
			event.getTable().setChanged();
		}
	}
}

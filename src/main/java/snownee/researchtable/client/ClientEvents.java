package snownee.researchtable.client;

import dev.ftb.mods.ftblibrary.api.client.FTBLibraryClientApi;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import snownee.researchtable.Registration;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.client.gui.screen.TableScreen;
import snownee.researchtable.client.renderer.ConditionRenderer;
import snownee.researchtable.client.renderer.FluidConditionRenderer;
import snownee.researchtable.client.renderer.ItemConditionRenderer;
import snownee.researchtable.plugin.crafttweaker.ConditionCrTItem;
import snownee.researchtable.plugin.crafttweaker.ConditionCrTLiquid;
import snownee.researchtable.plugin.forge.ConditionForgeEnergy;
import snownee.researchtable.plugin.forge.RendererForgeEnergy;
import snownee.researchtable.plugin.kubejs.ConditionKubeJSFluid;
import snownee.researchtable.plugin.kubejs.ConditionKubeJSItem;
import snownee.researchtable.plugin.minecraft.ConditionExperience;
import snownee.researchtable.plugin.minecraft.ConditionFluid;
import snownee.researchtable.plugin.minecraft.ConditionItem;
import snownee.researchtable.plugin.minecraft.RendererExperience;

@EventBusSubscriber(modid = ResearchTable.MODID, value = Dist.CLIENT)
public final class ClientEvents {

	private ClientEvents() {
	}

	@SubscribeEvent
	public static void registerScreens(RegisterMenuScreensEvent event) {
		event.register(Registration.TABLE_MENU.get(), TableScreen::new);
		if (ModList.get().isLoaded("ftblibrary")) {
			FTBLibraryClientApi.get().addSidebarScreenBlacklist(TableScreen.class.getName());
		}
	}

	@SubscribeEvent
	public static void clientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			ConditionRenderer.register(ConditionItem.class, ItemConditionRenderer::new);
			ConditionRenderer.register(ConditionFluid.class, FluidConditionRenderer::new);
			ConditionRenderer.register(ConditionForgeEnergy.class, new RendererForgeEnergy.Factory());
			ConditionRenderer.register(ConditionExperience.class, new RendererExperience.Factory());
			if (ModList.get().isLoaded("crafttweaker")) {
				ConditionRenderer.register(
						ConditionCrTItem.class,
						ItemConditionRenderer::new);
				ConditionRenderer.register(
						ConditionCrTLiquid.class,
						FluidConditionRenderer::new);
			}
			if (ModList.get().isLoaded("kubejs")) {
				ConditionRenderer.register(
						ConditionKubeJSItem.class,
						ItemConditionRenderer::new);
				ConditionRenderer.register(
						ConditionKubeJSFluid.class,
						FluidConditionRenderer::new);
			}
		});
	}
}

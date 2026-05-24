package snownee.researchtable.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import snownee.researchtable.Registration;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.client.gui.GuiTable;
import snownee.researchtable.client.renderer.ConditionRenderer;
import snownee.researchtable.plugin.forge.ConditionForgeEnergy;
import snownee.researchtable.plugin.forge.RendererForgeEnergy;
import snownee.researchtable.plugin.minecraft.ConditionExperience;
import snownee.researchtable.plugin.minecraft.RendererExperience;

@EventBusSubscriber(modid = ResearchTable.MODID, value = Dist.CLIENT)
public final class ClientEvents {

	private ClientEvents() {
	}

	@SubscribeEvent
	public static void registerScreens(RegisterMenuScreensEvent event) {
		event.register(Registration.TABLE_MENU.get(), GuiTable::new);
	}

	@SubscribeEvent
	public static void clientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			ConditionRenderer.register(ConditionForgeEnergy.class, new RendererForgeEnergy.Factory());
			ConditionRenderer.register(ConditionExperience.class, new RendererExperience.Factory());
			if (ModList.get().isLoaded("crafttweaker")) {
				ConditionRenderer.register(
						snownee.researchtable.plugin.crafttweaker.ConditionCrTItem.class,
						new snownee.researchtable.plugin.crafttweaker.RendererCrTItem.Factory());
				ConditionRenderer.register(
						snownee.researchtable.plugin.crafttweaker.ConditionCrTLiquid.class,
						new snownee.researchtable.plugin.crafttweaker.RendererCrTLiquid.Factory());
			}
		});
	}
}

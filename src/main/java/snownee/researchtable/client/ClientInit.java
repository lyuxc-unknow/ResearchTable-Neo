package snownee.researchtable.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only initialization invoked from the main mod class.
 * Centralizes anything that references client-only NeoForge classes
 * so the server JVM never tries to load them.
 */
public final class ClientInit {

	private ClientInit() {
	}

	public static void register(ModContainer container) {
		container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
	}
}

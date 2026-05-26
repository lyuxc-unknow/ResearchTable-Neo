package snownee.researchtable.core;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import snownee.researchtable.block.TileTable;

public class OpenTableEvent extends PlayerEvent implements ICancellableEvent {
	private final TileTable table;

	public OpenTableEvent(Player player, TileTable table) {
		super(player);
		this.table = table;
	}

	public TileTable getTable() {
		return table;
	}
}

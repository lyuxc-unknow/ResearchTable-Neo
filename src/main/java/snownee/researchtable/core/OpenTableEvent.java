package snownee.researchtable.core;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import snownee.researchtable.block.TableBlockEntity;

public class OpenTableEvent extends PlayerEvent implements ICancellableEvent {
	private final TableBlockEntity table;

	public OpenTableEvent(Player player, TableBlockEntity table) {
		super(player);
		this.table = table;
	}

	public TableBlockEntity getTable() {
		return table;
	}
}

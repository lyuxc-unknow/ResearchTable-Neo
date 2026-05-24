package snownee.researchtable.core.team;

import java.util.UUID;

import javax.annotation.Nonnull;

import snownee.researchtable.core.DataStorage;

public class TeamHelper {
	@Nonnull
	public static TeamProvider provider = TeamProvider.Stub.INSTANCE;

	public static void onPlayerAdd(UUID uuid, UUID owner) {
		DataStorage.onPlayerAdd(owner, owner);
	}
}

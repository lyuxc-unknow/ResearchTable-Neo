package snownee.researchtable.plugin.ftbteam;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.PlayerJoinedPartyTeamEvent;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import snownee.researchtable.api.team.TeamProvider;
import snownee.researchtable.core.team.TeamHelper;

public class FTBTeamProvider implements TeamProvider {
	public static final FTBTeamProvider INSTANCE = new FTBTeamProvider();

	public static void init() {
		TeamHelper.provider = INSTANCE;
		TeamEvent.PLAYER_JOINED_PARTY.register(INSTANCE::onPlayerJoinTeam);
	}

	public void onPlayerJoinTeam(PlayerJoinedPartyTeamEvent event) {
		UUID playerUUID = event.getPlayer().getUUID();
		UUID newOwner = event.getTeam().getOwner();
		// Owner creating their own party: records[playerUUID] already keyed under what will be
		// the team owner UUID, so no migration is needed.
		if (playerUUID.equals(newOwner)) {
			return;
		}
		TeamHelper.onPlayerAdd(playerUUID, newOwner);
	}

	@Override
	public @Nullable UUID getOwner(UUID player) {
		Team teams = FTBTeamsAPI.api().getManager().getTeamByID(player).orElse(null);
		if (teams != null) {
			return teams.getOwner();
		}
		return null;
	}

	@Override
	public Collection<UUID> getMembers(UUID player) {
		Team teams = FTBTeamsAPI.api().getManager().getTeamByID(player).orElse(null);
		if (teams != null) {
			return teams.getMembers();
		}
		return List.of();
	}

	@Override
	public @Nullable String getTeamName(UUID player) {
		Team teams = FTBTeamsAPI.api().getManager().getTeamByID(player).orElse(null);
		if (teams != null) {
			return teams.getShortName();
		}
		return "";
	}
}

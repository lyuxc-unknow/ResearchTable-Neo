package snownee.researchtable.core;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import snownee.researchtable.ModConfig;
import snownee.researchtable.api.IReward;

public class RewardExecute implements IReward {
	private final String[] commands;

	public RewardExecute(String... commands) {
		this.commands = commands;
	}

	@Override
	public void earn(Level world, BlockPos pos, Player player) {
		MinecraftServer server = player.getServer();
		if (server == null) {
			return;
		}
		CommandSourceStack source;
		if (ModConfig.nonPrivilegedMode) {
			source = player.createCommandSourceStack();
		} else {
			player.getDisplayName();
			source = new CommandSourceStack(
					CommandSource.NULL,
					Vec3.atCenterOf(pos),
					Vec2.ZERO,
					(net.minecraft.server.level.ServerLevel) world,
					2,
					player.getName().getString(),
					player.getDisplayName(),
					server,
					player);
		}
		for (String command : commands) {
			server.getCommands().performPrefixedCommand(source, command);
		}
	}
}

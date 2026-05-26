package snownee.researchtable.client.gui.container;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import snownee.researchtable.Registration;
import snownee.researchtable.block.TileTable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TableContainer extends AbstractContainerMenu {

	private final BlockPos pos;
	@Nullable
	private final TileTable tile;
	private final Inventory inventory;

	public TableContainer(int id, Inventory inventory, BlockPos pos) {
		this(id, inventory, pos, resolveTile(inventory, pos));
	}

	public TableContainer(int id, Inventory inventory, BlockPos pos, @Nullable TileTable tile) {
		super(Registration.TABLE_MENU.get(), id);
		this.pos = pos;
		this.tile = tile;
		this.inventory = inventory;
	}

	@Nullable
	private static TileTable resolveTile(Inventory inv, BlockPos pos) {
		BlockEntity be = inv.player.level().getBlockEntity(pos);
		return be instanceof TileTable t ? t : null;
	}

	public BlockPos getPos() {
		return pos;
	}

	@Nullable
	public TileTable getTile() {
		return tile;
	}

	@Override
	public boolean stillValid(Player playerIn) {
		if (tile != null && tile.hasLevel()) {
			return playerIn.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
		}
		return false;
	}

	@Override
	public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
		return net.minecraft.world.item.ItemStack.EMPTY;
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		if (tile != null && tile.hasChanged) {
			Level level = tile.getLevel();
			if (level instanceof ServerLevel serverLevel) {
				BlockState state = tile.getBlockState();
				serverLevel.sendBlockUpdated(tile.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
			}
			tile.hasChanged = false;
		}
	}
}

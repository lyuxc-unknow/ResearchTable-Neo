package snownee.researchtable.block;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Objects;

import com.mojang.authlib.GameProfile;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import snownee.researchtable.Registration;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.api.ICondition;
import snownee.researchtable.client.gui.container.TableContainer;
import snownee.researchtable.core.ConditionTypes;
import snownee.researchtable.core.DataStorage;
import snownee.researchtable.core.Research;
import snownee.researchtable.core.ResearchList;
import snownee.researchtable.core.team.TeamHelper;
import snownee.researchtable.plugin.minecraft.ExperienceHelper;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TableBlockEntity extends BlockEntity implements MenuProvider {

	public class ResearchItemWrapper implements IItemHandler {

		ResearchItemWrapper() {
		}

		@Override
		public int getSlots() {
			return getResearch() != null && !canComplete ? 1 : 0;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (slot == 0 && getResearch() != null && !stack.isEmpty() && !canComplete) {
				long matched = match(ConditionTypes.ITEM, stack, simulate);
				var itemStack = stack.copy();
				itemStack.setCount(stack.getCount() - (int) matched);
				return itemStack;
			}
			return stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return Integer.MAX_VALUE;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return slot == 0 && getResearch() != null && !canComplete;
		}
	}

	public class ResearchEnergyWrapper implements IEnergyStorage {

		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			if (canReceive()) {
				return (int) match(ConditionTypes.ENERGY, (long) maxReceive, simulate);
			}
			return 0;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			return 0;
		}

		@Override
		public int getEnergyStored() {
			return 0;
		}

		@Override
		public int getMaxEnergyStored() {
			return Integer.MAX_VALUE;
		}

		@Override
		public boolean canExtract() {
			return false;
		}

		@Override
		public boolean canReceive() {
			return getResearch() != null && !canComplete;
		}

	}

	public class ResearchFluidWrapper implements IFluidHandler {

		public ResearchFluidWrapper() {
		}

		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return FluidStack.EMPTY;
		}

		@Override
		public int getTankCapacity(int tank) {
			return Integer.MAX_VALUE;
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return getResearch() != null && !canComplete && match(ConditionTypes.FLUID, stack, true) > 0;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (getResearch() != null && !canComplete) {
				return (int) match(ConditionTypes.FLUID, resource, action.simulate());
			}
			return 0;
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return FluidStack.EMPTY;
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return FluidStack.EMPTY;
		}
	}

	@Nullable
	private String researchName;
	@Nullable
	private String lastResearchName;
	@Nullable
	private long[] progress;
	public boolean hasChanged;
	public String ownerName = "None";
	@Nullable
	private UUID ownerUUID;
	private final ResearchItemWrapper itemHandler = new ResearchItemWrapper();
	private final ResearchEnergyWrapper energyHandler = new ResearchEnergyWrapper();
	private final ResearchFluidWrapper fluidHandler = new ResearchFluidWrapper();
	private boolean canComplete;
	private CompoundTag data = new CompoundTag();
	public boolean powered;

	public TableBlockEntity(BlockPos pos, BlockState state) {
		super(Registration.TABLE_BLOCK_ENTITY.get(), pos, state);
	}

	public IItemHandler getItemHandler() {
		return itemHandler;
	}

	public IEnergyStorage getEnergyHandler() {
		return energyHandler;
	}

	public IFluidHandler getFluidHandler() {
		return fluidHandler;
	}

	@Nullable
	public Research getResearch() {
		if (researchName == null) {
			return null;
		}
		Research r = ResearchList.find(researchName).orElse(null);
		// Detect post-reload condition list changes and reseat the progress array to the new size.
		// Without this the old progress[] would either short-read or under-fill match() and the
		// research could never complete.
		if (r != null && progress != null && progress.length != r.getConditions().size()) {
			progress = new long[r.getConditions().size()];
			refreshCanComplete();
		}
		return r;
	}

	@Nullable
	public Research getLastResearch() {
		return lastResearchName == null ? null : ResearchList.find(lastResearchName).orElse(null);
	}

	@Nullable
	public UUID getOwnerUUID() {
		return this.ownerUUID;
	}

	public void setOwnerUUID(UUID uuid) {
		if (this.ownerUUID == null) {
			this.ownerUUID = uuid;
		} else {
			ResearchTable.logger.debug("An attempt of re-setting research table owner uuid occurred. Action aborted.");
		}
	}

	public void setResearch(@Nullable Research research) {
		String newName = research == null ? null : research.getName();
		if (this.researchName != null && this.researchName.equals(newName)) {
			return;
		}
		if (this.researchName != null) {
			lastResearchName = this.researchName;
		}
		this.researchName = newName;
		if (research == null) {
			progress = null;
		} else {
			progress = new long[research.getConditions().size()];
		}
		refreshCanComplete();
		invalidateCapabilities();
	}

	private void readData(CompoundTag tag) {
		if (tag.contains("data", 10)) {
			data = tag.getCompound("data");
		}
		if (tag.contains("owner", 10)) {
			CompoundTag credential = tag.getCompound("owner");
			this.ownerName = credential.getString("name");
			if (credential.contains("uuid")) {
				this.ownerUUID = credential.getUUID("uuid");
			}
		}
		if (tag.contains("research", 8)) {
			String name = tag.getString("research");
			researchName = name;
			Research r = ResearchList.find(name).orElse(null);
			if (r != null) {
				progress = new long[r.getConditions().size()];
				for (int i = 0; i < progress.length; i++) {
					if (tag.contains("progress" + i, 4)) {
						progress[i] = tag.getLong("progress" + i);
					}
				}
				refreshCanComplete();
			} else {
				// Research name persisted, but not present in the current ResearchList. Could be
				// a pre-sync BE update on the client, or a script removal. getResearch() will lazily
				// allocate progress next time the name resolves.
				progress = null;
				canComplete = false;
			}
			invalidateCapabilities();
		} else {
			setResearch(null);
		}
		hasChanged = true;
	}

	private void writeData(CompoundTag tag) {
		CompoundTag credential = new CompoundTag();
		if (!ownerName.isEmpty()) {
			credential.putString("name", ownerName);
		}
		if (ownerUUID != null) {
			credential.putUUID("uuid", ownerUUID);
		}
		tag.put("owner", credential);
		if (researchName != null && progress != null) {
			tag.putString("research", researchName);
			for (int i = 0; i < progress.length; i++) {
				tag.putLong("progress" + i, progress[i]);
			}
		}
		tag.put("data", data);
	}

	@Override
	public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains("last", 8)) {
			lastResearchName = tag.getString("last");
		}
		if (tag.contains("powered", 1)) {
			powered = tag.getBoolean("powered");
		}
		readData(tag);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		writeData(tag);
		if (lastResearchName != null) {
			tag.putString("last", lastResearchName);
		}
		tag.putBoolean("powered", powered);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		writeData(tag);
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
		super.handleUpdateTag(tag, registries);
		readData(tag);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
		readData(pkt.getTag());
	}

	public float getProgress() {
		Research r = getResearch();
		if (r == null || progress == null) {
			return 0;
		}
		List<ICondition> conditions = r.getConditions();
		if (conditions.isEmpty()) {
			return 100;
		}
		double sum = 0;
		for (int i = 0; i < conditions.size(); i++) {
			if (conditions.get(i).getGoal() == 0) {
				continue;
			}
			sum += (double) progress[i] / conditions.get(i).getGoal();
		}
		return (float) (sum / conditions.size()) * 100;
	}

	public long getProgress(int index) {
		if (progress != null && index >= 0 && index < progress.length) {
			return progress[index];
		}
		return 0;
	}

	public CompoundTag getData() {
		return data;
	}

	public void setData(CompoundTag data) {
		this.data = data;
	}

	public boolean hasPermission(@Nullable Player player) {
		if (player == null || player.level().isClientSide) {
			return true;
		}
		return ownerUUID == null
				|| player.getGameProfile().getId().equals(this.ownerUUID)
				|| Objects.equal(TeamHelper.provider.getOwner(player.getGameProfile().getId()), ownerUUID);
	}

	public boolean canComplete() {
		return canComplete;
	}

	private void refreshCanComplete() {
		Research r = researchName == null ? null : ResearchList.find(researchName).orElse(null);
		if (r == null || progress == null) {
			canComplete = false;
			setChanged();
			return;
		}
		List<ICondition> conditions = r.getConditions();
		if (progress.length != conditions.size()) {
			// Out-of-sync after /reload; rebuild and re-evaluate.
			progress = new long[conditions.size()];
		}
		for (int i = 0; i < progress.length; i++) {
			if (conditions.get(i).getGoal() > progress[i]) {
				canComplete = false;
				setChanged();
				return;
			}
		}
		canComplete = true;
		setChanged();
	}

	public void complete(Player player) {
		Research r = getResearch();
		if (r == null || ownerUUID == null || level == null || level.isClientSide) {
			return;
		}
		if (DataStorage.complete(ownerUUID, r) > 0) {
			r.complete(level, getBlockPos(), player);
			hasChanged = true;
		}
		setResearch(null);
	}

	public void submit(Player player) {
		if (getResearch() == null || level == null || level.isClientSide) {
			return;
		}
		for (int i = 0; i < player.getInventory().items.size(); ++i) {
			ItemStack stack = player.getInventory().items.get(i);
			ItemStack remain = itemHandler.insertItem(0, stack, false);
			if (remain != stack) {
				player.getInventory().items.set(i, remain);
			}
		}
		int availableXp = ExperienceHelper.getTotalXp(player);
		if (availableXp > 0) {
			long consumed = match(ConditionTypes.EXPERIENCE, availableXp, false);
			if (consumed > 0) {
				ExperienceHelper.drain(player, (int) consumed);
			}
		}
	}

	public <T> long match(Supplier<Class<T>> type, T e, boolean simulate) {
		Research r = getResearch();
		if (r == null || progress == null) {
			return 0;
		}
		List<ICondition> conditions = r.getConditions();
		long matched = 0;
		for (int i = 0; i < conditions.size(); ++i) {
			@SuppressWarnings("unchecked")
			ICondition<T> condition = (ICondition<T>) conditions.get(i);
			if (condition.getMatchType() == type) {
				long matchedIn = condition.matches(e);
				if (matchedIn < 0) {
					matchedIn = 0;
				}
				if (matchedIn > condition.getGoal() - progress[i]) {
					matchedIn = condition.getGoal() - progress[i];
				}
				if (matched + matchedIn < matched) {
					matchedIn = Long.MAX_VALUE - matched;
				}
				matched += matchedIn;
				if (matchedIn > 0 && !simulate) {
					progress[i] += matchedIn;
				}
				if (matched == Long.MAX_VALUE) {
					break;
				}
			}
		}
		if (matched > 0 && !simulate) {
			refreshCanComplete();
			hasChanged = true;
		}
		return matched;
	}

	public void putOwnerInfo(Player player) {
		if (player instanceof FakePlayer) {
			return;
		}
		UUID uuid = player.getGameProfile().getId();
		UUID owner = TeamHelper.provider.getOwner(uuid);
		if (owner != null) {
			ownerName = TeamHelper.provider.getTeamName(owner);
			if (ownerName == null) {
				ownerName = lookupName(owner, player.getServer());
			}
			if (ownerName == null) {
				ownerName = player.getName().getString();
			}
			setOwnerUUID(owner);
		} else {
			ownerName = player.getName().getString();
			setOwnerUUID(uuid);
		}
	}

	@Nullable
	private static String lookupName(UUID uuid, @Nullable MinecraftServer server) {
		if (server == null) {
			return null;
		}
		GameProfileCache cache = server.getProfileCache();
		if (cache == null) {
			return null;
		}
		return cache.get(uuid).map(GameProfile::getName).orElse(null);
	}

	@SuppressWarnings("unused")
	private static UUID touchUUIDUtil() {
		return UUIDUtil.uuidFromIntArray(new int[]{0, 0, 0, 0});
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block." + ResearchTable.MODID + ".table");
	}

	@Override
	@Nullable
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new TableContainer(containerId, inventory, getBlockPos(), this);
	}
}

package snownee.researchtable.core;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import snownee.researchtable.api.ICondition;

/**
 * Identifies a concrete {@link ICondition} implementation for network sync. Each implementation
 * registers exactly one type at mod init; the registry lets the client reconstruct the right
 * subclass from a wire-format payload sent by the server (used by PacketSyncResearchList).
 */
public final class ConditionType<C extends ICondition<?>> {

	public interface Writer<C extends ICondition<?>> {
		void write(RegistryFriendlyByteBuf buf, C condition);
	}

	public interface Reader<C extends ICondition<?>> {
		C read(RegistryFriendlyByteBuf buf);
	}

	private static final Map<ResourceLocation, ConditionType<?>> REGISTRY = new HashMap<>();

	public final ResourceLocation id;
	public final Writer<C> writer;
	public final Reader<C> reader;

	private ConditionType(ResourceLocation id, Writer<C> writer, Reader<C> reader) {
		this.id = id;
		this.writer = writer;
		this.reader = reader;
	}

	public static <C extends ICondition<?>> ConditionType<C> register(ResourceLocation id, Writer<C> w, Reader<C> r) {
		ConditionType<C> type = new ConditionType<>(id, w, r);
		ConditionType<?> previous = REGISTRY.putIfAbsent(id, type);
		if (previous != null) {
			throw new IllegalStateException("Duplicate ICondition type: " + id);
		}
		return type;
	}

	@Nullable
	public static ConditionType<?> get(ResourceLocation id) {
		return REGISTRY.get(id);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void writeAny(RegistryFriendlyByteBuf buf, ICondition<?> condition) {
		ConditionType type = condition.getType();
		buf.writeResourceLocation(type.id);
		type.writer.write(buf, condition);
	}

	@Nullable
	public static ICondition<?> readAny(RegistryFriendlyByteBuf buf) {
		ResourceLocation id = buf.readResourceLocation();
		ConditionType<?> type = REGISTRY.get(id);
		if (type == null) {
			throw new IllegalStateException("Unknown ICondition type: " + id);
		}
		return type.reader.read(buf);
	}
}

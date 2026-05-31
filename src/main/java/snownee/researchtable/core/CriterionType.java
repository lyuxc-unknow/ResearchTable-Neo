package snownee.researchtable.core;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import snownee.researchtable.api.ICriterion;

/**
 * Type registry for {@link ICriterion} subclasses, mirroring {@link ConditionType}. Criteria are
 * evaluated client-side (for the "available" check and failing text), so the client needs the full
 * implementation reconstructed from the wire-format payload, not just a render-time snapshot.
 */
public final class CriterionType<C extends ICriterion> {

	public interface Writer<C extends ICriterion> {
		void write(RegistryFriendlyByteBuf buf, C criterion);
	}

	public interface Reader<C extends ICriterion> {
		C read(RegistryFriendlyByteBuf buf);
	}

	private static final Map<ResourceLocation, CriterionType<?>> REGISTRY = new HashMap<>();

	public final ResourceLocation id;
	public final Writer<C> writer;
	public final Reader<C> reader;

	private CriterionType(ResourceLocation id, Writer<C> writer, Reader<C> reader) {
		this.id = id;
		this.writer = writer;
		this.reader = reader;
	}

	public static <C extends ICriterion> CriterionType<C> register(ResourceLocation id, Writer<C> w, Reader<C> r) {
		CriterionType<C> type = new CriterionType<>(id, w, r);
		CriterionType<?> previous = REGISTRY.putIfAbsent(id, type);
		if (previous != null) {
			throw new IllegalStateException("Duplicate ICriterion type: " + id);
		}
		return type;
	}

	@Nullable
	public static CriterionType<?> get(ResourceLocation id) {
		return REGISTRY.get(id);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void writeAny(RegistryFriendlyByteBuf buf, ICriterion criterion) {
		CriterionType type = criterion.getType();
		buf.writeResourceLocation(type.id);
		type.writer.write(buf, criterion);
	}

	@Nullable
	public static ICriterion readAny(RegistryFriendlyByteBuf buf) {
		ResourceLocation id = buf.readResourceLocation();
		CriterionType<?> type = REGISTRY.get(id);
		if (type == null) {
			throw new IllegalStateException("Unknown ICriterion type: " + id);
		}
		return type.reader.read(buf);
	}
}

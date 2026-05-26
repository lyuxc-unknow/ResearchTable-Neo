# ResearchTable 第三方扩展接入说明

本文档记录当前版本中第三方模组接入 ResearchTable 研究条件的方式，重点覆盖两类扩展点：

- 研究前条件：决定玩家是否可以开始某项研究。
- 研究时条件：研究开始后，向研究台提交进度并最终完成研究。

## 当前可扩展性结论

当前模组已经留出了可用的 Java 接口，第三方模组可以比较方便地编写兼容扩展：

- 研究前条件实现 `snownee.researchtable.api.ICriterion`。
- 研究时条件实现 `snownee.researchtable.api.ICondition<T>`。
- 条件类型通过 `snownee.researchtable.core.CriterionType.register(...)` 和 `snownee.researchtable.core.ConditionType.register(...)` 注册。
- 服务端会把研究列表同步到客户端，同步时使用上述类型注册表序列化和反序列化条件对象。
- CraftTweaker 兼容模组可以通过 `@ZenCodeType.Expansion("mods.researchtable.ResearchBuilder")` 向脚本构建器添加自己的方法。

需要注意的边界：

- `ConditionType` 和 `CriterionType` 当前位于 `core` 包，而不是 `api` 包。它们是公开类且可用，但从包名上看还不是完全稳定的公共 API 门面。
- 第三方条件类型必须在服务端和客户端都完成注册，并且要早于研究列表网络包解码。
- 自定义研究时条件如果不属于内置的物品、流体、能量、经验四种提交来源，需要第三方模组自行在合适交互中调用 `TileTable#match(...)` 推进进度。
- 研究时条件如果需要在 GUI 中显示图标、名称和提示，需要在客户端注册 `ConditionRenderer`。

## 研究前条件

研究前条件用于 `Research#canResearch(...)`。它只决定研究是否能开始，不保存进度。

实现步骤：

1. 创建一个实现 `ICriterion` 的类。
2. 定义一个静态 `CriterionType<你的条件类>`，使用唯一的 `ResourceLocation` 注册。
3. 在 `matches(...)` 中写判断逻辑。
4. 在 `getFailingText(...)` 中返回不满足条件时显示的文本。
5. 在模组初始化阶段强制触碰该 `TYPE`，确保两端注册表都有这个类型。
6. 通过 `ResearchBuilder.criteria.add(...)` 添加到研究。

示例：

```java
public class CriterionMyFlag implements ICriterion {
    public static final CriterionType<CriterionMyFlag> TYPE = CriterionType.register(
            ResourceLocation.fromNamespaceAndPath("mymod", "my_flag"),
            (buf, criterion) -> buf.writeUtf(criterion.flag),
            buf -> new CriterionMyFlag(buf.readUtf()));

    private final String flag;

    public CriterionMyFlag(String flag) {
        this.flag = flag;
    }

    @Override
    public boolean matches(Player player, CompoundTag data) {
        return data.getBoolean("mymod." + flag);
    }

    @Override
    public String getFailingText(Player player, CompoundTag data) {
        return "Requires flag: " + flag;
    }

    @Override
    public CriterionType<CriterionMyFlag> getType() {
        return TYPE;
    }
}
```

## 研究时条件

研究时条件用于研究开始后的进度提交。每个条件有一个目标值 `getGoal()`，每次提交时 `matches(...)` 返回本次可贡献的数量。

内置提交来源：

- `ConditionTypes.ITEM`：物品输入。
- `ConditionTypes.FLUID`：流体输入。
- `ConditionTypes.ENERGY`：NeoForge 能量输入。
- `ConditionTypes.EXPERIENCE`：玩家经验输入。

实现步骤：

1. 创建一个实现 `ICondition<T>` 的类。
2. `getMatchType()` 返回对应的提交来源。
3. `matches(T input)` 返回本次输入能够匹配的数量，不能匹配时返回 `0`。
4. `getGoal()` 返回完成目标。
5. 定义静态 `ConditionType<你的条件类>` 并注册读写逻辑。
6. 客户端可选注册 `ConditionRenderer`，否则 GUI 无法显示定制渲染。

示例：基于内置经验提交来源的条件。

```java
public class ConditionMyXP implements ICondition<Integer> {
    public static final ConditionType<ConditionMyXP> TYPE = ConditionType.register(
            ResourceLocation.fromNamespaceAndPath("mymod", "my_xp"),
            (buf, condition) -> buf.writeVarInt(condition.amount),
            buf -> new ConditionMyXP(buf.readVarInt()));

    private final int amount;

    public ConditionMyXP(int amount) {
        this.amount = amount;
    }

    @Override
    public Supplier<Class<Integer>> getMatchType() {
        return ConditionTypes.EXPERIENCE;
    }

    @Override
    public long matches(Integer availableXp) {
        return availableXp;
    }

    @Override
    public long getGoal() {
        return amount;
    }

    @Override
    public ConditionType<ConditionMyXP> getType() {
        return TYPE;
    }
}
```

## 自定义研究时提交来源

如果第三方模组需要自己的提交来源，可以定义自己的 `Supplier<Class<T>>`，并在自己的交互逻辑中调用研究台方块实体的 `match(...)`。

```java
public final class MyResearchTypes {
    public static final Supplier<Class<MyValue>> MY_VALUE = () -> MyValue.class;
}
```

条件返回该类型：

```java
@Override
public Supplier<Class<MyValue>> getMatchType() {
    return MyResearchTypes.MY_VALUE;
}
```

提交进度时：

```java
if (level.getBlockEntity(pos) instanceof TileTable table) {
    long accepted = table.match(MyResearchTypes.MY_VALUE, value, false);
}
```

`TileTable#match(...)` 会负责：

- 找到当前研究中 `getMatchType()` 相同的条件。
- 根据条件剩余目标裁剪本次进度。
- 写入进度并刷新是否可完成。

## CraftTweaker 扩展

第三方兼容模组可以给 `mods.researchtable.ResearchBuilder` 增加 ZenScript 方法。当前 `ResearchBuilder` 的 `criteria`、`conditions`、`triggers`、`rewards` 字段是 `public`，可直接添加对象。

示例：

```java
@ZenRegister
@ZenCodeType.Expansion("mods.researchtable.ResearchBuilder")
public class MyResearchTableExpansion {
    @ZenCodeType.Method
    public static ResearchBuilder setRequiredMyFlag(ResearchBuilder builder, String flag) {
        builder.criteria.add(new CriterionMyFlag(flag));
        return builder;
    }

    @ZenCodeType.Method
    public static ResearchBuilder addMyXPCondition(ResearchBuilder builder, int amount) {
        builder.conditions.add(new ConditionMyXP(amount));
        return builder;
    }
}
```

ZenScript 使用示例：

```zenscript
import mods.researchtable.ResearchTable;

val category = ResearchTable.addCategory(<item:minecraft:book>, "research.category.example");

ResearchTable.builder("example", category)
    .setTitle("research.example.title")
    .setDescription("research.example.description")
    .setRequiredMyFlag("unlocked")
    .addMyXPCondition(100)
    .build();
```

## 注册时机

条件类型注册必须发生在两端，且早于研究列表同步包解码。推荐做法是在第三方模组的 common setup 或等价初始化阶段强制触碰静态 `TYPE`：

```java
private void commonSetup(FMLCommonSetupEvent event) {
    Objects.requireNonNull(CriterionMyFlag.TYPE);
    Objects.requireNonNull(ConditionMyXP.TYPE);
}
```

如果条件需要客户端渲染，在 `FMLClientSetupEvent` 中注册：

```java
event.enqueueWork(() ->
        ConditionRenderer.register(ConditionMyXP.class, new RendererMyXP.Factory()));
```

## 兼容性建议

- 类型 ID 必须使用第三方模组自己的命名空间，例如 `mymod:my_xp`。
- 注册读写逻辑必须覆盖条件在客户端显示和判断所需的全部字段。
- 研究前条件会在客户端用于可用性和失败文本显示，因此不要只在服务端注册。
- 研究时条件的 `matches(...)` 不应修改外部状态；消耗输入由调用方或 ResearchTable 内置提交逻辑处理。
- 对内置物品、流体、能量、经验条件，优先复用现有提交来源；只有确实需要新交互时再定义自定义提交类型。

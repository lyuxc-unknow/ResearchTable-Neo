# ResearchTable Datapacks

ResearchTable loads vanilla datapack JSON before script integrations are applied.
CraftTweaker and KubeJS additions are merged into the same pending registry and published once after reload.

## Categories

Put reusable categories in:

```text
data/<namespace>/researchtable/categories/<id>.json
```

Example:

```json
{
  "icon": "minecraft:book",
  "name": "item.minecraft.book"
}
```

## Researches

Put researches in:

```text
data/<namespace>/researchtable/researches/<id>.json
```

The research name defaults to `<namespace>:<id>`. You can override it with `"name"`.

Example:

```json
{
  "category": "example:general",
  "title": "research.example.title",
  "description": "research.example.description",
  "icons": ["minecraft:book", "minecraft:paper"],
  "required_researches": ["example:first"],
  "conditions": [
    { "type": "item", "item": "minecraft:apple", "amount": 4 },
    { "type": "fluid", "fluid": "minecraft:water", "amount": 1000 },
    { "type": "energy", "amount": 500 },
    { "type": "xp", "amount": 10 }
  ],
  "triggers": [
    { "type": "command", "command": "/say research started" }
  ],
  "rewards": [
    { "type": "item", "item": { "item": "minecraft:diamond", "count": 1 } },
    { "type": "xp", "amount": 20 }
  ]
}
```

Supported criteria fields include `required_researches`, `optional_researches`, `score`, `scores`, `biomes`, `dimensions`, `weather`, `time`, and, when AStages is loaded, `required_stages` and `optional_stages`.

Optional score indicator files can be placed in:

```text
data/<namespace>/researchtable/score_indicators/<id>.json
```

```json
{
  "formatting_text": "Kills: %s",
  "scores": ["kills"]
}
```

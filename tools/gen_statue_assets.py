"""
Generates blockstates, placeholder models, item models and recipes for the ChunkComfort
statues and candle. Run from the project root:  python tools/gen_statue_assets.py

Candle positions go to assets/chunkcomfort/statues/<statue>.json (model pixels, statue
facing north, y = where the candle stands). Once a statue has its real Blockbench model,
set its last field to False so the generator keeps your block model, and put that
statue's real candle positions in LAYOUTS (tools/import_statue.py does the model import).

Every statue has its own texture at textures/blocks/statue/<statue>.png. If that file is
missing, a flat placeholder in the statue's colour is written; existing textures are never
overwritten.
"""
import json, os, struct, zlib

ROOT = "src/main/resources/assets/chunkcomfort"

# Placeholder layout: candles on the pedestal top around the pillar
PLACEHOLDER_SLOTS = [(3, 4, 3), (13, 4, 3), (3, 4, 8), (13, 4, 8), (3, 4, 13), (13, 4, 13), (8, 4, 3)]
CANDLE_HEIGHT = 4

# Real candle positions per statue (from Blockbench, see import_statue.py), replaces the
# placeholder layout. Height is the candle's wax height, leave it out for the default 4.
# The dwarf (artisan) layout, every model copied from the dwarf file shares it
DWARF_SLOTS = [(14, 2, 14, 3), (14, 2, 12, 2), (14, 2, 10, 1), (14, 2, 8, 2), (14, 2, 6, 1), (14, 2, 4, 3),
               (14, 2, 2, 2), (12, 2, 2, 1), (10, 2, 2, 2), (8, 2, 2, 1), (6, 2, 2, 3), (4, 2, 2, 2),
               (2, 2, 2, 1), (2, 2, 4, 2), (2, 2, 6, 1), (2, 2, 8, 3), (2, 2, 10, 2), (2, 2, 12, 1),
               (2, 2, 14, 2)]

LAYOUTS = {
    # "wind_statue": [(x, y, z, height), ...],
    "artisan_statue": DWARF_SLOTS,
    "fortune_statue": DWARF_SLOTS,
    # Placeholders: a copy of the dwarf model and texture until they get their own
    "wind_statue": DWARF_SLOTS,
    "guardian_statue": DWARF_SLOTS,
    "elements_statue": DWARF_SLOTS,
    "arcane_statue": DWARF_SLOTS,
    "warrior_statue": DWARF_SLOTS,
    "healer_statue": DWARF_SLOTS,
}

STATUES = {
    # name: (slot count, placeholder texture colour, recipe center item, generate placeholder block model)
    "artisan_statue":  (4, (216, 216, 216), "minecraft:iron_pickaxe", False),
    "fortune_statue":  (2, (249, 212, 79),  "minecraft:rabbit_foot",  False),
    "wind_statue":     (7, (235, 229, 222), "minecraft:feather",      False),
    "guardian_statue": (6, (132, 134, 133), "minecraft:shield",       False),
    "elements_statue": (4, (197, 92, 31),   "minecraft:blaze_powder", False),
    "arcane_statue":   (5, (169, 125, 169), "minecraft:ender_eye",    False),
    "warrior_statue":  (2, (83, 10, 13),    "minecraft:bow",          False),
    "healer_statue":   (6, (72, 216, 112),  "minecraft:ghast_tear",   False),
}

FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def write(path, data):
    path = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def write_placeholder_texture(path, rgb):
    """16x16 PNG in one colour with a darker border, only if no texture exists yet."""
    path = os.path.join(ROOT, path)
    if os.path.exists(path):
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    dark = tuple(c * 3 // 4 for c in rgb)
    rows = b""
    for y in range(16):
        rows += b"\0" + b"".join(bytes(dark if x in (0, 15) or y in (0, 15) else rgb) for x in range(16))

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 2, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(rows)) + chunk(b"IEND", b""))


def cube(frm, to, tex):
    faces = {d: {"texture": tex} for d in ("north", "east", "south", "west", "up", "down")}
    return {"from": frm, "to": to, "faces": faces}


def candle_elements(x, y, z):
    return [
        cube([x - 1, y, z - 1], [x + 1, y + CANDLE_HEIGHT, z + 1], "#wax"),
        cube([x - 0.25, y + CANDLE_HEIGHT, z - 0.25], [x + 0.25, y + CANDLE_HEIGHT + 1, z + 0.25], "#wick"),
    ]


CANDLE_TEXTURES = {"wax": "minecraft:blocks/wool_colored_white", "wick": "minecraft:blocks/wool_colored_black",
                   "particle": "minecraft:blocks/wool_colored_white"}

# Candle item: a single candle standing in the middle
write("models/block/candle.json", {"parent": "block/block", "textures": CANDLE_TEXTURES,
                                   "elements": candle_elements(8, 0, 8)})
write("models/item/candle.json", {
    "parent": "chunkcomfort:block/candle",
    "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 3, 0], "scale": [1.4, 1.4, 1.4]},
        "ground": {"translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0], "scale": [0.6, 0.6, 0.6]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.5, 0.5, 0.5]},
    },
})
write("recipes/candle.json", {
    "type": "minecraft:crafting_shaped",
    "pattern": ["S", "W"],
    "key": {"S": {"item": "minecraft:string"}, "W": {"item": "minecraft:slime_ball"}},
    "result": {"item": "chunkcomfort:candle", "count": 2},
})

for name, (slot_count, colour, recipe_item, gen_model) in STATUES.items():
    texture = f"chunkcomfort:blocks/statue/{name}"
    write_placeholder_texture(f"textures/blocks/statue/{name}.png", colour)
    if gen_model:
        # Placeholder: stone brick pedestal with a pillar in the statue's own texture
        write(f"models/block/statue/{name}.json", {
            "parent": "block/block",
            "textures": {"base": "minecraft:blocks/stonebrick", "pillar": texture, "particle": texture},
            "elements": [
                cube([2, 0, 2], [14, 4, 14], "#base"),
                cube([5, 4, 5], [11, 14, 11], "#pillar"),
                cube([4, 14, 4], [12, 16, 12], "#pillar"),
            ],
        })

    # Candles are drawn in code (StatueBakedModel), so the blockstate only needs facing
    variants = {}
    for facing, y in FACING_Y.items():
        variant = {"model": f"chunkcomfort:statue/{name}"}
        if y:
            variant["y"] = y
        variants[f"facing={facing}"] = variant
    write(f"blockstates/{name}.json", {"variants": variants})

    slots = LAYOUTS.get(name, PLACEHOLDER_SLOTS[:slot_count])
    # One slot per line so pack makers can edit candle positions by hand
    path = os.path.join(ROOT, f"statues/{name}.json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        rows = ",\n".join("    [" + ", ".join(str(v) for v in slot) + "]" for slot in slots)
        f.write('{\n  "slots": [\n' + rows + '\n  ]\n}\n')

    write(f"models/item/{name}.json", {"parent": f"chunkcomfort:block/statue/{name}"})

    write(f"recipes/{name}.json", {
        "type": "minecraft:crafting_shaped",
        "pattern": ["SSS", "SXS", "SSS"],
        "key": {"S": {"item": "minecraft:stonebrick", "data": 0}, "X": {"item": recipe_item}},
        "result": {"item": f"chunkcomfort:{name}"},
    })

print("Generated assets for", len(STATUES), "statues and the candle")

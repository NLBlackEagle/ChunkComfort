"""
Imports a Blockbench statue (Java Block/Item format) into the mod. Run from the project root:
    python tools/import_statue.py <file.bbmodel> <statue name>
    python tools/import_statue.py F:/BlockBench/Statues/statue_dwarf.bbmodel artisan_statue

Writes models/block/statue/<statue>.json and textures/blocks/statue/<statue>.png (the texture
saved in the .bbmodel, or the .png next to it). Cubes named slot_N are candle positions (bottom
centre = where the candle stands, cube height = candle wax height): they are left out of the
model and printed so you can paste them into LAYOUTS in gen_statue_assets.py.
"""
import base64, json, os, re, sys

ROOT = "src/main/resources/assets/chunkcomfort"
VALID_ANGLES = (-45, -22.5, 0, 22.5, 45)


def num(v):
    # Keep the JSON readable: 4.0 -> 4, 5.62151 stays as is
    v = round(v, 5)
    return int(v) if v == int(v) else v


def texture_bytes(model, bbmodel_path):
    source = model["textures"][0].get("source", "") if model.get("textures") else ""
    if source.startswith("data:image/png;base64,"):
        return base64.b64decode(source.split(",", 1)[1])
    png = os.path.splitext(bbmodel_path)[0] + ".png"
    with open(png, "rb") as f:
        return f.read()


def main(bbmodel_path, name):
    with open(bbmodel_path, encoding="utf-8") as f:
        model = json.load(f)
    if model["meta"].get("box_uv"):
        sys.exit("Model uses box UV, switch it to per-face UV in Blockbench first")
    if len(model.get("textures", [])) != 1:
        sys.exit("Statues use exactly one texture, this model has %d" % len(model.get("textures", [])))

    tex_w = model["resolution"]["width"] / 16.0
    tex_h = model["resolution"]["height"] / 16.0
    texture = f"chunkcomfort:blocks/statue/{name}"

    elements, slots, warnings = [], [], []
    for e in model["elements"]:
        if e.get("type", "cube") != "cube" or not e.get("export", True):
            continue
        frm = [v - e.get("inflate", 0) for v in e["from"]]
        to = [v + e.get("inflate", 0) for v in e["to"]]

        match = re.fullmatch(r"slot_(\d+)", e.get("name", ""))
        if match:
            # Candle stands on the bottom centre of the slot cube, as tall as the cube
            slots.append((int(match.group(1)), ((frm[0] + to[0]) / 2, frm[1], (frm[2] + to[2]) / 2, to[1] - frm[1])))
            continue

        out = {"from": [num(v) for v in frm], "to": [num(v) for v in to]}
        if not e.get("shade", True):
            out["shade"] = False

        rotation = e.get("rotation") or [0, 0, 0]
        axes = [i for i in range(3) if rotation[i]]
        if len(axes) > 1:
            sys.exit(f"Cube '{e['name']}' is rotated on more than one axis, Minecraft 1.12 only allows one")
        if axes:
            angle = rotation[axes[0]]
            if angle not in VALID_ANGLES:
                sys.exit(f"Cube '{e['name']}' is rotated {angle}, only -45/-22.5/22.5/45 work in 1.12")
            out["rotation"] = {"origin": [num(v) for v in e["origin"]], "axis": "xyz"[axes[0]], "angle": num(angle)}
            if e.get("rescale"):
                out["rotation"]["rescale"] = True

        faces = {}
        for side, face in e["faces"].items():
            if face.get("texture") is None:
                continue
            u1, v1, u2, v2 = face["uv"]
            faces[side] = {"uv": [num(u1 / tex_w), num(v1 / tex_h), num(u2 / tex_w), num(v2 / tex_h)],
                           "texture": "#statue"}
            if face.get("rotation"):
                faces[side]["rotation"] = face["rotation"]
        out["faces"] = faces
        elements.append(out)

        if min(frm) < 0 or max(to[0], to[2]) > 16 or to[1] > 16:
            warnings.append(e.get("name", "cube"))

    data = {"parent": "block/block", "textures": {"statue": texture, "particle": texture}, "elements": elements}
    if model.get("display"):
        data["display"] = model["display"]

    model_path = os.path.join(ROOT, f"models/block/statue/{name}.json")
    with open(model_path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")

    tex_path = os.path.join(ROOT, f"textures/blocks/statue/{name}.png")
    os.makedirs(os.path.dirname(tex_path), exist_ok=True)
    with open(tex_path, "wb") as f:
        f.write(texture_bytes(model, bbmodel_path))

    print(f"Wrote {model_path} ({len(elements)} cubes) and {tex_path}")
    if warnings:
        print(f"Note: {len(warnings)} cube(s) stick out of the block (0-16), clicks there won't hit the statue")
    if slots:
        numbers = [n for n, _ in sorted(slots)]
        if numbers != list(range(1, len(slots) + 1)):
            print(f"Warning: slot numbers are {numbers}, expected slot_1 to slot_{len(slots)} without gaps")
        rows = ", ".join("(" + ", ".join(str(num(v)) for v in slot) + ")" for _, slot in sorted(slots))
        print(f'Candle slots for LAYOUTS ({len(slots)} slots, x/y/z/height):\n    "{name}": [{rows}],')
    else:
        print("No slot_N cubes found, the statue keeps its LAYOUTS / placeholder candle positions")
    print(f'Remember to set "{name}" to False in STATUES so gen_statue_assets.py keeps this model')


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])

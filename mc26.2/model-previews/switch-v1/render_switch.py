from __future__ import annotations

from pathlib import Path
from zipfile import ZipFile
from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
SIGNAL_TEXTURE = ROOT.parent / "rail-v1/assets/fabric_train_simulator/textures/block/signal_rail.png"
BLOCK_TEXTURES = ROOT / "assets/fabric_train_simulator/textures/block"
ITEM_TEXTURES = ROOT / "assets/fabric_train_simulator/textures/item"
OUTPUT = ROOT / "renders"
GAME_JAR = Path(r"G:\game\mcfabric\.minecraft\versions\26.2\26.2.jar")

INACTIVE = (14, 97, 116, 255)
ACTIVE_EDGE = (20, 160, 174, 255)
ACTIVE = (79, 232, 220, 255)
ACTIVE_LIGHT = (179, 255, 247, 255)


def vanilla_texture(name: str) -> Image.Image:
    with ZipFile(GAME_JAR) as jar:
        with jar.open(f"assets/minecraft/textures/block/{name}.png") as source:
            return Image.open(source).convert("RGBA")


def grass() -> Image.Image:
    source = vanilla_texture("grass_block_top")
    result = source.copy()
    for y in range(16):
        for x in range(16):
            r, g, b, a = source.getpixel((x, y))
            result.putpixel((x, y), (r*82//148, g*137//148, b*60//148, a))
    return result


def base_grid() -> Image.Image:
    straight = Image.open(SIGNAL_TEXTURE).convert("RGBA")
    horizontal = straight.rotate(90, resample=Image.Resampling.NEAREST)
    result = straight.copy()
    result.alpha_composite(horizontal)
    return result


def marker(draw: ImageDraw.ImageDraw, direction: str, active: bool) -> None:
    boxes = {
        "front": (7, 0, 8, 3),
        "back": (7, 12, 8, 15),
        "left": (0, 7, 3, 8),
        "right": (12, 7, 15, 8),
    }
    box = boxes[direction]
    if not active:
        draw.rectangle(box, fill=INACTIVE)
        return
    draw.rectangle(box, fill=ACTIVE_EDGE)
    x0, y0, x1, y1 = box
    if x1-x0 > y1-y0:
        draw.rectangle((x0, y0, x1, y0), fill=ACTIVE_LIGHT)
        draw.rectangle((x0, y0+1, x1, y1), fill=ACTIVE)
    else:
        draw.rectangle((x0, y0, x0, y1), fill=ACTIVE_LIGHT)
        draw.rectangle((x0+1, y0, x1, y1), fill=ACTIVE)


def state_texture(selected: str | None, all_lit: bool = False) -> Image.Image:
    result = base_grid()
    draw = ImageDraw.Draw(result)
    for direction in ("front", "back", "left", "right"):
        is_active = all_lit or direction == "back" or direction == selected
        marker(draw, direction, is_active)
    return result


def pixel_scene(texture: Image.Image, inventory: bool) -> Image.Image:
    scale = 20
    if inventory:
        canvas = Image.new("RGBA", (400, 400), (0,0,0,0))
        draw = ImageDraw.Draw(canvas)
        draw.rectangle((15,15,385,385), fill=(139,139,139,255), outline=(24,24,24,255), width=10)
        draw.line((27,27,373,27), fill=(235,235,235,255), width=7)
        draw.line((27,27,27,373), fill=(235,235,235,255), width=7)
        draw.line((27,373,373,373), fill=(70,70,70,255), width=7)
        draw.line((373,27,373,373), fill=(70,70,70,255), width=7)
        origin_x = origin_y = 40
    else:
        canvas = Image.new("RGB", (320,320))
        draw = ImageDraw.Draw(canvas)
        ground = grass()
        for y in range(16):
            for x in range(16):
                draw.rectangle((x*scale,y*scale,(x+1)*scale-1,(y+1)*scale-1), fill=ground.getpixel((x,y))[:3])
        origin_x = origin_y = 0

    for y in range(16):
        for x in range(16):
            colour = texture.getpixel((x,y))
            if colour[3] == 0:
                continue
            x0, y0 = origin_x+x*scale, origin_y+y*scale
            draw.rectangle((x0,y0,x0+scale-1,y0+scale-1), fill=colour[:3])
    return canvas


def main() -> None:
    BLOCK_TEXTURES.mkdir(parents=True, exist_ok=True)
    ITEM_TEXTURES.mkdir(parents=True, exist_ok=True)
    OUTPUT.mkdir(parents=True, exist_ok=True)

    states = {
        "all": state_texture(None, all_lit=True),
        "front": state_texture("front"),
        "left": state_texture("left"),
        "right": state_texture("right"),
    }
    base_grid().save(BLOCK_TEXTURES / "switch_rail_grid.png")
    states["all"].save(ITEM_TEXTURES / "switch_rail_all_lit.png")
    for name in ("front", "left", "right"):
        states[name].save(BLOCK_TEXTURES / f"switch_rail_{name}.png")

    pixel_scene(states["all"], inventory=True).save(OUTPUT / "01_inventory_all_directions_lit.png")
    pixel_scene(states["front"], inventory=False).save(OUTPUT / "02_placed_front.png")
    pixel_scene(states["left"], inventory=False).save(OUTPUT / "03_placed_left.png")
    pixel_scene(states["right"], inventory=False).save(OUTPUT / "04_placed_right.png")


if __name__ == "__main__":
    main()

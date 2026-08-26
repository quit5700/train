from __future__ import annotations

from pathlib import Path
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parent
REFERENCE = ROOT / "vanilla-reference"
TEXTURES = ROOT / "assets/fabric_train_simulator/textures/block"
OUTPUT = ROOT / "renders"

DARK_CYAN = (20, 103, 125, 255)
MID_CYAN = (37, 183, 207, 255)
DIAMOND = (92, 238, 225, 255)
DIAMOND_LIGHT = (88, 232, 219, 255)


def font(size: int):
    for candidate in (
        Path("C:/Windows/Fonts/msyh.ttc"),
        Path("C:/Windows/Fonts/simhei.ttf"),
    ):
        if candidate.exists():
            return ImageFont.truetype(str(candidate), size)
    return ImageFont.load_default()


def diamond_recolour(image: Image.Image) -> Image.Image:
    result = image.convert("RGBA")
    pixels = result.load()
    for y in range(result.height):
        for x in range(result.width):
            r, g, b, a = pixels[x, y]
            if not a:
                continue
            # 原版铁轨的金属部分是近灰色；木枕保持原色。
            if max(r, g, b) - min(r, g, b) <= 32 and r >= 45:
                if r >= 165:
                    pixels[x, y] = DIAMOND_LIGHT
                elif r >= 100:
                    pixels[x, y] = DIAMOND
                else:
                    pixels[x, y] = MID_CYAN
    return result


def make_textures() -> dict[str, Image.Image]:
    TEXTURES.mkdir(parents=True, exist_ok=True)
    straight = diamond_recolour(Image.open(REFERENCE / "rail.png"))
    corner = diamond_recolour(Image.open(REFERENCE / "rail_corner.png"))

    cross = Image.new("RGBA", (16, 16))
    cross.alpha_composite(straight)
    cross.alpha_composite(straight.rotate(90, resample=Image.Resampling.NEAREST))

    # 四个方向标记均为钻石色；南侧作为入口常亮，东侧作为当前选择出口高亮。
    draw = ImageDraw.Draw(cross)
    dim = DARK_CYAN
    bright = DIAMOND_LIGHT
    draw.rectangle((7, 0, 8, 3), fill=dim)
    draw.rectangle((7, 12, 8, 15), fill=bright)
    draw.rectangle((0, 7, 3, 8), fill=dim)
    draw.rectangle((12, 7, 15, 8), fill=bright)

    straight.save(TEXTURES / "signal_rail.png")
    corner.save(TEXTURES / "signal_rail_corner.png")
    cross.save(TEXTURES / "switch_rail.png")
    return {"straight": straight, "corner": corner, "switch": cross}


def grass_texture() -> Image.Image:
    image = Image.new("RGBA", (16, 16), (77, 125, 45, 255))
    draw = ImageDraw.Draw(image)
    flecks = [(1, 2), (5, 1), (10, 3), (14, 1), (3, 7), (8, 6), (12, 9), (1, 13), (6, 14), (14, 13)]
    for index, (x, y) in enumerate(flecks):
        draw.point((x, y), fill=(91, 144, 54, 255) if index % 2 else (61, 105, 37, 255))
    return image


def project(texture: Image.Image, size: tuple[int, int], quad: tuple[int, ...]) -> Image.Image:
    large = texture.resize((256, 256), Image.Resampling.NEAREST)
    return large.transform(size, Image.Transform.QUAD, quad, Image.Resampling.NEAREST)


def preview_flat(texture: Image.Image, title: str, subtitle: str) -> Image.Image:
    canvas = Image.new("RGBA", (640, 430), (28, 32, 37, 255))
    grass = grass_texture().resize((320, 320), Image.Resampling.NEAREST).rotate(45, expand=True, resample=Image.Resampling.NEAREST)
    grass = grass.resize((450, 225), Image.Resampling.NEAREST)
    canvas.alpha_composite(grass, (95, 125))
    rail = texture.resize((320, 320), Image.Resampling.NEAREST).rotate(45, expand=True, resample=Image.Resampling.NEAREST)
    rail = rail.resize((450, 225), Image.Resampling.NEAREST)
    canvas.alpha_composite(rail, (95, 120))
    draw = ImageDraw.Draw(canvas)
    draw.text((30, 24), title, font=font(30), fill=(242, 244, 247))
    draw.text((31, 67), subtitle, font=font(18), fill=(151, 220, 230))
    return canvas.convert("RGB")


def textured_quad(canvas: Image.Image, texture: Image.Image,
                  corners: tuple[tuple[float, float], tuple[float, float],
                                 tuple[float, float], tuple[float, float]]) -> None:
    """以 16×16 像素单元映射纹理，避免预览产生游戏模型中不存在的平滑或破洞。"""
    draw = ImageDraw.Draw(canvas)
    source = texture.convert("RGBA")

    def point(u: float, v: float) -> tuple[float, float]:
        p00, p10, p11, p01 = corners
        x = ((1 - u) * (1 - v) * p00[0] + u * (1 - v) * p10[0]
             + u * v * p11[0] + (1 - u) * v * p01[0])
        y = ((1 - u) * (1 - v) * p00[1] + u * (1 - v) * p10[1]
             + u * v * p11[1] + (1 - u) * v * p01[1])
        return x, y

    for y in range(source.height):
        for x in range(source.width):
            colour = source.getpixel((x, y))
            if colour[3] == 0:
                continue
            u0, u1 = x / source.width, (x + 1) / source.width
            v0, v1 = y / source.height, (y + 1) / source.height
            draw.polygon((point(u0, v0), point(u1, v0), point(u1, v1), point(u0, v1)), fill=colour)


def preview_slope(texture: Image.Image) -> Image.Image:
    canvas = Image.new("RGBA", (640, 430), (28, 32, 37, 255))
    draw = ImageDraw.Draw(canvas)
    # 一个方块长、一个方块高的原版 45° 铁轨平面。
    top = [(120, 305), (335, 360), (525, 190), (310, 135)]
    draw.polygon(top, fill=(72, 119, 45, 255))
    draw.polygon([(120, 305), (335, 360), (335, 390), (120, 335)], fill=(91, 62, 38, 255))
    draw.polygon([(335, 360), (525, 190), (525, 220), (335, 390)], fill=(105, 72, 43, 255))
    textured_quad(canvas, texture, (top[0], top[1], top[2], top[3]))
    draw = ImageDraw.Draw(canvas)
    draw.text((30, 24), "信号铁轨｜上坡", font=font(30), fill=(242, 244, 247))
    draw.text((31, 67), "原版 45° 坡道模型＋同一张钻石色直线贴图", font=font(18), fill=(151, 220, 230))
    return canvas.convert("RGB")


def main() -> None:
    textures = make_textures()
    OUTPUT.mkdir(parents=True, exist_ok=True)
    images = [
        ("01_signal_rail_straight.png", preview_flat(textures["straight"], "信号铁轨｜直线", "实际 16×16 贴图与平直铁轨模型")),
        ("02_signal_rail_curve.png", preview_flat(textures["corner"], "信号铁轨｜转弯", "独立弯道贴图，不再继承普通铁轨外观")),
        ("03_signal_rail_slope.png", preview_slope(textures["straight"])),
        ("04_switch_rail.png", preview_flat(textures["switch"], "道岔｜入口南＋选择东", "南侧入口常亮；东侧为当前选择出口；其余两侧暗色")),
    ]
    for name, image in images:
        image.save(OUTPUT / name, quality=95)

    sheet = Image.new("RGB", (1280, 860), (18, 21, 25))
    for index, (_, image) in enumerate(images):
        sheet.paste(image, ((index % 2) * 640, (index // 2) * 430))
    sheet.save(OUTPUT / "00_all_models.png", quality=95)


if __name__ == "__main__":
    main()

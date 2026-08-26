from __future__ import annotations

from pathlib import Path
from zipfile import ZipFile
from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
GAME_JAR = Path(r"G:\game\mcfabric\.minecraft\versions\26.2\26.2.jar")
OUTPUT = ROOT / "renders"


def vanilla_texture(name: str) -> Image.Image:
    with ZipFile(GAME_JAR) as jar:
        with jar.open(f"assets/minecraft/textures/block/{name}.png") as source:
            return Image.open(source).convert("RGBA")


def current_signal_texture() -> Image.Image:
    rail = vanilla_texture("rail")
    diamond = vanilla_texture("diamond_block")
    result = rail.copy()
    # 当前 JSON 的两条覆盖层位于 X=2.5..3.5 与 X=12.5..13.5，
    # 在 16×16 像素预览中分别对应第 3、13 列。
    for target_x, source_x in ((3, 3), (13, 13)):
        for y in range(16):
            result.putpixel((target_x, y), diamond.getpixel((source_x, y)))
    return result


def textured_quad(canvas: Image.Image, texture: Image.Image,
                  corners: tuple[tuple[float, float], tuple[float, float],
                                 tuple[float, float], tuple[float, float]]) -> None:
    draw = ImageDraw.Draw(canvas)

    def point(u: float, v: float) -> tuple[float, float]:
        p00, p10, p11, p01 = corners
        return (
            (1-u)*(1-v)*p00[0] + u*(1-v)*p10[0] + u*v*p11[0] + (1-u)*v*p01[0],
            (1-u)*(1-v)*p00[1] + u*(1-v)*p10[1] + u*v*p11[1] + (1-u)*v*p01[1],
        )

    for y in range(16):
        for x in range(16):
            colour = texture.getpixel((x, y))
            if colour[3] == 0:
                continue
            u0, u1 = x/16, (x+1)/16
            v0, v1 = y/16, (y+1)/16
            draw.polygon((point(u0,v0), point(u1,v0), point(u1,v1), point(u0,v1)), fill=colour)


def grass() -> Image.Image:
    # 26.2 原版草方块顶面是灰度纹理，游戏会按生物群系施加绿色着色。
    source = vanilla_texture("grass_block_top")
    tinted = source.copy()
    for y in range(source.height):
        for x in range(source.width):
            r, g, b, a = source.getpixel((x, y))
            tinted.putpixel((x, y), (r * 82 // 148, g * 137 // 148, b * 60 // 148, a))
    return tinted


def inventory_preview(texture: Image.Image) -> Image.Image:
    canvas = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)
    draw.rectangle((26, 26, 486, 486), fill=(139,139,139,255), outline=(25,25,25,255), width=12)
    draw.line((39,39,473,39), fill=(235,235,235,255), width=8)
    draw.line((39,39,39,473), fill=(235,235,235,255), width=8)
    draw.line((39,473,473,473), fill=(72,72,72,255), width=8)
    draw.line((473,39,473,473), fill=(72,72,72,255), width=8)
    # 按用户指定的物品栏样式：正俯视方形铁轨图标，不带草方块。
    icon = texture.resize((352, 352), Image.Resampling.NEAREST)
    canvas.alpha_composite(icon, (80, 80))
    return canvas


def placed_preview(texture: Image.Image) -> Image.Image:
    # 与用户截图一致：一个草方块顶面的正俯视放置效果。
    scale = 20
    canvas = Image.new("RGB", (16 * scale, 16 * scale))
    draw = ImageDraw.Draw(canvas)
    grass_texture = grass()
    for y in range(16):
        for x in range(16):
            colour = grass_texture.getpixel((x, y))[:3]
            draw.rectangle((x * scale, y * scale, (x + 1) * scale - 1, (y + 1) * scale - 1), fill=colour)
    for y in range(16):
        for x in range(16):
            colour = texture.getpixel((x, y))
            if colour[3] == 0:
                continue
            draw.rectangle((x * scale, y * scale, (x + 1) * scale - 1, (y + 1) * scale - 1), fill=colour[:3])
    return canvas


def topdown_on_grass(texture: Image.Image) -> Image.Image:
    scale = 20
    canvas = Image.new("RGB", (16 * scale, 16 * scale))
    draw = ImageDraw.Draw(canvas)
    ground = grass()
    for y in range(16):
        for x in range(16):
            colour = ground.getpixel((x, y))[:3]
            draw.rectangle((x * scale, y * scale, (x + 1) * scale - 1, (y + 1) * scale - 1), fill=colour)
    for y in range(16):
        for x in range(16):
            colour = texture.getpixel((x, y))
            if colour[3]:
                draw.rectangle((x * scale, y * scale, (x + 1) * scale - 1, (y + 1) * scale - 1), fill=colour[:3])
    return canvas


def slope_on_grass(texture: Image.Image, downhill: bool = False) -> Image.Image:
    canvas = Image.new("RGBA", (640, 480), (105, 157, 210, 255))
    draw = ImageDraw.Draw(canvas)

    def iso(x: float, y: float, z: float) -> tuple[float, float]:
        return 150 + x * 180 - z * 45, 380 + z * 55 - y * 100

    def block(x: float, y: float, z: float) -> None:
        top = (iso(x, y+1, z), iso(x+1, y+1, z), iso(x+1, y+1, z+1), iso(x, y+1, z+1))
        side_x = (iso(x+1,y+1,z), iso(x+1,y,z), iso(x+1,y,z+1), iso(x+1,y+1,z+1))
        side_z = (iso(x,y+1,z+1), iso(x+1,y+1,z+1), iso(x+1,y,z+1), iso(x,y,z+1))
        draw.polygon(side_x, fill=(94, 64, 39, 255))
        draw.polygon(side_z, fill=(79, 54, 34, 255))
        textured_quad(canvas, grass(), top)

    # 真实方块阶梯：低处草方块 + 相邻高一格的草方块，不制作不存在的斜面草方块。
    block(0, 0, 0)
    block(1, 1, 0)
    rail_plane = (iso(0,1.04,0), iso(1,2.04,0), iso(1,2.04,1), iso(0,1.04,1))
    p00, p10, p11, p01 = rail_plane

    def mix(a: tuple[float,float], b: tuple[float,float], amount: float) -> tuple[float,float]:
        return a[0] + (b[0]-a[0])*amount, a[1] + (b[1]-a[1])*amount

    # 按原版直线铁轨贴图结构绘制：横向木枕、两条平行金属轨。
    for amount in (0.08, 0.27, 0.47, 0.67, 0.87):
        left = mix(p00, p10, amount)
        right = mix(p01, p11, amount)
        draw.line((left, right), fill=(84, 61, 34, 255), width=12)
        draw.line((left, right), fill=(151, 116, 65, 255), width=7)
    for side in (0.18, 0.82):
        bottom = mix(p00, p01, side)
        top_end = mix(p10, p11, side)
        draw.line((bottom, top_end), fill=(71, 72, 71, 255), width=12)
        draw.line((bottom, top_end), fill=(177, 178, 177, 255), width=7)
    if downhill:
        canvas = canvas.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    return canvas.convert("RGB")


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    texture = current_signal_texture()
    texture.save(OUTPUT / "current_signal_rail_texture.png")
    inventory_preview(texture).save(OUTPUT / "01_inventory_icon.png")
    placed_preview(texture).save(OUTPUT / "02_placed_model.png")
    # 当前弯道和坡道模型仍继承原版铁轨贴图；这里忠实展示现状。
    corner = vanilla_texture("rail_corner")
    raised = vanilla_texture("rail")
    topdown_on_grass(corner).save(OUTPUT / "03_current_curve_on_grass.png")
    slope_on_grass(raised, downhill=False).save(OUTPUT / "04_current_uphill_on_grass.png")
    slope_on_grass(raised, downhill=True).save(OUTPUT / "05_current_downhill_on_grass.png")


if __name__ == "__main__":
    main()

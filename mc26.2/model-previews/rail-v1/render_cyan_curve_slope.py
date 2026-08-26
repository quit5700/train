from __future__ import annotations

from pathlib import Path
from zipfile import ZipFile
from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
OUTPUT = ROOT / "renders-v2"
TEXTURES = ROOT / "assets/fabric_train_simulator/textures/block"
GAME_JAR = Path(r"G:\game\mcfabric\.minecraft\versions\26.2\26.2.jar")


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
            result.putpixel((x, y), (r * 82 // 148, g * 137 // 148, b * 60 // 148, a))
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


def curve_preview() -> Image.Image:
    scale = 20
    canvas = Image.new("RGB", (320, 320))
    draw = ImageDraw.Draw(canvas)
    ground = grass()
    curve = Image.open(TEXTURES / "signal_rail_corner.png").convert("RGBA")
    for y in range(16):
        for x in range(16):
            base = ground.getpixel((x, y))[:3]
            draw.rectangle((x*scale, y*scale, (x+1)*scale-1, (y+1)*scale-1), fill=base)
            rail = curve.getpixel((x, y))
            if rail[3]:
                draw.rectangle((x*scale, y*scale, (x+1)*scale-1, (y+1)*scale-1), fill=rail[:3])
    return canvas


def slope_preview(downhill: bool) -> Image.Image:
    canvas = Image.new("RGBA", (640, 480), (105, 157, 210, 255))
    draw = ImageDraw.Draw(canvas)

    # 新视角：从坡道侧前方上方观察，坡面不会与视线重合。
    def iso(x: float, y: float, z: float) -> tuple[float, float]:
        return 150 + x*160, 365 + x*50 + z*80 - y*90

    def block(x: float, y: float, z: float) -> None:
        top = (iso(x,y+1,z), iso(x+1,y+1,z), iso(x+1,y+1,z+1), iso(x,y+1,z+1))
        side_x = (iso(x+1,y+1,z), iso(x+1,y,z), iso(x+1,y,z+1), iso(x+1,y+1,z+1))
        side_z = (iso(x,y+1,z+1), iso(x+1,y+1,z+1), iso(x+1,y,z+1), iso(x,y,z+1))
        draw.polygon(side_x, fill=(94,64,39,255))
        draw.polygon(side_z, fill=(77,52,33,255))
        textured_quad(canvas, grass(), top)

    block(0,0,0)
    block(1,1,0)
    p00, p10, p11, p01 = (iso(0,1.04,0), iso(1,2.04,0), iso(1,2.04,1), iso(0,1.04,1))

    def mix(a: tuple[float,float], b: tuple[float,float], amount: float) -> tuple[float,float]:
        return a[0]+(b[0]-a[0])*amount, a[1]+(b[1]-a[1])*amount

    # 枕木保持原版棕色。
    for amount in (0.08,0.27,0.47,0.67,0.87):
        left, right = mix(p00,p10,amount), mix(p01,p11,amount)
        draw.line((left,right), fill=(82,59,33,255), width=12)
        draw.line((left,right), fill=(151,116,65,255), width=7)
    # 两条铁轨改为与直线信号铁轨一致的青色深浅层。
    for side in (0.18,0.82):
        low, high = mix(p00,p01,side), mix(p10,p11,side)
        draw.line((low,high), fill=(18,128,142,255), width=13)
        draw.line((low,high), fill=(78,229,218,255), width=8)
        draw.line((low,high), fill=(180,255,247,255), width=3)

    if downhill:
        canvas = canvas.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    return canvas.convert("RGB")


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    curve_preview().save(OUTPUT / "01_cyan_curve_on_grass.png")
    slope_preview(False).save(OUTPUT / "02_cyan_uphill_new_angle.png")
    slope_preview(True).save(OUTPUT / "03_cyan_downhill_new_angle.png")


if __name__ == "__main__":
    main()

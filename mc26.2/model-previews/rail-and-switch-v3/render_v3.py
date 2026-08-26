from __future__ import annotations

from pathlib import Path
from zipfile import ZipFile
from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
TEXTURES = ROOT / "assets/fabric_train_simulator/textures/block"
ITEM_TEXTURES = ROOT / "assets/fabric_train_simulator/textures/item"
OUTPUT = ROOT / "renders"
GAME_JAR = Path(r"G:\game\mcfabric\.minecraft\versions\26.2\26.2.jar")

DARK = (43, 77, 79, 255)
CYAN_DARK = (15, 132, 148, 255)
CYAN = (69, 222, 213, 255)
HIGHLIGHT = (202, 244, 238, 255)
INACTIVE = (12, 88, 105, 255)


def vanilla(name: str) -> Image.Image:
    with ZipFile(GAME_JAR) as jar:
        with jar.open(f"assets/minecraft/textures/block/{name}.png") as source:
            return Image.open(source).convert("RGBA")


def grass() -> Image.Image:
    source = vanilla("grass_block_top")
    result = source.copy()
    for y in range(16):
        for x in range(16):
            r, g, b, a = source.getpixel((x, y))
            result.putpixel((x, y), (r*82//148, g*137//148, b*60//148, a))
    return result


def is_metal(colour: tuple[int,int,int,int]) -> bool:
    r, g, b, a = colour
    return a > 0 and max(r,g,b)-min(r,g,b) <= 34 and r >= 42


def realistic_signal(source: Image.Image) -> Image.Image:
    result = source.convert("RGBA").copy()
    for y in range(16):
        for x in range(16):
            colour = result.getpixel((x,y))
            if not is_metal(colour):
                continue
            brightness = sum(colour[:3]) // 3
            if brightness >= 165:
                replacement = HIGHLIGHT
            elif brightness >= 105:
                replacement = CYAN
            elif brightness >= 70:
                replacement = CYAN_DARK
            else:
                replacement = DARK
            result.putpixel((x,y), replacement)
    return result


def confirmed_straight_signal() -> Image.Image:
    """复用用户已确认的直线信号铁轨结构：原版金属轨旁覆盖钻石色纹理。"""
    rail = vanilla("rail")
    diamond = vanilla("diamond_block")
    result = rail.copy()
    for target_x, source_x in ((3,3),(13,13)):
        for y in range(16):
            result.putpixel((target_x,y), diamond.getpixel((source_x,y)))
    return result


def sleepers_only(source: Image.Image) -> Image.Image:
    result = source.convert("RGBA").copy()
    for y in range(16):
        for x in range(16):
            if is_metal(result.getpixel((x,y))):
                result.putpixel((x,y), (0,0,0,0))
    return result


def switch_base(straight: Image.Image) -> Image.Image:
    result = straight.copy()
    result.alpha_composite(straight.rotate(90, resample=Image.Resampling.NEAREST))

    # 横向合成后重新覆盖两条纵向金属轨，确保纵向也完整保留
    # “灰色边缘＋浅色高光＋青色主体”，不会被横向枕木压掉。
    diamond_columns = {3,13}
    for y in range(16):
        for x in range(16):
            colour = straight.getpixel((x,y))
            if colour[3] and (x in diamond_columns or is_metal(colour)):
                result.putpixel((x,y), colour)

    # 只删除红框轨道以外的四角孤立碎片；四条完整信号铁轨纹理全部保留。
    rail_columns = {2,3,4,12,13,14}
    corner_regions = ((0,0,4,4),(12,0,16,4),(0,12,4,16),(12,12,16,16))
    for x0,y0,x1,y1 in corner_regions:
        for y in range(y0,y1):
            for x in range(x0,x1):
                if x not in rail_columns and y not in rail_columns:
                    result.putpixel((x,y), (0,0,0,0))
    return result


def marker(draw: ImageDraw.ImageDraw, direction: str, active: bool) -> None:
    boxes = {"front":(7,0,8,3), "back":(7,12,8,15), "left":(0,7,3,8), "right":(12,7,15,8)}
    box = boxes[direction]
    if not active:
        draw.rectangle(box, fill=INACTIVE)
        return
    x0,y0,x1,y1 = box
    draw.rectangle(box, fill=CYAN_DARK)
    if x1-x0 > y1-y0:
        draw.rectangle((x0,y0,x1,y0), fill=HIGHLIGHT)
        draw.rectangle((x0,y0+1,x1,y1), fill=CYAN)
    else:
        draw.rectangle((x0,y0,x0,y1), fill=HIGHLIGHT)
        draw.rectangle((x0+1,y0,x1,y1), fill=CYAN)


def switch_state(base: Image.Image, selected: str | None, all_lit: bool=False) -> Image.Image:
    result = base.copy()
    draw = ImageDraw.Draw(result)
    for direction in ("front","back","left","right"):
        marker(draw, direction, all_lit or direction == "back" or direction == selected)
    return result


def topdown(texture: Image.Image, inventory: bool=False) -> Image.Image:
    scale = 20
    if inventory:
        canvas = Image.new("RGBA", (400,400), (0,0,0,0))
        draw = ImageDraw.Draw(canvas)
        draw.rectangle((15,15,385,385), fill=(139,139,139,255), outline=(24,24,24,255), width=10)
        draw.line((27,27,373,27), fill=(235,235,235,255), width=7)
        draw.line((27,27,27,373), fill=(235,235,235,255), width=7)
        draw.line((27,373,373,373), fill=(70,70,70,255), width=7)
        draw.line((373,27,373,373), fill=(70,70,70,255), width=7)
        ox=oy=40
    else:
        canvas = Image.new("RGB", (320,320))
        draw = ImageDraw.Draw(canvas)
        ground = grass()
        for y in range(16):
            for x in range(16):
                draw.rectangle((x*scale,y*scale,(x+1)*scale-1,(y+1)*scale-1), fill=ground.getpixel((x,y))[:3])
        ox=oy=0
    for y in range(16):
        for x in range(16):
            colour = texture.getpixel((x,y))
            if colour[3]:
                draw.rectangle((ox+x*scale,oy+y*scale,ox+(x+1)*scale-1,oy+(y+1)*scale-1), fill=colour[:3])
    return canvas


def textured_quad(canvas: Image.Image, texture: Image.Image, corners) -> None:
    draw=ImageDraw.Draw(canvas)
    def point(u,v):
        p00,p10,p11,p01=corners
        return ((1-u)*(1-v)*p00[0]+u*(1-v)*p10[0]+u*v*p11[0]+(1-u)*v*p01[0],
                (1-u)*(1-v)*p00[1]+u*(1-v)*p10[1]+u*v*p11[1]+(1-u)*v*p01[1])
    for y in range(16):
        for x in range(16):
            c=texture.getpixel((x,y))
            if not c[3]: continue
            u0,u1=x/16,(x+1)/16; v0,v1=y/16,(y+1)/16
            draw.polygon((point(u0,v0),point(u1,v0),point(u1,v1),point(u0,v1)),fill=c)


def slope(downhill: bool=False) -> Image.Image:
    canvas=Image.new("RGBA",(640,480),(105,157,210,255)); draw=ImageDraw.Draw(canvas)
    def iso(x,y,z): return 150+x*160,365+x*50+z*80-y*90
    def block(x,y,z):
        top=(iso(x,y+1,z),iso(x+1,y+1,z),iso(x+1,y+1,z+1),iso(x,y+1,z+1))
        draw.polygon((iso(x+1,y+1,z),iso(x+1,y,z),iso(x+1,y,z+1),iso(x+1,y+1,z+1)),fill=(94,64,39,255))
        draw.polygon((iso(x,y+1,z+1),iso(x+1,y+1,z+1),iso(x+1,y,z+1),iso(x,y,z+1)),fill=(77,52,33,255))
        textured_quad(canvas,grass(),top)
    block(0,0,0); block(1,1,0)
    p00,p10,p11,p01=(iso(0,1.04,0),iso(1,2.04,0),iso(1,2.04,1),iso(0,1.04,1))
    def mix(a,b,t): return a[0]+(b[0]-a[0])*t,a[1]+(b[1]-a[1])*t
    for t in (.08,.27,.47,.67,.87):
        a,b=mix(p00,p10,t),mix(p01,p11,t)
        draw.line((a,b),fill=(82,59,33,255),width=12); draw.line((a,b),fill=(151,116,65,255),width=7)
    for side in (.18,.82):
        a,b=mix(p00,p01,side),mix(p10,p11,side)
        draw.line((a,b),fill=DARK,width=15); draw.line((a,b),fill=CYAN_DARK,width=11); draw.line((a,b),fill=CYAN,width=7)
        ah,bh=mix(p00,p01,min(1,side+.025)),mix(p10,p11,min(1,side+.025))
        draw.line((ah,bh),fill=HIGHLIGHT,width=3)
    if downhill: canvas=canvas.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    return canvas.convert("RGB")


def main() -> None:
    TEXTURES.mkdir(parents=True,exist_ok=True); ITEM_TEXTURES.mkdir(parents=True,exist_ok=True); OUTPUT.mkdir(parents=True,exist_ok=True)
    straight=confirmed_straight_signal(); curve=realistic_signal(vanilla("rail_corner")); base=switch_base(straight)
    straight.save(TEXTURES/"signal_rail.png"); curve.save(TEXTURES/"signal_rail_corner.png"); base.save(TEXTURES/"switch_rail_base.png")
    states={"all":switch_state(base,None,True),"front":switch_state(base,"front"),"left":switch_state(base,"left"),"right":switch_state(base,"right")}
    states["all"].save(ITEM_TEXTURES/"switch_rail_all_lit.png")
    for name in ("front","left","right"): states[name].save(TEXTURES/f"switch_rail_{name}.png")
    topdown(states["all"],True).save(OUTPUT/"01_switch_inventory_all_lit.png")
    topdown(states["front"]).save(OUTPUT/"02_switch_front.png")
    topdown(states["left"]).save(OUTPUT/"03_switch_left.png")
    topdown(states["right"]).save(OUTPUT/"04_switch_right.png")
    topdown(curve).save(OUTPUT/"05_signal_curve_realistic.png")
    slope(False).save(OUTPUT/"06_signal_uphill_realistic.png")
    slope(True).save(OUTPUT/"07_signal_downhill_realistic.png")


if __name__ == "__main__": main()

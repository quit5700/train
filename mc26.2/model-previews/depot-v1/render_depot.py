from __future__ import annotations

from pathlib import Path
from zipfile import ZipFile
from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
GAME_JAR = Path(r"G:\game\mcfabric\.minecraft\versions\26.2\26.2.jar")
TEXTURES = ROOT / "assets/fabric_train_simulator/textures/block"
OUTPUT = ROOT / "renders"


def vanilla(name: str) -> Image.Image:
    with ZipFile(GAME_JAR) as jar:
        with jar.open(f"assets/minecraft/textures/block/{name}.png") as source:
            return Image.open(source).convert("RGBA")


def grass() -> Image.Image:
    source = vanilla("grass_block_top")
    result = source.copy()
    for y in range(16):
        for x in range(16):
            r,g,b,a = source.getpixel((x,y))
            result.putpixel((x,y),(r*82//148,g*137//148,b*60//148,a))
    return result


def front_texture() -> Image.Image:
    image = vanilla("white_concrete")
    draw = ImageDraw.Draw(image)
    # 能直接用于 16×16 方块面的火车头像素图标。
    dark = (39,39,43,255)
    red_dark = (112,24,25,255)
    red = (177,43,38,255)
    gold = (231,171,54,255)
    glass = (137,211,224,255)
    wheel = (28,28,31,255)
    draw.rectangle((3,5,12,11), fill=red_dark)
    draw.rectangle((4,4,9,10), fill=red)
    draw.rectangle((9,3,12,10), fill=red_dark)
    draw.rectangle((10,4,11,6), fill=glass)
    draw.rectangle((3,3,9,4), fill=dark)
    draw.rectangle((2,7,4,9), fill=gold)
    draw.rectangle((2,10,13,11), fill=dark)
    draw.rectangle((4,11,6,13), fill=wheel)
    draw.rectangle((10,11,12,13), fill=wheel)
    draw.point((3,8), fill=(255,221,105,255))
    return image


def textured_quad(canvas: Image.Image, texture: Image.Image, corners) -> None:
    draw=ImageDraw.Draw(canvas)
    def point(u,v):
        p00,p10,p11,p01=corners
        return ((1-u)*(1-v)*p00[0]+u*(1-v)*p10[0]+u*v*p11[0]+(1-u)*v*p01[0],
                (1-u)*(1-v)*p00[1]+u*(1-v)*p10[1]+u*v*p11[1]+(1-u)*v*p01[1])
    for y in range(16):
        for x in range(16):
            colour=texture.getpixel((x,y))
            if not colour[3]: continue
            u0,u1=x/16,(x+1)/16; v0,v1=y/16,(y+1)/16
            draw.polygon((point(u0,v0),point(u1,v0),point(u1,v1),point(u0,v1)),fill=colour)


def cube(canvas: Image.Image, center_x: int, top_y: int, scale: float) -> None:
    white=vanilla("white_concrete")
    front=front_texture()
    half=150*scale; rise=78*scale; height=158*scale
    top=((center_x,top_y),(center_x+half,top_y+rise),(center_x,top_y+rise*2),(center_x-half,top_y+rise))
    left=(top[3],top[2],(top[2][0],top[2][1]+height),(top[3][0],top[3][1]+height))
    right=(top[2],top[1],(top[1][0],top[1][1]+height),(top[2][0],top[2][1]+height))
    # 左侧作为朝向玩家的正面，右侧为普通白色侧面。
    textured_quad(canvas,front,left)
    textured_quad(canvas,white,right)
    textured_quad(canvas,white,top)


def inventory_preview() -> Image.Image:
    canvas=Image.new("RGBA",(512,512),(0,0,0,0)); draw=ImageDraw.Draw(canvas)
    draw.rectangle((20,20,492,492),fill=(139,139,139,255),outline=(25,25,25,255),width=11)
    draw.line((34,34,478,34),fill=(236,236,236,255),width=8)
    draw.line((34,34,34,478),fill=(236,236,236,255),width=8)
    draw.line((34,478,478,478),fill=(70,70,70,255),width=8)
    draw.line((478,34,478,478),fill=(70,70,70,255),width=8)
    cube(canvas,256,96,.78)
    return canvas


def placed_preview() -> Image.Image:
    canvas=Image.new("RGBA",(720,560),(105,157,210,255))
    ground=grass().resize((560,560),Image.Resampling.NEAREST).rotate(45,expand=True,resample=Image.Resampling.NEAREST)
    ground=ground.resize((760,380),Image.Resampling.NEAREST)
    canvas.alpha_composite(ground,(-20,245))
    cube(canvas,360,100,1.0)
    return canvas.convert("RGB")


def main() -> None:
    TEXTURES.mkdir(parents=True,exist_ok=True); OUTPUT.mkdir(parents=True,exist_ok=True)
    front_texture().save(TEXTURES/"train_depot_front.png")
    inventory_preview().save(OUTPUT/"01_inventory_isometric.png")
    placed_preview().save(OUTPUT/"02_placed_front_isometric.png")


if __name__ == "__main__": main()

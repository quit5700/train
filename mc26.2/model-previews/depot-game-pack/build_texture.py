from pathlib import Path
from zipfile import ZipFile
from PIL import Image

ROOT = Path(__file__).resolve().parent
SOURCE = Path(r"C:\Users\quit5700\AppData\Local\Temp\codex-clipboard-08a6eafa-afcd-4d73-8e8e-df4abcdd9429.png")
GAME_JAR = Path(r"G:\game\mcfabric\.minecraft\versions\26.2\26.2.jar")
OUTPUT = ROOT / "assets/fabric_train_simulator/textures/block/train_depot_side.png"

with ZipFile(GAME_JAR) as jar:
    with jar.open("assets/minecraft/textures/block/white_concrete.png") as stream:
        concrete = Image.open(stream).convert("RGBA").resize((32, 32), Image.Resampling.NEAREST)

source = Image.open(SOURCE).convert("RGBA")
background = Image.new("RGBA", source.size, (255, 255, 255, 255))
background.alpha_composite(source)
gray = background.convert("L")
bounds = gray.point(lambda value: 255 if value < 220 else 0).getbbox()
if bounds is None:
    raise RuntimeError("火车头参考图中没有检测到深色图案")

icon = gray.crop(bounds).resize((29, 25), Image.Resampling.LANCZOS)
mask = icon.point(lambda value: 255 if value < 155 else 0)
black = Image.new("RGBA", icon.size, (18, 18, 20, 255))
concrete.paste(black, (1, 4), mask)

OUTPUT.parent.mkdir(parents=True, exist_ok=True)
concrete.save(OUTPUT)

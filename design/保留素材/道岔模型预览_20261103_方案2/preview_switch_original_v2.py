# -*- coding: utf-8 -*-
"""原道岔材质的矢量预览；不修改原始PNG或模组运行包。"""
from pathlib import Path
import base64
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT/'design/道岔方向预览_原模型_方案2'
ASSETS = ROOT/'versions/0.2.42-test5/mc26.3/src/main/resources/assets/fabric_train_simulator/textures/block'
OUT.mkdir(parents=True, exist_ok=True)
cards=[]
for index,(title,state,overlay) in enumerate([
    ('原模型（对照）','front',False),('直行','front',True),
    ('左转','left',True),('右转','right',True)]):
    encoded=base64.b64encode((ASSETS/('switch_rail_'+state+'.png')).read_bytes()).decode('ascii')
    triangle='<g fill="#ed2024"><rect x="8" y="12" width="1" height="1"/><rect x="7" y="13" width="3" height="1"/><rect x="6" y="14" width="5" height="1"/></g>' if overlay else ''
    x=26+index*264
    cards.append(f'<rect x="{x}" y="108" width="246" height="278" fill="#303945" stroke="#647385"/><text x="{x+123}" y="140" text-anchor="middle" font-size="21">{title}</text><svg x="{x+14}" y="154" width="218" height="218" viewBox="0 0 16 16" shape-rendering="crispEdges"><rect width="16" height="16" fill="#638b48"/><image width="16" height="16" style="image-rendering:pixelated" href="data:image/png;base64,{encoded}"/>{triangle}</svg>')
svg='<svg xmlns="http://www.w3.org/2000/svg" width="1100" height="540" viewBox="0 0 1100 540"><rect width="1100" height="540" fill="#20262e"/><g fill="#f3f5f7" font-family="Microsoft YaHei,sans-serif"><text x="26" y="42" font-size="28">原道岔模型 · 方向标记预览</text><text x="26" y="78" font-size="17" fill="#c6d1db">直接复用现有16×16材质，保留轨道、木枕、钻石色边轨和原方向短线。</text>'+''.join(cards)+'<text x="26" y="426" font-size="18"><tspan fill="#ff6669">红色三角：</tspan>加在原来的固定出发短线上，尖端始终指向直行。</text><text x="26" y="461" font-size="18">另一条原有钻石色短线：分别指示直行、左转、右转。</text><text x="26" y="505" font-size="16" fill="#bcc9d4">原材质叠加标记预览，尚未改动模组模型、原PNG及运行包。</text></g></svg>'
(OUT/'预览.svg').write_text(svg, encoding='utf-8')
(OUT/'预览.html').write_text('<!doctype html><html lang="zh-CN"><meta charset="utf-8"><style>html,body{margin:0;background:#20262e}</style>'+svg+'</html>',encoding='utf-8')
print(OUT/'预览.svg')

# 通过PDF（矢量文档）导出同一个原材质对照板，不操作浏览器。
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.colors import HexColor
font=ROOT/'versions/0.2.42-test5/mc26.3/src/main/resources/assets/station_access/font/noto_sans_sc.ttf'
pdfmetrics.registerFont(TTFont('PreviewSC',str(font)))
c=canvas.Canvas(str(OUT/'预览.pdf'),pagesize=(1100,540))
c.setFillColor(HexColor('#20262e'));c.rect(0,0,1100,540,fill=1,stroke=0)
def text(x,y,size,value,color='#f3f5f7'):
    c.setFillColor(HexColor(color));c.setFont('PreviewSC',size);c.drawString(x,540-y,value)
text(26,42,28,'原道岔模型 · 方向标记预览')
text(26,78,17,'直接复用现有16×16材质，保留轨道、木枕、钻石色边轨和原方向短线。','#c6d1db')
for index,(title,state,overlay) in enumerate([('原模型（对照）','front',False),('直行','front',True),('左转','left',True),('右转','right',True)]):
    x=26+index*264
    c.setFillColor(HexColor('#303945'));c.setStrokeColor(HexColor('#647385'));c.rect(x,154,246,278,fill=1,stroke=1)
    text(x+123-pdfmetrics.stringWidth(title,'PreviewSC',21)/2,140,21,title)
    px=x+11;py=162
    c.setFillColor(HexColor('#638b48'));c.rect(px,py,224,224,fill=1,stroke=0)
    c.drawImage(str(ASSETS/('switch_rail_'+state+'.png')),px,py,224,224,mask='auto')
    if overlay:
        c.setFillColor(HexColor('#ed2024'))
        for xx,yy,ww in [(8,12,1),(7,13,3),(6,14,5)]:c.rect(px+xx*14,py+(15-yy)*14,ww*14,14,fill=1,stroke=0)
text(26,426,18,'红色三角：加在原来的固定出发短线上，尖端始终指向直行。','#ff6669')
text(26,461,18,'另一条原有钻石色短线：分别指示直行、左转、右转。')
text(26,505,16,'原材质叠加标记预览，尚未改动模组模型、原PNG及运行包。','#bcc9d4')
c.save()

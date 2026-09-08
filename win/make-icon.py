from pathlib import Path
from PIL import Image, ImageDraw
root = Path(__file__).parent / 'assets'
im = Image.new('RGBA', (1024, 1024), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
d.rounded_rectangle((0, 0, 1023, 1023), radius=240, fill='#176B49')
def xy(v): return int(v / 108 * 1024)
points = [(35, 43), (73, 35), (57, 73)]
d.line([(xy(x), xy(y)) for x,y in points+[points[0]]], fill='#B5E8C8', width=xy(5), joint='curve')
for x,y in points:
    d.ellipse((xy(x-8),xy(y-8),xy(x+8),xy(y+8)),fill='#FFFFFF')
im.resize((256,256),Image.Resampling.LANCZOS).save(root/'icon.png')
im.save(root/'icon.ico', sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])
(root/'icon.svg').write_text('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108"><rect width="108" height="108" rx="25" fill="#176B49"/><path d="M35 43L73 35L57 73Z" stroke="#B5E8C8" stroke-width="5" fill="none"/>'+''.join(f'<circle cx="{x}" cy="{y}" r="8" fill="white"/>' for x,y in points)+'</svg>')

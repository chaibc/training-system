from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

out = Path(r"E:\deepseek\项目\书法培训\server\uploads\demo")
out.mkdir(parents=True, exist_ok=True)

FONT = r"C:\Windows\Fonts\simsun.ttc"
FONT_BOLD = r"C:\Windows\Fonts\simhei.ttf"


def font(size, bold=False):
    path = FONT_BOLD if bold else FONT
    try:
        return ImageFont.truetype(path, size)
    except Exception:
        return ImageFont.load_default()


def make(name, char, title, subtitle):
    w, h = 1200, 800
    img = Image.new("RGB", (w, h), (246, 241, 231))
    d = ImageDraw.Draw(img)
    # paper grain
    for y in range(h):
        shade = int(3 * (y / h))
        d.line([(0, y), (w, y)], fill=(243 + shade, 237 + shade, 225 + shade))
    # frame
    d.rectangle([60, 60, w - 60, h - 60], outline=(210, 198, 178), width=3)
    d.rectangle([86, 86, w - 86, h - 86], outline=(225, 216, 199), width=1)
    # giant character
    f_char = font(330, True)
    bbox = d.textbbox((0, 0), char, font=f_char)
    cw, ch = bbox[2] - bbox[0], bbox[3] - bbox[1]
    d.text(((w - cw) / 2 - bbox[0], 130 - bbox[1]), char, font=f_char, fill=(43, 39, 32))
    # red seal
    d.rectangle([w - 220, 140, w - 130, 230], fill=(180, 71, 60))
    f_seal = font(42, True)
    for i, c in enumerate("翰墨"):
        d.text((w - 202 + i * 44, 152), c, font=f_seal, fill=(255, 253, 248))
    # labels
    f_title = font(58, True)
    d.text((110, 590), title, font=f_title, fill=(43, 39, 32))
    f_sub = font(30)
    d.text((112, 675), subtitle, font=f_sub, fill=(120, 111, 97))
    # gold line
    d.line([(112, 660), (360, 660)], fill=(194, 160, 94), width=3)
    img.save(out / name, quality=90)
    print(out / name)


make("classroom.jpg", "习", "书法教室", "小班教学 · 逐字点评")
make("copybook.jpg", "临", "临帖区", "从碑帖中理解结构与用笔")
make("gallery.jpg", "作", "作品墙", "每一幅作品都值得被看见")

from PIL import Image, ImageDraw
from pathlib import Path

out = Path(r"E:\deepseek\项目\书法培训\miniprogram\assets\tab")
out.mkdir(parents=True, exist_ok=True)
normal = (161, 154, 139, 255)
active = (180, 71, 60, 255)
size = 81


def canvas():
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def home(color):
    img, d = canvas()
    d.line([(14, 39), (40, 15), (67, 39)], fill=color, width=5, joint="curve")
    d.line([(21, 36), (21, 67), (60, 67), (60, 36)], fill=color, width=5, joint="curve")
    d.rectangle([34, 49, 47, 67], outline=color, width=4)
    return img


def book(color):
    img, d = canvas()
    d.line([(40, 22), (40, 65)], fill=color, width=4)
    d.line([(40, 24), (18, 28), (18, 64), (40, 60)], fill=color, width=4, joint="curve")
    d.line([(40, 24), (62, 28), (62, 64), (40, 60)], fill=color, width=4, joint="curve")
    return img


def image(color):
    img, d = canvas()
    d.rounded_rectangle([14, 18, 67, 63], radius=6, outline=color, width=4)
    d.ellipse([24, 27, 34, 37], outline=color, width=3)
    d.line([(18, 56), (34, 41), (45, 51), (54, 43), (64, 54)], fill=color, width=4, joint="curve")
    return img


def user(color):
    img, d = canvas()
    d.ellipse([29, 15, 52, 38], outline=color, width=4)
    d.arc([16, 40, 65, 78], start=180, end=360, fill=color, width=4)
    return img


for name, fn in [("home", home), ("book", book), ("gallery", image), ("me", user)]:
    fn(normal).save(out / f"{name}.png")
    fn(active).save(out / f"{name}-on.png")
print("generated", out)

from pathlib import Path

root = Path(r"E:\deepseek\项目\书法培训\miniprogram\pages")
changed = []
for path in root.rglob("*.wxml"):
    text = path.read_text(encoding="utf-8")
    if "\\n" in text:
        path.write_text(text.replace("\\n", "\n"), encoding="utf-8")
        changed.append(str(path))
print("changed", len(changed))
for item in changed:
    print(item)

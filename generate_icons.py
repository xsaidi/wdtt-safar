#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / "safarvpn-icon.png"
RES_DIR = ROOT / "app" / "src" / "main" / "res"
BACKGROUND = (0x1A, 0x1A, 0x2E, 255)
RESAMPLE_LANCZOS = getattr(Image, "Resampling", Image).LANCZOS

DENSITIES = {
    "mipmap-mdpi": 1,
    "mipmap-hdpi": 1.5,
    "mipmap-xhdpi": 2,
    "mipmap-xxhdpi": 3,
    "mipmap-xxxhdpi": 4,
}


def remove_checkerboard_background(image: Image.Image) -> Image.Image:
    image = image.convert("RGBA")
    pixels = image.load()

    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = pixels[x, y]
            chroma = max(r, g, b) - min(r, g, b)
            if chroma <= 12:
                alpha = 0
            elif chroma >= 36:
                alpha = a
            else:
                alpha = int(a * (chroma - 12) / 24)
            pixels[x, y] = (r, g, b, alpha)

    bbox = image.getbbox()
    if bbox is None:
        raise ValueError("source icon has no visible pixels")
    return image.crop(bbox)


def fit_icon(icon: Image.Image, content_size: int) -> Image.Image:
    scale = min(content_size / icon.width, content_size / icon.height)
    size = (
        max(1, round(icon.width * scale)),
        max(1, round(icon.height * scale)),
    )
    return icon.resize(size, RESAMPLE_LANCZOS)


def centered_icon(icon: Image.Image, canvas_size: int, content_size: int) -> Image.Image:
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    fitted = fit_icon(icon, content_size)
    offset = ((canvas_size - fitted.width) // 2, (canvas_size - fitted.height) // 2)
    canvas.alpha_composite(fitted, offset)
    return canvas


def legacy_icon(icon: Image.Image, size: int, round_mask: bool = False) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), BACKGROUND)

    if round_mask:
        mask = Image.new("L", (size, size), 0)
        draw = ImageDraw.Draw(mask)
        draw.ellipse((0, 0, size - 1, size - 1), fill=255)
        canvas.putalpha(mask)

    content = round(size * 0.78)
    fitted = fit_icon(icon, content)
    offset = ((size - fitted.width) // 2, (size - fitted.height) // 2)
    canvas.alpha_composite(fitted, offset)
    return canvas


def save_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, "PNG", optimize=True)


def main() -> None:
    if not SOURCE.exists():
        raise FileNotFoundError(f"source icon not found: {SOURCE}")

    icon = remove_checkerboard_background(Image.open(SOURCE))

    for density, scale in DENSITIES.items():
        base = RES_DIR / density
        legacy_size = round(48 * scale)
        adaptive_size = round(108 * scale)
        adaptive_content = round(72 * scale)

        save_png(legacy_icon(icon, legacy_size), base / "ic_launcher.png")
        save_png(legacy_icon(icon, legacy_size, round_mask=True), base / "ic_launcher_round.png")
        save_png(
            centered_icon(icon, adaptive_size, adaptive_content),
            base / "ic_launcher_foreground.png",
        )
        save_png(
            Image.new("RGBA", (adaptive_size, adaptive_size), BACKGROUND),
            base / "ic_launcher_background.png",
        )


if __name__ == "__main__":
    main()

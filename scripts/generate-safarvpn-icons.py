#!/usr/bin/env python3
import os
import struct
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

BLUE = (0x1E, 0x88, 0xE5, 255)
GREEN = (0x43, 0xA0, 0x47, 255)
WHITE = (255, 255, 255, 255)
DARK = (18, 18, 18, 255)
INK = (255, 255, 255, 255)
STROKE = (12, 47, 92, 70)

DENSITIES = {
    "mipmap-mdpi": (48, 108),
    "mipmap-hdpi": (72, 162),
    "mipmap-xhdpi": (96, 216),
    "mipmap-xxhdpi": (144, 324),
    "mipmap-xxxhdpi": (192, 432),
}


def lerp(a, b, t):
    return int(round(a + (b - a) * t))


def blend(dst, src):
    sa = src[3] / 255.0
    da = dst[3] / 255.0
    out_a = sa + da * (1.0 - sa)
    if out_a <= 0:
        return (0, 0, 0, 0)
    rgb = []
    for i in range(3):
        rgb.append(int(round((src[i] * sa + dst[i] * da * (1.0 - sa)) / out_a)))
    return (rgb[0], rgb[1], rgb[2], int(round(out_a * 255)))


def write_png(path, width, height, pixels):
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for x in range(width):
            raw.extend(pixels[y * width + x])

    def chunk(kind, data):
        return (
            struct.pack(">I", len(data))
            + kind
            + data
            + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
        )

    png = (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + chunk(b"IEND", b"")
    )
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


def point_in_poly(px, py, points):
    inside = False
    j = len(points) - 1
    for i in range(len(points)):
        xi, yi = points[i]
        xj, yj = points[j]
        if ((yi > py) != (yj > py)) and (px < (xj - xi) * (py - yi) / ((yj - yi) or 1e-9) + xi):
            inside = not inside
        j = i
    return inside


def draw_circle(pixels, width, height, cx, cy, radius, color):
    r2 = radius * radius
    x0 = max(0, int(cx - radius - 1))
    x1 = min(width - 1, int(cx + radius + 1))
    y0 = max(0, int(cy - radius - 1))
    y1 = min(height - 1, int(cy + radius + 1))
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            dx = x + 0.5 - cx
            dy = y + 0.5 - cy
            if dx * dx + dy * dy <= r2:
                idx = y * width + x
                pixels[idx] = blend(pixels[idx], color)


def draw_line(pixels, width, height, x1, y1, x2, y2, radius, color):
    steps = max(1, int(max(abs(x2 - x1), abs(y2 - y1)) / max(1, radius / 2)))
    for i in range(steps + 1):
        t = i / steps
        draw_circle(
            pixels,
            width,
            height,
            x1 + (x2 - x1) * t,
            y1 + (y2 - y1) * t,
            radius,
            color,
        )


def draw_shield(pixels, width, height, scale=1.0):
    cx = width / 2
    cy = height / 2
    size = min(width, height) * scale
    left = cx - size / 2
    top = cy - size / 2

    def p(x, y):
        return (left + x * size, top + y * size)

    shield = [
        p(0.50, 0.07),
        p(0.82, 0.19),
        p(0.76, 0.67),
        p(0.50, 0.90),
        p(0.24, 0.67),
        p(0.18, 0.19),
    ]

    min_x = max(0, int(min(x for x, _ in shield)))
    max_x = min(width - 1, int(max(x for x, _ in shield)))
    min_y = max(0, int(min(y for _, y in shield)))
    max_y = min(height - 1, int(max(y for _, y in shield)))

    for y in range(min_y, max_y + 1):
        t = (y - min_y) / max(1, max_y - min_y)
        color = (
            lerp(BLUE[0], GREEN[0], t),
            lerp(BLUE[1], GREEN[1], t),
            lerp(BLUE[2], GREEN[2], t),
            255,
        )
        for x in range(min_x, max_x + 1):
            if point_in_poly(x + 0.5, y + 0.5, shield):
                pixels[y * width + x] = blend(pixels[y * width + x], color)

    for a, b in zip(shield, shield[1:] + shield[:1]):
        draw_line(pixels, width, height, a[0], a[1], b[0], b[1], max(1, size * 0.018), STROKE)

    r = size * 0.038
    draw_line(pixels, width, height, *p(0.62, 0.31), *p(0.43, 0.31), r, INK)
    draw_line(pixels, width, height, *p(0.43, 0.31), *p(0.37, 0.47), r, INK)
    draw_line(pixels, width, height, *p(0.37, 0.50), *p(0.62, 0.50), r, INK)
    draw_line(pixels, width, height, *p(0.62, 0.50), *p(0.67, 0.67), r, INK)
    draw_line(pixels, width, height, *p(0.67, 0.69), *p(0.39, 0.69), r, INK)


def downsample(pixels, width, height, factor):
    out_w = width // factor
    out_h = height // factor
    out = []
    area = factor * factor
    for y in range(out_h):
        for x in range(out_w):
            acc = [0, 0, 0, 0]
            for yy in range(factor):
                for xx in range(factor):
                    px = pixels[(y * factor + yy) * width + (x * factor + xx)]
                    for i in range(4):
                        acc[i] += px[i]
            out.append(tuple(v // area for v in acc))
    return out


def render(size, background, shield_scale, round_icon=False):
    factor = 4
    w = h = size * factor
    bg = background
    pixels = [bg for _ in range(w * h)]

    if round_icon:
        pixels = [(0, 0, 0, 0) for _ in range(w * h)]
        draw_circle(pixels, w, h, w / 2, h / 2, w * 0.47, bg)

    draw_shield(pixels, w, h, shield_scale)
    return downsample(pixels, w, h, factor)


def main():
    for density, (legacy_size, foreground_size) in DENSITIES.items():
        base = os.path.join(ROOT, "app", "src", "main", "res", density)
        write_png(
            os.path.join(base, "ic_launcher.png"),
            legacy_size,
            legacy_size,
            render(legacy_size, WHITE, 0.72),
        )
        write_png(
            os.path.join(base, "ic_launcher_round.png"),
            legacy_size,
            legacy_size,
            render(legacy_size, WHITE, 0.70, round_icon=True),
        )
        write_png(
            os.path.join(base, "ic_launcher_foreground.png"),
            foreground_size,
            foreground_size,
            render(foreground_size, (0, 0, 0, 0), 0.58),
        )

    write_png(
        os.path.join(ROOT, "app", "src", "main", "ic_launcher-playstore.png"),
        512,
        512,
        render(512, WHITE, 0.72),
    )


if __name__ == "__main__":
    main()

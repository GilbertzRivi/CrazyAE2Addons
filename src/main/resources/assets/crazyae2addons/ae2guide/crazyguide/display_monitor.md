---
navigation:
  parent: crazyae2addons_index.md
  title: Display Monitor
  icon: crazyae2addonslite:display_monitor
categories:
  - Monitoring and Automation
item_ids:
  - crazyae2addonslite:display_monitor
---

# Display Monitor - User Guide

## Short Reference

### Formatting

* `**bold**`
* `*italic*`
* `__underline__`
* `~~strikethrough~~`
* `#`, `##`, `###`, ... — headings
* `* ` or `- ` at line start — bullet
* `>>` at line start — indentation marker
* real newlines or `&nl` — line breaks

### Colors

* `&cRRGGBB` — set text color for following text
* `&cRRGGBB(...)` — color only the text inside the parentheses
* `&bRRGGBB` — set display background color

### Tokens

* `&i^id` — inline icon
* `&s^id` — ME stock amount
* `&s^id%N` — ME stock divided by `10^N`, rounded
* `&d^id@WINDOW` — average delta over `WINDOW`, shown per 1 second
* `&d^id%PER@WINDOW` — average delta over `WINDOW`, shown per `PER`
* `&( expression )` — math expression, evaluated after token resolution

### Tables

Markdown-style tables are supported, including alignment markers:

* `|:---|` — left
* `|:---:|` — center
* `|---:|` — right

---

## What it does

The Display Monitor renders formatted text with inline icons, ME stock values, rates, math expressions, headings, bullets, indentation, tables and images.

---

## Quick Start

Type text into the monitor and use tokens directly.

Example:

```text
&b101820# System
* &i^minecraft:iron_ingot stock: &s^minecraft:iron_ingot
* &i^minecraft:iron_ingot /s: &d^minecraft:iron_ingot@10s
* stacks: &(&s^minecraft:iron_ingot / 64)
```

---

## New Lines

You can use:

* real new lines
* `&nl`

Example:

```text
line one&nlline two
```

---

## Text Formatting

Supported inline styles:

* `**bold**`
* `*italic*`
* `__underline__`
* `~~strikethrough~~`

Example:

```text
**bold** *italic* __underline__ ~~strikethrough~~
```

### Headings

Lines starting with `#` scale up automatically.

Examples:

```text
# Heading 1
## Heading 2
### Heading 3
```

### Bullets

A line starting with `* ` or `- ` becomes a bullet.

Example:

```text
* first
- second
```

### Indentation

A line starting with one or more `>>` gets visual indentation markers.

Example:

```text
>> once
>>>> twice
```

---

## Colors

### Text Color

`&cRRGGBB` changes the current text color.

Example:

```text
&cFF0000red &c00FF00green &cFFFFFFwhite
```

### Scoped Text Color

`&cRRGGBB(...)` applies the color only inside the parentheses.

Example:

```text
normal &cFF0000(red only) normal again
```

### Background Color

`&bRRGGBB` sets the display background color.

Example:

```text
&b202020hello
```

Background color is global for the rendered display. If multiple background tokens appear, the last one wins.

---

## Inline Icons

Use:

```text
&i^id
```

### Supported lookup forms

#### Auto-resolved

The monitor tries, in order:

1. item
2. block
3. fluid

Examples:

```text
&i^minecraft:diamond
&i^minecraft:oak_log
&i^minecraft:water
```

#### Explicit type prefix

You can force the type:

```text
&i^item:minecraft:iron_ingot
&i^fluid:minecraft:water
```

#### Compat key prefixes

Compat-specific prefixes registered by the mod are also supported.

If an icon cannot be resolved, the token is rendered as plain text.

---

## ME Stock Tokens

Use:

```text
&s^id
&s^id%N
```

Examples:

```text
&s^minecraft:iron_ingot
&s^fluid:minecraft:water
&s^minecraft:iron_ingot%1
&s^minecraft:iron_ingot%2
```

### Meaning of `%N`

The value is divided by `10^N` and rounded.

If storage has `64`:

* `&s^minecraft:iron_ingot` → `64`
* `&s^minecraft:iron_ingot%1` → `6`
* `&s^minecraft:iron_ingot%2` → `1`

### Key resolution

Stock tokens support:

* plain ids like `minecraft:iron_ingot`
* explicit prefixes like `item:minecraft:iron_ingot`
* explicit prefixes like `fluid:minecraft:water`
* compat prefixes registered by the mod

If the key resolves but the network has none of it, the result is `0`.

---

## Delta / Rate Tokens

Use:

```text
&d^id@WINDOW
&d^id%PER@WINDOW
```

### Units

* `t` = ticks
* `s` = seconds
* `m` = minutes

### Meaning

* `WINDOW` = how far back the monitor looks
* `PER` = what unit the result is scaled to

If `%PER` is omitted, the result is shown per 1 second.

Examples:

```text
&d^minecraft:iron_ingot@10s
&d^minecraft:iron_ingot%1m@5m
&d^fluid:minecraft:water@30s
```

### Notes

* minimum window: `1s`
* maximum window: `30m`
* output is signed:

  * positive values show `+`
  * negative values show `-`

---

## Math Expressions

Use:

```text
&( expression )
```

Math is evaluated after token resolution.

Examples:

```text
&(&s^minecraft:iron_ingot / 64)
&((&s^minecraft:iron_ingot / 64) * 2 + 1)
```

Nested expressions are supported.

If parsing fails, the result is:

```text
ERR
```

---

## Tables

Markdown-style tables are supported.

Example:

```text
| Item | Icon | Stock | Rate |
|:-----|:----:|------:|:----:|
| Iron | &i^minecraft:iron_ingot | &s^minecraft:iron_ingot | &d^minecraft:iron_ingot@10s |
| Water | &i^fluid:minecraft:water | &s^fluid:minecraft:water | &d^fluid:minecraft:water@30s |
```

### Alignment

* `|:---|` → left
* `|:---:|` → center
* `|---:|` → right

### Notes

* cells can contain inline formatting
* cells can contain icons
* cells can contain stock / delta / math tokens
* color tokens placed before the first `|` on a row are applied to that whole row

---

## Example

```text
&b101820# Display
**bold** *italic* __underline__ ~~strike~~ &cFF4444(red)&nl>> scoped &c55FF55green
* Item &i^minecraft:iron_ingot &s^minecraft:iron_ingot
* Block &i^minecraft:oak_log
* Fluid &i^fluid:minecraft:water &s^fluid:minecraft:water
* Rate &d^minecraft:iron_ingot@10s / &d^minecraft:iron_ingot%1m@5m
* Math &(&s^minecraft:iron_ingot / 64)
| Name | Icon | Stock | Rate |
|:-----|:----:|------:|:----:|
| Iron | &i^minecraft:iron_ingot | &s^minecraft:iron_ingot | &d^minecraft:iron_ingot@10s |
| Water | &i^fluid:minecraft:water | &s^fluid:minecraft:water | &d^fluid:minecraft:water@30s |
```

---

## Troubleshooting

* unresolved icon token → shown as plain text
* invalid math expression → `ERR`
* valid ME key with no stored amount → `0`
* delta may show `0` until enough history is collected

---

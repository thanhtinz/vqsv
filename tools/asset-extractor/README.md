# VQSV Asset Extractor

Converts the original J2ME game's custom binary assets into open formats
(PNG + JSON) for the modern LibGDX / Flutter clients.

## Usage

```bash
cd tools/asset-extractor
python3 extract.py ../../assets/original/vqsv-original.jar out
```

Output:

```
out/
├── img/   img_N.png      (309 files — verified PNG, byte-exact)
├── spr/   spr_N.json     (345 files — sprite tables, partial decode)
├── map/   map_N.json     (102 files — tile maps, partial decode)
└── ui/    NAME.json      (44 files  — UI layouts, partial decode)
```

Point the output at a client's asset folder to bundle real game art, e.g.:

```bash
python3 extract.py ../../assets/original/vqsv-original.jar ../../clients/android/app/src/main/assets/game
```

## Updating after a game patch

Drop the new game JAR into `assets/original/`, re-run the command, and commit the
refreshed JAR. Derived assets are regenerated, never hand-edited — this keeps
the pipeline reproducible.

## Notes

- Only `python3` (stdlib) is required. No third-party packages.
- `img` conversion is byte-exact and verified.
- `spr` / `map` / `ui` are partially decoded (headers parsed, payloads captured
  losslessly). See [`docs/ASSET-FORMATS.md`](../../docs/ASSET-FORMATS.md) for the
  format breakdown and how to refine them.

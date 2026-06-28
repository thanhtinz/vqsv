# VQSV Asset Extractor

Converts the original J2ME game's custom binary assets into open formats
(PNG + JSON) for the modern LibGDX / Flutter clients.

The converted assets are already committed under
`clients/core/src/main/resources/game/`, so you only need this tool when
re-extracting from a new game JAR (which you supply — the JAR is not stored in
this repo).

## Usage

```bash
cd tools/asset-extractor
python3 extract.py path/to/game.jar out
```

Output:

```
out/
├── img/   img_N.png      (309 files — verified PNG, byte-exact)
├── spr/   spr_N.json     (345 files — sprite tables, partial decode)
├── map/   map_N.json     (102 files — tile maps, partial decode)
└── ui/    NAME.json      (44 files  — UI layouts, partial decode)
```

To refresh the assets the clients actually use, write straight into the resources
tree and commit the result:

```bash
python3 extract.py path/to/game.jar ../../clients/core/src/main/resources/game
```

## Updating after a game patch

Re-run the command above with the new JAR and commit the refreshed
`clients/core/src/main/resources/game/`. Derived assets are regenerated, never
hand-edited — this keeps the pipeline reproducible.

## Notes

- Only `python3` (stdlib) is required. No third-party packages.
- `img` conversion is byte-exact and verified.
- `spr` / `map` / `ui` are partially decoded (headers parsed, payloads captured
  losslessly). See [`docs/ASSET-FORMATS.md`](../../docs/ASSET-FORMATS.md) for the
  format breakdown and how to refine them.

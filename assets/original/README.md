# Original game (source of truth)

`vqsv-original.jar` is the original J2ME game. It is the SOURCE the asset pipeline
converts from — the converted, ready-to-use assets are committed under
`clients/core/assets/game/`, so clients build & run without needing this JAR.

Re-run the pipeline after replacing the JAR to regenerate assets:

```bash
python3 tools/asset-extractor/extract.py assets/original/vqsv-original.jar clients/core/assets/game
```

See `docs/ASSET-FORMATS.md` for the decoded binary formats.

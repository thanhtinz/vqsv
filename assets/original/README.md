# Original Game Asset Source

Place the original J2ME game JAR here as the **source of truth** for all client
assets:

```
assets/original/vqsv-original.jar
```

The asset pipeline reads this file and regenerates the open-format assets
(PNG + JSON) consumed by the modern clients:

```bash
cd tools/asset-extractor
python3 extract.py ../../assets/original/vqsv-original.jar out
```

See [`docs/ASSET-FORMATS.md`](../../docs/ASSET-FORMATS.md) for the binary format
breakdown and [`tools/asset-extractor/README.md`](../../tools/asset-extractor/README.md)
for usage.

## Why the JAR isn't committed yet

The JAR is a ~1.8 MB binary. It could not be committed through this automated
session because the environment's git egress is proxied (direct `git push` is
blocked) and the GitHub content API path available here stores file content as
text, which would corrupt binary data.

**To add it:** drop `vqsv-original.jar` into this folder and commit it normally
from a local clone, or upload it via the GitHub web UI
("Add file" → "Upload files"). Once present, the extractor works unchanged.

> Reproducibility: keep only the JAR + the extractor in version control. The
> derived `out/` assets are regenerated on demand, so a game update is just
> "replace the JAR, re-run the tool".

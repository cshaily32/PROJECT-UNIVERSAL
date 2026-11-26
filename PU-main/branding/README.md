# PU app icons

These are the supplied master icons for the two shipping applications:

| Edition | Master SVG | Android launcher resource |
| --- | --- | --- |
| PU Lite | `pu-lite.svg` | `../lite/src/main/res/drawable/ic_launcher.xml` |
| PU Plus | `pu-plus.svg` | `../plus/src/main/res/drawable/ic_launcher.xml` |

Android does not accept browser SVG files directly as launcher resources, so each supplied master is translated to a native Android VectorDrawable with the same paths, gradients, rounded tile, and edition-specific mark. Each application module references its own resource in `AndroidManifest.xml`.

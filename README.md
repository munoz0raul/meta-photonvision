# meta-photonvision

A Yocto layer that adds [PhotonVision](https://photonvision.org) — the FRC
(FIRST Robotics Competition) vision-coprocessor software — to **Qualcomm Linux
(QLI) 2.0** images for the Dragonwing **IQ-8275 EVK** (and any other aarch64
QLI target).

It follows the *prebuilt-artifact* strategy: rather than building PhotonVision
from source (which would drag Gradle + Node.js/pnpm + the WPILib toolchain into
the Yocto build), it installs the upstream **`linuxarm64` fat JAR** and a
**JRE 25**, wired up as a systemd service exactly like the official
`photon-image-modifier` install does on Debian.

## Why this works on QLI 2.0

Everything PhotonVision needs natively is *already* in the QLI 2.0
`qcom-multimedia-image` (verified against the IQ-8275 EVK rootfs manifest):

| Dependency        | In QLI 2.0 image | Version   |
|-------------------|:----------------:|-----------|
| glibc             | ✅               | 2.43      |
| libstdc++6        | ✅               | 15.2.0    |
| libcamera + -gst  | ✅               | 0.6.0     |
| libv4l (USB cams) | ✅               | 1.32.0    |
| GStreamer         | ✅               | 1.28.2    |
| **Java runtime**  | ❌ **(added by this layer)** | Temurin JRE 25.0.4 |

The prebuilt arm64 JAR bundles its own native OpenCV / mrcal / libcamera JNI
`.so` files, all built against glibc — matching QLI (which is glibc, not musl).

## Layer contents

```
meta-photonvision/
├── conf/layer.conf
├── COPYING.MIT
├── recipes-devtools/temurin-jre/
│   └── temurin-jre-bin_25.0.4.bb          # prebuilt Adoptium Temurin JRE 25 (aarch64)
├── recipes-photonvision/
│   ├── photonvision/
│   │   ├── photonvision_2026.3.4.bb       # fetches the linuxarm64 fat JAR
│   │   └── files/photonvision.service     # systemd unit (mirrors upstream)
│   ├── packagegroups/
│   │   └── packagegroup-photonvision.bb   # PV + JRE + camera/util deps
│   └── images/
│       └── photonvision-image.bb          # optional demo image
└── README.md
```

## Integrating into the QLI 2.0 kas build

The build server workspace lives at
`/local/mnt/workspace/build/qli-2.0`. Two steps:

### 1. Make kas clone this layer into the workspace

Edit `meta-qcom/ci/qcom-distro.yml` and add a `repos:` entry (per the
Dragonwing "Add third-party layers to the workspace" guide):

```yaml
repos:
  meta-photonvision:
    url: <git URL where you push this layer>
    branch: main
    # If you keep it as a local path instead of a git repo, drop url/branch and
    # kas will treat an already-present dir under the workspace as a layer.
```

### 2. Add PhotonVision to a target image

Either add the packagegroup to an existing image via a `local_conf_header` in
your kas file:

```yaml
local_conf_header:
  photonvision: |
    IMAGE_INSTALL:append = " packagegroup-photonvision"
```

…or build the bundled demo image target:

```yaml
target:
  - photonvision-image
```

Then build as usual:

```bash
kas build meta-qcom/ci/iq-8275-evk.yml:meta-qcom/ci/qcom-distro.yml
```

## ⚠️ Before the first build — two checksums to fill in

Both prebuilt fetches have **placeholder checksums** that must be replaced.
BitBake prints the correct value on the first (failing) fetch, or precompute:

1. **PhotonVision JAR** — in `photonvision_2026.3.4.bb`, `SRC_URI[sha256sum]`:
   ```bash
   curl -sL https://github.com/PhotonVision/photonvision/releases/download/v2026.3.4/photonvision-v2026.3.4-linuxarm64.jar | sha256sum
   ```
   (The real sha256 is already set in the recipe:
   `ccaf5e862a4427c90cb063953903e4967e2041747b1d3f9d0f04b68e1cd975dc`.)

2. **Temurin JRE** — the sha256 in `temurin-jre-bin_25.0.4.bb` is already set to
   the value published by the Adoptium API
   (`1f2644427000316bc431df3389504551ed7464fe8486bf6b4f1130af9ffc8f55`), but the
   `LIC_FILES_CHKSUM` md5 for the bundled LICENSE is a placeholder — fill it in
   from the first build error.

## Runtime

Once flashed and booted on the IQ-8275 EVK:

- Web UI: `http://<device-ip>:5800`
- NetworkTables: TCP 5810 / 5811 (client), served for robot code
- Service: `systemctl status photonvision`
- Logs: `journalctl -u photonvision` and `/opt/photonvision/logs`

## Open items / caveats

- **JDK 25 vs meta-java** — JDK 25 is newer than the OpenJDK recipes in the
  `meta-java` OE layer, so this layer ships the Adoptium binary directly. If you
  later add `meta-java` with a 25 recipe, you can drop `temurin-jre-bin` and
  `RDEPEND` on `openjdk-25` instead.
- **Camera backend** — PhotonVision auto-detects USB (V4L2) cameras out of the
  box. CSI/MIPI cameras via the Qualcomm camera stack are *not* automatically
  wired to PhotonVision; USB UVC cameras are the low-risk starting point.
- **License compliance** — GPL-3.0 (PhotonVision) and GPL-2.0-w-CPE (OpenJDK).
  Both are redistributed unmodified as prebuilt binaries.

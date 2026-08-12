SUMMARY = "PhotonVision — FRC vision coprocessor software"
DESCRIPTION = "PhotonVision is a free, fast, open-source vision-processing \
solution for FIRST Robotics Competition. Installs the upstream prebuilt \
linuxarm64 fat JAR to /opt/photonvision and runs it as a systemd service. \
The bundled libphotonlibcamera.so is replaced at install time with a locally \
built version that adds IMX577 / single-plane ABGR8888 support for QCS8275."
HOMEPAGE = "https://photonvision.org"
BUGTRACKER = "https://github.com/PhotonVision/photonvision/issues"

LICENSE = "GPL-3.0-or-later"
# The published JAR does not ship a standalone LICENSE file next to it, so we
# point at the repository license mirrored into this layer's files/ dir.
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-or-later;md5=1c76c4cc354acaac30ed4d5eefea7245"

# --- Version pin ---------------------------------------------------------
# Latest stable at time of writing. Bump PV, then update the sha256 below
# (bitbake prints the correct value on a checksum mismatch).
PV = "2026.3.4"
PV_TAG = "v${PV}"
PV_JAR = "photonvision-${PV_TAG}-linuxarm64.jar"

SRC_URI = "https://github.com/PhotonVision/photonvision/releases/download/${PV_TAG}/${PV_JAR};downloadfilename=${PV_JAR};unpack=0 \
           file://photonvision.service \
           file://patch-libphotonlibcamera.py \
           file://photon-java-patches.tar.gz;unpack=0 \
           file://photonvision-modules.conf"
SRC_URI[sha256sum] = "ccaf5e862a4427c90cb063953903e4967e2041747b1d3f9d0f04b68e1cd975dc"
# ^ sha256 of photonvision-v2026.3.4-linuxarm64.jar (verified). If you bump PV,
#   recompute:  curl -sL <url> | sha256sum

# Prebuilt arm64 JAR (contains native OpenCV/libcamera/mrcal .so for arm64).
COMPATIBLE_HOST = "aarch64.*-linux"

# The JAR is not extracted into a source tree, so point S at UNPACKDIR (where
# the downloaded file lands) to avoid the "S doesn't exist" do_unpack warning.
S = "${UNPACKDIR}"

inherit systemd

SYSTEMD_SERVICE:${PN} = "photonvision.service"
# Match upstream: enabled at boot.
SYSTEMD_AUTO_ENABLE = "enable"

# Nothing to compile — it's a fat JAR.
do_compile[noexec] = "1"

# Depend on our locally-built driver so libphotonlibcamera.so is in the sysroot
# before we try to patch it into the JAR. python3-native provides ${PYTHON}.
do_install[depends] += "photon-libcamera-gl-driver:do_populate_sysroot python3-native:do_populate_sysroot"

do_install() {
    install -d ${D}/opt/photonvision
    install -m 0644 ${UNPACKDIR}/${PV_JAR} ${D}/opt/photonvision/photonvision.jar

    # Replace the bundled libphotonlibcamera.so with our QCS8275-capable build.
    # The helper script updates linux/arm64/shared/libphotonlibcamera.so inside
    # the JAR and refreshes its MD5 in ResourceInformation.json.
    ${PYTHON} ${UNPACKDIR}/patch-libphotonlibcamera.py \
        ${D}/opt/photonvision/photonvision.jar \
        ${STAGING_LIBDIR}/libphotonlibcamera.so \
        ${UNPACKDIR}/photon-java-patches.tar.gz

    install -d ${D}/opt/photonvision/logs

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/photonvision.service ${D}${systemd_system_unitdir}/photonvision.service

    install -d ${D}${sysconfdir}/modules-load.d
    install -m 0644 ${UNPACKDIR}/photonvision-modules.conf ${D}${sysconfdir}/modules-load.d/photonvision.conf
}

FILES:${PN} += "/opt/photonvision ${systemd_system_unitdir}/photonvision.service ${sysconfdir}/modules-load.d/photonvision.conf"

# Runtime dependencies. The JRE is our bundled Temurin build; the rest match
# the packages photon-image-modifier apt-installs on Debian:
#   avahi-daemon libatomic1 v4l-utils sqlite3 openjdk-25-jre-headless usbtop
# libcamera / gstreamer are already in the QLI multimedia image.
RDEPENDS:${PN} = " \
    temurin-jre-bin \
    avahi-daemon \
    libatomic \
    v4l-utils \
    sqlite3 \
"

# The prebuilt fat JAR bundles arch-specific native libraries; do not let QA
# complain that this 'all/noarch-looking' package contains arm64 ELF.
INSANE_SKIP:${PN} += "arch"

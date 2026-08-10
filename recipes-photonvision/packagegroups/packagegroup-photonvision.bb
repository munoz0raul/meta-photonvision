SUMMARY = "PhotonVision and its runtime dependencies"
DESCRIPTION = "Convenience packagegroup: pulls PhotonVision, the Temurin JRE, \
and the camera/utility userspace it relies on. Add \
'packagegroup-photonvision' to your image (or IMAGE_INSTALL) to get a \
ready-to-run vision coprocessor."

inherit packagegroup

# libatomic is dynamically renamed per-arch (libatomic -> libatomic1), and an
# allarch packagegroup is not allowed to depend on such packages. Several of
# our RDEPENDS (the JRE, native camera libs) are arch-specific anyway, so make
# this packagegroup machine-specific.
PACKAGE_ARCH = "${MACHINE_ARCH}"

RDEPENDS:${PN} = " \
    photonvision \
    temurin-jre-bin \
    avahi-daemon \
    v4l-utils \
    libcamera \
    libcamera-gst \
    gstreamer1.0 \
    gstreamer1.0-plugins-base \
    gstreamer1.0-plugins-good \
    sqlite3 \
    libatomic \
"

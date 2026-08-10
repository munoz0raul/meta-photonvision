SUMMARY = "PhotonVision and its runtime dependencies"
DESCRIPTION = "Convenience packagegroup: pulls PhotonVision, the Temurin JRE, \
and the camera/utility userspace it relies on. Add \
'packagegroup-photonvision' to your image (or IMAGE_INSTALL) to get a \
ready-to-run vision coprocessor."

inherit packagegroup

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

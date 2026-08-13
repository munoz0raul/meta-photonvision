SUMMARY = "PhotonVision and its runtime dependencies"
DESCRIPTION = "Convenience packagegroup: pulls PhotonVision, the Temurin JRE, \
and the camera/utility userspace it relies on. Add \
'packagegroup-photonvision' to your image (or IMAGE_INSTALL) to get a \
ready-to-run vision coprocessor."

inherit packagegroup

# `inherit packagegroup` forces allarch, and the packagegroup QA check hard-errors
# if an allarch packagegroup RDEPENDS on a dynamically-renamed package. The only
# such dep here is libatomic (renamed to libatomic1). It is not listed below
# because photonvision (the recipe) already RDEPENDS on libatomic directly, so it
# is still pulled into the image transitively — the packagegroup does not need it.
# The RDEPENDS below are all statically-named packages, so allarch is fine.

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
"

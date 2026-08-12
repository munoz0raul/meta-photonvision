SUMMARY = "photon-libcamera-gl-driver — PhotonVision libcamera/OpenGL bridge"
DESCRIPTION = "Builds libphotonlibcamera.so from source. \
Adds IMX577 / single-plane ABGR8888 support for Qualcomm QCS8275 \
(IQ-8275 EVK) alongside the existing Raspberry Pi YUV420 path."
HOMEPAGE = "https://github.com/munoz0raul/photon-libcamera-gl-driver"

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# Pin to the tip of our feature branch (update SRCREV on every rebase/merge).
SRCREV = "db12e1b19b3ba961d1dc799820ea88c8f63f9c96"
SRC_URI = "git://github.com/munoz0raul/photon-libcamera-gl-driver.git;protocol=https;branch=feature/imx577-qcs8275 \
           https://frcmaven.wpi.edu/artifactory/release/edu/wpi/first/thirdparty/frc2025/opencv/opencv-cpp/4.10.0-3/opencv-cpp-4.10.0-3-linuxarm64.zip;name=opencv_lib;subdir=opencv_lib \
           https://frcmaven.wpi.edu/artifactory/release/edu/wpi/first/thirdparty/frc2025/opencv/opencv-cpp/4.10.0-3/opencv-cpp-4.10.0-3-headers.zip;name=opencv_header;subdir=opencv_header \
           "
SRC_URI[opencv_lib.sha256sum] = "be814284499e70c94c11934f2ab6ce2f90714f76031d3384957f071cec7f30bc"
SRC_URI[opencv_header.sha256sum] = "b5b7c4a73300b71b96569a26041bc59702b6d4974e60725a569e2d50b140d65e"

inherit cmake pkgconfig

# libcamera pkg-config is available in the QLI sysroot.
DEPENDS = "libcamera virtual/egl virtual/libgles2 virtual/libgbm libdrm pkgconfig-native"

# JNI headers: pass them directly from the photonvision build tools JDK so
# CMake skips find_package(JNI) (which would need a host JDK in the sysroot).
# The JDK is already present on this build server from the PhotonVision Gradle build.
JNI_INCLUDE = "/local/mnt/workspace/build/photonvision-src/tools/jdk-17.0.13+11/include"

EXTRA_OECMAKE = " \
    -DCMAKE_BUILD_TYPE=Release \
    -DJNI_INCLUDE_DIRS='${JNI_INCLUDE};${JNI_INCLUDE}/linux' \
    -DFETCHCONTENT_SOURCE_DIR_OPENCV_LIB=${UNPACKDIR}/opencv_lib \
    -DFETCHCONTENT_SOURCE_DIR_OPENCV_HEADER=${UNPACKDIR}/opencv_header \
"

do_install() {
    install -d ${D}${libdir}
    install -m 0755 ${B}/libphotonlibcamera.so ${D}${libdir}/libphotonlibcamera.so
}

FILES:${PN} = "${libdir}/libphotonlibcamera.so"
FILES:${PN}-dev = ""

# The .so is arm64 and intentionally has no soname — suppress QA noise.
INSANE_SKIP:${PN} += "ldflags dev-so"

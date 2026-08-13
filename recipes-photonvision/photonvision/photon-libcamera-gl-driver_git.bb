SUMMARY = "photon-libcamera-gl-driver — PhotonVision libcamera/OpenGL bridge"
DESCRIPTION = "Builds libphotonlibcamera.so from source. \
Adds IMX577 / single-plane ABGR8888 support for Qualcomm QCS8275 \
(IQ-8275 EVK) alongside the existing Raspberry Pi YUV420 path."
HOMEPAGE = "https://github.com/munoz0raul/photon-libcamera-gl-driver"

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# 56f3bc3: Fix Adreno GLES compiler abort on thresholding shader (QCS8275) —
# the 1.877.2 shader compiler SIGABRTs on a negative-start int loop, which
# crashed PhotonVision the moment a CSI camera's GPU pipeline started.
SRCREV = "56f3bc38af1da3f2623cc212efb7e140d08e5b1f"
SRC_URI = "git://github.com/munoz0raul/photon-libcamera-gl-driver.git;protocol=https;branch=imx577-qcs8275 \
           https://frcmaven.wpi.edu/artifactory/release/edu/wpi/first/thirdparty/frc2025/opencv/opencv-cpp/4.10.0-3/opencv-cpp-4.10.0-3-linuxarm64.zip;name=opencv_lib;subdir=opencv_lib \
           https://frcmaven.wpi.edu/artifactory/release/edu/wpi/first/thirdparty/frc2025/opencv/opencv-cpp/4.10.0-3/opencv-cpp-4.10.0-3-headers.zip;name=opencv_header;subdir=opencv_header \
           "
SRC_URI[opencv_lib.sha256sum] = "be814284499e70c94c11934f2ab6ce2f90714f76031d3384957f071cec7f30bc"
SRC_URI[opencv_header.sha256sum] = "b5b7c4a73300b71b96569a26041bc59702b6d4974e60725a569e2d50b140d65e"

inherit cmake pkgconfig

DEPENDS = "libcamera virtual/egl virtual/libgles2 virtual/libgbm libdrm pkgconfig-native"

# JNI headers from the PhotonVision Gradle-bootstrapped JDK on this build server.
# This avoids needing an openjdk recipe in the Yocto layer set.
JNI_INCLUDE = "/local/mnt/workspace/build/photonvision-src/tools/jdk-17.0.13+11/include"

# Tell CMake where the pre-fetched OpenCV zips are so FetchContent works
# offline (Yocto sets FETCHCONTENT_FULLY_DISCONNECTED=true during do_configure).
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

# libphotonlibcamera.so links against OpenCV libs that are bundled inside
# the PhotonVision fat JAR (extracted to ~/.wpilib/nativecache at runtime),
# not installed as Yocto packages — suppress the resulting QA warnings.
# buildpaths: debug .so embeds the build-server TMPDIR path (harmless).
INSANE_SKIP:${PN} += "ldflags dev-so file-rdeps"
INSANE_SKIP:${PN}-dbg += "buildpaths"

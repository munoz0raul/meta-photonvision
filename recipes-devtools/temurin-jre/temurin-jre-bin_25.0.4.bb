SUMMARY = "Eclipse Temurin JRE 25 (prebuilt, aarch64) — headless runtime for PhotonVision"
DESCRIPTION = "Prebuilt Eclipse Adoptium Temurin OpenJDK 25 headless JRE for \
aarch64/glibc Linux. PhotonVision v2026.x requires a JDK/JRE 25 runtime, which \
is newer than the OpenJDK recipes currently available in meta-java, so we ship \
the upstream Adoptium binary directly."
HOMEPAGE = "https://adoptium.net"

# OpenJDK is GPL-2.0 with the Classpath exception.
LICENSE = "GPL-2.0-with-classpath-exception"
# TODO(verify): the md5 below is a PLACEHOLDER. After the first fetch, run:
#   md5sum tmp/work/*/temurin-jre-bin/*/jdk-25.0.4+7-jre/legal/java.base/LICENSE
# and paste the real value here. bitbake will also print the correct md5 in the
# "LIC_FILES_CHKSUM does not match" error on first build.
LIC_FILES_CHKSUM = "file://legal/java.base/LICENSE;md5=00000000000000000000000000000000"

# Prebuilt binary release pinned to a specific Temurin build.
PV = "25.0.4+7"
TEMURIN_TAG = "jdk-25.0.4+7"
TEMURIN_FILE = "OpenJDK25U-jre_aarch64_linux_hotspot_25.0.4_7.tar.gz"

SRC_URI = "https://github.com/adoptium/temurin25-binaries/releases/download/${@d.getVar('TEMURIN_TAG').replace('+', '%2B')}/${TEMURIN_FILE};downloadfilename=${TEMURIN_FILE}"
SRC_URI[sha256sum] = "1f2644427000316bc431df3389504551ed7464fe8486bf6b4f1130af9ffc8f55"

S = "${UNPACKDIR}/jdk-25.0.4+7-jre"

# This is a prebuilt aarch64 binary — only valid on 64-bit Arm.
COMPATIBLE_HOST = "aarch64.*-linux"

# We are installing a foreign prebuilt toolchain; disable the usual QA that
# assumes we compiled it ourselves.
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_SYSROOT_STRIP = "1"
EXCLUDE_FROM_SHLIBS = "1"

INSTALL_DIR = "${libdir}/jvm/temurin-25-jre"

do_install() {
    install -d ${D}${INSTALL_DIR}
    cp -a ${S}/. ${D}${INSTALL_DIR}/

    # Expose the runtime on PATH as the system java.
    install -d ${D}${bindir}
    for tool in java keytool rmiregistry; do
        if [ -x ${D}${INSTALL_DIR}/bin/$tool ]; then
            ln -sf ${INSTALL_DIR}/bin/$tool ${D}${bindir}/$tool
        fi
    done
}

# The tarball ships stripped ELF binaries and .so files with an embedded
# RPATH/interpreter that the QA framework flags. These are expected for a
# prebuilt JRE.
INSANE_SKIP:${PN} += "already-stripped ldflags rpaths libdir dev-so staticdev arch"

FILES:${PN} = "${INSTALL_DIR} ${bindir}"

# The JRE bundles its own libs; do not let do_package try to split debug info.
PACKAGES = "${PN}"

RDEPENDS:${PN} += "glibc libstdc++ libgcc"

# Provide a generic alias so recipes can RDEPEND on "java-runtime".
RPROVIDES:${PN} += "java-runtime"

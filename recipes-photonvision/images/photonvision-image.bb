SUMMARY = "PhotonVision demo image for the IQ-8275 EVK"
DESCRIPTION = "QLI multimedia image + PhotonVision, as a single flashable \
target. Optional convenience — you can equally just add \
IMAGE_INSTALL:append = ' packagegroup-photonvision' to an existing image."

require recipes-products/images/qcom-multimedia-image.bb

IMAGE_INSTALL:append = " packagegroup-photonvision"

# PhotonVision + JRE + config need headroom on the rootfs.
IMAGE_ROOTFS_EXTRA_SPACE = "1048576"

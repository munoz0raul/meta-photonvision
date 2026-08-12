FILESEXTRAPATHS:prepend := "${THISDIR}/linux-qcom-next-dts:"

SRC_URI:append:qcom = " \
    file://0001-arm64-dts-qcom-monaco-evk-usb-host-Add-USB-host-DT-o.patch \
"

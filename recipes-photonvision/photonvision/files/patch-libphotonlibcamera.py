#!/usr/bin/env python3
"""
Replace linux/arm64/shared/libphotonlibcamera.so inside a PhotonVision fat JAR
and refresh its MD5 checksum in ResourceInformation.json.

Uses only Python stdlib (zipfile) — no jar(1) required.

Usage:
    patch-libphotonlibcamera.py <photonvision.jar> <new-libphotonlibcamera.so>
"""
import hashlib
import json
import os
import shutil
import sys
import zipfile

JAR_SO_PATH = "linux/arm64/shared/libphotonlibcamera.so"
RI_JSON_PATH = "ResourceInformation.json"


def md5(path):
    return hashlib.md5(open(path, "rb").read()).hexdigest()


def rewrite_zip(src, dst, replacements):
    """Copy src zip to dst, replacing entries listed in replacements dict."""
    with zipfile.ZipFile(src, "r") as zin, \
         zipfile.ZipFile(dst, "w", compression=zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            if item.filename in replacements:
                data = replacements[item.filename]
            else:
                data = zin.read(item.filename)
            # Preserve the original ZipInfo (permissions, timestamp, etc.)
            zout.writestr(item, data)


def main():
    if len(sys.argv) != 3:
        sys.exit(f"Usage: {sys.argv[0]} <photonvision.jar> <new.so>")

    jar_path, so_path = sys.argv[1], sys.argv[2]

    if not os.path.isfile(jar_path):
        sys.exit(f"JAR not found: {jar_path}")
    if not os.path.isfile(so_path):
        sys.exit(f".so not found: {so_path}")

    new_so_data = open(so_path, "rb").read()
    new_hash = hashlib.md5(new_so_data).hexdigest()

    # Read and update ResourceInformation.json from the JAR
    with zipfile.ZipFile(jar_path, "r") as z:
        ri_data = json.loads(z.read(RI_JSON_PATH))

    ri_key = "/" + JAR_SO_PATH
    arm64_hashes = (
        ri_data.get("platforms", {})
        .get("linux", {})
        .get("architectures", {})
        .get("arm64", {})
        .get("fileHashes", {})
    )
    if ri_key not in arm64_hashes:
        print(f"WARNING: {ri_key} not in ResourceInformation.json; adding it")
    arm64_hashes[ri_key] = new_hash
    new_ri_data = json.dumps(ri_data).encode()

    # Rewrite the JAR atomically using a temp file
    tmp_path = jar_path + ".patching"
    rewrite_zip(jar_path, tmp_path, {
        JAR_SO_PATH: new_so_data,
        RI_JSON_PATH: new_ri_data,
    })
    shutil.move(tmp_path, jar_path)

    print(f"Patched {jar_path}:")
    print(f"  {JAR_SO_PATH}  md5={new_hash}")
    print(f"  {RI_JSON_PATH} updated")


if __name__ == "__main__":
    main()

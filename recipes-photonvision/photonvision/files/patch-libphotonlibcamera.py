#!/usr/bin/env python3
"""
Patch a PhotonVision fat JAR for QCS8275 / IMX577 support:
  1. Replace linux/arm64/shared/libphotonlibcamera.so with the Yocto-built
     version (linked against libcamera.so.0.6, adds ABGR8888 single-plane path)
  2. Replace the 6 Java .class files that open the libcamera JNI gate on
     QCS8275 and add IMX577 SensorModel / video modes
  3. Refresh ResourceInformation.json with the new .so MD5

Uses only Python stdlib (zipfile, tarfile) — no jar(1) or javac required.

Usage:
    patch-libphotonlibcamera.py <photonvision.jar> <libphotonlibcamera.so> <java-patches.tar.gz>
"""
import hashlib
import json
import os
import shutil
import sys
import tarfile
import zipfile

JAR_SO_PATH = "linux/arm64/shared/libphotonlibcamera.so"
RI_JSON_PATH = "ResourceInformation.json"


def rewrite_zip(src, dst, replacements):
    """Copy src zip to dst, replacing entries listed in replacements dict."""
    with zipfile.ZipFile(src, "r") as zin, \
         zipfile.ZipFile(dst, "w", compression=zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            zout.writestr(item, replacements.get(item.filename, zin.read(item.filename)))


def main():
    if len(sys.argv) != 4:
        sys.exit(f"Usage: {sys.argv[0]} <photonvision.jar> <new.so> <java-patches.tar.gz>")

    jar_path, so_path, tar_path = sys.argv[1], sys.argv[2], sys.argv[3]

    for p in (jar_path, so_path, tar_path):
        if not os.path.isfile(p):
            sys.exit(f"File not found: {p}")

    replacements = {}

    # 1. New libphotonlibcamera.so
    new_so_data = open(so_path, "rb").read()
    new_hash = hashlib.md5(new_so_data).hexdigest()
    replacements[JAR_SO_PATH] = new_so_data

    # 2. Java class patches from tarball
    with tarfile.open(tar_path, "r:gz") as t:
        for member in t.getmembers():
            if member.isfile():
                replacements[member.name] = t.extractfile(member).read()

    # 3. Update ResourceInformation.json
    with zipfile.ZipFile(jar_path, "r") as z:
        ri_data = json.loads(z.read(RI_JSON_PATH))

    ri_key = "/" + JAR_SO_PATH
    arm64_hashes = (
        ri_data.setdefault("platforms", {})
               .setdefault("linux", {})
               .setdefault("architectures", {})
               .setdefault("arm64", {})
               .setdefault("fileHashes", {})
    )
    arm64_hashes[ri_key] = new_hash
    replacements[RI_JSON_PATH] = json.dumps(ri_data).encode()

    # Rewrite JAR atomically
    tmp_path = jar_path + ".patching"
    rewrite_zip(jar_path, tmp_path, replacements)
    shutil.move(tmp_path, jar_path)

    print(f"Patched {jar_path}:")
    print(f"  {JAR_SO_PATH}  md5={new_hash}")
    print(f"  {RI_JSON_PATH} updated")
    for k in sorted(replacements):
        if k not in (JAR_SO_PATH, RI_JSON_PATH):
            print(f"  {k} patched")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Bind the native host signature check to the certificate used to sign this build."""
import argparse
from pathlib import Path
import re

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("certificate", type=Path, help="DER certificate exported by keytool")
parser.add_argument("--header", type=Path, default=Path("app/src/main/cpp/root/fakeloc_common.h"))
args = parser.parse_args()
certificate = args.certificate.read_bytes()
if not certificate or certificate[0] != 0x30:
    parser.error("Expected a non-empty DER X.509 certificate")
hex_value = certificate.hex()
replacement = 'static const char *kReleaseSign =\n' + '\n'.join(
    '    "' + hex_value[index:index + 96] + '"' for index in range(0, len(hex_value), 96)
) + ';'
header, count = re.subn(
    r'static const char \*kReleaseSign\s*=\s*(?:"[0-9a-fA-F]*"\s*)+;',
    lambda _: replacement,
    args.header.read_text(),
)
if count != 1:
    parser.error(f"Expected exactly one native signing certificate, found {count}")
args.header.write_text(header)
print("Native host signature check configured for the build certificate.")

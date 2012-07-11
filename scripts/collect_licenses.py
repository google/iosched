#!/usr/bin/python
# Copyright 2011 Google, Inc. All Rights Reserved.

# simple script to walk source tree looking for third-party licenses
# dumps resulting html page to stdout


import os, re, mimetypes, sys


# read source directories to scan from command line
SOURCE = sys.argv[1:]

# regex to find /* */ style comment blocks
COMMENT_BLOCK = re.compile(r"(/\*.+?\*/)", re.MULTILINE | re.DOTALL)
# regex used to detect if comment block is a license
COMMENT_LICENSE = re.compile(r"(license)", re.IGNORECASE)
COMMENT_COPYRIGHT = re.compile(r"(copyright)", re.IGNORECASE)

EXCLUDE_TYPES = [
    "application/xml",
    "image/png",
]


# list of known licenses; keys are derived by stripping all whitespace and
# forcing to lowercase to help combine multiple files that have same license.
KNOWN_LICENSES = {}


class License:
    def __init__(self, license_text):
        self.license_text = license_text
        self.filenames = []

    # add filename to the list of files that have the same license text
    def add_file(self, filename):
        if filename not in self.filenames:
            self.filenames.append(filename)


LICENSE_KEY = re.compile(r"[^\w]")

def find_license(license_text):
    # TODO(alice): a lot these licenses are almost identical Apache licenses.
    # Most of them differ in origin/modifications.  Consider combining similar
    # licenses.
    license_key = LICENSE_KEY.sub("", license_text).lower()
    if license_key not in KNOWN_LICENSES:
        KNOWN_LICENSES[license_key] = License(license_text)
    return KNOWN_LICENSES[license_key]



def discover_license(exact_path, filename):
    # when filename ends with LICENSE, assume applies to filename prefixed
    if filename.endswith("LICENSE"):
        with open(exact_path) as file:
            license_text = file.read()
        target_filename = filename[:-len("LICENSE")]
        if target_filename.endswith("."): target_filename = target_filename[:-1]
        find_license(license_text).add_file(target_filename)
        return None

    # try searching for license blocks in raw file
    mimetype = mimetypes.guess_type(filename)
    if mimetype in EXCLUDE_TYPES: return None

    with open(exact_path) as file:
        raw_file = file.read()

    # include comments that have both "license" and "copyright" in the text
    for comment in COMMENT_BLOCK.finditer(raw_file):
        comment = comment.group(1)
        if COMMENT_LICENSE.search(comment) is None: continue
        if COMMENT_COPYRIGHT.search(comment) is None: continue
        find_license(comment).add_file(filename)


for source in SOURCE:
    for root, dirs, files in os.walk(source):
        for name in files:
            discover_license(os.path.join(root, name), name)


print "<html><head><style> body { font-family: sans-serif; } pre { background-color: #eeeeee; padding: 1em; white-space: pre-wrap; } </style></head><body>"

for license in KNOWN_LICENSES.values():

    print "<h3>Notices for files:</h3><ul>"
    filenames = license.filenames
    filenames.sort()
    for filename in filenames:
        print "<li>%s</li>" % (filename)
    print "</ul>"
    print "<pre>%s</pre>" % license.license_text

print "</body></html>"

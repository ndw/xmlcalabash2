#!/usr/bin/env python3
import os

# N.B. This script expects to run in the meerschaum project directory
# with the test-suite project directory located as a sibling.
PASSING = "src/test/resources/passing-tests.txt"
FAILING = "src/test/resources/failing-tests.txt"

with open(PASSING) as file:
    passing = file.readlines()
passing = sorted([x.strip() for x in passing])

failing = []
for dirname, subdirList, fileList in os.walk("../test-suite/test-suite/tests"):
    for fname in sorted(fileList):
        if fname not in passing:
            failing.append(fname)

print("%d tests:" % (len(passing)+len(failing)))
print("%d passing." % len(passing))
with open(PASSING, "w") as file:
    for fname in passing:
        file.write("%s\n" % fname)

print("%d failing." % len(failing))
with open(FAILING, "w") as file:
    for fname in failing:
        file.write("%s\n" % fname)


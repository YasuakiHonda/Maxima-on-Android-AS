#!/bin/sh
# Run native/build.sh inside the pinned build container.
#   native/docker-build.sh [build.sh arguments...]
# e.g. native/docker-build.sh all          (every stage, every ABI)
#      native/docker-build.sh maxima arm64-v8a
set -eu

here=$(cd "$(dirname "$0")" && pwd)
repo=$(dirname "$here")
. "$here/versions.env"

image="moa-native-build:ndk-$NDK_VERSION"
docker build -q -t "$image" \
    --build-arg NDK_VERSION="$NDK_VERSION" \
    --build-arg NDK_SHA1="$NDK_SHA1" \
    "$here" >/dev/null

tty_flag=
[ -t 1 ] && tty_flag=-t

exec docker run --rm $tty_flag \
    -u "$(id -u):$(id -g)" \
    -e HOME=/tmp \
    -e JOBS="${JOBS:-$(nproc)}" \
    -v "$repo:/work" \
    "$image" native/build.sh "$@"

# Build environment for the native binaries (ECL + Maxima, gnuplot, qepcad).
# Built and used by native/docker-build.sh; NDK version comes from versions.env.
FROM ubuntu:24.04

ARG NDK_VERSION
ARG NDK_SHA1

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends \
        autoconf automake build-essential ca-certificates curl file libtool \
        m4 patch pkg-config python3 texinfo unzip xz-utils zip \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL -o /tmp/ndk.zip \
        "https://dl.google.com/android/repository/android-ndk-${NDK_VERSION}-linux.zip" \
    && echo "${NDK_SHA1}  /tmp/ndk.zip" | sha1sum -c - \
    && unzip -q /tmp/ndk.zip -d /opt \
    && mv /opt/android-ndk-${NDK_VERSION} /opt/android-ndk \
    && rm /tmp/ndk.zip

ENV ANDROID_NDK_HOME=/opt/android-ndk
WORKDIR /work

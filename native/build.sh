#!/bin/bash
# Build the native binaries for the app from pinned upstream sources.
# Normally run inside the build container via native/docker-build.sh.
#
#   native/build.sh <stage> [abi...]
#
# Stages (each runs the ones it depends on if their output is missing):
#   fetch     download and verify source archives
#   host-ecl  ECL for the build machine (needed to build and cross compile)
#   ecl       ECL for the target ABI (static libecl.a + target-info.lsp)
#   maxima    Maxima cross compiled with ECL -> libmaxima.so
#   gnuplot   gnuplot (canvas terminal only) -> libgnuplot.so
#   qepcad    saclib + QEPCAD B -> libqepcad.so
#   package   collect the binaries into native/out/<abi>/
#   data      ABI-independent data zips into native/out/
#   docs      the Maxima manual as HTML
#   mathjax   MathJax for the app's web views
#   bundle    native/out/moa-native.zip for the app build / GitHub Release
#   all       every stage above
#   clean     remove native/.build and native/out
#
# ABIs default to $ABIS from versions.env.
set -euo pipefail

NATIVE=$(cd "$(dirname "$0")" && pwd)
. "$NATIVE/versions.env"

BUILD=$NATIVE/.build
DL=$BUILD/dl
OUT=$NATIVE/out
JOBS=${JOBS:-$(nproc)}
NDK=${ANDROID_NDK_HOME:-/opt/android-ndk}
TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64
HOST_PREFIX=$BUILD/host/ecl

log() { printf '\n==> %s\n' "$*" >&2; }
die() { printf 'error: %s\n' "$*" >&2; exit 1; }

target_triple() {
    case $1 in
        arm64-v8a) echo aarch64-linux-android ;;
        x86_64)    echo x86_64-linux-android ;;
        *) die "unsupported ABI: $1" ;;
    esac
}

# Exports CC & co for cross compiling to ABI $1.
use_target_toolchain() {
    local triple
    triple=$(target_triple "$1")
    [ -d "$TOOLCHAIN" ] || die "NDK not found at $NDK"
    export CC="$TOOLCHAIN/bin/$triple$ANDROID_API-clang"
    export CXX="$TOOLCHAIN/bin/$triple$ANDROID_API-clang++"
    export AR=$TOOLCHAIN/bin/llvm-ar
    export RANLIB=$TOOLCHAIN/bin/llvm-ranlib
    export STRIP=$TOOLCHAIN/bin/llvm-strip
    export NM=$TOOLCHAIN/bin/llvm-nm
    export LD=$TOOLCHAIN/bin/ld.lld
    # 16 KB page alignment is required by Google Play for 64-bit native code.
    export LDFLAGS="-Wl,-z,max-page-size=16384"
    export CFLAGS="-O2"
    HOST_TRIPLE=$triple
}

# fetch <name>: download $<NAME>_URL into $DL and verify $<NAME>_SHA256.
fetch_one() {
    local name=$1 url sha file
    url=$(eval echo "\$${name}_URL")
    sha=$(eval echo "\$${name}_SHA256")
    file=$DL/$(basename "$url")
    mkdir -p "$DL"
    if [ ! -f "$file" ]; then
        log "Downloading $(basename "$url")"
        curl -fL --retry 3 -o "$file.part" "$url"
        mv "$file.part" "$file"
    fi
    echo "$sha  $file" | sha256sum -c --quiet - || die "checksum mismatch for $file"
}

stage_fetch() {
    fetch_one ECL
    fetch_one MAXIMA
    fetch_one GNUPLOT
    fetch_one SACLIB
    fetch_one QEPCAD
    fetch_one MATHJAX
}

# unpack <name> <destdir>: fresh copy of the source tree with our patches applied.
unpack() {
    local name=$1 dest=$2 url file lower
    url=$(eval echo "\$${name}_URL")
    file=$DL/$(basename "$url")
    lower=$(echo "$name" | tr '[:upper:]' '[:lower:]')
    rm -rf "$dest"
    mkdir -p "$dest"
    tar -xf "$file" -C "$dest" --strip-components=1
    if [ -d "$NATIVE/patches/$lower" ]; then
        for p in "$NATIVE/patches/$lower"/*.patch; do
            [ -e "$p" ] || continue
            log "Applying $(basename "$p")"
            patch -d "$dest" -p1 --forward < "$p"
        done
    fi
}

stage_host_ecl() {
    [ -x "$HOST_PREFIX/bin/ecl" ] && return 0
    stage_fetch
    log "Building ECL $ECL_VERSION for the build machine"
    local src=$BUILD/host/ecl-src
    unpack ECL "$src"
    (
        cd "$src"
        ./configure --prefix="$HOST_PREFIX" --disable-c99complex --enable-manual=no
        make -j"$JOBS"
        make install
    )
}

stage_ecl() {
    local abi=$1 prefix=$BUILD/$abi/ecl
    [ -f "$prefix/lib/libecl.a" ] && return 0
    stage_host_ecl
    log "Building ECL $ECL_VERSION for $abi"
    local src=$BUILD/$abi/ecl-src
    unpack ECL "$src"
    (
        use_target_toolchain "$abi"
        cd "$src"
        # Same layout on every 64-bit little-endian Android ABI; ECL only ships
        # the arm64 one, so reuse it for x86_64.
        local cross_config=$src/src/util/android-arm64.cross_config
        export ECL_TO_RUN=$HOST_PREFIX/bin/ecl
        ./configure --host="$HOST_TRIPLE" \
            --prefix="$prefix" \
            --with-cross-config="$cross_config" \
            --disable-shared \
            --disable-c99complex \
            --enable-gmp=included \
            --enable-boehm=included \
            --with-dffi=no \
            --enable-manual=no
        make -j"$JOBS"
        make install
    )
    [ -f "$prefix/lib/libecl.a" ] || die "ECL for $abi did not produce libecl.a"
}

stage_maxima() {
    local abi=$1 dir=$BUILD/$abi/maxima-src
    [ -f "$dir/src/binary-ecl/maxima" ] && return 0
    stage_ecl "$abi"
    log "Cross compiling Maxima $MAXIMA_VERSION for $abi"
    unpack MAXIMA "$dir"
    local target_info
    target_info=$(find "$BUILD/$abi/ecl/lib" -name target-info.lsp | head -1)
    [ -n "$target_info" ] || die "target-info.lsp not found for $abi"
    (
        cd "$dir"
        # Configure for the build machine's ECL: this generates
        # src/autoconf-variables.lisp and the binary-ecl directories.
        ./configure --enable-ecl --with-ecl="$HOST_PREFIX/bin/ecl" \
            --prefix=/maxima --disable-build-docs
        make -C src bd
        use_target_toolchain "$abi"
        cd src
        "$HOST_PREFIX/bin/ecl" -norc \
            --load "$NATIVE/lisp/cross-build-maxima.lisp" \
            --eval "(cross-build-maxima \"$target_info\" \"binary-ecl/maxima\")" \
            --eval '(ext:quit 0)'
    )
    [ -f "$dir/src/binary-ecl/maxima" ] || die "Maxima build for $abi produced no binary"
}

stage_gnuplot() {
    local abi=$1 dir=$BUILD/$abi/gnuplot-src
    [ -f "$dir/src/gnuplot" ] && return 0
    stage_fetch
    log "Building gnuplot $GNUPLOT_VERSION for $abi"
    unpack GNUPLOT "$dir"
    (
        use_target_toolchain "$abi"
        # gnuplot links with the C++ driver; the device has no libc++_shared.so.
        export LDFLAGS="$LDFLAGS -static-libstdc++"
        cd "$dir"
        # Only the canvas (HTML5) terminal is used by the app. Nothing is
        # installed: the app passes the JavaScript directory in $GNUPLOT_JS_DIR.
        ./configure --host="$HOST_TRIPLE" \
            --prefix=/usr \
            --without-x --without-cairo --without-qt --disable-wxwidgets \
            --without-lua --without-gd --without-latex --without-libcerf \
            --without-readline --without-tektronix --disable-plugins
        make -j"$JOBS" -C src gnuplot
    )
    [ -f "$dir/src/gnuplot" ] || die "gnuplot build for $abi produced no binary"
}

stage_qepcad() {
    local abi=$1 dir=$BUILD/$abi
    [ -f "$dir/qesource/source/qepcad" ] && return 0
    stage_fetch
    log "Building saclib $SACLIB_VERSION and QEPCAD B $QEPCAD_VERSION for $abi"
    unpack SACLIB "$dir/saclib"
    unpack QEPCAD "$dir/qesource"
    (
        use_target_toolchain "$abi"
        # Both build systems call plain ar/ranlib in places.
        local shim=$dir/toolshim
        mkdir -p "$shim"
        ln -sf "$AR" "$shim/ar"
        ln -sf "$RANLIB" "$shim/ranlib"
        export PATH=$shim:$PATH
        export saclib=$dir/saclib qe=$dir/qesource

        (cd "$saclib/sysdep/android" && bash ./install)
        (cd "$saclib" && bash bin/mkproto && bash bin/mkmake)
        # Both were written for platforms where char is signed (it is not on
        # arm64), e.g. QEPCAD stores EOF in char arrays.
        make -C "$saclib/lib/objo" CC="$CC" SACFLAG="-O2 -fsigned-char" EXTENSION=o \
            AR="$AR" RANLIB="$RANLIB"

        # QEPCAD compiles its .c files as C++; C++17 dropped `register`.
        local qflags="-O2 -std=gnu++14 -fsigned-char -fPIC -w -D__CORRECT_ISO_CPP_STRINGS_H_PROTO"
        make -C "$qe/extensions" opt CCo="$CXX" FLAGSo="$qflags" SUBMAKEFLAGS=-j1
        make -C "$qe/source" opt CCo="$CXX" \
            FLAGSo="$qflags $LDFLAGS -static-libstdc++" \
            EXTLIBS="$qe/extensions/sfext/sfexto.a $qe/extensions/lift2D/lift2Do.a \
$qe/extensions/newadj/newadjo.a $qe/extensions/adj2d/adj2do.a \
$qe/extensions/rend/rendo.a $saclib/lib/saclibo.a"
    )
    [ -f "$dir/qesource/source/qepcad" ] || die "QEPCAD build for $abi produced no binary"
}

stage_package() {
    local abi=$1
    stage_maxima "$abi"
    stage_gnuplot "$abi"
    stage_qepcad "$abi"
    log "Packaging $abi"
    use_target_toolchain "$abi"
    mkdir -p "$OUT/$abi"
    "$STRIP" -o "$OUT/$abi/libmaxima.so" "$BUILD/$abi/maxima-src/src/binary-ecl/maxima"
    "$STRIP" -o "$OUT/$abi/libgnuplot.so" "$BUILD/$abi/gnuplot-src/src/gnuplot"
    "$STRIP" -o "$OUT/$abi/libqepcad.so" "$BUILD/$abi/qesource/source/qepcad"
    chmod 755 "$OUT/$abi"/*.so
}

# ABI-independent runtime data, unpacked by the app's installer:
#   maxima-<version>.zip  Maxima's share/ tree (MAXIMA_PREFIX)
#   additions.zip         gnuplot canvas JS, QEPCAD data files and qepmax
stage_package_data() {
    local abi=${1:-${ABIS%% *}}
    stage_maxima "$abi"
    stage_gnuplot "$abi"
    stage_qepcad "$abi"
    log "Packaging data files"
    local stage=$BUILD/data
    rm -rf "$stage"
    mkdir -p "$stage/maxima-$MAXIMA_VERSION" "$OUT"
    local msrc=$BUILD/$abi/maxima-src
    # lisp-utils/defsystem.lisp is loaded at runtime by init.lisp and draw.
    cp -a "$msrc/share" "$msrc/demo" "$msrc/lisp-utils" "$stage/maxima-$MAXIMA_VERSION/"
    (cd "$stage" && rm -f "$OUT/maxima-$MAXIMA_VERSION.zip" &&
        zip -qr -X "$OUT/maxima-$MAXIMA_VERSION.zip" "maxima-$MAXIMA_VERSION")

    local add=$stage/additions
    mkdir -p "$add"
    cp -a "$NATIVE/additions/." "$add/"
    local gdir=$add/gnuplot/js
    mkdir -p "$gdir"
    cp -a "$BUILD/$abi/gnuplot-src/term/js/." "$gdir/"
    rm -f "$gdir"/Makefile*
    mkdir -p "$add/qepcad/bin"
    cp "$BUILD/$abi/qesource/default.qepcadrc" "$add/qepcad/"
    cp "$BUILD/$abi/qesource/bin/qepcad.help" "$add/qepcad/bin/"
    (cd "$stage" && rm -f "$OUT/additions.zip" && zip -qr -X "$OUT/additions.zip" additions)
}

# The Maxima manual as HTML, one directory per language with index.html as
# the entry page. Built from the same Maxima sources with the build
# machine's ECL (makeinfo plus Maxima's category generator).
stage_docs() {
    local dir=$BUILD/docs/maxima-src dest=$BUILD/docs/maxima-doc
    [ -f "$dest/en/index.html" ] && return 0
    stage_host_ecl
    log "Building the Maxima $MAXIMA_VERSION manual (en $MANUAL_LANGS)"
    unpack MAXIMA "$dir"
    (
        export PATH=$HOST_PREFIX/bin:$PATH
        cd "$dir"
        local langopts=""
        for l in $MANUAL_LANGS; do langopts="$langopts --enable-lang-$l"; done
        ./configure --enable-ecl --with-ecl="$HOST_PREFIX/bin/ecl" \
            --prefix=/maxima --enable-build-docs $langopts
        # Formulas: the app's local MathJax 2 instead of MathJax 3 from a CDN.
        sed -i "s|HTML_MATH=mathjax\"|HTML_MATH=mathjax -c MATHJAX_SCRIPT=file:///android_asset/MathJax-$MATHJAX_VERSION/MathJax.js?config=TeX-AMS-MML_SVG\"|" \
            doc/info/build_html.sh
        grep -q "MATHJAX_SCRIPT=file:" doc/info/build_html.sh || die "could not set MATHJAX_SCRIPT"
        make -C doc/info index.html
        for l in $MANUAL_LANGS; do make -C "doc/info/$l" index.html; done
    )
    rm -rf "$dest"
    mkdir -p "$dest/en"
    cp "$dir"/doc/info/*.html "$dir/doc/info/manual.css" "$dest/en/"
    cp -r "$dir/doc/info/figures" "$dest/"
    for l in $MANUAL_LANGS; do
        mkdir -p "$dest/$l"
        cp "$dir/doc/info/$l"/*.html "$dir/doc/info/manual.css" "$dest/$l/"
    done
    # Only the multi-page manual is used.
    rm -f "$dest"/*/maxima_singlepage.html
    # The figures are shared by all languages: maxima-doc/figures.
    sed -i -E 's#src="(\./)?figures/#src="../figures/#g' "$dest"/*/*.html
}

stage_mathjax() {
    local dest=$BUILD/mathjax/MathJax-$MATHJAX_VERSION
    [ -f "$dest/MathJax.js" ] && return 0
    stage_fetch
    log "Unpacking MathJax $MATHJAX_VERSION"
    local src=$BUILD/mathjax/src
    unpack MATHJAX "$src"
    rm -rf "$dest"
    mkdir -p "$dest"
    # The app's pages load config TeX-AMS-MML_SVG with the TeX (manual) and
    # Neo-Euler (console) SVG fonts. Leave out the web fonts in fonts/
    # (~30000 files), the HTML-CSS output and the other configs and fonts.
    cp -a "$src/MathJax.js" "$src/LICENSE" "$src/extensions" \
        "$src/jax" "$src/localization" "$dest/"
    mkdir -p "$dest/config"
    cp -a "$src/config/TeX-AMS-MML_SVG.js" "$src/config/local" "$dest/config/"
    rm -rf "$dest/jax/output/HTML-CSS"
    find "$dest/jax/output/SVG/fonts" -mindepth 1 -maxdepth 1 \
        ! -name TeX ! -name Neo-Euler -exec rm -rf {} +
}

# Single archive consumed by the app build (app/build.gradle) and published
# as the GitHub Release asset:
#   jniLibs/<abi>/lib{maxima,gnuplot,qepcad}.so
#   assets/maxima-<version>.zip, assets/additions.zip
#   assets/maxima-doc/<lang>/  the manual (HTML)
#   assets/MathJax-<version>/
stage_bundle() {
    log "Creating $OUT/moa-native.zip"
    local stage=$BUILD/bundle
    rm -rf "$stage"
    mkdir -p "$stage/jniLibs" "$stage/assets"
    for abi in "$@"; do
        [ -f "$OUT/$abi/libmaxima.so" ] || die "missing binaries for $abi; run the package stage"
        cp -a "$OUT/$abi" "$stage/jniLibs/"
    done
    cp "$OUT/maxima-$MAXIMA_VERSION.zip" "$OUT/additions.zip" "$stage/assets/"
    stage_docs
    stage_mathjax
    cp -a "$BUILD/docs/maxima-doc" "$BUILD/mathjax/MathJax-$MATHJAX_VERSION" "$stage/assets/"
    (cd "$stage" && rm -f "$OUT/moa-native.zip" && zip -qr -X "$OUT/moa-native.zip" jniLibs assets)
    (cd "$OUT" && sha256sum moa-native.zip | tee moa-native.zip.sha256)
}

run_stage() {
    local stage=$1; shift
    case $stage in
        fetch)    stage_fetch ;;
        host-ecl) stage_host_ecl ;;
        ecl|maxima|gnuplot|qepcad|package)
            for abi in "$@"; do "stage_${stage//-/_}" "$abi"; done ;;
        data)     stage_package_data "$@" ;;
        docs)     stage_docs ;;
        mathjax)  stage_mathjax ;;
        bundle)   stage_bundle "$@" ;;
        all)
            for abi in "$@"; do stage_package "$abi"; done
            stage_package_data "$@"
            stage_bundle "$@" ;;
        clean)    rm -rf "$BUILD" "$OUT" ;;
        *) die "unknown stage: $stage" ;;
    esac
}

[ $# -ge 1 ] || { sed -n '2,24p' "$0"; exit 1; }
stage=$1; shift
[ $# -ge 1 ] || set -- $ABIS
run_stage "$stage" "$@"

# Native binaries and bundled web content

The app runs three native programs, shipped as `lib*.so` files so that the
package installer extracts them into `nativeLibraryDir` (the only place an app
targeting API 29+ may execute files from):

| File            | Program                                             |
|-----------------|-----------------------------------------------------|
| `libmaxima.so`  | Maxima, compiled with ECL and linked statically     |
| `libgnuplot.so` | gnuplot, canvas (HTML5) terminal only               |
| `libqepcad.so`  | QEPCAD B with saclib (used by Maxima's `qepmax`)    |

plus two data archives the app's installer unpacks into its files directory:
`maxima-<version>.zip` (Maxima's `share/` tree) and `additions.zip` (gnuplot's
canvas JavaScript, QEPCAD data files and the `qepmax` package), and two asset
trees the app's web views load directly: the Maxima manual (`maxima-doc/`) and
MathJax.

Everything is built here from pinned upstream sources for `arm64-v8a` and
`x86_64`; none of it is committed to the repository. Nothing in this directory
is needed to build the app itself.

## Layout

```
versions.env           pinned versions, URLs and SHA-256 of all sources; NDK version
Dockerfile             build environment (Ubuntu + NDK)
docker-build.sh        runs build.sh in that environment
build.sh               the build, in stages
lisp/                  driver that cross compiles Maxima with ECL
patches/<project>/     our changes to upstream sources, applied in order
additions/             files shipped in additions.zip that come from neither
                       upstream archive (qepmax, GPL v2)
prebuilt.properties    which published bundle the app build downloads
```

Build output goes to `.build/` (work tree, cached between runs) and `out/`.

## Building

Requires Docker.

```sh
native/docker-build.sh all            # everything, all ABIs -> native/out/
native/docker-build.sh maxima x86_64  # one stage for one ABI
native/docker-build.sh clean
```

`all` produces `native/out/moa-native.zip`:

```
jniLibs/<abi>/libmaxima.so, libgnuplot.so, libqepcad.so
assets/maxima-<version>.zip
assets/additions.zip
assets/maxima-doc/<lang>/index.html   manual: en de es ja pt pt_BR
assets/maxima-doc/figures/
assets/MathJax-<version>/
```

## Using a build in the app

The app build (`app/build.gradle`, `prepare<Variant>NativeBundle`) unpacks
`moa-native.zip` into generated `jniLibs`/`assets` source directories.

* By default it downloads the GitHub Release named in `prebuilt.properties`,
  verifies its SHA-256 and caches it in `~/.gradle/caches/moa-native/`.
* To use a local build: `./gradlew assembleDebug -Pmoa.nativeDir=native/out`
  (or put `moa.nativeDir=native/out` in `~/.gradle/gradle.properties`).

## Publishing a release

1. Push a tag `native-maxima-<maxima version>-<build>`, e.g.
   `native-maxima-5.50.0-1`; increase the build number when anything else
   changes (another component, a patch, the NDK). The `native` workflow builds
   everything and creates a release with `moa-native.zip`, its checksum and the
   upstream source archives.
2. Put the tag and the SHA-256 from the release notes into `prebuilt.properties`
   and commit.

## How the pieces are built

**ECL** is built twice: for the build machine, and statically for the target
(bundled GMP and Boehm GC, no shared libraries). The target build writes
`target-info.lsp`, which lets the host ECL cross compile Lisp code (ECL ≥ 26).

**Maxima** is configured for the host ECL, then `lisp/cross-build-maxima.lisp`
lets Maxima's own `maxima.system` drive the compilation in dependency order,
but compiles every file for the target (`compile-file :target`) and loads the
source into the host so that macros are available to later files. The objects
are linked into one static program with `c:build-program`. ECL's `sockets`
modules are linked in because `server.lisp` requires them at load time.

**gnuplot** is a plain autotools cross build with everything but the canvas
terminal disabled. The C++ runtime is linked statically.

**The manual** is built from the same Maxima sources with Maxima's own
`build_html.sh` (makeinfo, plus the category pages generated with the host
ECL), one directory per language. Its formulas use the app's MathJax instead
of MathJax 3 from a CDN, and the figures are shared by all languages.

**MathJax** 2.7.9 is the release archive, reduced to what the app's pages use
(config `TeX-AMS-MML_SVG`, SVG output with the TeX and Neo-Euler fonts).

**saclib/QEPCAD** use their own makefiles; `build.sh` passes the NDK
compilers and archiver. Both are compiled with `-fsigned-char` because they
assume signed `char` (arm64's is unsigned).

All binaries depend only on Android system libraries (`libc`, `libm`, `libdl`,
`libz`) and are linked with 16 KB page alignment.

## Patches

* `gnuplot/0001-android-no-setuid.patch`: gnuplot calls `setuid()` at start;
  Android's seccomp filter kills apps for that (Android 8+). Same fix as Termux.
* `gnuplot/0002-canvas-jsdir-from-env.patch`: the canvas terminal takes its
  JavaScript directory from `$GNUPLOT_JS_DIR`, which the app sets; otherwise it
  would be a path compiled into the binary.
* `saclib/0001-android-sysdep.patch`: adds an Android system-dependent
  configuration. Its `GC()` saves registers with `__builtin_unwind_init()`;
  bionic mangles registers stored by `setjmp()`, and the dummy-register trick of
  the Linux port fails with current compilers (it crashes under GC pressure
  even on Linux/x86_64).
* `qepcad/0001-android-no-readline.patch`: the NDK has no readline; QEPCAD only
  uses it for terminal input, which the app never provides.

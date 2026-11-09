# Dependency notices

| Dependency | Version | Licence / source |
|---|---|---|
| OpenJFX controls, graphics, base and swing | 21.0.5 | GPL v2 with Classpath Exception; https://openjdk.org/legal/gplv2+ce.html and https://openjfx.io/ |
| JOCL | 2.0.5 | MIT; https://www.jocl.org/ and https://github.com/gpu/JOCL |
| Gson | 2.11.0 | Apache License 2.0; https://github.com/google/gson |
| JUnit Jupiter (tests only) | 5.11.3 | Eclipse Public License 2.0; https://junit.org/junit5/ |
| JavaFX Maven plugin (build only) | 0.0.8 | BSD 3-Clause; https://github.com/openjfx/javafx-maven-plugin |
| Maven compiler / Surefire plugins (build only) | 3.13.0 / 3.5.2 | Apache License 2.0; https://maven.apache.org/ |

A JDK installation is required but is not bundled. Its licence depends on the selected vendor. No third-party fonts are bundled: system fonts are used. Maven resolves transitive dependencies. Preserve upstream notices when redistributing bundled runtimes or dependencies. This source ZIP contains no bundled runtime or dependency JARs.

The emblem was created specifically for this application using image generation. It is decorative artwork, not a mathematical plot. The viewport imagery is calculated by the Mandelbrot engine.

Adapted legacy numerical, gradient and scan-order techniques originate in George Miller’s supplied 2025 MIT project. See LEGACY-LICENSE.txt and docs/LEGACY-ALGORITHMS.md. No vendor SDK or encoder binaries from that archive are redistributed.

OpenCL system drivers are not bundled. Optional compute uses the installed vendor runtime. The new observatory artwork was generated specifically for this release.


## FFmpeg integration (0.9)

The project now declares Bytedeco FFmpeg 7.1-1.5.11 and JavaCPP 1.5.11 through Maven, including the selected platform's GPL native artifact with libx264. The Java wrappers are provided under their upstream Apache-2.0/GPLv2-with-Classpath-Exception terms. The validated FFmpeg executable reports GPL version 3 or later; its components carry their own upstream notices. Dependency binaries are resolved by Maven rather than copied into this source ZIP. See docs/FFMPEG.md and https://ffmpeg.org/legal.html before distributing native runtimes/installers. Java ImageIO supplies GIF encoding.

## Wolfgang Beyer / Wikimedia Commons zoom images

`src/main/resources/com/jenga/aeternalis/assets/wiki/00.jpg` through `14.jpg`: © Wolfgang Beyer, created with Ultra Fractal 3. Selected license: Creative Commons Attribution–ShareAlike 3.0 Unported, https://creativecommons.org/licenses/by-sa/3.0/. Author: https://commons.wikimedia.org/wiki/User:Wolfgangbeyer. These are Wikimedia-generated 120-pixel thumbnails, otherwise unaltered; resized copies retain that license. The MIT source-code license does not replace their image license. No endorsement is implied. Per-image titles, source URLs and published coordinates are in `docs/WIKIMEDIA-CREDITS.md` and the packaged `zoom-gallery.xml`. In-app credits are available from every thumbnail and Help.

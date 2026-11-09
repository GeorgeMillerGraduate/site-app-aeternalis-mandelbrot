# MASTER DEVELOPMENT PROMPT
# AETERNALIS FRACTAL STUDIO
## A High-Performance JavaFX Fractal Exploration and Rendering Application

### 1. Project vision

Act as a senior Java software architect, numerical computing specialist, fractal mathematics researcher, performance engineer, and professional desktop UI designer.

Your task is to design and implement **Aeternalis Fractal Studio**, a sophisticated, professional-grade fractal exploration and rendering application written primarily in Java 21 using JavaFX.

The application should be comparable in ambition and feature depth to software such as Ultra Fractal, Fractint, and other advanced fractal exploration tools.

However, Aeternalis Fractal Studio must have its **own original visual identity, codebase, architecture, interaction design, and graphical assets**.

The application will form a flagship project for my programming portfolio and website, *Code and Curiosity*.

It must demonstrate expertise in:

- Java and JavaFX application development.
- Numerical algorithms and arbitrary-precision arithmetic.
- Complex numbers and fractal mathematics.
- High-performance multithreaded computation.
- Adaptive precision and deep-zoom techniques.
- Software architecture and extensibility.
- Scientific visualisation.
- Image processing and colour theory.
- Custom desktop interface development.
- Mathematical art and animation.

The final product should feel like a complete scientific and artistic workstation, rather than a programming demonstration.

---

### 2. Branding and visual identity

**Official application name: Aeternalis Fractal Studio**

Suggested tagline:

*An Infinite Canvas for Mathematical Discovery.*

The interface should combine the visual sophistication of Victorian scientific instruments with the functionality of modern numerical software.

The design language should incorporate:

- Antique parchment and finely textured ivory surfaces.
- Engraved brass and bronze borders.
- Ornamental mathematical illustrations.
- Elegant serif typography for headings.
- Highly legible typography for controls and numerical data.
- Dark mahogany, bronze, gold, and warm parchment colours.
- Subtle metallic highlights and shadows.
- Carefully designed icons inspired by historical scientific equipment.
- Beautiful fractal imagery contrasting with the restrained interface.

The application should resemble an advanced scientific instrument housed inside an elegant nineteenth-century mathematical observatory.

It must not sacrifice usability for ornamentation.

The fractal viewport must remain the dominant element of the interface.

#### Custom graphical assets

Create a dedicated graphical asset system containing:

1. Main application icon and branding.
2. Window header and decorative title artwork.
3. Left and right panel backgrounds.
4. Ornamental panel borders and separators.
5. Toolbar icons.
6. Hover and pressed button states.
7. Navigation controls.
8. Precision-mode indicators.
9. Rendering-progress indicators.
10. Palette preview artwork.
11. Tab decorations.
12. Status-bar decorations.
13. Loading and rendering graphics.
14. Splash screen.
15. About-window artwork.

Use custom PNG assets with transparency where appropriate.

The images must be consistent in appearance and dimensions.

Avoid regenerating an entire background for every hover state when a reusable transparent overlay or JavaFX CSS effect would provide a more reliable result.

The visual system should support different screen resolutions and Windows display scaling.

Keep graphical assets separate from functional controls. Buttons, text fields, sliders, tabs, and other interactive elements should remain real JavaFX controls wherever practical.

---

### 3. Technology stack

Use the following technologies as the foundation.

| Component | Technology |
|---|---|
| Programming language | Java 21 |
| GUI framework | JavaFX |
| Build system | Maven |
| IDE | Apache NetBeans |
| Standard precision | Java `double` |
| Extended precision | Custom double-double arithmetic |
| Arbitrary precision | `BigDecimal` or a justified specialist library |
| Parallel rendering | Java concurrency utilities |
| Pixel rendering | JavaFX `PixelBuffer` or `PixelWriter` |
| Image export | PNG and other suitable formats |
| Project persistence | JSON-based project files |
| Unit testing | JUnit 5 |
| Profiling | Java Flight Recorder and JMH where appropriate |

Use a modular, maintainable architecture.

Avoid putting numerical algorithms directly inside JavaFX controllers.

The rendering engine must be independently testable without launching the GUI.

---

### 4. Main application interface

The application should launch into a professional, maximised JavaFX window.

The layout should consist of:

**Top menu bar**

File, Edit, View, Fractal, Render, Layers, Formula, Animation, Tools, Window, Help.

**Primary toolbar**

New, Open, Save, Undo, Redo, Render, Cancel, Zoom In, Zoom Out, Reset View, Export, Full Screen.

**Left sidebar**

Fractal formulas, iteration settings, precision configuration, rendering algorithms, performance options, and colour settings.

**Central viewport**

A large interactive fractal canvas.

**Right sidebar**

Coordinates, zoom information, rendering statistics, navigation controls, viewport overview, and selected-object properties.

**Bottom status bar**

Rendering state, active precision, current algorithm, iteration count, zoom depth, elapsed time, throughput, and mouse coordinates.

The interface should support collapsible and resizable panels.

Users should be able to maximise the fractal viewport by hiding secondary controls.

The central rendering canvas must resize dynamically while maintaining the correct complex-plane aspect ratio.

---

### 5. Supported fractal formulas

Begin with a mathematically correct Mandelbrot implementation.

Then extend the formula system to support:

- Mandelbrot set.
- Julia sets.
- Multibrot sets.
- Burning Ship fractal.
- Tricorn fractal.
- Newton fractals.
- Phoenix fractals.
- Custom polynomial formulas.
- Parameterised complex iterative functions.

Design a formula interface so new fractals can be added without rewriting the rendering engine.

For example, the system should support formula-specific initial conditions, iteration rules, bailout conditions, derivatives, and colouring metadata.

Not every optimisation is applicable to every fractal. The engine must expose formula capabilities and select compatible rendering methods accordingly.

---

### 6. Precision management — a central feature

Aeternalis Fractal Studio must automatically determine the numerical precision required for the current viewport.

Provide these modes:

1. Automatic precision.
2. Standard double precision.
3. Double-double precision.
4. Optional quad-double precision.
5. Arbitrary precision.

#### Automatic precision selection

The application should evaluate:

- Current viewport width in the complex plane.
- Pixel spacing.
- Magnification.
- Coordinate magnitude.
- Representable floating-point increments.
- Numerical error estimates.
- Selected rendering algorithm.
- Required iteration depth.

The application must not rely exclusively on a fixed zoom threshold.

For example, double precision may become inadequate when adjacent pixels map to indistinguishable floating-point coordinates.

The precision controller should identify this condition before visible corruption occurs.

When additional precision is required, the application should automatically select an appropriate numerical representation or rendering technique.

Display the current mode in the interface:

**Precision: Double**

**Precision: Double-Double**

**Precision: Arbitrary — 256 bits equivalent**

Show an explanation of why the mode was selected.

#### Double-double arithmetic

Implement or integrate a carefully tested double-double arithmetic system.

Use error-free transformations such as TwoSum and TwoProd, with appropriate handling of IEEE-754 behaviour.

Provide addition, subtraction, multiplication, division, comparison, and complex arithmetic.

Test the implementation against high-precision reference calculations.

#### Arbitrary precision

Support extreme zooming through arbitrary-precision reference coordinates and orbit calculations.

Allow users to configure precision manually when desired.

Precision should be chosen according to numerical requirements, with a configurable safety margin.

Avoid calculating every pixel with expensive arbitrary-precision arithmetic when a faster mathematically justified method is available.

---

### 7. High-performance fractal rendering

The renderer must support multiple algorithms and select between them intelligently.

#### Basic escape-time rendering

Implement a correct baseline renderer.

For Mandelbrot:

`z(n+1) = z(n)^2 + c`

Use this implementation as a correctness reference for advanced algorithms.

#### Cardioid and period-2 bulb tests

Skip unnecessary iterations for points provably inside the main cardioid or period-2 bulb.

Apply these tests only to compatible formulas.

#### Periodicity checking

Detect repeating orbits to accelerate the classification of interior points.

Use a method that avoids incorrectly classifying exterior points due to floating-point tolerances.

#### Smooth escape-time colouring

Use continuous escape values to avoid harsh colour banding.

#### Distance estimation

Support derivative-based distance estimation for compatible fractal formulas.

Use it for optional colouring, shading, and adaptive sampling.

#### Perturbation theory

Implement reference-orbit perturbation for deep Mandelbrot zooms.

Calculate a high-precision reference orbit and evaluate nearby pixel orbits using lower-precision deltas when numerically safe.

Detect perturbation glitches and rebase or recalculate affected pixels as required.

Do not assume perturbation remains valid indefinitely.

#### Series approximation

Implement series approximation where mathematically appropriate to accelerate deep-zoom rendering.

Include error bounds or validation procedures that determine when the approximation can safely be used.

#### Reference orbit caching

Cache reference orbits and related metadata.

Reuse compatible calculations when panning, zooming, or recolouring.

Use a memory-bounded cache with clear invalidation rules.

#### Adaptive iteration counts

Estimate useful iteration limits based on the viewport and observed orbit behaviour.

Allow users to override automatic values.

Do not claim that a particular iteration count guarantees complete accuracy.

#### Parallel processing

Divide the viewport into rendering tiles.

Use a bounded worker pool and prioritise tiles near the centre or cursor.

Support cancellation, progressive rendering, and partial image updates.

Ensure that obsolete rendering tasks cannot overwrite newer viewport results.

---

### 8. Rendering pipeline and responsiveness

Implement an asynchronous rendering architecture.

The JavaFX application thread must never perform expensive fractal calculations.

Use a pipeline similar to:

**Viewport Request → Precision Analysis → Algorithm Selection → Tile Scheduling → Numerical Computation → Colour Mapping → Display Update**

Each render should have a unique generation identifier.

When the user changes coordinates or zoom level, cancel or invalidate obsolete work.

Use progressive rendering:

- Immediate low-resolution preview.
- Refined full-resolution render.
- Optional anti-aliasing pass.
- Final colour and image processing.

Avoid creating unnecessary JavaFX objects for every pixel.

Use efficient primitive arrays and pixel buffers.

Measure actual performance rather than assuming a particular optimisation is faster.

---

### 9. Interactive navigation

The fractal viewport should support:

- Mouse-wheel zooming.
- Zooming towards the mouse cursor.
- Click-and-drag panning.
- Rectangle-selection zoom.
- Double-click zoom.
- Keyboard navigation.
- Zoom history.
- Undo and redo of viewport changes.
- Reset to the full fractal.
- Bookmarked locations.
- Direct coordinate entry.
- Direct magnification entry.
- Full-screen exploration.

Coordinate transformations must preserve numerical precision.

At extreme zoom levels, avoid converting high-precision coordinates to `double` unnecessarily.

The interface should display coordinates with enough digits to reproduce the current viewport accurately.

---

### 10. Colour palette editor

Implement a professional colour palette editor.

Support:

- Multiple colour stops.
- Gradient interpolation.
- Smooth colouring.
- Logarithmic and cyclic colouring.
- Palette offset.
- Palette scaling.
- Colour cycling.
- Histogram-based colouring.
- Interior and exterior colours.
- Palette import and export.
- Saved palette presets.

Provide an interactive gradient editor.

Changing colours should generally reuse stored iteration data instead of recomputing the fractal.

Separate numerical iteration results from colour mapping.

This is essential for interactive palette editing.

---

### 11. Layers and compositing

Implement an extensible layer system inspired by professional graphics software.

Each layer should support:

- Fractal formula or image content.
- Visibility.
- Opacity.
- Blend mode.
- Colour palette.
- Transformations.
- Independent parameters.
- Layer ordering.
- Duplication.
- Deletion.
- Naming and grouping.

Support blend modes such as Normal, Multiply, Screen, Overlay, and Add.

Provide a layer manager with visibility controls and drag-and-drop reordering.

Layer compositing should be separate from fractal calculation.

Start with a simple working layer architecture before adding advanced transformations.

---

### 12. Fractal animation

Provide an animation workspace.

Support keyframes for:

- Zoom.
- Viewport centre.
- Rotation.
- Iteration limits.
- Colour palette offsets.
- Layer opacity.
- Formula parameters.

Provide a timeline with playback controls.

Allow users to create smooth deep-zoom animations.

At extreme magnifications, coordinate interpolation must retain sufficient precision.

Provide frame-by-frame rendering with resumable output.

Support image sequences as the primary lossless animation output.

Optional video export may use an external encoder such as FFmpeg.

---

### 13. Image export and render queue

Users should be able to export high-resolution fractal artwork.

Support:

- PNG.
- JPEG.
- TIFF where an appropriate library is available.
- High-bit-depth image formats where supported.
- Transparent backgrounds for applicable compositions.
- Custom dimensions.
- Aspect ratio locking.
- Anti-aliasing.
- Export presets.
- Batch rendering.

Implement a render queue with:

- Pending jobs.
- Active jobs.
- Completed jobs.
- Failed jobs.
- Progress percentages.
- Estimated remaining time.
- Cancellation.
- Retry functionality.

Export rendering must not depend on the visible viewport resolution.

Large images should use tiled rendering to avoid excessive memory allocation.

---

### 14. Statistics and diagnostics

Include a dedicated performance and diagnostics panel.

Display:

- Active formula.
- Precision mode.
- Numerical precision.
- Selected rendering algorithm.
- Current magnification.
- Iteration limit.
- Pixels calculated.
- Tiles completed.
- Worker-thread count.
- Elapsed rendering time.
- Iterations per second.
- Memory consumption.
- Cache hit rate.
- Perturbation glitch count.
- Reference orbit calculation time.

Provide optional diagnostic visualisations showing:

- Tile boundaries.
- Iteration density.
- Rendering time per tile.
- Precision changes.
- Perturbation fallback regions.

These should help demonstrate the software's underlying algorithms.

---

### 15. Project files and persistence

Create an original project format, such as:

`.aeternalis`

The format should store:

- Fractal formula.
- High-precision coordinates.
- Zoom and rotation.
- Iteration settings.
- Precision settings.
- Rendering settings.
- Colour palettes.
- Layers.
- Animation keyframes.
- Export configuration.
- Relevant application version information.

Use a versioned JSON schema or a ZIP-based container containing JSON and associated assets.

Ensure project files can be loaded reliably across application versions.

Provide automatic recovery files and safeguards against data loss.

---

### 16. Software architecture

Use clearly separated packages.

Suggested structure:

```text
com.jenga.aeternalis
├── Main.java
├── app/
├── ui/
│   ├── viewport/
│   ├── panels/
│   ├── controls/
│   └── dialogs/
├── fractal/
│   ├── formulas/
│   └── iteration/
├── math/
│   ├── complex/
│   └── precision/
├── rendering/
│   ├── algorithms/
│   ├── perturbation/
│   ├── scheduling/
│   └── caching/
├── colouring/
├── layers/
├── animation/
├── export/
├── persistence/
└── diagnostics/
```

The architecture should distinguish mathematical models, rendering services, application state, and UI components.

Use interfaces where they enable meaningful extensibility.

Avoid unnecessary abstraction and oversized classes.

---

### 17. Testing and numerical correctness

Implement comprehensive automated tests.

Test:

- Complex arithmetic.
- Double-double arithmetic.
- Arbitrary-precision calculations.
- Coordinate transformations.
- Fractal iteration results.
- Cardioid and bulb classification.
- Periodicity detection.
- Perturbation accuracy.
- Series approximation accuracy.
- Palette interpolation.
- Project serialization.
- Rendering cancellation.
- Layer compositing.

Compare advanced renderers against trusted high-precision reference calculations.

Create reproducible test coordinates at progressively deeper zoom levels.

Verify that automatic precision selection prevents coordinate collapse and visible numerical artefacts.

Performance benchmarks should be reproducible and clearly distinguished from correctness tests.

---

### 18. User experience and accessibility

The interface should feel polished and predictable.

Provide:

- Tooltips.
- Keyboard shortcuts.
- Context menus.
- Sensible defaults.
- Input validation.
- Clear error messages.
- Progress indicators.
- Undo and redo.
- High-DPI support.
- Responsive panel resizing.
- Accessible control labels.
- Optional reduced decorative styling.

All numerical input fields should accept appropriate scientific notation.

High-precision coordinate fields must preserve their original textual precision.

Never silently round a coordinate in a way that changes the user's selected deep-zoom location.

---

### 19. Development roadmap

Build the application incrementally.

**Phase 1 — Foundation**

Create the Maven project, JavaFX application shell, custom visual theme, main viewport, and baseline Mandelbrot renderer.

**Phase 2 — Navigation**

Implement panning, zooming, coordinate input, bookmarks, and viewport history.

**Phase 3 — Performance**

Introduce tiled multithreaded rendering, cardioid/bulb tests, periodicity checking, progressive rendering, and cancellation.

**Phase 4 — Precision**

Implement double-double arithmetic, arbitrary-precision coordinates, and automatic precision selection.

**Phase 5 — Deep zoom**

Implement reference-orbit perturbation, glitch handling, caching, and series approximation.

**Phase 6 — Colouring**

Build the interactive palette editor and advanced colouring algorithms.

**Phase 7 — Creative tools**

Implement layers, blend modes, transformations, and additional fractal formulas.

**Phase 8 — Animation and export**

Implement the timeline, animation rendering, image export, and render queue.

**Phase 9 — Polish**

Complete the custom graphical assets, icons, keyboard shortcuts, diagnostics, testing, documentation, and Windows packaging.

Each phase should produce a working application.

Do not attempt to implement all features simultaneously in a single enormous class.

---

### 20. Required documentation

Produce detailed documentation covering:

- Application architecture.
- Mandelbrot mathematics.
- Numerical precision limitations.
- Double-double arithmetic.
- Perturbation theory.
- Series approximation.
- Rendering performance.
- Multithreading.
- Colour algorithms.
- Project file format.
- User instructions.
- Build and packaging instructions.

Include mathematical explanations and diagrams where useful.

The documentation should be suitable for adaptation into technical articles on *Code and Curiosity*.

---

### 21. Development and delivery requirements

When implementing the application:

1. Produce complete, compilable Java files rather than disconnected code fragments.
2. Include meaningful Javadoc and explanatory comments.
3. Use the MIT licence with copyright attribution to George Miller, 2026.
4. Keep the project compatible with Java 21 and Apache NetBeans.
5. Use Maven to manage dependencies.
6. Keep generated artwork and other assets in `src/main/resources`.
7. Clearly identify external libraries and their licences.
8. Do not invent benchmark results or claim unimplemented features work.
9. Test each major subsystem before proceeding.
10. Explain important mathematical and architectural decisions.

For graphical assets, provide consistent filenames, dimensions, transparent backgrounds where appropriate, and a clear resource directory structure.

Avoid copying Ultra Fractal's proprietary graphics, code, branding, or exact interface layouts.

The application must be an original product with comparable capabilities.

---

### 22. Final objective

The finished application should be something that a mathematician, programmer, fractal artist, or numerical computing enthusiast could genuinely use.

It should combine the computational capabilities of a serious fractal renderer with the visual character of a beautifully crafted scientific instrument.

The application should make deep mathematical exploration approachable while exposing advanced controls to experienced users.

**Aeternalis Fractal Studio** should be a flagship JavaFX project demonstrating that sophisticated mathematical software can be technically rigorous, highly performant, extensible, and visually distinctive.

**Begin by designing the complete architecture, Maven project structure, numerical rendering pipeline, and JavaFX application shell. Then implement Phase 1 as a working, compilable application before proceeding to subsequent phases.**
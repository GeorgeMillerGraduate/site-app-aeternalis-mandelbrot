# Zoom film workshop · 0.9.0

## Create a journey

1. Navigate to the desired final view. Apply any typed location, formula, quality and palette edits.
2. Open **Animation → Render zoom frames…**. It freezes the currently applied complete document as the destination.
3. Set starting span (default: the formula's Home span), output width/height, horizontal-span retention and playback fps. Home supplies the starting centre. Only the selected layer moves; the other layers retain their captured views and are composited normally.
4. Choose **Create zoom job…** and an empty folder. The app writes a self-contained `zoom.xml` containing exact start/destination real/imaginary/span, dimensions, retention, fps and the full formula/layer/palette snapshot. No external project file is required.
5. Choose **Render / resume PNGs**. It renders one complete frame at a time at the selected output resolution, through the existing precision-aware renderer, then writes `frame-000000.png`, `frame-000001.png`, etc. These are real independent fractal renders, not enlarged crops of one image.

The width rule is S(k) = S(start) × retention^k. For a start width of 4, retention 0.9 and destination 1e-13, n = ceil(log(1e-13/4) / log(0.9)) = 298 zoom steps. Including the opening frame gives 299 PNGs. The last span and centre are clamped to the exact destination. When the ratio is not an exact power, the final step is smaller than the regular step. Retaining 90% width retains 81% area at fixed aspect ratio. It does not multiply the centre coordinates by 0.9.

The centre moves from Home towards the captured centre using the normalized decrease in span, so it approaches the destination as the image narrows. The exact start and end are included. If spans are equal but centres differ, there are two endpoint frames. This first version has one continuous geometric segment, without easing curves, keyframe editors, target holds or variable iteration limits. The job uses the captured quality/iteration settings for every frame; insufficient iterations can still hide deep structures.

## Pause, exit, resume

Pause cooperatively cancels the current frame. Earlier complete PNGs remain. Closing the workshop or application also pauses its work. Reopen the program, choose the workshop, then **Open / resume XML…** and select the saved XML. Press Render/resume. Each existing PNG is decoded and dimension-checked; missing or damaged frames are regenerated. A frame is published through a temporary-file rename, followed by an advisory atomic progress checkpoint. A crash between the two is reconciled from the PNGs on restart. `progress.txt` is not trusted as proof of completion.

A folder lock prevents two jobs writing concurrently. `frames-job.sha256` binds the generated frames to the XML bytes: changing the XML after rendering starts is rejected. Use a fresh folder for a changed job. Keep the XML, identity file and PNGs together when moving the sequence. Temporary crash files can be ignored; the next run writes fresh temporary files. PNG validation detects unreadable/truncated files and incorrect dimensions; it is not a checksum audit against deliberate replacement of a valid same-size image. On network filesystems, atomicity/lock guarantees depend on that filesystem.

The main document can still be edited after capturing the job; those edits do not change the saved sequence. Animation uses its own renderer and CPU workers. Starting another main-canvas render can compete for resources; workshop opening cancels the current canvas render. The one-job-at-a-time folder lock is local filesystem based. No multi-machine distributed rendering is claimed.

## Make GIF or YouTube MP4

After rendering completes, close the renderer or leave it open. Open **Animation → Convert frame folder to video / GIF…**, then choose **Choose PNG frame folder…**. This is a separate prompt with its own playback fps and GIF/MP4 export controls. Export buttons remain disabled until a complete numbered sequence is found. Choose a completed folder containing consecutive `frame-000000.png` filenames. The converters reject gaps, unreadable files, inconsistent dimensions and unfinished jobs with `zoom.xml`. Encoding does not alter PNGs. Output is written to a temporary file and published only on successful completion. Pause also cancels encoding.

- **Animated GIF** is built in, streaming one frame at a time. It loops indefinitely, uses GIF colour quantization and hundredth-second timing (rounded cumulatively); fps is 1–100. Transparency is composited over black. GIF loses colour detail; MP4 is preferable for long full-resolution sequences.
- **YouTube MP4** uses the FFmpeg runtime integrated into the Maven project. The first Maven build resolves its platform-specific artifact; JavaCPP extracts and locates it automatically. There is no separate install or executable picker. H.264 output uses libx264, CRF18, yuv420p and faststart, with even-size padding where necessary. It creates a file for manual upload, without signing into YouTube. See FFMPEG.md for platform and dependency details.


You may select a different playback fps for encoding without rerendering PNGs. The XML retains its original fps; the encoder uses the visible fps field. Frame rendering speed does not affect playback speed. Rendering is sequential and bounded by one frame plus renderer caches; disk requirements grow with the number and dimensions of PNGs. Job limits: 100,001 frames, retention 0.01–0.9999, dimensions 16–7680 × 16–4320, plus existing per-layer/heap sample limits.

FFmpeg reference: https://ffmpeg.org/ffmpeg.html

## Validation

Eight automated animation tests cover geometric width/endpoints (including the 4→1e-13 example), exact XML round-trip, XML entity rejection, pause/restart, damaged-frame regeneration, completed-frame preservation, final-frame equality with an independent render, changed-job protection, GIF frame count, incomplete sequence/cancelled output handling and actual MP4 encoding when FFmpeg is available. The explicit release smoke rendered 27 actual 320×180 frames, generated a looping GIF and MP4, and captured the workshop UI. ffprobe confirmed H.264, 27 frames, 15 fps, 1.8 seconds. Windows native file-picker behaviour still needs target-machine validation.

0.7.1 UI regression: verified initial export gating, helpful empty-folder guidance, incomplete loaded-job gating, export unlocking after a real render and missing-frame detection before opening an output chooser. The workshop scrolls on smaller windows, shows the export-source folder, and updates the displayed destination when loading another XML job.

0.7.2: both prompts are separate owned windows with scrollable content. All application-owned settings stages and themed dialogs are capped at 90% of usable monitor width/height and constrained inside the visible main window, updating as it moves/resizes. Native file/folder chooser sizing belongs to the OS.

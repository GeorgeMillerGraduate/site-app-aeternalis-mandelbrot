# Artwork and icons

`assets/aeternalis-emblem.png`: original generated astrolabe emblem, retained from the initial application (1280×1280).

`assets/observatory-artwork.png`: new original generated 1536×1024 illustration, used in the masthead and About window. Brief: a premium Victorian mathematical observatory, brass astrolabe and drafting compass around a luminous teal/amber abstract spiral, copperplate engraving, dark charcoal/teal negative space for separately rendered UI typography; no text or watermark. Generated specifically for this application. This decorative spiral is not a numerical rendering or proof of fractal geometry.

Toolbar icons are authored code-native SVGPath shapes in Icons.java, not rasterized/generated labels. Preview PNGs in docs are screenshots of the actual JavaFX application. System fonts are used; no font binaries or vendor product assets are bundled.

`assets/opening-illustration.png`: the exact user-supplied Aeternalis portrait illustration (1193×1319), used intact in the three-second startup splash. No regeneration or edits were applied. Website colour reference: the user-supplied Jenga-Code KingFischer page screenshot. No browser chrome or website screenshot is shipped as a decorative app asset.

0.8.1 replaces the opening illustration with an image-generated edit adding an empty black bottom strip for the live timer. Prompt: preserve the Aeternalis title, portrait artwork and gold frame; add a blank full-width black strip below the frame, without text or a baked-in progress bar. The live gold bar is rendered by JavaFX, filling left to right over three seconds.

## Editorial artwork added in 0.12

Three original generated raster assets (built-in image generation), stored under `src/main/resources/com/jenga/aeternalis/assets/`:

- `editorial-masthead.png`: panoramic copperplate engraving of a mathematical notebook, drafting compass, fern, shell, telescope and observatory on ivory, with left negative space. Used in the masthead, About, Network and animation headers.
- `editorial-explorer.png`: portrait copperplate brass armillary sphere on a notebook, compass and ferns, copper/petrol ink on ivory. Used in the responsive left sidebar.
- `editorial-prism.png`: wide copperplate brass optical prism experiment, gently coloured spectrum, compass and geometric construction lines on ivory. Used in the gradient editor, including its detached window.

Production prompt set: antique Victorian scientific-journal copperplate engraving; cream #f5efe3, copper #a95126, petrol ink #123743; detailed crosshatching; edges fading to ivory; no text, UI, heavy frame or baroque ornament. Subject/composition variants are listed above. These decorative illustrations are separate raster assets, not flattened screenshots replacing functional controls. Existing emblem and three-second black-strip splash are retained.

The fifteen atlas JPEGs are actual Wikimedia thumbnails, not generated fractals. Their separate license and exact source coordinates are documented in WIKIMEDIA-CREDITS.md.

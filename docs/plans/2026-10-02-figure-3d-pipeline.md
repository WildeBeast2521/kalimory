# Pre-rendered 3D figure: pipeline plan

- **Status:** In progress. The owner chose this on 2026-10-02 (see the ADR 0008 amendment).
- **Goal:** each catalogue step shows a muscular human performing it, from the step's own three-quarter camera, with the worked muscles in red.

## Pieces

1. **Tools (local, not committed).**
   - Blender 4.2 LTS (portable tarball), the MPFB2 add-on and the MakeHuman system assets (CC0), all under `~/.local/share/figure-pipeline`.
   - The scripts in `scripts/figure/` record the exact versions and how to fetch them.
2. **Motion export (in the repo).**
   - A JVM entry point samples every `StepMotions` motion at a fixed number of frames, 16 per loop to start.
   - It writes a JSON file with each frame's joint positions, the camera, the props, and the step's primary and secondary muscles.
   - Being pure Kotlin, the same data drives the debug preview and the renders.
3. **Body (Blender, headless).**
   - MPFB2 builds one muscular male (high muscle, average weight) with its default skeleton and a skin material, saved as a `.blend` template.
   - Vertex groups for each catalogue muscle come from the rig's bone weights, split front and back where one bone carries two muscles (for example biceps and triceps).
4. **Posing.**
   - Empties are placed at the exported joints for every frame. IK constraints pull the hands, feet and head to them, and pole targets set the elbows and knees.
   - The pelvis carries the root position and turn.
5. **Rendering.**
   - The orthographic camera uses the step's turn and tilt; the render is transparent and about 256 px square.
   - The step's muscles are tinted red (primary) or light red (secondary) in the skin shader; props (bar, bench, wall) are simple boxes in a neutral grey.
   - The frames are packed into one WebP sprite sheet per step.
6. **App.**
   - The sprite sheets live in `assets/figures/<step id>.webp`. A small composable draws the current frame from the time, or the still frame when animations are off.
   - It replaces the procedural figure on each step's sheet.
   - A size check in the build keeps the total within budget.

## Budget and checks

- 53 steps × 16 frames × about 8 KB comes to about 7 MB; the hard ceiling is 30 MB.
- A unit test checks that every catalogue step has a sprite sheet with the expected frame count.
- Every render batch is reviewed on the emulator before merging.

## Order of work

1. Spike: Blender runs headless; MPFB2 builds the body; one still frame renders. Decide go or no-go on the look with the owner.
2. Motion export and IK posing for one exercise (the push-up), animated.
3. Muscle vertex groups and the red tint.
4. Render all 53, then the app's sprite player, then the step sheet.

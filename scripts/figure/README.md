# 3D figure pipeline

These scripts turn the catalogue's motions (`app/src/main/java/.../data/figure/`) into renders of a muscular human, as the owner chose on 2026-10-02 (ADR 0008 amendment, `docs/plans/2026-10-02-figure-3d-pipeline.md`).

- `fetch_tools.sh` downloads and checks Blender 4.2 LTS, MPFB2 and the MakeHuman CC0 assets into `~/.local/share/figure-pipeline`, with a private Blender profile. Nothing it fetches is committed or shipped.
- `build_body.py` builds `figure.blend`:
  - a muscular male (MakeHuman macros: male, muscle 1.0, weight 0.72) with the default rig and eyebrows;
  - plain shorts cut from the body's own skin;
  - one point attribute per catalogue muscle (`m_<muscle>`), from the rig's bone weights and the rest pose, split front and back;
  - a skin shader that mixes towards red by the `worked` attribute (x = primary, y = secondary).
- `render_still.py` renders one still with the given primary and secondary muscles and camera turn, to check the look.

Run them with the environment that `fetch_tools.sh` sets, for example:

```
DIR=~/.local/share/figure-pipeline
export BLENDER_USER_CONFIG=$DIR/bconfig/config BLENDER_USER_SCRIPTS=$DIR/bconfig/scripts \
       BLENDER_USER_EXTENSIONS=$DIR/bconfig/extensions BLENDER_USER_DATAFILES=$DIR/bconfig/datafiles
cp scripts/figure/*.py $DIR/work/
$DIR/blender-4.2.23-linux-x64/blender -b --python $DIR/work/build_body.py
$DIR/blender-4.2.23-linux-x64/blender -b --python $DIR/work/render_still.py -- chest,triceps shoulders $DIR/work/front.png 35
```

Still to come: the motion export, posing by inverse kinematics, sprite sheets, and the app's player.

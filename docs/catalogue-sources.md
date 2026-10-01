# Catalogue sources

Where the content of the built-in exercise catalogue comes from (ADR 0006). Update this file whenever content from a new source is used.

The catalogue's single source is `scripts/catalogue/catalogue_spec.py`. `scripts/catalogue/generate_catalogue.py` turns it into `data/catalogue/Catalogue.kt` and `res/values/catalogue_strings.xml`.

**Catalogue v1, core chains (PR #89):**
- 9 chains and 53 steps: push-up, handstand push-up, dip, pull-up, row, squat, hip hinge, core and leg raise.
- Every name, description, cue, standard and difficulty is written for this project.
- free-exercise-db served only as a checklist and a cross-check for muscle groups. None of its text was copied.

| Content | Source | Licence | What is used |
|:---|:---|:---|:---|
| Exercise names, primary and secondary muscles, equipment, force, mechanic, level | [free-exercise-db](https://github.com/yuhonas/free-exercise-db) | Unlicense (public domain) | Facts only. No instructions text and no photos, because their origin is unclear. |
| Descriptions, form cues, standards, chains and difficulty | Written for this project | GPL-3.0-or-later | Everything |
| Exercises missing from free-exercise-db (pistol squat, levers, planche, L-sit, muscle-up, hollow body, Nordic curl, archer variations and others) | Written for this project | GPL-3.0-or-later | Everything |
| Muscle map body regions | [react-body-highlighter](https://github.com/GV79/react-body-highlighter) 2.0.5 (npm tarball sha256 `c15467a8fcf48193896ecbaef1eb98ff260e50021cea3454313ff1016f59e2cb`), Copyright (c) 2020 GV79 | MIT | Polygon outlines only, converted by `scripts/musclemap/generate_body_map.py` into `ui/components/muscles/BodyMapData.kt`, which carries the MIT notice. Listed in Open Source Licenses. |
| Figure keyframes | Written for this project | GPL-3.0-or-later | Everything |

Not used, and why:
- **wger exercise data:** mixed CC BY-SA 3.0 and 4.0. Only 4.0 is compatible with GPLv3.
- **Convict Conditioning, Overcoming Gravity:** all rights reserved. Their ideas (steps with standards, one difficulty scale) are general practice, but their text, tables and numbers are not copied.
- **r/bodyweightfitness wiki:** no explicit licence. Used only as a reference for common practice.

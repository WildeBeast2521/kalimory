# Credits and sources

## The app

This app began as a fork of [Calisthenics Memory by Gonbei774](https://codeberg.org/Gonbei774/CalisthenicsMemory), licensed under GPL-3.0. Its history is kept in this repository. Thanks to Gonbei774 and to the original project's translators.

## The exercise catalogue

Every name, description, form cue, standard, chain and difficulty in the catalogue was written for this project.

| Content | Source | Licence | What is used |
|:---|:---|:---|:---|
| Exercise names, primary and secondary muscles, equipment, level | [free-exercise-db](https://github.com/yuhonas/free-exercise-db) | Unlicense (public domain) | Facts only, as a checklist and a cross-check. No text and no photos. |
| Descriptions, cues, standards, chains and difficulty | Written for this project | GPL-3.0-or-later | Everything |
| Muscle map body outlines | [react-body-highlighter](https://github.com/GV79/react-body-highlighter) 2.0.5, Copyright (c) 2020 GV79 | MIT | Polygon outlines only, converted by `scripts/musclemap/generate_body_map.py` into `BodyMapData.kt`, which carries the MIT notice. Also listed in the app's Open Source Licenses screen. |

Not used:

- **wger exercise data:** mixed CC BY-SA 3.0 and 4.0 licences. Only 4.0 is compatible with GPL-3.0.
- **Convict Conditioning and Overcoming Gravity:** all rights reserved. Stepped progressions with standards are common practice, but no text, tables or numbers were copied.
- **The r/bodyweightfitness wiki:** no explicit licence. Used only as a reference for common practice.

## Sounds

The workout sounds are original, synthesised by `scripts/sounds/generate_workout_sounds.py`.

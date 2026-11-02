# Credits and sources

## The app

This app began as a fork of [Calisthenics Memory by Gonbei774](https://codeberg.org/Gonbei774/CalisthenicsMemory), licensed under GPL-3.0-only. Kalimory keeps that licence. Its history is kept in this repository. Thanks to Gonbei774 and to the original project's translators.

## The exercise catalogue

Every name, description, form cue, standard, chain and difficulty in the catalogue was written for this project.

| Content | Source | Licence | What is used |
|:---|:---|:---|:---|
| Exercise names, primary and secondary muscles, equipment, level | [free-exercise-db](https://github.com/yuhonas/free-exercise-db) | Unlicense (public domain) | Facts only, as a checklist and a cross-check. No text and no photos. |
| Descriptions, cues, standards, chains and difficulty | Written for this project | GPL-3.0-only | Everything |
| Muscle map body outlines | [react-body-highlighter](https://github.com/GV79/react-body-highlighter) 2.0.5, Copyright (c) 2020 GV79 | MIT | Polygon outlines only, converted by `scripts/musclemap/generate_body_map.py` into `BodyMapData.kt`, which carries the MIT notice. Also listed in the app's Open Source Licenses screen. |

Not used:

- **wger exercise data:** mixed CC BY-SA 3.0 and 4.0 licences. Only 4.0 is compatible with GPL-3.0.
- **Convict Conditioning and Overcoming Gravity:** all rights reserved. Stepped progressions with standards are common practice, but no text, tables or numbers were copied.
- **The r/bodyweightfitness wiki:** no explicit licence. Used only as a reference for common practice.

## Fonts and icons

- **Onest** (SIL Open Font License 1.1) is the app's typeface. The README images draw their text from it.
- **Material Symbols Rounded** (Apache-2.0) supply the app's icons and the README's feature icons.
- The launcher icon, The Climb, was made for this project.

## Sounds

The workout sounds are original, synthesised by `scripts/sounds/generate_workout_sounds.py`.

## Research the app cites

Every claim the app attributes to research is listed here: the source, what it found and did not find, and the exact wording the app uses.

### 10 sets a week (weekly review)

**Source.** Schoenfeld BJ, Ogborn D, Krieger JW. Dose-response relationship between weekly resistance training volume and increases in muscle mass: a systematic review and meta-analysis. *Journal of Sports Sciences* 2017;35(11):1073–1082. [doi:10.1080/02640414.2016.1210197](https://doi.org/10.1080/02640414.2016.1210197) (published online 19 July 2016).

**What it found.**

- 15 studies with 34 training groups. All used weight training at 65 % of the one-repetition maximum or more, for at least 6 weeks. 12 of the 15 studies had untrained participants.
- More weekly sets per muscle went with more muscle growth: each extra weekly set added about 0.37 % to the gain (P = 0.002).
- Grouped by weekly sets per muscle, mean gains were 5.4 % for fewer than 5 sets, 6.6 % for 5 to 9 and 9.8 % for 10 or more (Table 2). The authors conclude that "greater muscular development is achieved by performing at least 10 weekly sets per muscle group".

**What it did not find.**

- The three-group comparison was a trend, not statistically significant (P = 0.074). With one influential study removed, the 10-or-more group no longer had an advantage (Table 3).
- There was too little data above 12 sets a week to say where the benefit stops.
- The studies used external loads, not bodyweight exercises, and mostly untrained people.

**What the app says.** On the weekly review, Push, Pull and Legs show a mark at 10 sets a week, and the line under the rows reads: "The mark shows 10 sets a week. In a review of 15 studies of weight training, more sets a week went with more muscle growth, and groups doing 10 or more sets per muscle grew the most (Schoenfeld et al., 2017)." (string `weekly_review_reference`). The mark is a reference point, not a target: counts under or over it look the same apart from the length of the bar. The app counts every logged set except one marked as a warm-up, whatever its effort rating, and a movement group such as Push stands in for the muscles it trains.

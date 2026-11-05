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

### Helping muscles count as half a set (body map)

**Source.** Pelland JC, Remmert JF, Robinson ZP, Hinson S, Zourdos MC. The resistance training dose-response: meta-regressions exploring the effects of weekly volume and frequency on muscle hypertrophy and strength gain. *SportRxiv* preprint, 2024 (last modified September 2024; not peer reviewed). The app's wording was checked against this preprint.

**What it found.**

- 67 studies with 2,058 participants (28 studies of untrained and 39 of trained people), interventions of about 10 weeks.
- Each set was classed as direct for a muscle (the muscle is the main force generator) or indirect (it helps as a synergist). Indirect sets were counted three ways: as a full set ("total"), as half a set ("fractional") or not at all ("direct").
- "The relative evidence for the 'fractional' quantification method was strongest". For weekly volume and muscle growth, half counting beat full counting (2 × log Bayes factor 9.48, "strong") and beat leaving helping sets out (10.29, "very strong").
- More weekly sets went with more growth, with diminishing returns.

**What it did not find.**

- The half weighting is a convention the analysis compared, not a measured value for any muscle: the authors note the method "suffers from the assumption that all indirect training should be quantified as half a set".
- They also note that "any choice of time period for quantifying volume and frequency is arbitrary" (the week).
- The studies used external loads, mostly taken to failure; none is described as bodyweight training. Which muscles help in a step is the app's own reading of the catalogue (its secondary muscles), not the paper's.

**What the app says.** On the body map's legend: "Sets a week for each muscle. A set counts in full for the muscles it mainly works and as half for the muscles that help: in a review of 67 studies, counting helping sets as half matched muscle growth best (Pelland et al., 2024)." (string `body_map_counting`). The shades (under 5, 5 to 9, 10 or more sets a week) are a reading aid, not a finding of either paper.

# Interface rules

How Kalimory screens are built, so a new screen looks and behaves like the rest. The rules are
short; the shared components named here already follow them, so using them is usually all it takes.
`ConsistencyGuardTest` fails when a screen builds its own version of the ones it checks.

## Text

Pick text by its role and take the style from `MaterialTheme.typography`, not from a size.

| Role | Style |
| --- | --- |
| Screen title | `CalmTopBar`; it may wrap to two lines |
| Section heading | `titleMedium`, `onSurfaceVariant` |
| Row title | `titleMedium` (`titleSmall` in a dense picker) |
| Row detail, description | `bodyMedium`, `onSurfaceVariant` |
| Tags, badges, meta text | `labelMedium`; `labelSmall` is the smallest |
| Number in a stepper | `headlineMedium` |
| Workout counts and times | `WorkoutNumerals` |

A bare `fontSize = 14.sp` inherits a line height meant for larger text. Where the size matches a
style, use the style.

## Layout

- **Spacing** comes from `Spacing`, on a 4 dp grid. Screen sides and card padding are 16 dp. Keep the
  bottom of a scrolling list clear of the floating button.
- **Group related rows in one card** (`surfaceContainerLow`, large corners), as the Library tab does.
  One card per section, not one per row. Rows are separated by space, not dividers.
  `SettingsGroup` and `SettingRow` build this for settings screens.
- **Rows keep one shape:** a leading slot (kept even when empty, so titles line up), a text column
  that takes the remaining width, and a trailing slot. `InfoActionRow` and `SettingRow` do this.
- **Shapes** come from the theme: `MaterialTheme.shapes`. Chips and tags are small, text fields and
  inner rows medium, cards large, and every button is a pill. Do not write `RoundedCornerShape(12.dp)`
  where a theme shape has that radius.

## Colour

- **Primary** marks the one main action on a screen, filled, and things that are done.
- **Tertiary** is only for what is happening now: the current set, a countdown.
- **Error** is only for destructive actions and warnings.
- Numbers and emphasis use `onSurface` and weight, not the primary colour. A plan's sets, reps and rest
  share one muted colour.
- In dark themes, unselected choices must still stand apart from the card behind them.

## Buttons

Every tappable word is a button, and each role has one look. A screen has one filled button.

| Role | Look | Used for |
| --- | --- | --- |
| Primary | Filled (`WorkoutPrimaryButton` on a workout, `StartButton` on a list row, `TopBarAction` in an editor) | The one main action |
| Helper | Tonal (`FilledTonalButton`) | Add set, Add exercise, See all, Weekly review, Browse, Redo, Jump, Set to target |
| Quiet | Outlined (`OutlinedButton`) | Restart set, Skip set, Skip exercise, Save and exit, Stop, -10 s and +10 s |
| Destructive | `DestructiveButton` | Delete or discard something |
| Dialog action | Confirm is a filled `Button` (error colour when it deletes or discards), dismiss is an `OutlinedButton` | Cancel, OK, Exit, Delete |
| Icon action | `IconButton` with a label, or the `RowMenu` | A row's other actions |

A waiting screen (get ready, rest) has no main button, so its Skip is tonal. Buttons keep the theme's
colours and shape and a 48 dp minimum height, and two buttons never touch: leave a gap of at least 4 dp
(`tapTargetProblems` fails a screen test when they do). There is no text button anywhere.
`ConsistencyGuardTest` fails on a text button, on tappable text, on a dialog whose confirm is not a filled
button or whose dismiss is not outlined, and on a button with colours or a border set by hand.

## Text size and touch targets

The app is checked at 200 % font size on a 360 dp phone.

- Anything that holds text uses `heightIn(min = 48.dp)`, never a fixed `height`.
- No fixed label widths. A label that does not fit wraps or the row changes layout
  (`isLargeFontScale()`); it is not shrunk out of reach. One-line labels in a fixed width use `FitText`.
- Controls at the end of a row take about 40 % of it at most. Beyond that they move under the text.
- Every tappable thing is at least 48 × 48 dp and says what it is, by its text or a
  `contentDescription`.

## Shared components

Use these instead of a local version.

| For | Use |
| --- | --- |
| Screen header | `CalmTopBar` |
| Floating add button | `AppFab` |
| Start a workout | `StartButton` |
| One number | `NumberStepper`, with `NumberStepButton` for the buttons |
| On/off setting | `SwitchRow`, `SettingSwitchRow`, `SettingValueRow` |
| Pick one of a few | `ConnectedChoices` |
| Line under an exercise name | `ExerciseBadges` |
| Choose one exercise | `ExercisePickRow` |
| Tick several exercises | `ExerciseCheckRow` |
| Nothing here yet | `EmptyState` |
| Text and the buttons that act on it | `InfoActionRow` |
| Swipe to delete | `SwipeToDeleteBackground` |
| Delete or discard something | `DestructiveButton` |
| Group name on a row | `GroupBadge` |

## Empty screens

Say what is missing and give the action: a short sentence and a button, near the top under the
title. Do not centre it in an otherwise empty screen, and do not say "tap + to create one".

## Entering numbers

Use `NumberStepper`: buttons that step, a number that can also be typed, and the unit beside it.
Steppers use the same button size and value width everywhere, so neighbouring ones line up. An empty
value shows a dash, not a gap.
A measurement with a unit or decimals (weight, distance, seconds, rest) uses `MetricStepper`, the same
buttons with a label above; a one-line setting inside a card uses a labelled stepper line. A bare text box
is not used for a number.

## Lists and gestures

Tapping a row opens it (its editor, or its details). A row's other actions, such as duplicate, share or
delete, sit behind the ⋮ button at its end (`RowMenu`), so every action can be found and none depends on a
gesture. A long press does not open a menu; where a list can be reordered, a long press on the drag
handle moves the row. Swipe-to-delete uses `SwipeToDeleteBackground`. `ConsistencyGuardTest` fails when a long press opens a menu again.

Titles of delete dialogs say what is deleted ("Delete this exercise?"), and the dialog's text says what
else goes with it.

## Forms

A form's save button stays enabled. When something is missing, tapping it marks the field and says
what to enter, so the person is never left with a grey button and no reason.

## Words

Sentence case. Keep one word for one thing across a flow: a button and the message it leads to use the
same verb. Strings are resources in every language the app ships; counts use plurals.

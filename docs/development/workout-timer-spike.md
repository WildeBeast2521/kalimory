# Durable workout timer spike (Task 6)

Status: Task 6 is complete. The interval screen runs on the reducer. Program and single-exercise execution time sets and rests with `StepStopwatch`. All three modes resume after process death, and the timer service releases its wake lock.

## Problem

Workout, program, and interval timers keep time in Compose `LaunchedEffect` loops that call `delay(1000)` and then change a counter, for example in `ui/components/program/ProgramIntervalComponents.kt`. This has four consequences:

- Time drifts by the loop's own run time and by late scheduling.
- Pausing drops any partial second.
- Process death loses the timer. Only the program "Save & Exit" snapshot in `SavedWorkoutState` survives.
- `WorkoutTimerService` only holds a wake lock and a foreground notification.

## Contract (`app/src/main/java/io/github/gonbei774/calisthenicsmemory/workout/`)

- `WorkoutState` holds the steps (countdown or stopwatch), the current step, and a timer that is either not started, running since a monotonic time with accumulated time, or paused with accumulated time. Elapsed and remaining time are computed from "now".
- `WorkoutReducer.reduce(state, event)` is pure.
  - A countdown completes at its exact deadline, however late the tick arrives.
  - Paused time never elapses.
  - Every refused event returns `Transition.Rejected` with the unchanged state and a reason: time going backwards, wrong status, stale step index, or invalid step.
- `WorkoutCheckpoint` (version 1) stores the state with the monotonic time, wall time, and boot count.
- `WorkoutRecovery.recover` rebuilds the state:
  - It is `EXACT` on the same boot.
  - After a reboot it estimates the gap from the wall clock (`APPROXIMATE_AFTER_REBOOT`). A wall clock that went backwards counts as no gap (`APPROXIMATE_CLOCK_CHANGED`).
- `WorkoutCheckpointStore` writes to a temporary file, syncs it, then renames it atomically. It reports missing, unsupported-version, or unreadable files and never deletes them.

Tests: `WorkoutReducerTest` (16), `WorkoutTimerRecoveryTest` (6), and `WorkoutCheckpointStoreTest` (5) on the JVM, plus `WorkoutCheckpointDeviceTest` on a device.

## Integration

- Done for the interval screen (`IntervalWorkoutPlan` plus a 100 ms render tick). On the floor_api29 AVD, a 35 s program with the screen off for 12 s completed at 36.4 s (detection latency included), and a 10.3 s pause gave 46.4 s against 45.3 s expected.
- Done for the interval screen. It saves on start, pause, resume, and skip, and clears on finish, stop, or discard. Reopening the program offers Resume. On floor_api29:
  - After a `kill -9` at 13 s during a rest, resuming caught up to the true end at 35 s.
  - A paused workout killed for 37 s resumed with the same remaining time.
  - Reboot mid-workout: the dialog notes the estimate, and the resumed remaining time was 61 s against 61-64 s from the wall clock.
  - Same boot with the wall clock set back 1 h: recovery stays exact (95 s against 94-97 s).
  - Reboot, then the clock set back 1 h: recovery assumes no time away and resumes from the last checkpoint. Checkpoints are saved only on start, pause, resume, and skip, so this can be well before the interruption.
  - Recovery runs when Resume is tapped, so time spent reading the dialog is not counted.
- Program execution now times its start countdown, rests, holds, and rep timers with `StepStopwatch`, so there is no drift and pause is exact. On floor_api29 the 35 s auto program completed at 37.1 s with the screen on and 38.0 s with the screen off from 8 s to 21 s. Program sessions still rely on the manual "Save & Exit" (`SavedWorkoutState`) rather than automatic checkpoints.
- Single-exercise execution uses `StepStopwatch` too. On floor_api29 its 35 s auto run completed at 36.8 s with the screen on and 37.7 s with the screen off from 8 s to 21 s. No `delay(1000)` counting loop remains in the execution screens.
- Program sessions checkpoint automatically (`ProgramSessionCheckpoint`), separately from the user's "Save & Exit" slot. Resume restarts the interrupted set, or returns to the result screen. Verified on floor_api29 with kills during a set and on the result screen.
- Single-exercise sessions checkpoint automatically too (`SingleSessionCheckpoint`), and are offered for resume when the Workout screen opens. Verified on floor_api29 with kills during a set and on the confirmation screen.
- `WorkoutTimerService` keeps no workout state or decisions. It holds a foreground notification and a wake lock while a timer runs. The lock was reference-counted, and every step change started the service again, so it stayed held after workouts ended. It is now held once and released by stop, destroy, or task removal. This is guarded by `WorkoutTimerServiceTest`.

## Remaining notes

- The notification shows a fixed text. Showing the live step and remaining time is optional polish, not part of Task 6.
- `util/WakeLockManager.kt` is unused. Removing it is listed in the open small fixes in `overhaul-status.md`. (`WorkoutScreen.ExecutingStep` is already gone.)
- `WorkoutTimerService` is an adapter, and its wake-lock release is tested (see "Integration").
- The manual protocol ran on floor_api29 (API 29): screen off, background, kill, reboot and clock change, with the results above. No run on an API 26 device is recorded.

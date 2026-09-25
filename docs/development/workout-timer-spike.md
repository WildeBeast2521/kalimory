# Durable workout timer spike (Task 6)

Status: the pure core and the checkpoint layer are done. The interval screen runs on the reducer and resumes after process death.

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

## Not done yet (integration)

- Done for the interval screen (`IntervalWorkoutPlan` plus a 100 ms render tick). On the floor_api29 AVD, a 35 s program with the screen off for 12 s completed at 36.4 s (detection latency included), and a 10.3 s pause gave 46.4 s against 45.3 s expected. Still to move over: program and single-exercise execution.
- Done for the interval screen. It saves on start, pause, resume, and skip, and clears on finish, stop, or discard. Reopening the program offers Resume. On floor_api29:
  - After a `kill -9` at 13 s during a rest, resuming caught up to the true end at 35 s.
  - A paused workout killed for 37 s resumed with the same remaining time.
  - Reboot mid-workout: the dialog notes the estimate, and the resumed remaining time was 61 s against 61-64 s from the wall clock.
  - Same boot with the wall clock set back 1 h: recovery stays exact (95 s against 94-97 s).
  - Reboot, then the clock set back 1 h: recovery assumes no time away and resumes from the last checkpoint. Checkpoints are saved only on start, pause, resume, and skip, so this can be well before the interruption.
  - Recovery runs when Resume is tapped, so time spent reading the dialog is not counted.
  - Still to do: the other execution screens.
- Make `WorkoutTimerService` an adapter: it shows state and forwards user actions and never makes domain decisions. Verify it releases its wake lock on teardown.
- Run the manual API 26+ protocol and record the results: screen off during a countdown, app in the background, process killed with `adb shell am kill`, device reboot, and a wall-clock change. For each, note the observed drift and the recovery timing.

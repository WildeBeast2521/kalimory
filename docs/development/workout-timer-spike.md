# Durable workout timer spike (Task 6)

Status: the pure core and the checkpoint layer are done. Nothing in the app uses them yet.

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

- Drive one execution screen, starting with the interval screen or the start countdown, from the reducer. Render from `remainingInStep(clock.nowMillis())` on a display tick instead of counting.
- Save a checkpoint on every accepted non-tick event, restore it on start-up, and clear it on finish or abandon.
- Make `WorkoutTimerService` an adapter: it shows state and forwards user actions and never makes domain decisions. Verify it releases its wake lock on teardown.
- Run the manual API 26+ protocol and record the results: screen off during a countdown, app in the background, process killed with `adb shell am kill`, device reboot, and a wall-clock change. For each, note the observed drift and the recovery timing.

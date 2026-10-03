package io.github.gonbei774.calisthenicsmemory.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import io.github.gonbei774.calisthenicsmemory.R

/**
 * The workout sounds ("wood and brass", scripts/sounds): one per moment, so each is recognisable
 * without looking. While a cue plays, other audio such as music is ducked for just that moment.
 */
class SoundPlayer(context: Context) {
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(attributes)
        .build()
    private val countdownId = soundPool.load(context, R.raw.sound_countdown, 1)
    private val goId = soundPool.load(context, R.raw.sound_go, 1)
    private val setDoneId = soundPool.load(context, R.raw.sound_set_done, 1)
    private val repId = soundPool.load(context, R.raw.sound_rep, 1)
    private val holdTickId = soundPool.load(context, R.raw.sound_hold_tick, 1)

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .build()
    private val handler = Handler(Looper.getMainLooper())
    private val releaseFocus = Runnable { audioManager?.abandonAudioFocusRequest(focusRequest) }

    /** Each of the last three seconds of a countdown. */
    fun playCountdown() = play(countdownId, COUNTDOWN_MS)

    /** A set or work phase starts. */
    fun playStartCue() = play(goId, GO_MS)

    /** The target is reached or the hold is finished. */
    fun playSetComplete() = play(setDoneId, SET_DONE_MS)

    /** Each rep, when the count sound is on: too short and frequent to duck other audio for. */
    fun playRep() {
        soundPool.play(repId, 1f, 1f, 1, 0, 1f)
    }

    /** Every few seconds of a hold, when that sound is on. */
    fun playHoldTick() = play(holdTickId, HOLD_TICK_MS)

    fun release() {
        handler.removeCallbacks(releaseFocus)
        audioManager?.abandonAudioFocusRequest(focusRequest)
        soundPool.release()
    }

    private fun play(id: Int, lengthMs: Long) {
        // Ask other audio to duck for the length of this sound; a later cue extends the window.
        audioManager?.requestAudioFocus(focusRequest)
        soundPool.play(id, 1f, 1f, 1, 0, 1f)
        handler.removeCallbacks(releaseFocus)
        handler.postDelayed(releaseFocus, lengthMs + FOCUS_MARGIN_MS)
    }

    internal companion object {
        // Lengths of the generated sounds (scripts/sounds/generate_workout_sounds.py).
        const val COUNTDOWN_MS = 220L
        const val GO_MS = 1_399L
        const val SET_DONE_MS = 1_450L
        const val HOLD_TICK_MS = 160L
        const val FOCUS_MARGIN_MS = 150L
    }
}

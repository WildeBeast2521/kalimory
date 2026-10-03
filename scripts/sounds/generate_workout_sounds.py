"""Synthesises the app's workout sounds (the "wood and brass" set) as 44.1 kHz mono WAV files.

The owner chose this set on 2026-09-30. The sounds are our
own work, made by this script: pure Python, no samples, deterministic (seeded noise), all tuned to
D major so they read as one family. scripts/sounds/build-sounds.sh encodes them into res/raw.

    python3 scripts/sounds/generate_workout_sounds.py <output-dir>
"""
import math, os, random, struct, sys, wave

SR = 44100


def silence(sec):
    return [0.0] * int(SR * sec)


def mix(*tracks):
    n = max(len(t) for t in tracks)
    return [sum(t[i] for t in tracks if i < len(t)) for i in range(n)]


def at(track, sec, total=None):
    pad = [0.0] * int(SR * sec)
    return pad + track


def normalise(x, peak_db):
    peak = max(abs(v) for v in x) or 1.0
    g = 10 ** (peak_db / 20) / peak
    return [v * g for v in x]


def fade_tail(x, sec=0.01):
    n = int(SR * sec)
    for i in range(1, n + 1):
        if i <= len(x):
            x[-i] *= (i - 1) / n
    return x


def fade_out(x, sec):
    """A smooth (raised-cosine) release over the last sec seconds, so a ringing bell dies away
    instead of stopping mid-ring."""
    n = min(len(x), int(SR * sec))
    start = len(x) - n
    for i in range(n):
        x[start + i] *= 0.5 * (1 + math.cos(math.pi * i / n))
    return x


def partials(freq, ratios, amps, decays, dur, attack=0.002):
    """Sum of decaying sine partials: the basis for mallets and bells."""
    n = int(SR * dur)
    out = [0.0] * n
    a_n = max(1, int(SR * attack))
    for r, a, d in zip(ratios, amps, decays):
        f = freq * r
        if f > SR / 2.2:
            continue
        w = 2 * math.pi * f / SR
        k = math.exp(-1.0 / (SR * d))
        env = a
        for i in range(n):
            att = min(1.0, i / a_n)
            out[i] += math.sin(w * i) * env * att
            env *= k
    return out


def noise_click(dur, decay, seed, lowpass=0.35):
    rnd = random.Random(seed)
    n = int(SR * dur)
    out, prev, env = [], 0.0, 1.0
    k = math.exp(-1.0 / (SR * decay))
    for _ in range(n):
        v = rnd.uniform(-1, 1)
        prev = prev + lowpass * (v - prev)
        out.append(prev * env)
        env *= k
    return out


# ---- instruments -------------------------------------------------------

def wood(freq, dur=0.12, bright=1.0, seed=1, ring=0.035):
    """A wood block: a hollow modal body with a short noise strike. [ring] sets how long it sounds."""
    body = partials(freq, [1, 2.58, 4.2], [1, 0.35 * bright, 0.12 * bright], [ring, ring * 0.5, ring * 0.3], dur)
    click = noise_click(0.012, 0.003, seed)
    return mix(body, [c * 0.35 for c in click])


def marimba(freq, dur=0.9):
    """Warm bar: fundamental with the 4th and 10th partials tuned bars have."""
    return partials(freq, [1, 3.93, 9.9], [1, 0.22, 0.05], [0.35, 0.08, 0.02], dur, attack=0.003)


def brass_bell(freq, dur=1.2):
    """A small brass bell or singing bowl: inharmonic partials, long warm decay."""
    ratios = [0.5, 1.0, 1.19, 1.56, 2.0, 2.51, 3.0]
    amps = [0.25, 1.0, 0.45, 0.3, 0.35, 0.15, 0.08]
    decays = [0.9, 0.7, 0.5, 0.35, 0.3, 0.18, 0.12]
    return partials(freq, ratios, amps, decays, dur, attack=0.003)


def glock(freq, dur=0.8):
    """Glockenspiel: bright, glassy, fast upper decay."""
    return partials(freq, [1, 2.76, 5.4], [1, 0.3, 0.12], [0.45, 0.12, 0.05], dur, attack=0.001)


def soft_beep(freq, dur=0.12):
    """A rounded electronic beep: sine with a soft attack and release."""
    n = int(SR * dur)
    a, r = int(SR * 0.006), int(SR * 0.04)
    out = []
    for i in range(n):
        env = min(1.0, i / a) * min(1.0, (n - i) / r)
        out.append(math.sin(2 * math.pi * freq * i / SR) * env + 0.15 * math.sin(4 * math.pi * freq * i / SR) * env)
    return out


# D major pitches
D5, Fs5, A5, B5, D6, E6, Fs6, A6 = 587.33, 739.99, 880.0, 987.77, 1174.66, 1318.51, 1479.98, 1760.0

# One sound per moment. The more often a sound plays, the shorter and quieter it is.
SOUNDS = {
    # Last three seconds of any countdown: a wood block, as audible as the old beep.
    "countdown": lambda: normalise(wood(A5, 0.22, bright=1.3, ring=0.075), -1),
    # A set or work phase starts: a small brass bell, a step above the countdown.
    # It rings out for 1.4 s and fades over the last 0.6 s (it used to stop mid-ring at 0.9 s).
    "go": lambda: normalise(fade_out(brass_bell(D6, 1.4), 0.6), -1),
    # Target reached or hold finished: a rising marimba phrase that resolves on the bell.
    # The closing bell rings to 1.45 s and fades over the last 0.55 s.
    "set_done": lambda: normalise(fade_out(mix(marimba(D5, 1.2), at(marimba(Fs5, 1.1), 0.09), at(marimba(A5, 1.0), 0.18), at(brass_bell(D6, 1.18), 0.27)), 0.55), -1),
    # Each rep, when the count sound is on: short, soft and high, so thirty in a row stay light.
    "rep": lambda: normalise(wood(E6, 0.06, bright=0.6, seed=2), -8),
    # Every few seconds of a hold, when that sound is on: a lower wood tick.
    "hold_tick": lambda: normalise(wood(D5, 0.16, seed=3, ring=0.05), -4),
}


def write(path, x):
    x = fade_tail(list(x))
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", max(-32767, min(32767, int(v * 32767)))) for v in x))


if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    for name, make in SOUNDS.items():
        x = make()
        # The longer cues must die away: their last 50 ms stay below 1 % of the peak.
        if name in ("go", "set_done"):
            tail = max(abs(v) for v in x[-int(SR * 0.05):])
            assert tail < 0.01 * max(abs(v) for v in x), f"{name} ends abruptly"
        write(os.path.join(out, name + ".wav"), x)

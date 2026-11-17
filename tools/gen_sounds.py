#!/usr/bin/env python3
"""Synthesizes the Traffic Jam Rush sound set (SFX + two seamless music loops).

Pure standard library. Output: 16-bit mono 22.05 kHz WAV files in app/src/main/res/raw.
Run:  python3 tools/gen_sounds.py
"""
import math
import os
import random
import struct
import wave

SR = 22050
TWO_PI = 2.0 * math.pi
OUT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                    '..', 'app', 'src', 'main', 'res', 'raw'))
rng = random.Random(20261104)


# ---------------------------------------------------------------- basics

def n_of(sec):
    return int(round(sec * SR))


def midi(m):
    return 440.0 * 2.0 ** ((m - 69) / 12.0)


def zeros(sec):
    return [0.0] * n_of(sec)


def add(dst, src, at=0.0, gain=1.0, wrap=False):
    """Mixes src into dst starting at `at` seconds. With wrap, tails wrap around (for loops)."""
    off = n_of(at)
    L = len(dst)
    if wrap:
        for i, v in enumerate(src):
            dst[(off + i) % L] += v * gain
    else:
        end = min(L, off + len(src))
        for i in range(max(0, off), end):
            dst[i] += src[i - off] * gain
    return dst


def mul(x, e):
    return [a * b for a, b in zip(x, e)]


def scale(x, g):
    return [v * g for v in x]


# ---------------------------------------------------------------- oscillators

SINE = ((1, 1.0),)
SAW = tuple((k, 1.0 / k) for k in range(1, 16))
SQUARE = tuple((k, 1.0 / k) for k in range(1, 16, 2))
TRI = tuple((k, ((-1) ** ((k - 1) // 2)) / (k * k)) for k in range(1, 12, 2))
SOFT = ((1, 1.0), (2, 0.35), (3, 0.18), (4, 0.08))
BRASS = tuple((k, 1.0 / (k ** 0.85)) for k in range(1, 10))
REED = ((1, 1.0), (2, 0.6), (3, 0.5), (4, 0.25), (5, 0.3), (6, 0.12), (7, 0.1))
BASS = ((1, 1.0), (2, 0.45), (3, 0.2), (4, 0.08))

_TN = 2048
_tables = {}


def _table(harm):
    if harm not in _tables:
        t = []
        for i in range(_TN):
            ph = TWO_PI * i / _TN
            t.append(sum(a * math.sin(k * ph) for k, a in harm))
        peak = max(abs(v) for v in t) or 1.0
        t = [v / peak for v in t]
        t.append(t[0])
        _tables[harm] = t
    return _tables[harm]


def osc(freq, dur, harm=SINE, f_end=None, vib=0.0, vib_rate=5.5, vib_delay=0.0,
        am=0.0, am_rate=0.0, phase=0.0):
    """Band-limited wavetable oscillator with optional exponential glide, vibrato and tremolo."""
    top = max(freq, f_end or freq)
    harm = tuple((k, a) for k, a in harm if k * top < SR * 0.45) or SINE
    tb = _table(harm)
    n = n_of(dur)
    out = [0.0] * n
    ph = phase
    ratio = (f_end / freq) if f_end else 1.0
    for i in range(n):
        t = i / SR
        f = freq * ratio ** (t / dur) if f_end else freq
        if vib:
            depth = vib
            if vib_delay:
                depth *= min(1.0, max(0.0, (t - vib_delay) / 0.15))
            f *= 1.0 + depth * math.sin(TWO_PI * vib_rate * t)
        ph += f / SR
        ph -= int(ph)
        x = ph * _TN
        j = int(x)
        v = tb[j] + (tb[j + 1] - tb[j]) * (x - j)
        if am:
            v *= 1.0 - am * 0.5 * (1.0 + math.sin(TWO_PI * am_rate * t))
        out[i] = v
    return out


def partials(freq, dur, parts):
    """Sum of decaying sine partials: parts = [(ratio, amp, decay_per_sec)] — bells, marimba."""
    n = n_of(dur)
    out = [0.0] * n
    for ratio, amp, decay in parts:
        f = freq * ratio
        if f >= SR * 0.45:
            continue
        w = TWO_PI * f / SR
        k = math.exp(-decay / SR)
        a = amp
        for i in range(n):
            out[i] += a * math.sin(w * i)
            a *= k
    return out


def noise(dur):
    return [rng.uniform(-1.0, 1.0) for _ in range(n_of(dur))]


# ---------------------------------------------------------------- envelopes

def env_adsr(n, a=0.005, d=0.05, s=0.7, r=0.05):
    A, D, R = max(1, n_of(a)), max(1, n_of(d)), max(1, n_of(r))
    e = [0.0] * n
    for i in range(n):
        if i < A:
            v = i / A
        elif i < A + D:
            v = 1.0 - (1.0 - s) * (i - A) / D
        else:
            v = s
        if i > n - R:
            v *= max(0.0, (n - i) / R)
        e[i] = v
    return e


def env_exp(n, a=0.003, decay=8.0, r=0.01):
    """Fast attack, exponential decay, short release at the very end."""
    A, R = max(1, n_of(a)), max(1, n_of(r))
    e = [0.0] * n
    for i in range(n):
        v = i / A if i < A else math.exp(-decay * (i - A) / SR)
        if i > n - R:
            v *= max(0.0, (n - i) / R)
        e[i] = v
    return e


def env_bell(n, peak=0.5):
    """Smooth rise and fall (raised sine), peak position as fraction of the length."""
    P = max(1, int(n * peak))
    e = [0.0] * n
    for i in range(n):
        if i < P:
            e[i] = 0.5 - 0.5 * math.cos(math.pi * i / P)
        else:
            e[i] = 0.5 + 0.5 * math.cos(math.pi * (i - P) / max(1, n - P))
    return e


# ---------------------------------------------------------------- filters / fx

def lowpass(x, fc):
    """One-pole low-pass. fc is Hz or a function of time (s) -> Hz."""
    out = [0.0] * len(x)
    y = 0.0
    if callable(fc):
        for i, v in enumerate(x):
            a = 1.0 - math.exp(-TWO_PI * min(fc(i / SR), SR * 0.45) / SR)
            y += a * (v - y)
            out[i] = y
    else:
        a = 1.0 - math.exp(-TWO_PI * fc / SR)
        for i, v in enumerate(x):
            y += a * (v - y)
            out[i] = y
    return out


def highpass(x, fc):
    lp = lowpass(x, fc)
    return [a - b for a, b in zip(x, lp)]


def bandpass(x, lo, hi):
    return highpass(lowpass(x, hi), lo)


def echo(x, delay, fb=0.3, mix=0.25, damp=3500.0):
    d = n_of(delay)
    buf = [0.0] * len(x)
    a = 1.0 - math.exp(-TWO_PI * damp / SR)
    lp = 0.0
    out = [0.0] * len(x)
    for i, v in enumerate(x):
        dl = buf[i - d] if i >= d else 0.0
        lp += a * (dl - lp)
        buf[i] = v + lp * fb
        out[i] = v + lp * mix
    return out


def reverb(x, wet=0.18, room=0.78, damp=0.35):
    """Small Freeverb-style reverb: 4 damped combs + 2 allpasses."""
    combs = [557, 593, 641, 677]
    acc = [0.0] * len(x)
    for d in combs:
        buf = [0.0] * d
        idx = 0
        f = 0.0
        for i, v in enumerate(x):
            o = buf[idx]
            f = o * (1.0 - damp) + f * damp
            buf[idx] = v + f * room
            idx += 1
            if idx == d:
                idx = 0
            acc[i] += o
    for d in (278, 171):
        buf = [0.0] * d
        idx = 0
        for i, v in enumerate(acc):
            b = buf[idx]
            o = -v + b
            buf[idx] = v + b * 0.5
            idx += 1
            if idx == d:
                idx = 0
            acc[i] = o
    return [v + a * wet * 0.25 for v, a in zip(x, acc)]


def periodic(fx, x):
    """Applies an effect to a loop so its tail wraps seamlessly: process two copies, keep the 2nd."""
    L = len(x)
    return fx(x + x)[L:]


def soft_clip(x, drive=1.3):
    peak = max(abs(v) for v in x) or 1.0
    k = math.tanh(drive)
    return [math.tanh(drive * v / peak) / k for v in x]


# ---------------------------------------------------------------- finishing / io

def finalize(x, peak_db, fade_in=0.003, fade_out=0.005):
    m = sum(x) / len(x)
    x = [v - m for v in x]
    fi, fo = n_of(fade_in), n_of(fade_out)
    n = len(x)
    for i in range(min(fi, n)):
        x[i] *= i / fi
    for i in range(min(fo, n)):
        x[n - 1 - i] *= i / fo
    peak = max(abs(v) for v in x) or 1.0
    g = 10 ** (peak_db / 20.0) / peak
    return [v * g for v in x]


def write(name, x):
    path = os.path.join(OUT, name + '.wav')
    data = bytearray()
    for v in x:
        v = max(-1.0, min(1.0, v))
        data += struct.pack('<h', int(round(v * 32767)))
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(bytes(data))


def sfx(name, x, peak_db=-3.0):
    write(name, finalize(x, peak_db))


# ---------------------------------------------------------------- drum voices

def kick(dur=0.32, f0=150.0, f1=46.0, decay=9.0):
    body = mul(osc(f0, dur, SINE, f_end=f1), env_exp(n_of(dur), 0.002, decay))
    click = mul(highpass(noise(0.012), 2000), env_exp(n_of(0.012), 0.0005, 300))
    return add(body, click, 0, 0.35)


def snare(dur=0.18, tone=190.0, bright=1800.0):
    n = n_of(dur)
    nz = mul(highpass(noise(dur), bright), env_exp(n, 0.001, 22))
    tn = mul(osc(tone, dur, SINE, f_end=tone * 0.8), env_exp(n, 0.001, 30))
    return [a * 0.8 + b * 0.5 for a, b in zip(nz, tn)]


def hat(dur=0.05, decay=70.0, cutoff=7000.0):
    return mul(highpass(noise(dur), cutoff), env_exp(n_of(dur), 0.0005, decay))


def shaker(dur=0.07):
    return mul(bandpass(noise(dur), 4000, 9000), env_bell(n_of(dur), 0.3))


def rim(dur=0.06):
    n = n_of(dur)
    t = mul(osc(1700, dur, TRI), env_exp(n, 0.0005, 70))
    c = mul(highpass(noise(dur), 3000), env_exp(n, 0.0005, 120))
    return [a * 0.6 + b * 0.4 for a, b in zip(t, c)]


# ---------------------------------------------------------------- instrument voices

def pluck(freq, note_len, ring=0.9, bright=7.0, decay=3.6, vib=0.003):
    total = min(note_len + 0.25, ring)
    n = n_of(total)
    x = osc(freq, total, SAW, vib=vib, vib_rate=5.5, vib_delay=0.18)
    x = lowpass(x, lambda t: freq * (1.6 + bright * math.exp(-t * 11.0)))
    e = env_exp(n, 0.003, decay, r=0.04)
    cut = n_of(note_len)
    for i in range(cut, n):
        e[i] *= max(0.0, 1.0 - (i - cut) / n_of(0.14))
    sub = mul(osc(freq, total, SINE), e)
    return [a * b * 0.85 + s * 0.25 for a, b, s in zip(x, e, sub)]


def marimba(freq, dur=0.7):
    x = partials(freq, dur, [(1.0, 1.0, 5.0), (3.93, 0.32, 22.0), (9.8, 0.08, 45.0)])
    hit = mul(lowpass(noise(0.01), 3000), env_exp(n_of(0.01), 0.0005, 300))
    x = add(x, hit, 0, 0.15)
    return mul(x, env_exp(len(x), 0.002, 0.5, r=0.05))


def bell(freq, dur=0.5, decay=6.0):
    return partials(freq, dur, [(1.0, 1.0, decay), (2.0, 0.45, decay * 1.6),
                                (3.01, 0.25, decay * 2.4), (4.17, 0.12, decay * 3.2)])


def pad_note(freq, dur, attack=0.35, release=0.45):
    a = osc(freq * 1.0017, dur, SAW, vib=0.002, vib_rate=4.2)
    b = osc(freq / 1.0017, dur, SAW, vib=0.002, vib_rate=3.7, phase=0.37)
    x = lowpass([(p + q) * 0.5 for p, q in zip(a, b)], 1100)
    return mul(x, env_adsr(len(x), attack, 0.3, 0.8, release))


def bass_note(freq, dur, cutoff=700.0):
    x = lowpass(osc(freq, dur, BASS), cutoff)
    return mul(x, env_adsr(len(x), 0.006, 0.12, 0.65, 0.05))


# ================================================================= SFX

def make_sfx():
    # ui_click: bubbly pop
    d = 0.07
    x = mul(osc(520, d, SOFT, f_end=1150), env_exp(n_of(d), 0.002, 45))
    sfx('ui_click', x)

    # ui_open: rising swoosh + chime
    d = 0.26
    sw = lowpass(noise(d), lambda t: 400 + 5000 * (t / d) ** 1.5)
    sw = mul(highpass(sw, 300), env_bell(n_of(d), 0.55))
    x = scale(sw, 0.6)
    add(x, mul(bell(midi(88), 0.14, 14), env_exp(n_of(0.14), 0.002, 4)), 0.10, 0.45)
    add(x, mul(bell(midi(95), 0.12, 16), env_exp(n_of(0.12), 0.002, 4)), 0.15, 0.4)
    sfx('ui_open', x)

    # ui_close: falling swoosh + soft tone
    d = 0.2
    sw = lowpass(noise(d), lambda t: 4500 * (1 - t / d) ** 1.4 + 300)
    sw = mul(highpass(sw, 250), env_bell(n_of(d), 0.3))
    x = scale(sw, 0.6)
    add(x, mul(osc(720, 0.16, SOFT, f_end=340), env_exp(n_of(0.16), 0.003, 14)), 0.0, 0.5)
    sfx('ui_close', x)

    # tap: tiny tick
    d = 0.05
    x = mul(highpass(noise(d), 3500), env_exp(n_of(d), 0.0005, 160))
    add(x, mul(osc(2100, d, SINE, f_end=1700), env_exp(n_of(d), 0.0005, 90)), 0, 0.6)
    sfx('tap', x)

    # bump: rubbery thud + small boing
    d = 0.22
    th = mul(osc(150, d, SOFT, f_end=55), env_exp(n_of(d), 0.002, 16))
    boing = osc(310, d, SINE, vib=0.18, vib_rate=19)
    boing = mul(boing, env_exp(n_of(d), 0.004, 14))
    knock = mul(lowpass(noise(0.03), 1500), env_exp(n_of(0.03), 0.0005, 120))
    x = [a * 0.9 + b * 0.35 for a, b in zip(th, boing)]
    add(x, knock, 0, 0.5)
    sfx('bump', x)

    # engine_go: revving vroom with pitch rise
    d = 0.45
    eng = osc(68, d, SAW, f_end=175, am=0.55, am_rate=32)
    eng = lowpass(eng, lambda t: 500 + 1800 * (t / d))
    rumble = lowpass(noise(d), 380)
    x = [a + b * 0.6 for a, b in zip(eng, rumble)]
    x = mul(x, env_adsr(n_of(d), 0.03, 0.15, 0.85, 0.12))
    sfx('engine_go', x)

    # park: brake squeak + thunk
    x = zeros(0.26)
    sq = mul(osc(2300, 0.12, SOFT, f_end=1850, vib=0.012, vib_rate=38), env_bell(n_of(0.12), 0.25))
    add(x, sq, 0, 0.35)
    th = mul(osc(125, 0.11, SOFT, f_end=65), env_exp(n_of(0.11), 0.001, 26))
    add(x, th, 0.13, 0.9)
    add(x, mul(lowpass(noise(0.05), 900), env_exp(n_of(0.05), 0.0005, 80)), 0.13, 0.5)
    sfx('park', x)

    # board pops: three rising pitches
    for idx, f in enumerate((520, 620, 740), start=1):
        d = 0.09
        e = env_exp(n_of(d), 0.002, 32)
        x = mul(osc(f, d, SOFT, f_end=f * 1.7), e)
        sfx('board_pop%d' % idx, x)

    # vehicle_full: bright 3-note ding
    x = zeros(0.42)
    for k, m in enumerate((84, 88, 91)):
        add(x, bell(midi(m), 0.42 - k * 0.08, 7), k * 0.08, 0.6)
    sfx('vehicle_full', reverb(x, 0.25))

    # depart_horn: cartoon beep-beep
    x = zeros(0.42)
    for at, ln in ((0.0, 0.15), (0.2, 0.2)):
        a = osc(440, ln, REED, f_end=452)
        b = osc(554, ln, REED, f_end=566)
        h = lowpass([p + q for p, q in zip(a, b)], 2600)
        add(x, mul(h, env_adsr(len(h), 0.01, 0.04, 0.85, 0.03)), at, 0.5)
    sfx('depart_horn', x)

    # coin: two-tone ping
    x = zeros(0.2)
    c1 = mul(osc(988, 0.06, SQUARE), env_adsr(n_of(0.06), 0.002, 0.02, 0.8, 0.005))
    c2 = mul(osc(1319, 0.14, SQUARE), env_exp(n_of(0.14), 0.002, 14))
    add(x, lowpass(c1, 6000), 0, 0.45)
    add(x, lowpass(c2, 6000), 0.055, 0.45)
    sfx('coin', x)

    # coin_burst: cascade of coin pings
    x = zeros(0.75)
    for k in range(9):
        at = k * 0.06 + rng.uniform(-0.01, 0.01)
        f = 988 * 2 ** (rng.choice((0, 2, 4, 5, 7)) / 12.0)
        p = mul(osc(f * 1.335, 0.13, SQUARE), env_exp(n_of(0.13), 0.002, 16))
        add(x, lowpass(p, 6000), max(0.0, at), 0.3 * (1 - k * 0.04))
    sfx('coin_burst', reverb(x, 0.3))

    # stars: sparkly chimes, rising
    for idx, m in enumerate((84, 88, 91), start=1):
        x = zeros(0.42)
        add(x, bell(midi(m), 0.42, 6.5), 0, 0.7)
        for k in range(4):
            sp = mul(osc(rng.uniform(2600, 4200), 0.06, SINE), env_exp(n_of(0.06), 0.001, 50))
            add(x, sp, 0.02 + k * 0.05 + rng.uniform(0, 0.02), 0.18)
        sfx('star%d' % idx, reverb(x, 0.3))

    # win_fanfare: major arpeggio + held chord + drums
    d = 2.0
    x = zeros(d)
    beat = 0.12
    for k, m in enumerate((72, 76, 79, 84)):
        n = mul(lowpass(osc(midi(m), 0.2, BRASS, vib=0.004), 3500),
                env_adsr(n_of(0.2), 0.015, 0.05, 0.75, 0.05))
        add(x, n, k * beat, 0.32)
    for m in (72, 76, 79, 84):
        n = osc(midi(m), 1.4, BRASS, vib=0.007, vib_rate=5.5, vib_delay=0.25)
        n = mul(lowpass(n, 3000), env_adsr(len(n), 0.02, 0.2, 0.7, 0.5))
        add(x, n, 0.55, 0.2)
    add(x, kick(), 0.0, 0.6)
    add(x, kick(), 0.36, 0.5)
    add(x, kick(), 0.55, 0.7)
    crash = mul(highpass(noise(1.3), 4000), env_exp(n_of(1.3), 0.002, 3.2))
    add(x, crash, 0.55, 0.22)
    for k in range(4):
        add(x, snare(), 0.36 + k * 0.045, 0.12 + k * 0.05)
    for k in range(6):
        add(x, bell(midi(96 + rng.choice((0, 4, 7))), 0.3, 9), 0.6 + k * 0.11, 0.08)
    sfx('win_fanfare', reverb(x, 0.35))

    # fail: sad descending wah-wah
    x = zeros(1.55)
    notes = ((0.0, 0.3, 67), (0.33, 0.3, 66), (0.66, 0.3, 65), (1.0, 0.55, 64))
    for at, ln, m in notes:
        last = m == 64
        f = midi(m - 12)
        tone = osc(f, ln, SAW, f_end=f * (0.94 if last else 0.98),
                   vib=0.02 if last else 0.0, vib_rate=6.0, vib_delay=0.1)
        tone = lowpass(tone, lambda t, ln=ln: 250 + 1500 * math.sin(math.pi * min(1.0, t / ln)) ** 1.5)
        tone = mul(tone, env_adsr(len(tone), 0.03, 0.05, 0.9, 0.08))
        add(x, tone, at, 0.6)
    sfx('fail', reverb(x, 0.25))

    # unlock: click + magic shimmer
    x = zeros(0.62)
    add(x, mul(highpass(noise(0.03), 2500), env_exp(n_of(0.03), 0.0005, 140)), 0, 0.6)
    add(x, mul(osc(1400, 0.04, SINE, f_end=900), env_exp(n_of(0.04), 0.0005, 80)), 0, 0.4)
    penta = (84, 86, 88, 91, 93, 96, 98, 100, 103)
    for k, m in enumerate(penta):
        add(x, bell(midi(m), 0.3, 10), 0.04 + k * 0.035, 0.22)
    sw = mul(highpass(lowpass(noise(0.5), lambda t: 1500 + 6000 * t), 1200), env_bell(n_of(0.5), 0.6))
    add(x, sw, 0.05, 0.12)
    sfx('unlock', reverb(x, 0.35))

    # whoosh
    d = 0.35
    w = lowpass(noise(d), lambda t: 300 + 4200 * math.sin(math.pi * t / d) ** 2)
    w = mul(highpass(w, 200), env_bell(n_of(d), 0.45))
    sfx('whoosh', w)

    # booster: rising power-up sweep
    d = 0.6
    s = osc(200, d, SAW, f_end=1250, vib=0.03, vib_rate=14)
    s = lowpass(s, lambda t: 800 + 4000 * t / d)
    s = mul(s, env_adsr(n_of(d), 0.02, 0.1, 0.8, 0.15))
    x = scale(s, 0.55)
    add(x, bell(midi(96), 0.25, 12), 0.42, 0.4)
    add(x, bell(midi(100), 0.2, 12), 0.47, 0.3)
    sfx('booster', reverb(x, 0.25))

    # crane: whirr + servo whine + metallic clank
    x = zeros(0.82)
    wh = osc(90, 0.6, SQUARE, f_end=140, am=0.6, am_rate=30)
    wh = mul(lowpass(wh, 900), env_adsr(n_of(0.6), 0.05, 0.1, 0.9, 0.1))
    add(x, wh, 0, 0.5)
    sv = mul(osc(600, 0.6, SOFT, f_end=900), env_adsr(n_of(0.6), 0.08, 0.1, 0.7, 0.1))
    add(x, sv, 0, 0.12)
    clank = partials(380, 0.22, [(1.0, 1.0, 22), (2.43, 0.7, 28), (3.91, 0.5, 34), (5.37, 0.35, 40)])
    add(x, clank, 0.6, 0.45)
    add(x, mul(highpass(noise(0.05), 1500), env_exp(n_of(0.05), 0.0005, 90)), 0.6, 0.4)
    sfx('crane', reverb(x, 0.2))

    # warning: two soft alert beeps
    x = zeros(0.4)
    for at in (0.0, 0.2):
        b = osc(880, 0.13, TRI)
        add(x, mul(b, env_adsr(n_of(0.13), 0.012, 0.03, 0.8, 0.04)), at, 0.6)
    sfx('warning', x)


# ================================================================= MUSIC

def render_song(bpm, bars, chords, melody, lead, bass_pattern, drums, pad=False,
                stab=True, lead_gain=0.5, echo_beats=0.75):
    """chords: per-bar list of midi chord tones (one entry per bar in a cycle);
    melody: list of (beat, midi, dur_beats) across the whole song."""
    beat = 60.0 / bpm
    L = n_of(bars * 4 * beat)
    lead_bus = [0.0] * L
    rest = [0.0] * L

    for b, m, ln in melody:
        add(lead_bus, lead(midi(m), ln * beat), b * beat, lead_gain, wrap=True)

    for bar in range(bars):
        ch = chords[bar % len(chords)]
        t0 = bar * 4 * beat
        root = ch[0]
        for pb, interval, ln in bass_pattern:
            f = midi(root - 24 + interval)
            add(rest, bass_note(f, ln * beat), t0 + pb * beat, 0.42, wrap=True)
        if pad:
            for m in ch:
                add(rest, pad_note(midi(m - 12), 4 * beat + 0.4), t0, 0.07, wrap=True)
        if stab:
            for off in (0.5, 1.5, 2.5, 3.5):
                for m in ch:
                    s = mul(lowpass(osc(midi(m - 12), 0.22, SOFT), 2200),
                            env_exp(n_of(0.22), 0.004, 14))
                    add(rest, s, t0 + off * beat, 0.07, wrap=True)
        drums(rest, bar, t0, beat)

    lead_bus = periodic(lambda s: echo(s, echo_beats * beat, 0.32, 0.28), lead_bus)
    mix = [a + b for a, b in zip(lead_bus, rest)]
    mix = periodic(lambda s: reverb(s, 0.22), mix)
    mix = soft_clip(mix, 1.1)
    m = sum(mix) / len(mix)
    mix = [v - m for v in mix]
    peak = max(abs(v) for v in mix)
    g = 10 ** (-8.0 / 20.0) / peak
    return [v * g for v in mix]


def make_music():
    # --- music_home: C major, 112 BPM, C | G | Am | F x3 (12 bars)
    C, G, Am, F = (72, 76, 79), (67, 71, 74), (69, 72, 76), (65, 69, 72)
    cycle_a = [(0, 76, .5), (.5, 79, .5), (1, 84, 1), (2, 79, .5), (2.5, 76, .5), (3, 79, 1),
               (4, 83, .5), (4.5, 81, .5), (5, 79, 1), (6, 74, .5), (6.5, 79, .5), (7, 83, 1),
               (8, 84, .5), (8.5, 83, .5), (9, 81, 1), (10, 76, .5), (10.5, 81, .5), (11, 84, 1),
               (12, 81, .5), (12.5, 79, .5), (13, 77, .5), (13.5, 81, .5), (14, 79, 2)]
    cycle_a2 = cycle_a[:-5] + [(12, 81, .5), (12.5, 84, .5), (13, 86, .5), (13.5, 84, .5), (14, 84, 2)]
    cycle_b = [(0, 84, .5), (.5, 84, .5), (1, 86, .5), (1.5, 88, 1), (2.5, 86, .5), (3, 84, 1),
               (4, 83, .5), (4.5, 83, .5), (5, 84, .5), (5.5, 86, 1), (6.5, 83, .5), (7, 79, 1),
               (8, 81, .5), (8.5, 83, .5), (9, 84, .5), (9.5, 88, 1), (10.5, 86, .5), (11, 84, 1),
               (12, 81, 1), (13, 84, .5), (13.5, 81, .5), (14, 79, 2)]
    melody = []
    for ci, cyc in enumerate((cycle_a, cycle_a2, cycle_b)):
        melody += [(b + ci * 16, m, ln) for b, m, ln in cyc]
    home_bass = [(0, 0, .45), (.75, 0, .2), (1.5, 12, .45), (2, 0, .45), (2.75, 0, .2), (3.5, 7, .45)]

    def home_drums(bus, bar, t0, beat):
        for b in (0, 2):
            add(bus, kick(), t0 + b * beat, 0.55, wrap=True)
        if bar % 2 == 1:
            add(bus, kick(), t0 + 2.5 * beat, 0.35, wrap=True)
        for b in (1, 3):
            add(bus, snare(), t0 + b * beat, 0.2, wrap=True)
        for k in range(8):
            open_hat = (bar % 4 == 3 and k == 7)
            h = hat(0.25, 12) if open_hat else hat()
            add(bus, h, t0 + k * 0.5 * beat, 0.07 if k % 2 == 0 else 0.11, wrap=True)

    home = render_song(112, 12, [C, G, Am, F], melody, pluck, home_bass, home_drums,
                       pad=False, stab=True, lead_gain=0.5)
    write('music_home', home)

    # --- music_game: F major, 100 BPM, F | Dm | Bb | C x3 (12 bars), marimba + pad
    Fc, Dm, Bb, Cc = (65, 69, 72), (62, 65, 69), (70, 74, 77), (72, 76, 79)
    ga = [(0, 72, .5), (.5, 77, .5), (1.5, 81, 1), (3, 79, .5), (3.5, 77, .5),
          (4, 74, .5), (4.5, 77, .5), (5.5, 81, .5), (6, 79, 1), (7, 77, 1),
          (8, 74, .5), (8.5, 77, .5), (9.5, 82, 1), (11, 81, .5), (11.5, 79, .5),
          (12, 76, .5), (12.5, 79, .5), (13.5, 84, 1), (14.5, 82, .5), (15, 79, 1)]
    gb = [(0, 81, 1), (1, 79, .5), (1.5, 77, .5), (2, 72, 1), (3.5, 77, .5),
          (4, 81, 1), (5, 79, .5), (5.5, 77, .5), (6, 74, 1.5), (7.5, 76, .5),
          (8, 77, 1), (9, 74, .5), (9.5, 77, .5), (10, 82, 1), (11, 81, 1),
          (12, 79, .5), (12.5, 76, .5), (13, 72, 1), (14, 76, .5), (14.5, 79, .5), (15, 84, .5), (15.5, 82, .5)]
    gmel = []
    for ci, cyc in enumerate((ga, gb, ga)):
        gmel += [(b + ci * 16, m, ln) for b, m, ln in cyc]
    game_bass = [(0, 0, .9), (1.5, 0, .4), (2, 0, .9), (3.5, 7, .4)]

    def game_drums(bus, bar, t0, beat):
        add(bus, kick(0.3, 120, 45, 11), t0, 0.4, wrap=True)
        add(bus, kick(0.3, 120, 45, 11), t0 + 2 * beat, 0.3, wrap=True)
        add(bus, rim(), t0 + 3 * beat, 0.12, wrap=True)
        if bar % 2 == 1:
            add(bus, rim(), t0 + 1 * beat, 0.08, wrap=True)
        for k in range(8):
            add(bus, shaker(), t0 + k * 0.5 * beat, 0.035 if k % 2 == 0 else 0.06, wrap=True)

    game = render_song(100, 12, [Fc, Dm, Bb, Cc], gmel,
                       lambda f, ln: marimba(f, max(0.5, ln + 0.3)), game_bass, game_drums,
                       pad=True, stab=False, lead_gain=0.55, echo_beats=0.75)
    write('music_game', game)


# ================================================================= verify

EXPECTED = ['ui_click', 'ui_open', 'ui_close', 'tap', 'bump', 'engine_go', 'park',
            'board_pop1', 'board_pop2', 'board_pop3', 'vehicle_full', 'depart_horn', 'coin',
            'coin_burst', 'star1', 'star2', 'star3', 'win_fanfare', 'fail', 'unlock', 'whoosh',
            'booster', 'crane', 'warning', 'music_home', 'music_game']


def verify():
    total = 0
    ok = True
    for name in EXPECTED:
        path = os.path.join(OUT, name + '.wav')
        if not os.path.exists(path):
            print('MISSING', name)
            ok = False
            continue
        with wave.open(path, 'rb') as w:
            assert w.getnchannels() == 1 and w.getsampwidth() == 2 and w.getframerate() == SR
            frames = w.readframes(w.getnframes())
            n = w.getnframes()
        s = struct.unpack('<%dh' % n, frames)
        peak = max(abs(v) for v in s) / 32767.0
        db = 20 * math.log10(peak) if peak > 0 else -999
        size = os.path.getsize(path)
        total += size
        extra = ''
        if name.startswith('music'):
            jump = abs(s[-1] - s[0]) / 32767.0
            extra = '  loop: first=%.4f last=%.4f jump=%.4f' % (s[0] / 32767.0, s[-1] / 32767.0, jump)
        clip = sum(1 for v in s if abs(v) >= 32767)
        if clip:
            ok = False
        print('%-14s %6.3fs  peak %6.2f dBFS  %7.1f KB  clipped=%d%s'
              % (name, n / SR, db, size / 1024, clip, extra))
    print('TOTAL %.1f KB (%d files)  %s' % (total / 1024, len(EXPECTED), 'OK' if ok else 'PROBLEMS'))


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    make_sfx()
    make_music()
    verify()

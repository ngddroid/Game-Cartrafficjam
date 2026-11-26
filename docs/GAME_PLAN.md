# Traffic Jam Rush — single-phase build plan

This is an original color-sorting parking-jam game for Android, written in Jetpack Compose. It uses the same genre and core mechanics as the reference (*Car Jam Solver*), but its name, logo, mascot, art, sounds, text and levels are all our own. Nothing is copied from the reference app.

Reference screenshots captured from the device (Samsung SM-A076B, 720×1600) are in `docs/reference/`.

---

## 1. Reference analysis (what we observed)

| Area | Observation | Our take |
|---|---|---|
| **Splash** | Publisher logo on a blue screen, then a key-art screen with a game logo, a yellow bus, a loading bar and "600+ levels". About 4 s in total (`15_splash.jpg`). | Android 12 system splash, then an animated splash: logo drops in, a bus drives across a road, the loading bar fills, and the screen shows "5000 Levels". |
| **Screen layout (gameplay)** | Top: station scene with a train, queue counter board ("404 Queue") and passengers in a snake line. Middle: 4 parking slots + 3 locked "Unlock (+)" slots, then a road strip. Bottom: the jam lot. Footer: 3 boosters (Refresh, Eliminate/VIP, Sort). | Same 3-zone layout, with scene art for each collection theme. |
| **Vehicles** | Car = 4 seats, van = 6, bus = 10 (the level 1 tutorial text says so). Each has a white arrow on its roof. The lot is a grid rotated about 30°, and vehicles sit along the grid axes (`08_lot_closeup.jpg`). | Same seat counts. The grid is rotated 0° in early levels and 15–35° later. Boards come in shapes: rectangle, circle, heart, diamond, flower, rings. |
| **Tap: free** | The vehicle shoots out along its arrow with a smoke puff, drives along the road to the first free slot and parks (`09_vehicle_exit.jpg`). | Ease-in launch, follows a road path, then rotates into the slot. |
| **Tap: blocked** | Tap ring, the vehicle lurches forward until it touches the blocker, then springs back (`10_blocked_bump.jpg`). | Lurch to the contact point, a spring return with overshoot, a shake on the blocker, a bump sound and a light haptic. |
| **Boarding** | Passengers at the front of the queue whose color matches a parked vehicle walk in one by one. Mini figures appear on the roof. When the vehicle is full it drives off and the slot frees up. The queue counter drops. | Same, plus a pop sound for each passenger and a "full" chime. |
| **Queue** | The front of the queue is at the left. The line snakes to the right and then up a column by the train door. | Same snake path, with the path shape depending on the theme. |
| **Win** | "Perfect!" ribbon with a vehicle and sparks, then **Victory**: +10 coins, league table, a "Get ×3/×4/×5" ad multiplier that cycles, "New Feature 1/3" progress, and a **Next** button (`03_victory.jpg`). | Victory: 1–3 stars, a coin burst flying to the counter, ×2 coins via ad, Next / Home. |
| **Fail** | Slots full → **Revive** dialog ("watch ad to revive and unlock a parking spot", or 120 coins), then **Fail** (crying face, Replay / Skip via ad) (`13_revive.jpg`, `14_fail.jpg`). | Same flow: Revive (ad / coins), then Fail (Retry / Skip via ad). |
| **Boosters** | "Prop" dialog with an icon, a description, a coin price (60–80) or **Free** with an ad. Refresh = shuffle lot colors, Eliminate = send any vehicle to a VIP spot, Sort = reorder the queue by parked colors (`11`, `12`). | Shuffle / Crane (VIP) / Sort with the same coin-or-ad dialog. |
| **Locked slots** | 3 extra slots marked "Unlock +", each unlocked by an ad. | Same (per level). |
| **Difficulty** | A "Difficulty Soars" banner on hard levels. Level 2 already has about 60 vehicles and 404 passengers. | A "Hard Level" / "Super Hard" banner every 5th / 10th level, with a smoother difficulty curve. |
| **Home** | Avatar, coins, gear, a feature-unlock bar, side event icons locked by level, a "Car Tour" event button, a big pulsing **Level N** button, and a 5-tab bottom bar (Tasks, Collection, Home, Skin, Leaderboard) (`04_home.jpg`, `05_bottom_tabs.jpg`). | Home with a big Play button and a 5-tab bottom bar (see §3). |
| **Settings** | A modal with Sound / Music / Vibration toggles, Sign-in, Language, Share, Feedback, and Privacy Policy / User Agreement links (`06_settings.jpg`). | A full Settings screen styled like a game panel, plus custom HTML Privacy and Terms pages. |
| **Back press** | A system "Do you want to leave?" dialog. | A game-styled exit dialog. |

---

## 2. What we build (scope)

1. **Splash**: system splash (`core-splashscreen`), then an animated Compose splash (about 2.5 s).
2. **Onboarding**: 4 pages, **no Skip button**. Each page has a **Next** button, and the last one has **Let's Go**. It shows only once.
   1. *Tap to drive*: an animated mini-lot where a car slides out along its arrow.
   2. *Match colors*: passengers walk into a same-colored van. Shows the seat counts (4 / 6 / 10).
   3. *Don't jam the slots*: slots fill up, then a booster saves the day.
   4. *5000 levels · 50 collections*: themed world cards fan in, followed by **Let's Go**.
3. **Home** (center tab): animated sky and city backdrop with clouds and cars driving by, logo, coin pill, mascot avatar (an original traffic-cone character, "Coney"), a current-collection card with progress, a daily reward chest, and a big pulsing **Level N** play button.
4. **Bottom bar** (custom game-style, with a raised center Home tab): **Levels · Collections · Home · Garage · Settings**.
5. **Levels screen**: a winding road map of **5000 level nodes** (lazy), grouped by collection headers. Nodes are completed (with stars), current (bouncing), or locked (padlock). Tapping a locked node opens the **Watch Ad** dialog, and **Watch Ad** unlocks it.
6. **Collections screen**: **50 collections × 100 levels**, each a themed world card with art, a name, progress, and a lock. Tapping a locked collection opens the **Watch Ad** dialog, which unlocks the collection and its first level.
7. **Garage**: vehicle skin sets (Classic, Candy, Metallic, Neon, Retro, Safari). Classic is free. Others unlock with coins or an ad.
8. **Settings screen**: Sound, Music and Vibration (custom toggles), Rate Us, Share, More Games, **Privacy Policy**, **Terms of Service**, Reset Progress (with confirm), and the version number.
9. **Privacy Policy / Terms**: `app/src/main/assets/privacy_policy.html` and `terms_of_service.html`. These are fully custom, game-themed HTML pages (road-stripe header, rounded cards, cartoon colors, embedded CSS, offline). They open in a styled in-app WebView screen with a game top bar.
10. **Gameplay** with the full mechanics, boosters, revive, win and fail flows.
11. **Ads hook**: an `AdManager` interface. Today a `FakeAdManager` shows a short full-screen "Ad" countdown, then grants the reward. AdMob can be swapped in later without touching the UI.

### Lock rules (as requested)

- On first install only **Level 1** and **Collection 1** are unlocked.
- Winning a level unlocks the next level. Finishing the last level of a collection unlocks the next collection.
- Tapping any locked level or collection opens the **Watch Ad** dialog. **Watch Ad** runs the ad stub and then unlocks it.
- Unlocking a collection by ad also unlocks its first level.

> **Assumption:** "first collection" means the first group of 100 levels (a themed world). If you meant a car or card collection like the reference's Collection tab, tell me before I start.

---

## 3. Visual design system (high-quality "looks like a game")

- **Fonts**: bundled OFL fonts. **Lilita One** (chunky titles, about 40 KB) and **Fredoka** (UI text, about 100 KB per weight), from the Google Fonts GitHub repo. Titles get a stroke outline and a drop shadow, like a game logo.
- **Palette**: sky blue `#4FC3F7` → `#1E88E5`, sunny yellow `#FFC107`, play orange `#FF9800`, grass green `#7CB342`, candy pink, deep navy panels `#1A3A6B`. Every theme has its own ground, lot and decor colors.
- **Components** (all custom, not stock Material):
  - `GameButton`: a 3D pill with a darker bottom lip, an inner gloss highlight, a press-down squash animation, and a click sound plus haptic.
  - `GamePanel`: a dialog card with a thick colored border, a ribbon title banner and a red round close button.
  - `CoinPill`, `LevelBadge`, `RibbonBanner`, `StarRow`, `GameToggle` (sliding knob with ON/OFF), `LockBadge`, `ProgressBar` (striped and animated).
  - The bottom bar has a raised center tab, an icon bounce on select, and a glow on the selected tab.
- **Icons**: hand-made vector drawables (home, map, collection, garage, gear, coin, lock, booster icons). No icon fonts.
- **App icon**: an adaptive vector icon (a yellow bus with a white arrow on a road and sky background).

### Game art (drawn in code, cached as bitmaps)

There are no 3D engines or downloaded sprites. All art is drawn on a Compose `Canvas` in a pseudo-3D top-down style:

- **Vehicles**: soft drop shadow, a body extrusion (side skirt in a darker shade), a body gradient, a lighter roof panel, glass windows with a glare streak, head and tail lights, wheel hints, and a white roof arrow with a shadow. Car, van and bus have distinct proportions, and the bus has a row of windows. Each type × color × skin is pre-rendered once into an `ImageBitmap`, then drawn rotated and translated, which holds 60 fps with 90+ vehicles.
- **Mystery vehicles** (level 25 and up): gray with a "?" until their path is clear, then they flip to their color.
- **Passengers**: jelly-bean figures (head plus capsule body with a gradient and a shadow), an idle bob, a hop-walk when boarding, and mini figures on the roof once seated.
- **Scenes**: 10 themes that repeat across 50 collections in 5 tiers. The themes are City Station, Sunny Zoo, Candy Fair, Ocean Pier, Desert Dunes, Snowy Peak, Neon Night, Autumn Farm, Jungle Camp and Space Port. Each has its own top backdrop (train or building or tent shapes, trees, lamps, benches), slot colors and lot ground.
- **FX** (a lightweight particle system): smoke puffs on launch and departure, tap ripple rings, bump sparks with a micro screen-shake, confetti and starburst on win, coins flying to the counter, a glow pulse on a newly freed slot, the "Hard Level" banner sweep, and the "Perfect!" ribbon.

---

## 4. Gameplay rules (engine)

- **Board**: a W×H cell grid with a shape mask, drawn under a transform (scale, rotation, center). Taps are inverse-transformed to find the vehicle.
- **Vehicle**: `type` (Car 2 cells / 4 seats, Van 3 cells / 6 seats, Bus 4 cells / 10 seats), `color`, `dir` (N/E/S/W), `cells`, and an optional `mystery` flag.
- **Tap**: cast a ray from the vehicle's nose to the board edge.
  - If the ray hits another vehicle, the vehicle bumps (lurches to the contact distance and springs back).
  - If the ray is clear and a slot is free, the vehicle exits, drives the road path to the leftmost free slot and parks.
  - If the ray is clear but no slot is free, the vehicle shakes and a "No free spot!" toast appears.
- **Slots**: 4 open + 3 locked (each unlocked by an ad within the level), 7 at most.
- **Boarding loop**: while the front passenger's color matches a parked vehicle with free seats (the earliest parked first), that passenger walks in, staggered about 60 ms apart. A full vehicle departs, its slot frees, and the queue advances.
- **Win**: the queue is empty. The rewards are coins (10 base, plus bonuses for hard levels) and stars (3 = no boosters or revive, 2 = one booster used, 1 = otherwise).
- **Lose**: every open slot is occupied, nothing is boarding or moving, and no parked vehicle matches the front passenger. That opens Revive (an ad or 120 coins: +1 slot and the queue is re-sorted), then Fail (Retry / Skip via ad).
- **Boosters** (coins or an ad; you start with 2 of each):
  - **Shuffle**: re-deals the colors of the vehicles still in the lot while keeping per-color seat totals, then re-validates that the board is solvable.
  - **Crane (VIP)**: tap any vehicle. A crane lifts it to a special VIP spot even if it's blocked.
  - **Sort**: reorders the front of the queue to match the colors of the parked vehicles.
- **Tutorial**: level 1 shows a hand pointer and tip text (same idea as the reference, with our own wording and art).

---

## 5. 5000 levels: procedural, deterministic, guaranteed solvable

Levels are not stored. `LevelGenerator.generate(levelNumber)` is seeded by the level number, so the same level always looks the same. It takes about 5 ms and the result is LRU-cached.

1. **Difficulty curve** from the level number: vehicle count (3 at level 1, about 60 by level 100, capped at 90), color count (2 → 8), board size (6×6 → 13×15), mix of cars, vans and buses, the shape mask (rectangles first, then circle, heart, diamond, flower, rings, cross), rotation (0° → 15–35°), mystery ratio, and a "queue scramble window". Every 5th level is Hard and every 10th is Super Hard. Easy "breather" levels come after them.
2. **Placement guarantees a removal order.** Place vehicles one at a time, and accept a placement only if its exit ray is clear of the vehicles already placed. Removing them in reverse placement order is then always legal.
3. **Queue built from that order.** Concatenate each vehicle's seats in removal order, then do a bounded shuffle so that no vehicle's group moves more than (open slots − 1) positions. With 4 slots this keeps the level solvable without boosters.
4. **Verifier**: a fast greedy and look-ahead solver simulates the level. If it fails, the generator retries with seed + k.
5. A unit test runs all 5000 levels through generation and the solver, and fails if any level can't be solved or takes longer than its time budget.

Collection *c* (1–50) = levels `(c−1)·100 + 1 … c·100`, using theme `(c−1) % 10` and tier `(c−1) / 10 + 1` (for example "City Station II").

---

## 6. Sound and haptics

Sound effects and two music loops are **synthesized by a Python script** (sine, square and triangle tones, noise, envelopes) into 16-bit mono WAV files at 22 kHz in `res/raw`. Nothing is downloaded, so there are no licensing questions, and you can replace the files with licensed audio later.

- **SFX** (played with `SoundPool`): `ui_click`, `ui_open`, `ui_close`, `tap`, `bump`, `engine_go`, `park`, `board_pop` (3 pitch variants), `vehicle_full`, `depart_horn`, `coin`, `coin_burst`, `star_1–3`, `win_fanfare`, `fail`, `unlock`, `whoosh`, `booster`, `crane`, `warning`.
- **Music** (played with `MediaPlayer`, crossfaded): `music_home` (cheerful, about 24 s loop) and `music_game` (light and bouncy, about 32 s loop). Music ducks while an ad is showing.
- **Haptics**: tap, a stronger bump, and a pattern on win and fail. Respects the Vibration toggle.
- `AudioManager` pauses everything when the app goes to the background (lifecycle-aware).

---

## 7. Architecture and code layout

Single Activity, Navigation Compose, MVVM, DataStore. No DI framework: a small `AppContainer` holds the singletons.

```
com.ngstudio.trafficjam
├─ App.kt, MainActivity.kt, AppContainer.kt
├─ core/
│  ├─ audio/   SoundManager, MusicPlayer, Haptics
│  ├─ ads/     AdManager (interface), FakeAdManager, AdOverlay
│  └─ data/    GamePrefs (DataStore), ProgressRepository
├─ game/
│  ├─ model/   Vehicle, VehicleType, Dir, Passenger, Board, Level, Theme
│  ├─ gen/     DifficultyCurve, ShapeMasks, LevelGenerator, Solver
│  ├─ engine/  GameEngine (frame loop via withFrameNanos), RoadPaths, Boosters, GameState
│  └─ render/  VehicleArt (bitmap cache), PassengerArt, SceneArt (10 themes), Particles, BoardRenderer
└─ ui/
   ├─ theme/   Colors, Type (Lilita/Fredoka), Shapes
   ├─ components/ GameButton, GamePanel, RibbonBanner, CoinPill, GameToggle, StarRow, BottomBar, WatchAdDialog, ExitDialog
   ├─ splash/ onboarding/ home/ levels/ collections/ garage/ settings/ web/ game/
   └─ nav/     AppNavHost, Routes
assets/  privacy_policy.html, terms_of_service.html
res/raw/ *.wav     res/font/ lilita_one.ttf, fredoka_*.ttf     res/drawable/ ic_*.xml
```

**Persistence** (DataStore): coins, booster counts, highest unlocked level, ad-unlocked levels (a compact bitset of 5000 bits, stored as Base64), stars per level (a 5000-byte array, stored as Base64), unlocked collections, the selected skin, sound/music/vibration flags, the onboarding-done flag, and the daily reward day.

**New dependencies**: `navigation-compose`, `datastore-preferences`, `lifecycle-viewmodel-compose`, `core-splashscreen`. Everything else uses the existing Compose BOM.

Portrait only, edge-to-edge, `minSdk 24`. Back on Home opens the game exit dialog. Back during gameplay opens a Pause dialog (Resume / Restart / Home).

---

## 8. Build order (one continuous phase)

1. Gradle dependencies, manifest (portrait, splash theme), fonts, theme and typography.
2. Core: prefs and repository, audio and haptics, ad stub.
3. Generate the WAV sound set with a Python script and add it to `res/raw`.
4. Game model, generator and solver, plus the 5000-level unit test.
5. Rendering: vehicle and passenger art caches, the 10 scene themes, particles.
6. Engine: tap, bump, exit, road path, parking, boarding, win and lose, boosters, revive.
7. Game screen UI: HUD, slots, booster bar, dialogs (Prop, Revive, Victory, Fail, Pause), the level 1 tutorial.
8. Splash, onboarding, home, bottom bar, levels map, collections, garage, settings.
9. HTML privacy and terms pages in assets, plus the WebView screen.
10. App icon, then the polish pass (timings, easing, sounds wired everywhere).
11. **Verify**: `./gradlew assembleDebug` and the unit tests. Install on your connected Samsung over ADB, play levels 1–3, test locked level → ad → unlock, locked collection → ad, fail → revive, settings toggles and the HTML pages, and take screenshots of each screen.

---

## 9. Things to confirm (defaults in brackets)

- Game name: **Traffic Jam Rush** [default]. The package stays `com.ngstudio.trafficjam`.
- "First collection" = the first world of 100 levels [default, see §2].
- The contact email and company name in the Privacy Policy and Terms: **placeholders** (`support@yourstudio.com`) until you give me real ones.
- Downloading the two OFL font families from the official Google Fonts GitHub repo (about 300 KB total) during the build.
- Leaderboard, sign-in, language and real IAP are **out of scope** (they need backends). The UI can leave room for them later.

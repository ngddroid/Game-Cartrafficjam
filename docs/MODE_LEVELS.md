# Game modes: level design and difficulty (levels 1–5000)

Every mode now has exactly **5000 levels**, the same as Classic. After a player beats level 5000
of a mode, the Modes card says **"All 5000 done!"** and tapping it replays level 5000.

All levels are **generated from the level number** (with a fixed random seed per level), so level
237 is the same for every player and nothing has to be stored.

The tuning for Crowd Loop, Pixel Crowd and Sand Sweep lives in one file:
`app/src/main/java/com/ngstudio/trafficjam/game/gen/ModeCurve.kt`.
Classic keeps its own curve in `DifficultyCurve` (in `LevelGenerator.kt`).

---

## 1. How difficulty rises: two layers

### Layer 1: the long ramp (chapters)

Each setting is a line through a few anchor levels, so it climbs smoothly:

| Chapter | Levels | What the player feels |
|---|---|---|
| Teaching | 1–10 | Very few pieces, 2–3 colors, a bigger belt (Sand). Learn the rules. |
| Learning | 11–60 | Something new every few levels: more colors, shapes, rotation. |
| Skilled | 61–300 | Steady growth, "?" mystery cars, tighter Pixel buses, looser Sand order. |
| Expert | 301–2000 | Slow, gentle growth toward the limits the screen can show. |
| Mastery | 2001–5000 | Flat at the top. Variety comes from shapes, pictures and colors. |

The ramp stops at about level 2000 on purpose. Past that point, more pieces would not fit on a
phone screen or would feel unfair, so later levels stay challenging through variety, not size.

### Layer 2: the ten-level rhythm

This repeats in every block of 10, in every mode:

| Level ends in | Type | What changes |
|---|---|---|
| 1 and 6 | **Breather** (from level 6) | Noticeably easier: fewer pieces, fewer colors, plain rectangle lot |
| 2, 3, 4, 7, 8, 9 | Normal | The base curve |
| 5 | **Hard** (shown in game) | One step harder |
| 0 | **Super Hard** (from level 10, shown in game) | Two steps harder |

So a player gets: easy → normal → **hard** → easy → normal → **super hard** → repeat.
After each peak comes a breather, so a hard level never feels like a wall.

> Before this change, Hard / Super Hard was only a label in Pixel Crowd and Sand Sweep: the
> level itself was not any harder. It now really is harder in all three modes.

---

## 2. Crowd Loop

**How a level is built (`LoopLevels.kt`)**
1. A Classic-style parking lot is generated: vehicles packed into a shape (rectangle, circle,
   heart, hex, diamond, flower, ring or cross), sometimes rotated. Each vehicle gets an arrow.
2. The lot is "peeled" from the outside in, which records an order that is always solvable.
3. Every vehicle's seats ×4 become the crowd (a car's 4 seats = 16 people), four abreast on
   the ring road.
4. The crowd's color order is "scrambled" a little away from the easy order. A built-in solver
   plays the level and rejects any version it can't finish, so **every level is solvable**.

**What gets harder**

| Setting | Effect on the player |
|---|---|
| Vehicles | Bigger jam to unblock, more planning |
| Colors | More vehicles that can't use the front of the crowd right now |
| Scramble | The color you need next is buried deeper in the lot |
| Mystery "?" | You can't see some vehicles' colors until they leave |
| Shapes / rotation | Odd shapes and tilted lots make blocking harder to read |
| Vehicle mix | Early levels mostly small cars; later more vans and buses (bigger groups) |

**Normal-level curve** (Hard adds +1 vehicle, +1 color and +0.3 scramble; Super Hard adds +2,
+1 and +0.6 plus 4% more mystery; breathers take −2 vehicles, −1 color and −0.4 scramble)

| Level | Vehicles | Colors | Scramble | Mystery |
|---|---|---|---|---|
| 1 | 4 | 2 | 0 | 0% |
| 4 | 6 | 3 | 0.9 | 0% |
| 9 | 7 | 3 | 1.1 | 0% |
| 22 | 11 | 5 | 1.5 | 0% |
| 32 | 12 | 5 | 1.6 | 5% |
| 62 | 14 | 6 | 2.0 | 6% |
| 102 | 15 | 6 | 2.1 | 7% |
| 202 | 16 | 7 | 2.2 | 9% |
| 502 | 17 | 8 | 2.4 | 13% |
| 1502 | 18 | 8 | 2.4 | 16% |
| 2002–5000 | 18 | 8 | 2.4 | 18% |

Rotation starts at level 8 and mystery at level 30. The upper limit is 20 vehicles, because
the lot sits under the park and has to stay readable.

**Lot shapes: 13, unlocked in stages** (Sand Sweep's truck lot uses the same list)

| From level | New shapes |
|---|---|
| 1 | Rectangle |
| 4 | Circle, Hex |
| 11 | Diamond, Heart |
| 31 | Flower, Cross, Ring |
| 81 | Star, Triangle, Clover |
| 201 | Hourglass, Crescent |

- **Breathers and levels 1–3** use a plain rectangle.
- **No repeats:** a shape is never used twice in a row.
- **Super Hard levels** use an awkward shape (Heart, Flower, Cross, Ring, Star, Hourglass or
  Crescent), where blocking is hardest to read.

**New twist:** from level 150, Super Hard levels open **3 parking stalls instead of 4**. The
solver checks every such level, so it can still be finished. The locked stalls can be opened
with an ad.

---

## 3. Pixel Crowd

**How a level is built (`PixelPictures.kt`, `PixelEngine.plan`)**
1. The level picks one of 16 pixel pictures (Fish, Heart, Apple, Sun, … Rainbow). Every pixel
   is a rider.
2. The planner "peels" the picture the way a player would. Only riders on the **outside** can
   leave, so each bus takes riders of one color from the edge, which uncovers the next layer.
   This produces a list of buses that is always solvable in order.
3. The buses are spread across the **3 garage lanes**, keeping that order reachable. There are
   **3 open stops** (a 4th unlocks with an ad).

**The pictures: 61 in all** (Pixel Crowd and Sand Sweep share them)

| Group | Pictures |
|---|---|
| Simple (2–3 colors) | Leaf, Moon, Star, Crab, Gem, Gift, Ladybug, Carrot, Snowflake, Cherry, Umbrella, Drop, Bell, Pear, Lemon, Grapes, Ghost … |
| Medium (3–4 colors) | Turtle, Whale, Chick, Penguin, Strawberry, Planet, Octopus, Mug, Fish, Fox, Tree, Heart, Hot Air, Apple, Sun, Car, Boat, Dino, Bunny, Cactus, Duck, Bee, Pine, Balloon, Pizza … |
| Busy (5+ colors) | Ice Cream, Robot, Donut, Flower, Frog, Melon, Pig, Parrot, Cupcake, Rocket, Butterfly, Crown, House, Castle, Owl, Mushroom, Kitty, Rainbow, Beach Ball |

Each picture is ranked by how demanding it is (number of colors × 12 + riders ÷ 10).
The Donut is a special one: its hole is closed off, so the riders around it can only leave
after a way in has been cleared.

**Which picture each level shows**
- **Levels 1–61:** every picture exactly once, from simplest (Leaf) to busiest (Beach Ball).
  Each early level brings something new.
- **Level 62 on:** the pictures are dealt in shuffled decks. A picture never comes back within
  20 levels, and the next deck only starts once the current one is used up.
- **Peaks and breathers:** Super Hard levels take the busiest picture left in the deck, and
  breathers take the simplest.
- **Late game:** from level 300 the simplest pictures slowly leave the decks (a quarter of them
  are gone by level 1500).
- **Returning pictures:** a picture's 2nd showing is mirrored, and from its 3rd showing its
  colors are swapped too, so it looks new.

**What gets harder: bus size.** Smaller buses split the picture into more trips, so you juggle
more buses between the lanes and the stops, and a wrong dispatch blocks a stop for longer.

| Level | Biggest bus (normal) | Hard (×0.85) | Super Hard (×0.72) | Breather (×1.25) |
|---|---|---|---|---|
| 1 | 48 | – | – | – |
| 4 | 40 | – | – | – |
| 12 | 31 | 27 | 23 | 39 |
| 32 | 27 | 23 | 19 | 34 |
| 62 | 23 | 20 | 17 | 29 |
| 102 | 21 | 18 | 15 | 26 |
| 202 | 19 | 16 | 14 | 24 |
| 502 | 17 | 15 | 12 | 21 |
| 1002 | 16 | 13 | 11 | 20 |
| 2002–5000 | 14 | 12 | 10 | 18 |

**New twist:** from level 200, Super Hard levels open only **2 of the 4 stops** (3 normally),
so parking the wrong bus costs more. It's still always solvable, because the next bus the
picture needs is always at the front of a garage lane. The other stops can be opened with an ad.

---

## 4. Sand Sweep

**How a level is built (`SandEngine.build`)**
1. The level's pixel picture is painted into fine sand (180 × 128 grains) with soft edges and
   brush shading. Empty space is filled with the picture's background color.
2. A Classic-style lot of trucks is generated (always solvable as a blocking puzzle).
3. Each color gets trucks in proportion to how much sand it has. The trucks' capacities add up
   to exactly that color's sand.
4. Truck colors are ordered so that **colors near the bottom of the picture come out of the lot
   first**, because a truck can only scoop sand within reach of the bottom.

**What gets harder**

| Setting | Effect on the player |
|---|---|
| Trucks | Bigger jam to unblock, sand split into more loads |
| Order jitter | Some trucks come out before their sand is reachable, so they ride the belt waiting and use up its room |
| Belt room | 6 trucks for levels 1–10 and on breathers, 5 otherwise (+ button and revives raise it to 8) |
| Lot shape | The same 13 shapes as Crowd Loop, unlocked in stages (see above) |
| Picture | The same 61-picture order as Pixel Crowd; busier pictures mean more sand colors |

**Normal-level curve** (for a 5-color picture; Hard adds +2 trucks and +0.1 jitter, Super Hard
+3 and +0.2; breathers take −3 trucks, use jitter 0.05 and give a 6-truck belt)

| Level | Trucks | Order jitter | Belt |
|---|---|---|---|
| 1 | 8 | 0.10 | 6 |
| 9 | 10 | 0.14 | 6 |
| 12 | 10 | 0.16 | 5 |
| 32 | 14 | 0.22 | 5 |
| 62 | 16 | 0.27 | 5 |
| 102 | 18 | 0.31 | 5 |
| 202 | 20 | 0.36 | 5 |
| 502 | 23 | 0.42 | 5 |
| 1002 | 25 | 0.46 | 5 |
| 1502–5000 | 26 | 0.50 | 5 |

Jitter is kept moderate on purpose. Sand Sweep has no solver, so the order is only loosened,
never shuffled. If the belt ever jams, the revive (more room, then sending stuck trucks back)
always gets the player moving again.

---

## 5. Classic (for comparison, unchanged)

Built by `DifficultyCurve.params`. Level 1 is a 3-vehicle tutorial. Vehicles then climb to about
32 by level 10 and 50 by level 50, then slowly to 62. Colors go from 2 to 8 by level 221, mystery cars start at level 25 and reach 22%, and shapes
start at level 6 and rotation at level 8. It uses the same ten-level rhythm.

---

## 6. What changed from before

| Mode | Before | Now |
|---|---|---|
| All three | No level limit | 5000 levels, with "All 5000 done!" at the end |
| Crowd Loop | Stopped getting harder around level 15 (17 vehicles, 6 colors, fixed scramble) | Climbs to level ~2000: up to 8 colors, scramble 2.4, 18% mystery |
| Pixel Crowd | Bus size stopped shrinking at level 151; Hard was a label only; fixed picture order | Shrinks to level 2000; Hard and Super Hard really are harder; first round goes simple → complex; later rounds shuffled |
| Sand Sweep | Stopped getting harder around level 30; Hard was a label only; same truck order looseness everywhere | Trucks and order looseness climb to level ~1500; real Hard and Super Hard; roomier belt while learning |

### Second update: more variety

| Mode | Before | Now |
|---|---|---|
| Pixel Crowd + Sand Sweep | 16 pictures, repeating every 16 levels | 61 pictures; 1–61 all new, simple → busy; then shuffled decks with no repeat within 20 levels; peaks get busy pictures, breathers simple ones |
| Crowd Loop + Sand Sweep | 8 random lot shapes | 13 shapes unlocked in stages, never the same twice in a row, awkward shapes on Super Hard |
| Crowd Loop | Always 4 open stalls | 3 on Super Hard levels from 150 (solver-checked) |
| Pixel Crowd | Always 3 open stops | 2 on Super Hard levels from 200 |

## 7. Tuning

Open `ModeCurve.kt` and change an anchor, for example `20 to 11f` means "11 vehicles by
level 20". Every level around it follows. `ModeLevelsTest.printMilestones` prints the table
above, and the other tests check that:
- levels build across all 5000;
- every Pixel rider has a bus seat;
- normal levels never get easier as you go further.

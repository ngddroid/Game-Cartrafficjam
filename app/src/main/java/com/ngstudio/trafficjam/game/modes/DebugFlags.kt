package com.ngstudio.trafficjam.game.modes

/** Debug-build switches set from launch intent extras. Always off in release builds. */
object DebugFlags {
    /** Mode engines tap their own free vehicles, for hands-free performance runs. */
    @Volatile var autoplay = false
    /** Mode engines log how long their frames take. */
    @Volatile var perf = false
}

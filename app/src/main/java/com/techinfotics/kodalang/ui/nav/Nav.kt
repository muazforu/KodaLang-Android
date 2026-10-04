package com.techinfotics.kodalang.ui.nav

/** Navigation destinations for the MVP. */
object Routes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val HOME = "home"
    const val PRACTICE = "practice"
    const val PROFILE = "profile"
}

/** Bottom-bar tabs (all require a signed-in user). */
val BottomTabs = listOf(
    Routes.HOME,
    Routes.PRACTICE,
    Routes.PROFILE,
)

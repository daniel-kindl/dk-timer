package dev.danielkindl.ocho.data.audio

interface AudioPlayer {
    fun playIntervalBeep()
    fun playCompletionSound()
    /** High-pitched beep: signals the start of a Tabata work phase. */
    fun playWorkStartBeep()
    /** Low-pitched beep: signals the start of a Tabata rest phase. */
    fun playRestStartBeep()
    fun release()
}

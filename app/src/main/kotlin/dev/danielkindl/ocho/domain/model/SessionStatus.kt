package dev.danielkindl.ocho.domain.model

/**
 * Lifecycle of a running session, shared by the EMOM and Tabata view models.
 *
 * [Stopped] and [Completed] are deliberately distinct: stopping is an early exit and
 * navigates away immediately, while completing shows the summary and waits for the
 * user to dismiss it.
 */
enum class SessionStatus { CountingDown, Running, Paused, Stopped, Completed }

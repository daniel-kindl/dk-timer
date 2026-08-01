package dev.danielkindl.ocho.domain.model

/**
 * Length of the pre-start countdown, in seconds.
 *
 * Shared by both session view models and by the setup screens' run timeline, so the
 * amber prepare segment a user previews is exactly the one they get. Previously
 * duplicated as a private constant in each view model, where the two could drift.
 */
const val PREPARE_COUNTDOWN_SECONDS = 3

/** [PREPARE_COUNTDOWN_SECONDS] in milliseconds, for timeline weighting. */
const val PREPARE_COUNTDOWN_MILLIS = PREPARE_COUNTDOWN_SECONDS * 1_000L

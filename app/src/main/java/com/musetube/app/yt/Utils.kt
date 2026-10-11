package com.musetube.app.yt

import android.util.Log

/** Forwards to Crashlytics when the Firebase dependency is enabled (no-op in Phase 0 stub builds). */
fun reportException(throwable: Throwable) {
    Log.e("Mtb-DL", "Exception", throwable)
    // TODO(Phase0): re-enable with Firebase (see project.md Phase 0).
    // runCatching { FirebaseCrashlytics.getInstance().recordException(throwable) }
}

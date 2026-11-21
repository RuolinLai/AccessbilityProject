package com.emma_zara.accessbilityproject.closet

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions

/**
 * The "background remover" (ML Kit subject segmentation) isn't inside the app: Google Play services
 * downloads it separately. When the app is installed from an APK (not the Play Store), that only
 * happens when we ask. This object asks as soon as the app starts and keeps track of how it's going,
 * so the screens can tell the user instead of silently falling back.
 */
object BackgroundRemover {

    sealed interface Status {
        data object Checking : Status
        data object Ready : Status
        /** [percent] is null when Google Play hasn't said how big the download is yet. */
        data class Downloading(val percent: Int?) : Status
        data object WaitingForInternet : Status
        /** Can't be used. [canRetry] is false when trying again won't help (for example no Google Play services). */
        data class Unavailable(val reason: String, val canRetry: Boolean = true) : Status
    }

    /** Compose screens re-draw automatically when this changes. */
    var status: Status by mutableStateOf(Status.Checking)
        private set

    /** Starts checking / downloading. Safe to call many times: it does nothing if it's ready or already busy. */
    fun prepare(context: Context) {
        val current = status
        if (current is Status.Ready || current is Status.Downloading || current is Status.WaitingForInternet) return
        status = Status.Checking
        val appContext = context.applicationContext

        when (val result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(appContext)) {
            ConnectionResult.SUCCESS -> Unit
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED, ConnectionResult.SERVICE_UPDATING ->
                return fail("Google Play services is too old. Update it in the Play Store, then try again.")
            ConnectionResult.SERVICE_DISABLED ->
                return fail("Google Play services is turned off on this phone. Turn it on in Settings, then try again.")
            else -> return fail(
                "This phone doesn't have Google Play services (code $result), so the background " +
                    "remover can't be downloaded. The app will read the color from the middle of the photo instead.",
                canRetry = false,
            )
        }

        val segmenter = SubjectSegmentation.getClient(
            SubjectSegmenterOptions.Builder().enableForegroundConfidenceMask().build(),
        )
        val installer = ModuleInstall.getClient(appContext)

        val listener = object : InstallStatusListener {
            override fun onInstallStatusUpdated(update: ModuleInstallStatusUpdate) {
                when (update.installState) {
                    InstallState.STATE_COMPLETED -> status = Status.Ready
                    InstallState.STATE_DOWNLOAD_PAUSED -> status = Status.WaitingForInternet
                    InstallState.STATE_CANCELED -> fail("The download was stopped. Tap \"Try again\".")
                    InstallState.STATE_FAILED -> fail("The download didn't work (error ${update.errorCode}). Check the internet and tap \"Try again\".")
                    else -> status = Status.Downloading(
                        update.progressInfo?.let { p ->
                            if (p.totalBytesToDownload > 0) (p.bytesDownloaded * 100 / p.totalBytesToDownload).toInt() else null
                        },
                    )
                }
                val done = update.installState in setOf(
                    InstallState.STATE_COMPLETED, InstallState.STATE_CANCELED, InstallState.STATE_FAILED,
                )
                if (done) {
                    installer.unregisterListener(this)
                    segmenter.close()
                }
            }
        }

        installer.areModulesAvailable(segmenter)
            .addOnSuccessListener { availability ->
                if (availability.areModulesAvailable()) {
                    status = Status.Ready
                    segmenter.close()
                    return@addOnSuccessListener
                }
                status = Status.Downloading(null)
                installer.installModules(ModuleInstallRequest.newBuilder().addApi(segmenter).setListener(listener).build())
                    .addOnSuccessListener { response ->
                        if (response.areModulesAlreadyInstalled()) {
                            status = Status.Ready
                            installer.unregisterListener(listener)
                            segmenter.close()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("BackgroundRemover", "Couldn't start the download", e)
                        installer.unregisterListener(listener)
                        segmenter.close()
                        fail("Couldn't start the download. Check the internet and tap \"Try again\".")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("BackgroundRemover", "Couldn't check the background remover", e)
                segmenter.close()
                fail("Couldn't check the background remover. Tap \"Try again\".")
            }
    }

    /** Called by the scanner when cutting out an item worked, so we know for sure it's ready. */
    fun markReady() {
        status = Status.Ready
    }

    private fun fail(reason: String, canRetry: Boolean = true) {
        status = Status.Unavailable(reason, canRetry)
    }
}

package com.ar.biometricprompt

import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class BiometricPromptManager(
    private val activity: AppCompatActivity,
) {
    private val resultChannel = Channel<BiometricResults>()
    val resultFlow = resultChannel.receiveAsFlow()

    fun showBiometricPrompt(
        title: String,
        description: String,
    ) {
        val manager = BiometricManager.from(activity)
        val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

        val promptInfo =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(title)
                .setSubtitle(description)
                .setAllowedAuthenticators(authenticators)

        when (manager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                resultChannel.trySend(BiometricResults.HardwareUnavailable)
                return
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                resultChannel.trySend(BiometricResults.FeatureUnavailable)
                return
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                resultChannel.trySend(BiometricResults.AuthenticationNotSet)
                return
            }

            else -> Unit
        }

        val prompt =
            BiometricPrompt(
                activity,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ){
                        super.onAuthenticationError(errorCode, errString)
                        resultChannel.trySend(BiometricResults.AuthenticationError(errString.toString()))
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        resultChannel.trySend(BiometricResults.AuthenticationSuccess)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        resultChannel.trySend(BiometricResults.AuthenticationFailed)
                    }
                },
            )
        prompt.authenticate(promptInfo.build())
    }

    sealed interface BiometricResults {
        data object HardwareUnavailable : BiometricResults

        data object FeatureUnavailable : BiometricResults

        data class AuthenticationError(
            val error: String,
        ) : BiometricResults

        data object AuthenticationFailed : BiometricResults

        data object AuthenticationSuccess : BiometricResults

        data object AuthenticationNotSet : BiometricResults
    }
}

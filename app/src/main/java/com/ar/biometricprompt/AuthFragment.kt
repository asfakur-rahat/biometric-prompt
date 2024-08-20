package com.ar.biometricprompt

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ar.biometricprompt.BiometricPromptManager.*
import com.ar.biometricprompt.databinding.FragmentAuthBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AuthFragment : Fragment(R.layout.fragment_auth) {

    private val promptManager by lazy {
        BiometricPromptManager(requireActivity() as AppCompatActivity)
    }
    private lateinit var binding: FragmentAuthBinding
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentAuthBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        initObservers()
    }

    private fun initObservers() {
        lifecycleScope.launch {
            promptManager.resultFlow.collectLatest { result ->
                when (result) {
                    is BiometricResults.AuthenticationError -> {
                        showError(result.error)
                    }
                    BiometricResults.AuthenticationFailed -> {
                        showError("Authentication failed")
                    }
                    BiometricResults.AuthenticationNotSet -> {
                        showError("Authentication not set")
                        openSettings()
                    }
                    BiometricResults.AuthenticationSuccess -> {
                        goToHome()
                    }
                    BiometricResults.FeatureUnavailable -> {
                        showError("Feature unavailable")
                    }
                    BiometricResults.HardwareUnavailable -> {
                        showError("Hardware unavailable")
                    }
                }
            }
        }
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
            putExtra(
                Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                BIOMETRIC_STRONG or DEVICE_CREDENTIAL
            )
        }
        launcher.launch(intent)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            promptManager.showBiometricPrompt(
                title = "Biometric Authentication",
                description = "Authenticate to continue"
            )
        }
    }

    private fun showError(error: String) {
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.text = error
    }

    private fun goToHome() {
        binding.tvError.visibility = View.GONE
        findNavController().navigate(R.id.action_authFragment_to_homeFragment)
    }

    private fun initListeners() {
        binding.authenticateButton.setOnClickListener {
            promptManager.showBiometricPrompt(
                title = "Biometric Authentication",
                description = "Authenticate to continue"
            )
        }
    }
}
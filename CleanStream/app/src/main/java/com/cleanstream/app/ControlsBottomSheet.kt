package com.cleanstream.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.cleanstream.app.databinding.BottomSheetControlsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Bottom sheet dialog presenting feature toggles:
 * - Block Ads (YouTube)
 * - Hide Shorts (YouTube)
 * - Background Play
 * - Block Ads in All Apps & System (VPN)
 * - 1-Tap shortcut to configure native Android Private DNS
 */
class ControlsBottomSheet : BottomSheetDialogFragment() {

    interface Listener {
        fun onSettingsChanged(blockAds: Boolean, blockShorts: Boolean, backgroundPlay: Boolean)
        fun onSystemAdBlockToggled(enabled: Boolean)
    }

    private var _binding: BottomSheetControlsBinding? = null
    private val binding get() = _binding!!

    var listener: Listener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetControlsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSavedPreferencesIntoToggles()
        attachToggleListeners()
        attachPrivateDnsButtonListener()
        binding.buttonDone.setOnClickListener { dismiss() }
    }

    /** Populates each switch with the user's last saved setting. */
    private fun loadSavedPreferencesIntoToggles() {
        binding.switchBlockAds.isChecked = PreferencesManager.isAdBlockEnabled()
        binding.switchBlockShorts.isChecked = PreferencesManager.isShortsBlockEnabled()
        binding.switchBackgroundPlay.isChecked = PreferencesManager.isBackgroundPlayEnabled()
        binding.switchBlockSystemAds.isChecked = PreferencesManager.isSystemAdBlockEnabled()
    }

    /** Saves each toggle change to prefs and notifies the host activity. */
    private fun attachToggleListeners() {
        binding.switchBlockAds.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.setAdBlockEnabled(isChecked)
            notifySettingsChanged()
        }
        binding.switchBlockShorts.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.setShortsBlockEnabled(isChecked)
            notifySettingsChanged()
        }
        binding.switchBackgroundPlay.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.setBackgroundPlayEnabled(isChecked)
            notifySettingsChanged()
        }
        binding.switchBlockSystemAds.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.setSystemAdBlockEnabled(isChecked)
            listener?.onSystemAdBlockToggled(isChecked)
        }
    }

    /**
     * Copies the Private DNS hostname to clipboard and opens Android Network Settings.
     * This provides native, zero-battery, VPN-free ad blocking for the entire OS.
     */
    private fun attachPrivateDnsButtonListener() {
        binding.buttonPrivateDns.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Private DNS Hostname", PRIVATE_DNS_HOSTNAME)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(requireContext(), getString(R.string.dns_copied_toast), Toast.LENGTH_LONG).show()

            try {
                startActivity(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS))
            } catch (e: Exception) {
                try {
                    startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                } catch (fallbackError: Exception) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }
    }

    private fun notifySettingsChanged() {
        listener?.onSettingsChanged(
            blockAds = PreferencesManager.isAdBlockEnabled(),
            blockShorts = PreferencesManager.isShortsBlockEnabled(),
            backgroundPlay = PreferencesManager.isBackgroundPlayEnabled()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ControlsBottomSheet"
        private const val PRIVATE_DNS_HOSTNAME = "dns.adguard-dns.com"
    }
}

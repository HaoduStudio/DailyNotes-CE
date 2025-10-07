package com.haodustudio.DailyNotes.view.activities

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import com.haodustudio.DailyNotes.R
import com.haodustudio.DailyNotes.databinding.ActivityPrivacySettingsBinding
import com.haodustudio.DailyNotes.helper.PrivacySettingsManager
import com.haodustudio.DailyNotes.helper.makeToast
import com.haodustudio.DailyNotes.view.activities.base.BaseActivity

class PrivacySettingsActivity : BaseActivity() {

    private val binding by lazy { ActivityPrivacySettingsBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupListeners()
        refreshIndicators()
    }

    private fun setupListeners() {
        binding.privacyCardCrash.setOnClickListener {
            handleSettingClick(PrivacySetting.CRASH_REPORTING)
        }
        binding.privacyCardLocation.setOnClickListener {
            handleSettingClick(PrivacySetting.LOCATION)
        }
        binding.privacyCardCloud.setOnClickListener {
            handleSettingClick(PrivacySetting.CLOUD_RESOURCES)
        }
    }

    private fun handleSettingClick(setting: PrivacySetting) {
        val current = isSettingEnabled(setting)
        val target = !current
        showConfirmationDialog(setting, target)
    }

    private fun showConfirmationDialog(setting: PrivacySetting, targetState: Boolean) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_privacy_confirmation)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)

        val titleView = dialog.findViewById<TextView>(R.id.privacyDialogTitle)
        val messageView = dialog.findViewById<TextView>(R.id.privacyDialogMessage)
        val cancelView = dialog.findViewById<LinearLayout>(R.id.privacyDialogCancel)
        val confirmView = dialog.findViewById<LinearLayout>(R.id.privacyDialogConfirm)

        titleView.text = getString(R.string.privacy_confirm_title)
        val actionText = getString(
            if (targetState) R.string.privacy_confirm_enable else R.string.privacy_confirm_disable,
            getString(setting.titleRes)
        )
        messageView.text = buildString {
            append(actionText)
            append('\n')
            append(getString(R.string.privacy_confirm_message))
        }

        cancelView.setOnClickListener { dialog.dismiss() }
        confirmView.setOnClickListener {
            applySetting(setting, targetState)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun applySetting(setting: PrivacySetting, enabled: Boolean) {
        when (setting) {
            PrivacySetting.CRASH_REPORTING -> PrivacySettingsManager.updateCrashReportingEnabled(enabled)
            PrivacySetting.LOCATION -> PrivacySettingsManager.updateLocationEnabled(enabled)
            PrivacySetting.CLOUD_RESOURCES -> PrivacySettingsManager.updateCloudResourcesEnabled(enabled)
        }
        refreshIndicators()
        val stateText = if (enabled) R.string.privacy_state_enabled else R.string.privacy_state_disabled
        makeToast("${getString(setting.titleRes)}：${getString(stateText)}")
    }

    private fun refreshIndicators() {
        val crashEnabled = PrivacySettingsManager.isCrashReportingEnabled()
        val locationEnabled = PrivacySettingsManager.isLocationEnabled()
        val cloudEnabled = PrivacySettingsManager.isCloudResourcesEnabled()

        updateIndicator(binding.privacyCardCrashIndicator, crashEnabled)
        updateIndicator(binding.privacyCardLocationIndicator, locationEnabled)
        updateIndicator(binding.privacyCardCloudIndicator, cloudEnabled)

        binding.privacyCardCrashSubtitle.text = buildSubtitle(R.string.privacy_crash_description, crashEnabled)
        binding.privacyCardLocationSubtitle.text = buildSubtitle(R.string.privacy_location_description, locationEnabled)
        binding.privacyCardCloudSubtitle.text = buildSubtitle(R.string.privacy_cloud_description, cloudEnabled)
    }

    private fun updateIndicator(indicator: ImageView, enabled: Boolean) {
        indicator.setImageResource(
            if (enabled) R.drawable.bg_privacy_indicator_on else R.drawable.bg_privacy_indicator_off
        )
    }

    private fun isSettingEnabled(setting: PrivacySetting): Boolean = when (setting) {
        PrivacySetting.CRASH_REPORTING -> PrivacySettingsManager.isCrashReportingEnabled()
        PrivacySetting.LOCATION -> PrivacySettingsManager.isLocationEnabled()
        PrivacySetting.CLOUD_RESOURCES -> PrivacySettingsManager.isCloudResourcesEnabled()
    }

    private fun buildSubtitle(@StringRes descriptionRes: Int, enabled: Boolean): String {
        val statusText = getString(if (enabled) R.string.privacy_state_enabled else R.string.privacy_state_disabled)
        return buildString {
            append(getString(descriptionRes))
            append('\n')
            append(statusText)
        }
    }

    private enum class PrivacySetting(@StringRes val titleRes: Int) {
        CRASH_REPORTING(R.string.privacy_crash_title),
        LOCATION(R.string.privacy_location_title),
        CLOUD_RESOURCES(R.string.privacy_cloud_title)
    }
}

package com.haodustudio.DailyNotes.view.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import com.haodustudio.DailyNotes.BaseApplication
import com.haodustudio.DailyNotes.R
import com.haodustudio.DailyNotes.databinding.ActivityAboutSoftwareBinding
import com.haodustudio.DailyNotes.helper.makeToast
import com.haodustudio.DailyNotes.utils.BitmapUtils
import com.haodustudio.DailyNotes.view.activities.base.BaseActivity
import com.haodustudio.DailyNotes.view.activities.ViewImage
import com.haodustudio.DailyNotes.view.activities.PrivacySettingsActivity

class AboutSoftware : BaseActivity() {
    private val binding by lazy { ActivityAboutSoftwareBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        loadBannerImageSafely()
        configureBanner()
        configureCards()
    }

    private fun loadBannerImageSafely() {
        try {
            binding.bannerImage.post {
                val viewWidth = binding.bannerImage.width
                val viewHeight = binding.bannerImage.height
                
                val bitmap = BitmapUtils.decodeSampledBitmapFromResource(
                    R.drawable.about_banner,
                    viewWidth,
                    viewHeight
                )
                
                if (bitmap != null) {
                    binding.bannerImage.setImageBitmap(bitmap)
                } else {
                    // 如果加载失败,使用占位符
                    binding.bannerImage.setImageResource(android.R.color.darker_gray)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.bannerImage.setImageResource(android.R.color.darker_gray)
        }
    }

    private fun configureBanner() {
        binding.bannerBadge.text = getString(R.string.about_codename_label)
        binding.bannerTitle.text = getString(R.string.about_banner_title)
        binding.bannerSubtitle.text = getString(R.string.about_banner_subtitle)
    }

    private fun configureCards() {
        val versionName = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).versionName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionName
            }
        }.getOrNull()

        binding.cardSoftwareVersion.apply {
            title.text = getString(R.string.about_section_version_title)
            subtitle.text = versionName?.let {
                getString(R.string.about_section_version_subtitle, it)
            } ?: getString(R.string.about_section_version_placeholder)
            icon.setImageResource(R.drawable.info)
            root.contentDescription = getString(R.string.about_section_version_desc)
            root.setOnClickListener {
                makeToast(subtitle.text.toString())
            }
        }

        binding.cardContributors.apply {
            title.text = getString(R.string.about_section_contributors_title)
            subtitle.text = getString(R.string.about_section_contributors_subtitle)
            icon.setImageResource(R.drawable.usergroup)
            root.contentDescription = getString(R.string.about_section_contributors_desc)
            root.setOnClickListener {
                openContributorsPage()
            }
        }

        binding.cardRepository.apply {
            title.text = getString(R.string.about_section_repo_title)
            subtitle.text = getString(R.string.about_section_repo_subtitle)
            icon.setImageResource(R.drawable.ic_about_github)
            root.contentDescription = getString(R.string.about_section_repo_desc)
            root.setOnClickListener {
                openExternalUrl("https://github.com/HaoduStudio/DailyNotes")
            }
        }

        binding.cardPolicy.apply {
            title.text = getString(R.string.about_section_policy_title)
            subtitle.text = getString(R.string.about_section_policy_subtitle)
            icon.setImageResource(R.drawable.ic_about_policy)
            root.contentDescription = getString(R.string.about_section_policy_desc)
            root.setOnClickListener {
                openRemoteAsset("/static/about_user_privacy_policy.png", zoomEnabled = true)
            }
        }

        binding.cardPrivacy.apply {
            title.text = getString(R.string.about_section_privacy_title)
            subtitle.text = getString(R.string.about_section_privacy_subtitle)
            icon.setImageResource(R.drawable.ic_about_privacy)
            root.contentDescription = getString(R.string.about_section_privacy_desc)
            root.setOnClickListener {
                startActivity(Intent(this@AboutSoftware, PrivacySettingsActivity::class.java))
            }
        }
    }

    private fun openRemoteAsset(path: String, zoomEnabled: Boolean) {
        val assetUrl = BaseApplication.buildServerUrl(path)
        if (assetUrl == null) {
            makeToast(getString(R.string.about_network_unavailable))
            return
        }

        val viewerIntent = Intent(this, ViewImage::class.java).apply {
            putExtra("path", assetUrl)
            putExtra("zoomEnabled", zoomEnabled)
            if (path.endsWith(".mp4")) {
                putExtra("isVideo", true)
            }
        }
        startActivity(viewerIntent)
    }

    private fun openContributorsPage() {
        startActivity(Intent(this, AboutContributorsActivity::class.java))
    }

    private fun openExternalUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            makeToast(url)
        }
    }
}
package com.haodustudio.DailyNotes.view.activities

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.haodustudio.DailyNotes.R
import com.haodustudio.DailyNotes.databinding.ActivityAboutContributorsBinding
import com.haodustudio.DailyNotes.databinding.LayoutContributorCardBinding
import com.haodustudio.DailyNotes.view.activities.base.BaseActivity
import java.util.Calendar

class AboutContributorsActivity : BaseActivity() {

    private val binding by lazy { ActivityAboutContributorsBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupCoreContributors()
        setupFooter()
    }

    private fun setupCoreContributors() {
        val inflater = LayoutInflater.from(this)
        val coreMembers = listOf(
            CoreContributor(
                name = "好读游独",
                roleText = getString(R.string.contributors_core_role_major),
                avatarRes = R.drawable.avatar_contributor_haoduyoudu
            ),
            CoreContributor(
                name = "HikaruQwQ",
                roleText = getString(R.string.contributors_core_role_ce_lead),
                avatarRes = R.drawable.avatar_contributor_hikaruqwq
            ),
            CoreContributor(
                name = "huanli233",
                roleText = getString(R.string.contributors_core_role_support),
                avatarRes = R.drawable.avatar_contributor_huanli233
            )
        )

        binding.coreContributorsContainer.removeAllViews()
        coreMembers.forEach { contributor ->
            val cardBinding = LayoutContributorCardBinding.inflate(inflater, binding.coreContributorsContainer, false)
            cardBinding.name.text = contributor.name
            cardBinding.role.text = contributor.roleText
            val avatar = ContextCompat.getDrawable(this, contributor.avatarRes)
            if (avatar != null) {
                cardBinding.avatar.setImageDrawable(avatar)
            } else {
                cardBinding.avatar.setImageResource(R.drawable.ic_avatar_placeholder)
            }
            binding.coreContributorsContainer.addView(cardBinding.root)
        }
    }

    private fun setupFooter() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        binding.footerCopyright.text = getString(R.string.contributors_footer_copyright, currentYear)
    }

    private data class CoreContributor(
        val name: String,
        val roleText: String,
        val avatarRes: Int
    )
}

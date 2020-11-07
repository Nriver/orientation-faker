/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.orientation.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout.LayoutParams
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.gridlayout.widget.GridLayout
import androidx.gridlayout.widget.GridLayout.spec
import com.google.android.gms.ads.AdView
import net.mm2d.android.orientationfaker.R
import net.mm2d.android.orientationfaker.databinding.ActivityDetailedSettingsBinding
import net.mm2d.color.chooser.ColorChooserDialog
import net.mm2d.orientation.control.Orientation
import net.mm2d.orientation.control.OrientationHelper
import net.mm2d.orientation.event.EventRouter
import net.mm2d.orientation.service.MainController
import net.mm2d.orientation.settings.Default
import net.mm2d.orientation.settings.OrientationList
import net.mm2d.orientation.settings.Settings
import net.mm2d.orientation.util.AdMob
import net.mm2d.orientation.util.SystemSettings
import net.mm2d.orientation.view.dialog.OrientationHelpDialog
import net.mm2d.orientation.view.dialog.ResetLayoutDialog
import net.mm2d.orientation.view.dialog.ResetThemeDialog
import net.mm2d.orientation.view.view.CheckItemView

class DetailedSettingsActivity : AppCompatActivity(),
    ResetThemeDialog.Callback,
    ResetLayoutDialog.Callback,
    ColorChooserDialog.Callback {
    private val settings by lazy {
        Settings.get()
    }
    private lateinit var notificationSample: NotificationSample
    private lateinit var checkList: List<CheckItemView>
    private lateinit var orientationListStart: List<Int>
    private val orientationList: MutableList<Int> = mutableListOf()
    private lateinit var adView: AdView
    private lateinit var binding: ActivityDetailedSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailedSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setUpViews()
        EventRouter.observeUpdate(this) { notificationSample.update() }
        setUpAdView()
    }

    private fun setUpAdView() {
        adView = AdMob.makeDetailedAdView(this)
        val param = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
            it.gravity = Gravity.CENTER_HORIZONTAL
        }
        binding.container.addView(adView, param)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!orientationList.contains(settings.orientation)) {
            settings.orientation = orientationList[0]
            MainController.update()
            if (!OrientationHelper.isEnabled) {
                EventRouter.notifyUpdate()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (orientationListStart != orientationList) {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        notificationSample.update()
        applyLayoutSelection()
        applyUseRoundBackground()
        applyUseBlankIcon()
        applyAutoRotateWarning()
        applyNotificationPrivacy()
        AdMob.loadAd(this, adView)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setUpViews() {
        notificationSample = NotificationSample(this)
        setUpSample()
        setUpLayoutSelector()
        setUpUseRoundBackground()
        setUpUseBlankIcon()
        setUpAutoRotateWarning()
        setUpNotificationPrivacy()
        setUpSystemSetting()
    }

    private fun setUpSample() {
        binding.content.sampleForeground.setColorFilter(settings.foregroundColor)
        binding.content.sampleBackground.setColorFilter(settings.backgroundColor)
        binding.content.sampleForegroundSelected.setColorFilter(settings.foregroundColorSelected)
        binding.content.sampleBackgroundSelected.setColorFilter(settings.backgroundColorSelected)
        binding.content.foreground.setOnClickListener {
            ColorChooserDialog.show(this, it.id, settings.foregroundColor)
        }
        binding.content.background.setOnClickListener {
            ColorChooserDialog.show(this, it.id, settings.backgroundColor)
        }
        binding.content.foregroundSelected.setOnClickListener {
            ColorChooserDialog.show(this, it.id, settings.foregroundColorSelected)
        }
        binding.content.backgroundSelected.setOnClickListener {
            ColorChooserDialog.show(this, it.id, settings.backgroundColorSelected)
        }
        binding.content.resetTheme.setOnClickListener { ResetThemeDialog.show(this) }
        setUpOrientationIcons()
    }

    private fun setUpOrientationIcons() {
        notificationSample.buttonList.forEach { view ->
            view.button.setOnClickListener { updateOrientation(view.orientation) }
        }
    }

    private fun updateOrientation(orientation: Int) {
        settings.orientation = orientation
        notificationSample.update()
        MainController.update()
    }

    override fun onColorChooserResult(requestCode: Int, resultCode: Int, color: Int) {
        if (resultCode != Activity.RESULT_OK) return
        when (requestCode) {
            R.id.foreground -> {
                settings.foregroundColor = color
                binding.content.sampleForeground.setColorFilter(color)
            }
            R.id.background -> {
                settings.backgroundColor = color
                binding.content.sampleBackground.setColorFilter(color)
            }
            R.id.foreground_selected -> {
                settings.foregroundColorSelected = color
                binding.content.sampleForegroundSelected.setColorFilter(color)
            }
            R.id.background_selected -> {
                settings.backgroundColorSelected = color
                binding.content.sampleBackgroundSelected.setColorFilter(color)
            }
        }
        notificationSample.update()
        MainController.update()
    }

    override fun resetTheme() {
        settings.resetTheme()
        binding.content.sampleForeground.setColorFilter(settings.foregroundColor)
        binding.content.sampleBackground.setColorFilter(settings.backgroundColor)
        binding.content.sampleForegroundSelected.setColorFilter(settings.foregroundColorSelected)
        binding.content.sampleBackgroundSelected.setColorFilter(settings.backgroundColorSelected)
        notificationSample.update()
        MainController.update()
    }

    private fun setUpLayoutSelector() {
        orientationListStart = settings.orientationList
        orientationList.addAll(orientationListStart)

        checkList = Orientation.values.map { orientation ->
            CheckItemView(this).also { view ->
                view.orientation = orientation.orientation
                view.setIcon(orientation.icon)
                view.setText(orientation.label)
                view.setOnClickListener {
                    onClickCheckItem(view)
                    updateCaution()
                }
            }
        }
        checkList.forEachIndexed { index, view ->
            val params = GridLayout.LayoutParams(
                spec(index / 4),
                spec(index % 4, 1f)
            ).also {
                it.width = 0
                it.height = resources.getDimensionPixelSize(R.dimen.customize_height)
            }
            binding.content.checkHolder.addView(view, params)
        }
        applyLayoutSelection()
        binding.content.resetLayout.setOnClickListener { ResetLayoutDialog.show(this) }
        binding.content.helpLayout.setOnClickListener { OrientationHelpDialog.show(this) }
        updateCaution()
    }

    private fun updateCaution() {
        if (orientationList.any { Orientation.experimental.contains(it) }) {
            binding.content.caution.visibility = View.VISIBLE
        } else {
            binding.content.caution.visibility = View.GONE
        }
    }

    private fun onClickCheckItem(view: CheckItemView) {
        if (view.isChecked) {
            if (orientationList.size <= OrientationList.MIN) {
                Toast.makeText(this, R.string.toast_select_item_min, Toast.LENGTH_LONG).show()
            } else {
                orientationList.remove(view.orientation)
                view.isChecked = false
                updateLayoutSelector()
            }
        } else {
            if (orientationList.size >= OrientationList.MAX) {
                Toast.makeText(this, R.string.toast_select_item_max, Toast.LENGTH_LONG).show()
            } else {
                orientationList.add(view.orientation)
                view.isChecked = true
                updateLayoutSelector()
            }
        }
    }

    private fun updateLayoutSelector() {
        settings.orientationList = orientationList
        notificationSample.update()
        MainController.update()
    }

    override fun resetLayout() {
        orientationList.clear()
        orientationList.addAll(Default.orientationList)
        applyLayoutSelection()
        updateLayoutSelector()
        updateCaution()
    }

    private fun applyLayoutSelection() {
        checkList.forEach { view ->
            view.isChecked = orientationList.contains(view.orientation)
        }
    }

    private fun setUpUseRoundBackground() {
        binding.content.useRoundBackground.setOnClickListener {
            toggleUseRoundBackground()
        }
    }

    private fun applyUseRoundBackground() {
        binding.content.useRoundBackground.isChecked = settings.shouldUseRoundBackground
    }

    private fun toggleUseRoundBackground() {
        settings.shouldUseRoundBackground = !settings.shouldUseRoundBackground
        applyUseRoundBackground()
        MainController.update()
        notificationSample.update()
    }

    private fun setUpUseBlankIcon() {
        binding.content.useBlankIconForNotification.setOnClickListener { toggleUseBlankIcon() }
    }

    private fun applyUseBlankIcon() {
        binding.content.useBlankIconForNotification.isChecked = settings.shouldUseBlankIconForNotification
    }

    private fun toggleUseBlankIcon() {
        settings.shouldUseBlankIconForNotification = !settings.shouldUseBlankIconForNotification
        applyUseBlankIcon()
        MainController.update()
    }

    private fun setUpAutoRotateWarning() {
        binding.content.autoRotateWarning.setOnClickListener { toggleAutoRotateWarning() }
    }

    private fun applyAutoRotateWarning() {
        binding.content.autoRotateWarning.isChecked = settings.autoRotateWarning
    }

    private fun toggleAutoRotateWarning() {
        settings.autoRotateWarning = !settings.autoRotateWarning
        applyAutoRotateWarning()
    }

    private fun setUpNotificationPrivacy() {
        binding.content.notificationPrivacy.setOnClickListener { toggleNotificationPrivacy() }
    }

    private fun applyNotificationPrivacy() {
        binding.content.notificationPrivacy.isChecked = settings.notifySecret
    }

    private fun toggleNotificationPrivacy() {
        settings.notifySecret = !settings.notifySecret
        applyNotificationPrivacy()
        MainController.update()
    }

    private fun setUpSystemSetting() {
        binding.content.systemApp.setOnClickListener {
            SystemSettings.startApplicationDetailsSettings(this)
        }
        binding.content.systemNotification.setOnClickListener {
            SystemSettings.startAppNotificationSettings(this)
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, DetailedSettingsActivity::class.java))
        }
    }
}

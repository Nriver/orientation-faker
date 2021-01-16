/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.orientation.view

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.gridlayout.widget.GridLayout
import androidx.lifecycle.ViewModelProvider
import net.mm2d.android.orientationfaker.R
import net.mm2d.android.orientationfaker.databinding.FragmentDetailedSettingsBinding
import net.mm2d.color.chooser.ColorChooserDialog
import net.mm2d.orientation.control.Orientation
import net.mm2d.orientation.event.EventRouter
import net.mm2d.orientation.service.MainController
import net.mm2d.orientation.service.MainService
import net.mm2d.orientation.settings.Default
import net.mm2d.orientation.settings.IconShape
import net.mm2d.orientation.settings.OrientationList
import net.mm2d.orientation.settings.Settings
import net.mm2d.orientation.util.SystemSettings
import net.mm2d.orientation.util.alpha
import net.mm2d.orientation.util.opaque
import net.mm2d.orientation.view.dialog.*
import net.mm2d.orientation.view.view.CheckItemView

class DetailedSettingsFragment : Fragment(R.layout.fragment_detailed_settings),
    ColorChooserDialog.Callback {
    private val settings by lazy {
        Settings.get()
    }
    private lateinit var notificationSample: NotificationSample
    private lateinit var checkList: List<CheckItemView>
    private lateinit var orientationListStart: List<Int>
    private val orientationList: MutableList<Int> = mutableListOf()
    private lateinit var binding: FragmentDetailedSettingsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentDetailedSettingsBinding.bind(view)
        setHasOptionsMenu(false)
        setUpViews()
        EventRouter.observeUpdate(this) { notificationSample.update() }
        val provider = ViewModelProvider(this)
        provider.get(ResetThemeDialogViewModel::class.java)
            .resetThemeLiveData()
            .observe(viewLifecycleOwner, ::resetTheme)
        provider.get(ResetLayoutDialogViewModel::class.java)
            .resetLayoutLiveData()
            .observe(viewLifecycleOwner, ::resetLayout)
        provider.get(IconShapeDialogViewModel::class.java)
            .iconShapeLiveData()
            .observe(viewLifecycleOwner, ::onSelectIconShape)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!orientationList.contains(settings.orientation)) {
            settings.orientation = orientationList[0]
            MainController.update()
            if (!MainService.isStarted) {
                EventRouter.notifyUpdate()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        notificationSample.update()
        applyLayoutSelection()
        applyUseIconBackground()
        applyUseBlankIcon()
        applyAutoRotateWarning()
        applyNotificationPrivacy()
    }

    private fun setUpViews() {
        notificationSample = NotificationSample(binding.content.notificationSample)
        setUpSample()
        setUpLayoutSelector()
        setUpUseIconBackground()
        setUpIconShape()
        setUpUseBlankIcon()
        setUpAutoRotateWarning()
        setUpNotificationPrivacy()
        setUpSystemSetting()
    }

    private fun setUpSample() {
        binding.content.sampleForeground.setImageColor(settings.foregroundColor)
        binding.content.sampleBackground.setImageColor(settings.backgroundColor)
        binding.content.sampleForegroundSelected.setImageColor(settings.foregroundColorSelected)
        binding.content.sampleBackgroundSelected.setImageColor(settings.backgroundColorSelected)
        binding.content.sampleBase.setImageColor(settings.baseColor)
        binding.content.foreground.setOnClickListener {
            ColorChooserDialog.show(this, it.id, settings.foregroundColor, true)
        }
        binding.content.background.setOnClickListener {
            ColorChooserDialog.show(this, it.id, settings.backgroundColor, true)
        }
        binding.content.foregroundSelected.setOnClickListener {
            ColorChooserDialog.show(this, it.id, settings.foregroundColorSelected, true)
        }
        binding.content.backgroundSelected.setOnClickListener {
            ColorChooserDialog.show(this, it.id, settings.backgroundColorSelected, true)
        }
        binding.content.base.isVisible = settings.shouldUseIconBackground
        binding.content.base.setOnClickListener {
            ColorChooserDialog.show(this, it.id, settings.baseColor, true)
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
                binding.content.sampleForeground.setImageColor(color)
            }
            R.id.background -> {
                settings.backgroundColor = color
                binding.content.sampleBackground.setImageColor(color)
            }
            R.id.foreground_selected -> {
                settings.foregroundColorSelected = color
                binding.content.sampleForegroundSelected.setImageColor(color)
            }
            R.id.background_selected -> {
                settings.backgroundColorSelected = color
                binding.content.sampleBackgroundSelected.setImageColor(color)
            }
            R.id.base -> {
                settings.baseColor = color
                binding.content.sampleBase.setImageColor(color)
            }
        }
        notificationSample.update()
        MainController.update()
    }

    private fun resetTheme(unit: Unit?) {
        unit ?: return
        settings.resetTheme()
        binding.content.sampleForeground.setImageColor(settings.foregroundColor)
        binding.content.sampleBackground.setImageColor(settings.backgroundColor)
        binding.content.sampleForegroundSelected.setImageColor(settings.foregroundColorSelected)
        binding.content.sampleBackgroundSelected.setImageColor(settings.backgroundColorSelected)
        binding.content.sampleBase.setImageColor(settings.baseColor)
        notificationSample.update()
        MainController.update()
    }

    private fun ImageView.setImageColor(@ColorInt color: Int) {
        setColorFilter(color.opaque())
        imageAlpha = color.alpha()
    }

    private fun setUpLayoutSelector() {
        orientationListStart = settings.orientationList
        orientationList.addAll(orientationListStart)

        checkList = Orientation.values.map { orientation ->
            CheckItemView(requireContext()).also { view ->
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
                GridLayout.spec(index / 4),
                GridLayout.spec(index % 4, 1f)
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
                Toast.makeText(requireContext(), R.string.toast_select_item_min, Toast.LENGTH_LONG).show()
            } else {
                orientationList.remove(view.orientation)
                view.isChecked = false
                updateLayoutSelector()
            }
        } else {
            if (orientationList.size >= OrientationList.MAX) {
                Toast.makeText(requireContext(), R.string.toast_select_item_max, Toast.LENGTH_LONG).show()
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

    private fun resetLayout(unit: Unit?) {
        unit ?: return
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

    private fun setUpUseIconBackground() {
        binding.content.useIconBackground.setOnClickListener {
            toggleUseIconBackground()
        }
    }

    private fun applyUseIconBackground() {
        binding.content.useIconBackground.isChecked = settings.shouldUseIconBackground
    }

    private fun toggleUseIconBackground() {
        val useIcon = !settings.shouldUseIconBackground
        settings.shouldUseIconBackground = useIcon
        if (useIcon && !settings.hasBaseColor()) {
            settings.baseColor = settings.backgroundColor
            binding.content.sampleBase.setColorFilter(settings.baseColor)
        }
        binding.content.base.isVisible = useIcon
        applyUseIconBackground()
        applyIconShape()
        MainController.update()
        notificationSample.update()
    }

    private fun setUpIconShape() {
        applyIconShape()
        binding.content.iconShape.setOnClickListener {
            IconShapeDialog.show(this)
        }
    }

    private fun onSelectIconShape(iconShape: IconShape?) {
        iconShape ?: return
        settings.iconShape = iconShape
        applyIconShape()
        MainController.update()
        notificationSample.update()
    }

    private fun applyIconShape() {
        if (settings.shouldUseIconBackground) {
            binding.content.iconShape.isEnabled = true
            binding.content.iconShape.alpha = 1.0f
        } else {
            binding.content.iconShape.isEnabled = false
            binding.content.iconShape.alpha = 0.5f
        }
        val iconShape = settings.iconShape
        binding.content.iconShapeIcon.setImageResource(iconShape.iconId)
        binding.content.iconShapeDescription.setText(iconShape.textId)
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
            SystemSettings.startApplicationDetailsSettings(requireActivity())
        }
        binding.content.systemNotification.setOnClickListener {
            SystemSettings.startAppNotificationSettings(requireActivity())
        }
    }
}

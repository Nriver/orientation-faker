/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.orientation

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.appcompat.app.AppCompatDelegate
import net.mm2d.orientation.control.ForegroundPackageSettings
import net.mm2d.orientation.control.OrientationHelper
import net.mm2d.orientation.service.KeepAlive
import net.mm2d.orientation.service.MainController
import net.mm2d.orientation.settings.Settings
import net.mm2d.orientation.tabs.CustomTabsHelper
import net.mm2d.orientation.util.AdMob
import net.mm2d.orientation.view.notification.NotificationHelper

@Suppress("unused")
open class App : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeOverrideWhenDebug()
        Settings.initialize(this)
        MainController.initialize(this)
        NotificationHelper.createChannel(this)
        ForegroundPackageSettings.initialize(this)
        CustomTabsHelper.initialize(this)
        OrientationHelper.initialize(this)
        KeepAlive.ensureResident()
        AppCompatDelegate.setDefaultNightMode(Settings.get().nightMode)
        AdMob.initialize(this)
    }

    protected open fun initializeOverrideWhenDebug() {
        setUpStrictMode()
    }

    private fun setUpStrictMode() {
        StrictMode.setThreadPolicy(ThreadPolicy.LAX)
        StrictMode.setVmPolicy(VmPolicy.LAX)
    }
}

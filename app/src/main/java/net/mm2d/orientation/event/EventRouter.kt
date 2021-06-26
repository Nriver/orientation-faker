/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.orientation.event

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect

object EventRouter {
    private val updateFlow: MutableSharedFlow<Unit> = MutableSharedFlow()
    private val job = Job()
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + job)

    fun notifyUpdate() {
        scope.launch {
            updateFlow.emit(Unit)
        }
    }

    fun observeUpdate(owner: LifecycleOwner, callback: () -> Unit) {
        owner.lifecycleScope.launchWhenStarted {
            updateFlow.collect { callback() }
        }
    }
}

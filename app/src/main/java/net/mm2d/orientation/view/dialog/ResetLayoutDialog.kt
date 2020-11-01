package net.mm2d.orientation.view.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import net.mm2d.android.orientationfaker.R

class ResetLayoutDialog : DialogFragment() {
    interface Callback {
        fun resetLayout()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_title_reset_layout)
            .setMessage(R.string.dialog_message_reset_layout)
            .setPositiveButton(R.string.ok) { _, _ ->
                (activity as? Callback)?.resetLayout()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

    companion object {
        private const val TAG = "ResetLayoutDialog"

        fun show(activity: FragmentActivity) {
            val manager = activity.supportFragmentManager
            if (manager.isStateSaved || manager.findFragmentByTag(TAG) != null) return
            ResetLayoutDialog().show(manager, TAG)
        }
    }
}

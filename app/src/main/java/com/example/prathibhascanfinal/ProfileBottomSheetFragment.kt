package com.example.prathibhascanfinal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ProfileBottomSheetFragment : BottomSheetDialogFragment() {

    var onMenuItemClick: ((Int) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.menu_my_profile)?.setOnClickListener {
            onMenuItemClick?.invoke(R.id.menu_my_profile)
            dismiss()
        }
        view.findViewById<View>(R.id.menu_achievements)?.setOnClickListener {
            onMenuItemClick?.invoke(R.id.menu_achievements)
            dismiss()
        }
        view.findViewById<View>(R.id.menu_history)?.setOnClickListener {
            onMenuItemClick?.invoke(R.id.menu_history)
            dismiss()
        }
        view.findViewById<View>(R.id.menu_settings)?.setOnClickListener {
            onMenuItemClick?.invoke(R.id.menu_settings)
            dismiss()
        }
        view.findViewById<View>(R.id.menu_logout)?.setOnClickListener {
            onMenuItemClick?.invoke(R.id.menu_logout)
            dismiss()
        }
    }

    companion object {
        const val TAG = "ProfileBottomSheet"
    }
}

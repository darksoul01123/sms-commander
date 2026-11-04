package com.smstrigger.app

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

class LockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "LockAccessibility"
        private var instanceRef: WeakReference<LockAccessibilityService>? = null

        fun isRunning(): Boolean {
            return instanceRef?.get() != null
        }

        fun lockScreen(): Boolean {
            val service = instanceRef?.get()
            if (service == null) {
                Log.w(TAG, "Cannot lock screen: LockAccessibilityService is not active")
                return false
            }

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val success = service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                Log.i(TAG, "Screen lock action result: $success")
                success
            } else {
                Log.w(TAG, "Lock screen via AccessibilityService requires API 28+")
                false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
        Log.i(TAG, "LockAccessibilityService connected successfully")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event interception needed, service is used solely for global actions
    }

    override fun onInterrupt() {
        Log.w(TAG, "LockAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instanceRef?.get() == this) {
            instanceRef = null
        }
        Log.i(TAG, "LockAccessibilityService destroyed")
    }
}

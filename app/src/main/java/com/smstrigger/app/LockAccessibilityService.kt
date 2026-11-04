package com.smstrigger.app

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class LockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "LockAccessibility"

        // Strong reference — not garbage collected unlike WeakReference.
        // Nulled explicitly in onDestroy() to prevent memory leaks.
        @Volatile
        private var instance: LockAccessibilityService? = null

        fun isRunning(): Boolean = instance != null

        fun lockScreen(): Boolean {
            val service = instance
            if (service == null) {
                Log.w(TAG, "Cannot lock screen: LockAccessibilityService is not active")
                return false
            }

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val success =
                    service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                Log.i(TAG, "Screen lock action dispatched. Result: $success")
                success
            } else {
                Log.w(TAG, "Lock screen via AccessibilityService requires API 28+")
                false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "LockAccessibilityService connected and instance registered")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not needed — this service is used solely for global actions (lock screen)
    }

    override fun onInterrupt() {
        Log.w(TAG, "LockAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Null out explicitly to prevent stale reference after service is killed
        if (instance == this) {
            instance = null
            Log.i(TAG, "LockAccessibilityService destroyed — instance cleared")
        }
    }
}

package com.itsastrodude.blindeye

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class BlindEyeAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "BlindEye"

        // Debug flag — set to false for release builds to silence node tree logging
        private const val DEBUG_LOG_TREE = true

        // Global STOP flag. Layer 3 (Action Executor) will check this before every action.
        @Volatile var stopRequested = false
            private set

        fun requestStop() {
            stopRequested = true
            Log.d(TAG, "STOP requested.")
        }

        fun clearStop() {
            stopRequested = false
        }
    }

    private lateinit var overlayManager: OverlayManager

    override fun onServiceConnected() {
        super.onServiceConnected()

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            packageNames = null
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }

        overlayManager = OverlayManager(this)
        overlayManager.show()

        Log.d(TAG, "BlindEye connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Window state changes are the meaningful signal — content changes fire too often
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val rootNode = rootInActiveWindow ?: return
        val appName = event.packageName?.toString() ?: "Unknown"
        val elementCount = countClickableElements(rootNode)

        overlayManager.updateStatus(appName, elementCount)

        if (DEBUG_LOG_TREE) {
            Log.d(TAG, "--- Screen changed: $appName | clickable: $elementCount ---")
            readNodeTree(rootNode, depth = 0)
        }

        rootNode.recycle()
    }

    private fun countClickableElements(node: AccessibilityNodeInfo): Int {
        var count = if (node.isClickable) 1 else 0
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            count += countClickableElements(child)
            child.recycle()
        }
        return count
    }

    private fun readNodeTree(node: AccessibilityNodeInfo, depth: Int) {
        val indent = "  ".repeat(depth)
        val className = node.className ?: "Unknown"
        val text = node.text ?: ""
        val contentDesc = node.contentDescription ?: ""

        if (text.isNotEmpty() || contentDesc.isNotEmpty() || node.isClickable) {
            Log.d(TAG, "$indent[$className]" +
                    (if (text.isNotEmpty()) " text=\"$text\"" else "") +
                    (if (contentDesc.isNotEmpty()) " desc=\"$contentDesc\"" else "") +
                    (if (node.isClickable) " (clickable)" else "") +
                    (if (node.isEditable) " (editable)" else "")
            )
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            readNodeTree(child, depth + 1)
            child.recycle()
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "BlindEye interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager.hide()
    }
}

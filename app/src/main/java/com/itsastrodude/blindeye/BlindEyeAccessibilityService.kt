package com.itsastrodude.blindeye

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class BlindEyeAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "BlindEye"
    }

    // Called when the service is connected and ready
    override fun onServiceConnected() {
        super.onServiceConnected()

        // Tell Android what kinds of events we want to be notified about
        val info = AccessibilityServiceInfo().apply {
            // Notify us when the screen content changes
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

            // We want events from all apps, not just one
            packageNames = null

            // Return the full node tree so we can read all elements
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        serviceInfo = info

        Log.d(TAG, "BlindEye Accessibility Service connected.")
    }

    // Called every time an accessibility event happens on screen
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Get the root node of the current screen
        val rootNode = rootInActiveWindow ?: return

        Log.d(TAG, "--- New screen state ---")
        Log.d(TAG, "App: ${event.packageName}")
        Log.d(TAG, "Event type: ${AccessibilityEvent.eventTypeToString(event.eventType)}")

        // Walk the UI tree and print all interactive elements
        readNodeTree(rootNode, depth = 0)

        // Always recycle the root node when done to free memory
        rootNode.recycle()
    }

    // Recursively walks the UI element tree and logs what it finds
    private fun readNodeTree(node: AccessibilityNodeInfo, depth: Int) {
        val indent = "  ".repeat(depth)
        val className = node.className ?: "Unknown"
        val text = node.text ?: ""
        val contentDesc = node.contentDescription ?: ""
        val isClickable = node.isClickable
        val isEditable = node.isEditable

        // Only log elements that have some useful information
        if (text.isNotEmpty() || contentDesc.isNotEmpty() || isClickable) {
            Log.d(TAG, "$indent[$className]" +
                    (if (text.isNotEmpty()) " text=\"$text\"" else "") +
                    (if (contentDesc.isNotEmpty()) " desc=\"$contentDesc\"" else "") +
                    (if (isClickable) " (clickable)" else "") +
                    (if (isEditable) " (editable)" else "")
            )
        }

        // Recurse into child elements
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            readNodeTree(child, depth + 1)
            child.recycle()
        }
    }

    // Called when the service is interrupted — required but we leave it empty for now
    override fun onInterrupt() {
        Log.d(TAG, "BlindEye service interrupted.")
    }
}

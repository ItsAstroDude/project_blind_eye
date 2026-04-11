package com.itsastrodude.blindeye

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Single persistent root view — never torn down while service is alive
    private var rootView: FrameLayout? = null
    private var statusText: TextView? = null
    private var isExpanded = false

    private val collapsedParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.END
        x = 16
        y = 200
    }

    private val expandedParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM
    }

    fun show() {
        if (rootView != null) return
        buildViews()
        showCollapsed()
    }

    fun hide() {
        rootView?.let { windowManager.removeView(it) }
        rootView = null
        statusText = null
        isExpanded = false
    }

    fun updateStatus(appName: String, elementCount: Int) {
        statusText?.text = "Watching: $appName\nElements found: $elementCount"
    }

    // ── View construction (once) ──────────────────────────────────────────────

    private fun buildViews() {
        val root = FrameLayout(context)

        // ── Collapsed: small floating eye button ──────────────────────────────
        val eyeButton = Button(context).apply {
            text = "👁"
            textSize = 18f
            setPadding(16, 8, 16, 8)
            id = R.id.overlay_eye_button
            setOnClickListener { toggleExpanded() }
        }

        // ── Expanded: bottom panel ────────────────────────────────────────────
        val panel = LinearLayout(context).apply {
            id = R.id.overlay_panel
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xEE0A0A0A.toInt())
            setPadding(40, 32, 40, 40)
            visibility = View.GONE
        }

        val titleText = TextView(context).apply {
            text = "👁  BlindEye"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
        }

        val st = TextView(context).apply {
            text = "Watching screen..."
            textSize = 13f
            setTextColor(0xFFAAAAAA.toInt())
            setPadding(0, 12, 0, 20)
            id = R.id.overlay_status
        }
        statusText = st

        val stopButton = Button(context).apply {
            text = "✕  STOP"
            textSize = 15f
            setBackgroundColor(0xFFCC0000.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener { onStop() }
        }

        panel.addView(titleText)
        panel.addView(st)
        panel.addView(stopButton)

        root.addView(eyeButton)
        root.addView(panel)

        rootView = root
    }

    // ── Toggle helpers ────────────────────────────────────────────────────────

    private fun showCollapsed() {
        rootView?.let {
            it.findViewById<View>(R.id.overlay_eye_button)?.visibility = View.VISIBLE
            it.findViewById<View>(R.id.overlay_panel)?.visibility = View.GONE
            windowManager.addView(it, collapsedParams)
            isExpanded = false
        }
    }

    private fun toggleExpanded() {
        if (isExpanded) collapse() else expand()
    }

    private fun expand() {
        val root = rootView ?: return
        windowManager.removeView(root)
        root.findViewById<View>(R.id.overlay_eye_button)?.visibility = View.GONE
        root.findViewById<View>(R.id.overlay_panel)?.visibility = View.VISIBLE
        windowManager.addView(root, expandedParams)
        isExpanded = true
    }

    private fun collapse() {
        val root = rootView ?: return
        windowManager.removeView(root)
        root.findViewById<View>(R.id.overlay_eye_button)?.visibility = View.VISIBLE
        root.findViewById<View>(R.id.overlay_panel)?.visibility = View.GONE
        windowManager.addView(root, collapsedParams)
        isExpanded = false
    }

    // ── STOP ──────────────────────────────────────────────────────────────────

    private fun onStop() {
        BlindEyeAccessibilityService.requestStop()
        collapse()
    }
}

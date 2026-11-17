package com.limitcrosses.app.platform.service

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.limitcrosses.app.MainActivity

/**
 * System Overlay Window Manager.
 * When the daily social limit is reached (0 min remaining) and a user opens
 * Instagram, Snapchat, or Facebook, this manager places an impassable full-screen
 * overlay over the blocked app using Android's WindowManager (TYPE_APPLICATION_OVERLAY).
 * This completely prevents touch interaction with the social app underneath.
 */
object AppBlockerOverlayManager {

    private var currentOverlayView: View? = null
    private var windowManager: WindowManager? = null

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun showOverlay(context: Context, blockedPackage: String, appName: String) {
        if (!hasOverlayPermission(context)) {
            // Fallback: If overlay permission not granted, bounce to home screen and launch activity
            bounceToHome(context)
            launchLimitReachedActivity(context, blockedPackage, appName)
            return
        }

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        windowManager = wm

        // If overlay already showing, don't duplicate
        if (currentOverlayView != null) {
            return
        }

        try {
            val overlayView = buildOverlayView(context, appName, blockedPackage)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            wm.addView(overlayView, params)
            currentOverlayView = overlayView
        } catch (_: Exception) {
            // If window addition fails, fallback to launching MainActivity
            bounceToHome(context)
            launchLimitReachedActivity(context, blockedPackage, appName)
        }
    }

    fun hideOverlay() {
        try {
            if (currentOverlayView != null && windowManager != null) {
                windowManager?.removeView(currentOverlayView)
                currentOverlayView = null
            }
        } catch (_: Exception) {
            currentOverlayView = null
        }
    }

    private fun buildOverlayView(context: Context, appName: String, blockedPackage: String): View {
        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#E60B0E17")) // 90% opaque dark cosmic navy
            isClickable = true
            isFocusable = true
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#FF1E2438"))
                cornerRadius = dpToPx(context, 24f)
                setStroke(dpToPx(context, 2f).toInt(), Color.parseColor("#FFFF5252")) // Cosmic Danger border
            }
            background = bg
            val pad = dpToPx(context, 24f).toInt()
            setPadding(pad, pad, pad, pad)
        }

        val cardParams = FrameLayout.LayoutParams(
            dpToPx(context, 340f).toInt(),
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
            setMargins(dpToPx(context, 20f).toInt(), 0, dpToPx(context, 20f).toInt(), 0)
        }

        // Icon
        val icon = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_lock_lock)
            setColorFilter(Color.parseColor("#FFFF5252"))
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(context, 54f).toInt(),
                dpToPx(context, 54f).toInt()
            ).apply {
                bottomMargin = dpToPx(context, 16f).toInt()
            }
        }
        card.addView(icon)

        val isLoggedOut = com.limitcrosses.app.data.local.LocalDataStorage(context).getCurrentUserId() == null

        // Title
        val title = TextView(context).apply {
            text = if (isLoggedOut) "TIME EXCEEDED" else "DAILY LIMIT REACHED"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#FFF0F4FC"))
            gravity = Gravity.CENTER
        }
        card.addView(title)

        // Subtitle (Requirement 4)
        val subtitle = TextView(context).apply {
            text = if (isLoggedOut) "Focus on yourself now" else "Time is up for $appName today"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(if (isLoggedOut) "#FF00E5FF" else "#FFFF5252"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(context, 6f).toInt()
                bottomMargin = dpToPx(context, 12f).toInt()
            }
        }
        card.addView(subtitle)

        // Description
        val desc = TextView(context).apply {
            text = if (isLoggedOut)
                "You are signed out of Limit Crosses. Social apps remain locked. Step away from distractions and focus on yourself now."
            else
                "Your combined 0–45m social allowance has hit 0 minutes. Reset occurs at midnight (12:00 AM)."
            textSize = 13f
            setTextColor(Color.parseColor("#FF94A3B8"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(context, 20f).toInt()
            }
        }
        card.addView(desc)

        // Button: Solve Puzzles & Request Extra Time
        val btnPuzzles = Button(context).apply {
            text = "Solve 5 Puzzles for Extra Time"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            val btnBg = GradientDrawable().apply {
                setColor(Color.parseColor("#FF7C4DFF")) // Cosmic Primary
                cornerRadius = dpToPx(context, 14f)
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(context, 48f).toInt()
            ).apply {
                bottomMargin = dpToPx(context, 10f).toInt()
            }
            setOnClickListener {
                hideOverlay()
                launchLimitReachedActivity(context, blockedPackage, appName)
            }
        }
        card.addView(btnPuzzles)

        // Button: Return Home
        val btnHome = Button(context).apply {
            text = "Close to Home Screen"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#FF00E5FF"))
            val homeBg = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                cornerRadius = dpToPx(context, 14f)
                setStroke(dpToPx(context, 1.5f).toInt(), Color.parseColor("#FF00E5FF"))
            }
            background = homeBg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(context, 46f).toInt()
            ).apply {
                bottomMargin = dpToPx(context, 16f).toInt()
            }
            setOnClickListener {
                hideOverlay()
                bounceToHome(context)
            }
        }
        card.addView(btnHome)

        // Creator Attribution
        val footer = TextView(context).apply {
            text = "Created by Pankaj Yadav"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#FF00E5FF"))
            gravity = Gravity.CENTER
        }
        card.addView(footer)

        root.addView(card, cardParams)
        return root
    }

    private fun bounceToHome(context: Context) {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(homeIntent)
    }

    private fun launchLimitReachedActivity(context: Context, blockedPackage: String, appName: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = AppMonitorBlockerService.ACTION_LIMIT_REACHED
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("blocked_pkg", blockedPackage)
            putExtra("blocked_name", appName)
        }
        context.startActivity(intent)
    }

    private fun dpToPx(context: Context, dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
}


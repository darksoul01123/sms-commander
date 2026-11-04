package com.smstrigger.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PreferencesHelper

    private lateinit var switchEnabled: SwitchMaterial
    private lateinit var tvSmsStatus: TextView
    private lateinit var btnGrantSms: Button
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var btnOpenAccessibility: Button
    private lateinit var btnOpenMiuiAutostart: Button
    private lateinit var btnOpenBatterySettings: Button
    private lateinit var etTriggerCode: TextInputEditText
    private lateinit var rgMatchMode: RadioGroup
    private lateinit var rbExact: RadioButton
    private lateinit var rbContains: RadioButton
    private lateinit var btnSaveConfig: Button
    private lateinit var btnTestLockNow: Button
    private lateinit var tvLogs: TextView
    private lateinit var btnClearLogs: Button

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        updatePermissionStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = PreferencesHelper(this)

        initViews()
        bindEvents()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        refreshLogs()
    }

    private fun initViews() {
        switchEnabled = findViewById(R.id.switchEnabled)
        tvSmsStatus = findViewById(R.id.tvSmsStatus)
        btnGrantSms = findViewById(R.id.btnGrantSms)
        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus)
        btnOpenAccessibility = findViewById(R.id.btnOpenAccessibility)
        btnOpenMiuiAutostart = findViewById(R.id.btnOpenMiuiAutostart)
        btnOpenBatterySettings = findViewById(R.id.btnOpenBatterySettings)
        etTriggerCode = findViewById(R.id.etTriggerCode)
        rgMatchMode = findViewById(R.id.rgMatchMode)
        rbExact = findViewById(R.id.rbExact)
        rbContains = findViewById(R.id.rbContains)
        btnSaveConfig = findViewById(R.id.btnSaveConfig)
        btnTestLockNow = findViewById(R.id.btnTestLockNow)
        tvLogs = findViewById(R.id.tvLogs)
        btnClearLogs = findViewById(R.id.btnClearLogs)

        // Load saved state
        switchEnabled.isChecked = prefs.isEnabled
        etTriggerCode.setText(prefs.triggerCode)
        if (prefs.matchMode == PreferencesHelper.MATCH_CONTAINS) {
            rbContains.isChecked = true
        } else {
            rbExact.isChecked = true
        }
    }

    private fun bindEvents() {
        switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            prefs.isEnabled = isChecked
            Toast.makeText(
                this,
                if (isChecked) "Service enabled" else "Service disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnGrantSms.setOnClickListener {
            val permissions = mutableListOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            smsPermissionLauncher.launch(permissions.toTypedArray())
        }

        btnOpenAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(
                this,
                "Find 'SMS Commander Screen Lock' and turn it ON",
                Toast.LENGTH_LONG
            ).show()
        }

        btnOpenMiuiAutostart.setOnClickListener {
            openMiuiAutostart()
        }

        btnOpenBatterySettings.setOnClickListener {
            openBatteryOptimizationSettings()
        }

        btnSaveConfig.setOnClickListener {
            val code = etTriggerCode.text?.toString()?.trim() ?: ""
            if (code.isEmpty()) {
                Toast.makeText(this, "Trigger code cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.triggerCode = code
            prefs.matchMode = if (rbContains.isChecked) {
                PreferencesHelper.MATCH_CONTAINS
            } else {
                PreferencesHelper.MATCH_EXACT
            }
            Toast.makeText(this, "Saved: Trigger when text has \"$code\"", Toast.LENGTH_SHORT).show()
        }

        btnTestLockNow.setOnClickListener {
            if (!LockAccessibilityService.isRunning()) {
                Toast.makeText(
                    this,
                    "Accessibility Service is NOT enabled. Please enable it first.",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                return@setOnClickListener
            }

            ActionExecutor.executeLock(this, "Manual Test", prefs.triggerCode)
            refreshLogs()
        }

        btnClearLogs.setOnClickListener {
            prefs.clearLogs()
            refreshLogs()
        }
    }

    private fun updatePermissionStatus() {
        // 1. SMS Permission Check
        val hasSms = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasSms) {
            tvSmsStatus.text = "1. SMS Permission: Granted (Active)"
            tvSmsStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
            btnGrantSms.isEnabled = false
            btnGrantSms.text = "Granted"
        } else {
            tvSmsStatus.text = "1. SMS Permission: Not Granted"
            tvSmsStatus.setTextColor(ContextCompat.getColor(this, R.color.danger))
            btnGrantSms.isEnabled = true
            btnGrantSms.text = "Grant"
        }

        // 2. Accessibility Service Check
        val isServiceActive = LockAccessibilityService.isRunning()
        if (isServiceActive) {
            tvAccessibilityStatus.text = "2. Screen Lock Service: Running (Active)"
            tvAccessibilityStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
            btnOpenAccessibility.text = "Active"
        } else {
            tvAccessibilityStatus.text = "2. Screen Lock Service: Disabled"
            tvAccessibilityStatus.setTextColor(ContextCompat.getColor(this, R.color.danger))
            btnOpenAccessibility.text = "Enable"
        }
    }

    private fun refreshLogs() {
        val logs = prefs.getLogs()
        if (logs.isEmpty()) {
            tvLogs.text = "No events recorded yet."
        } else {
            tvLogs.text = logs.joinToString("\n\n")
        }
    }

    private fun openMiuiAutostart() {
        val miuiIntents = listOf(
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.letv.android.letvsafe",
                    "com.letv.android.letvsafe.AutobootManageActivity"
                )
            ),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        )

        for (intent in miuiIntents) {
            try {
                startActivity(intent)
                return
            } catch (e: Exception) {
            }
        }
    }

    private fun openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                    return
                } catch (e: Exception) {
                }
            }
        }

        // Fallback to Application details settings
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}

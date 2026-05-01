package app.yoanndev.reboxt.data

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Intent
import android.content.ComponentName
import android.util.Log

class ReboxtAccessibilityService : AccessibilityService() {

    private var currentSteps: List<AccessibilityStep>? = null
    private var currentStepIndex = 0

    fun startAutomation(schema: AccessibilitySchema) {
        currentSteps = schema.steps
        currentStepIndex = 0
        executeNextStep()
    }

    private fun executeNextStep() {
        val steps = currentSteps ?: return
        if (currentStepIndex >= steps.size) {
            Log.i("Accessibility", "Automation completed")
            currentSteps = null
            return
        }

        val step = steps[currentStepIndex]
        Log.d("Accessibility", "Executing step $currentStepIndex: ${step.type} - ${step.value}")

        when (step.type) {
            "launch_intent" -> {
                try {
                    val parts = step.value.split("/")
                    val intent = Intent().apply {
                        component = ComponentName(parts[0], parts[1])
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    currentStepIndex++
                    // On attend l'event d'accessibilité pour l'étape suivante
                } catch (e: Exception) {
                    Log.e("Accessibility", "Failed to launch intent", e)
                }
            }
            "find_id", "find_text" -> {
                val node = findNode(step)
                if (node != null) {
                    if (step.action == "click") {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    currentStepIndex++
                    // Attendez un peu ou l'event suivant
                }
            }
        }
    }

    private fun findNode(step: AccessibilityStep): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        return when (step.type) {
            "find_id" -> rootNode.findAccessibilityNodeInfosByViewId(step.value).firstOrNull()
            "find_text" -> rootNode.findAccessibilityNodeInfosByText(step.value).firstOrNull()
            else -> null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (currentSteps != null) {
            // Tentative d'exécution de l'étape suivante à chaque changement d'écran
            executeNextStep()
        }
    }

    override fun onInterrupt() {
        Log.w("Accessibility", "Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("Accessibility", "Service connected")
    }
}

package app.yoanndev.reboxt.data

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Intent
import android.content.ComponentName
import android.util.Log

class ReboxtAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ReboxtAccessibilityService? = null
    }

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
            Logger.i("Accessibility", "Automation completed successfully")
            currentSteps = null
            return
        }

        val step = steps[currentStepIndex]
        Logger.d("Accessibility", "Executing step $currentStepIndex: ${step.type} - ${step.value}")

        when (step.type) {
            "launch_intent" -> {
                try {
                    val parts = step.value.split("/")
                    val intent = Intent().apply {
                        component = ComponentName(parts[0], parts[1])
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    Logger.i("Accessibility", "Launching intent: ${parts[0]} / ${parts[1]}")
                    startActivity(intent)
                    currentStepIndex++
                } catch (e: Exception) {
                    Logger.e("Accessibility", "Failed to launch intent: ${step.value}", e)
                    currentSteps = null 
                }
            }
            "find_id", "find_text" -> {
                val node = findNode(step)
                if (node != null) {
                    Logger.i("Accessibility", "Found node for ${step.value}, performing ${step.action}")
                    if (step.action == "click") {
                        val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Logger.d("Accessibility", "Click action result: $clicked")
                    }
                    currentStepIndex++
                } else {
                    Logger.w("Accessibility", "Node not found for ${step.value} (Type: ${step.type}). Waiting for next event...")
                }
            }
        }
    }

    private fun findNode(step: AccessibilityStep): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            Logger.w("Accessibility", "rootInActiveWindow is null")
            return null
        }
        return when (step.type) {
            "find_id" -> {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(step.value)
                Logger.d("Accessibility", "Searching by ID: ${step.value}. Found ${nodes.size} nodes.")
                nodes.firstOrNull()
            }
            "find_text" -> {
                val nodes = rootNode.findAccessibilityNodeInfosByText(step.value)
                Logger.d("Accessibility", "Searching by Text: ${step.value}. Found ${nodes.size} nodes.")
                nodes.firstOrNull()
            }
            else -> null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (currentSteps != null && event != null) {
            Logger.d("Accessibility", "Event: ${AccessibilityEvent.eventTypeToString(event.eventType)} from ${event.packageName}")
            executeNextStep()
        }
    }

    override fun onInterrupt() {
        Logger.w("Accessibility", "Service interrupted")
    }

    fun i(tag: String, msg: String) = Logger.i(tag, msg)
    fun w(tag: String, msg: String) = Logger.w("Accessibility", msg) // Helper for accessibility specific warnings

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Logger.i("Accessibility", "Service connected and instance set")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}

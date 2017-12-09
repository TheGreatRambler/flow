package org.firstinspires.ftc.teamcode.nodes

import org.firstinspires.ftc.teamcode.Node
import org.firstinspires.ftc.teamcode.messages.*

/**
 * Created by shaash on 10/26/17.
 */

class GlyphHolderNode : Node("Glyph Holder") {
    // Top Holder Servo Position Constants

    fun getBottomPosition (state : GripperState) = when (state) {
        GripperState.OPEN -> 0.64
        GripperState.CLOSED -> 1.0
        GripperState.MIDDLE -> 0.8
    }
    fun getTopPosition (state : GripperState) = when (state) {
        GripperState.OPEN -> 0.3
        GripperState.CLOSED -> 0.7
        GripperState.MIDDLE -> 0.5
    }

    enum class HuggerStatus {
        OPEN,
        CLOSED,
        OFF
    }

    val cr_stop = 0.0
    val right_inward = 0.1
    val right_outward = -0.1

    val left_inward = -0.1
    val left_outward = 0.1


    var huggerStatus = HuggerStatus.OFF
    var huggerTime : Long = 0
    val huggerLimit = 3000
    var huggerCallback = {}

    // Holder States
    override fun subscriptions() {
        this.subscribe("/glyph/upper", {upper(it)})
        this.subscribe("/glyph/lower", {lower(it)})
        this.subscribe("/start", {start()})
        subscribe("/hugger", {updateHugger(it)})
        subscribe("/hugger/cancel", {cancelHugger()})
        subscribe("/heartbeat", {pollHugging()})
    }

    fun start() {
        this.publish("/servos/bottomServo", ServoMsg(getBottomPosition(GripperState.OPEN), 1))
        this.publish("/servos/topServo", ServoMsg(getTopPosition(GripperState.OPEN), 1))
    }

    fun lower(m : Message) {
        val (state) = m as GripperMsg
        this.publish("/servos/bottomServo", ServoMsg(getBottomPosition(state), priority = 1))
    }
    fun upper(m: Message){
        val (state) = m as GripperMsg
        this.publish("/servos/topServo", ServoMsg(getTopPosition(state), priority = 1))
    }

    fun updateHugger(m : Message){
        val (closeIt, onClosed) = m as HuggerMsg
        huggerCallback = onClosed
        if (!closeIt) { // NEED TO OPEN IT DOOD
            huggerTime = System.currentTimeMillis()
            this.publish("/crServos/hugger_l", MotorMsg(left_outward, 2))
            this.publish("/crServos/hugger_r", MotorMsg(right_outward, 2))
            huggerStatus = HuggerStatus.OPEN
        }
        else { // NEED TO CLOSE IT DOOD
            publish("/debug", TextMsg("Closin'"))
            huggerTime = System.currentTimeMillis()
            this.publish("/crServos/hugger_l", MotorMsg(left_inward, 2))
            this.publish("/crServos/hugger_r", MotorMsg(right_inward, 2))
            huggerStatus = HuggerStatus.CLOSED
        }
    }
    fun pollHugging() {
        if (huggerStatus == HuggerStatus.OPEN && System.currentTimeMillis()-huggerTime > huggerLimit) {
            this.publish("/crServos/hugger_l", MotorMsg(cr_stop, 0))
            this.publish("/crServos/hugger_r", MotorMsg(cr_stop, 0))
            huggerCallback()
            huggerCallback = {}
            huggerStatus = HuggerStatus.OFF
        }
        else if (huggerStatus == HuggerStatus.CLOSED && System.currentTimeMillis()-huggerTime > huggerLimit) { // Keep going tho
            publish("/debug", TextMsg("Done closin"))
            huggerCallback()
            huggerCallback = {}
            huggerStatus = HuggerStatus.OFF
        }
    }
    fun cancelHugger() {
        this.publish("/crServos/hugger_l", MotorMsg(cr_stop, 0))
        this.publish("/crServos/hugger_r", MotorMsg(cr_stop, 0))
    }
}
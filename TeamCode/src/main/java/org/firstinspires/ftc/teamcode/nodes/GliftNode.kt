package org.firstinspires.ftc.teamcode.nodes

/**
 * Created by shaash on 10/15/17.
 */

import org.firstinspires.ftc.teamcode.Node
import org.firstinspires.ftc.teamcode.messages.*

class GliftNode : Node("Glyph Lift") {
    override fun subscriptions() {
        this.subscribe("/gamepad1/dpad_up", {recieveUpMessage(upPower = (it as GamepadButtonMsg).value)})
        this.subscribe("/gamepad1/dpad_down", {recieveDownMessage(downPower = (it as GamepadButtonMsg).value)})
    }

    fun recieveUpMessage(upPower : Boolean) {
//        this.publish("/servos/bottomServo", ServoMsg(bottomClosedPosition, priority = 1))
//        this.publish("/servos/topServo", ServoMsg(topClosedPosition, priority = 1))
        this.publish("/crServos/liftServo", MotorMsg((if (upPower)-1.0 else 0.0), priority = 1))
    }
    fun recieveDownMessage(downPower : Boolean) {
        this.publish("/crServos/liftServo", MotorMsg((if (downPower)1.0 else 0.0), priority = 1))
    }
}

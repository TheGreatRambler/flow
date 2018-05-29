package org.firstinspires.ftc.teamcode.old.nodes.human

import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.teamcode.old.NodeOld
import org.firstinspires.ftc.teamcode.old.messages.Message
import org.firstinspires.ftc.teamcode.old.messages.MotorMsg
import org.firstinspires.ftc.teamcode.old.messages.TextMsg

/**
 * Created by shaash on 10/24/17.
 */
class DebugNodeOld : NodeOld("Debug") {
    override fun subscriptions() {
        this.subscribe("/status", {this.printStatus(it as TextMsg)})
        this.subscribe("/debug", {this.printMsg(it)})
        this.subscribe("/warn"){this.printWarning(it as TextMsg)}
        this.subscribe("/error", {this.printError(it as TextMsg)})
    }
    fun printMsg(m : Message){
        this.publish("/telemetry/line", TextMsg(m.toString()))
    }
    fun printStatus(m: TextMsg) {
        publish("/telemetry/line", TextMsg(text="[STATUS] ${m.text}", priority = 2))
    }
    fun printWarning(m : TextMsg) {
        this.publish("/telemetry/line", TextMsg(text="[WARNING] ${m.text}", priority = 0))
    }
    fun printError(m : TextMsg) {
        this.publish("/telemetry/line", TextMsg("[ERROR] ${m.text}", -1))
    }
}
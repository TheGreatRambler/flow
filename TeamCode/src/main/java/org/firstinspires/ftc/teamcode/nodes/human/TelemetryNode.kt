package org.firstinspires.ftc.teamcode.nodes.human

import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.teamcode.Node
import org.firstinspires.ftc.teamcode.messages.LinesMsg
import org.firstinspires.ftc.teamcode.messages.TextMsg

/**
 * Created by max on 11/10/17.
 */

/**
 * A centralized effector for sending telemetry messages.
 */
class TelemetryNode(val telemetry : Telemetry) : Node("Telemetry") {
    var staticCounter = 0
    init {
        telemetry.log().setCapacity(10)
        addLine(TextMsg("Warning: Servos/lifts will move on start."))
    }

    override fun subscriptions() {
        this.subscribe("/telemetry/line", {this.addLine(it as TextMsg)})
        this.subscribe("/telemetry/staticLine", {this.staticLine(it as TextMsg)})
        this.subscribe("/telemetry/lines", {this.renderLines(it as LinesMsg)})
        this.subscribe("/telemetry/clear", {this.clear()})
        this.subscribe("/telemetry/clearLines", {this.clearLines()})
    }

    fun addLine(m: TextMsg) {
        telemetry.log().add(m.text)
    }

    fun renderLines(msg: LinesMsg) {
        telemetry.clear()
        for (i in msg.lines.indices) {
            telemetry.addData("$i", msg.lines[i])
        }
    }

    fun staticLine(msg: TextMsg) {
        telemetry.addData("$staticCounter", msg.text)
        staticCounter++
    }

    fun clear() {
        telemetry.clearAll()
    }

    fun clearLines() {
        telemetry.log().clear()
    }
}
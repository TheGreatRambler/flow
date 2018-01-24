package org.firstinspires.ftc.teamcode.nodes.routines

import org.firstinspires.ftc.teamcode.RoutineNode
import org.firstinspires.ftc.teamcode.messages.AnalogMsg
import org.firstinspires.ftc.teamcode.messages.Message
import org.firstinspires.ftc.teamcode.messages.OmniDrive

/**
 * Created by max on 1/19/18.
 */

class DriveToCryptoboxRoutine : RoutineNode("Drive to cryptobox") {
    var buffer = Pair(0.0, 0.0)

    override fun subscriptions() {
        subscribe("/analog/ultra1", {bufferDistance(it)})
    }

    fun bufferDistance(m: Message) {
        val (value) = m as AnalogMsg
        // Making sure a drastic change is legit
        if (Math.abs(value - (buffer.first + buffer.second)/2) <= 0.03) {
            onDistance(value)
        }
        buffer = Pair(buffer.second, value)
    }

    fun onDistance(value : Double) {
        if (value <= 0.05) {
            publish("/drive", OmniDrive(0f, 0f, 0f, 1))
            end()
        }
    }

    override fun start() {
        publish("/drive", OmniDrive(0.2f, 0f, 0f, 1))
    }
}

package org.firstinspires.ftc.teamcode.messages

/**
 * Created by max on 8/31/17.
 */
data class OmniDrive(val upDown : Float, val leftRight : Float, val rotation : Float, override val priority: Int) : Message //motorVals go lf, rf, lb, rb

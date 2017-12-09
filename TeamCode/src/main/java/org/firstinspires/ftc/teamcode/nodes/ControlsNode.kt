package org.firstinspires.ftc.teamcode.nodes

import org.firstinspires.ftc.teamcode.Node
import org.firstinspires.ftc.teamcode.RoutineGroup
import org.firstinspires.ftc.teamcode.messages.*
import org.firstinspires.ftc.teamcode.nodes.routines.TimedCallbackRoutine
import org.firstinspires.ftc.teamcode.util.whenDown

/**
 * Created by max on 11/24/17.
 */

class ControlsNode : Node("Controls") {
    object gripperStates {
        var lower = GripperState.OPEN
        var upper = GripperState.OPEN
    }


    // Finite State Machine for Grippers
    fun gripperTransition(prevState: GripperState) = when(prevState) {
        GripperState.OPEN -> GripperState.CLOSED
        GripperState.CLOSED -> GripperState.OPEN
        GripperState.MIDDLE -> GripperState.OPEN
    }

    fun updateGrippers(lower : GripperState = gripperStates.lower, upper : GripperState = gripperStates.upper) {
        gripperStates.lower = lower
        gripperStates.upper = upper
        publish("/glyph/lower", GripperMsg(lower, 1))
        publish("/glyph/upper", GripperMsg(upper, 1))
    }

    override fun subscriptions() {
        /**
         * Press X to do the hugger macro.
         */
        subscribe("/gamepad1/x", whenDown {
            val routine = listOf(
                    TimedCallbackRoutine({
                        publish("/glift/middle", UnitMsg()) // Move glift up...
                    }, 1000, {cb ->
                        publish("/hugger", HuggerMsg(closeIt = true, onClosed = cb, priority = 1)) //... close the hugger
                    }),
                    TimedCallbackRoutine({
                        updateGrippers(lower=GripperState.MIDDLE) // loosen grip on block
                    }, 500, {cb -> cb()}),
                    TimedCallbackRoutine({
                        publish("/glift/bottom", UnitMsg()) // hugger now has block. move lift down
                    }, 2000, {cb ->
                        updateGrippers(upper = GripperState.CLOSED) // grab block with upper grabber
                        publish("/hugger", HuggerMsg(closeIt = false, onClosed = cb, priority = 1)) // open huggers
                    }) // donezo!
            )
            val routineGroup = RoutineGroup(routine)
            publish("/status", TextMsg("Hugger routine STARTED"))
            routineGroup.begin {
                publish("/status", TextMsg("Hugger routine FINISHED"))
            }
        })

        /**
         * Press A to toggle grabbing or ejecting a lower block.
         */
        subscribe("/gamepad1/a", whenDown {
            updateGrippers(lower = gripperTransition(gripperStates.lower))
        })

        /**
         * Press B to toggle grabbing or ejecting an upper block.
         */
        subscribe("/gamepad1/b", whenDown {
            updateGrippers(upper = gripperTransition(gripperStates.upper))
        })

        /**
         * Press and hold LT when delivering blocks into the shelf. Release when done.
         */
        subscribe("/gamepad1/left_trigger", {msg ->
            val (value) = (msg as GamepadJoyOrTrigMsg)
            // If intent detected
            if (value > 0.5) updateGrippers(lower=GripperState.MIDDLE, upper=GripperState.MIDDLE)
            // If user is done and not the initial push-in. Ready to collect.
            else if(gripperStates.lower == GripperState.MIDDLE && gripperStates.upper == GripperState.MIDDLE) {
                updateGrippers(lower=GripperState.OPEN, upper=GripperState.OPEN)
            }
        })

        /**
         * Press RT to grab both blocks.
         */
        subscribe("/gamepad1/right_trigger", whenDown {
            updateGrippers(lower=GripperState.CLOSED, upper = GripperState.CLOSED)
        })

        /**
         * TEST BUTTON 1: Turn 30º with PID
         */
        subscribe("/gamepad1/left_stick_button", whenDown {
            publish("/AngleTurning/turnTo", AngleTurnMsg(angle = 30.0, callback = {}, priority = 1))
        })

        /**
         * TEST BUTTON 2: Cancel PID Turn and hugger.
         */
        subscribe("/gamepad1/right_stick_button", whenDown {
            publish("/AngleTurning/cancel", UnitMsg())
            publish("/hugger/cancel", UnitMsg())
        })
    }
}

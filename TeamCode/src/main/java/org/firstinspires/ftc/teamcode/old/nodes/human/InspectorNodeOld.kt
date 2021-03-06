package org.firstinspires.ftc.teamcode.old.nodes.human

import org.firstinspires.ftc.teamcode.old.Dispatcher
import org.firstinspires.ftc.teamcode.old.NodeOld
import org.firstinspires.ftc.teamcode.old.messages.*
import org.firstinspires.ftc.teamcode.old.util.whenDown
import java.util.*
import org.firstinspires.ftc.teamcode.old.messages.IncrementState

/**
 * Created by max on 11/10/17.
 */
class InspectorNodeOld : NodeOld("Inspector") {
    enum class STATES { OFF,
        MAIN, // main menu
        INSPECTALL, // list of channels
        CHANNELOPT, // channel menu
        TAIL, // watch live stream of channel
        CMD, // command pallette
        TAILING
    }
    var state = STATES.OFF
    var tailId = ""
    var tailName = ""

    override fun subscriptions() {
        this.subscribe("/gamepad1/y", whenDown { initialize() })
        this.subscribe("/gamepad1/y", {tailBack(it as GamepadButtonMsg)} )
    }

    fun initialize() {
        if (this.state == STATES.OFF) {
            main()
        }
    }
    fun main() {
            this.state = STATES.MAIN
            val menu = hashMapOf(
                    "Inspect Channels" to { inspectAll("/") },
                    "Disable All Channels" to { disableAll() },
                    "Global Command Palette" to {commandPalette()},
                    "Exit" to { stopInspector() }
            )
            this.publish("/selector/begin", CallbackMapMsg(menu, priority = 1))
    }

    fun isDisabled(channel: String) : Boolean { // if not unlocked (null) or >-1
        return (Dispatcher.channels[channel]?.first ?: 0) == -1
    }

    // Global command palette with pre-assigned channel destinations.
    // For stuff where you know it's going.
    fun commandPalette() {
        val AngleTurn30 = AngleTurnMsg(angle=30.0, callback = {}, priority = 1)
        val AngleTurnNeg90 = AngleTurnMsg(angle=-90.0, callback = {}, priority = 1)

        val menu = hashMapOf(
                "/AngleTurning/turnTo $AngleTurn30" to {this.publish("/AngleTurning/turnTo", AngleTurn30)},
                "/AngleTurning/turnTo $AngleTurnNeg90" to {this.publish("/AngleTurning/turnTo", AngleTurnNeg90)},
                "/drive/straight" to {this.publish("/drive/straight", DriveStraightMsg(0.0, 0.25, false, 1))},
                "Back" to {main()}
        )
        this.publish("/selector/begin", CallbackMapMsg(menu, priority = 1))
    }

    // Starts at "/" and truncates all the sub-routes.
    // Once you call it with "/servo" it shows everything prefixed with "/servo"
    fun inspectAll(currentRoute : String) {
        this.state = STATES.INSPECTALL
        val menu = HashMap<String, () -> Unit>()
        for (channel in Dispatcher.channels.keys) {
            if (channel.startsWith(currentRoute)) { // my sincere apologies
                val substr = channel.substring(currentRoute.length)
                if (!substr.contains("/")) {
                    menu.put("${if(isDisabled(channel)) "[DISABLED]" else ""} $channel [${Dispatcher.channels[channel]?.second?.size ?: 0} subs]", {inspect(channel)})
                }
                else {
                    if (substr.indexOf("/") == 0){
                        if (substr.substring(1).contains("/")) {
                            val cname = substr.substring(0, 1+substr.substring(1).indexOf("/"))
                            menu.put(cname, {inspectAll(currentRoute + cname)})
                        }
                        else {
                            menu.put("${if (isDisabled(channel)) "[DISABLED]" else ""} $channel [${Dispatcher.channels[channel]?.second?.size ?: 0} subs]", { inspect(channel) })
                        }
                    }
                    else {
                        val cname = substr.substring(0, substr.indexOf("/"))
                        menu.put(cname, {inspectAll(currentRoute + cname)})
                    }
                }
            }
        }
        menu.put("Back", {inspectBack(currentRoute)})
        this.publish("/selector/begin", CallbackMapMsg(menu, 0))
    }

    // Go "up" a channel route
    fun inspectBack(channel: String) {
        if(channel == "/") main()
        else {
            val x = channel.indexOfLast{it == '/'}
            if (x == 0) inspectAll("/")
            else inspectAll(channel.substring(0, x))
        }
    }

    // Channel inspection menu.
    fun inspect(channel : String) {
        this.state = STATES.CHANNELOPT

        val menu = hashMapOf(
                "${if (isDisabled(channel)) "Enable" else "Disable"}" to {
                    if (isDisabled(channel)) {
                        // POTENTIAL BUG NOTE: this unlocks channel completely,
                        // so any restrictions before are gone.
                        Dispatcher.unlock(channel)
                        this.publish("/selector/update", UpdateMsg<String>("Enable", "Disable"))
                    }
                    else {
                        Dispatcher.lock(channel, -1)
                        this.publish("/selector/update", UpdateMsg<String>("Disable", "Enable"))
                    }
                },
                "Tail" to {
                    this.publish("/selector/end", UnitMsg())
                    tail(channel)
                },
                "Command Palette" to {
                  channelCommandPalette(channel)
                },
                "Back" to {inspectBack(channel)}
        )

        this.publish("/selector/begin", CallbackMapMsg(menu, 0))
    }

    // Channel specific commands menu item
    fun channelCommandPalette(channel: String) {
        this.state = STATES.CMD
        val menu = hashMapOf(
                "If there's an error at the bottom when sending these, you sent to the wrong channel / didn't handle it right." to {},
                "[Increment] +0.1" to {this.publish(channel, IncrementMsg(IncrementState.INCREMENT, 0.1))},
                "[Increment] -0.1" to {this.publish(channel, IncrementMsg(IncrementState.INCREMENT, -0.1))},
                "[Increment] +0.05" to {this.publish(channel, IncrementMsg(IncrementState.INCREMENT, 0.05))},
                "[Increment] -0.05" to {this.publish(channel, IncrementMsg(IncrementState.INCREMENT, -0.05))},
                "[Increment] +0.025" to {this.publish(channel, IncrementMsg(IncrementState.INCREMENT, 0.025))},
                "[Increment] -0.025" to {this.publish(channel, IncrementMsg(IncrementState.INCREMENT, -0.025))},
                "[Increment] +0.001" to {this.publish(channel, IncrementMsg(IncrementState.INCREMENT, 0.001))},
                "[Increment] -0.001" to {this.publish(channel, IncrementMsg(IncrementState.INCREMENT, -0.001))},
                "[Increment] Zero" to {this.publish(channel, IncrementMsg(IncrementState.ZERO))},
                "Back" to {this.inspect(channel)}
        )
        this.publish("/selector/begin", CallbackMapMsg(menu, 0))
    }

    // Monitor the channel's chatter.
    fun tail(channel: String) {
        this.publish("/telemetry/clear", UnitMsg())
        this.publish("/telemetry/staticLine", TextMsg("Tailing $channel"))
        this.publish("/telemetry/staticLine", TextMsg("Press Y to go back"))
        Dispatcher.lock("/debug", -1)
        tailName = channel
        tailId = subscribe(channel, {this.publish("/telemetry/line", TextMsg(it.toString()))})
        this.state = STATES.TAIL
    }

    // Really edgy unsubscribing function for tail()
    fun tailBack(m: GamepadButtonMsg) {
        if(state == STATES.TAIL && !m.value ) {
            this.state = STATES.TAILING
        }
        else if(state == STATES.TAILING && m.value) {
            Dispatcher.unlock("/debug")
            Dispatcher.unsubscribe(tailName, tailId)
            this.publish("/telemetry/clear", UnitMsg())
            inspect(tailName)
        }
    }

    // Disable all the channels except...
    fun disableAll() {
        for (key in Dispatcher.channels.keys) {
            val whiteList = arrayOf("/telemetry/line",
                    "/gamepad1/dpad_left",
                    "/gamepad1/dpad_right",
                    "/gamepad1/y",
                    "/telemetry/staticLine",
                    "/telemetry/lines",
                    "/telemetry/clear",
                    "/warn",
                    "/error",
                    "/start",
                    "/stop"
            )
            if(!whiteList.contains(key)) Dispatcher.lock(key, -1)
        }
        this.publish("/selector/update", UpdateMsg<String>("Disable All Channels", "All Channels Disabled."))
    }

    // Bye bye!!!
    fun stopInspector() {
        state = STATES.OFF
        this.publish("/selector/end", UnitMsg())
    }
}

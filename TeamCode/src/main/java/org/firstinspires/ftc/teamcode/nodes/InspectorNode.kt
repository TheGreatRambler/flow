package org.firstinspires.ftc.teamcode.nodes

import org.firstinspires.ftc.teamcode.Dispatcher
import org.firstinspires.ftc.teamcode.Node
import org.firstinspires.ftc.teamcode.messages.*
import org.firstinspires.ftc.teamcode.util.whenDown
import java.util.*
import org.firstinspires.ftc.teamcode.messages.IncrementState

/**
 * Created by max on 11/10/17.
 */
class InspectorNode : Node("Inspector") {
    enum class STATES { OFF,
        MAIN, // main menu
        INSPECTALL, // list of channels
        CHANNELOPT, // channel menu
        TAIL, // watch live stream of channel
        CMD, // command pallette
        TAILING
    }
    var state = STATES.OFF
    var tailIndice = 0
    var tailName = ""

    override fun subscriptions() {
        this.subscribe("/gamepad1/y", whenDown { start() })
        this.subscribe("/gamepad1/y", {tailBack(it as GamepadButtonMsg)} )
    }

    fun start() {
        if (this.state == STATES.OFF) {
            main()
        }
    }
    fun main() {
            this.state = STATES.MAIN
            val menu = hashMapOf(
                    "Inspect Channels" to { inspectAll("/") },
                    "Disable All Channels" to { disableAll() },
                    "Exit" to { end() }
            )
            this.publish("/selector/begin", CallbackMapMsg(menu, priority = 1))
    }

    fun isDisabled(channel: String) : Boolean { // if not unlocked (null) or >-1
        return (Dispatcher.channels[channel]?.first ?: 0) == -1
    }

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

    fun inspectBack(channel: String) {
        if(channel == "/") main()
        else {
            val x = channel.indexOfLast{it == '/'}
            if (x == 0) inspectAll("/")
            else inspectAll(channel.substring(0, x))
        }
    }

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
                  commandPalette(channel)
                },
                "Back" to {inspectBack(channel)}
        )

        this.publish("/selector/begin", CallbackMapMsg(menu, 0))
    }

    fun commandPalette(channel: String) {
        this.state = STATES.CMD
        val menu = hashMapOf(
                "Don't send this to sketchy channels." to {},
                "Increment +0.1" to {this.publish(channel, IncrementMsg(IncrementState.INCREMENT, 0.1))},
                "Increment -0.1" to {this.publish(channel, IncrementMsg(IncrementState.INCREMENT, -0.1))},
                "Zero" to {this.publish(channel, IncrementMsg(IncrementState.ZERO))},
                "Back" to {this.inspect(channel)}
        )
        this.publish("/selector/begin", CallbackMapMsg(menu, 0))
    }

    fun tail(channel: String) {
        this.publish("/telemetry/clear", UnitMsg())
        this.publish("/telemetry/staticLine", TextMsg("Tailing $channel"))
        this.publish("/telemetry/staticLine", TextMsg("Press Y to go back"))
        Dispatcher.lock("/debug", -1)
        tailIndice = Dispatcher.channels[channel]?.second?.size ?: -1
        tailName = channel
        this.subscribe(channel, {this.publish("/telemetry/line", TextMsg(it.toString()))})
        this.state = STATES.TAIL
    }

    fun tailBack(m: GamepadButtonMsg) {
        if(state == STATES.TAIL && !m.value ) {
            this.state = STATES.TAILING
        }
        else if(state == STATES.TAILING && m.value) {
            // TODO: Scarily risky, but it'll do
            Dispatcher.unlock("/debug")
            Dispatcher.channels[tailName]?.second?.removeAt(tailIndice)
            this.publish("/telemetry/clear", UnitMsg())
            inspect(tailName)
        }
    }

    fun disableAll() {
        for (key in Dispatcher.channels.keys) {
            val whiteList = arrayOf("/telemetry/line",
                    "/gamepad1/dpad_left",
                    "/gamepad1/dpad_right",
                    "/gamepad1/y",
                    "/gamepad1/start",
                    "/telemetry/staticLine",
                    "/telemetry/lines",
                    "/telemetry/clear",
                    "/warn",
                    "/error"
            )
            if(!whiteList.contains(key)) Dispatcher.lock(key, -1)
        }
        this.publish("/selector/update", UpdateMsg<String>("Disable All Channels", "All Channels Disabled."))
    }

    fun end() {
        state = STATES.OFF
        this.publish("/selector/end", UnitMsg())
    }
}

package net.bnbdiscord.borderkit.commands

import net.bnbdiscord.borderkit.database.DatabaseManager
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.plugin.Plugin

// Return instead of null for no autocompletion. Returning null autocompletes to the player's username, not nothing.
val EMPTY = listOf<String>()

class PassportCommandCompleter(val plugin: Plugin, val db: DatabaseManager) : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String> {
        if (args.isEmpty()) return EMPTY

        if (args.size == 1) {
            val result = mutableListOf("attest", "dattest", "query", "ruleset", "sign")
            if (sender.hasPermission("borderkit.jurisdiction"))
                result.add("jurisdiction")

            return result.filter { it.startsWith(args[0]) }
        }

        return when (args[0]) {
            // /passport attest <jurisdictionCode> <rulesetName> <selector>
            "attest" -> when (args.size) {
                2 -> anyJurisdictionCode(args[1])
                3 -> ruleset(args[1], args[2])
                4 -> selector(args[3])
                else -> EMPTY
            }
            // /passport dattest <jurisdictionCode1> <rulesetName1> <jurisdictionCode2> <rulesetName2> <selector>
            "dattest" -> when (args.size) {
                2 -> anyJurisdictionCode(args[1])
                3 -> ruleset(args[1], args[2])
                4 -> anyJurisdictionCode(args[3])
                5 -> ruleset(args[3], args[4])
                6 -> selector(args[5])
                else -> EMPTY
            }
            // /passport jurisdiction ...
            "jurisdiction" -> when {
                !sender.hasPermission("borderkit.jurisdiction") -> EMPTY
                args.size == 2 -> listOf("add", "update", "remove").filter { it.startsWith(args[1]) }
                else -> when (args[1]) {
                    // /passport jurisdiction add <jurisdictionCode: anything> <name: anything>
                    "add" -> EMPTY
                    // /passport jurisdiction update <jurisdictionCode> <name: anything>
                    "update" -> when (args.size) {
                        3 -> anyJurisdictionCode(args[2])
                        else -> EMPTY
                    }
                    // /passport jurisdiction remove <jurisdictionCode>
                    "remove" -> when (args.size) {
                        3 -> anyJurisdictionCode(args[2])
                        else -> EMPTY
                    }
                    else -> EMPTY
                }
            }
            // /passport ruleset ...
            "ruleset" -> when (args.size) {
                2 -> listOf("editor", "remove", "set").filter { it.startsWith(args[1]) }
                else -> when (args[1]) {
                    // /passport ruleset editor <jurisdictionCode>
                    "editor" -> when (args.size) {
                        3 -> jurisdictionCodeWithPermission(args[2], "borderkit.jurisdiction.", sender)
                        else -> EMPTY
                    }
                    // /passport ruleset remove|set <jurisdictionCode> <name>
                    "remove", "set" -> when (args.size) {
                        3 -> jurisdictionCodeWithPermission(args[2], "borderkit.jurisdiction.", sender)
                        4 -> ruleSetWithPermission(args[2], args[3], sender)
                        else -> EMPTY
                    }
                    else -> EMPTY
                }
            }
            // /passport sign <jurisdictionCode>
            "sign" -> when (args.size) {
                2 -> jurisdictionCodeWithPermission(args[1], "borderkit.passport.sign.", sender)
                else -> EMPTY
            }
            else -> EMPTY
        }
    }

    private fun anyJurisdictionCode(arg: String): List<String> =
        db.jurisdictionDao.queryBuilder()
            .where()
            .like("code", "$arg%")
            .query()
            .map { it.code }

    private fun jurisdictionCodeWithPermission(arg: String, permissionPrefix: String, sender: CommandSender): List<String> =
        anyJurisdictionCode(arg)
            .filter { sender.hasPermission(permissionPrefix + it.lowercase()) }

    private fun ruleset(jurisdictionCode: String, arg: String): List<String> {
        return db.rulesetDao.queryBuilder()
            .where()
            .eq("jurisdiction_id", jurisdictionCode)
            .and()
            .like("name", "$arg%")
            .query()
            .map { it.name }
    }

    private fun ruleSetWithPermission(jurisdictionCode: String, arg: String, sender: CommandSender): List<String> {
        if (!sender.hasPermission("borderkit.jurisdiction." + jurisdictionCode.lowercase())) return EMPTY

        return ruleset(jurisdictionCode, arg)
    }

    private fun selector(arg: String): List<String> {
        val result = mutableListOf("@a", "@e", "@p", "@r", "@s")
        result.addAll(plugin.server.onlinePlayers.map { it.name })
        return result.filter { it.lowercase().startsWith(arg.lowercase()) }
    }
}
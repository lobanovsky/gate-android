package ru.housekpr.gate.services

import ru.housekpr.gate.models.Device
import ru.housekpr.gate.models.GateAction
import ru.housekpr.gate.models.GateArea
import ru.housekpr.gate.models.GateDirection
import ru.housekpr.gate.models.GateSection
import ru.housekpr.gate.models.UserDevices
import ru.housekpr.gate.models.Zone

object GateLayoutBuilder {
    fun build(userDevices: UserDevices): List<GateSection> {
        return GateArea.entries.map { area ->
            val zone = bestZone(area, userDevices.zones)
            GateSection(
                area = area,
                actions = buildActions(zone)
            )
        }
    }

    private fun bestZone(area: GateArea, zones: List<Zone>): Zone? {
        return zones
            .map { zone -> zone to score(area, zone.name) }
            .sortedWith(compareByDescending<Pair<Zone, Int>> { it.second }.thenBy { it.first.id })
            .firstOrNull { it.second > 0 }
            ?.first
    }

    private fun buildActions(zone: Zone?): Map<GateDirection, GateAction> {
        if (zone == null) return emptyMap()

        val actions = linkedMapOf<GateDirection, GateAction>()
        val devices = zone.devices

        firstMatchingDevice(devices, listOf("заех", "въезд", "въез", "enter", "in"))?.let { device ->
            actions[GateDirection.ENTER] = GateAction(GateDirection.ENTER, device)
        }

        firstMatchingDevice(devices, listOf("выех", "выезд", "exit", "out"))?.let { device ->
            actions[GateDirection.EXIT] = GateAction(GateDirection.EXIT, device)
        }

        val unused = devices.filterNot { device ->
            actions.values.any { it.device == device }
        }
        if (GateDirection.ENTER !in actions && unused.isNotEmpty()) {
            actions[GateDirection.ENTER] = GateAction(GateDirection.ENTER, unused.first())
        }

        val remaining = devices.filterNot { device ->
            actions.values.any { it.device == device }
        }
        if (GateDirection.EXIT !in actions && remaining.isNotEmpty()) {
            actions[GateDirection.EXIT] = GateAction(GateDirection.EXIT, remaining.first())
        }

        return actions
    }

    private fun score(area: GateArea, zoneName: String): Int {
        val name = zoneName.lowercase()
        val keywords = when (area) {
            GateArea.COURTYARD -> listOf("двор", "террит", "шлагбаум")
            GateArea.PARKING -> listOf("паркинг", "гараж", "ворота")
        }
        return keywords.sumOf { if (name.contains(it)) 1 else 0 }
    }

    private fun firstMatchingDevice(devices: List<Device>, keywords: List<String>): Device? {
        return devices.firstOrNull { device ->
            val haystack = "${device.name} ${device.label}".lowercase()
            keywords.any { haystack.contains(it) }
        }
    }
}

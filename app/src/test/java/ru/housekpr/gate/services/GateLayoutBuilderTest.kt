package ru.housekpr.gate.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.housekpr.gate.models.Device
import ru.housekpr.gate.models.GateArea
import ru.housekpr.gate.models.GateDirection
import ru.housekpr.gate.models.UserDevices
import ru.housekpr.gate.models.Zone

class GateLayoutBuilderTest {
    @Test
    fun buildMapsCourtyardAndParkingDevices() {
        val devices = UserDevices(
            userId = "42",
            zones = listOf(
                Zone(
                    id = 1,
                    name = "Двор",
                    devices = listOf(
                        Device("1", "Двор-заезд", "Заехать", null, null, "a"),
                        Device("2", "Двор-выезд", "Выехать", null, null, "b")
                    )
                ),
                Zone(
                    id = 2,
                    name = "Паркинг",
                    devices = listOf(
                        Device("3", "Паркинг въезд", "Заехать", null, null, "c"),
                        Device("4", "Паркинг выезд", "Выехать", null, null, "d")
                    )
                )
            )
        )

        val sections = GateLayoutBuilder.build(devices)

        assertEquals("1", sections.first { it.area == GateArea.COURTYARD }.actions[GateDirection.ENTER]?.device?.id)
        assertEquals("2", sections.first { it.area == GateArea.COURTYARD }.actions[GateDirection.EXIT]?.device?.id)
        assertEquals("3", sections.first { it.area == GateArea.PARKING }.actions[GateDirection.ENTER]?.device?.id)
        assertEquals("4", sections.first { it.area == GateArea.PARKING }.actions[GateDirection.EXIT]?.device?.id)
    }

    @Test
    fun buildFallsBackToDeviceOrderWithoutKeywords() {
        val devices = UserDevices(
            userId = "42",
            zones = listOf(
                Zone(
                    id = 1,
                    name = "Двор",
                    devices = listOf(
                        Device("1", "Устройство A", "Кнопка 1", null, null, "a"),
                        Device("2", "Устройство B", "Кнопка 2", null, null, "b")
                    )
                )
            )
        )

        val section = GateLayoutBuilder.build(devices).first { it.area == GateArea.COURTYARD }

        assertEquals("1", section.actions[GateDirection.ENTER]?.device?.id)
        assertEquals("2", section.actions[GateDirection.EXIT]?.device?.id)
    }

    @Test
    fun buildReturnsEmptyActionsWhenZoneIsMissing() {
        val devices = UserDevices(userId = "42", zones = emptyList())

        val sections = GateLayoutBuilder.build(devices)

        assertTrue(sections.all { it.actions.isEmpty() })
    }
}

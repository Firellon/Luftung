package dev.macfi.luftung.widget

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class VentilationWidgetInfoTest {
    @Test
    fun widgetProviderDeclaresTwoByTwoTargetSize() {
        val xml = File("src/main/res/xml/ventilation_widget_info.xml").readText()

        assertTrue(xml.contains("android:targetCellWidth=\"2\""))
        assertTrue(xml.contains("android:targetCellHeight=\"2\""))
    }
}

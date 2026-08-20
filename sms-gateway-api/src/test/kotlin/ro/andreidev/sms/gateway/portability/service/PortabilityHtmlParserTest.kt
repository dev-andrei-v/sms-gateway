package ro.andreidev.sms.gateway.portability.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PortabilityHtmlParserTest {
    private val parser = PortabilityHtmlParser()

    @Test
    fun `parses a ported lookup page`() {
        val response = parser.parse(
            html = PORTED_HTML,
            phoneNumber = "0799991518",
            queryUrl = "https://www.portabilitate.ro/ro-no-0799991518",
            language = "ro",
        )

        assertEquals("ported", response.status)
        assertTrue(response.ported == true)
        assertEquals("ORANGE ROMANIA", response.operators.current)
        assertEquals("VODAFONE ROMANIA", response.operators.initial)
        assertEquals("2026-04-25T13:32:29", response.timestamps.currentIso)
        assertEquals("2026-04-25", response.timestamps.infoValidOnIso)
        assertEquals("Mobil", response.numberType)
    }

    @Test
    fun `parses a non ported lookup page`() {
        val response = parser.parse(
            html = NOT_PORTED_HTML,
            phoneNumber = "0318118016",
            queryUrl = "https://www.portabilitate.ro/ro-no-0318118016",
            language = "ro",
        )

        assertEquals("not_ported", response.status)
        assertFalse(response.ported ?: true)
        assertEquals("NET-CONNECT COMMUNICATIONS", response.operators.current)
        assertNull(response.operators.initial)
        assertEquals("2026-03-25T15:22:33", response.timestamps.currentIso)
        assertEquals("2026-03-25", response.timestamps.infoValidOnIso)
        assertEquals("Geo", response.numberType)
    }

    companion object {
        private const val PORTED_HTML = """
            <html>
              <body>
                <span class="ContentTitle">Numarul 0799991518 este portat</span>
                <div id="ctl00_cphBody_panelDetails">
                  <table class="warning-message">
                    <tr>
                      <td><span id="ctl00_cphBody_lblCurrentOperator">Operator curent:</span></td>
                      <td><a id="ctl00_cphBody_lnkOperator">ORANGE ROMANIA</a></td>
                    </tr>
                    <tr id="ctl00_cphBody_rowOperatorInitial">
                      <td><span id="ctl00_cphBody_lblInitialOperator">Operator iniţial:</span></td>
                      <td><a id="ctl00_cphBody_lnkOperatorInitial">VODAFONE ROMANIA</a></td>
                    </tr>
                    <tr>
                      <td><span id="ctl00_cphBody_lblCurrentDate">Data curentă:</span></td>
                      <td><span id="ctl00_cphBody_lbDataCurenta">25.04.2026 13:32:29</span></td>
                    </tr>
                    <tr>
                      <td><span id="ctl00_cphBody_lblNumberType">Tip număr:</span></td>
                      <td><span id="ctl00_cphBody_lbNumberType">Mobil</span></td>
                    </tr>
                  </table>
                  <span id="ctl00_cphBody_lbLastUpdate">Informatii valabile la data de 25.04.2026</span>
                </div>
              </body>
            </html>
        """

        private const val NOT_PORTED_HTML = """
            <html>
              <body>
                <span class="ContentTitle">Numarul 0318118016 nu este portat</span>
                <div id="ctl00_cphBody_panelDetails">
                  <table class="warning-message">
                    <tr>
                      <td>Operator curent:</td>
                      <td>NET-CONNECT COMMUNICATIONS</td>
                    </tr>
                    <tr>
                      <td>Data curentă:</td>
                      <td>25.03.2026 15:22:33</td>
                    </tr>
                    <tr>
                      <td>Tip număr:</td>
                      <td>Geo</td>
                    </tr>
                  </table>
                  <span id="ctl00_cphBody_lbLastUpdate">Informatii valabile la data de 25.03.2026</span>
                </div>
              </body>
            </html>
        """
    }
}

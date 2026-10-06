package com.paytimeshift.pts

import com.paytimeshift.pts.domain.*
import com.paytimeshift.pts.data.LocalStore
import com.paytimeshift.pts.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class LocalizationTest {
    @Test fun freshInstallEnglishAndLegacyLanguagePreserved() {
        assertEquals("en",AppData().preferences.language)
        val root=JSONObject(LocalStore.encode(AppData()));root.getJSONObject("preferences").remove("language")
        assertEquals("mk",LocalStore.decode(root.toString()).preferences.language)
    }
    @Test fun everySupportedLanguageHasAllReportAndAccountMessages() {
        assertEquals(9,languageNames.size)
        val keys=translationCatalog("mk").keys
        languageNames.keys.filter {it!="en"}.forEach {language->
            val catalog=translationCatalog(language);assertEquals(keys,catalog.keys)
            listOf("Job costs","Real estimated earnings","Monthly Report Email","Taxes, government deductions and payroll deductions are not included.","Delete account").forEach {assertFalse(translate(it,language).isBlank())}
        }
    }
    @Test fun arbitraryJobNamesAreNotReplacedByDictionaryWords() {
        assertEquals("My Fuel Job",translate("My Fuel Job","de"))
        assertEquals("8 Std. geplant diesen Monat",translate("8h scheduled this month","de"))
    }
}

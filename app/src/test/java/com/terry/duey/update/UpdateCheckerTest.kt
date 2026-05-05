package com.terry.duey.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UpdateCheckerTest {
    private val checker = UpdateChecker()

    @Test
    fun parseLatestStageRelease_selectsHighestStagePreReleaseWithApk() {
        val updateInfo = checker.parseLatestStageRelease(
            """
            [
              {
                "tag_name": "v0.0.2-stage",
                "prerelease": true,
                "body": "older",
                "assets": [
                  {
                    "name": "duey-stage-0.0.2.apk",
                    "browser_download_url": "https://github.com/YubinMoon/Duey/releases/download/v0.0.2-stage/duey-stage-0.0.2.apk"
                  }
                ]
              },
              {
                "tag_name": "v0.1.0-stage",
                "prerelease": true,
                "body": "newer",
                "assets": [
                  {
                    "name": "duey-stage-0.1.0.apk",
                    "browser_download_url": "https://github.com/YubinMoon/Duey/releases/download/v0.1.0-stage/duey-stage-0.1.0.apk"
                  }
                ]
              },
              {
                "tag_name": "v9.9.9",
                "prerelease": false,
                "assets": [
                  {
                    "name": "duey-prod-9.9.9.apk",
                    "browser_download_url": "https://github.com/YubinMoon/Duey/releases/download/v9.9.9/duey-prod-9.9.9.apk"
                  }
                ]
              }
            ]
            """.trimIndent(),
        )

        assertEquals(1000, updateInfo.versionCode)
        assertEquals("0.1.0", updateInfo.versionName)
        assertEquals("newer", updateInfo.releaseNotes)
    }

    @Test
    fun parseLatestStageRelease_ignoresStageReleaseWithoutHttpsApk() {
        assertThrows(IllegalArgumentException::class.java) {
            checker.parseLatestStageRelease(
                """
                [
                  {
                    "tag_name": "v0.1.0-stage",
                    "prerelease": true,
                    "assets": [
                      {
                        "name": "duey-stage-0.1.0.apk",
                        "browser_download_url": "http://example.com/duey-stage-0.1.0.apk"
                      }
                    ]
                  }
                ]
                """.trimIndent(),
            )
        }
    }
}

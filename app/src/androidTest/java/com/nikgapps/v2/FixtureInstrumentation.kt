package com.nikgapps.v2

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Temporary component view only. No storage writes, credentials, network or arbitrary class/command loading. */
class FixtureInstrumentation : Instrumentation() {
    private var arguments = Bundle()
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        this.arguments = arguments ?: Bundle()
        start()
    }
    override fun onStart() {
        check(targetContext.packageName == "com.nikgapps.v2")
        val scenario = arguments.getString("scenario", "outdated")
        check(scenario in setOf("outdated", "outdated-long"))
        val theme = arguments.getString("theme", "dark")
        check(theme in setOf("dark", "light", "classic"))
        val activity = startActivitySync(Intent(targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val projects = (1..if (scenario == "outdated-long") 18 else 5).map { index -> BuildProject(id = "fixture-$index", name = "Fixture project $index",
            androidVersion = AndroidVersion.ANDROID_17, createdAt = 1795737600000L) }
        val unavailable = projects.drop(2).associate { it.id to setOf("retired-fixture-package") }
        runOnMainSync {
            activity.setContent {
                CompositionLocalProvider(LocalGlassEnabled provides (theme != "classic"),
                    LocalGlassBackground provides GlassBackground.GOOGLE, LocalRadialProjectMenu provides false) {
                    AppTheme(activity, if (theme == "light") 0 else 1, dynamicColor = false, ready = true) {
                        ProjectsBackdrop {
                            Scaffold(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground,
                                topBar = { Text("Projects", Modifier.statusBarsPadding().padding(16.dp), style = MaterialTheme.typography.headlineSmall) },
                                bottomBar = { Text("Component test", Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                                    style = MaterialTheme.typography.labelMedium) }) { padding ->
                                Box(Modifier.fillMaxSize().padding(padding)) {
                                    LocalProjects(projects, onEdit = {}, onCopy = {}, onDelete = {}, unavailable = unavailable)
                                }
                            }
                        }
                    }
                }
            }
        }
        sendStatus(1, Bundle().apply { putString("fixture", "ready") })
        CountDownLatch(1).await(15, TimeUnit.MINUTES)
        finish(Activity.RESULT_OK, Bundle().apply { putString("fixture", "finished") })
    }
}

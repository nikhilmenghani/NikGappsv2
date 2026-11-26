package com.nikgapps.v2

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectCreationSheet(repository: BuildProjectRepository, targets: List<AndroidVersion>,
    onSaved: (List<BuildProject>) -> Unit, onDismiss: () -> Unit, project: BuildProject? = null) {
    var name by rememberSaveable(project?.id) { mutableStateOf(project?.name.orEmpty()) }
    var targetId by rememberSaveable(project?.id) { mutableStateOf(project?.androidVersion?.name ?: targets.first().name) }
    val target = targets.firstOrNull { it.name == targetId } ?: targets.first()
    var saving by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    fun dismiss() {
        if (saving || closing) return
        closing = true; visible = false; keyboard?.hide()
        scope.launch { delay(240); onDismiss() }
    }
    fun save() {
        if (saving || closing || name.isBlank()) return
        saving = true; keyboard?.hide()
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    if (project == null) repository.createProject(name, target, targets.toSet())
                    else repository.updateProject(project.id, name, target, targets.toSet())
                }
                onSaved(result)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (_: Exception) { error = true; saving = false }
        }
    }
    LaunchedEffect(Unit) {
        visible = true
        delay(120)
        if (!closing && project == null) { focus.requestFocus(); keyboard?.show() }
    }
    Dialog(onDismissRequest = ::dismiss, properties = DialogProperties(usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .42f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = ::dismiss)) {
            AnimatedVisibility(visible, enter = slideInVertically { -it / 2 } + fadeIn(),
                exit = slideOutVertically { -it / 2 } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().navigationBarsPadding().imePadding()) {
                GlassSurface(Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = {}), shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp, shadowElevation = 8.dp) {
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (project == null) "Create project" else "Edit project", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                            IconButton(onClick = { info = true }) { Icon(AppIcons.Info, "About projects") }
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text("ARM64 only", Modifier.padding(8.dp, 4.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        OutlinedTextField(value = name, onValueChange = { name = it.take(REGULAR_NAME_LIMIT) },
                            label = { Text("Project name") }, singleLine = true, enabled = !saving && !closing,
                            shape = RoundedCornerShape(16.dp), supportingText = { Text("${name.length}/$REGULAR_NAME_LIMIT") },
                            modifier = Modifier.fillMaxWidth().focusRequester(focus),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { save() }))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Android version", style = MaterialTheme.typography.labelLarge)
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                targets.forEachIndexed { index, option ->
                                    SegmentedButton(selected = target == option, onClick = { targetId = option.name },
                                        enabled = !saving && !closing,
                                        shape = SegmentedButtonDefaults.itemShape(index, targets.size, RoundedCornerShape(16.dp))) {
                                        Text(option.displayName.removePrefix("Android "))
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassTonalButton(onClick = ::dismiss, enabled = !saving && !closing, modifier = Modifier.weight(1f)) { Text("Cancel") }
                            GlassButton(onClick = ::save, enabled = !saving && !closing && name.isNotBlank(), modifier = Modifier.weight(1f)) {
                                Text(if (saving) "Saving…" else if (project == null) "Create project" else "Save changes")
                            }
                        }
                    }
                }
            }
        }
        if (info) AppDialog("Projects", { info = false }) {
            Text("Choose a name and the Android version for your ZIP. Projects use ARM64. You can keep up to 5 projects, with names up to 20 characters.",
                style = MaterialTheme.typography.bodyMedium)
        }
        if (error) AppDialog("Couldn't save project", { error = false }) {
            Text("Please try again.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

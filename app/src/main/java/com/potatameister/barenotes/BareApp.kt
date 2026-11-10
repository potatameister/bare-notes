package com.potatameister.barenotes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun BareApp(viewModel: NotesViewModel) {
    val dark = when (viewModel.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    BareTheme(dark = dark) {
        val colors = LocalBareColors.current
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        var routeKey by rememberSaveable { mutableStateOf("notes") }
        val route = routeKey.substringAfterLast('|')
        val canPop = routeKey.contains('|')

        val messenger: (String) -> Unit = { message ->
            scope.launch { snackbar.showSnackbar(message) }
        }

        fun push(next: String) {
            routeKey = if (routeKey.isEmpty()) next else "$routeKey|$next"
        }

        fun pop() {
            if (canPop) routeKey = routeKey.substringBeforeLast('|')
        }

        BackHandler(enabled = drawerState.isOpen || canPop) {
            if (drawerState.isOpen) {
                scope.launch { drawerState.close() }
            } else {
                pop()
            }
        }

        CompositionLocalProvider(LocalMessenger provides messenger) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = route == "notes",
                scrimColor = Color.Black.copy(alpha = 0.32f),
                drawerContent = {
                    BareDrawer(
                        viewModel = viewModel,
                        dark = dark,
                        drawerState = drawerState,
                        onOpenFolders = { push("folders") },
                        onOpenSettings = { push("settings") },
                        onOpenAbout = { push("about") }
                    )
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.bg)
                ) {
                    when {
                        route == "notes" -> NotesScreen(
                            viewModel = viewModel,
                            onMenu = { scope.launch { drawerState.open() } },
                            onAdd = {
                                viewModel.prepareNewNote()
                                push("editor:new")
                            },
                            onOpenNote = { note -> push("editor:${note.id}") },
                            onOpenFolders = { push("folders") }
                        )
                        route == "folders" -> FoldersScreen(
                            viewModel = viewModel,
                            onBack = { pop() }
                        )
                        route == "settings" -> SettingsScreen(
                            viewModel = viewModel,
                            onBack = { pop() },
                            onFolders = { push("folders") }
                        )
                        route == "about" -> AboutScreen(
                            onBack = { pop() }
                        )
                        route.startsWith("editor:") -> EditorScreen(
                            viewModel = viewModel,
                            noteId = route.removePrefix("editor:").takeUnless { it == "new" },
                            onClose = { pop() },
                            onOpenFolders = { push("folders") }
                        )
                    }
                    SnackbarHost(
                        hostState = snackbar,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                    )
                }
            }
        }
    }
}

@Composable
private fun BareDrawer(
    viewModel: NotesViewModel,
    dark: Boolean,
    drawerState: androidx.compose.material3.DrawerState,
    onOpenFolders: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val colors = LocalBareColors.current
    val scope = rememberCoroutineScope()

    fun close() {
        scope.launch { drawerState.close() }
    }

    ModalDrawerSheet(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.systemBars),
        drawerShape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
        drawerContainerColor = colors.surface,
        drawerTonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(BareSidebarPadding)
        ) {
            Text(
                text = "BARE",
                style = BareType.Wordmark,
                color = colors.text
            )
            Text(
                text = "NOTES",
                style = BareType.Eyebrow,
                color = colors.textMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
            BareDivider(
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                BareNavRow(
                    label = "All notes",
                    selected = viewModel.filter == NoteFilter.All,
                    onClick = {
                        viewModel.setFilter(NoteFilter.All)
                        close()
                    }
                )
                BareNavRow(
                    label = "Unfiled",
                    selected = viewModel.filter == NoteFilter.Unfiled,
                    onClick = {
                        viewModel.setFilter(NoteFilter.Unfiled)
                        close()
                    }
                )

                Text(
                    text = "FOLDERS",
                    style = BareType.Eyebrow,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                )
                viewModel.folders.forEach { folder ->
                    BareNavRow(
                        label = folder.name,
                        selected = viewModel.filter == NoteFilter.InFolder(folder.id),
                        onClick = {
                            viewModel.setFilter(NoteFilter.InFolder(folder.id))
                            close()
                        }
                    )
                }
                BareTextRow(
                    label = "Manage folders",
                    onClick = {
                        close()
                        onOpenFolders()
                    }
                )

                BareDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dark mode",
                        style = BareType.Body,
                        color = colors.text,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = dark,
                        onCheckedChange = {
                            viewModel.setThemeMode(if (it) ThemeMode.DARK else ThemeMode.LIGHT)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.text,
                            checkedTrackColor = colors.divider,
                            checkedBorderColor = colors.divider,
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.divider,
                            uncheckedBorderColor = colors.divider
                        )
                    )
                }

                BareNavRow(
                    label = "Settings",
                    selected = false,
                    onClick = {
                        close()
                        onOpenSettings()
                    }
                )
                BareNavRow(
                    label = "About",
                    selected = false,
                    onClick = {
                        close()
                        onOpenAbout()
                    }
                )
            }

            Text(
                text = "v${BuildConfig.VERSION_NAME} · offline",
                style = BareType.Meta,
                color = colors.textMuted,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

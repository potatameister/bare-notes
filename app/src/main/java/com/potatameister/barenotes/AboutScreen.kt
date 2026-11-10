package com.potatameister.barenotes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = LocalBareColors.current
    val uriHandler = LocalUriHandler.current
    val messenger = LocalMessenger.current

    fun open(url: String) {
        runCatching { uriHandler.openUri(url) }
            .onFailure { messenger("No browser available.") }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        BareTopBar(
            navigation = {
                BareIconButton(
                    iconRes = R.drawable.ic_back,
                    contentDescription = "Back",
                    onClick = onBack
                )
            },
            title = {
                Text("About", style = BareType.ScreenTitle, color = colors.text)
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.bare),
                style = BareType.Wordmark,
                color = colors.text
            )
            Text(
                text = stringResource(R.string.notes),
                style = BareType.Eyebrow,
                color = colors.textMuted,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = stringResource(R.string.about_intro),
                style = BareType.ScreenTitle,
                color = colors.text,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = stringResource(R.string.about_body),
                style = BareType.Body,
                color = colors.textMuted,
                modifier = Modifier.padding(top = 8.dp)
            )

            BareDivider(modifier = Modifier.padding(top = 24.dp, bottom = 4.dp))

            AboutRow(
                label = stringResource(R.string.version),
                value = BuildConfig.VERSION_NAME
            )
            BareDivider()

            AboutRow(
                label = stringResource(R.string.source_code),
                value = stringResource(R.string.source_url),
                onClick = { open("https://github.com/potatameister/bare-notes") }
            )
            BareDivider()

            AboutRow(
                label = stringResource(R.string.support),
                value = stringResource(R.string.support_url),
                onClick = { open("https://potatameister.github.io/support") }
            )
            BareDivider()

            AboutRow(
                label = stringResource(R.string.license),
                value = stringResource(R.string.license_value)
            )
        }
    }
}

@Composable
private fun AboutRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalBareColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = BareType.Body,
            color = colors.text
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = BareType.Meta,
            color = colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

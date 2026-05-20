package com.example.securecall.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.securecall.R

@Composable
fun FilterChips(
    selectedFilter: ChatFilter,
    onFilterSelected: (ChatFilter) -> Unit
) {

    ChatFilter.entries.forEachIndexed { index, filter ->

        val label = when (filter) {
            ChatFilter.ALL -> stringResource(R.string.filter_all)
            ChatFilter.VERIFIED -> stringResource(R.string.filter_verified)
            ChatFilter.UNVERIFIED -> stringResource(R.string.filter_unverified)
        }

        val isSelected = selectedFilter == filter

        val bgColor by animateColorAsState(
            targetValue = if (isSelected)
                MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            animationSpec = tween(200),
            label = ""
        )

        val textColor by animateColorAsState(
            targetValue = if (isSelected)
                MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(200),
            label = ""
        )

        if (index > 0) Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(bgColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onFilterSelected(filter) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

enum class ChatFilter { ALL, VERIFIED, UNVERIFIED }

package com.example.securecall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.securecall.R
import com.example.securecall.domain.model.Message
import com.example.securecall.domain.model.MessageStatus

@Composable
fun MessageBubble(message: Message, currentUserId: String?) {
    val isMine = message.senderId == currentUserId

    val bubbleColor = if (isMine)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (isMine)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurface

    val timeColor = if (isMine)
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalArrangement = if (isMine) androidx.compose.foundation.layout.Arrangement.End
        else androidx.compose.foundation.layout.Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 308.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isMine) 18.dp else 5.dp,
                        bottomEnd = if (isMine) 5.dp else 18.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.content,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = timeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isMine) {
                    Text(
                        text = "  " + when (message.status) {
                            MessageStatus.SENT -> stringResource(R.string.message_status_sent)
                            MessageStatus.DELIVERED -> stringResource(R.string.message_status_delivered)
                            MessageStatus.READ -> stringResource(R.string.message_status_read)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = timeColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

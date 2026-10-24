package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CloudSyncStatus
import com.example.ui.theme.AmberSecurity
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TealPrimary

@Composable
fun CloudSyncBadge(
    status: CloudSyncStatus,
    isOfflineMode: Boolean,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (bgColor, contentColor, text, icon) = when {
        isOfflineMode -> Quadruple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Offline Mode",
            Icons.Default.CloudOff
        )
        status == CloudSyncStatus.SYNCED -> Quadruple(
            Color(0xFFE6F4EA),
            EmeraldSuccess,
            "Cloud Synced",
            Icons.Default.CheckCircle
        )
        status == CloudSyncStatus.SYNCING -> Quadruple(
            Color(0xFFE0F2FE),
            Color(0xFF0284C7),
            "Syncing...",
            Icons.Default.Sync
        )
        status == CloudSyncStatus.PENDING -> Quadruple(
            Color(0xFFFEF3C7),
            AmberSecurity,
            "Backup Pending",
            Icons.Default.Cloud
        )
        else -> Quadruple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Offline Saved",
            Icons.Default.CloudOff
        )
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        modifier = modifier
            .testTag("cloud_sync_badge")
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (status == CloudSyncStatus.SYNCING && !isOfflineMode) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = "Sync status",
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun FolderTagPill(
    folderName: String,
    colorHex: String,
    isVault: Boolean = false,
    modifier: Modifier = Modifier
) {
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        TealPrimary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(parsedColor.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(parsedColor)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = folderName,
            color = parsedColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        if (isVault) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Encrypted Vault Folder",
                tint = AmberSecurity,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

@Composable
fun E2eeSecurityBadge(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AmberSecurity.copy(alpha = 0.14f))
            .border(0.5.dp, AmberSecurity.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "E2E Encryption",
            tint = AmberSecurity,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "AES-256 E2EE",
            color = AmberSecurity,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

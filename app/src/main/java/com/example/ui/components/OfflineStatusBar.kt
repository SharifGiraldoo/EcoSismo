package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.BrightCyan
import com.example.ui.theme.LiveGreenGlow

@Composable
fun OfflineStatusBar(
    isOnline: Boolean,
    isSyncing: Boolean,
    pendingCount: Int,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isOnline && !isSyncing && pendingCount == 0) {
        return
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isOnline) Color(0x8005111E) else Color(0x33F77F00),
        border = BorderStroke(0.5.dp, if (isOnline) Color(0x1F00F5D4) else Color(0x40F77F00))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (!isOnline) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Modo Offline",
                        tint = AlertAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Modo Offline • Memoria local Room protegida",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = AlertAmber
                    )
                }

                if (pendingCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AlertAmber.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "$pendingCount pendientes",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = AlertAmber,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            if (isSyncing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = BrightCyan
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sincronizando...",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = BrightCyan
                    )
                }
            } else if (pendingCount > 0 && isOnline) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x3300F5D4),
                    border = BorderStroke(0.6.dp, BrightCyan.copy(alpha = 0.5f))
                ) {
                    TextButton(
                        onClick = onSyncClick,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sincronizar",
                            tint = BrightCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Sincronizar",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = BrightCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

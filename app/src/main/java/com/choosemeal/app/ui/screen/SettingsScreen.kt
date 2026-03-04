package com.choosemeal.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.choosemeal.app.data.preferences.UserSettings

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settings: UserSettings,
    onCooldownEnabledChange: (Boolean) -> Unit,
    onAnimationsEnabledChange: (Boolean) -> Unit,
    onHapticsEnabledChange: (Boolean) -> Unit,
    onWindowSizeChange: (Int) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingSwitchRow(
                    title = "启用冷却去重",
                    subtitle = "减少连续抽中同一食堂/菜品",
                    checked = settings.cooldownEnabled,
                    onCheckedChange = onCooldownEnabledChange,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("冷却窗口")
                        Text("最近 ${settings.recentWindowSize} 次结果参与降权", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onWindowSizeChange(settings.recentWindowSize - 1) }) { Text("-1") }
                        Text(settings.recentWindowSize.toString(), fontWeight = FontWeight.Bold)
                        TextButton(onClick = { onWindowSizeChange(settings.recentWindowSize + 1) }) { Text("+1") }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingSwitchRow(
                    title = "动画效果",
                    subtitle = "转盘减速与抽签翻牌",
                    checked = settings.animationsEnabled,
                    onCheckedChange = onAnimationsEnabledChange,
                )
                SettingSwitchRow(
                    title = "触感反馈",
                    subtitle = "点击决策按钮时震动反馈",
                    checked = settings.hapticsEnabled,
                    onCheckedChange = onHapticsEnabledChange,
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("随机策略", fontWeight = FontWeight.SemiBold)
                Text("冷却去重采用加权随机：命中最近窗口的候选会按 0.25 权重参与抽取，避免无聊连击。")
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

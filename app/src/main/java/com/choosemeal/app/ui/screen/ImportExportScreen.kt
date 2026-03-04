package com.choosemeal.app.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.choosemeal.app.data.importexport.CommunityConfigEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImportExportScreen(
    modifier: Modifier = Modifier,
    onImport: (Uri) -> Unit,
    onExport: (Uri) -> Unit,
    onShareCurrentConfig: () -> Unit,
    communityConfigs: List<CommunityConfigEntry>,
    communityUpdatedAt: String,
    isCommunityLoading: Boolean,
    communityImportingId: String?,
    communityIssueUrl: String,
    communityRepoUrl: String,
    onLoadCommunityConfigs: (Boolean) -> Unit,
    onImportCommunityConfig: (CommunityConfigEntry) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        onLoadCommunityConfigs(false)
    }

    val openLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> if (uri != null) onImport(uri) },
    )

    val createLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri -> if (uri != null) onExport(uri) },
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("导入导出", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("社区配置下载", style = MaterialTheme.typography.titleMedium)
                Text("从社区下载其他学校同学分享的配置，并直接导入到本地。")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onLoadCommunityConfigs(true) },
                        enabled = !isCommunityLoading,
                    ) {
                        Text(if (isCommunityLoading) "刷新中..." else "刷新列表")
                    }
                    OutlinedButton(onClick = { uriHandler.openUri(communityRepoUrl) }) {
                        Text("社区仓库")
                    }
                }

                if (communityUpdatedAt.isNotBlank()) {
                    Text("索引更新时间：$communityUpdatedAt", style = MaterialTheme.typography.bodySmall)
                }

                when {
                    isCommunityLoading && communityConfigs.isEmpty() -> {
                        CircularProgressIndicator()
                    }

                    communityConfigs.isEmpty() -> {
                        Text("当前没有可用社区配置。")
                    }

                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            communityConfigs.forEach { item ->
                                val importing = communityImportingId == item.id
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(item.schoolName, fontWeight = FontWeight.SemiBold)
                                        Text("${item.city} · 更新 ${item.updatedAt}")
                                        if (item.tags.isNotEmpty()) {
                                            Text(item.tags.joinToString(" / "), style = MaterialTheme.typography.bodySmall)
                                        }
                                        Button(
                                            onClick = { onImportCommunityConfig(item) },
                                            enabled = !importing,
                                        ) {
                                            Text(if (importing) "导入中..." else "下载并导入")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("上传与分享", style = MaterialTheme.typography.titleMedium)
                Text("一键生成当前配置并调用系统分享，可直接发给同学。")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onShareCurrentConfig) {
                        Text("一键分享配置")
                    }
                    OutlinedButton(onClick = { uriHandler.openUri(communityIssueUrl) }) {
                        Text("提交到社区")
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("JSON 导出", style = MaterialTheme.typography.titleMedium)
                Text("备份当前所有食堂/楼层/伙食数据，可用于迁移或恢复。")
                Button(onClick = {
                    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    createLauncher.launch("choosemeal_backup_${stamp}.json")
                }) {
                    Text("导出为 JSON")
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("JSON 导入", style = MaterialTheme.typography.titleMedium)
                Text("导入会覆盖当前本地数据，导入前请先导出备份。")
                Button(onClick = {
                    openLauncher.launch(arrayOf("application/json", "text/plain"))
                }) {
                    Text("从 JSON 导入")
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("合约版本", fontWeight = FontWeight.SemiBold)
                Text("当前使用 JSON contract v1：version + cafeterias[] + floors[] + meals[]")
            }
        }
    }
}

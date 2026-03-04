package com.choosemeal.app.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImportExportScreen(
    modifier: Modifier = Modifier,
    onImport: (Uri) -> Unit,
    onExport: (Uri) -> Unit,
) {
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

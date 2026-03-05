package com.choosemeal.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.choosemeal.app.data.local.entity.CafeteriaEntity
import com.choosemeal.app.data.local.entity.FloorEntity
import com.choosemeal.app.data.local.entity.MealEntity

@Composable
fun DataManagementScreen(
    modifier: Modifier = Modifier,
    cafeterias: List<CafeteriaEntity>,
    allFloors: List<FloorEntity>,
    selectedCafeteriaId: Long?,
    selectedFloorId: Long?,
    floors: List<FloorEntity>,
    meals: List<MealEntity>,
    flavorOptions: List<String>,
    onSelectCafeteria: (Long?) -> Unit,
    onSelectFloor: (Long?) -> Unit,
    onUpsertCafeteria: (CafeteriaEntity) -> Unit,
    onUpsertFloor: (FloorEntity) -> Unit,
    onUpsertMeal: (MealEntity) -> Unit,
    onDeleteCafeteria: (Long) -> Unit,
    onDeleteFloor: (Long) -> Unit,
    onDeleteMeal: (Long) -> Unit,
) {
    var cafeteriaEdit by remember { mutableStateOf<CafeteriaEntity?>(null) }
    var floorEdit by remember { mutableStateOf<FloorEntity?>(null) }
    var mealEdit by remember { mutableStateOf<MealEntity?>(null) }

    if (cafeteriaEdit != null) {
        NameEditDialog(
            title = if (cafeteriaEdit?.id == 0L) "新增食堂" else "编辑食堂",
            initial = cafeteriaEdit?.name.orEmpty(),
            onDismiss = { cafeteriaEdit = null },
            onConfirm = { newName ->
                val base = cafeteriaEdit ?: return@NameEditDialog
                onUpsertCafeteria(base.copy(name = newName))
                cafeteriaEdit = null
            },
        )
    }

    if (floorEdit != null) {
        NameEditDialog(
            title = if (floorEdit?.id == 0L) "新增楼层" else "编辑楼层",
            initial = floorEdit?.name.orEmpty(),
            onDismiss = { floorEdit = null },
            onConfirm = { newName ->
                val base = floorEdit ?: return@NameEditDialog
                onUpsertFloor(base.copy(name = newName))
                floorEdit = null
            },
        )
    }

    if (mealEdit != null) {
        MealEditDialog(
            title = if (mealEdit?.id == 0L) "新增伙食" else "编辑伙食",
            initialName = mealEdit?.name.orEmpty(),
            initialTags = mealEdit?.tags.orEmpty(),
            initialFlavor = mealEdit?.flavor.orEmpty(),
            initialPriceYuan = mealEdit?.priceYuan,
            flavorOptions = flavorOptions,
            onDismiss = { mealEdit = null },
            onConfirm = { newName, tags, flavor, priceYuan ->
                val base = mealEdit ?: return@MealEditDialog
                onUpsertMeal(
                    base.copy(
                        name = newName,
                        tags = tags,
                        flavor = flavor,
                        priceYuan = priceYuan,
                    ),
                )
                mealEdit = null
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("数据管理", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        DataSectionCard(
            title = "食堂",
            onAdd = {
                cafeteriaEdit = CafeteriaEntity(
                    id = 0,
                    name = "",
                    sortOrder = (cafeterias.maxOfOrNull { it.sortOrder } ?: 0) + 1,
                    enabled = true,
                )
            },
        ) {
            cafeterias.forEach { cafeteria ->
                EditableRow(
                    title = cafeteria.name,
                    subtitle = "排序 ${cafeteria.sortOrder}",
                    enabled = cafeteria.enabled,
                    onToggle = { onUpsertCafeteria(cafeteria.copy(enabled = it)) },
                    onEdit = { cafeteriaEdit = cafeteria },
                    onDelete = {
                        if (selectedCafeteriaId == cafeteria.id) onSelectCafeteria(null)
                        onDeleteCafeteria(cafeteria.id)
                    },
                )
            }
        }

        DataSectionCard(title = "楼层", onAdd = {
            val cafeteriaId = selectedCafeteriaId ?: return@DataSectionCard
            floorEdit = FloorEntity(
                id = 0,
                cafeteriaId = cafeteriaId,
                name = "",
                sortOrder = (floors.maxOfOrNull { it.sortOrder } ?: 0) + 1,
                enabled = true,
            )
        }) {
            SelectionDropDown(
                title = "当前食堂",
                selectedText = cafeterias.firstOrNull { it.id == selectedCafeteriaId }?.name ?: "请选择食堂",
                entries = cafeterias.map { it.id to it.name },
                onSelect = onSelectCafeteria,
            )

            floors.forEach { floor ->
                EditableRow(
                    title = floor.name,
                    subtitle = "排序 ${floor.sortOrder}",
                    enabled = floor.enabled,
                    onToggle = { onUpsertFloor(floor.copy(enabled = it)) },
                    onEdit = { floorEdit = floor },
                    onDelete = {
                        if (selectedFloorId == floor.id) onSelectFloor(null)
                        onDeleteFloor(floor.id)
                    },
                )
            }
        }

        DataSectionCard(title = "伙食", onAdd = {
            val floorId = selectedFloorId ?: return@DataSectionCard
            mealEdit = MealEntity(
                id = 0,
                floorId = floorId,
                name = "",
                tags = "",
                flavor = "",
                priceYuan = null,
                enabled = true,
            )
        }) {
            SelectionDropDown(
                title = "当前楼层",
                selectedText = allFloors.firstOrNull { it.id == selectedFloorId }?.name ?: "请选择楼层",
                entries = floors.map { it.id to it.name },
                onSelect = onSelectFloor,
            )

            meals.forEach { meal ->
                EditableRow(
                    title = meal.name,
                    subtitle = buildMealSubtitle(meal),
                    enabled = meal.enabled,
                    onToggle = { onUpsertMeal(meal.copy(enabled = it)) },
                    onEdit = { mealEdit = meal },
                    onDelete = { onDeleteMeal(meal.id) },
                )
            }
        }
    }
}

@Composable
private fun DataSectionCard(
    title: String,
    onAdd: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onAdd) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("新增")
                }
            }
            content()
        }
    }
}

@Composable
private fun EditableRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
        IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = null) }
        IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = null) }
    }
}

@Composable
private fun SelectionDropDown(
    title: String,
    selectedText: String,
    entries: List<Pair<Long, String>>,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedText)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            entries.forEach { (id, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onSelect(id)
                    },
                )
            }
        }
    }
}

@Composable
private fun NameEditDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("名称") })
        },
        confirmButton = {
            TextButton(onClick = { if (value.isNotBlank()) onConfirm(value.trim()) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealEditDialog(
    title: String,
    initialName: String,
    initialTags: String,
    initialFlavor: String,
    initialPriceYuan: Int?,
    flavorOptions: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int?) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var tags by remember(initialTags) { mutableStateOf(initialTags) }
    val normalizedFlavors = remember(flavorOptions) {
        flavorOptions.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    var selectedFlavor by remember(initialFlavor, normalizedFlavors) {
        mutableStateOf(initialFlavor.takeIf { it in normalizedFlavors }.orEmpty())
    }
    var useCustomFlavor by remember(initialFlavor, normalizedFlavors) {
        mutableStateOf(initialFlavor.isNotBlank() && initialFlavor !in normalizedFlavors)
    }
    var customFlavor by remember(initialFlavor) {
        mutableStateOf(if (initialFlavor.isNotBlank() && initialFlavor !in normalizedFlavors) initialFlavor else "")
    }
    var flavorMenuExpanded by remember { mutableStateOf(false) }
    var priceText by remember(initialPriceYuan) { mutableStateOf(initialPriceYuan?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("伙食名") })
                Text("口味", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
                ExposedDropdownMenuBox(
                    expanded = flavorMenuExpanded,
                    onExpandedChange = { flavorMenuExpanded = !flavorMenuExpanded },
                ) {
                    val flavorText = when {
                        useCustomFlavor -> "自定义：${customFlavor.ifBlank { "未填写" }}"
                        selectedFlavor.isNotBlank() -> selectedFlavor
                        else -> "未设置"
                    }
                    TextField(
                        value = flavorText,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = flavorMenuExpanded) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    )
                    ExposedDropdownMenu(
                        expanded = flavorMenuExpanded,
                        onDismissRequest = { flavorMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("未设置") },
                            onClick = {
                                flavorMenuExpanded = false
                                useCustomFlavor = false
                                selectedFlavor = ""
                            },
                        )
                        normalizedFlavors.forEach { flavor ->
                            DropdownMenuItem(
                                text = { Text(flavor) },
                                onClick = {
                                    flavorMenuExpanded = false
                                    useCustomFlavor = false
                                    selectedFlavor = flavor
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("自定义口味") },
                            onClick = {
                                flavorMenuExpanded = false
                                useCustomFlavor = true
                            },
                        )
                    }
                }
                if (useCustomFlavor) {
                    OutlinedTextField(
                        value = customFlavor,
                        onValueChange = { customFlavor = it },
                        label = { Text("自定义口味") },
                    )
                }
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter(Char::isDigit).take(4) },
                    label = { Text("价格") },
                    prefix = { Text("￥") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("标签") })
                Text(
                    text = "标签用于补充说明（如面食、清真）。价格和口味将用于筛选。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    val flavor = if (useCustomFlavor) customFlavor.trim() else selectedFlavor.trim()
                    onConfirm(name.trim(), tags.trim(), flavor, priceText.toIntOrNull())
                }
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun buildMealSubtitle(meal: MealEntity): String {
    val chunks = buildList {
        if (meal.flavor.isNotBlank()) add("口味 ${meal.flavor}")
        if (meal.priceYuan != null) add("￥${meal.priceYuan}")
        if (meal.tags.isNotBlank()) add(meal.tags)
    }
    return chunks.joinToString(" · ").ifBlank { "无附加信息" }
}

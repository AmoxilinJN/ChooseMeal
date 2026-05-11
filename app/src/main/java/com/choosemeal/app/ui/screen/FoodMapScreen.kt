package com.choosemeal.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.choosemeal.app.data.local.entity.MealEntity
import com.choosemeal.app.domain.model.CafeteriaWithFloors
import com.choosemeal.app.domain.model.FloorWithMeals
import com.choosemeal.app.domain.model.MealOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FoodMapScreen(
    modifier: Modifier = Modifier,
    hierarchy: List<CafeteriaWithFloors>,
    enabledOptions: List<MealOption>,
    onRecordMeal: (MealOption) -> Unit,
) {
    var recordedMealId by remember { mutableLongStateOf(-1L) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("美食地图", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "浏览所有伙食，点击「今天就吃这个」记录你的选择",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )

        if (hierarchy.isEmpty()) {
            Text(
                "暂无数据，请先在「数据」页面添加食堂和伙食。",
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 24.dp),
            )
            return@Column
        }

        val enabledMap = remember(enabledOptions) {
            enabledOptions.associateBy { it.mealId }
        }

        hierarchy.forEach { cafeteriaWithFloors ->
            CafeteriaSection(
                cafeteriaName = cafeteriaWithFloors.cafeteria.name,
                floors = cafeteriaWithFloors.floors,
                enabledMap = enabledMap,
                recordedMealId = recordedMealId,
                onRecordMeal = { option ->
                    recordedMealId = option.mealId
                    onRecordMeal(option)
                    // Reset feedback after 2 seconds
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(2000)
                        recordedMealId = -1L
                    }
                },
            )
        }
    }
}

@Composable
private fun CafeteriaSection(
    cafeteriaName: String,
    floors: List<FloorWithMeals>,
    enabledMap: Map<Long, MealOption>,
    recordedMealId: Long,
    onRecordMeal: (MealOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(cafeteriaName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    modifier = Modifier.padding(4.dp),
                )
            }

            if (expanded) {
                floors.forEach { floorWithMeals ->
                    FloorSection(
                        floorName = floorWithMeals.floor.name,
                        meals = floorWithMeals.meals.filter { it.id in enabledMap },
                        enabledMap = enabledMap,
                        recordedMealId = recordedMealId,
                        onRecordMeal = onRecordMeal,
                    )
                }
            }
        }
    }
}

@Composable
private fun FloorSection(
    floorName: String,
    meals: List<MealEntity>,
    enabledMap: Map<Long, MealOption>,
    recordedMealId: Long,
    onRecordMeal: (MealOption) -> Unit,
) {
    if (meals.isEmpty()) return

    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
        Text(
            floorName,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(vertical = 4.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            meals.forEach { meal ->
                val option = enabledMap[meal.id]
                if (option != null) {
                    MealCard(
                        mealName = meal.name,
                        flavor = meal.flavor,
                        priceYuan = meal.priceYuan,
                        tags = meal.tags,
                        isRecorded = meal.id == recordedMealId,
                        onRecord = { onRecordMeal(option) },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MealCard(
    mealName: String,
    flavor: String,
    priceYuan: Int?,
    tags: String,
    isRecorded: Boolean,
    onRecord: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(mealName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (flavor.isNotBlank()) {
                        Text(flavor, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    if (priceYuan != null) {
                        Text("￥$priceYuan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    if (tags.isNotBlank()) {
                        Text(tags, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            if (isRecorded) {
                OutlinedButton(
                    onClick = {},
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(start = 8.dp),
                    enabled = false,
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("已记录")
                }
            } else {
                Button(
                    onClick = onRecord,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("今天就吃这个")
                }
            }
        }
    }
}

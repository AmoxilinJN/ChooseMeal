package com.choosemeal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.choosemeal.app.ui.ChooseMealRoot
import com.choosemeal.app.ui.theme.ChooseMealTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChooseMealTheme {
                ChooseMealRoot()
            }
        }
    }
}

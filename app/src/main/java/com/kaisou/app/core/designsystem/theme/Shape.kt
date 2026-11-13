package com.kaisou.app.core.designsystem.theme

// Compose layouts and foundations
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Shapes
// UI styling and utilities
import androidx.compose.ui.unit.dp

val KaisouShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

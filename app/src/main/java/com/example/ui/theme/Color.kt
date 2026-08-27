package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =====================================================================================
// Светло-бежевая тема «бумага и терракота».
//
// Фон — тёплый песочный, карточки на полтона светлее, акцент — приглушённая терракота.
// Контраст текста к фону держим не ниже 4.5:1, у крупных заголовков — не ниже 3:1.
// =====================================================================================

val Sand50 = Color(0xFFFDFAF5) // карточки
val Sand100 = Color(0xFFF7F1E8) // фон экрана
val Sand200 = Color(0xFFEFE6D9) // вторичные поверхности
val Sand300 = Color(0xFFE3D6C3) // разделители, рамки
val Sand400 = Color(0xFFC9B79E) // контур
val Clay600 = Color(0xFF9A5B33) // основной акцент
val Clay700 = Color(0xFF7A4526) // акцент на светлом
val Clay100 = Color(0xFFF6E3D3) // контейнер акцента
val Clay900 = Color(0xFF3A2013) // текст на контейнере акцента
val Ink900 = Color(0xFF241C15) // основной текст
val Ink600 = Color(0xFF5F5245) // вторичный текст
val Moss600 = Color(0xFF3F6B4F) // «всё в порядке» / зафиксировано
val Rust700 = Color(0xFF9C2F1E) // ошибки и предупреждения

val PrimaryLight = Clay600
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Clay100
val OnPrimaryContainerLight = Clay900

val SecondaryLight = Sand200
val OnSecondaryLight = Ink900
val SecondaryContainerLight = Sand200
val OnSecondaryContainerLight = Ink900

val BackgroundLight = Sand100
val OnBackgroundLight = Ink900
val SurfaceLight = Sand50
val OnSurfaceLight = Ink900
val SurfaceVariantLight = Sand200
val OnSurfaceVariantLight = Ink600
val OutlineLight = Sand400
val OutlineVariantLight = Sand300
val ErrorLight = Rust700
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DED8)
val OnErrorContainerLight = Color(0xFF41100A)

// Тёмный вариант оставлен на случай, если тему снова захотят переключаемой.
val PrimaryDark = Color(0xFFE8A87C)
val OnPrimaryDark = Color(0xFF44210D)
val PrimaryContainerDark = Color(0xFF5E3218)
val OnPrimaryContainerDark = Color(0xFFFFDCC5)

val SecondaryDark = Color(0xFF4A4238)
val OnSecondaryDark = Color(0xFFEDE3D6)
val SecondaryContainerDark = Color(0xFF3A332B)
val OnSecondaryContainerDark = Color(0xFFEDE3D6)

val BackgroundDark = Color(0xFF1A1611)
val OnBackgroundDark = Color(0xFFEDE3D6)
val SurfaceDark = Color(0xFF241F19)
val OnSurfaceDark = Color(0xFFEDE3D6)
val SurfaceVariantDark = Color(0xFF3B342C)
val OnSurfaceVariantDark = Color(0xFFCFC2B2)
val OutlineDark = Color(0xFF5B5245)

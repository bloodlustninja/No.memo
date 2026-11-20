# ProGuard & R8 Configuration for No Memo v2.0.0

# ---------------------------------------------------------
# Room Database
# ---------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class androidx.room.** { *; }
-keep class com.example.nomemo.data.db.** { *; }
-keep class com.example.nomemo.data.entity.** { *; }
-keepclassmembers class * {
    @androidx.room.Dao *;
    @androidx.room.Database *;
    @androidx.room.Entity *;
    @androidx.room.TypeConverter *;
    @androidx.room.TypeConverters *;
}

# ---------------------------------------------------------
# Gson / Data Serialization
# ---------------------------------------------------------
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}
-keep class com.google.gson.** { *; }
-keep class com.example.nomemo.data.entity.** { *; }

# ---------------------------------------------------------
# Jetpack Glance Widgets
# ---------------------------------------------------------
-keep class androidx.glance.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class com.example.nomemo.widget.** { *; }

# ---------------------------------------------------------
# Android AppFunctions (Gemini Local Control)
# ---------------------------------------------------------
-keep class androidx.appfunctions.** { *; }
-keep class com.example.nomemo.appfunctions.** { *; }
-keep @androidx.appfunctions.compiler.** class * { *; }
-keep @androidx.appfunctions.service.** class * { *; }

# ---------------------------------------------------------
# WorkManager (Background Burn Note Engine)
# ---------------------------------------------------------
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class com.example.nomemo.worker.** { *; }

# ---------------------------------------------------------
# Guava / ListenableFuture (WorkManager & AppFunctions)
# ---------------------------------------------------------
-dontwarn com.google.common.util.concurrent.**

# ---------------------------------------------------------
# Nothing Phone Glyph Interface (Reflection & Ketchum SDK)
# ---------------------------------------------------------
-dontwarn com.nothing.ketchum.**
-keep class com.nothing.ketchum.** { *; }
-keep interface com.nothing.ketchum.** { *; }
-keep class com.example.nomemo.glyph.** { *; }
-keepclassmembers class com.example.nomemo.glyph.GlyphHardwareManager { *; }

# ---------------------------------------------------------
# Compose Runtime & Animations
# ---------------------------------------------------------
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.ReadOnlyComposable *;
}

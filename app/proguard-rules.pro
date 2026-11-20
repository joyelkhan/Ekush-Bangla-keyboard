# Ekush Bangla Keyboard — R8 / ProGuard Release Hardening Rules
# Copyright (c) 2026 Ekush contributors · NaserX Labs (Abu Naser Khan)

# Preserve source line numbers for readable manual diagnostic stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Android InputMethodService entry point referenced by AndroidManifest.xml
-keep class com.example.ime.EkushImeService {
    public <init>();
}

# Preserve custom Views inflated or referenced dynamically
-keep class com.example.ime.EkushKeyboardView { *; }
-keep class com.example.ime.SuggestionStripView { *; }

# Keep Room database, entities, and generated implementations
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Keep ViewModels instantiated via reflection
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# Pure-Kotlin engine data models
-keep class com.ekush.engine.** { *; }

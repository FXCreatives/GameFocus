# Keep everything to minimize impact, just use R8 to shrink dex count
-keep class ** { *; }
-keep interface ** { *; }
-dontwarn **
-dontoptimize
-dontobfuscate

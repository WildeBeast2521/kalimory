# Kalimory R8 rules. The libraries in use (Room, kotlinx.serialization, AndroidX, Compose,
# AboutLibraries) ship their own consumer rules, and the app uses no reflection, so only stack
# trace readability is configured here.

# Keep line numbers so a stack trace from a user maps back to the source with mapping.txt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

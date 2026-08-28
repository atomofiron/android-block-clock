# ProGuard/R8 rules for release builds.

# WorkManager creates the WorkDatabase via Room reflection
# (Class.forName("...WorkDatabase_Impl")), so the generated class must not be
# obfuscated or removed — otherwise the app crashes on startup with
# "Failed to create an instance of androidx.work.impl.WorkDatabase".
-keep class androidx.work.impl.WorkDatabase_Impl { *; }

# WorkManager instantiates InputMerger subclasses reflectively
# (InputMerger.fromClassName); the AAR consumer rule keeps the classes but R8
# still strips their zero-arg constructors, which breaks every worker run with
# "Trouble instantiating + ... OverwritingInputMerger".
-keep class * extends androidx.work.InputMerger {
    <init>();
}

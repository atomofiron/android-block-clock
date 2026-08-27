# Правила ProGuard/R8 для release-сборки.

# WorkManager создаёт WorkDatabase через Room рефлексией
# (Class.forName("...WorkDatabase_Impl")), поэтому сгенерированный класс
# нельзя обфусцировать или удалить — иначе краш при старте:
# "Failed to create an instance of androidx.work.impl.WorkDatabase".
-keep class androidx.work.impl.WorkDatabase_Impl { *; }

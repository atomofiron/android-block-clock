package app.blockclock.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class AppScope : CoroutineScope by CoroutineScope(Dispatchers.Default)
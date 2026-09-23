package app.blockclock.util

class UpdatableLazy<T>(private val initializer: () -> T) : Lazy<T> {

    private var initializedValue: T? = null
    private var initialized = false

    @Suppress("UNCHECKED_CAST")
    override val value: T get()  {
        if (!initialized) update()
        return initializedValue as T
    }

    override fun isInitialized(): Boolean = initialized

    fun update() {
        initialized = true
        initializedValue = initializer()
    }
}

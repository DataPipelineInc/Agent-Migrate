interface Migrate {
    fun onError(e: Throwable, args: Map<String, Any>? = null)
    fun onComplete(msg: String, args: Map<String, String>? = null)
}
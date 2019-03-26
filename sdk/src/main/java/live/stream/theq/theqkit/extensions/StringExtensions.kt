package live.stream.theq.theqkit.extensions

internal fun String?.nullIfNullOrEmpty() : String? {
  return if (this == null || this.isEmpty()) null else this
}

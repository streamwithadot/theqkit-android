package live.stream.theq.theqkit.exception

import java.lang.IllegalStateException

class QKitInitializationException(message: String = "TheQKit.init(context) has not been called. This must be called before any other QKit methods") :
    IllegalStateException(message)
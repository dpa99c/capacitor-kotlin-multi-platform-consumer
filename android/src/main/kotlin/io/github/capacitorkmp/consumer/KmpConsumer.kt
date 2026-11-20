package io.github.capacitorkmp.consumer

import com.getcapacitor.Logger

class KmpConsumer {

    fun echo(value: String): String {
        Logger.info("Echo", value)

        return value
    }
}

package fr.convergence.proddoc.util

import fr.convergence.proddoc.model.lib.obj.MaskMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.slf4j.LoggerFactory.getLogger

inline fun faireUneReponseAsynchrone(
    emitterSurLequelFaireLaReponse: Emitter<MaskMessage>?,
    crossinline message: () -> MaskMessage
) {
    GlobalScope.launch {
        val resultatInvocation = message.invoke()
        getLogger(this::class.java).info("Reponse asynchrone = $resultatInvocation")
        emitterSurLequelFaireLaReponse?.send(resultatInvocation)
    }
}
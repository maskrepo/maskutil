package fr.convergence.proddoc.util

import fr.convergence.proddoc.model.lib.obj.MaskMessage
import io.smallrye.reactive.messaging.ChannelRegistry
import org.eclipse.microprofile.reactive.messaging.Emitter
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
open class MessageUtils(@Inject open var channelRegistry: ChannelRegistry) {

    @Suppress("UNCHECKED_CAST")
    fun getEmitter(channel: String): Emitter<MaskMessage> = channelRegistry.getEmitter(channel) as Emitter<MaskMessage>

}
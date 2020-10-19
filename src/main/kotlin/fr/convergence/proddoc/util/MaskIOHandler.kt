package fr.convergence.proddoc.util

import fr.convergence.proddoc.model.lib.obj.MaskMessage
import io.smallrye.reactive.messaging.ChannelRegistry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.slf4j.LoggerFactory.getLogger
import java.util.*
import java.util.concurrent.TimeoutException
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import kotlin.concurrent.schedule

@ApplicationScoped
open class MaskIOHandler(
    @Inject open var channelRegistry: ChannelRegistry
) {

    companion object {
        val LOG = getLogger(MaskIOHandler::class.java)
    }

    private var demandeMap: MutableMap<String, String> = mutableMapOf()

    inline fun <reified T> maskIOHandler(messageOrigine: MaskMessage, block: () -> T): MaskMessage {
        try {
            return MaskMessage.reponseOk(block.invoke(), messageOrigine, messageOrigine.entete.idReference)
        } catch (ex: Exception) {
            return MaskMessage.reponseKo<T>(ex, messageOrigine, messageOrigine.entete.idReference)
        }
    }

    inline fun <reified T> maskIOHandler(messageOrigine: MaskMessage, topicReponse: String, block: () -> T) {

        Timer().schedule(10000) {
            LOG.info("Envoi d'une réponse de type : TimeOut")
            @Suppress("UNCHECKED_CAST")
            val emitter: Emitter<MaskMessage> = channelRegistry.getEmitter(topicReponse) as Emitter<MaskMessage>
            val send = emitter.send(
                MaskMessage.reponseKo<T>(
                    @Suppress("ThrowableNotThrown") TimeoutException("TimeOut sur le topic $topicReponse"),
                    messageOrigine,
                    messageOrigine.entete.idReference
                )
            )
        }
    }
}
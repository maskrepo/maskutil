package fr.convergence.proddoc.util

import io.quarkus.runtime.StartupEvent
import io.smallrye.reactive.messaging.ChannelRegistry
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory.getLogger
import java.util.stream.Collectors.joining
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Observes
import javax.inject.Inject


@ApplicationScoped
open class StartupUtils(
    @Inject open var channelRegistry: ChannelRegistry,
    @ConfigProperty(name = "quarkus.http.host") open val host: String,
    @ConfigProperty(name = "quarkus.http.port") open val port: String
) {

    companion object {
        val LOG = getLogger(StartupUtils::class.java)
    }

    open fun startup(@Observes event: StartupEvent?) {
        LOG.info(
            "Liste des topics 'sortie' pour ce micro-service : ${
                channelRegistry.outgoingNames.stream().collect(joining(", "))
            }"
        )
        LOG.info(
            "Liste des topics 'entree' pour ce micro-service : ${
                channelRegistry.incomingNames.stream().collect(joining(", "))
            }"
        )
        LOG.info(
            "Liste des 'emitters' pour ce micro-service : ${
                channelRegistry.emitterNames.stream().collect(joining(", "))
            }"
        )
        LOG.info("Ce service utilise le hot : $host et le port : $port, health sur : http://$host:$port/health")
    }

}
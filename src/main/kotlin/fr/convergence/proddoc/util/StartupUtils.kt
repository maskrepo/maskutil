package fr.convergence.proddoc.util

import io.smallrye.reactive.messaging.ChannelRegistry
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory.getLogger
import java.util.stream.Collectors
import javax.annotation.PostConstruct
import javax.enterprise.context.ApplicationScoped
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

    @PostConstruct
    open fun init() {
        LOG.info(
            "Liste des topics enregistrés pour ce micro-service : ${
                channelRegistry.emitterNames.stream().collect(Collectors.joining(", "))
            }"
        )
        LOG.info("Ce service utilise le hot : $host et le port : $port, health sur : http://$host:$port/health")
    }

}
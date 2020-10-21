package fr.convergence.proddoc.util

import fr.convergence.proddoc.model.lib.obj.MaskMessage
import fr.convergence.proddoc.model.lib.serdes.MaskMessageSerDes
import java.io.InputStream
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.client.ClientBuilder.newClient
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType


@ApplicationScoped
object WSUtils {

    /**
     * récupère un octet stream sur une url et renvoie un inputstream
     */
    fun getOctetStreamREST(urlAbs: String): InputStream {
        return (newClient()
            .target(urlAbs)
            .request(MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .get(InputStream::class.java)
                )
    }

    /**
     * poste un stream et retourne un stream
     */
    fun postOctetStreamREST(urlOuFaireLePost: String, maskMessage: MaskMessage): InputStream {
        val serialize = MaskMessageSerDes().serialize("topic", maskMessage)
        return newClient()
            .target(urlOuFaireLePost)
            .request(MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .post(Entity.entity(serialize, MediaType.APPLICATION_JSON_TYPE), InputStream::class.java)
    }
}
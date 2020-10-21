package fr.convergence.proddoc.util

import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory.getLogger
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.NotFoundException
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder


@ApplicationScoped
open class MyGreffeUtil(@ConfigProperty(name = "myGreffe.baseUrl") open val myGreffeBaseUrl: String) {

    companion object {
        private val LOG = getLogger(MyGreffeUtil::class.java)
        private const val PATH_URL_MYGREFFE = "/convergence-greffe-web/rest"
        private const val TIMEOUT: Long = 2000
    }

    private fun creerURLApplimyGreffe(): String = myGreffeBaseUrl + PATH_URL_MYGREFFE

    /**
     * Fabrique une URI myGreffe à partir d'un path et d'une map de paramètres éventuels
     * Attention l'URL de base (le host) est définie dans la méthode creerURLApplimyGreffe
     */
    fun fabriqueURImyGreffe(
        pathDuService: String = "/kbis",
        parametresRequete: Map<String, *>
    ): URI {

        // 1) fabriquer l'URI à partir des paramètres fournis en entrée
        var uriString = creerURLApplimyGreffe() + pathDuService
        val uriWSmyGreffe: URI
        val builder = UriBuilder.fromUri(uriString)

        // parcourir la Map des paramètres pour les ajouter à l'URI :
        for (param in parametresRequete) {
            builder.queryParam(param.key, param.value.toString())
        }

        // 2) appeler le service en fonction du retour attendu
        uriWSmyGreffe = builder.build()
        LOG.debug("uriWSmyGreffe fabriquée : $uriWSmyGreffe")

        // 3) répondre à l'appelant
        return (uriWSmyGreffe)
    }

    /**
     * récupère un flux en appelant un  WS rest myGreffe
     * en entrée : l'URI... le contenu attendu
     * en sortie : une Response
     *        - contenant le flux, quel qu'il soit (binaire, json, xml, etc...)
     *        - avec un content-type bien renseigné
     */
    fun appelleURImyGreffe(uriCible: URI, timeOut: Long = TIMEOUT, contenuAttendu: String): Response {

        val retourWS = try {
            // Appel de l'URI du Kbis PDF
            val monClient = ClientBuilder.newBuilder()
                .connectTimeout(timeOut, TimeUnit.MILLISECONDS)
                .build()
            val maReponse = monClient
                .target(uriCible)
                .request(MediaType.WILDCARD)
                .get()

            LOG.debug("L'URI suivante a été appelée : $uriCible")

            // si not found ou server error on lève une exception sinon on retourne le stream
            when (maReponse.status.toString()) {
                Response.Status.INTERNAL_SERVER_ERROR.toString() -> {
                    LOG.error("Appel à myGreffe en erreur")
                    throw IllegalStateException(maReponse.statusInfo.reasonPhrase)
                }
                Response.Status.NOT_FOUND.toString() -> {
                    LOG.error("Document non trouvé lors de l'appel à myGreffe")
                    throw NotFoundException(maReponse.statusInfo.reasonPhrase)
                }
                else -> {
                    if ((maReponse.getHeaderString(HttpHeaders.CONTENT_TYPE) != contenuAttendu)
                        && contenuAttendu != "*"
                    ) {
                        LOG.error("contenuAttendu = $contenuAttendu")
                        LOG.error("contenu de la réponse  = ${maReponse.getHeaderString(HttpHeaders.CONTENT_TYPE)}")
                        throw IllegalStateException("Le contenu de la réponse : ${maReponse.getHeaderString(HttpHeaders.CONTENT_TYPE)} n'est pas celui attendu : $contenuAttendu")
                    } else maReponse
                }
            }
        } catch (ex: Exception) {
            throw if (ex.cause is TimeoutException) {
                LOG.error("Timeout sur l'appel à myGreffe")
                TimeoutException(ex.message)
            } else {
                ex
            }
        }

        return retourWS
    }


    /**
     * fait la fabrication de l'URL + l'appel et retourne la response
     */
    fun demandeRestURLmyGreffe(
        pathDuService: String,
        parametresRequete: Map<String, *>,
        timeOut: Long = TIMEOUT,
        retourAttendu: String
    ): Response {

        return (appelleURImyGreffe(
            fabriqueURImyGreffe(pathDuService, parametresRequete),
            timeOut,
            retourAttendu
        ))
    }
}
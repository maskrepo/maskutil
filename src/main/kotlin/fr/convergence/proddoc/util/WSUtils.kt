package fr.convergence.proddoc.util

import fr.convergence.proddoc.model.lib.obj.MaskMessage
import fr.convergence.proddoc.model.lib.serdes.MaskMessageSerDes
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import java.io.InputStream
import java.net.URI
import java.util.concurrent.TimeoutException
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.NotFoundException
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder

@ApplicationScoped
object WSUtils {

    private val LOG: Logger = LoggerFactory.getLogger(WSUtils::class.java)

    private const val PATH_URL_MYGREFFE = "/convergence-greffe-web/rest"
    // Url de myGreffe => "http://172.31.4.97:8880"
    // Url du simulacre Outlaw => "http://localhost:8100"
    // Url de Alain => "http://127.0.0.1:3001"
    private const val BASE_URL_MYGREFFE = "http://localhost:8100"

    private fun creerURLApplimyGreffe(): String = BASE_URL_MYGREFFE + PATH_URL_MYGREFFE

    /**
     * enum des différents retours possibles lors de l'appel à un WS myGreffe :
     * PDF ou JSON ou XML ou HTTP.Response ou alors un pointeur (comme une URL d'accès par exemple)
     */
    enum class TypeRetourWS { PDF, JSON, XML, HTTP_RESPONSE, POINTEUR, TOPIC_MESSAGE }

    /**
     * Fabrique une URI à partir d'un path et d'une map de paramètres éventuels
     * Attention l'URL de base (le host) est définie ailleurs c'est supposé est un paramètre applicatif
     * (voir méthode creerURLApplimyGreffe)
     */
    fun fabriqueURI(
        pathDuService: String = "/kbis", typeRetour: TypeRetourWS,
        parametresRequete: Map<String, String>
    ): URI {

        // 1) fabriquer l'URI à partir des paramètres fournis en entrée
        var uriString = creerURLApplimyGreffe() + pathDuService
        when (typeRetour) {
            TypeRetourWS.PDF -> {
                uriString += "/recupererPdf"
            }
            TypeRetourWS.JSON -> {
                uriString += "/recupererJson"
            }
            TypeRetourWS.XML -> {
                uriString += "/recupererXML"
            }
            TypeRetourWS.HTTP_RESPONSE -> {
                uriString += "/recupererResponse"
            }
            TypeRetourWS.POINTEUR -> {
                uriString += "/recupererPointeur"
            }
            TypeRetourWS.TOPIC_MESSAGE -> {
            uriString
        }
        }

        val uriWSmyGreffe: URI
        val builder = UriBuilder.fromUri(uriString)
        // parcourir la Map des paramètresp our les ajouter à l'URI :
        for (param in parametresRequete) {
            builder.queryParam(param.key, param.value)
        }

        // 2) appeler le service en fonction du retour attendu
        uriWSmyGreffe = builder.build()
        LOG.debug("uriWSmyGreffe : $uriWSmyGreffe")

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
    fun appelleURI(uriCible: URI, timeOut: Long = 10000, contenuAttendu :String) : Response {

        val retourWS = try {
            // Appel de l'URI du Kbis PDF
            var mareponse = ClientBuilder.newClient()
                    .target(uriCible)
                    .request(MediaType.WILDCARD)
                    .get()
            // peut-on gérer un timeout? à creuser

            LOG.debug("L'URI suivante a été appelée : $uriCible")

            // si not found ou server error on lève une exception sinon on retourne le stream
            when (mareponse.status.toString()) {
                Response.Status.INTERNAL_SERVER_ERROR.toString() -> {
                    LOG.error("Appel à myGreffe en erreur")
                    throw IllegalStateException(mareponse.statusInfo.reasonPhrase)
                }
                Response.Status.NOT_FOUND.toString() -> {
                    LOG.error("Document non trouvé lors de l'appel à myGreffe")
                    throw NotFoundException(mareponse.statusInfo.reasonPhrase)
                }
                else -> {
                    if ( (mareponse.getHeaderString(HttpHeaders.CONTENT_TYPE) != contenuAttendu)
                            && contenuAttendu != "*")   {
                        LOG.error("contenuAttendu = $contenuAttendu")
                        LOG.error("contenu de la réponse  = ${mareponse.getHeaderString(HttpHeaders.CONTENT_TYPE)}")
                        throw IllegalStateException("Le contenu de la réponse : ${mareponse.getHeaderString(HttpHeaders.CONTENT_TYPE)} n'est pas celui attendu : $contenuAttendu" )
                    }
                    else mareponse
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

    fun getOctetStreamREST(urlAbs: String): InputStream {
        return (ClientBuilder.newClient()
            .target(urlAbs)
            .request(MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .get(InputStream::class.java)
                )
    }

    fun postOctetStreamREST(urlOuFaireLePost: String, maskMessage: MaskMessage): InputStream {
        val serialize = MaskMessageSerDes().serialize("topic", maskMessage)
        return ClientBuilder.newClient()
            .target(urlOuFaireLePost)
            .request(MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .post(Entity.entity(serialize, MediaType.APPLICATION_JSON_TYPE), InputStream::class.java)
    }
}
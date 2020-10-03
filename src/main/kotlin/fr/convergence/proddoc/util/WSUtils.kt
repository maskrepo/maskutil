package fr.convergence.proddoc.util

import fr.convergence.proddoc.model.lib.obj.MaskMessage
import fr.convergence.proddoc.model.lib.serdes.MaskMessageSerDes
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.buffer.Buffer
import io.vertx.reactivex.ext.web.client.HttpResponse
import io.vertx.reactivex.ext.web.client.WebClient
import java.io.InputStream
import java.net.URI
import java.util.concurrent.TimeoutException
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.NotFoundException
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
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
     * enum des différents retours possibles lros de l'appel à un WS myGreffe :
     * PDF ou JSON ou XML ou HTTP.Response ou alors un pointeur (comme une URL d'accès par exemple)
     */
    enum class TypeRetourWS { PDF, JSON, XML, HTTP_RESPONSE, POINTEUR }

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
     * en entrée : l'URL
     * en sortie : une HttpResponse
     *        - contenant le flux, quel qu'il soit (binaire, json, xml, etc...)
     *        - avec un content-type bien renseigné
     */
    fun <T> appelleURI(uriCible: URI, timeOut: Long = 10000, block: (resp: HttpResponse<Buffer?>) -> T): T {

        val client = WebClient.create(Vertx.vertx())
        val retourWS: T

        retourWS = try {
            client.getAbs(uriCible.toASCIIString())
                .timeout(timeOut)
                .rxSend()
                .map {
                    // gestion spécifique des éventuelles erreurs qui nous intéressent
                    // (404, 500, TimeOut...)
                    if (it.statusCode() == Response.Status.NOT_FOUND.statusCode) {
                        LOG.error("Document non trouvé lors de l'appel à myGreffe")
                        throw NotFoundException(it.statusMessage())
                    } else if (it.statusCode() == Response.Status.INTERNAL_SERVER_ERROR.statusCode) {
                        LOG.error("Appel à myGreffe en erreur")
                        throw IllegalStateException(it.statusMessage())
                    } else {
                        block.invoke(it)
                    }
                }
                .blockingGet()

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
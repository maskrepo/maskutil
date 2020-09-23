package fr.convergence.proddoc.util

import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.buffer.Buffer
import io.vertx.reactivex.ext.web.client.HttpResponse
import io.vertx.reactivex.ext.web.client.WebClient
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeoutException
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.NotFoundException
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder

@ApplicationScoped
object WSUtils {

    private val LOG: Logger = LoggerFactory.getLogger(WSUtils::class.java)

    /**
     *    crée un fichier temporaire à partir d'un tableau de bytes
     *    et retourne un objet fichier
     */
    fun creeFichierTempBinaire(fichier: ByteArrayInputStream): File {
        try {
            LOG.debug("Début création du fichier binaire temporaire")
            val fichierTemp = createTempFile(suffix = ".pdf")
            fichierTemp.writeBytes(fichier.readBytes())
            LOG.debug("Fin création du fichier binaire temporaire")
            return (fichierTemp)
        } catch (e: java.lang.Exception) {
            if (e is IOException) {
                throw (IllegalStateException("Problème d'écriture sur disque", e))
            } else {
                throw e
            }
        }
    }

    /**
     *  crée et retourne l'URL de récupération du Kbis à partir d'un identifiant du cache
     *  l'URL de base est un paramètre "applicatif"...
     */
    fun creeURLKbisLocale(numGestion: String): String {

        val baseURL = "http://127.0.0.1:8080/"
        val pathURL = "kbis/pdfnumgestion/$numGestion"

        return (baseURL + pathURL)
    }

    private fun creerURLApplimyGreffe() :String {

        // val baseURL = "http://172.31.4.97:8880"
        val baseURL = "http://127.0.0.1:3001"
        val pathURL = "/convergence-greffe-web/rest"

        return (baseURL + pathURL)
    }

    /**
     * enum des différents retours possibles lros de l'appel à un WS myGreffe :
     * PDF ou JSON ou XML ou HTTP.Response ou alors un pointeur (comme une URL d'accès par exemple)
     */
    enum class TypeRetourWS  { PDF, JSON, XML, HTTP_RESPONSE, POINTEUR }

    /**
     * Fabrique une URI à partir d'un path et d'une map de paramètres éventuels
     * Attention l'URL de base (le host) est définie ailleurs c'est supposé est un paramètre applicatif
     * (voir méthode creerURLApplimyGreffe)
     */
    fun fabriqueURI(pathDuService :String= "/kbis", typeRetour :TypeRetourWS,
                    parametresRequete :Map<String, String> ) :URI
    {

        // 1) fabriquer l'URI à partir des paramètres fournis en entrée
        var uriString =  creerURLApplimyGreffe()+pathDuService
        when (typeRetour){
            TypeRetourWS.PDF -> {uriString+="/recupererPdf"}
            TypeRetourWS.JSON -> {uriString+="/recupererJson"}
            TypeRetourWS.XML -> {uriString+="/recupererXML"}
            TypeRetourWS.HTTP_RESPONSE -> {uriString+="/recupererResponse"}
            TypeRetourWS.POINTEUR ->{uriString+="/recupererPointeur"}
        }

        val uriWSmyGreffe :URI
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
    fun <T> appelleURI(uriCible: URI, timeOut :Long = 10000, block: (resp : HttpResponse<Buffer?>) -> T): T {

        val client = WebClient.create(Vertx.vertx())
        val retourWS : T

        retourWS = try {
            client.getAbs(uriCible.toASCIIString())
                    .timeout(timeOut)
                    .rxSend()
                    .map {
                        // gestion spécifique des éventuelles erreurs qui nous intéressent
                        // (404, 500, TimeOut...)
                        if (it.statusCode()== Response.Status.NOT_FOUND.statusCode) {
                            throw NotFoundException(it.statusMessage())
                        }
                        else if (it.statusCode()== Response.Status.INTERNAL_SERVER_ERROR.statusCode) {
                            throw IllegalStateException(it.statusMessage())
                        }
                        else {
                            block.invoke(it)
                        }
                    }
                    .blockingGet()

        } catch (e: Exception) {
            if (e.cause is TimeoutException) {
                LOG.error("Timeout sur l'appel à myGreffe")
                throw TimeoutException(e.message)
            }
            if (e is NotFoundException) {
                LOG.error("Document non trouvé lors de l'appel à myGreffe")
                throw NotFoundException(e.message)
            }
            if (e is IllegalStateException) {
                LOG.error("Appel à myGreffe en erreur")
                throw NotFoundException(e.message)
            }else {
                throw (e)
            }
        }

        return retourWS
    }
}
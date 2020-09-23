package fr.convergence.proddoc.util

import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
object FichiersUtils {

    private val LOG: Logger = LoggerFactory.getLogger(FichiersUtils::class.java)

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

}
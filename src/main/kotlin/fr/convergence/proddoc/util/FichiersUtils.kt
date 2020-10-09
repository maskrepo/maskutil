package fr.convergence.proddoc.util

import org.slf4j.LoggerFactory.getLogger
import java.io.*
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class FichiersUtils {

    companion object {
        private val LOG = getLogger(FichiersUtils::class.java)
    }

    /**
     *    crée un fichier temporaire à partir d'un tableau de bytes
     *    et retourne un objet fichier
     */
    fun creeFichierTempBinaire(fichier: ByteArrayInputStream): File {
        return internalCreeFichierTempBinaire {
            it.writeBytes(fichier.readBytes())
        }
    }

    /**
     *    crée un fichier temporaire à partir d'un java.io.file
     *    et retourne un objet fichier
     */
    fun creeFichierTempByteArray(fichier: ByteArray): File {
        return internalCreeFichierTempBinaire {
            it.writeBytes(fichier)
        }
    }

    /**
     *    crée un fichier temporaire à partir d'un java.io.file
     *    et retourne un objet fichier
     */
    fun creeFichierTemp(fichier: File): File {
        return internalCreeFichierTempBinaire {
            it.writeBytes(fichier.readBytes())
        }
    }

    private fun internalCreeFichierTempBinaire(block: (File) -> Unit): File {
        try {
            LOG.debug("Début création du fichier binaire temporaire")
            val fichierTemp = createTempFile(suffix = ".pdf")
            LOG.debug("Nom du fichier binaire temporaire : ${fichierTemp.name}")
            block.invoke(fichierTemp)
            LOG.debug("Fin création du fichier binaire temporaire")
            return fichierTemp
        } catch (e: java.lang.Exception) {
            if (e is IOException) {
                throw (IllegalStateException("Problème d'écriture sur disque", e))
            } else {
                throw e
            }
        }
    }

    /**
     * met un inputStream dans un fichier
     * le streaming c'est bon pour la mémoire
     */
    fun copyInputStreamToTempFile(entree: InputStream, identifiantFichier: String): File {
        try {
            val fichierTemp = createTempFile(identifiantFichier, suffix = ".fic")
            val sortie = FileOutputStream(fichierTemp)
            entree.transferTo(sortie)
            return fichierTemp
        } catch (ioException: IOException) {
            throw ioException
        } catch (e: Exception) {
            throw e
        }

    }
}
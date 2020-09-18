package fr.convergence.proddoc.util

import fr.convergence.proddoc.model.lib.obj.MaskMessage

inline fun <reified T> maskIOHandler(messageOrigine: MaskMessage, block: () -> T): MaskMessage {
    try {
        return MaskMessage.reponseOk(block.invoke(), messageOrigine)
    } catch (ex: Exception) {
        return MaskMessage.reponseKo<T>(ex, messageOrigine)
    }
}
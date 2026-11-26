package sgyt.pu.core

/**
 * Implemented by the Application class of every edition so Compose screens can
 * reach the shared core graph without depending on an edition-specific class.
 */
interface PuGraphOwner {
    val puGraph: PuCoreGraph
}

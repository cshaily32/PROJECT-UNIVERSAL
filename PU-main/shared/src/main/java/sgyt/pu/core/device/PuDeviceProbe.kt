package sgyt.pu.core.device

import sgyt.pu.core.model.PuDeviceSnapshot
import sgyt.pu.core.model.PuEditionRecommendation

/**
 * Domain-facing contract for reading the device facts.
 *
 * The presentation layer depends on this interface, so the Android-specific
 * inspector stays behind the boundary.
 */
interface PuDeviceProbe {
    fun snapshot(): PuDeviceSnapshot

    fun recommend(snapshot: PuDeviceSnapshot = snapshot()): PuEditionRecommendation
}

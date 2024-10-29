package cz.cvut.fel.dsva.datastructure.system

data class WorkStation(
    val ip: ByteArray,
    val port: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WorkStation

        if (!ip.contentEquals(other.ip)) return false
        if (port != other.port) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ip.contentHashCode()
        result = 31 * result + port
        return result
    }
}

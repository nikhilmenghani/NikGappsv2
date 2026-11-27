package com.nikgapps.v2

internal sealed class RegistryException(message: String) : Exception(message) {
    class UnsupportedApi(id: String, api: Int, min: Int?, max: Int?) : RegistryException("Package '$id' does not support API $api (min=$min, max=$max)")
    class UnsupportedArchitecture(id: String, arch: String) : RegistryException("Package '$id' does not support $arch")
    class UnsupportedDeviceType(id: String, type: String) : RegistryException("Package '$id' does not support $type devices")
}

internal fun validateCompatibility(id: String, version: PackageVersion, api: Int, architecture: String,
    deviceType: String = "phone", androidVersion: String? = null) {
    if ((androidVersion != null && version.supportedAndroidVersions.isNotEmpty() && androidVersion !in version.supportedAndroidVersions) ||
        (version.android.minApi != null && api < version.android.minApi) || (version.android.maxApi != null && api > version.android.maxApi))
        throw RegistryException.UnsupportedApi(id, api, version.android.minApi, version.android.maxApi)
    if (version.architectures.isNotEmpty() && architecture !in version.architectures)
        throw RegistryException.UnsupportedArchitecture(id, architecture)
    if (version.deviceTypes.isNotEmpty() && deviceType !in version.deviceTypes)
        throw RegistryException.UnsupportedDeviceType(id, deviceType)
}

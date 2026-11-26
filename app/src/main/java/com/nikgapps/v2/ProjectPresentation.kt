package com.nikgapps.v2

internal enum class ProjectUpdateKind { CURRENT, AVAILABLE, INCOMPATIBLE }
internal data class ProjectUpdate(val kind: ProjectUpdateKind, val releaseId: String?, val message: String)
internal fun isProjectOutdated(unavailable: Set<String>?, update: ProjectUpdate?): Boolean =
    unavailable?.isNotEmpty() == true || update?.kind == ProjectUpdateKind.INCOMPATIBLE

internal fun initialProjectDisplayOrder(projects: List<BuildProject>, unavailable: Map<String, Set<String>>,
    updates: Map<String, ProjectUpdate>): List<String> = projects.sortedBy {
    isProjectOutdated(unavailable[it.id], updates[it.id])
}.map { it.id }

internal fun projectsInDisplayOrder(projects: List<BuildProject>, order: List<String>): List<BuildProject> {
    val byId = projects.associateBy { it.id }
    val known = order.toSet()
    return order.distinct().mapNotNull(byId::get) + projects.filter { it.id !in known }
}

package io.github.yin.residencestoragespigot.supports

import java.util.UUID

class ResidenceInfo(
    var residenceName: String,
    var ownerUUID: UUID,
    var owner: String,
    var residenceFlags: MutableMap<String, Boolean>,
    var playerFlags: MutableMap<String, MutableMap<String, Boolean>>,
    var serverName: String
)
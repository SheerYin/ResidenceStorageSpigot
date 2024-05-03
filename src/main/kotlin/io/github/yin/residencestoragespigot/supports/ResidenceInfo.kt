package io.github.yin.residencestoragespigot.supports

class ResidenceInfo(
    var residenceName: String,
    var ownerUUID: String,
    var owner: String,
    var residenceFlags: MutableMap<String, Boolean>,
    var playerFlags: MutableMap<String, MutableMap<String, Boolean>>,
    var serverName: String
)
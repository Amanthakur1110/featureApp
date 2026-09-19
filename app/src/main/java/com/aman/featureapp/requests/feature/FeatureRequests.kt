package com.aman.featureapp.requests.feature

data class CreateFeatureRequest(
    val name: String,
    val description: String
)

data class UpdateFeatureRequest(
    val name: String? = null,
    val description: String? = null,
    val is_public: Boolean? = null,
    val is_favourite: Boolean? = null
)

data class ClarifyFeatureRequest(
    val answer: String
)

data class EditFeatureRequest(
    val description: String
)

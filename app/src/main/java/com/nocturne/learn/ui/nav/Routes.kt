package com.nocturne.learn.ui.nav

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val MAP = "map"
    const val REVIEW = "review"
    const val PROFILE = "profile"
    const val SESSION = "session"
    const val ASK = "ask"
    const val IMPORT_TEXTBOOK = "import"
    const val OUTLINE = "outline"
    const val SEARCH_SETTINGS = "search"

    const val API_PROVIDERS = "api"
    const val API_NEW = "api/new/{templateId}"
    const val API_EDIT = "api/edit/{providerId}"

    val TABS = setOf(HOME, MAP, REVIEW, PROFILE)

    fun apiNew(templateId: String) = "api/new/$templateId"
    fun apiEdit(providerId: String) = "api/edit/$providerId"
}

package com.hermanoslimpieza.mobile

import android.app.Application
import com.hermanoslimpieza.mobile.data.ApiClient
import com.hermanoslimpieza.mobile.data.TokenStore

class HermanosApp : Application() {
    lateinit var tokenStore: TokenStore
        private set
    lateinit var apiClient: ApiClient
        private set

    override fun onCreate() {
        super.onCreate()
        tokenStore = TokenStore(this)
        apiClient = ApiClient(tokenStore)
    }
}

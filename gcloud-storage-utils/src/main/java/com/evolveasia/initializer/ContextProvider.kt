package com.evolveasia.initializer

import android.content.Context

internal abstract class ContextProvider {

    companion object {
        fun getAppContext(context: Context?) {
            ContextProviderImpl.setContext(context)
        }
    }
}
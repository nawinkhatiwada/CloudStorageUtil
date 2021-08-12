package com.evolveasia.initializer

import android.content.Context
import java.lang.RuntimeException

internal class ContextProviderImpl private constructor(private val ctx: Context?) {

    companion object {
        private var context: Context? = null
        private var contextProviderImpl: ContextProviderImpl? = null

        fun setContext(context: Context?) {
            this.context = context
        }

       fun getInstance(): ContextProviderImpl {
            return if (this.contextProviderImpl == null) {
                ContextProviderImpl(this.context)
            } else {
                this.contextProviderImpl!!
            }
        }
    }

    fun getAppCtx(): Context {
        if (this.ctx == null) {
            throw RuntimeException("CloudUtilInitProvider not set in the Manifest. \n" +
                    "Try adding provider in the manifest file to initialize the library properly. ")
        }
        return ctx
    }
}
package com.synap.compose.routing

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
class RouteHandlerScope(
    val route: Route?,
    val parameters: Array<Any?>,
    private val push: (Route?, Array<Any?>) -> Unit,
    val pop: () -> Unit,
) {
    @SuppressLint("ComposableNaming")
    @Composable
    inline fun host(content: @Composable () -> Unit) {
        if (route == null) {
            content()
        }
    }

    operator fun Route.invoke() {
        push(this, arrayOfNulls(2))
    }

    operator fun <P0> Route.invoke(p0: P0) {
        push(this, arrayOf(p0, null))
    }

    operator fun <P0, P1> Route.invoke(p0: P0, p1: P1) {
        push(this, arrayOf(p0, p1))
    }

    fun replace(newRoute: Route?, vararg params: Any?) {
        pop()
        push(newRoute, arrayOf(*params, null, null).copyOf(2))
    }
}

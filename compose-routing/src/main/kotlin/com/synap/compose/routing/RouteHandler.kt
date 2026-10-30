package com.synap.compose.routing

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@ExperimentalAnimationApi
@Composable
fun RouteHandler(
    modifier: Modifier = Modifier,
    listenToGlobalEmitter: Boolean = false,
    handleBackPress: Boolean = true,
    transitionSpec: AnimatedContentScope<RouteHandlerScope>.() -> ContentTransform = {
        when {
            isStacking -> defaultStacking
            isStill -> defaultStill
            else -> defaultUnstacking
        }
    },
    content: @Composable RouteHandlerScope.() -> Unit
) {
    var stack by rememberSaveable(
        stateSaver = listSaver(
            save = { stack ->
                stack.map { entry ->
                    listOf(entry.first?.tag ?: "", entry.second.toList())
                }
            },
            restore = { saved ->
                saved.map { item ->
                    val list = item as List<Any?>
                    val tag = list[0] as String
                    val params = list[1] as List<Any?>
                    (if (tag.isEmpty()) null else Route(tag)) to params.toTypedArray()
                }
            }
        )
    ) {
        mutableStateOf(emptyList<Pair<Route?, Array<Any?>>>())
    }

    val currentEntry = stack.lastOrNull()
    val route = currentEntry?.first
    val parameters = currentEntry?.second ?: remember { arrayOfNulls<Any?>(2) }

    val onRouteChanged: (Route?, Array<Any?>) -> Unit = { newRoute, newParameters ->
        stack = if (newRoute == null) {
            emptyList()
        } else {
            stack + (newRoute to newParameters)
        }
    }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val scope = remember(route, parameters, stack) {
        RouteHandlerScope(
            route = route,
            parameters = parameters,
            push = onRouteChanged,
            pop = { 
                if (handleBackPress) backDispatcher?.onBackPressed() 
                else if (stack.isNotEmpty()) stack = stack.dropLast(1)
            }
        )
    }

    if (listenToGlobalEmitter) {
        OnGlobalRoute { (newRoute, newParameters) ->
            val p = arrayOfNulls<Any?>(2)
            newParameters.copyInto(p)
            onRouteChanged(newRoute, p)
        }
    }

    BackHandler(enabled = handleBackPress && route != null) {
        stack = stack.dropLast(1)
    }

    updateTransition(targetState = scope, label = null).AnimatedContent(
        transitionSpec = transitionSpec,
        contentKey = { it.route },
        modifier = modifier,
    ) {
        it.content()
    }
}

@Suppress("unused")
@ExperimentalAnimationApi
@Composable
fun RouteHandler(
    route: Route?,
    onRouteChanged: (Route?, Array<Any?>) -> Unit,
    modifier: Modifier = Modifier,
    listenToGlobalEmitter: Boolean = false,
    handleBackPress: Boolean = true,
    transitionSpec: AnimatedContentScope<RouteHandlerScope>.() -> ContentTransform = {
        when {
            isStacking -> defaultStacking
            isStill -> defaultStill
            else -> defaultUnstacking
        }
    },
    content: @Composable RouteHandlerScope.() -> Unit
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val parameters = remember { arrayOfNulls<Any?>(2) }

    val scope = remember(route) {
        RouteHandlerScope(
            route = route,
            parameters = parameters,
            push = onRouteChanged,
            pop = { 
                if (handleBackPress) backDispatcher?.onBackPressed() 
                else onRouteChanged(null, arrayOfNulls(2)) 
            }
        )
    }

    if (listenToGlobalEmitter) {
        OnGlobalRoute { (newRoute, newParameters) ->
            val p = arrayOfNulls<Any?>(2)
            newParameters.copyInto(p)
            onRouteChanged(newRoute, p)
        }
    }

    BackHandler(enabled = handleBackPress && route != null) {
        onRouteChanged(null, arrayOfNulls(2))
    }

    updateTransition(targetState = scope, label = null).AnimatedContent(
        transitionSpec = transitionSpec,
        contentKey = { it.route },
        modifier = modifier,
    ) {
        it.content()
    }
}

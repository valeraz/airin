/*
 * Copyright 2021 Pavlo Stavytskyi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("PropertyName")

package org.morfly.airin.starlark.library

import org.morfly.airin.starlark.elements.*
import org.morfly.airin.starlark.lang.*
import org.morfly.airin.starlark.lang.feature.FunctionCallContext
import org.morfly.airin.starlark.lang.feature.registerFunctionCallStatement


// ===== register_toolchains =====

/**
 * register_toolchains Bazel function.
 */
fun WorkspaceContext.register_toolchains(vararg toolchains: Label?) {
    val args = linkedSetOf<Argument>().also {
        it += Argument("", Expression(toolchains.toList(), ::ListExpression))
    }
    registerFunctionCallStatement("register_toolchains", args)
}

// ===== workspace =====

/**
 * workspace Bazel function.
 */
fun WorkspaceContext.workspace(
    name: Name,
    managed_directories: Map<Key, Value>? = UnspecifiedDictionary
) {
    val args = linkedSetOf<Argument>().also {
        it += Argument("name", Expression(name, ::StringLiteral))
        if (managed_directories !== UnspecifiedDictionary)
            it += Argument("managed_directories", Expression(managed_directories, ::DictionaryExpression))
    }
    registerFunctionCallStatement("workspace", args)
}

/**
 * workspace Bazel function.
 */
fun BuildContext.workspace(body: WorkspaceFunctionContext.() -> Unit) {
    registerFunctionCallStatement("workspace", WorkspaceFunctionContext(), body)
}

class WorkspaceFunctionContext : FunctionCallContext() {
    var name: Name by fargs
    var managed_directories: Map<Key, Value>? by fargs
}

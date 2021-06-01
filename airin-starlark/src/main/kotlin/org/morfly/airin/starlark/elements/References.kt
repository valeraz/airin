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

package org.morfly.airin.starlark.elements

import org.morfly.airin.starlark.lang.*


/**
 * Syntax element that represents a variable.
 */
sealed interface Reference : Expression {

    /**
     * Name of the variable.
     */
    val name: String

    override fun <A> accept(visitor: ElementVisitor<A>, position: Int, mode: PositionMode, accumulator: A) {
        visitor.visit(this, position, mode, accumulator)
    }
}

/**
 * Syntax element for a string variable.
 */
class StringReference(override val name: String) : Reference,
    StringType by name

/**
 * Syntax element for a list variable.
 */
class IntegerReference(override val name: String) : Reference,
    IntegerType by 0L

/**
 * Syntax element for a dictionary variable.
 */
class FloatReference(override val name: String) : Reference,
    FloatType by 0.0

/**
 * Syntax element for a boolean variable.
 */
class BooleanReference(override val name: String) : Reference,
    BooleanType by false

/**
 * Syntax element for a list variable.
 */
class ListReference<out T>(override val name: String) : Reference,
    List<T> by emptyList()

/**
 * Syntax element for a dictionary variable
 */
class DictionaryReference<K /*: Key*/, V : Value>(override val name: String) : Reference,
    Map<K, V> by emptyMap()

/**
 * Syntax element for variable if the type does not matter..
 */
@JvmInline
value class AnyReference(override val name: String) : Reference
/*
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2016-2019 Maksim Gramin
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.mgramin.sqlboot.model.uri.wrappers

import com.github.mgramin.sqlboot.model.uri.Uri

/**
 * @author Maksim Gramin (mgramin@gmail.com)
 * @version $Id: bce1e45321cd26b04727a56fc64fbc4ff54bd82c $
 * @since 0.1
 */
class SqlPlaceholdersWrapper(private val origin: Uri) : Uri {

    override fun type(): String {
        return origin.type()
    }

    override fun path(): List<String> {
        return origin.path().asSequence()
                .map { v -> v.replace("*", "%") }
                .toList()
    }

    override fun path(index: Int): String {
        return origin.path(index).replace("*", "%")
    }

    override fun recursive(): Boolean? {
        return origin.recursive()
    }

    override fun params(): Map<String, String> {
        return origin.params()
    }

    override fun action(): String {
        return origin.action()
    }

    override fun toString(): String {
        return origin.toString()
    }

}

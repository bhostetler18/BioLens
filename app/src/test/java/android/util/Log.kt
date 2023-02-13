/*
 * Copyright (c) 2022-2023 University of Florida
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

// Quick workaround for mocking:
// see https://stackoverflow.com/questions/36787449/how-to-mock-method-e-in-log

@file:JvmName("Log")

package android.util

fun v(tag: String, msg: String): Int = mockLog("VERBOSE", tag, msg)
fun d(tag: String, msg: String): Int = mockLog("DEBUG", tag, msg)
fun w(tag: String, msg: String): Int = mockLog("WARN", tag, msg)
fun e(tag: String, msg: String): Int = mockLog("ERROR", tag, msg)

private fun mockLog(name: String, tag: String, msg: String): Int {
    println("$name: $tag: $msg")
    return 0
}

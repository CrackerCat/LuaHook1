package com.kulipai.luahook.LuaLib

import android.Manifest
import com.kulipai.luahook.simplifyLuaError
import com.kulipai.luahook.util.ShellManager
import com.kulipai.luahook.util.d
import de.robv.android.xposed.XposedBridge
import org.luaj.Globals
import org.luaj.LuaTable
import org.luaj.LuaValue
import org.luaj.LuaValue.NIL
import org.luaj.LuaValue.NONE
import org.luaj.Varargs
import org.luaj.lib.OneArgFunction
import org.luaj.lib.VarArgFunction
import org.luaj.lib.jse.CoerceJavaToLua

object LuaUtil {
    fun Shell(_G: Globals) {
        _G["shell"] = object : OneArgFunction() {
            override fun call(cmdValue: LuaValue): LuaValue {
                val cmd = cmdValue.checkjstring() // 确保输入是字符串

                // 调用 ShellManager.shell 获取 Pair 返回值
                val resultPair: Pair<String, Boolean> = ShellManager.shell(cmd)

                // 将 Pair 的 first (执行结果) 和 second (是否成功) 转换为 LuaValue
                val luaResultString = CoerceJavaToLua.coerce(resultPair.first)
                val luaSuccessBoolean = CoerceJavaToLua.coerce(resultPair.second)

                // 使用 varargsOf 返回两个 LuaValue，模拟 Lua 的多返回值
                return varargsOf(luaResultString, luaSuccessBoolean) as LuaValue
            }
        }
    }

    fun LoadBasicLib(_G: Globals) {
        LuaHttp.registerTo(_G)
        Luafile.registerTo(_G)
        LuaJson.registerTo(_G)
        LuaTask.registerTo(_G)
        LuaResource.registerTo(_G)
        LuaSharedPreferences.registerTo(_G)
        LuaDrawableLoader().registerTo(_G)


        _G["pcall"] = object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg1().isfunction()) {
                    return varargsOf(
                        FALSE,
                        valueOf("first argument must be a function")
                    )
                }

                val func = args.arg1().checkfunction()
                val funcArgs = args.subargs(2) // Get all arguments after the function

                try {
                    // Execute the function with provided arguments
                    val result = func.invoke(funcArgs)

                    // Return true followed by any results from the function
                    return varargsOf(TRUE, result)
                } catch (e: Exception) {
                    // Catch any Lua or Java exceptions
                    val err = simplifyLuaError(e.toString())

                    // Return false followed by the error message
                    return varargsOf(FALSE, valueOf(err))
                }
            }
        }


        _G["print"] = object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val stringBuilder = StringBuilder()
                for (i in 1..args.narg()) {
                    val value = args.arg(i)
                    stringBuilder.append(valueToString(value))
                    if (i < args.narg()) {
                        stringBuilder.append("\t")
                    }
                }
                (stringBuilder.toString()).d()
                return NONE
            }

            private fun valueToString(value: LuaValue): String {
                return when {
                    value.isnil() -> "nil"
                    value.isboolean() -> value.checkboolean().toString()
                    value.isnumber() -> value.checkdouble().toString()
                    value.isstring() -> value.checkjstring()
                    value.istable() -> tableToString(value.checktable())
                    value.isfunction() -> "function: ${value.tojstring()}"
                    value.isuserdata() -> {
                        // 处理 Java 对象，尝试调用 toString()
                        val userdata = value.checkuserdata()
                        userdata?.toString() ?: "userdata: null"
                    }

                    else -> value.tojstring() // 其他类型使用 LuaValue 默认的 toString
                }
            }

            private fun tableToString(table: LuaTable): String {
                val tableStringBuilder = StringBuilder()
                tableStringBuilder.append("{")
                var first = true

                var k: LuaValue = NIL
                while (true) {
                    val n: Varargs = table.next(k)
                    k = n.arg1() // 获取当前键
                    if (k.isnil()) { // 如果键是 nil，表示遍历结束
                        break
                    }
                    val v: LuaValue = n.arg(2) // 获取当前值

                    if (!first) {
                        tableStringBuilder.append(", ")
                    }
                    first = false

                    if (k.isstring()) {
                        tableStringBuilder.append(k.checkjstring())
                    } else if (k.isnumber()) {
                        tableStringBuilder.append("[${k.checkint()}]") // 对数字键使用方括号
                    } else {
                        tableStringBuilder.append(k.tojstring()) // 其他类型键
                    }
                    tableStringBuilder.append(" = ")
                    tableStringBuilder.append(valueToString(v)) // 递归处理值
                }
                tableStringBuilder.append("}")
                return tableStringBuilder.toString()
            }
        }

        //解析Pair,返回table
        _G["unPair"] = object : OneArgFunction() {
            override fun call(pairValue: LuaValue): LuaValue {
                if (pairValue.isuserdata()) {
                    val userdata = pairValue.checkuserdata()
                    if (userdata is Pair<*, *>) {
                        val pair = userdata
                        val table = LuaTable()
                        table.set(1, CoerceJavaToLua.coerce(pair.first))
                        table.set(2, CoerceJavaToLua.coerce(pair.second))
                        return table
                    }
                }
                return NIL
            }
        }


    }
}
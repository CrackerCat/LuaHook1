import android.content.Context
import de.robv.android.xposed.XSharedPreferences
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.VarArgFunction

// LuaSharedPreferences 类不再接收 Context 参数
class LuaSharedPreferences(
) : OneArgFunction() {

    // modName 和 mod 变量似乎没有在 call 方法的返回值中使用，可以考虑移除如果不需要
    private val modName = ""
    private var mod =
        LuaTable() // call 方法返回的是 mod，但 mod 一直是空的，实际注册的是 globals["sp"] 和 globals["xsp"]

    override fun call(globals: LuaValue): LuaValue {

        // 封装 Android 原生的 SharedPreferences 给 Lua 使用
        val sp = LuaTable()

        // 注意：原生的 SharedPreferences 操作现在需要 Context 作为第一个参数从 Lua 传入

        // 设置值
        sp["set"] = object : VarArgFunction() {
            override fun invoke(args: Varargs): LuaValue {
                // 需要 Context, name, key, value (至少4个参数)
                if (args.narg() < 4) {
                    return error("Usage: sp.set(context, name, key, value)")
                }

                // 从参数中获取 Context
                val context = args.checkuserdata(1, Context::class.java) as Context
                val name = args.checkjstring(2) // 参数索引后移
                val key = args.checkjstring(3) // 参数索引后移
                // value 是第四个参数，不需要单独取，直接通过 args.arg(4) 检查类型和值

                val prefs =
                    context.getSharedPreferences(name, Context.MODE_PRIVATE) // 使用传入的 Context
                val editor = prefs.edit()

                when {
                    args.isstring(4) -> editor.putString(key, args.checkjstring(4)) // 参数索引后移
                    args.isnumber(4) -> { // 参数索引后移
                        val num = args.checkdouble(4) // 参数索引后移
                        if (num % 1 == 0.0 && num >= Int.MIN_VALUE && num <= Int.MAX_VALUE) {
                            editor.putInt(key, num.toInt())
                        } else {
                            editor.putFloat(key, num.toFloat())
                        }
                    }

                    args.toboolean(4) -> editor.putBoolean(key, args.checkboolean(4)) // 参数索引后移
                    else -> return error("Unsupported value type for sp.set")
                }

                editor.apply()
                return TRUE
            }
        }

        // 获取值
        sp["get"] = object : VarArgFunction() {
            override fun invoke(args: Varargs): LuaValue {
                // 需要 Context, name, key, defaultValue (至少4个参数)
                if (args.narg() < 4) {
                    return error("Usage: sp.get(context, name, key, defaultValue)")
                }

                // 从参数中获取 Context
                val context = args.checkuserdata(1, Context::class.java) as Context
                val name = args.checkjstring(2) // 参数索引后移
                val key = args.checkjstring(3) // 参数索引后移
                // defaultValue 是第四个参数，args.arg(4)

                val prefs =
                    context.getSharedPreferences(name, Context.MODE_PRIVATE) // 使用传入的 Context


                // 如果键不存在，直接返回默认值 (第四个参数)
                if (!prefs.contains(key)) {
                    return args.arg(4)
                }


                // 根据默认值的类型来获取 SharedPreferences 中的值
                return when {
                    // 如果默认值是字符串，按字符串获取
                    args.isstring(4) -> LuaValue.valueOf(
                        prefs.getString(
                            key,
                            args.checkjstring(4) // 使用传入的默认值
                        )!!
                    )

                    // 如果默认值是布尔，按布尔获取
                    args.toboolean(4) -> LuaValue.valueOf(
                        prefs.getBoolean(
                            key,
                            args.checkboolean(4) // 使用传入的默认值
                        )
                    )

                    // 如果默认值是数字，尝试按 Int 或 Float 获取
                    args.isnumber(4) -> {
                        val defaultValue = args.checkdouble(4) // 使用传入的默认值
                        if (defaultValue % 1 == 0.0 && defaultValue >= Int.MIN_VALUE && defaultValue <= Int.MAX_VALUE) {
                            LuaValue.valueOf(prefs.getInt(key, defaultValue.toInt()).toDouble())
                        } else {
                            LuaValue.valueOf(prefs.getFloat(key, defaultValue.toFloat()).toDouble())
                        }
                    }

                    // 对于其他类型的默认值，直接返回默认值（例如 Lua nil, table 等）
                    else -> args.arg(4)
                }
            }
        }

        // 检查键是否存在
        sp["contains"] = object : VarArgFunction() {
            override fun invoke(args: Varargs): LuaValue {
                // 需要 Context, name, key (至少3个参数)
                if (args.narg() < 3) {
                    return error("Usage: sp.contains(context, name, key)")
                }

                // 从参数中获取 Context
                val context = args.checkuserdata(1, Context::class.java) as Context
                val name = args.checkjstring(2) // 参数索引后移
                val key = args.checkjstring(3) // 参数索引后移
                val prefs =
                    context.getSharedPreferences(name, Context.MODE_PRIVATE) // 使用传入的 Context

                return LuaValue.valueOf(prefs.contains(key))
            }
        }

        // 删除键
        sp["remove"] = object : VarArgFunction() {
            override fun invoke(args: Varargs): LuaValue {
                // 需要 Context, name, key (至少3个参数)
                if (args.narg() < 3) {
                    return error("Usage: sp.remove(context, name, key)")
                }

                // 从参数中获取 Context
                val context = args.checkuserdata(1, Context::class.java) as Context
                val name = args.checkjstring(2) // 参数索引后移
                val key = args.checkjstring(3) // 参数索引后移
                val prefs =
                    context.getSharedPreferences(name, Context.MODE_PRIVATE) // 使用传入的 Context

                prefs.edit().remove(key).apply()
                return LuaValue.TRUE
            }
        }

        // 获取所有键值对
        sp["getAll"] = object : VarArgFunction() {
            override fun invoke(args: Varargs): LuaValue {
                // 需要 Context, name (至少2个参数)
                if (args.narg() < 2) {
                    return error("Usage: sp.getAll(context, name)")
                }

                // 从参数中获取 Context
                val context = args.checkuserdata(1, Context::class.java) as Context
                val name = args.checkjstring(2) // 参数索引后移
                val prefs =
                    context.getSharedPreferences(name, Context.MODE_PRIVATE) // 使用传入的 Context
                val all = prefs.all
                val table = LuaTable()

                // 遍历并转换为 LuaValue
                all.forEach { (k, v) ->
                    when (v) {
                        is String -> table[LuaValue.valueOf(k)] = LuaValue.valueOf(v)
                        is Int -> table[LuaValue.valueOf(k)] =
                            LuaValue.valueOf(v.toDouble()) // Lua numbers are doubles
                        is Float -> table[LuaValue.valueOf(k)] =
                            LuaValue.valueOf(v.toDouble()) // Lua numbers are doubles
                        is Boolean -> table[LuaValue.valueOf(k)] = LuaValue.valueOf(v)
                        else -> table[LuaValue.valueOf(k)] =
                            LuaValue.valueOf(v.toString()) // Fallback to string
                    }
                }

                return table
            }
        }

        // 清除所有键值对
        sp["clear"] = object : VarArgFunction() {
            override fun invoke(args: Varargs): LuaValue {
                // 需要 Context, name (至少2个参数)
                if (args.narg() < 2) {
                    return error("Usage: sp.clear(context, name)")
                }

                // 从参数中获取 Context
                val context = args.checkuserdata(1, Context::class.java) as Context
                val name = args.checkjstring(2) // 参数索引后移
                val prefs =
                    context.getSharedPreferences(name, Context.MODE_PRIVATE) // 使用传入的 Context

                prefs.edit().clear().apply()
                return LuaValue.TRUE
            }
        }

        // 将封装好的 sp 表注册到 Lua 全局变量 globals 中
        globals["sp"] = sp


        // XSharedPreferences 部分不需要 Context，所以函数签名不变
        val xsp = LuaTable()

        // 获取指定包名和名称的 XSharedPreferences
        // 这个 helper 函数仍然放在这里，它不依赖于类构造函数中的 Context
        val getXPrefs = { packageName: String, name: String ->
            val prefs = XSharedPreferences(packageName, name)
            // 确保可以访问
            prefs.makeWorldReadable()
            prefs
        }

        // 获取值 (参数不变，因为不需要 Context)
        xsp["get"] = object : VarArgFunction() {
            override fun invoke(args: Varargs): LuaValue {
                if (args.narg() < 4) {
                    return error("Usage: xsp.get(packageName, name, key, defaultValue)")
                }

                val packageName = args.checkjstring(1)
                val name = args.checkjstring(2)
                val key = args.checkjstring(3)
                val prefs = getXPrefs(packageName, name) // 使用 helper 函数获取 prefs

                if (!prefs.contains(key)) {
                    return args.arg(4) // 返回默认值
                }

                return when {
                    args.isstring(4) -> LuaValue.valueOf(
                        prefs.getString(
                            key,
                            args.checkjstring(4)
                        )!!
                    )

                    args.toboolean(4) -> LuaValue.valueOf(
                        prefs.getBoolean(
                            key,
                            args.checkboolean(4)
                        )
                    )

                    args.isnumber(4) -> {
                        val defaultValue = args.checkdouble(4)
                        if (defaultValue % 1 == 0.0 && defaultValue >= Int.MIN_VALUE && defaultValue <= Int.MAX_VALUE) {
                            LuaValue.valueOf(prefs.getInt(key, defaultValue.toInt()).toDouble())
                        } else {
                            LuaValue.valueOf(prefs.getFloat(key, defaultValue.toFloat()).toDouble())
                        }
                    }

                    else -> args.arg(4) // 返回默认值
                }
            }
        }
        // 检查键是否存在 (参数不变)
        xsp["contains"] = object : VarArgFunction() {
            override fun invoke(args: Varargs): LuaValue {
                if (args.narg() < 3) {
                    return error("Usage: xsp.contains(packageName, name, key)")
                }

                val packageName = args.checkjstring(1)
                val name = args.checkjstring(2)
                val key = args.checkjstring(3)
                val prefs = getXPrefs(packageName, name) // 使用 helper 函数获取 prefs

                return LuaValue.valueOf(prefs.contains(key))
            }
        }

        // 获取所有键值对 (参数不变)
        xsp["getAll"] = object : VarArgFunction() {
            override fun invoke(args: Varargs): LuaValue {
                if (args.narg() < 2) {
                    return error("Usage: xsp.getAll(packageName, name)")
                }

                val packageName = args.checkjstring(1)
                val name = args.checkjstring(2)
                val prefs = getXPrefs(packageName, name) // 使用 helper 函数获取 prefs
                val all = prefs.all
                val table = LuaTable()

                all.forEach { (k, v) ->
                    when (v) {
                        is String -> table[LuaValue.valueOf(k)] = LuaValue.valueOf(v)
                        is Int -> table[LuaValue.valueOf(k)] = LuaValue.valueOf(v.toDouble())
                        is Float -> table[LuaValue.valueOf(k)] = LuaValue.valueOf(v.toDouble())
                        is Boolean -> table[LuaValue.valueOf(k)] = LuaValue.valueOf(v)
                        else -> table[LuaValue.valueOf(k)] = LuaValue.valueOf(v.toString())
                    }
                }

                return table
            }
        }

        // 重新加载 (参数不变)
        xsp["reload"] = object : VarArgFunction() {
            override fun invoke(args: Varargs): LuaValue {
                if (args.narg() < 2) {
                    return error("Usage: xsp.reload(packageName, name)")
                }

                val packageName = args.checkjstring(1)
                val name = args.checkjstring(2)
                val prefs = getXPrefs(packageName, name) // 使用 helper 函数获取 prefs

                prefs.reload()
                return NIL
            }
        }

        // 将封装好的 xsp 表注册到 Lua 全局变量 globals 中
        globals["xsp"] = xsp

        // 返回一个 LuaValue，通常库函数返回 nil 或者一个表示库的表
        // 由于我们将 sp 和 xsp 直接注册到了 globals，这里返回 mod (空的) 或 NIL 都可以
        return mod // 或者 LuaValue.NIL
    }
}
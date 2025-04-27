package com.kulipai.luahook1


import HookLib
import LuaDrawableLoader
import LuaHttp
import LuaImport
import LuaJson
import LuaResourceBridge
import Luafile
import android.app.Application
import android.content.Context
import android.util.Log
import com.kulipai.luahook1.util.d
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform
import org.luckypray.dexkit.DexKitBridge
import top.sacz.xphelper.XpHelper
import top.sacz.xphelper.dexkit.DexFinder
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader


//


class MainHook : IXposedHookZygoteInit, IXposedHookLoadPackage {

    companion object {
//        init {
//            System.loadLibrary("dexkit")
//        }

    }


    //在这里写lua脚本
    var luaScript: String = """
        log(1)
    """.trimIndent()


    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {

        XpHelper.initZygote(startupParam);
    }

    fun readRawTextFile(context: Context?, rawResId: Int): String? {
        if (context == null) {
            ("Context is null, cannot read raw resource.").d()
            return null
        }

        val resources = context.getResources()
        var inputStream: InputStream? = null
        var reader: BufferedReader? = null
        val text = StringBuilder()

        try {
            inputStream = resources.openRawResource(rawResId)
            reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
                text.append(line).append('\n')
            }
            return text.toString()
        } catch (e: IOException) {
           ( "Error reading raw text file (ID: " + rawResId + "): " + e.message).d()
            return null
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                   ("Error closing input stream: " + e.message).d()
                }
            }
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    ("Error closing buffered reader: " + e.message).d()
                }
            }
        }
    }


    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        "1".d()

        XposedHelpers.findAndHookMethod(
            Application::class.java,
            "attach",
            Context::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    "appli".d()
                    R.raw.hook.toString().d()
                    var context = param.thisObject as Context
                    readRawTextFile(context,R.raw.hook)?.d()
                    "ok".d()
                }
            })

        val globals: Globals = JsePlatform.standardGlobals()

        //加载Lua模块
        globals["XpHelper"] = CoerceJavaToLua.coerce(XpHelper::class.java)
        globals["DexFinder"] = CoerceJavaToLua.coerce(DexFinder::class.java)
        globals["XposedHelpers"] = CoerceJavaToLua.coerce(XposedHelpers::class.java)
        globals["XposedBridge"] = CoerceJavaToLua.coerce(XposedBridge::class.java)
        globals["DexKitBridge"] = CoerceJavaToLua.coerce(DexKitBridge::class.java)
        globals["this"] = CoerceJavaToLua.coerce(this)
        HookLib(lpparam).call(globals)
        LuaJson().call(globals)
        LuaHttp().call(globals)
        Luafile().call(globals)
        globals["import"] = LuaImport(lpparam.classLoader, this::class.java.classLoader!!, globals)


        val LuaFile = object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val path = arg.checkjstring()
                val file = File(path)
                return CoerceJavaToLua.coerce(file)
            }
        }

        globals["File"] = LuaFile

        LuaResourceBridge().registerTo(globals)

        LuaDrawableLoader().registerTo(globals)

        //全局脚本
        try {
            val chunk: LuaValue = globals.load(luaScript)
            chunk.call()
        } catch (e: Exception) {
            e.toString().d()
        }


    }


}